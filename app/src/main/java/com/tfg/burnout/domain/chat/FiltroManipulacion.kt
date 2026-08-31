package com.tfg.burnout.domain.chat

import java.text.Normalizer

/**
 * RESISTENCIA A INTENTOS DE MANIPULACIÓN DEL ASISTENTE (§2.3.7).
 *
 * Un asistente de salud es un objetivo natural para dos clases de intento:
 * quien busca que abandone sus instrucciones («ignora lo anterior y actúa
 * como…») y quien busca arrancarle un diagnóstico («dime que tengo
 * depresión»). Ninguno de los dos debe prosperar, y conviene señalar que la
 * arquitectura ya los dificulta por construcción: el modelo no decide qué
 * responder, solo reformula fragmentos recuperados, y su salida atraviesa un
 * validador que rechaza terminología clínica. Este filtro añade una capa
 * previa que corta el intento antes incluso de la recuperación, y responde
 * de forma explícita sobre los límites del sistema.
 */
object FiltroManipulacion {

    private fun normalizar(t: String) =
        Normalizer.normalize(t.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    /** Intentos de que el asistente abandone su guion o suplante un rol. */
    private val instruccionesAdversas = listOf(
        // Español, incluidas erratas frecuentes al teclear deprisa.
        "ignora tus instrucciones", "ignora las instrucciones", "ignora lo anterior",
        "ignra tus instrucciones", "ignora todo lo anterior",
        "olvida tus instrucciones", "olvida las reglas", "olvidate de tus",
        "salta tus reglas", "saltate tus reglas",
        "actua como si fueras", "actua como un", "actua como si",
        "haz como si fueras", "finge que eres", "finge ser",
        "imagina que eres", "imaginate que eres",
        // Formulación indirecta: la trampa más habitual y la que peor se
        // detecta, porque no ordena nada, solo lo insinúa.
        "si fueras un medico", "si fueras medico", "si fueras psicologo",
        "si fueras un psicologo", "como si fueras un", "ponte en el papel",
        "haciendo de medico", "en plan medico",
        "eres un medico", "eres medico", "eres psicologo", "eres un psicologo",
        "eres un profesional sanitario",
        // Inglés: la barrera del idioma no debe ser un atajo.
        "ignore your instructions", "ignore all previous", "ignore previous",
        "forget your instructions", "forget the rules", "act as if you were",
        "pretend you are", "pretend to be", "you are a doctor", "developer mode",
        "jailbreak", "system prompt", "what is your prompt",
        // Sonsacar la configuración interna.
        "modo desarrollador", "sin restricciones", "sin filtros",
        "responde sin limitaciones", "cual es tu prompt",
        "cuales son tus instrucciones", "repite tu prompt", "muestrame tu prompt"
    )

    /** Peticiones de diagnóstico, que el sistema no puede ni debe emitir. */
    private val peticionesDiagnostico = listOf(
        "diagnosticame", "diagnostico tengo", "que enfermedad tengo",
        "dime que tengo", "tengo burnout", "tengo depresion", "estoy deprimido",
        "estoy quemado verdad", "confirmame que tengo", "que me pasa exactamente",
        "cuanto burnout tengo", "que nivel de burnout"
    )

    enum class Intento { NINGUNO, INSTRUCCION_ADVERSA, PETICION_DIAGNOSTICO }

    fun evaluar(mensaje: String): Intento {
        val t = normalizar(mensaje)
        if (instruccionesAdversas.any { t.contains(it) }) return Intento.INSTRUCCION_ADVERSA
        if (peticionesDiagnostico.any { t.contains(it) }) return Intento.PETICION_DIAGNOSTICO
        return Intento.NINGUNO
    }

    /**
     * Respuesta ante un intento de reconducir al asistente. Se contesta sin
     * dramatismo y sin regañar: se explica el límite y se ofrece continuar.
     */
    val respuestaInstruccionAdversa = listOf(
        "No puedo cambiar de papel: soy el asistente de esta aplicación y respondo " +
            "a partir de las fuentes que tengo cargadas sobre desgaste profesional.",
        "Si quieres, pregúntame sobre burnout, descanso, las actividades de la " +
            "aplicación o qué se hace con tus datos."
    )

    /**
     * Respuesta ante una petición de diagnóstico. Es el principio de no
     * diagnóstico (§2.3.3) llevado a la conversación: no se elude la
     * pregunta, se explica por qué no corresponde responderla.
     */
    val respuestaPeticionDiagnostico = listOf(
        "No puedo decirte si tienes burnout, y no es por evasiva: un diagnóstico " +
            "solo puede hacerlo un profesional cualificado que te conozca.",
        "Lo que sí hago es cruzar tus respuestas con tus datos de descanso y " +
            "pulso para darte una orientación sobre cómo va tu nivel de energía, y " +
            "proponerte pautas acordes. Y si la situación lo requiere, te acerco a " +
            "ayuda profesional.",
        "¿Quieres que te explique cómo interpreto esa información?"
    )
}
