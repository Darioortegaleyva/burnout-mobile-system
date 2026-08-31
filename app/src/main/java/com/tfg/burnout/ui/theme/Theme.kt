package com.tfg.burnout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema Material 3 de la app. Se fuerza un esquema claro y sin rojo (P4).
 * Se evita el color dinámico de Android 12+ para no introducir tonos rojos
 * provenientes del fondo de pantalla del usuario.
 */
private val EsquemaClaro = lightColorScheme(
    primary = VerdeCalmado,
    onPrimary = Superficie,
    primaryContainer = VerdeCalmadoClaro,
    onPrimaryContainer = TextoPrincipal,
    secondary = Lavanda,
    secondaryContainer = LavandaClara,
    tertiary = Ambar,
    tertiaryContainer = AmbarClaro,
    background = FondoPantalla,
    onBackground = TextoPrincipal,
    surface = Superficie,
    onSurface = TextoPrincipal,
    outline = LineaSuave
)

@Composable
fun BurnoutTheme(content: @Composable () -> Unit) {
    // Aunque exista modo oscuro en el sistema, mantenemos el esquema controlado
    // para garantizar la ausencia de rojo y el tono calmado en todo momento.
    @Suppress("UNUSED_VARIABLE")
    val oscuro = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography(),
        content = content
    )
}
