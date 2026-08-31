package com.tfg.burnout.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tfg.burnout.data.healthconnect.HealthConnectManager
import com.tfg.burnout.data.repository.BurnoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados del semáforo de sincronización (§6.4). Deliberadamente sin rojo. */
enum class EstadoSync { VERDE, AMBAR, GRIS }

data class DevicesUiState(
    val estado: EstadoSync = EstadoSync.GRIS,
    val mensajeEstado: String = "Comprobando...",
    val sincronizando: Boolean = false,
    /** Diagnóstico de la última lectura (cadena Zepp → Health Connect). */
    val fechaUltima: String? = null,
    val tstOk: Boolean = false,
    val rhrOk: Boolean = false,
    val rmssdOk: Boolean = false,
    /** Paquetes de las apps que escriben en Health Connect (Tarea 5). */
    val fuentes: List<String> = emptyList(),
    /** Edad declarada (opcional): personaliza el umbral de FC. */
    val edad: Int? = null
)

class DevicesViewModel(
    private val repository: BurnoutRepository,
    private val healthConnect: HealthConnectManager
) : ViewModel() {

    private val _ui = MutableStateFlow(DevicesUiState())
    val ui: StateFlow<DevicesUiState> = _ui.asStateFlow()

    init { comprobarEstado() }

    fun comprobarEstado() {
        viewModelScope.launch {
            val disponible = healthConnect.disponibilidad()
            val tienePermisos = runCatching { healthConnect.tienePermisos() }.getOrDefault(false)
            _ui.value = when {
                !tienePermisos -> DevicesUiState(
                    EstadoSync.GRIS, "Sin fuente conectada. Vincula tu wearable y concede permisos."
                )
                else -> DevicesUiState(
                    EstadoSync.VERDE, "Conectado. Recibiendo datos de salud."
                )
            }
            // Perfil físico opcional, si el usuario lo aportó.
            repository.obtenerUsuario()?.let { u ->
                _ui.value = _ui.value.copy(edad = u.edad)
            }

            // Fuentes reales detectadas (panel de configuración, Tarea 5).
            _ui.value = _ui.value.copy(fuentes = healthConnect.fuentesDetectadas())

            // Diagnóstico: ¿qué llegó realmente en la última lectura?
            val b = repository.ultimaBiometria()
            if (b != null) {
                val fecha = java.time.LocalDate.ofEpochDay(b.fechaEpochDay).toString()
                _ui.value = _ui.value.copy(
                    fechaUltima = fecha,
                    tstOk = b.tstMin != null,
                    rhrOk = b.rhrBpm != null,
                    rmssdOk = b.rmssdMs != null
                )
            }
        }
    }

    /** Botón "Forzar lectura ahora" (§6.4). */
    /** Lee varios días de golpe (tras insertar una semana simulada). */
    fun guardarPerfilFisico(edad: Int?) {
        viewModelScope.launch {
            repository.guardarPerfilFisico(edad)
            comprobarEstado()
        }
    }

    fun borrarPerfilFisico() {
        viewModelScope.launch {
            repository.borrarPerfilFisico()
            comprobarEstado()
        }
    }

    fun forzarLecturaSemana() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(sincronizando = true)
            runCatching { repository.sincronizarUltimosDias(7) }
            _ui.value = _ui.value.copy(sincronizando = false)
            comprobarEstado()
        }
    }

    fun forzarLectura() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(sincronizando = true)
            runCatching { repository.sincronizarBiometriaHoy() }
            _ui.value = _ui.value.copy(sincronizando = false)
            comprobarEstado()
        }
    }

    /**
     * Derecho de PORTABILIDAD (RGPD art. 20): genera el JSON con todos los
     * datos del usuario y lo entrega al callback para que la pantalla lance
     * el selector de compartir del sistema. La exportación la inicia siempre
     * el propio usuario.
     */
    fun exportarDatos(alTerminar: (String) -> Unit) {
        viewModelScope.launch {
            val json = runCatching { repository.exportarDatosJson() }
                .getOrDefault("{\"error\":\"No se pudieron exportar los datos\"}")
            alTerminar(json)
        }
    }

    class Factory(
        private val repository: BurnoutRepository,
        private val healthConnect: HealthConnectManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DevicesViewModel(repository, healthConnect) as T
    }
}
