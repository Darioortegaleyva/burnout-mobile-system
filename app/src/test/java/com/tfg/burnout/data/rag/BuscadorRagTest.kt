package com.tfg.burnout.data.rag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El recuperador debe encontrar el fragmento correcto para preguntas
 * formuladas como las haría un usuario real y, sobre todo, DEBE ADMITIR que
 * no sabe cuando la pregunta cae fuera del corpus: de ese comportamiento
 * depende que el asistente no invente (Tarea 4).
 */
class BuscadorRagTest {

    @Test
    fun `recupera el fragmento de sueño ante una pregunta coloquial`() {
        val r = BuscadorRag.buscar("¿cómo puedo dormir mejor?")
        assertTrue("Debería recuperar algo", r.isNotEmpty())
        assertTrue(r.any { it.id == "higiene_sueno" || it.id == "sueno_importancia" })
    }

    @Test
    fun `recupera privacidad cuando preguntan por sus datos`() {
        val r = BuscadorRag.buscar("¿mi empresa puede ver mis datos?")
        assertTrue(r.any { it.id == "app_privacidad" })
    }

    @Test
    fun `recupera la definicion ante que es el burnout`() {
        val r = BuscadorRag.buscar("qué es exactamente el burnout")
        assertTrue(r.any { it.id == "def_burnout" })
    }

    @Test
    fun `devuelve vacio si la pregunta esta fuera del corpus`() {
        assertEquals(0, BuscadorRag.buscar("¿quién ganó la liga de fútbol?").size)
        assertEquals(0, BuscadorRag.buscar("receta de tortilla de patatas").size)
    }

    @Test
    fun `nunca devuelve mas fragmentos de los pedidos`() {
        assertTrue(BuscadorRag.buscar("burnout trabajo sueño estrés", maximo = 2).size <= 2)
    }
}
