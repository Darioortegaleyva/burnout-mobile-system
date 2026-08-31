package com.tfg.burnout.domain.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El validador es la última barrera antes de mostrar texto generado por el
 * modelo local (§2.3.6): si algo falla, se muestra la plantilla original.
 */
class ValidadorSalidaTest {

    private val original = "Antes de nada: sentirse así cuando las exigencias son altas es una respuesta normal, no una falla tuya."

    @Test
    fun `acepta una reformulacion razonable`() {
        val ok = "Que te pese la jornada cuando las exigencias aprietan es una reacción esperable; no es culpa tuya."
        assertTrue(ValidadorSalida.validar(original, ok))
    }

    @Test
    fun `rechaza vacios nulos e identicas`() {
        assertFalse(ValidadorSalida.validar(original, null))
        assertFalse(ValidadorSalida.validar(original, "   "))
        assertFalse(ValidadorSalida.validar(original, original))
    }

    @Test
    fun `rechaza terminos clinicos introducidos por el modelo`() {
        assertFalse(ValidadorSalida.validar(original, "Puede que tengas depresión; sentirse así no es una falla tuya, de verdad."))
        assertFalse(ValidadorSalida.validar(original, "Quizá necesites medicación, pero recuerda que no es una falla tuya."))
    }

    @Test
    fun `rechaza longitudes fuera de rango y enlaces`() {
        assertFalse(ValidadorSalida.validar(original, "Ok."))
        assertFalse(ValidadorSalida.validar(original, "Mira esto: https://ejemplo.com y no te preocupes por nada de lo que sientes."))
        assertFalse(ValidadorSalida.validar(original, "x".repeat(300)))
    }
}
