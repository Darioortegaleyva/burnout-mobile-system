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

/**
 * Antigüedad máxima, en días, para considerar «reciente» una lectura.
 *
 * La Tabla 8 habla de las últimas 24 horas, pero la biometría se almacena
 * consolidada por jornada (`fechaEpochDay`), no con marca de tiempo, así que
 * la comprobación se hace con la granularidad de que se dispone: vale la
 * lectura de hoy y la de ayer. Es además lo coherente con el ciclo del
 * sistema, porque el trabajador nocturno consolida a las 03:00 la noche
 * anterior (§2.2.6): a media mañana, la lectura buena está fechada ayer.
 */
private const val DIAS_LECTURA_RECIENTE = 1L

/**
 * REGLA DEL SEMÁFORO (Tabla 8). Se decide con los DATOS, no con los permisos.
 *
 *  · GRIS  — sin fuente conectada: no hay permisos que permitan leer nada.
 *  · VERDE — hay una lectura dentro de la ventana reciente.
 *  · ÁMBAR — la fuente está conectada pero no ha traído datos recientes,
 *            sea porque no hay ninguna lectura o porque la que hay es vieja.
 *
 * Se extrae como función pura para poder verificarla sin instanciar el
 * ViewModel, y para que los tres estados queden documentados en un solo
 * sitio. Antes el color salía únicamente de si había permisos, de modo que
 * el verde podía convivir con «sin datos» en las tres métricas y el estado
 * ÁMBAR era inalcanzable pese a estar declarado, pintado y documentado.
 */
internal fun estadoDeSincronizacion(
    tienePermisos: Boolean,
    fechaUltimaEpochDay: Long?,
    hoyEpochDay: Long
): EstadoSync = when {
    !tienePermisos -> EstadoSync.GRIS
    fechaUltimaEpochDay == null -> EstadoSync.AMBAR
    hoyEpochDay - fechaUltimaEpochDay <= DIAS_LECTURA_RECIENTE -> EstadoSync.VERDE
    else -> EstadoSync.AMBAR
}

/** Mensaje que acompaña a cada estado, con su «Acción sugerida» (Tabla 8). */
internal fun mensajeDe(estado: EstadoSync): String = when (estado) {
    EstadoSync.GRIS ->
        "Sin fuente conectada. Vincula tu wearable y concede permisos."
    EstadoSync.AMBAR ->
        "Conectado, pero sin datos recientes. Abre la app de tu reloj o pulsa " +
            "«Conectar y actualizar datos»."
    EstadoSync.VERDE ->
        "Conectado. Recibiendo datos de salud."
}

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

            // Se recoge TODO antes de publicar el estado. Si se fijase primero
            // el color y luego se encadenasen copy(), la pantalla mostraría un
            // verde momentáneo que pasaría a ámbar al llegar la biometría.
            val usuario = repository.obtenerUsuario()
            val fuentes = healthConnect.fuentesDetectadas()
            val b = repository.ultimaBiometria()

            val estado = estadoDeSincronizacion(
                tienePermisos = tienePermisos,
                fechaUltimaEpochDay = b?.fechaEpochDay,
                hoyEpochDay = java.time.LocalDate.now().toEpochDay()
            )

            _ui.value = DevicesUiState(
                estado = estado,
                mensajeEstado = mensajeDe(estado),
                fechaUltima = b?.let { java.time.LocalDate.ofEpochDay(it.fechaEpochDay).toString() },
                tstOk = b?.tstMin != null,
                rhrOk = b?.rhrBpm != null,
                rmssdOk = b?.rmssdMs != null,
                fuentes = fuentes,
                edad = usuario?.edad
            )
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
