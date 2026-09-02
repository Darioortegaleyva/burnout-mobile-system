package com.tfg.burnout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tfg.burnout.ui.navigation.AppNavigation
import com.tfg.burnout.ui.onboarding.ConsentScreen
import com.tfg.burnout.ui.onboarding.ContextoScreen
import com.tfg.burnout.ui.theme.BurnoutTheme
import kotlinx.coroutines.launch

/**
 * Única Activity de la aplicación (patrón single-activity recomendado por
 * Google para apps Compose). Toda la navegación ocurre dentro de Compose.
 *
 * El árbol de UI está condicionado por el CONSENTIMIENTO INFORMADO (RGPD
 * art. 7): hasta que el usuario no lo acepta, no se muestra la app ni se
 * recoge ningún dato.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BurnoutTheme {
                RootScreen(abrirEnChat = intent?.getBooleanExtra(
                        com.tfg.burnout.work.NotificadorCuestionario.EXTRA_ABRIR_CHAT, false
                    ) == true)
            }
        }
    }
}

@Composable
private fun RootScreen(abrirEnChat: Boolean = false) {
    val app = LocalContext.current.applicationContext as BurnoutApp
    val scope = rememberCoroutineScope()
    val usuario by app.repository.observarUsuario().collectAsState(initial = null)

    when {
        usuario == null -> {
            // La base de datos aún está inicializándose (asset seeding).
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        !usuario!!.consentimientoAceptado -> {
            ConsentScreen(onAceptar = {
                scope.launch { app.repository.aceptarConsentimiento() }
            })
        }
        usuario!!.cargaPercibida == null -> {
            // Perfil de contexto laboral (§5.5): una sola vez, tras el
            // consentimiento; modula las pautas sin diagnosticar.
            ContextoScreen(onGuardar = { c, a, ap ->
                scope.launch { app.repository.guardarPerfilContexto(c, a, ap) }
            })
        }
        else -> {
            // El permiso de notificaciones se pide AQUÍ, ya aceptado el
            // consentimiento. Estaba antes al principio de RootScreen, fuera
            // del when, de modo que el diálogo del sistema se superponía a la
            // pantalla de consentimiento: se pedía permiso para avisar al
            // usuario antes de que este hubiera aceptado nada, justo lo que la
            // propia pantalla promete que no ocurrirá (RGPD art. 7).
            PedirPermisoNotificaciones()
            AppNavigation(abrirEnChat = abrirEnChat)
        }
    }
}

/**
 * PERMISO DE NOTIFICACIONES (Android 13+, Tarea 2).
 *
 * Sin solicitarlo en tiempo de ejecución, declararlo en el manifest no basta
 * y el recordatorio de reevaluación no llegaría nunca. Se pide una sola vez y
 * su denegación no bloquea nada: la aplicación sigue funcionando, solo que
 * sin aviso.
 */
@Composable
private fun PedirPermisoNotificaciones() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permisoNotificaciones = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* concedido o no, se continúa igual */ }
        LaunchedEffect(Unit) {
            permisoNotificaciones.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
