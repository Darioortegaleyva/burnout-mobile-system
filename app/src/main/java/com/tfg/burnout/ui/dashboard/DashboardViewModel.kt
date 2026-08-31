package com.tfg.burnout.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tfg.burnout.data.local.entity.MetaEntity
import com.tfg.burnout.data.repository.BurnoutRepository
import com.tfg.burnout.domain.model.IndiceRiesgo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Estado de UI del Dashboard (P2 revisado).
 *
 * DECISIÓN DE DISEÑO — comunicación cualitativa asimétrica:
 * la pantalla de inicio NO muestra nunca la puntuación numérica del índice.
 * Mostrar "34 de energía" a alguien que ya se siente mal refuerza el malestar
 * (efecto iatrogénico, §2.3.3). En su lugar, el estado se traduce a una banda
 * cualitativa con un mensaje siempre orientado a la acción y al apoyo:
 * lo positivo se hace explícito; lo difícil se acompaña, no se cuantifica.
 * El valor numérico sigue existiendo internamente para el motor (§2.2.5).
 */
enum class BandaEstado { SIN_EVALUACION, BUEN_MOMENTO, EN_CAMINO, DIA_DE_CUIDARSE }

/** Reto negociado en el chat (CU-02) y su estado de hoy (CU-03). */
data class MetaUi(val id: Long, val titulo: String, val cumplidaHoy: Boolean)

data class DashboardUiState(
    val cargando: Boolean = true,
    val banda: BandaEstado = BandaEstado.SIN_EVALUACION,
    val titulo: String = "",
    val mensaje: String = "",
    val retoDelDia: String = "",
    val componenteDominante: IndiceRiesgo.Componente? = null,
    val metas: List<MetaUi> = emptyList(),
    /** Retos cumplidos en los últimos 7 días (progreso semanal). */
    val cumplimientosSemana: Int = 0,
    /** Objetivo semanal: nº de retos × 7 días. */
    val objetivoSemana: Int = 0,
)

class DashboardViewModel(
    private val repository: BurnoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var cacheMetas: List<MetaEntity> = emptyList()

    init {
        cargar()
        // CU-03: los retos elegidos en el chat, con su check diario.
        viewModelScope.launch {
            repository.observarMetasActivas().collect { lista ->
                cacheMetas = lista
                val hoy = "\"" + java.time.LocalDate.now() + "\""

                // PROGRESO SEMANAL (§5.4): se obtiene de lo que el usuario ya
                // ha ido marcando, sin necesidad de volver a preguntarle nada.
                val ultimos7 = (0L..6L).map {
                    "\"" + java.time.LocalDate.now().minusDays(it) + "\""
                }
                val cumplidos = lista.sumOf { meta ->
                    ultimos7.count { dia -> meta.cumplimientosJson.contains(dia) }
                }

                _uiState.value = _uiState.value.copy(
                    metas = lista.map {
                        MetaUi(it.id, it.titulo, it.cumplimientosJson.contains(hoy))
                    },
                    cumplimientosSemana = cumplidos,
                    objetivoSemana = lista.size * 7
                )
            }
        }
    }

    /** Marca/desmarca el cumplimiento de hoy; el Flow refresca la UI. */
    fun alternarMeta(id: Long) {
        val meta = cacheMetas.find { it.id == id } ?: return
        viewModelScope.launch { repository.alternarCumplimientoHoy(meta) }
    }

    fun cargar() {
        viewModelScope.launch {
            val indice = repository.calcularIndiceActual()
            _uiState.value = if (indice == null) {
                DashboardUiState(
                    cargando = false,
                    banda = BandaEstado.SIN_EVALUACION,
                    titulo = "Bienvenido",
                    mensaje = "Cuando quieras, pásate por el chat y hacemos juntos tu primera evaluación. Sin prisa.",
                    retoDelDia = "Completa tu evaluación inicial en el chat",
                )
            } else {
                val banda = when {
                    indice.energia >= 65 -> BandaEstado.BUEN_MOMENTO
                    indice.energia >= 40 -> BandaEstado.EN_CAMINO
                    else -> BandaEstado.DIA_DE_CUIDARSE
                }
                DashboardUiState(
                    cargando = false,
                    banda = banda,
                    titulo = tituloDe(banda),
                    mensaje = mensajeDe(banda),
                    retoDelDia = retoSegun(indice.componenteDominante),
                    componenteDominante = indice.componenteDominante,
                )
            }
        }
    }

    private fun tituloDe(b: BandaEstado): String = when (b) {
        BandaEstado.BUEN_MOMENTO -> "Buen momento"
        BandaEstado.EN_CAMINO -> "Vas haciendo camino"
        BandaEstado.DIA_DE_CUIDARSE -> "Hoy toca cuidarse"
        BandaEstado.SIN_EVALUACION -> "Bienvenido"
    }

    private fun mensajeDe(b: BandaEstado): String = when (b) {
        BandaEstado.BUEN_MOMENTO ->
            "Tu descanso y tus respuestas apuntan a que estás en una etapa estable. Sigue con lo que te funciona."
        BandaEstado.EN_CAMINO ->
            "Hay cosas que están yendo bien. Los pequeños pasos de cada día son los que más cuentan."
        BandaEstado.DIA_DE_CUIDARSE ->
            "Las temporadas exigentes existen y no dicen nada malo de ti. Hoy hemos preparado algo sencillo para ayudarte a recargar."
        BandaEstado.SIN_EVALUACION ->
            "Cuando quieras, hacemos juntos tu primera evaluación."
    }

    private fun retoSegun(c: IndiceRiesgo.Componente?): String = when (c) {
        IndiceRiesgo.Componente.SUENO -> "Apaga pantallas 30 min antes de dormir"
        IndiceRiesgo.Componente.HRV -> "Haz 5 minutos de respiración pausada"
        IndiceRiesgo.Componente.PSICOMETRICO -> "Comparte cómo te sientes con alguien de confianza"
        null -> "Empieza por tu evaluación inicial"
    }

    /** Factory para inyección manual del repositorio. */
    class Factory(private val repository: BurnoutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(repository) as T
    }
}
