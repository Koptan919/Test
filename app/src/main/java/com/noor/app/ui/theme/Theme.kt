package com.noor.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noor.app.R
import com.noor.app.data.prefs.ThemeMode

object NoorColors {
    val TitaniumBlack = Color(0xFF0E0F11)
    val Titanium = Color(0xFF17191D)
    val TitaniumRaised = Color(0xFF202329)
    val TitaniumEdge = Color(0xFF2C3037)
    val Burgundy = Color(0xFF6B1E2E)
    val BurgundyBright = Color(0xFF8E2A40)
    val BurgundyDeep = Color(0xFF3E0D1A)
    val Gold = Color(0xFFD4AF6A)
    val GoldSoft = Color(0xFFE8D3A2)
    val GoldMuted = Color(0xFF8A6D35)
    val Ivory = Color(0xFFF6F0E4)
    val Parchment = Color(0xFFEDE3D0)
    val Ink = Color(0xFF1B1416)
}

private val DarkScheme = darkColorScheme(
    primary = NoorColors.Gold,
    onPrimary = NoorColors.TitaniumBlack,
    primaryContainer = NoorColors.Burgundy,
    onPrimaryContainer = NoorColors.GoldSoft,
    secondary = NoorColors.BurgundyBright,
    onSecondary = NoorColors.Ivory,
    secondaryContainer = NoorColors.BurgundyDeep,
    onSecondaryContainer = NoorColors.GoldSoft,
    tertiary = NoorColors.GoldSoft,
    onTertiary = NoorColors.TitaniumBlack,
    background = NoorColors.TitaniumBlack,
    onBackground = Color(0xFFEDE6DA),
    surface = NoorColors.Titanium,
    onSurface = Color(0xFFEDE6DA),
    surfaceVariant = NoorColors.TitaniumRaised,
    onSurfaceVariant = Color(0xFFB7AFA2),
    surfaceContainer = NoorColors.Titanium,
    surfaceContainerHigh = NoorColors.TitaniumRaised,
    surfaceContainerHighest = NoorColors.TitaniumEdge,
    outline = Color(0x66D4AF6A),
    outlineVariant = Color(0x22FFFFFF),
    error = Color(0xFFE57373),
)

private val LightScheme = lightColorScheme(
    primary = NoorColors.Burgundy,
    onPrimary = NoorColors.Ivory,
    primaryContainer = Color(0xFFF1DCE0),
    onPrimaryContainer = NoorColors.BurgundyDeep,
    secondary = NoorColors.GoldMuted,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E6C8),
    onSecondaryContainer = Color(0xFF3B2C10),
    tertiary = NoorColors.GoldMuted,
    onTertiary = Color.White,
    background = NoorColors.Ivory,
    onBackground = NoorColors.Ink,
    surface = Color(0xFFFFFBF4),
    onSurface = NoorColors.Ink,
    surfaceVariant = NoorColors.Parchment,
    onSurfaceVariant = Color(0xFF5E5249),
    surfaceContainer = Color(0xFFFBF5EA),
    surfaceContainerHigh = NoorColors.Parchment,
    surfaceContainerHighest = Color(0xFFE5D8C0),
    outline = Color(0x888A6D35),
    outlineVariant = Color(0x22000000),
)

/** Extra brand values not covered by Material's ColorScheme. */
@Immutable
data class NoorPalette(
    val isDark: Boolean,
    val accent: Color,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassBorderStrong: Color,
    val glassBorderSoft: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val heroAccent: Color,
    val glowA: Color,
    val glowB: Color,
)

private val DarkPalette = NoorPalette(
    isDark = true,
    accent = NoorColors.Gold,
    glassFillTop = Color(0x17FFFFFF),
    glassFillBottom = Color(0x08FFFFFF),
    glassBorderStrong = Color(0x66D4AF6A),
    glassBorderSoft = Color(0x14FFFFFF),
    heroStart = Color(0xFF7A2236),
    heroEnd = Color(0xFF2A0912),
    onHero = NoorColors.Ivory,
    heroAccent = NoorColors.GoldSoft,
    glowA = Color(0x556B1E2E),
    glowB = Color(0x33D4AF6A),
)

private val LightPalette = NoorPalette(
    isDark = false,
    accent = NoorColors.GoldMuted,
    glassFillTop = Color(0xCCFFFFFF),
    glassFillBottom = Color(0x99FFFBF4),
    glassBorderStrong = Color(0x998A6D35),
    glassBorderSoft = Color(0x22000000),
    heroStart = Color(0xFF7A2236),
    heroEnd = Color(0xFF3E0D1A),
    onHero = NoorColors.Ivory,
    heroAccent = NoorColors.GoldSoft,
    glowA = Color(0x336B1E2E),
    glowB = Color(0x44D4AF6A),
)

val LocalNoorPalette = staticCompositionLocalOf { DarkPalette }

object NoorFonts {
    val Ui = FontFamily(
        Font(R.font.cairo_regular, FontWeight.Normal),
        Font(R.font.cairo_semibold, FontWeight.SemiBold),
        Font(R.font.cairo_bold, FontWeight.Bold),
    )
    val Quran = FontFamily(Font(R.font.amiri_quran, FontWeight.Normal))
    val Naskh = FontFamily(
        Font(R.font.amiri_regular, FontWeight.Normal),
        Font(R.font.amiri_bold, FontWeight.Bold),
    )
}

private fun typography(scale: Float): Typography {
    fun style(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) = TextStyle(
        fontFamily = NoorFonts.Ui,
        fontWeight = weight,
        fontSize = (size * scale).sp,
        lineHeight = (line * scale).sp,
        letterSpacing = spacing.sp,
    )
    return Typography(
        displayLarge = style(52, FontWeight.Bold, 64),
        displayMedium = style(42, FontWeight.Bold, 52),
        displaySmall = style(34, FontWeight.Bold, 44),
        headlineLarge = style(30, FontWeight.Bold, 40),
        headlineMedium = style(26, FontWeight.SemiBold, 36),
        headlineSmall = style(22, FontWeight.SemiBold, 32),
        titleLarge = style(20, FontWeight.SemiBold, 30),
        titleMedium = style(17, FontWeight.SemiBold, 26),
        titleSmall = style(15, FontWeight.SemiBold, 22),
        bodyLarge = style(16, FontWeight.Normal, 26),
        bodyMedium = style(14, FontWeight.Normal, 22),
        bodySmall = style(12, FontWeight.Normal, 18),
        labelLarge = style(15, FontWeight.SemiBold, 22),
        labelMedium = style(13, FontWeight.SemiBold, 18),
        labelSmall = style(11, FontWeight.SemiBold, 16, 0.3),
    )
}

private val NoorShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun NoorTheme(
    darkTheme: Boolean = true,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val scheme: ColorScheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(LocalNoorPalette provides if (darkTheme) DarkPalette else LightPalette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography(textScale),
            shapes = NoorShapes,
            content = content,
        )
    }
}
