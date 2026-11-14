package com.antigravity.pdfviewer.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antigravity.pdfviewer.domain.model.AppTheme

data class ThemeCardSpec(
    val theme: AppTheme,
    val title: String,
    val isPremium: Boolean,
    val previewBgColor: Color,
    val previewBarColor: Color,
    val previewPillColor: Color,
    val previewAccentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeThemeScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val themeSpecs = listOf(
        ThemeCardSpec(
            theme = AppTheme.LIGHT,
            title = "Xodo Light",
            isPremium = false,
            previewBgColor = Color(0xFFF2F4F7),
            previewBarColor = Color.White,
            previewPillColor = Color(0xFFE2E8F0),
            previewAccentColor = Color(0xFF007AFF)
        ),
        ThemeCardSpec(
            theme = AppTheme.DARK,
            title = "Xodo Dark",
            isPremium = false,
            previewBgColor = Color(0xFF14151B),
            previewBarColor = Color(0xFF1E2028),
            previewPillColor = Color(0xFF333644),
            previewAccentColor = Color(0xFF00B0FF)
        ),
        ThemeCardSpec(
            theme = AppTheme.OLED_ULTRA_DARK,
            title = "OLED Ultra Dark",
            isPremium = true,
            previewBgColor = Color(0xFF000000),
            previewBarColor = Color(0xFF0A0A0A),
            previewPillColor = Color(0xFF1E1E1E),
            previewAccentColor = Color(0xFF3B82F6)
        ),
        ThemeCardSpec(
            theme = AppTheme.PREMIUM_DARK,
            title = "Premium Dark",
            isPremium = true,
            previewBgColor = Color(0xFF18181B),
            previewBarColor = Color(0xFF27272A),
            previewPillColor = Color(0xFF3F3F46),
            previewAccentColor = Color(0xFFE4E4E7)
        ),
        ThemeCardSpec(
            theme = AppTheme.NIGHT_SEA,
            title = "Night Sea",
            isPremium = true,
            previewBgColor = Color(0xFF0A192F),
            previewBarColor = Color(0xFF112240),
            previewPillColor = Color(0xFF233554),
            previewAccentColor = Color(0xFF64FFDA)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Theme", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(themeSpecs, key = { it.theme.name }) { spec ->
                val isSelected = settings.theme == spec.theme
                ThemePreviewCard(
                    spec = spec,
                    isSelected = isSelected,
                    onClick = { viewModel.setTheme(spec.theme) }
                )
            }
        }
    }
}

@Composable
fun ThemePreviewCard(
    spec: ThemeCardSpec,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF00B0FF) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Title + Diamond badge + Radio Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = spec.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (spec.isPremium) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "Premium",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF00B0FF),
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Preview Mockup Box matching Image 2
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(spec.previewBgColor)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar Mockup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(spec.previewBarColor)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            tint = spec.previewPillColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(spec.previewPillColor)
                        )
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(spec.previewPillColor)
                        )
                    }

                    // Bottom Bar Mockup with Theme Accent
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(spec.previewBarColor)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(spec.previewAccentColor)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(spec.previewPillColor)
                        )
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(spec.previewAccentColor)
                        )
                    }
                }
            }
        }
    }
}
