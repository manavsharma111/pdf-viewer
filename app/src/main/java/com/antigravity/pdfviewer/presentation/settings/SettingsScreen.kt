package com.antigravity.pdfviewer.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antigravity.pdfviewer.domain.model.CachePreset
import com.antigravity.pdfviewer.domain.model.NightReadingMode
import com.antigravity.pdfviewer.domain.model.ReadingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToChangeTheme: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showReadingModeDialog by remember { mutableStateOf(false) }
    var showNightModeDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }

    // Reading Mode Dialog
    if (showReadingModeDialog) {
        AlertDialog(
            onDismissRequest = { showReadingModeDialog = false },
            title = { Text("Default Reading Mode") },
            text = {
                Column {
                    ReadingMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDefaultReadingMode(mode)
                                    showReadingModeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.defaultReadingMode == mode,
                                onClick = {
                                    viewModel.setDefaultReadingMode(mode)
                                    showReadingModeDialog = false
                                }
                            )
                            Text(mode.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Night Reading Mode Dialog
    if (showNightModeDialog) {
        AlertDialog(
            onDismissRequest = { showNightModeDialog = false },
            title = { Text("Night Reading Filter") },
            text = {
                Column {
                    NightReadingMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setNightReadingMode(mode)
                                    showNightModeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.nightReadingMode == mode,
                                onClick = {
                                    viewModel.setNightReadingMode(mode)
                                    showNightModeDialog = false
                                }
                            )
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Cache Preset Dialog
    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("Memory Cache Size") },
            text = {
                Column {
                    CachePreset.values().forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCachePreset(preset)
                                    showCacheDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.cacheSizePreset == preset,
                                onClick = {
                                    viewModel.setCachePreset(preset)
                                    showCacheDialog = false
                                }
                            )
                            Text(preset.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToChangeTheme) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "Themes",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Change Theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToChangeTheme() }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Change Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Choose from a handcrafted selection of themes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 2. Use Single Line Toolbar
            GeneralSettingSwitchItem(
                title = "Use Single Line Toolbar",
                subtitle = "When enabled, the toolbar will be single line. Otherwise the toolbar will be two lines.",
                checked = settings.useSingleLineToolbar,
                onCheckedChange = { viewModel.setUseSingleLineToolbar(it) }
            )

            // 3. Full Screen Mode
            GeneralSettingSwitchItem(
                title = "Full Screen Mode",
                subtitle = "When enabled, the system bars will be hidden when viewing a document.",
                checked = settings.fullScreenMode,
                onCheckedChange = { viewModel.setFullScreenMode(it) }
            )

            // 4. Screen Sleep Lock
            GeneralSettingSwitchItem(
                title = "Screen Sleep Lock",
                subtitle = "When enabled, the screen will stay on as long as you are viewing a document.",
                checked = settings.screenSleepLock,
                onCheckedChange = { viewModel.setScreenSleepLock(it) }
            )

            // 5. Remember Last Page
            GeneralSettingSwitchItem(
                title = "Remember Last Page",
                subtitle = "When enabled, documents opened from the Recent List are reopened on the last page they were on.",
                checked = settings.rememberLastPage,
                onCheckedChange = { viewModel.setRememberLastPage(it) }
            )

            // 6. Auto Save Warning
            GeneralSettingSwitchItem(
                title = "Auto Save Warning",
                subtitle = "When enabled, a warning will be shown to allow making copy of original file that is being edited.",
                checked = settings.autoSaveWarning,
                onCheckedChange = { viewModel.setAutoSaveWarning(it) }
            )

            // 7. Right-to-Left Support
            GeneralSettingSwitchItem(
                title = "Right-to-Left Support",
                subtitle = "When enabled, the viewer's Page Display dialog includes an option to enable right-to-left document support.",
                checked = settings.rightToLeftSupport,
                onCheckedChange = { viewModel.setRightToLeftSupport(it) }
            )

            // 8. Enable JavaScript
            GeneralSettingSwitchItem(
                title = "Enable JavaScript",
                subtitle = "When enabled, JavaScript actions embedded in PDF files will be executed.",
                checked = settings.enableJavaScript,
                onCheckedChange = { viewModel.setEnableJavaScript(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Reading Preferences Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Reading & Display",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            GeneralSettingClickableItem(
                title = "Default Reading Mode",
                subtitle = settings.defaultReadingMode.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                onClick = { showReadingModeDialog = true }
            )

            GeneralSettingClickableItem(
                title = "Night Reading Filter",
                subtitle = settings.nightReadingMode.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = { showNightModeDialog = true }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Performance Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Performance & Cache",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            GeneralSettingClickableItem(
                title = "Bitmap Cache Size",
                subtitle = settings.cacheSizePreset.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = { showCacheDialog = true }
            )

            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Adjacent Page Prefetch: ${settings.prefetchPages} pages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = settings.prefetchPages.toFloat(),
                    onValueChange = { viewModel.setPrefetchPages(it.toInt()) },
                    valueRange = 0f..5f,
                    steps = 4
                )
            }
        }
    }
}

@Composable
fun GeneralSettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00B0FF),
                uncheckedThumbColor = Color(0xFF9E9E9E),
                uncheckedTrackColor = Color(0xFF424242)
            ),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun GeneralSettingClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
