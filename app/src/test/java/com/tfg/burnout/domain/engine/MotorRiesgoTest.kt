package com.tfg.burnout.domain.engine

import com.tfg.burnout.domain.model.LecturaBiometrica
import com.tfg.burnout.domain.model.LineaBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios del Motor de Riesgo. Verifican la ecuación de triangulación
 * y la degradación elegante cuando falta biometría (§2.2.5, §4.4).
 */
class MotorRiesgoTest {

    private val motor = MotorRiesgo(w1 = 0.40, w2 = 0.25, w3 = 0.35)

    @Test
    fun `sin biometria opera solo con la rama psicometrica`() {
        val indice = motor.calcular(
            scoreCesqtNormalizado = 0.6,
            biometriaReciente = null,
            lineaBase = null
        )
        // R debe igualar el score psicométrico cuando no hay datos físicos.
        assertEquals(0.6, indice.r, 1e-6)
        assertEquals(40, indice.energia) // 100·(1−0.6)
    }

    @Test
    fun `caso completo combina las tres ramas ponderadas`() {
        // Línea base: RMSSD 50 ms, TST 480 min (8 h).
        val base = LineaBase(rmssdMedio = 50.0, tstMedioBaseMin = 480.0)
        val lectura = LecturaBiometrica(
            fechaEpochDay = 0,
            rmssdMs = 25.0,   // caída del 50% => ΔRMSSD = 0.5
            tstMin = 360.0,   // déficit del 25% => ΔTST = 0.25
            rhrBpm = 60.0
        )
        val indice = motor.calcular(
            scoreCesqtNormalizado = 0.4,
            biometriaReciente = lectura,
            lineaBase = base
        )
        // R = 0.40*0.4 + 0.25*0.5 + 0.35*0.25 = 0.16 + 0.125 + 0.0875 = 0.3725
        assertEquals(0.3725, indice.r, 1e-4)
        assertTrue(indice.energia in 0..100)
    }

    @Test
    fun `R siempre queda acotado en cero y uno`() {
        val base = LineaBase(rmssdMedio = 50.0, tstMedioBaseMin = 480.0)
        val lectura = LecturaBiometrica(0, rmssdMs = 1.0, tstMin = 1.0, rhrBpm = 90.0)
        val indice = motor.calcular(1.0, lectura, base)
        assertTrue(indice.r in 0.0..1.0)
    }
}
