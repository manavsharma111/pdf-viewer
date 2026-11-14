package com.antigravity.pdfviewer.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.antigravity.pdfviewer.domain.model.CachePreset
import com.antigravity.pdfviewer.domain.model.PdfPageInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class NativePdfRendererManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val renderMutex = Mutex()

    private var currentPfd: ParcelFileDescriptor? = null
    private var currentRenderer: PdfRenderer? = null
    private var currentUri: Uri? = null
    private var tempFile: File? = null

    // In-flight render jobs mapped by page index
    private val activeJobs = ConcurrentHashMap<Int, Job>()

    // Memory-aware LRU bitmap cache
    private var bitmapCache: LruCache<Int, Bitmap> = createBitmapCache(CachePreset.MEDIUM)

    val pageCount: Int
        get() = currentRenderer?.pageCount ?: 0

    fun updateCachePreset(preset: CachePreset) {
        val oldCache = bitmapCache
        bitmapCache = createBitmapCache(preset)
        oldCache.evictAll()
    }

    private fun createBitmapCache(preset: CachePreset): LruCache<Int, Bitmap> {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt() // in KB
        val fraction = when (preset) {
            CachePreset.LOW -> 12     // 1/12th of available heap
            CachePreset.MEDIUM -> 8   // 1/8th of available heap
            CachePreset.HIGH -> 5     // 1/5th of available heap
        }
        val cacheSize = (maxMemory / fraction).coerceAtLeast(16 * 1024) // at least 16MB

        return object : LruCache<Int, Bitmap>(cacheSize) {
            override fun sizeOf(key: Int, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    suspend fun openDocument(uri: Uri): Int = withContext(Dispatchers.IO) {
        closeDocument()
        currentUri = uri

        val pfd = try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            // If direct descriptor is not seekable or restricted by content provider, copy to cache
            val temp = File(context.cacheDir, "renderer_temp_${System.currentTimeMillis()}.pdf")
            tempFile = temp
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                }
            }
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
        } ?: throw IllegalArgumentException("Cannot open file descriptor for URI: $uri")

        currentPfd = pfd
        val renderer = PdfRenderer(pfd)
        currentRenderer = renderer
        renderer.pageCount
    }

    suspend fun openFile(file: File): Int = withContext(Dispatchers.IO) {
        closeDocument()
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        currentPfd = pfd
        val renderer = PdfRenderer(pfd)
        currentRenderer = renderer
        renderer.pageCount
    }

    suspend fun getPageInfo(pageIndex: Int): PdfPageInfo = renderMutex.withLock {
        val renderer = currentRenderer ?: throw IllegalStateException("Renderer not initialized")
        require(pageIndex in 0 until renderer.pageCount) { "Invalid page index $pageIndex" }

        val page = renderer.openPage(pageIndex)
        val info = PdfPageInfo(
            pageIndex = pageIndex,
            width = page.width,
            height = page.height,
            rotation = 0
        )
        page.close()
        info
    }

    fun getCachedBitmap(pageIndex: Int): Bitmap? {
        return bitmapCache.get(pageIndex)
    }

    suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        densityMultiplier: Float = 1.0f
    ): Bitmap = withContext(Dispatchers.Default) {
        // Check cache first
        bitmapCache.get(pageIndex)?.let { return@withContext it }

        renderMutex.withLock {
            // Re-check cache inside lock
            bitmapCache.get(pageIndex)?.let { return@withLock it }

            val renderer = currentRenderer
                ?: throw IllegalStateException("PDF renderer is not opened")

            if (pageIndex !in 0 until renderer.pageCount) {
                throw IndexOutOfBoundsException("Page index $pageIndex out of range 0..${renderer.pageCount - 1}")
            }

            val page = renderer.openPage(pageIndex)
            try {
                val pageWidth = page.width
                val pageHeight = page.height

                val (destWidth, destHeight) = if (targetWidth != null && targetHeight != null) {
                    targetWidth to targetHeight
                } else if (targetWidth != null) {
                    val ratio = targetWidth.toFloat() / pageWidth
                    targetWidth to (pageHeight * ratio).toInt()
                } else {
                    ((pageWidth * densityMultiplier).toInt()) to ((pageHeight * densityMultiplier).toInt())
                }

                val finalWidth = destWidth.coerceAtLeast(100)
                val finalHeight = destHeight.coerceAtLeast(100)

                val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
                // Fill with white background
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                bitmapCache.put(pageIndex, bitmap)
                bitmap
            } finally {
                page.close()
            }
        }
    }

    fun prefetchPages(currentPage: Int, prefetchCount: Int = 2, targetWidth: Int? = null) {
        if (prefetchCount <= 0 || currentRenderer == null) return

        val totalPages = pageCount
        val range = (currentPage - prefetchCount..currentPage + prefetchCount)
            .filter { it in 0 until totalPages && it != currentPage }

        for (page in range) {
            if (bitmapCache.get(page) == null && activeJobs[page] == null) {
                val job = scope.launch {
                    try {
                        renderPage(page, targetWidth = targetWidth)
                    } catch (e: CancellationException) {
                        // ignore cancelled prefetch
                    } catch (e: Exception) {
                        // ignore prefetch failure
                    } finally {
                        activeJobs.remove(page)
                    }
                }
                activeJobs[page] = job
            }
        }
    }

    fun cancelDistantJobs(currentPage: Int, keepDistance: Int = 3) {
        val iterator = activeJobs.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (kotlin.math.abs(entry.key - currentPage) > keepDistance) {
                entry.value.cancel()
                iterator.remove()
            }
        }
    }

    fun clearCache() {
        bitmapCache.evictAll()
    }

    fun closeDocument() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        bitmapCache.evictAll()

        try {
            currentRenderer?.close()
        } catch (e: Exception) {
            // ignore
        }
        currentRenderer = null

        try {
            currentPfd?.close()
        } catch (e: Exception) {
            // ignore
        }
        currentPfd = null

        tempFile?.let {
            runCatching { it.delete() }
        }
        tempFile = null
        currentUri = null
    }
}
