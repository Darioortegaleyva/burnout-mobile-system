package com.tfg.burnout.data.rag

import com.tfg.burnout.data.ia.ReformuladorLocal
import com.tfg.burnout.domain.engine.ValidadorSalida

/**
 * ASISTENTE DOCUMENTAL — BOT B (Tarea 4).
 *
 * Responde preguntas abiertas del usuario APOYÁNDOSE EXCLUSIVAMENTE en la
 * base de conocimiento local (RAG). El modelo de lenguaje no aporta
 * contenido propio: recibe los fragmentos recuperados y su única tarea es
 * redactarlos de forma natural y ajustada a la pregunta. Si no hay
 * fragmentos relevantes, el asistente lo admite; si el modelo no está
 * disponible o su salida no es válida, se muestra el fragmento tal cual.
 *
 * Se preserva así la regla del sistema: el motor decide, el modelo redacta
 * (§2.3.6), extendida aquí a «la fuente informa, el modelo redacta».
 */
class AsistenteRag(private val reformulador: ReformuladorLocal?) {

    data class Respuesta(
        val texto: String,
        val fuentes: List<String>,
        /** Enlaces a las fuentes empleadas, para que el usuario amplíe. */
        val enlaces: List<String> = emptyList(),
        /** true si el contenido salió de la base documental. */
        val fundamentada: Boolean
    )

    /**
     * Conectores conversacionales para el texto documental. No aportan
     * información: solo evitan que la respuesta empiece en seco cuando el
     * modelo no ha intervenido.
     */
    private val conectores = listOf(
        "Te cuento:", "Sobre eso:", "Mira:", "A ver, te explico:",
        "Lo que sé de eso es esto:", "Aquí va:"
    )

    /** Frases de reconocimiento de límite: no inventar nunca. */
    private val sinRespuesta = listOf(
        "Prefiero no aventurarme con eso: solo puedo responder sobre lo que " +
            "recogen las fuentes que tengo cargadas, que son las de burnout, " +
            "descanso y afrontamiento. ¿Te reformulo alguna de esas?",
        "Eso se me escapa de lo que tengo documentado. Puedo hablarte del " +
            "desgaste profesional, del sueño, de las pautas de afrontamiento o " +
            "de cómo funciona la propia aplicación.",
        "No tengo información fiable sobre eso, y prefiero decírtelo antes que " +
            "improvisar. Pregúntame sobre burnout, descanso, actividades o tus datos."
    )

    suspend fun responder(consulta: String): Respuesta {
        val fragmentos = BuscadorRag.buscar(consulta)

        if (fragmentos.isEmpty()) {
            return Respuesta(
                texto = sinRespuesta.random(),
                fuentes = emptyList(),
                fundamentada = false
            )
        }

        // Base factual: el texto que SIEMPRE se puede mostrar tal cual.
        //
        // Para el RESPALDO se emplea únicamente el fragmento mejor puntuado.
        // Concatenar varios producía respuestas largas y deslavazadas, que se
        // leían como una entrada de enciclopedia en vez de como una respuesta.
        // Al modelo, en cambio, se le entregan todos los recuperados: cuanto
        // más contexto tenga para redactar, mejor, siempre que sea material
        // ya verificado.
        val base = fragmentos.first().texto
        val fuentes = fragmentos.map { it.fuente }.distinct()
        val enlaces = fragmentos.mapNotNull { it.enlace }.distinct()

        // El modelo solo ajusta la redacción a la pregunta concreta.
        val redactado = reformulador?.let { r ->
            val contexto = fragmentos.joinToString("\n") { "- " + it.texto }
            val prompt =
                "Eres un asistente de bienestar laboral. Responde a la pregunta del " +
                "usuario en español, en dos o tres frases, con tono cercano y usando " +
                "ÚNICAMENTE la información de las notas. No añadas datos que no estén " +
                "en ellas, no des diagnósticos y no inventes cifras.\n\n" +
                "Notas:\n" + contexto + "\n\n" +
                "Pregunta: " + consulta + "\n\nRespuesta:"
            r.generarConPrompt(prompt)
        }

        val texto = if (redactado != null && ValidadorSalida.validar(base, redactado)) {
            redactado
        } else {
            // Respaldo: el fragmento documental íntegro, siempre seguro. Se
            // antepone un conector breve para que la respuesta no irrumpa
            // como un extracto de manual; el conector no aporta contenido,
            // solo enlaza con la pregunta del usuario.
            conectores.random() + " " + base
        }

        return Respuesta(texto = texto, fuentes = fuentes, enlaces = enlaces, fundamentada = true)
    }
}
