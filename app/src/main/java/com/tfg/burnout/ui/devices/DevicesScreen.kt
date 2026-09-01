package com.tfg.burnout.ui.devices

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Watch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tfg.burnout.BurnoutApp
import com.tfg.burnout.data.healthconnect.PreferenciasDispositivos
import com.tfg.burnout.data.ia.AjustesIa
import com.tfg.burnout.data.ia.AlmacenModelo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tfg.burnout.ui.theme.Ambar
import androidx.compose.ui.res.stringResource
import com.tfg.burnout.R
import com.tfg.burnout.ui.theme.TextoApagado
import com.tfg.burnout.ui.theme.VerdeCalmado

/**
 * Pantalla 3 — Gestión de dispositivos (§6.4). Panel de observabilidad sobre
 * la cadena de sincronización con un semáforo de tres estados (sin rojo, P4)
 * y la solicitud de permisos de Health Connect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BurnoutApp
    val vm: DevicesViewModel = viewModel(
        factory = DevicesViewModel.Factory(app.repository, app.healthConnect)
    )
    val estado by vm.ui.collectAsStateWithLifecycle()

    // --- Conexión unificada (permisos + lectura) ---
    // Un único botón: si faltan permisos de Health Connect los solicita y, al
    // concederse, lanza la lectura; si ya están concedidos, sincroniza
    // directamente. Más legible que dos botones separados.
    val permisos = app.healthConnect.permisos
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val launcherPermisos = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { concedidos ->
        if (concedidos.isNotEmpty()) vm.forzarLectura() else vm.comprobarEstado()
    }

    val disponibilidad = app.healthConnect.disponibilidad()
    val healthConnectInstalado = disponibilidad == HealthConnectClient.SDK_AVAILABLE

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dispositivos") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Estado de la sincronización", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Panel del estado actual con su luz de semáforo
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LuzSemaforo(estado.estado)
                    Text(estado.mensajeEstado, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Aviso si Health Connect no está disponible en el dispositivo.
            if (!healthConnectInstalado) {
                Card(colors = CardDefaults.cardColors(containerColor = com.tfg.burnout.ui.theme.AmbarClaro)) {
                    Text(
                        "Health Connect no está disponible en este dispositivo. " +
                        "Instálalo desde Google Play (en Android 14+ viene integrado).",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Leyenda de los tres estados
            Text("Significado de los estados:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FilaLeyenda(VerdeCalmado, "Verde — datos recibidos en las últimas 24 h")
            FilaLeyenda(Ambar, "Ámbar — sin datos recientes; abre la app del reloj")
            FilaLeyenda(TextoApagado, "Gris — sin fuente conectada todavía")

            Spacer(Modifier.weight(1f))

            // Botón único: conecta (pide permisos si faltan) y sincroniza.
            Button(
                onClick = {
                    scope.launch {
                        if (app.healthConnect.tienePermisos()) vm.forzarLectura()
                        else launcherPermisos.launch(permisos)
                    }
                },
                enabled = !estado.sincronizando && healthConnectInstalado,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (estado.sincronizando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (estado.sincronizando) "Actualizando…" else "Conectar y actualizar datos")
            }

            // --- Diagnóstico de la última lectura (cadena Zepp → Health Connect) ---
            if (estado.fechaUltima != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Última lectura (" + estado.fechaUltima + ")",
                            style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        FilaDato("Sueño (TST)", estado.tstOk)
                        FilaDato("FC en reposo", estado.rhrOk)
                        FilaDato("Variabilidad (RMSSD)", estado.rmssdOk)
                        if (!estado.rmssdOk) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "No todas las aplicaciones de salud exportan todas las " +
                                "métricas. Si falta alguna, el índice se adapta y sigue " +
                                "funcionando con las disponibles.",
                                style = MaterialTheme.typography.bodySmall, color = TextoApagado
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // --- DISPOSITIVOS Y APLICACIONES CONECTADAS (Tarea 5) ---
            // Health Connect no expone el wearable en sí, sino la aplicación
            // que escribe sus datos. Se muestra con nombre legible y con las
            // métricas que aporta, que es la información útil para el usuario.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Dispositivos conectados", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    if (estado.fuentes.isEmpty()) {
                        Text(
                            "Todavía no se detecta ninguna aplicación de salud escribiendo " +
                            "datos. Vincula tu reloj con su app (Zepp, Samsung Health…) y " +
                            "concédele permiso para Health Connect.",
                            style = MaterialTheme.typography.bodySmall, color = TextoApagado
                        )
                    } else {
                        estado.fuentes.forEach { paquete ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Watch, contentDescription = null,
                                    tint = VerdeCalmado,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        PreferenciasDispositivos.nombreLegible(paquete),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        paquete,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextoApagado
                                    )
                                }
                                if (estado.fechaUltima != null) {
                                    Text(
                                        "activa",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VerdeCalmado
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // --- PANEL DE CONFIGURACIÓN DE FUENTES Y MÉTRICAS (Tarea 5) ---
            var leerSueno by remember { mutableStateOf(PreferenciasDispositivos.leerSueno(context)) }
            var leerFc by remember { mutableStateOf(PreferenciasDispositivos.leerFc(context)) }
            var leerHrv by remember { mutableStateOf(PreferenciasDispositivos.leerHrv(context)) }
            var prioritaria by remember {
                mutableStateOf(PreferenciasDispositivos.fuentePrioritaria(context))
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Configuración de datos", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Elige qué quieres compartir con la app. Puedes desactivar " +
                        "cualquier métrica y el sistema seguirá funcionando con el resto.",
                        style = MaterialTheme.typography.bodySmall, color = TextoApagado
                    )
                    Spacer(Modifier.height(8.dp))
                    FilaInterruptor("Sueño", leerSueno) {
                        leerSueno = it; PreferenciasDispositivos.setLeerSueno(context, it)
                    }
                    FilaInterruptor("Frecuencia cardíaca en reposo", leerFc) {
                        leerFc = it; PreferenciasDispositivos.setLeerFc(context, it)
                    }
                    FilaInterruptor("Variabilidad cardíaca", leerHrv) {
                        leerHrv = it; PreferenciasDispositivos.setLeerHrv(context, it)
                    }

                    if (estado.fuentes.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Fuente preferida", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Si varias aplicaciones escriben los mismos datos, se usará esta.",
                            style = MaterialTheme.typography.bodySmall, color = TextoApagado
                        )
                        Spacer(Modifier.height(6.dp))
                        estado.fuentes.forEach { paquete ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = prioritaria == paquete,
                                    onClick = {
                                        prioritaria = paquete
                                        PreferenciasDispositivos.setFuentePrioritaria(context, paquete)
                                    }
                                )
                                Text(
                                    PreferenciasDispositivos.nombreLegible(paquete),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = prioritaria == null,
                                onClick = {
                                    prioritaria = null
                                    PreferenciasDispositivos.setFuentePrioritaria(context, null)
                                }
                            )
                            Text("Sin preferencia", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // --- PERSONALIZACIÓN OPCIONAL DEL PULSO (Tarea 6) ---
            PerfilFisicoCard(
                edadInicial = estado.edad,
                onGuardar = { e -> vm.guardarPerfilFisico(e) },
                onBorrar = { vm.borrarPerfilFisico() }
            )
            Spacer(Modifier.height(12.dp))

            // --- IA local opcional (§2.3.6): el motor decide, el modelo redacta ---
            var modeloDisponible by remember { mutableStateOf(AlmacenModelo.disponible(context)) }
            var iaActivada by remember { mutableStateOf(AjustesIa.activada(context)) }
            var importando by remember { mutableStateOf(false) }
            // El aprovisionamiento desde assets ocurre en segundo plano al
            // arrancar; al entrar en esta pantalla, refrescamos el estado.
            LaunchedEffect(Unit) {
                modeloDisponible = AlmacenModelo.disponible(context)
                iaActivada = AjustesIa.activada(context)
            }
            val selectorModelo = rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    importando = true
                    scope.launch {
                        withContext(Dispatchers.IO) { AlmacenModelo.importar(context, uri) }
                        modeloDisponible = AlmacenModelo.disponible(context)
                        importando = false
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Asistente con IA local (opcional)",
                                style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (modeloDisponible)
                                    "Modelo integrado (" + AlmacenModelo.tamanoMb(context) + " MB). Todo se ejecuta en tu móvil, sin conexión."
                                else
                                    "Esta compilación no incluye el modelo. Puedes importarlo manualmente (.task).",
                                style = MaterialTheme.typography.bodySmall, color = TextoApagado
                            )
                        }
                        Switch(
                            checked = iaActivada && modeloDisponible,
                            enabled = modeloDisponible,
                            onCheckedChange = {
                                iaActivada = it
                                AjustesIa.setActivada(context, it)
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { selectorModelo.launch(arrayOf("application/octet-stream", "*/*")) },
                        enabled = !importando,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (importando) "Importando…" else "Importar o reemplazar modelo (.task)") }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Solo varía la redacción de mensajes ya validados; nunca las preguntas " +
                        "del cuestionario ni el flujo de ayuda profesional.",
                        style = MaterialTheme.typography.bodySmall, color = TextoApagado
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // --- MODO DESARROLLO: probar sin wearable físico ---
            // Inserta una noche simulada en Health Connect y relanza la
            // lectura: valida el flujo completo (HC → motor → índice → UI)
            // exactamente igual que si los datos viniesen del reloj.
            if (com.tfg.burnout.BuildConfig.DEBUG) {
                var insertando by remember { mutableStateOf(false) }
                var resultadoPrueba by remember { mutableStateOf<String?>(null) }
                val launcherEscrituraSemana = rememberLauncherForActivityResult(
                    contract = PermissionController.createRequestPermissionResultContract()
                ) { _ ->
                    scope.launch {
                        insertando = true
                        val ok = app.healthConnect.insertarSemanaDePrueba()
                        if (ok) vm.forzarLecturaSemana()
                        resultadoPrueba = if (ok)
                            "Semana simulada insertada: seis noches normales y una mala. Leyendo…"
                        else "No se pudo insertar (¿permisos de escritura?)"
                        insertando = false
                    }
                }
                val launcherEscritura = rememberLauncherForActivityResult(
                    contract = PermissionController.createRequestPermissionResultContract()
                ) { _ ->
                    scope.launch {
                        insertando = true
                        val ok = app.healthConnect.insertarNocheDePrueba()
                        if (ok) vm.forzarLectura()
                        resultadoPrueba = if (ok) "Noche simulada insertada. Leyendo…"
                                          else "No se pudo insertar (¿permisos de escritura?)"
                        insertando = false
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Modo desarrollo", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Genera una noche de prueba (sueño 23:40–07:15, FC 58, RMSSD 42) " +
                            "en Health Connect para probar todo el flujo sin el reloj.",
                            style = MaterialTheme.typography.bodySmall, color = TextoApagado
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                launcherEscritura.launch(
                                    app.healthConnect.permisos + app.healthConnect.permisosEscrituraPrueba
                                )
                            },
                            enabled = !insertando && healthConnectInstalado,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (insertando) "Insertando…" else "Generar datos de prueba") }
                        Spacer(Modifier.height(8.dp))
                        // Con una sola noche la línea base no existe todavía
                        // (mínimo tres días), así que la biometría no puede
                        // influir en el índice. Esta opción inserta una semana
                        // completa: seis noches normales que forman la
                        // referencia y una última claramente peor.
                        OutlinedButton(
                            onClick = {
                                launcherEscrituraSemana.launch(
                                    app.healthConnect.permisos + app.healthConnect.permisosEscrituraPrueba
                                )
                            },
                            enabled = !insertando && healthConnectInstalado,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Generar semana de prueba (con mala noche)") }
                        Spacer(Modifier.height(8.dp))
                        // Vuelve al estado de primer arranque: útil para
                        // ensayar la demo completa sin desinstalar la app.
                        OutlinedButton(
                            onClick = { scope.launch { app.repository.borrarTodosLosDatos() } },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Restablecer la app (borrar mis datos)") }
                        resultadoPrueba?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = TextoApagado)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            TextButton(onClick = { /* abrir centro de ayuda */ }, modifier = Modifier.fillMaxWidth()) {
                Text("¿Problemas? Abre el centro de ayuda")
            }

            // Derecho de portabilidad (RGPD art. 20): exporta los datos del
            // usuario en JSON y abre el selector de compartir del sistema.
            // El nombre se lee del recurso aquí, en ámbito componible, porque
            // stringResource no puede invocarse dentro del onClick.
            val nombreApp = stringResource(R.string.app_name)
            TextButton(
                onClick = {
                    vm.exportarDatos { json ->
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Mis datos — $nombreApp")
                            putExtra(android.content.Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(intent, "Exportar mis datos")
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Exportar mis datos (RGPD)")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LuzSemaforo(estado: EstadoSync) {
    val color = when (estado) {
        EstadoSync.VERDE -> VerdeCalmado
        EstadoSync.AMBAR -> Ambar
        EstadoSync.GRIS -> TextoApagado
    }
    Box(Modifier.size(22.dp).clip(CircleShape).background(color))
}

@Composable
private fun FilaLeyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Text(texto, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FilaDato(nombre: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (ok) "✓" else "—",
            color = if (ok) VerdeCalmado else TextoApagado,
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(nombre, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(if (ok) "recibido" else "sin datos",
            color = if (ok) VerdeCalmado else TextoApagado,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FilaInterruptor(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onCambio)
    }
}
