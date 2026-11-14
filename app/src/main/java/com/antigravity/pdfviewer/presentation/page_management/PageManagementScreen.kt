package com.antigravity.pdfviewer.presentation.page_management

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antigravity.pdfviewer.presentation.reader.components.ThumbnailGridItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagementScreen(
    uri: Uri,
    viewModel: PageManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uri) {
        viewModel.loadPages(uri)
    }

    // Save Reorganized PDF launcher
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null) {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                viewModel.saveChanges(out)
            }
        }
    }

    // Extract Selected Pages launcher
    val extractLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null) {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                viewModel.extractSelectedToStream(out)
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "Operation completed successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Page Management (${uiState.pages.size} pages)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (uiState.selectedIndices.size == uiState.pages.size) {
                            viewModel.deselectAll()
                        } else {
                            viewModel.selectAll()
                        }
                    }) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                    }

                    IconButton(onClick = { saveLauncher.launch("Organized_document.pdf") }) {
                        Icon(Icons.Default.Save, contentDescription = "Save PDF")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.selectedIndices.isNotEmpty()) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.moveSelectedUp() }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                        }

                        IconButton(onClick = { viewModel.moveSelectedDown() }) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                        }

                        IconButton(onClick = { viewModel.rotateSelectedPages(true) }) {
                            Icon(Icons.Default.Rotate90DegreesCw, contentDescription = "Rotate CW")
                        }

                        IconButton(onClick = { viewModel.duplicateSelectedPages() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                        }

                        IconButton(onClick = { extractLauncher.launch("Extracted_pages.pdf") }) {
                            Icon(Icons.Default.IosShare, contentDescription = "Extract")
                        }

                        IconButton(onClick = { viewModel.deleteSelectedPages() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.pages, key = { index, item -> "${item.originalIndex}_$index" }) { index, item ->
                        val isSelected = uiState.selectedIndices.contains(index)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = MaterialTheme.shapes.small
                                )
                                .background(Color.White, shape = MaterialTheme.shapes.small)
                                .clickable { viewModel.togglePageSelection(index) }
                                .padding(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ThumbnailGridItem(
                                    pageIndex = item.originalIndex,
                                    isSelected = isSelected,
                                    onRender = { viewModel.renderThumbnail(it) },
                                    onClick = { viewModel.togglePageSelection(index) }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Page ${index + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.togglePageSelection(index) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isSaving) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Text("Processing pages...")
                    }
                }
            }
        }
    }
}
