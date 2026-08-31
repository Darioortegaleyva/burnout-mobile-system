package com.tfg.burnout.domain.engine

import com.tfg.burnout.domain.model.CategoriaCoping
import com.tfg.burnout.domain.model.PautaCoping

/**
 * CATÁLOGO DE PAUTAS DE MITIGACIÓN (§2.3.2).
 *
 * Materializa el "modelo de interacción bajo demanda": el usuario puede
 * consultar libremente la biblioteca de técnicas desde la pantalla de
 * Actividades, sin esperar a que el sistema se las sugiera (agencia, P3).
 * El motor (GestorCoping) usa estas mismas categorías para priorizar lo que
 * recomienda según el escenario del usuario.
 *
 * Contenido alineado con la evidencia revisada en la memoria: higiene del
 * sueño (Blasco Espinosa et al., 2002), mindfulness/ACT (OMS, 2022),
 * reestructuración cognitiva (Quiñones y Arreola, 2022) y apoyo social
 * (Schaufeli, 2006).
 */
object CatalogoPautas {

    val pautas: List<PautaCoping> = listOf(
        // ----- Higiene del sueño y desconexión digital -----
        PautaCoping(1, CategoriaCoping.HIGIENE_SUENO, "Apagón de pantallas",
            "Apaga móvil, tablet y ordenador 30 minutos antes de acostarte. La luz de las pantallas retrasa la señal natural de sueño."),
        PautaCoping(2, CategoriaCoping.HIGIENE_SUENO, "Ritual de cierre del día",
            "Repite cada noche una pequeña rutina (luz tenue, ducha templada, lectura ligera). Tu cuerpo aprenderá que es hora de descansar."),
        PautaCoping(3, CategoriaCoping.HIGIENE_SUENO, "Frontera con el trabajo",
            "Silencia las notificaciones de trabajo fuera de tu horario. Si algo es realmente urgente, encontrará otro camino."),
        PautaCoping(4, CategoriaCoping.HIGIENE_SUENO, "Cafeína con horario",
            "Evita el café y las bebidas energéticas a partir de media tarde; permanecen en el cuerpo más horas de las que parece."),

        // ----- Mindfulness y ACT -----
        PautaCoping(5, CategoriaCoping.MINDFULNESS_ACT, "Respiración 4-6",
            "Inhala contando hasta 4 y exhala contando hasta 6, durante 3 minutos. Alargar la exhalación calma el sistema nervioso."),
        PautaCoping(6, CategoriaCoping.MINDFULNESS_ACT, "Escáner corporal exprés",
            "Cierra los ojos 2 minutos y recorre mentalmente el cuerpo de pies a cabeza, soltando la tensión que encuentres."),
        PautaCoping(7, CategoriaCoping.MINDFULNESS_ACT, "Pensamientos como nubes",
            "Cuando te asalte un pensamiento que pesa, imagínalo pasando como una nube: no hay que pelearlo, solo verlo pasar."),
        PautaCoping(8, CategoriaCoping.MINDFULNESS_ACT, "Un minuto de pausa plena",
            "Antes de cambiar de tarea, dedica un minuto a notar tu respiración. Las microtransiciones reducen la carga acumulada."),

        // ----- Reestructuración cognitiva y resiliencia -----
        PautaCoping(9, CategoriaCoping.REESTRUCTURACION, "Diario de tres líneas",
            "Al acabar el día, escribe una cosa que salió bien, una que te costó y una que harás distinto. Da perspectiva sin exigirte."),
        PautaCoping(10, CategoriaCoping.REESTRUCTURACION, "Reencuadre del pensamiento",
            "Cuando pienses \u201ctodo me sale mal\u201d, busca el dato real: ¿todo, o esta tarea concreta hoy? Nombrar lo concreto reduce su tamaño."),
        PautaCoping(11, CategoriaCoping.REESTRUCTURACION, "Matriz de prioridades",
            "Divide tus tareas en urgente/importante. Lo que no sea ninguna de las dos, agéndalo o suéltalo sin culpa."),
        PautaCoping(12, CategoriaCoping.REESTRUCTURACION, "Logro mínimo del día",
            "Define cada mañana una única cosa que, si la haces, el día ya cuenta. El resto es extra, no deuda."),

        // ----- Apoyo social y reglas de oro -----
        PautaCoping(13, CategoriaCoping.APOYO_SOCIAL, "Llamada de cinco minutos",
            "Llama hoy a alguien con quien te sientas a gusto, sin agenda. El contacto social amortigua el desgaste."),
        PautaCoping(14, CategoriaCoping.APOYO_SOCIAL, "Pedir ayuda a tiempo",
            "Si una tarea te desborda, dilo antes de que se desborde contigo. Pedir ayuda es una habilidad, no una debilidad."),
        PautaCoping(15, CategoriaCoping.APOYO_SOCIAL, "Límite asertivo",
            "Practica una frase de límite amable: \u201cHoy no llego; puedo tenerlo el jueves\u201d. Decir cómo sí es mejor que un sí imposible."),
        PautaCoping(16, CategoriaCoping.APOYO_SOCIAL, "Ocio en la agenda",
            "Reserva en tu calendario un hueco de ocio igual que reservarías una reunión. Lo que no se agenda, se pierde."),
        // ---- Recursos de referencia sugeridos por la tutora ----
        // Redacción propia a partir de las recomendaciones de Mind (Reino
        // Unido) para personas que se sienten quemadas en el trabajo, y de la
        // guía de recuperación del burnout de la Universidad de Colorado
        // Denver. Ambas coinciden con la evidencia ya recogida en §2.3.2 y
        // aportan pautas concretas que faltaban en el catálogo.

        PautaCoping(17, CategoriaCoping.APOYO_SOCIAL, "Nombrar lo que pasa",
            "Pon en palabras con alguien de confianza qué parte concreta del trabajo te está desbordando. Nombrar el problema lo vuelve manejable y evita que se convierta en un malestar difuso."),
        PautaCoping(18, CategoriaCoping.APOYO_SOCIAL, "Conversación con quien decide",
            "Prepara una conversación con tu responsable centrada en hechos, no en quejas: qué tareas se acumulan, qué plazos no encajan y qué propondrías cambiar. Llevar una propuesta cambia el tono de la conversación."),
        PautaCoping(19, CategoriaCoping.REESTRUCTURACION, "Las tres columnas",
            "Divide tus tareas pendientes en tres columnas: lo que solo puedes hacer tú, lo que puede hacer otra persona y lo que en realidad puede esperar. La segunda y la tercera suelen ser más largas de lo que parecía."),
        PautaCoping(20, CategoriaCoping.REESTRUCTURACION, "Recuperar el porqué",
            "Dedica diez minutos a escribir qué parte de tu trabajo te sigue importando, aunque sea pequeña. Reconectar con eso protege frente al desencanto mejor que intentar convencerte de que todo va bien."),
        PautaCoping(21, CategoriaCoping.MINDFULNESS_ACT, "Microdescansos reales",
            "Cada noventa minutos, levántate y cambia de estímulo durante cinco: mirar por la ventana, estirarte, beber agua. Un descanso mirando el móvil no descansa la atención."),
        PautaCoping(22, CategoriaCoping.MINDFULNESS_ACT, "Algo que no rinda",
            "Reserva un rato a la semana para una actividad que no persiga ningún objetivo ni se mida: cocinar sin prisa, caminar sin destino, tocar música mal. La recuperación necesita espacios sin rendimiento."),
        PautaCoping(23, CategoriaCoping.HIGIENE_SUENO, "Descarga mental antes de dormir",
            "Si te acuestas dándole vueltas al trabajo, deja anotado en papel lo que te preocupa y qué harás mañana al respecto. Sacarlo de la cabeza reduce la rumiación nocturna."),
        PautaCoping(24, CategoriaCoping.HIGIENE_SUENO, "Luz por la mañana",
            "Sal a la luz natural durante los primeros minutos del día, aunque esté nublado. Es la señal más potente para reajustar el reloj interno y dormir mejor la noche siguiente."),
    )

    /**
     * Elige una pauta de la categoría evitando las propuestas recientemente
     * (bloque «Pautas»: no repetir). Si todas se han usado ya, se reinicia el
     * ciclo y se devuelve cualquiera: es preferible repetir a no proponer nada.
     */
    fun elegirSinRepetir(
        categoria: CategoriaCoping,
        yaPropuestas: Set<String>,
        aleatorio: kotlin.random.Random = kotlin.random.Random.Default
    ): PautaCoping? {
        val todas = de(categoria)
        if (todas.isEmpty()) return null
        val frescas = todas.filter { it.id.toString() !in yaPropuestas }
        val candidatas = frescas.ifEmpty { todas }
        return candidatas[aleatorio.nextInt(candidatas.size)]
    }

    fun de(categoria: CategoriaCoping): List<PautaCoping> =
        pautas.filter { it.categoria == categoria }
}
