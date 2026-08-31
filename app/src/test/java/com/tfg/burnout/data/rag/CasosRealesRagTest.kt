package com.tfg.burnout.data.rag

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Casos surgidos de las pruebas manuales sobre el dispositivo. Se conservan
 * como regresión porque cada uno reveló un fallo distinto del recuperador.
 */
class CasosRealesRagTest {

    private fun mejor(consulta: String): String? =
        BuscadorRag.buscar(consulta).firstOrNull()?.id

    @Test
    fun `una institucion homonima no arrastra el fragmento de definicion`() {
        // «Organización Mundial del Comercio» coincidía en dos términos con
        // «Organización Mundial de la Salud», ambos vacíos de contenido.
        assertTrue(BuscadorRag.buscar("¿qué es la Organización Mundial del Comercio?").isEmpty())
        assertTrue(BuscadorRag.buscar("¿quién ganó el mundial de fútbol?").isEmpty())
    }

    @Test
    fun `preguntar por el deporte no devuelve la derivacion profesional`() {
        // La etiqueta «ayuda», demasiado genérica, arrastraba el fragmento de
        // ayuda profesional ante cualquier consulta que contuviera el verbo.
        assertTrue(mejor("¿el deporte ayuda?") != "cuando_profesional")
    }

    @Test
    fun `las consultas de una sola palabra siguen resolviendo`() {
        assertTrue(mejor("sueño") != null)
        assertTrue(mejor("burnout") != null)
    }

    @Test
    fun `preguntar quien soy yo no devuelve la presentacion del asistente`() {
        assertTrue(mejor("¿quién soy yo?") != "asistente_quien_soy")
    }

    @Test
    fun `preguntar quien eres si devuelve la presentacion`() {
        assertTrue(mejor("hola, ¿quién eres?") == "asistente_quien_soy")
    }
}
