package com.antigravity.pdfviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.antigravity.pdfviewer.domain.model.AppTheme
import com.antigravity.pdfviewer.presentation.navigation.AppNavGraph
import com.antigravity.pdfviewer.presentation.navigation.Screen
import com.antigravity.pdfviewer.ui.theme.PDFViewerTheme

class MainActivity : ComponentActivity() {

    private val incomingUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PdfViewerApplication

        // Handle incoming PDF from ACTION_VIEW or ACTION_SEND
        val initialUri: Uri? = parseIntentUri(intent)?.also { uri ->
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        incomingUriState.value = initialUri

        setContent {
            val userSettings by app.preferencesRepository.userSettingsFlow.collectAsStateWithLifecycle(
                initialValue = com.antigravity.pdfviewer.domain.model.UserSettings()
            )
            val currentIncomingUri by incomingUriState

            PDFViewerTheme(appTheme = userSettings.theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    LaunchedEffect(currentIncomingUri) {
                        currentIncomingUri?.let { uri ->
                            navController.navigate(Screen.Reader.createRoute(uri, 0)) {
                                launchSingleTop = true
                            }
                        }
                    }

                    AppNavGraph(
                        navController = navController,
                        app = app,
                        initialUri = initialUri
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newUri = parseIntentUri(intent)?.also { uri ->
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        if (newUri != null) {
            incomingUriState.value = newUri
        }
    }

    private fun parseIntentUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }
    }
}
