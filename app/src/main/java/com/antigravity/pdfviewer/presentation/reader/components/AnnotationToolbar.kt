package com.antigravity.pdfviewer.presentation.reader.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.antigravity.pdfviewer.domain.model.AnnotationType
import com.antigravity.pdfviewer.domain.model.PointF
import com.antigravity.pdfviewer.domain.model.TextAnnotation

@Composable
fun AnnotationToolbar(
    activeTool: AnnotationType?,
    activeColor: Int,
    activeStrokeWidth: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSelected: (AnnotationType?) -> Unit,
    onColorSelected: (Int) -> Unit,
    onStrokeWidthSelected: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onAddTextNote: (TextAnnotation) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var textSize by remember { mutableFloatStateOf(16f) }

    val standardColors = listOf(
        AndroidColor.RED,
        AndroidColor.BLUE,
        AndroidColor.GREEN,
        AndroidColor.YELLOW,
        AndroidColor.rgb(255, 152, 0), // Orange
        AndroidColor.rgb(156, 39, 176), // Purple
        AndroidColor.BLACK
    )

    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Add Text Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Enter text") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Text("Font size: ${textSize.toInt()} sp")
                    Slider(
                        value = textSize,
                        onValueChange = { textSize = it },
                        valueRange = 10f..36f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onAddTextNote(
                                TextAnnotation(
                                    text = textInput,
                                    position = PointF(0.2f, 0.3f), // placed comfortably in viewport
                                    color = activeColor,
                                    fontSize = textSize
                                )
                            )
                        }
                        showTextDialog = false
                        textInput = ""
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Tools selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close annotation mode")
                }

                FilterChip(
                    selected = activeTool == AnnotationType.PEN,
                    onClick = { onToolSelected(if (activeTool == AnnotationType.PEN) null else AnnotationType.PEN) },
                    label = { Text("Pen") },
                    leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) }
                )

                FilterChip(
                    selected = activeTool == AnnotationType.HIGHLIGHT,
                    onClick = { onToolSelected(if (activeTool == AnnotationType.HIGHLIGHT) null else AnnotationType.HIGHLIGHT) },
                    label = { Text("Highlight") },
                    leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null) }
                )

                FilterChip(
                    selected = activeTool == AnnotationType.UNDERLINE,
                    onClick = { onToolSelected(if (activeTool == AnnotationType.UNDERLINE) null else AnnotationType.UNDERLINE) },
                    label = { Text("Underline") },
                    leadingIcon = { Icon(Icons.Default.FormatUnderlined, contentDescription = null) }
                )

                FilterChip(
                    selected = activeTool == AnnotationType.STRIKETHROUGH,
                    onClick = { onToolSelected(if (activeTool == AnnotationType.STRIKETHROUGH) null else AnnotationType.STRIKETHROUGH) },
                    label = { Text("Strike") },
                    leadingIcon = { Icon(Icons.Default.FormatStrikethrough, contentDescription = null) }
                )

                FilterChip(
                    selected = activeTool == AnnotationType.ERASER,
                    onClick = { onToolSelected(if (activeTool == AnnotationType.ERASER) null else AnnotationType.ERASER) },
                    label = { Text("Eraser") },
                    leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) }
                )

                FilterChip(
                    selected = false,
                    onClick = { showTextDialog = true },
                    label = { Text("Add Text") },
                    leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) }
                )

                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }

                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                }
            }

            // Row 2: Color Palette & Thickness
            if (activeTool != null && activeTool != AnnotationType.ERASER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color dots
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        standardColors.forEach { col ->
                            val isSelected = activeColor == col
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(col))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { onColorSelected(col) }
                            )
                        }
                    }

                    // Stroke width
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${activeStrokeWidth.toInt()}px",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = activeStrokeWidth,
                            onValueChange = onStrokeWidthSelected,
                            valueRange = 2f..24f,
                            modifier = Modifier.size(width = 100.dp, height = 24.dp)
                        )
                    }
                }
            }
        }
    }
}
