package com.tfg.burnout.domain.chat

import kotlin.random.Random

/**
 * FRASES DE APERTURA DEL ASISTENTE (Tarea 3).
 *
 * Cuando el sistema detecta que toca reevaluación, abre la conversación con
 * una de estas frases elegida al azar, en lugar de repetir siempre la misma.
 * Todas cumplen tres requisitos: tono cercano sin infantilizar, mención del
 * tiempo transcurrido y una invitación explícita (nunca una orden), de modo
 * que el usuario conserve su agencia y pueda decir que no.
 *
 * ESTA LISTA ESTÁ PENSADA PARA REVISIÓN DE LA TUTORA: es texto visible al
 * usuario y, como el resto del banco, se mantiene fuera del modelo de lenguaje.
 */
object FrasesEntrada {

    /** Apertura cuando toca la reevaluación periódica del cuestionario. */
    val invitacionCuestionario = listOf(
        "Buenas. Hace ya unas semanas que hicimos la última evaluación. " +
            "¿Te apetece que repasemos juntos cómo va todo?",
        "¡Hola! Ha pasado un mes desde la última vez que miramos cómo estabas. " +
            "¿Lo vemos de nuevo? Son unos minutos.",
        "Buenas. Toca revisión: unas preguntas para ver cómo has ido " +
            "avanzando este último mes. ¿Te viene bien ahora?",
        "Hola de nuevo. Me gustaría hacerte unas preguntas para ver cómo " +
            "estás llevando estas últimas semanas. ¿Lo hacemos?",
        "¡Hola! Ya ha pasado un ciclo completo desde tu última evaluación. " +
            "¿Quieres que la repitamos y comparamos?",
        "Buenas. Sin prisa, pero cuando puedas me gustaría repasar contigo " +
            "cómo ha ido el último mes. ¿Empezamos?",
        "Hola. Toca poner al día tu evaluación. Son las mismas preguntas de " +
            "siempre y no lleva mucho. ¿Te parece?",
        "¡Buenas! ¿Tienes unos minutos? Me vendría bien saber cómo te has " +
            "sentido estas últimas semanas para seguir acompañándote bien.",
    )

    /** Apertura de la primera evaluación (línea base, usuario nuevo). */
    val invitacionPrimeraVez = listOf(
        "Para empezar, me gustaría conocer tu punto de partida con unas " +
            "preguntas. ¿Las hacemos ahora?",
        "Antes de nada, ¿te parece que hagamos juntos tu primera evaluación? " +
            "Así sé desde dónde partimos.",
        "Para poder acompañarte bien necesito conocerte un poco. ¿Empezamos " +
            "con unas preguntas?",
    )

    /** Saludo del asistente cuando NO hay cuestionario pendiente (BOT B). */
    val saludoAsistente = listOf(
        "Hola, ¿en qué puedo ayudarte hoy?",
        "Buenas. ¿Qué tal va todo? Pregúntame lo que necesites.",
        "Hola de nuevo. ¿Te ayudo con algo?",
        "Buenas. Estoy por aquí si quieres consultarme algo o contarme cómo vas.",
    )

    fun aleatoria(lista: List<String>, aleatorio: Random = Random.Default): String =
        lista[aleatorio.nextInt(lista.size)]
}
