package com.tfg.burnout.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tfg.burnout.ui.actividades.ActividadesScreen
import com.tfg.burnout.ui.chatbot.ChatbotScreen
import com.tfg.burnout.ui.dashboard.DashboardScreen
import com.tfg.burnout.ui.devices.DevicesScreen

/** Rutas de navegación. */
sealed class Ruta(val ruta: String, val etiqueta: String, val icono: ImageVector) {
    data object Dashboard : Ruta("dashboard", "Inicio", Icons.Filled.Home)
    data object Chatbot : Ruta("chatbot", "Asistente", Icons.Filled.Chat)
    data object Actividades : Ruta("actividades", "Actividades", Icons.Filled.SelfImprovement)
    data object Devices : Ruta("devices", "Dispositivos", Icons.Filled.Watch)
}

@Composable
fun AppNavigation(abrirEnChat: Boolean = false) {
    val navController = rememberNavController()

    // Aterrizaje desde la notificación de reevaluación (Tarea 3): el usuario
    // llega directamente al asistente, donde ya le espera la invitación.
    LaunchedEffect(abrirEnChat) {
        if (abrirEnChat) {
            navController.navigate(Ruta.Chatbot.ruta) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
            }
        }
    }
    val items = listOf(Ruta.Dashboard, Ruta.Chatbot, Ruta.Actividades, Ruta.Devices)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val destinoActual = navBackStackEntry?.destination
                items.forEach { ruta ->
                    val seleccionado = destinoActual?.hierarchy?.any { it.route == ruta.ruta } == true
                    NavigationBarItem(
                        selected = seleccionado,
                        onClick = {
                            navController.navigate(ruta.ruta) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(ruta.icono, contentDescription = ruta.etiqueta) },
                        label = { Text(ruta.etiqueta) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // El NavHost DEBE respetar el espacio de la barra de pestañas: sin
        // este padding, el contenido inferior de cada pantalla (los chips de
        // respuesta del chat, los botones de Dispositivos) queda oculto
        // debajo de la barra y resulta inalcanzable.
        NavHost(
            navController = navController,
            startDestination = Ruta.Dashboard.ruta,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Ruta.Dashboard.ruta) {
                DashboardScreen(onIrAActividades = {
                    navController.navigate(Ruta.Actividades.ruta) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(Ruta.Chatbot.ruta) { ChatbotScreen() }
            composable(Ruta.Actividades.ruta) { ActividadesScreen() }
            composable(Ruta.Devices.ruta) { DevicesScreen() }
        }
        // innerPadding se aplica dentro de cada pantalla según convenga.
        @Suppress("UNUSED_EXPRESSION") innerPadding
    }
}
