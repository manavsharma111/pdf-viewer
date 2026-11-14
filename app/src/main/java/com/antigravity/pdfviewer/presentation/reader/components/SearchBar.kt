package com.antigravity.pdfviewer.presentation.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.pdfviewer.domain.model.SearchState

@Composable
fun SearchBar(
    searchState: SearchState,
    onQueryChanged: (String, Boolean, Boolean) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember(searchState.query) { mutableStateOf(searchState.query) }
    var caseSensitive by remember(searchState.isCaseSensitive) { mutableStateOf(searchState.isCaseSensitive) }
    var wholeWord by remember(searchState.isWholeWord) { mutableStateOf(searchState.isWholeWord) }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onQueryChanged(it, caseSensitive, wholeWord)
                    },
                    placeholder = { Text("Search document...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                if (searchState.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                } else if (searchState.totalMatches > 0) {
                    Text(
                        text = "${searchState.currentMatchIndex + 1} / ${searchState.totalMatches}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (query.isNotBlank()) {
                    Text(
                        text = "0 / 0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                IconButton(
                    onClick = onPreviousMatch,
                    enabled = searchState.totalMatches > 0
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
                }

                IconButton(
                    onClick = onNextMatch,
                    enabled = searchState.totalMatches > 0
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close search")
                }
            }

            // Options: Case-sensitive & Whole word
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = caseSensitive,
                        onCheckedChange = {
                            caseSensitive = it
                            onQueryChanged(query, it, wholeWord)
                        }
                    )
                    Text("Case sensitive", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = wholeWord,
                        onCheckedChange = {
                            wholeWord = it
                            onQueryChanged(query, caseSensitive, it)
                        }
                    )
                    Text("Whole word", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
