package com.antigravity.pdfviewer.presentation.tools.image_to_pdf

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antigravity.pdfviewer.presentation.tools.PdfToolsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    viewModel: PdfToolsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val toolState by viewModel.toolState.collectAsStateWithLifecycle()
    val selectedImages = remember { mutableStateListOf<Uri>() }
    var pageSize by remember { mutableStateOf("A4") }

    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.addAll(uris)
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null) {
            val out = context.contentResolver.openOutputStream(destUri)
            if (out != null) {
                viewModel.convertImagesToPdf(selectedImages.toList(), out, pageSize)
            } else {
                Toast.makeText(context, "Could not open destination file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(toolState.isSuccess) {
        if (toolState.isSuccess) {
            Toast.makeText(context, toolState.successMessage ?: "Created PDF successfully!", Toast.LENGTH_SHORT).show()
            selectedImages.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image to PDF") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected Images (${selectedImages.size})",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedButton(onClick = { pickImagesLauncher.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add Images", modifier = Modifier.padding(start = 4.dp))
                }
            }

            Text("Target Page Size", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = pageSize == "A4",
                    onClick = { pageSize = "A4" },
                    label = { Text("A4 Standard") }
                )
                FilterChip(
                    selected = pageSize == "LETTER",
                    onClick = { pageSize = "LETTER" },
                    label = { Text("US Letter") }
                )
                FilterChip(
                    selected = pageSize == "ORIGINAL",
                    onClick = { pageSize = "ORIGINAL" },
                    label = { Text("Original Image Size") }
                )
            }

            if (selectedImages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add images (JPEG, PNG, WebP) to combine into a PDF.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(selectedImages) { index, uri ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Text(
                                    text = "${index + 1}. ${uri.lastPathSegment ?: "Image"}",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val item = selectedImages.removeAt(index)
                                            selectedImages.add(index - 1, item)
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                                }

                                IconButton(
                                    onClick = {
                                        if (index < selectedImages.size - 1) {
                                            val item = selectedImages.removeAt(index)
                                            selectedImages.add(index + 1, item)
                                        }
                                    },
                                    enabled = index < selectedImages.size - 1
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                                }

                                IconButton(onClick = { selectedImages.removeAt(index) }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }
            }

            if (toolState.isRunning) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(toolState.currentStep, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Button(
                onClick = { savePdfLauncher.launch("Images_${System.currentTimeMillis()}.pdf") },
                enabled = selectedImages.isNotEmpty() && !toolState.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create PDF (${selectedImages.size} pages)")
            }
        }
    }
}
