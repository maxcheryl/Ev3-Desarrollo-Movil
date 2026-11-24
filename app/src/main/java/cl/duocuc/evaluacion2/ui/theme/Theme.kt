package cl.duocuc.evaluacion2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MaxTealLight,
    secondary = MaxSlateLight,
    tertiary = MaxAmberLight,
    background = MaxBackgroundDark,
    surface = MaxBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = MaxTeal,
    secondary = MaxSlate,
    tertiary = MaxAmber,
    background = MaxBackgroundLight,
    surface = MaxBackgroundLight
)

/**
 * Colores y estilos de Agencia Atenas.
 * Se usan los mismos colores propios en modo claro y oscuro.
 */
@Composable
fun MaxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
