package com.antigravity.pdfviewer.presentation.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.pdfviewer.domain.model.PdfDocumentInfo
import java.text.DecimalFormat

@Composable
fun MetadataDialog(
    documentInfo: PdfDocumentInfo,
    onDismiss: () -> Unit
) {
    val meta = documentInfo.metadata

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Properties") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetadataRow(label = "File Name", value = documentInfo.fileName)
                MetadataRow(label = "File Size", value = formatFileSize(documentInfo.fileSize))
                MetadataRow(label = "Total Pages", value = "${documentInfo.pageCount}")
                Divider()
                MetadataRow(label = "Title", value = meta.title ?: "Untitled")
                MetadataRow(label = "Author", value = meta.author ?: "Unknown")
                MetadataRow(label = "Subject", value = meta.subject ?: "None")
                MetadataRow(label = "Keywords", value = meta.keywords ?: "None")
                MetadataRow(label = "Creator", value = meta.creator ?: "Unknown")
                MetadataRow(label = "Producer", value = meta.producer ?: "Unknown")
                MetadataRow(label = "Created", value = meta.creationDate ?: "Unknown")
                MetadataRow(label = "Modified", value = meta.modificationDate ?: "Unknown")
                MetadataRow(label = "PDF Version", value = meta.version ?: "1.4")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
