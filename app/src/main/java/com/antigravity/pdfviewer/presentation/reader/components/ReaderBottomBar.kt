package com.antigravity.pdfviewer.presentation.reader.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    zoomScale: Float,
    onPageSelected: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onFirstPage: () -> Unit,
    onLastPage: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onThumbnailsClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpInputText by remember { mutableStateOf("") }
    var sliderValue by remember(currentPage) { mutableFloatStateOf((currentPage + 1).toFloat()) }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Go to Page") },
            text = {
                OutlinedTextField(
                    value = jumpInputText,
                    onValueChange = { jumpInputText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Page number (1 - $totalPages)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = jumpInputText.toIntOrNull()
                        if (target != null && target in 1..totalPages) {
                            onPageSelected(target - 1)
                        }
                        showJumpDialog = false
                        jumpInputText = ""
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Slider row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFirstPage, enabled = currentPage > 0) {
                    Icon(Icons.Default.FirstPage, contentDescription = "First page")
                }

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        onPageSelected((sliderValue.toInt() - 1).coerceIn(0, totalPages - 1))
                    },
                    valueRange = 1f..totalPages.coerceAtLeast(1).toFloat(),
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onLastPage, enabled = currentPage < totalPages - 1) {
                    Icon(Icons.Default.LastPage, contentDescription = "Last page")
                }
            }

            // Controls row: Page navigation, Jump indicator, Zoom controls, Thumbnails
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnails & Bookmarks shortcuts
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onThumbnailsClick) {
                        Icon(Icons.Default.GridView, contentDescription = "Thumbnails")
                    }
                    IconButton(onClick = onBookmarksClick) {
                        Icon(Icons.Default.Bookmarks, contentDescription = "Bookmarks")
                    }
                }

                // Page indicator with jump dialog trigger
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onPreviousPage, enabled = currentPage > 0) {
                        Icon(Icons.Default.NavigateBefore, contentDescription = "Previous Page")
                    }

                    Text(
                        text = "${currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                jumpInputText = (currentPage + 1).toString()
                                showJumpDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    IconButton(onClick = onNextPage, enabled = currentPage < totalPages - 1) {
                        Icon(Icons.Default.NavigateNext, contentDescription = "Next Page")
                    }
                }

                // Zoom controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onZoomOut, enabled = zoomScale > 1.0f) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom out")
                    }

                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { onResetZoom() }
                    )

                    IconButton(onClick = onZoomIn, enabled = zoomScale < 6.0f) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom in")
                    }
                }
            }
        }
    }
}
