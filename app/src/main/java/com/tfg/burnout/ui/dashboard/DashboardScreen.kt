package com.tfg.burnout.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tfg.burnout.BurnoutApp
import com.tfg.burnout.ui.theme.Ambar
import com.tfg.burnout.ui.theme.AmbarClaro
import com.tfg.burnout.ui.theme.LavandaClara
import com.tfg.burnout.ui.theme.TextoApagado
import androidx.compose.ui.res.stringResource
import com.tfg.burnout.R
import com.tfg.burnout.ui.theme.VerdeCalmado
import com.tfg.burnout.ui.theme.VerdeCalmadoClaro

/**
 * Pantalla 1 — Inicio (P2 revisado: comunicación cualitativa asimétrica).
 *
 * La pantalla NO muestra puntuaciones numéricas del estado del usuario.
 * Comunica una banda cualitativa con mensaje de apoyo, el reto del día y un
 * acceso directo a la biblioteca de Actividades. Ver la justificación de la
 * decisión en DashboardViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onIrAActividades: () -> Unit = {}) {
    val app = LocalContext.current.applicationContext as BurnoutApp
    val vm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(app.repository)
    )
    val estado by vm.uiState.collectAsStateWithLifecycle()

    // El ViewModel sobrevive al cambio de pestaña (la navegación inferior usa
    // saveState/restoreState), de modo que cargar solo en su init dejaba el
    // panel congelado: tras sincronizar en Dispositivos o completar una
    // evaluación en el Asistente, Inicio seguía mostrando la banda anterior
    // hasta reiniciar la aplicación. El composable sí se recompone al volver,
    // así que es aquí donde toca refrescar.
    LaunchedEffect(Unit) { vm.cargar() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            if (estado.cargando) {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // --- Tarjeta de estado cualitativo (sin números) ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorDeFondo(estado.banda)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(iconoDe(estado.banda), contentDescription = null,
                                tint = VerdeCalmado, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(estado.titulo, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(estado.mensaje, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- PROGRESO SEMANAL (§5.4) ---
                // Da al usuario una lectura de avance cada semana sin repetir
                // el cuestionario: el instrumento sigue siendo mensual, pero
                // el refuerzo es continuo.
                if (estado.objetivoSemana > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LavandaClara),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Tu semana", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Has cumplido " + estado.cumplimientosSemana +
                                    " de " + estado.objetivoSemana + " veces tus retos " +
                                    "en los últimos siete días.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    (estado.cumplimientosSemana.toFloat() /
                                        estado.objetivoSemana).coerceIn(0f, 1f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                when {
                                    estado.cumplimientosSemana == 0 ->
                                        "Empezar cuesta. Con marcar uno esta semana, ya es avance."
                                    estado.cumplimientosSemana >= estado.objetivoSemana / 2 ->
                                        "Buen ritmo. Lo que cuenta es la constancia, no la perfección."
                                    else ->
                                        "Vas sumando. No hace falta cumplirlos todos para que sirva."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoApagado
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // --- CU-03: retos elegidos por el usuario, con check diario ---
                if (estado.metas.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AmbarClaro),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Tus retos de esta semana", color = Ambar, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            estado.metas.forEach { meta ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = meta.cumplidaHoy,
                                        onCheckedChange = { vm.alternarMeta(meta.id) }
                                    )
                                    Text(meta.titulo, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (estado.metas.all { it.cumplidaHoy }) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Todo hecho por hoy. Date un respiro: te lo has ganado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VerdeCalmado
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // --- Reto del día (solo si aún no hay retos negociados) ---
                if (estado.metas.isEmpty()) Card(
                    colors = CardDefaults.cardColors(containerColor = AmbarClaro),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Reto de hoy", color = Ambar, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(estado.retoDelDia, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- Acceso a la biblioteca de Actividades (P3, agencia) ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = LavandaClara),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onIrAActividades
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Explora actividades", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Una biblioteca de pautas sencillas (sueño, relajación, " +
                            "organización, apoyo) para consultar cuando tú quieras.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// El fondo de la tarjeta de estado es el mismo verde calmado en todas las
// bandas: el estado del usuario nunca se "colorea" en negativo (P4).
private fun colorDeFondo(@Suppress("UNUSED_PARAMETER") b: BandaEstado) = VerdeCalmadoClaro

private fun iconoDe(b: BandaEstado) = when (b) {
    BandaEstado.BUEN_MOMENTO -> Icons.Filled.WbSunny
    BandaEstado.EN_CAMINO -> Icons.Filled.Whatshot
    else -> Icons.Filled.Spa
}
