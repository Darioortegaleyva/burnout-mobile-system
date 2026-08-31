package com.tfg.burnout.ui.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tfg.burnout.BurnoutApp
import com.tfg.burnout.ui.theme.LavandaClara
import com.tfg.burnout.ui.theme.VerdeCalmadoClaro

/**
 * Pantalla 2 — Interfaz conversacional (§6.3). Formato chat con respuestas
 * tappables (quick-reply chips), avatar no antropomórfico y predominio de la
 * respuesta cerrada.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChatbotScreen() {
    val app = LocalContext.current.applicationContext as BurnoutApp
    val vm: ChatbotViewModel = viewModel(
        factory = ChatbotViewModel.Factory(
            app.repository, app.gestorCoping, app.moduloEtico,
            reformular = { texto ->
                // IA local opcional: solo si el usuario la activó y hay modelo.
                if (com.tfg.burnout.data.ia.AjustesIa.activada(app) && app.reformulador.disponible())
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        app.reformulador.reformular(texto)
                    }
                else null
            },
            // BOT B: asistente documental. Se le pasa el reformulador solo si
            // el usuario tiene la IA activa; sin él, responde con el texto
            // documental íntegro, que siempre es seguro.
            contexto = app,
            asistente = com.tfg.burnout.data.rag.AsistenteRag(
                if (com.tfg.burnout.data.ia.AjustesIa.activada(app)) app.reformulador else null
            )
        )
    )
    val estado by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Asistente") }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // Barra de progreso durante la administración del CESQT (§6.3).
            estado.progresoCesqt?.let { (actual, total) ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        "Evaluación · pregunta $actual de $total",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { actual.toFloat() / total },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // AUTO-DESPLAZAMIENTO.
            //
            // Cada vez que entra un mensaje nuevo —o aparece el indicador de
            // que el asistente está consultando— la lista baja sola hasta el
            // final. Sin esto, en una conversación larga como el cuestionario
            // el usuario tendría que arrastrar a mano tras cada respuesta para
            // ver la pregunta siguiente.
            val estadoLista = rememberLazyListState()
            LaunchedEffect(estado.mensajes.size, estado.pensando) {
                if (estado.mensajes.isNotEmpty()) {
                    estadoLista.animateScrollToItem(estado.mensajes.lastIndex)
                }
            }

            LazyColumn(
                state = estadoLista,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(estado.mensajes, key = { it.id }) { msg ->
                    BurbujaChat(texto = msg.texto, esBot = msg.esBot)
                }
            }

            // Quick-reply chips
            if (estado.chips.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    estado.chips.forEach { chip ->
                        AssistChip(
                            onClick = { vm.responder(chip) },
                            label = { Text(chip) }
                        )
                    }
                }
            }

            // ATAJO DE DESARROLLO: fuerza una nueva administración del
            // cuestionario sin esperar al ciclo de cuatro semanas (§2.2.6).
            // Imprescindible para demostrar el flujo completo; NO existe en
            // la compilación de release.
            if (com.tfg.burnout.BuildConfig.DEBUG &&
                estado.fase != FaseChat.CESQT && estado.fase != FaseChat.DERIVACION
            ) {
                TextButton(
                    onClick = { vm.iniciarCesqt() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                ) {
                    Text("Repetir evaluación (modo desarrollo)")
                }
            }

            // Indicador de que el asistente documental está consultando.
            if (estado.pensando) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Consultando…", style = MaterialTheme.typography.labelMedium)
                }
            }

            // CAMPO DE TEXTO — solo en el BOT B (asistente documental).
            // Durante el cuestionario (BOT A) y la derivación permanece
            // oculto: ahí la respuesta debe ser cerrada por diseño (§6.3).
            if (estado.permiteTextoLibre) {
                var entrada by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = entrada,
                        onValueChange = { entrada = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe tu pregunta…") },
                        maxLines = 3,
                        enabled = !estado.pensando
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { vm.enviarTexto(entrada); entrada = "" },
                        enabled = entrada.isNotBlank() && !estado.pensando
                    ) { Text("Enviar") }
                }
            }
        }
    }
}

@Composable
private fun BurbujaChat(texto: String, esBot: Boolean) {
    val color = if (esBot) LavandaClara else VerdeCalmadoClaro
    val alineacion = if (esBot) Alignment.Start else Alignment.End
    Column(Modifier.fillMaxWidth(), horizontalAlignment = alineacion) {
        Surface(
            color = color,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Text(
                texto,
                modifier = Modifier.padding(12.dp),
                textAlign = if (esBot) TextAlign.Start else TextAlign.End
            )
        }
    }
}
