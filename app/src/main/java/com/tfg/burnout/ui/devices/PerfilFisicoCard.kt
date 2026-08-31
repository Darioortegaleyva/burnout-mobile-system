package com.tfg.burnout.ui.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tfg.burnout.ui.theme.TextoApagado

/**
 * PERSONALIZACIÓN OPCIONAL DE LA LECTURA DEL PULSO (Tarea 6).
 *
 * Se solicita ÚNICAMENTE la edad. El estudio de viabilidad de esta
 * personalización concluyó que la edad aporta un ajuste defendible a través
 * de la reserva cardíaca —el margen entre la frecuencia máxima teórica y la
 * frecuencia en reposo propia—, mientras que el peso, la altura y el sexo
 * influyen sobre todo en el NIVEL basal de la frecuencia y no en su
 * reactividad, y ese nivel ya queda absorbido por la línea base individual
 * de cada usuario. Recogerlos habría supuesto pedir datos personales
 * adicionales sin contrapartida real, contrario al principio de minimización
 * (RGPD art. 5.1.c), de modo que se retiraron del formulario.
 *
 * Sigue siendo opcional y revocable: el sistema funciona igual sin la edad,
 * empleando el umbral de referencia.
 */
@Composable
fun PerfilFisicoCard(
    edadInicial: Int?,
    onGuardar: (Int?) -> Unit,
    onBorrar: () -> Unit,
) {
    var desplegado by remember { mutableStateOf(false) }
    var edad by remember { mutableStateOf(edadInicial?.toString() ?: "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Afinar la lectura de tu pulso", style = MaterialTheme.typography.titleSmall)
            Text(
                "Opcional. Tu pulso se compara siempre contigo mismo, así que la app " +
                "funciona sin este dato. Saber tu edad permite ajustar mejor cuándo " +
                "una subida de pulso es significativa para ti.",
                style = MaterialTheme.typography.bodySmall, color = TextoApagado
            )
            Spacer(Modifier.height(8.dp))

            if (!desplegado) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { desplegado = true }) {
                        Text(
                            if (edadInicial != null) "Cambiar mi edad ($edadInicial años)"
                            else "Indicar mi edad"
                        )
                    }
                    if (edadInicial != null) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onBorrar) { Text("Borrar") }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = edad,
                        onValueChange = { edad = it.filter(Char::isDigit).take(3) },
                        label = { Text("Edad") },
                        modifier = Modifier.width(140.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onGuardar(edad.toIntOrNull()); desplegado = false },
                        enabled = edad.toIntOrNull()?.let { it in 14..99 } == true
                    ) { Text("Guardar") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { desplegado = false }) { Text("Cancelar") }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Se guarda en tu móvil como el resto de tus datos.",
                    style = MaterialTheme.typography.bodySmall, color = TextoApagado
                )
            }
        }
    }
}
