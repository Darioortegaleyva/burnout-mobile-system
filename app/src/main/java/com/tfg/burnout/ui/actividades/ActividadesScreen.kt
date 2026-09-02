package com.tfg.burnout.ui.actividades

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tfg.burnout.BurnoutApp
import com.tfg.burnout.domain.model.CategoriaCoping
import com.tfg.burnout.ui.theme.VerdeCalmado
import com.tfg.burnout.ui.theme.VerdeCalmadoClaro

/**
 * Pantalla de Actividades: la biblioteca de pautas de mitigación, accesible
 * bajo demanda (P3 — agencia del usuario). Las categorías sugeridas por el
 * motor para el momento actual aparecen primero con una insignia, pero todo
 * el catálogo es siempre consultable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActividadesScreen() {
    val app = LocalContext.current.applicationContext as BurnoutApp
    val vm: ActividadesViewModel = viewModel(factory = ActividadesViewModel.Factory(app.repository))
    val estado by vm.ui.collectAsStateWithLifecycle()

    // Igual que en Inicio: el ViewModel sobrevive al cambio de pestaña, así
    // que sin este refresco las categorías sugeridas seguirían ordenadas
    // según el índice que hubiera al abrir la aplicación, no el actual.
    LaunchedEffect(Unit) { vm.cargar() }

    Scaffold(topBar = { TopAppBar(title = { Text("Actividades") }) }) { padding ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    Text(
                        "Pequeñas acciones con respaldo científico. Explora a tu ritmo: " +
                        "no hay obligaciones, solo ideas.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                items(estado.bloques) { bloque -> TarjetaCategoria(bloque) }
            }
        }
    }
}

@Composable
private fun TarjetaCategoria(bloque: BloquePautas) {
    var abierta by remember { mutableStateOf(bloque.sugerido) }

    Card(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { abierta = !abierta },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(iconoDe(bloque.categoria), contentDescription = null, tint = VerdeCalmado)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(bloque.categoria.etiqueta, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall)
                    if (bloque.sugerido) {
                        Spacer(Modifier.height(4.dp))
                        AssistChip(
                            onClick = {}, enabled = false,
                            label = { Text("Sugerido para ti ahora") },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = VerdeCalmadoClaro,
                                disabledLabelColor = VerdeCalmado
                            )
                        )
                    }
                }
                Icon(
                    if (abierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (abierta) "Contraer" else "Expandir"
                )
            }
            if (abierta) {
                Spacer(Modifier.height(8.dp))
                bloque.pautas.forEach { p ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(p.titulo, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(p.descripcion, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun iconoDe(c: CategoriaCoping) = when (c) {
    CategoriaCoping.HIGIENE_SUENO -> Icons.Filled.Bedtime
    CategoriaCoping.MINDFULNESS_ACT -> Icons.Filled.SelfImprovement
    CategoriaCoping.REESTRUCTURACION -> Icons.Filled.Psychology
    CategoriaCoping.APOYO_SOCIAL -> Icons.Filled.Group
}
