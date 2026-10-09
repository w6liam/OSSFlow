package app.notype.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandPurple = Color(0xFF5B2EE0)

@Composable
fun NoTypeTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (dark) {
        darkColorScheme().copy(
            primary = Color(0xFFD0BCFF),
            onPrimary = Color(0xFF381E72),
        )
    } else {
        lightColorScheme().copy(
            primary = BrandPurple,
            onPrimary = Color.White,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
