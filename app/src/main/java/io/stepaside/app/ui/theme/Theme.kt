package io.stepaside.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    secondary = BrandGreen,
    tertiary = BrandGreen,
    background = BgPrimary,
    surface = BgSurface,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Error,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    secondary = BrandGreen,
    tertiary = BrandGreen,
)

/**
 * App theme. Dynamic color is intentionally disabled — StepAside has its own
 * brand identity (#39D353 green on near-black) that should not be overridden
 * by the user's wallpaper.
 */
@Composable
fun StepAsideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
