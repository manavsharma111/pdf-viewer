package com.antigravity.pdfviewer.presentation.tools.merge

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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antigravity.pdfviewer.presentation.tools.PdfToolsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(
    viewModel: PdfToolsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val toolState by viewModel.toolState.collectAsStateWithLifecycle()
    val selectedUris = remember { mutableStateListOf<Uri>() }

    val pickPdfsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris.addAll(uris)
        }
    }

    val saveMergedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null) {
            val out = context.contentResolver.openOutputStream(destUri)
            if (out != null) {
                viewModel.mergePdfs(selectedUris.toList(), out)
            } else {
                Toast.makeText(context, "Could not open destination file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(toolState.isSuccess) {
        if (toolState.isSuccess) {
            Toast.makeText(context, toolState.successMessage ?: "Merged successfully!", Toast.LENGTH_SHORT).show()
            selectedUris.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge PDFs") },
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
                    text = "Selected Files (${selectedUris.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(onClick = { pickPdfsLauncher.launch(arrayOf("application/pdf")) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add PDFs", modifier = Modifier.padding(start = 4.dp))
                }
            }

            if (selectedUris.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add 2 or more PDF files to merge into a single document.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(selectedUris) { index, uri ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Text(
                                    text = "${index + 1}. ${uri.lastPathSegment ?: "Document.pdf"}",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val item = selectedUris.removeAt(index)
                                            selectedUris.add(index - 1, item)
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                                }

                                IconButton(
                                    onClick = {
                                        if (index < selectedUris.size - 1) {
                                            val item = selectedUris.removeAt(index)
                                            selectedUris.add(index + 1, item)
                                        }
                                    },
                                    enabled = index < selectedUris.size - 1
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                                }

                                IconButton(onClick = { selectedUris.removeAt(index) }) {
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
                    LinearProgressIndicator(
                        progress = toolState.progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = { saveMergedLauncher.launch("Combined_${System.currentTimeMillis()}.pdf") },
                enabled = selectedUris.size >= 2 && !toolState.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Merge ${selectedUris.size} Documents")
            }
        }
    }
}
