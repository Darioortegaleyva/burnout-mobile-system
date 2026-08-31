package com.tfg.burnout.domain.model

/**
 * Snapshot biométrico de una jornada, tal como llega de Health Connect
 * tras consolidarse cada mañana (§2.2.6, flujo 1).
 *
 * @param rmssdMs  RMSSD nocturno en milisegundos (variabilidad cardíaca).
 * @param tstMin   Tiempo Total de Sueño en minutos.
 * @param rhrBpm   Frecuencia cardíaca en reposo (lpm).
 */
data class LecturaBiometrica(
    val fechaEpochDay: Long,
    val rmssdMs: Double?,
    val tstMin: Double?,
    val rhrBpm: Double?
)

/**
 * Línea base individual del usuario: medias móviles de las últimas semanas
 * frente a las que se calculan las desviaciones Δ (§2.2.5).
 */
data class LineaBase(
    val rmssdMedio: Double,
    val tstMedioMin: Double,
    /** FC en reposo habitual de la persona (Tarea 6). */
    val rhrMedio: Double = 0.0
)

/**
 * Resultado del cálculo del Índice de Riesgo Multimodal.
 *
 * @param r valor del índice, normalizado a [0,1] (0 = sin riesgo).
 * @param energia inversión gamificada E = 100·(1−R) que se muestra en el
 *        Dashboard (§6.2).
 * @param componenteDominante qué término pesa más, para que el Gestor de
 *        Coping priorice el bloque de pautas adecuado.
 */
data class IndiceRiesgo(
    val r: Double,
    val energia: Int,
    val componenteDominante: Componente,
    /** Score psicométrico normalizado [0,1] usado en el cálculo. */
    val scoreCesqt: Double = 0.0,
    /** Carga biométrica agregada [0,1] (media de ΔRMSSD y ΔTST), o null sin datos. */
    val cargaBiometrica: Double? = null
) {
    enum class Componente { PSICOMETRICO, HRV, SUENO }
}

/**
 * Perfil de contexto laboral (§5.5): tres factores inspirados en el FPSICO
 * del INSST, recogidos una sola vez en el onboarding. Escala 1..3
 * (Baja/Media/Alta). Modula las pautas; no diagnostica.
 */
data class PerfilContexto(
    val carga: Int,
    val autonomia: Int,
    val apoyo: Int
) {
    companion object { const val BAJA = 1; const val MEDIA = 2; const val ALTA = 3 }
}
