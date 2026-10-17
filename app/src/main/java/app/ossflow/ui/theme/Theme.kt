package app.ossflow.ui.theme

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
import app.ossflow.R

val Ink = Color(0xFF07080D)
val SurfaceInk = Color(0xFF141821)
val Hairline = Color(0xFF2A3142)
val Paper = Color(0xFFF4F1EA)
val InkText = Color(0xFF12141C)

internal val OutfitBrand = FontFamily(
    Font(
        R.font.outfit,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.outfit,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.outfit,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

private val DarkColors = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    primaryContainer = Color(0xFF1C2230),
    onPrimaryContainer = Paper,
    inversePrimary = InkText,
    secondary = Paper,
    onSecondary = Ink,
    secondaryContainer = Paper,
    onSecondaryContainer = Ink,
    tertiary = Paper,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF1C2230),
    onTertiaryContainer = Paper,
    background = Ink,
    onBackground = Paper,
    surface = SurfaceInk,
    onSurface = Paper,
    surfaceVariant = Color(0xFF1C2230),
    onSurfaceVariant = Color(0xFFC9C3B8),
    surfaceTint = Paper,
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
    primary = InkText,
    onPrimary = Paper,
    primaryContainer = Color(0xFFE7E1D6),
    onPrimaryContainer = InkText,
    inversePrimary = Paper,
    secondary = InkText,
    onSecondary = Paper,
    secondaryContainer = Color(0xFFE7E1D6),
    onSecondaryContainer = InkText,
    tertiary = InkText,
    onTertiary = Paper,
    tertiaryContainer = Color(0xFFE7E1D6),
    onTertiaryContainer = InkText,
    background = Paper,
    onBackground = InkText,
    surface = Color(0xFFFFFCF7),
    onSurface = InkText,
    surfaceVariant = Color(0xFFE7E1D6),
    onSurfaceVariant = Color(0xFF3F3A33),
    surfaceTint = InkText,
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
fun OSSFlowTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val base = Typography()
    val branded = base.copy(
        titleLarge = base.titleLarge.copy(
            fontFamily = OutfitBrand,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = OutfitBrand,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = branded,
        content = content,
    )
}
