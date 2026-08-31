package com.tfg.burnout.data.rag

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Blinda las dos mejoras del recuperador:
 *
 *  · Emparejamiento por raíz, para que las derivaciones de una palabra
 *    encuentren su fragmento («agobiado» → etiqueta «agobio»).
 *  · Filtro de pertinencia, que evita responder por una coincidencia casual
 *    con un término raro pero irrelevante. El caso que motivó la regla fue
 *    real: «¿quién ganó el mundial?» recuperaba la definición de burnout
 *    porque el corpus contiene «Organización Mundial de la Salud».
 */
class PertinenciaRagTest {

    private fun responde(consulta: String) = BuscadorRag.buscar(consulta).isNotEmpty()

    @Test
    fun `responde a consultas del dominio`() {
        listOf(
            "¿por qué el burnout me quita el sueño?",
            "¿mi jefe puede ver mis datos?",
            "cómo puedo dormir mejor",
            "cada cuánto tengo que hacer el test",
            "qué es la variabilidad cardíaca",
            "cuándo debo ir a un psicólogo",
            "estoy muy cansado del trabajo",
        ).forEach { assertTrue("Debería responder a: $it", responde(it)) }
    }

    @Test
    fun `encuentra el fragmento aunque varie la derivacion de la palabra`() {
        // «agobiado» no aparece en el corpus; «agobio» sí, como etiqueta.
        assertTrue(responde("qué puedo hacer si estoy agobiado"))
    }

    @Test
    fun `rechaza consultas ajenas al dominio`() {
        listOf(
            "¿quién ganó el mundial de fútbol?",
            "receta de tortilla de patatas",
            "cuánto cuesta un coche eléctrico",
            "qué tiempo hace mañana",
            "recomiéndame una película",
            "cómo se cambia una rueda",
        ).forEach { assertTrue("NO debería responder a: $it", !responde(it)) }
    }
}
