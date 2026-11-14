package com.antigravity.pdfviewer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.antigravity.pdfviewer.domain.model.AppTheme

// 1. Xodo Light
private val XodoLightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    secondary = Color(0xFF0288D1),
    onSecondary = Color.White,
    tertiary = Color(0xFF10B981),
    background = Color(0xFFF2F4F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5E7EB),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF4B5563)
)

// 2. Xodo Dark
private val XodoDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00B0FF),
    onPrimary = Color.Black,
    secondary = Color(0xFF38BDF8),
    onSecondary = Color.Black,
    tertiary = Color(0xFF34D399),
    background = Color(0xFF121318),
    surface = Color(0xFF1E2028),
    surfaceVariant = Color(0xFF2C2F3C),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF94A3B8)
)

// 3. OLED Ultra Dark (True pitch black for AMOLED screens)
private val OledUltraDarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    secondary = Color(0xFF60A5FA),
    onSecondary = Color.Black,
    tertiary = Color(0xFF10B981),
    background = Color(0xFF000000),
    surface = Color(0xFF0C0C0E),
    surfaceVariant = Color(0xFF18181B),
    onBackground = Color.White,
    onSurface = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF9CA3AF)
)

// 4. Premium Dark (Charcoal Zinc)
private val PremiumDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF4F4F5),
    onPrimary = Color(0xFF18181B),
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color.Black,
    tertiary = Color(0xFFE4E4E7),
    background = Color(0xFF18181B),
    surface = Color(0xFF27272A),
    surfaceVariant = Color(0xFF3F3F46),
    onBackground = Color(0xFFFAFAFA),
    onSurface = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFFA1A1AA)
)

// 5. Night Sea (Deep oceanic navy & teal)
private val NightSeaColorScheme = darkColorScheme(
    primary = Color(0xFF64FFDA),
    onPrimary = Color(0xFF0A192F),
    secondary = Color(0xFF57CBFF),
    onSecondary = Color(0xFF0A192F),
    tertiary = Color(0xFFCCD6F6),
    background = Color(0xFF0A192F),
    surface = Color(0xFF112240),
    surfaceVariant = Color(0xFF233554),
    onBackground = Color(0xFFCCD6F6),
    onSurface = Color(0xFFE6F1FF),
    onSurfaceVariant = Color(0xFF8892B0)
)

@Composable
fun PDFViewerTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()

    val colorScheme = when (appTheme) {
        AppTheme.SYSTEM -> if (systemInDark) XodoDarkColorScheme else XodoLightColorScheme
        AppTheme.LIGHT -> XodoLightColorScheme
        AppTheme.DARK -> XodoDarkColorScheme
        AppTheme.OLED_ULTRA_DARK -> OledUltraDarkColorScheme
        AppTheme.PREMIUM_DARK -> PremiumDarkColorScheme
        AppTheme.NIGHT_SEA -> NightSeaColorScheme
    }

    val isDark = when (appTheme) {
        AppTheme.SYSTEM -> systemInDark
        AppTheme.LIGHT -> false
        else -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
