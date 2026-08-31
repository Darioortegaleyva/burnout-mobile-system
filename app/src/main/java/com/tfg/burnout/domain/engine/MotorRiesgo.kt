package com.tfg.burnout.domain.engine

import com.tfg.burnout.domain.model.IndiceRiesgo
import com.tfg.burnout.domain.model.LecturaBiometrica
import com.tfg.burnout.domain.model.LineaBase
import kotlin.math.abs

/**
 * MOTOR DE RIESGO — núcleo algorítmico del sistema (§2.2.5, §4.3).
 *
 * Implementa la ecuación de combinación lineal ponderada:
 *
 *      R = ω1 · Score_CESQT + ω2 · ΔRMSSD + ω3 · ΔTST
 *
 * donde:
 *   - Score_CESQT  : puntuación psicométrica normalizada a [0,1].
 *   - ΔRMSSD       : desviación de la variabilidad cardíaca respecto a la
 *                    línea base individual, normalizada a [0,1] (más caída,
 *                    más riesgo).
 *   - ΔTST         : desviación del tiempo total de sueño respecto a la línea
 *                    base, normalizada a [0,1] (más déficit, más riesgo).
 *
 * Los pesos se otorgan con ω3 (sueño) ligeramente dominante, siguiendo la
 * evidencia de que la mala calidad del sueño es el marcador más discriminante
 * (Rodríguez Torres et al., 2023; Bourassa et al., 2026).
 */
class MotorRiesgo(
    private val w1: Double = 0.40,   // peso psicométrico (CESQT)
    private val w2: Double = 0.20,   // peso HRV (RMSSD)
    private val w3: Double = 0.30,   // peso sueño (TST) — dominante entre biométricos
    private val w4: Double = 0.10    // peso FC en reposo (RHR) — apoyo
) {
    init {
        // Los pesos deben sumar 1 para que R quede en [0,1].
        val suma = w1 + w2 + w3 + w4
        require(abs(suma - 1.0) < 1e-6) { "Los pesos ω deben sumar 1 (suma=$suma)." }
    }

    /**
     * Calcula el Índice de Riesgo a partir de la puntuación CESQT y de la
     * biometría reciente frente a la línea base individual.
     *
     * Si no hay datos biométricos frescos (p. ej., el usuario no ha abierto la
     * app del fabricante; véase la limitación reconocida en §4.4), el motor
     * degrada con elegancia y opera SOLO con la rama psicométrica.
     */
    fun calcular(
        scoreCesqtNormalizado: Double,
        biometriaReciente: LecturaBiometrica?,
        lineaBase: LineaBase?,
        /** Edad declarada (opcional): personaliza el umbral de FC. */
        edad: Int? = null
    ): IndiceRiesgo {

        val score = scoreCesqtNormalizado.coerceIn(0.0, 1.0)

        // --- Componente HRV (ΔRMSSD) ---
        // Una caída de RMSSD frente a la base indica activación simpática. Se
        // normaliza por el umbral de caída máxima (UmbralesRiesgo), de modo
        // que el componente satura en 1,0 y no distorsiona la suma.
        val deltaRmssd: Double? = if (
            biometriaReciente?.rmssdMs != null && lineaBase != null && lineaBase.rmssdMedio > 0
        ) {
            val caida = (lineaBase.rmssdMedio - biometriaReciente.rmssdMs) / lineaBase.rmssdMedio
            (caida / UmbralesRiesgo.CAIDA_RMSSD_MAXIMA).coerceIn(0.0, 1.0)
        } else null

        // --- Componente sueño (ΔTST) ---
        val deltaTst: Double? = if (
            biometriaReciente?.tstMin != null && lineaBase != null && lineaBase.tstMedioMin > 0
        ) {
            val deficit = (lineaBase.tstMedioMin - biometriaReciente.tstMin) / lineaBase.tstMedioMin
            (deficit / UmbralesRiesgo.DEFICIT_TST_MAXIMO).coerceIn(0.0, 1.0)
        } else null

        // --- Componente FC en reposo (ΔRHR) — Tarea 6 ---
        // Se computa la ELEVACIÓN en latidos por minuto sobre la línea base
        // individual, no el valor absoluto: 60 lpm puede ser alto en una
        // persona y bajo en otra. Solo penaliza la subida, nunca la bajada.
        val deltaRhr: Double? = if (
            biometriaReciente?.rhrBpm != null && lineaBase != null && lineaBase.rhrMedio > 0
        ) {
            val elevacion = biometriaReciente.rhrBpm - lineaBase.rhrMedio
            val umbral = UmbralesRiesgo.umbralElevacionRhr(lineaBase.rhrMedio, edad)
            (elevacion / umbral).coerceIn(0.0, 1.0)
        } else null

        // --- Combinación con renormalización ---
        // Si falta alguna rama biométrica (limitación §4.4: p. ej. Zepp no
        // exporta RMSSD), NO se asume un valor neutro ni se penaliza: se
        // reparte su peso entre las ramas disponibles. Así el índice sigue
        // viviendo en [0,1] y es comparable entre usuarios con distinto
        // hardware, a costa de perder resolución, lo que se documenta.
        val terminos = buildList {
            add(w1 to score)
            deltaRmssd?.let { add(w2 to it) }
            deltaTst?.let { add(w3 to it) }
            deltaRhr?.let { add(w4 to it) }
        }
        val pesoTotal = terminos.sumOf { it.first }
        val r: Double = (terminos.sumOf { it.first * it.second } / pesoTotal)
            .coerceIn(0.0, 1.0)

        val energia = (100 * (1 - r)).toInt().coerceIn(0, 100)

        // Componente dominante (para que el Gestor de Coping priorice).
        // El componente dominante es el que MÁS APORTA al riesgo total; el
        // Gestor de Coping lo usa para afinar la pauta (§2.3.5). La FC en
        // reposo se agrupa con HRV: ambas apuntan a activación fisiológica y
        // comparten las mismas pautas de desactivación.
        val aportes = listOf(
            IndiceRiesgo.Componente.PSICOMETRICO to w1 * score,
            IndiceRiesgo.Componente.SUENO to w3 * (deltaTst ?: 0.0),
            IndiceRiesgo.Componente.HRV to
                (w2 * (deltaRmssd ?: 0.0) + w4 * (deltaRhr ?: 0.0))
        )
        val dominante = aportes.maxByOrNull { it.second }?.first
            ?: IndiceRiesgo.Componente.PSICOMETRICO

        // Carga biométrica agregada para la matriz A–D del GestorCoping.
        // Carga biométrica agregada (media de las ramas disponibles) para la
        // matriz A–D del GestorCoping.
        val ramasBio = listOfNotNull(deltaRmssd, deltaTst, deltaRhr)
        val cargaBio: Double? = if (ramasBio.isEmpty()) null else ramasBio.average()

        return IndiceRiesgo(
            r = r, energia = energia, componenteDominante = dominante,
            scoreCesqt = score, cargaBiometrica = cargaBio
        )
    }
}
