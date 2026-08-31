package com.tfg.burnout.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * PERFIL DE CONTEXTO LABORAL (§5.5) — se muestra UNA vez, tras el
 * consentimiento. Tres preguntas inspiradas en factores del FPSICO (INSST):
 * carga de trabajo percibida, autonomía y apoyo social. Sirven para modular
 * las pautas (p. ej., priorizar límites y apoyo cuando hay mucha carga y poca
 * autonomía) y para desculpabilizar con contexto; nunca para diagnosticar.
 */
@Composable
fun ContextoScreen(onGuardar: (carga: Int, autonomia: Int, apoyo: Int) -> Unit) {
    var carga by remember { mutableStateOf<Int?>(null) }
    var autonomia by remember { mutableStateOf<Int?>(null) }
    var apoyo by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Cuéntame un poco de tu trabajo", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tres preguntas rápidas, solo esta vez. Me ayudan a proponerte pautas " +
            "que encajen con tu situación real. No hay respuestas buenas ni malas.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        PreguntaNivel("¿Cómo dirías que es tu carga de trabajo habitual?",
            listOf("Llevadera", "Media", "Muy alta"), carga) { carga = it }
        PreguntaNivel("¿Cuánto margen tienes para decidir cómo y cuándo haces tus tareas?",
            listOf("Poco", "Algo", "Bastante"), autonomia) { autonomia = it }
        PreguntaNivel("¿Sientes que puedes apoyarte en compañeros o superiores?",
            listOf("Poco", "A veces", "Sí, bastante"), apoyo) { apoyo = it }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onGuardar(carga!!, autonomia!!, apoyo!!) },
            enabled = carga != null && autonomia != null && apoyo != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Empezar") }
        Spacer(Modifier.height(8.dp))
        Text("Podrás seguir aunque tu situación cambie: esto solo orienta las sugerencias.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun PreguntaNivel(
    pregunta: String,
    etiquetas: List<String>,   // índice 0 -> nivel 1 (Baja) ... índice 2 -> nivel 3 (Alta)
    seleccion: Int?,
    onSeleccion: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(pregunta, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            etiquetas.forEachIndexed { i, etiqueta ->
                val nivel = i + 1
                FilterChip(
                    selected = seleccion == nivel,
                    onClick = { onSeleccion(nivel) },
                    label = { Text(etiqueta) }
                )
            }
        }
    }
}
