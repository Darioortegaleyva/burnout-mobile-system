package com.tfg.burnout.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * PANTALLA DE CONSENTIMIENTO INFORMADO (RGPD arts. 7 y 13; CU-01).
 *
 * Se muestra UNA vez, antes de cualquier recogida de datos. Explica en
 * lenguaje llano qué se mide, con qué finalidad, y que los datos no abandonan
 * el dispositivo. El usuario debe aceptarla activamente para continuar
 * (consentimiento libre, específico, informado e inequívoco).
 */
@Composable
fun ConsentScreen(onAceptar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Antes de empezar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Bloque(
            "¿Qué mide esta aplicación?",
            "Tu tiempo de sueño, tu frecuencia cardíaca y su variabilidad (a través de " +
            "Health Connect, si lo autorizas), junto con las respuestas que tú mismo des " +
            "a un cuestionario validado sobre desgaste laboral."
        )
        Bloque(
            "¿Para qué?",
            "Para ayudarte a observar tu propio nivel de energía y proponerte pautas de " +
            "bienestar. Esta aplicación NO emite diagnósticos médicos ni psicológicos; si " +
            "detecta señales de riesgo, te orientará hacia profesionales cualificados."
        )
        Bloque(
            "¿Dónde se guardan tus datos?",
            "Únicamente en este dispositivo. La aplicación funciona sin servidor y sin " +
            "conexión: tus datos no se envían a nadie, ni a empresas ni a tu empleador. " +
            "Si la desinstalas, se eliminan. Puedes exportarlos cuando quieras desde la " +
            "pantalla de dispositivos."
        )
        Bloque(
            "Uso personal y voluntario",
            "Esta herramienta es para ti. No está diseñada para que una empresa supervise " +
            "a sus trabajadores, y su uso es siempre voluntario."
        )

        Spacer(Modifier.height(28.dp))
        Button(onClick = onAceptar, modifier = Modifier.fillMaxWidth()) {
            Text("He leído y acepto")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Podrás revisar esta información en cualquier momento.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun Bloque(titulo: String, cuerpo: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(cuerpo, style = MaterialTheme.typography.bodyMedium)
    }
}
