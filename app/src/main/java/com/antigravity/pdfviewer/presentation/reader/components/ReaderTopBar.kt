package com.antigravity.pdfviewer.presentation.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antigravity.pdfviewer.domain.model.ReadingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    title: String,
    readingMode: ReadingMode,
    isBookmarked: Boolean,
    isAnnotationMode: Boolean,
    useSingleLineToolbar: Boolean = true,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onAnnotationClick: () -> Unit,
    onReadingModeSelected: (ReadingMode) -> Unit,
    onRotateClick: () -> Unit,
    onPageManagementClick: () -> Unit,
    onMetadataClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReadingModeMenu by remember { mutableStateOf(false) }

    if (useSingleLineToolbar) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }

                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onAnnotationClick) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Annotate",
                        tint = if (isAnnotationMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Box {
                    IconButton(onClick = { showReadingModeMenu = true }) {
                        val modeIcon = when (readingMode) {
                            ReadingMode.SINGLE_PAGE -> Icons.Default.ViewDay
                            ReadingMode.CONTINUOUS_VERTICAL -> Icons.Default.ViewAgenda
                            ReadingMode.CONTINUOUS_HORIZONTAL -> Icons.Default.ViewCarousel
                            ReadingMode.DOUBLE_PAGE -> Icons.Default.ViewWeek
                        }
                        Icon(modeIcon, contentDescription = "Reading Mode")
                    }

                    DropdownMenu(
                        expanded = showReadingModeMenu,
                        onDismissRequest = { showReadingModeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Single Page") },
                            leadingIcon = { Icon(Icons.Default.ViewDay, contentDescription = null) },
                            onClick = {
                                onReadingModeSelected(ReadingMode.SINGLE_PAGE)
                                showReadingModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Continuous Vertical") },
                            leadingIcon = { Icon(Icons.Default.ViewAgenda, contentDescription = null) },
                            onClick = {
                                onReadingModeSelected(ReadingMode.CONTINUOUS_VERTICAL)
                                showReadingModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Continuous Horizontal") },
                            leadingIcon = { Icon(Icons.Default.ViewCarousel, contentDescription = null) },
                            onClick = {
                                onReadingModeSelected(ReadingMode.CONTINUOUS_HORIZONTAL)
                                showReadingModeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Double Page") },
                            leadingIcon = { Icon(Icons.Default.ViewWeek, contentDescription = null) },
                            onClick = {
                                onReadingModeSelected(ReadingMode.DOUBLE_PAGE)
                                showReadingModeMenu = false
                            }
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rotate Page (90° CW)") },
                            leadingIcon = { Icon(Icons.Default.RotateRight, contentDescription = null) },
                            onClick = {
                                onRotateClick()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Page Management") },
                            leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            onClick = {
                                onPageManagementClick()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Document Info") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                onMetadataClick()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share PDF") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                onShareClick()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save / Export") },
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                            onClick = {
                                onSaveClick()
                                showMenu = false
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            modifier = modifier
        )
    } else {
        // Two-line Toolbar layout (as configured in General Settings)
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 4.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                // Line 1: Back + Title + Overflow Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Document Info") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    onMetadataClick()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share PDF") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    onShareClick()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }

                // Line 2: Quick Tools Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }

                    IconButton(onClick = onBookmarkClick) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onAnnotationClick) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Annotate",
                            tint = if (isAnnotationMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onRotateClick) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                    }

                    IconButton(onClick = onPageManagementClick) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Pages")
                    }

                    IconButton(onClick = onSaveClick) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            }
        }
    }
}
