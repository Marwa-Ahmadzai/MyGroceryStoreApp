package week4.st991647132.mygrocerystoreapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF000CFF),
    secondary = Color(0xFF000CFF),
    tertiary = Color(0xFF000CFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF000CFF),
    secondary = Color(0xFF000CFF),
    tertiary = Color(0xFF000CFF)
)

@Composable
fun MyGroceryStoreAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}