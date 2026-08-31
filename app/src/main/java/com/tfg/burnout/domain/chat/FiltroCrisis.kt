package com.tfg.burnout.domain.chat

import java.text.Normalizer

/**
 * FILTRO DE SEGURIDAD PREVIO AL ASISTENTE (§7.4).
 *
 * Barrera obligatoria antes de cualquier recuperación o generación: si el
 * mensaje del usuario contiene señales de crisis, la conversación NO pasa por
 * el modelo ni por la base de conocimiento. Se responde con texto fijo y se
 * ofrece ayuda inmediata.
 *
 * El filtro está calibrado para errar por exceso de prudencia: es preferible
 * ofrecer ayuda a quien no la necesitaba en ese momento que no ofrecérsela a
 * quien sí (asimetría ética de los errores, §7.5).
 */
object FiltroCrisis {

    private fun normalizar(t: String) =
        Normalizer.normalize(t.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    // Expresiones que activan la vía de atención inmediata.
    private val señalesCriticas = listOf(
        "no quiero vivir", "no quiero seguir viviendo", "quitarme la vida",
        "quitarme de en medio", "acabar con todo", "no merece la pena vivir",
        "desaparecer para siempre", "hacerme dano", "hacerme dano a mi mismo",
        "no aguanto mas", "no puedo mas con mi vida", "mejor no estar",
        "suicid", "matarme"
    )

    // Expresiones de malestar intenso: no son crisis aguda, pero merecen
    // acompañamiento y sugerencia de apoyo profesional, sin pasar por el RAG.
    private val señalesMalestarIntenso = listOf(
        "estoy fatal", "estoy hundido", "estoy hundida", "no puedo mas",
        "me quiero morir de agobio", "estoy al limite", "me supera todo",
        "llevo semanas llorando", "no duermo nada", "ataque de ansiedad",
        "ansiedad" , "depresion", "deprimido", "deprimida"
    )

    enum class Nivel { NINGUNO, MALESTAR_INTENSO, CRISIS }

    fun evaluar(mensaje: String): Nivel {
        val t = normalizar(mensaje)
        if (señalesCriticas.any { t.contains(it) }) return Nivel.CRISIS
        if (señalesMalestarIntenso.any { t.contains(it) }) return Nivel.MALESTAR_INTENSO
        return Nivel.NINGUNO
    }

    /** Respuesta fija ante crisis. Nunca se reformula ni se genera. */
    val respuestaCrisis: List<String> = listOf(
        "Siento mucho que estés pasando por esto, y me alegra que lo hayas escrito.",
        "Yo soy una aplicación y aquí me quedo corto: esto merece que hables con " +
            "alguien de verdad, ahora. En España tienes la Línea 024 de atención a " +
            "la conducta suicida, gratuita y disponible las veinticuatro horas. " +
            "También puedes llamar al 112 si es urgente.",
        "Si te apetece, puedo darte además el contacto del Colegio Oficial de " +
            "Psicología de tu provincia para más adelante."
    )

    /** Respuesta ante malestar intenso sin crisis aguda. */
    val respuestaMalestar: List<String> = listOf(
        "Gracias por contármelo, y siento que lo estés pasando así.",
        "Por lo que describes, creo que esto se te queda grande para afrontarlo " +
            "en solitario, y no es una debilidad tuya: es lo razonable. Te " +
            "recomiendo apoyarte en un profesional.",
        "Si quieres, dime tu provincia y te paso el contacto del Colegio Oficial " +
            "de Psicología más cercano. Y si en algún momento lo necesitas ya, la " +
            "Línea 024 atiende las veinticuatro horas."
    )
}
