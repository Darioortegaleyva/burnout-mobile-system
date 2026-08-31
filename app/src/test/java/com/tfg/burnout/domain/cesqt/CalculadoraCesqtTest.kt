package com.tfg.burnout.domain.cesqt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la puntuación del CESQT (§2.2.2): inversión de la escala de
 * Ilusión, exclusión de Culpa del score global y normalización a [0,1].
 */
class CalculadoraCesqtTest {

    /** Construye un mapa de 20 respuestas con un valor fijo por dimensión. */
    private fun respuestas(
        ilusion: Int, desgaste: Int, indolencia: Int, culpa: Int
    ): Map<Int, Int> = buildMap {
        CatalogoCesqt.items.forEach { item ->
            val v = when (item.dimension) {
                DimensionCesqt.ILUSION_POR_TRABAJO -> ilusion
                DimensionCesqt.DESGASTE_PSIQUICO -> desgaste
                DimensionCesqt.INDOLENCIA -> indolencia
                DimensionCesqt.CULPA -> culpa
            }
            put(item.id, v)
        }
    }

    @Test
    fun `ilusion alta reduce el score global porque la escala es inversa`() {
        // Máxima ilusión (4) y resto a cero => burnout mínimo.
        val r = CalculadoraCesqt.calcular(respuestas(4, 0, 0, 0))
        assertEquals(0.0, r.scoreGlobalNormalizado, 1e-9)
    }

    @Test
    fun `ilusion nula equivale a burnout en esa dimension`() {
        // Ilusión 0 (invertida: 4) y resto a cero => solo pesa la rama Ilusión.
        val r = CalculadoraCesqt.calcular(respuestas(0, 0, 0, 0))
        // Global = media(4, 0, 0) / 4 = 0.333...
        assertEquals(4.0 / 3.0 / 4.0, r.scoreGlobalNormalizado, 1e-9)
    }

    @Test
    fun `la Culpa no entra en el score global pero se devuelve aparte`() {
        val sinCulpa = CalculadoraCesqt.calcular(respuestas(2, 2, 2, 0))
        val conCulpa = CalculadoraCesqt.calcular(respuestas(2, 2, 2, 4))
        // El global no cambia al variar la Culpa...
        assertEquals(sinCulpa.scoreGlobalNormalizado, conCulpa.scoreGlobalNormalizado, 1e-9)
        // ...pero el subscore sí la refleja (disparador del Perfil 2).
        assertEquals(0.0, sinCulpa.subscoreCulpa, 1e-9)
        assertEquals(4.0, conCulpa.subscoreCulpa, 1e-9)
    }

    @Test
    fun `el score global queda normalizado entre cero y uno`() {
        val maximo = CalculadoraCesqt.calcular(respuestas(0, 4, 4, 4))
        assertEquals(1.0, maximo.scoreGlobalNormalizado, 1e-9)
        assertTrue(maximo.scoreGlobalNormalizado in 0.0..1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `faltan respuestas lanza error`() {
        CalculadoraCesqt.calcular(mapOf(1 to 2, 2 to 3))
    }
}
