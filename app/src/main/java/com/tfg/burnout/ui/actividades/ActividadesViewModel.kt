package com.tfg.burnout.ui.actividades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tfg.burnout.data.repository.BurnoutRepository
import com.tfg.burnout.domain.engine.CatalogoPautas
import com.tfg.burnout.domain.engine.GestorCoping
import com.tfg.burnout.domain.model.CategoriaCoping
import com.tfg.burnout.domain.model.PautaCoping
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Pantalla de Actividades: biblioteca de pautas consultable bajo demanda
 * (§2.3.2, P3 — agencia del usuario). Las categorías que el motor considera
 * prioritarias para el escenario actual del usuario se muestran primero, con
 * una insignia "Sugerido para ti"; el resto queda igualmente accesible.
 */
data class BloquePautas(
    val categoria: CategoriaCoping,
    val pautas: List<PautaCoping>,
    val sugerido: Boolean,
)

data class ActividadesUiState(
    val cargando: Boolean = true,
    val bloques: List<BloquePautas> = emptyList(),
)

class ActividadesViewModel(
    private val repository: BurnoutRepository,
    private val gestor: GestorCoping = GestorCoping(),
) : ViewModel() {

    private val _ui = MutableStateFlow(ActividadesUiState())
    val ui: StateFlow<ActividadesUiState> = _ui.asStateFlow()

    // La carga la dispara la pantalla en cada entrada a la pestaña; ver el
    // LaunchedEffect de ActividadesScreen.

    fun cargar() {
        viewModelScope.launch {
            // Categorías sugeridas según el escenario actual (si hay evaluación).
            val indice = repository.calcularIndiceActual()
            val sugeridas: List<CategoriaCoping> = if (indice != null) {
                gestor.categoriasPriorizadas(
                    gestor.clasificar(indice.scoreCesqt, indice.cargaBiometrica),
                    indice.componenteDominante
                )
            } else emptyList()

            val orden = sugeridas + CategoriaCoping.entries.filter { it !in sugeridas }
            _ui.value = ActividadesUiState(
                cargando = false,
                bloques = orden.map { cat ->
                    BloquePautas(cat, CatalogoPautas.de(cat), sugerido = cat in sugeridas)
                },
            )
        }
    }

    class Factory(private val repository: BurnoutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ActividadesViewModel(repository) as T
    }
}
