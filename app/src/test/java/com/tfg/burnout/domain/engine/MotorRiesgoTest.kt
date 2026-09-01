package com.tfg.burnout.domain.engine

import com.tfg.burnout.domain.model.LecturaBiometrica
import com.tfg.burnout.domain.model.LineaBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios del Motor de Riesgo. Verifican la ecuación de triangulación
 * de cuatro ramas (§2.2.5), la renormalización de pesos ante ramas ausentes
 * (§4.4) y la degradación elegante cuando no hay biometría.
 *
 * El motor se instancia SIN argumentos a propósito: así los tests validan los
 * pesos documentados por defecto (ω1 0,40 · ω2 0,20 · ω3 0,30 · ω4 0,10) en
 * lugar de fijar unos propios que podrían divergir de la memoria.
 */
class MotorRiesgoTest {

    private val motor = MotorRiesgo()

    @Test
    fun `sin biometria opera solo con la rama psicometrica`() {
        val indice = motor.calcular(
            scoreCesqtNormalizado = 0.6,
            biometriaReciente = null,
            lineaBase = null
        )
        // Al renormalizar, el único término disponible absorbe todo el peso:
        // R debe igualar el score psicométrico.
        assertEquals(0.6, indice.r, 1e-6)
        assertEquals(40, indice.energia) // 100·(1−0.6)
    }

    @Test
    fun `caso completo combina las cuatro ramas ponderadas`() {
        // Línea base: RMSSD 50 ms, TST 480 min (8 h), FC en reposo 55 lpm.
        val base = LineaBase(rmssdMedio = 50.0, tstMedioMin = 480.0, rhrMedio = 55.0)
        val lectura = LecturaBiometrica(
            fechaEpochDay = 0,
            rmssdMs = 25.0,   // caída del 50 % => satura en ΔRMSSD = 1.0
            tstMin = 360.0,   // déficit del 25 % sobre el máximo del 30 % => 0.8333
            rhrBpm = 60.0     // +5 lpm sobre una referencia de 10 => ΔRHR = 0.5
        )
        val indice = motor.calcular(
            scoreCesqtNormalizado = 0.4,
            biometriaReciente = lectura,
            lineaBase = base
        )
        // R = 0.40*0.4 + 0.20*1.0 + 0.30*0.8333 + 0.10*0.5
        //   = 0.16 + 0.20 + 0.25 + 0.05 = 0.66
        assertEquals(0.66, indice.r, 1e-4)
        assertEquals(34, indice.energia) // 100·(1−0.66)
    }

    @Test
    fun `sin FC en reposo el peso se reparte entre las ramas disponibles`() {
        // Sin rhrMedio, la línea base no habilita la cuarta rama (§4.4): su
        // peso NO se sustituye por un valor neutro, se redistribuye.
        val base = LineaBase(rmssdMedio = 50.0, tstMedioMin = 480.0)
        val lectura = LecturaBiometrica(
            fechaEpochDay = 0,
            rmssdMs = 25.0,
            tstMin = 360.0,
            rhrBpm = 60.0     // se ignora: sin línea base de FC no hay referencia
        )
        val indice = motor.calcular(
            scoreCesqtNormalizado = 0.4,
            biometriaReciente = lectura,
            lineaBase = base
        )
        // El peso total baja a 0.90 y el numerador es 0.16 + 0.20 + 0.25 = 0.61.
        // R = 0.61 / 0.90 = 0.6778 — el índice sigue viviendo en [0,1].
        assertEquals(0.6778, indice.r, 1e-4)
    }

    @Test
    fun `una bajada de la FC en reposo nunca penaliza`() {
        // La cuarta rama mide la ELEVACIÓN sobre la base: una FC más baja de lo
        // habitual es buena señal y debe quedarse en 0, jamás restar energía.
        val base = LineaBase(rmssdMedio = 50.0, tstMedioMin = 480.0, rhrMedio = 55.0)
        val lectura = LecturaBiometrica(
            fechaEpochDay = 0,
            rmssdMs = 50.0,   // igual a la base
            tstMin = 480.0,   // igual a la base
            rhrBpm = 45.0     // 10 lpm POR DEBAJO de la base
        )
        val indice = motor.calcular(
            scoreCesqtNormalizado = 0.0,
            biometriaReciente = lectura,
            lineaBase = base
        )
        assertEquals(0.0, indice.r, 1e-6)
        assertEquals(100, indice.energia)
    }

    @Test
    fun `R siempre queda acotado en cero y uno`() {
        val base = LineaBase(rmssdMedio = 50.0, tstMedioMin = 480.0, rhrMedio = 55.0)
        val lectura = LecturaBiometrica(0, rmssdMs = 1.0, tstMin = 1.0, rhrBpm = 90.0)
        val indice = motor.calcular(1.0, lectura, base)
        assertTrue(indice.r in 0.0..1.0)
    }
}
