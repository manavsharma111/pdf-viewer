package com.antigravity.pdfviewer.presentation.tools.split

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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

enum class SplitMode {
    BY_RANGE,
    EVERY_N_PAGES,
    EXTRACT_PAGES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(
    viewModel: PdfToolsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val toolState by viewModel.toolState.collectAsStateWithLifecycle()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var splitMode by remember { mutableStateOf(SplitMode.BY_RANGE) }
    var rangeInput by remember { mutableStateOf("1-5, 6-10") }
    var everyNPages by remember { mutableIntStateOf(5) }
    var extractPagesInput by remember { mutableStateOf("1, 3, 5") }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedUri = uri
    }

    LaunchedEffect(toolState.isSuccess) {
        if (toolState.isSuccess) {
            Toast.makeText(context, toolState.successMessage ?: "Split completed!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split PDF") },
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
                            text = if (selectedUri != null) "Ready to split" else "Choose a file to split",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) }) {
                        Text(if (selectedUri != null) "Change" else "Select PDF")
                    }
                }
            }

            // Mode Selector
            Text("Split Method", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = splitMode == SplitMode.BY_RANGE,
                    onClick = { splitMode = SplitMode.BY_RANGE },
                    label = { Text("By Range") }
                )
                FilterChip(
                    selected = splitMode == SplitMode.EVERY_N_PAGES,
                    onClick = { splitMode = SplitMode.EVERY_N_PAGES },
                    label = { Text("Every N Pages") }
                )
                FilterChip(
                    selected = splitMode == SplitMode.EXTRACT_PAGES,
                    onClick = { splitMode = SplitMode.EXTRACT_PAGES },
                    label = { Text("Extract Pages") }
                )
            }

            // Mode input
            when (splitMode) {
                SplitMode.BY_RANGE -> {
                    OutlinedTextField(
                        value = rangeInput,
                        onValueChange = { rangeInput = it },
                        label = { Text("Page Ranges (e.g., 1-5, 6-10)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SplitMode.EVERY_N_PAGES -> {
                    OutlinedTextField(
                        value = everyNPages.toString(),
                        onValueChange = { everyNPages = it.toIntOrNull() ?: 1 },
                        label = { Text("Split every N pages") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SplitMode.EXTRACT_PAGES -> {
                    OutlinedTextField(
                        value = extractPagesInput,
                        onValueChange = { extractPagesInput = it },
                        label = { Text("Selected Pages (e.g., 1, 3, 5, 8)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (toolState.isRunning) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(toolState.currentStep, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Button(
                onClick = {
                    val uri = selectedUri ?: return@Button
                    val outDir = File(context.cacheDir, "split_output_${System.currentTimeMillis()}").apply { mkdirs() }
                    when (splitMode) {
                        SplitMode.BY_RANGE -> {
                            val ranges = parseRanges(rangeInput)
                            viewModel.splitPdfByRanges(uri, ranges, outDir, "split")
                        }
                        SplitMode.EVERY_N_PAGES -> {
                            viewModel.splitPdfByEveryNPages(uri, everyNPages, outDir, "split")
                        }
                        SplitMode.EXTRACT_PAGES -> {
                            val pages = extractPagesInput.split(",").mapNotNull { it.trim().toIntOrNull() }
                            val ranges = pages.map { it..it }
                            viewModel.splitPdfByRanges(uri, ranges, outDir, "extracted")
                        }
                    }
                },
                enabled = selectedUri != null && !toolState.isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Process Split")
            }
        }
    }
}

private fun parseRanges(input: String): List<IntRange> {
    return input.split(",").mapNotNull { part ->
        val trimmed = part.trim()
        if (trimmed.contains("-")) {
            val tokens = trimmed.split("-")
            val start = tokens.getOrNull(0)?.trim()?.toIntOrNull()
            val end = tokens.getOrNull(1)?.trim()?.toIntOrNull()
            if (start != null && end != null && start <= end) start..end else null
        } else {
            val single = trimmed.toIntOrNull()
            if (single != null) single..single else null
        }
    }
}
