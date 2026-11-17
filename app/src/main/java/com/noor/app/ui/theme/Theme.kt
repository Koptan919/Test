package com.noor.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noor.app.R
import com.noor.app.data.prefs.ThemeMode
import com.noor.core.calendar.Season

/** Brand palette. */
object NoorColors {
    val TitaniumBlack = Color(0xFF101114)
    val Titanium = Color(0xFF17181C)
    val TitaniumRaised = Color(0xFF1F2026)
    val TitaniumEdge = Color(0xFF2A2B32)
    val Burgundy = Color(0xFF531C32)
    val BurgundyDeep = Color(0xFF2A0E1A)
    val BurgundyBright = Color(0xFF7A2B4A)
    val Champagne = Color(0xFFD8B878)
    val ChampagneSoft = Color(0xFFE9D6AC)
    val ChampagneInk = Color(0xFF6E5527)
    val Ivory = Color(0xFFF4F0E8)
    val IvoryRaised = Color(0xFFFBF8F2)
    val MutedGray = Color(0xFFA7A7AC)
    val Ink = Color(0xFF1A1619)
}

private val DarkScheme = darkColorScheme(
    primary = NoorColors.Champagne,
    onPrimary = NoorColors.TitaniumBlack,
    primaryContainer = NoorColors.Burgundy,
    onPrimaryContainer = NoorColors.ChampagneSoft,
    secondary = NoorColors.BurgundyBright,
    onSecondary = NoorColors.Ivory,
    secondaryContainer = NoorColors.BurgundyDeep,
    onSecondaryContainer = NoorColors.ChampagneSoft,
    tertiary = NoorColors.ChampagneSoft,
    onTertiary = NoorColors.TitaniumBlack,
    background = NoorColors.TitaniumBlack,
    onBackground = NoorColors.Ivory,
    surface = NoorColors.Titanium,
    onSurface = NoorColors.Ivory,
    surfaceVariant = NoorColors.TitaniumRaised,
    onSurfaceVariant = NoorColors.MutedGray,
    surfaceContainerLowest = NoorColors.TitaniumBlack,
    surfaceContainerLow = NoorColors.Titanium,
    surfaceContainer = NoorColors.Titanium,
    surfaceContainerHigh = NoorColors.TitaniumRaised,
    surfaceContainerHighest = NoorColors.TitaniumEdge,
    outline = Color(0x80D8B878),
    outlineVariant = Color(0x1FFFFFFF),
    error = Color(0xFFF2A0A8),
)

private val LightScheme = lightColorScheme(
    primary = NoorColors.Burgundy,
    onPrimary = NoorColors.Ivory,
    primaryContainer = Color(0xFFF0DDE4),
    onPrimaryContainer = NoorColors.BurgundyDeep,
    secondary = NoorColors.ChampagneInk,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E6CB),
    onSecondaryContainer = Color(0xFF3A2C10),
    tertiary = NoorColors.ChampagneInk,
    onTertiary = Color.White,
    background = NoorColors.Ivory,
    onBackground = NoorColors.Ink,
    surface = NoorColors.IvoryRaised,
    onSurface = NoorColors.Ink,
    surfaceVariant = Color(0xFFEAE4D8),
    onSurfaceVariant = Color(0xFF55525A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = NoorColors.IvoryRaised,
    surfaceContainer = NoorColors.IvoryRaised,
    surfaceContainerHigh = Color(0xFFEFEADF),
    surfaceContainerHighest = Color(0xFFE5DECF),
    outline = Color(0x996E5527),
    outlineVariant = Color(0x1F000000),
    error = Color(0xFFB3261E),
)

/** Brand values not covered by Material's ColorScheme, including the occasion atmosphere. */
@Immutable
data class NoorPalette(
    val isDark: Boolean,
    val season: Season,
    /** Gold accent used for highlights and selected states (meets contrast on the current background). */
    val accent: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val heroAccent: Color,
    val ambient: Color,
)

private fun palette(dark: Boolean, season: Season): NoorPalette {
    val base = NoorPalette(
        isDark = dark,
        season = season,
        accent = if (dark) NoorColors.Champagne else NoorColors.ChampagneInk,
        glassFill = if (dark) Color(0x0FFFFFFF) else Color(0xB3FFFFFF),
        glassBorder = if (dark) Color(0x1FFFFFFF) else Color(0x14000000),
        heroStart = NoorColors.Burgundy,
        heroEnd = NoorColors.BurgundyDeep,
        onHero = NoorColors.Ivory,
        heroAccent = NoorColors.Champagne,
        ambient = if (dark) Color(0x40531C32) else Color(0x1F531C32),
    )
    return when (season) {
        Season.NONE -> base
        // Night sky with champagne crescent tones.
        Season.RAMADAN -> base.copy(heroStart = Color(0xFF1E2448), heroEnd = Color(0xFF101114), ambient = if (dark) Color(0x332A3466) else Color(0x1A2A3466))
        // Respectful, subdued charcoal with silver accents.
        Season.MUHARRAM -> base.copy(
            accent = if (dark) Color(0xFFC9C9CE) else Color(0xFF4A4A50),
            heroStart = Color(0xFF26262B), heroEnd = Color(0xFF0B0B0D),
            heroAccent = Color(0xFFC9C9CE), ambient = if (dark) Color(0x22000000) else Color(0x14000000),
        )
        Season.ARBAEEN -> base.copy(heroStart = Color(0xFF3B1020), heroEnd = Color(0xFF101114))
    }
}

val LocalNoorPalette = staticCompositionLocalOf { palette(true, Season.NONE) }

/** True when the user (in Noor or system-wide) prefers reduced motion. */
val LocalReduceMotion = staticCompositionLocalOf { false }

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
        displayLarge = style(54, FontWeight.Bold, 64),
        displayMedium = style(44, FontWeight.Bold, 54),
        displaySmall = style(36, FontWeight.Bold, 46),
        headlineLarge = style(30, FontWeight.Bold, 40),
        headlineMedium = style(26, FontWeight.SemiBold, 36),
        headlineSmall = style(22, FontWeight.SemiBold, 32),
        titleLarge = style(20, FontWeight.SemiBold, 30),
        titleMedium = style(17, FontWeight.SemiBold, 26),
        titleSmall = style(15, FontWeight.SemiBold, 22),
        bodyLarge = style(17, FontWeight.Normal, 28),
        bodyMedium = style(15, FontWeight.Normal, 24),
        bodySmall = style(13, FontWeight.Normal, 20),
        labelLarge = style(15, FontWeight.SemiBold, 22),
        labelMedium = style(13, FontWeight.SemiBold, 18),
        labelSmall = style(12, FontWeight.SemiBold, 16, 0.2),
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
    season: Season = Season.NONE,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme: ColorScheme = if (darkTheme) DarkScheme else LightScheme
    val noorPalette = remember(darkTheme, season) { palette(darkTheme, season) }
    val type = remember(textScale) { typography(textScale) }
    CompositionLocalProvider(
        LocalNoorPalette provides noorPalette,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = type, shapes = NoorShapes, content = content)
    }
}
