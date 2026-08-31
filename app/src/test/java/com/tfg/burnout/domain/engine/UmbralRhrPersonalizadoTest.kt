package com.tfg.burnout.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Personalización del umbral de frecuencia cardíaca (Tarea 6). Sin edad
 * declarada debe comportarse exactamente como antes: la personalización es
 * opcional y nunca puede empeorar el caso base.
 */
class UmbralRhrPersonalizadoTest {

    @Test
    fun `sin edad se usa el valor de referencia`() {
        assertEquals(10.0, UmbralesRiesgo.umbralElevacionRhr(58.0, null), 1e-9)
    }

    @Test
    fun `una persona joven con pulso bajo tolera mas elevacion`() {
        // 22 años, 51 lpm: reserva amplia, un mismo repunte pesa menos.
        val u = UmbralesRiesgo.umbralElevacionRhr(51.0, 22)
        assertTrue("Esperaba > 10, fue $u", u > 10.0)
    }

    @Test
    fun `una persona mayor con pulso alto es mas sensible`() {
        // 58 años, 75 lpm: reserva estrecha, el mismo repunte pesa más.
        val u = UmbralesRiesgo.umbralElevacionRhr(75.0, 58)
        assertTrue("Esperaba < 10, fue $u", u < 10.0)
    }

    @Test
    fun `el umbral siempre queda acotado`() {
        for (edad in 14..99) {
            for (rhr in 35..95) {
                val u = UmbralesRiesgo.umbralElevacionRhr(rhr.toDouble(), edad)
                assertTrue("Fuera de rango: $u", u in 6.0..14.0)
            }
        }
    }

    @Test
    fun `datos absurdos no rompen el calculo`() {
        assertEquals(10.0, UmbralesRiesgo.umbralElevacionRhr(0.0, 30), 1e-9)
        assertEquals(10.0, UmbralesRiesgo.umbralElevacionRhr(58.0, 5), 1e-9)
        assertEquals(10.0, UmbralesRiesgo.umbralElevacionRhr(58.0, 200), 1e-9)
    }
}
