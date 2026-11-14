package com.antigravity.pdfviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.antigravity.pdfviewer.domain.model.AppTheme
import com.antigravity.pdfviewer.presentation.navigation.AppNavGraph
import com.antigravity.pdfviewer.ui.theme.PDFViewerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PdfViewerApplication

        // Handle incoming PDF from ACTION_VIEW or ACTION_SEND
        val initialUri: Uri? = parseIntentUri(intent)

        setContent {
            val userSettings by app.preferencesRepository.userSettingsFlow.collectAsStateWithLifecycle(
                initialValue = com.antigravity.pdfviewer.domain.model.UserSettings()
            )

            PDFViewerTheme(appTheme = userSettings.theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        app = app,
                        initialUri = initialUri
                    )
                }
            }
        }
    }

    private fun parseIntentUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            else -> null
        }
    }
}
