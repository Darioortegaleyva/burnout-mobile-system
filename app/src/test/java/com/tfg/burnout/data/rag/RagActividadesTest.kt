package com.tfg.burnout.data.rag

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El asistente debe responder también sobre las ACTIVIDADES del catálogo, no
 * solo sobre teoría: es una de las tres consultas previstas para el BOT B.
 */
class RagActividadesTest {

    @Test
    fun `el corpus incluye las pautas del catalogo`() {
        assertTrue(BaseConocimiento.fragmentos.any { it.id.startsWith("actividad_") })
    }

    @Test
    fun `encuentra una actividad concreta por su nombre`() {
        val r = BuscadorRag.buscar("en qué consiste el apagón de pantallas")
        assertTrue(r.any { it.titulo.contains("Apagón", ignoreCase = true) })
    }

    @Test
    fun `encuentra actividades ante una peticion generica`() {
        val r = BuscadorRag.buscar("qué ejercicios de mindfulness puedo hacer")
        assertTrue(r.isNotEmpty())
    }

    @Test
    fun `sigue sin responder lo que no esta en el corpus`() {
        assertTrue(BuscadorRag.buscar("cuánto cuesta un coche eléctrico").isEmpty())
    }
}
