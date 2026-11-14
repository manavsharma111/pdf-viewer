package com.antigravity.pdfviewer.presentation.tools.pdf_to_image

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antigravity.pdfviewer.presentation.tools.PdfToolsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImageScreen(
    viewModel: PdfToolsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val toolState by viewModel.toolState.collectAsStateWithLifecycle()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var format by remember { mutableStateOf("PNG") }
    var dpi by remember { mutableIntStateOf(150) }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedUri = uri
    }

    LaunchedEffect(toolState.isSuccess) {
        if (toolState.isSuccess) {
            Toast.makeText(context, toolState.successMessage ?: "Rendered images successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF to Image") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedUri?.lastPathSegment ?: "No PDF selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (selectedUri != null) "Ready to export images" else "Select a PDF document",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) }) {
                        Text(if (selectedUri != null) "Change" else "Select PDF")
                    }
                }
            }

            Text("Image Format", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = format == "PNG",
                    onClick = { format = "PNG" },
                    label = { Text("PNG (Lossless)") }
                )
                FilterChip(
                    selected = format == "JPEG",
                    onClick = { format = "JPEG" },
                    label = { Text("JPEG (Compact)") }
                )
            }

            Text("Resolution (DPI)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = dpi == 72,
                    onClick = { dpi = 72 },
                    label = { Text("72 DPI (Screen)") }
                )
                FilterChip(
                    selected = dpi == 150,
                    onClick = { dpi = 150 },
                    label = { Text("150 DPI (Standard)") }
                )
                FilterChip(
                    selected = dpi == 300,
                    onClick = { dpi = 300 },
                    label = { Text("300 DPI (High Res)") }
                )
            }

            if (toolState.isRunning) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(toolState.currentStep, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = toolState.progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    val uri = selectedUri ?: return@Button
                    val outDir = File(context.cacheDir, "images_${System.currentTimeMillis()}").apply { mkdirs() }
                    viewModel.convertPdfToImages(uri, outDir, format, dpi)
                },
                enabled = selectedUri != null && !toolState.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Convert to Images")
            }
        }
    }
}
