package app.saydroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.saydroid.R

val Ink = Color(0xFF07080D)
val SurfaceInk = Color(0xFF141821)
val Hairline = Color(0xFF2A3142)
val Signal = Color(0xFF2EE6C7)
val SignalDeep = Color(0xFF0F766E)
val Paper = Color(0xFFF4F1EA)
val InkText = Color(0xFF12141C)

private val Outfit = FontFamily(
    Font(
        R.font.outfit,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

private val DarkColors = darkColorScheme(
    primary = Signal,
    onPrimary = Ink,
    primaryContainer = Color(0xFF134E48),
    onPrimaryContainer = Signal,
    inversePrimary = SignalDeep,
    secondary = Signal,
    onSecondary = Ink,
    secondaryContainer = Signal,
    onSecondaryContainer = Ink,
    tertiary = Signal,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF134E48),
    onTertiaryContainer = Signal,
    background = Ink,
    onBackground = Paper,
    surface = SurfaceInk,
    onSurface = Paper,
    surfaceVariant = Color(0xFF1C2230),
    onSurfaceVariant = Color(0xFFC9C3B8),
    surfaceTint = Signal,
    outline = Hairline,
    outlineVariant = Hairline,
    surfaceDim = Ink,
    surfaceBright = Hairline,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Color(0xFF0E121A),
    surfaceContainer = SurfaceInk,
    surfaceContainerHigh = Color(0xFF1C2230),
    surfaceContainerHighest = Color(0xFF232A3A),
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Color(0xFFFECDD3),
)

private val LightColors = lightColorScheme(
    primary = SignalDeep,
    onPrimary = Paper,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E48),
    inversePrimary = Signal,
    secondary = SignalDeep,
    onSecondary = Paper,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = SignalDeep,
    tertiary = SignalDeep,
    onTertiary = Paper,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = SignalDeep,
    background = Paper,
    onBackground = InkText,
    surface = Color(0xFFFFFCF7),
    onSurface = InkText,
    surfaceVariant = Color(0xFFE7E1D6),
    onSurfaceVariant = Color(0xFF3F3A33),
    surfaceTint = SignalDeep,
    outline = Color(0xFFD4CBBC),
    outlineVariant = Color(0xFFD4CBBC),
    surfaceDim = Color(0xFFE7E1D6),
    surfaceBright = Color(0xFFFFFCF7),
    surfaceContainerLowest = Color(0xFFFFFDF8),
    surfaceContainerLow = Paper,
    surfaceContainer = Color(0xFFFFFCF7),
    surfaceContainerHigh = Color(0xFFE7E1D6),
    surfaceContainerHighest = Color(0xFFDDD6CB),
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF4C0519),
)

@Composable
fun SaydroidTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val base = Typography()
    val branded = base.copy(
        titleLarge = base.titleLarge.copy(
            fontFamily = Outfit,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = Outfit,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = branded,
        content = content,
    )
}
