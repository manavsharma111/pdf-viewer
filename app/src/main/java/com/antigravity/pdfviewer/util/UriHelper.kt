package com.antigravity.pdfviewer.util

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

object UriHelper {

    fun openInputStream(context: Context, uri: Uri): InputStream? {
        val scheme = uri.scheme
        if (scheme == "file" || scheme.isNullOrEmpty()) {
            val path = uri.path ?: uri.toString().removePrefix("file://")
            val file = File(path)
            if (file.exists() && file.canRead()) {
                return FileInputStream(file)
            }
        }

        // Try ContentResolver
        return try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            // Fallback: try raw path if uri has a path component
            uri.path?.let { rawPath ->
                val cleaned = if (rawPath.startsWith("/root/")) rawPath.removePrefix("/root") else rawPath
                val file = File(cleaned)
                if (file.exists() && file.canRead()) {
                    return FileInputStream(file)
                }
            }
            null
        }
    }

    fun openFileDescriptor(context: Context, uri: Uri, mode: String = "r"): ParcelFileDescriptor? {
        val scheme = uri.scheme
        if (scheme == "file" || scheme.isNullOrEmpty()) {
            val path = uri.path ?: uri.toString().removePrefix("file://")
            val file = File(path)
            if (file.exists() && file.canRead()) {
                val pfdMode = if (mode.contains("w")) ParcelFileDescriptor.MODE_READ_WRITE else ParcelFileDescriptor.MODE_READ_ONLY
                return ParcelFileDescriptor.open(file, pfdMode)
            }
        }

        // Try direct ContentResolver descriptor
        return try {
            context.contentResolver.openFileDescriptor(uri, mode)
        } catch (e: Exception) {
            // If ContentResolver denies or has no provider for this URI, copy stream to internal cache
            try {
                openInputStream(context, uri)?.use { input ->
                    val cacheFile = File(context.cacheDir, "doc_cache_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(cacheFile).use { out ->
                        input.copyTo(out)
                    }
                    ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                }
            } catch (fallbackEx: Exception) {
                null
            }
        }
    }

    fun queryFileName(context: Context, uri: Uri): String {
        val scheme = uri.scheme
        if (scheme == "content") {
            val name = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1 && cursor.moveToFirst()) {
                        cursor.getString(index)
                    } else null
                }
            }.getOrNull()
            if (!name.isNullOrBlank()) return name
        }

        // Fallback to path segment
        val lastSegment = uri.lastPathSegment ?: "Document.pdf"
        val cleaned = lastSegment.substringAfterLast('/').substringAfterLast(':')
        return if (cleaned.isNotBlank()) cleaned else "Document.pdf"
    }

    fun queryFileSize(context: Context, uri: Uri): Long {
        val scheme = uri.scheme
        if (scheme == "content") {
            val size = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index != -1 && cursor.moveToFirst()) {
                        cursor.getLong(index)
                    } else null
                }
            }.getOrNull()
            if (size != null && size > 0) return size
        }

        // Fallback: check file
        return runCatching {
            val path = uri.path ?: uri.toString().removePrefix("file://")
            File(path).length()
        }.getOrDefault(0L)
    }
}
