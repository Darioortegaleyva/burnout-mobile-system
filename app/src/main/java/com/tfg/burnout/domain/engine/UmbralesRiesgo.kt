package com.tfg.burnout.domain.engine

/**
 * UMBRALES Y BAREMOS DEL ÍNDICE DE RIESGO (Tarea 6 / bloque «Score general»).
 *
 * Responde a las preguntas: ¿entre qué valores se mueve el índice?, ¿qué es
 * bueno y qué es malo?, ¿cómo se traduce un número en una valoración?
 *
 * ESCALA. El Índice de Riesgo R vive en [0, 1], donde 0 es el mejor estado
 * posible y 1 el peor. Todos sus componentes se normalizan a ese mismo rango
 * antes de combinarse, de modo que la suma ponderada nunca puede salirse.
 * De cara al usuario NUNCA se muestra R, sino su inversión E = 100·(1−R)
 * traducida a una banda cualitativa (§6.2).
 *
 * CALIBRACIÓN. Los cortes que siguen son una PROPUESTA razonada, no un baremo
 * clínico validado: se apoyan en la lógica de cada componente y en el criterio
 * de prudencia asimétrica (§7.5), pero su validación exigiría un estudio
 * longitudinal (limitación reconocida en el Capítulo 8). Se documentan de
 * forma explícita para que sean revisables y sustituibles.
 */
object UmbralesRiesgo {

    // ---------------------------------------------------------------
    // BANDAS DEL ÍNDICE GLOBAL R
    // ---------------------------------------------------------------

    /** Por debajo de este valor, la situación se considera favorable. */
    const val R_BUENO = 0.35

    /** A partir de aquí, la situación requiere atención preferente. */
    const val R_ALTO = 0.60

    enum class Banda(val etiqueta: String, val descripcion: String) {
        BUENO(
            "Buen momento",
            "Los indicadores acompañan. Prevención primaria: reforzar lo que ya funciona."
        ),
        INTERMEDIO(
            "Vas haciendo camino",
            "Hay señales de desgaste incipiente. Momento idóneo para pautas preventivas."
        ),
        CUIDARSE(
            "Hoy toca cuidarse",
            "El desgaste es apreciable. Intervención prioritaria y valoración de apoyo profesional."
        );
    }

    fun bandaDe(r: Double): Banda = when {
        r < R_BUENO -> Banda.BUENO
        r < R_ALTO -> Banda.INTERMEDIO
        else -> Banda.CUIDARSE
    }

    // ---------------------------------------------------------------
    // CORTES POR RAMA (matriz de escenarios A–D, §2.3.4)
    // ---------------------------------------------------------------

    /**
     * Corte del cuestionario. El CESQT se responde en una escala de frecuencia
     * de 0 a 4; normalizada a [0,1], la mitad de la escala (equivalente a
     * responder «algunas veces» de forma sistemática) marca la frontera entre
     * un resultado favorable y uno desfavorable.
     */
    const val CESQT_DESFAVORABLE = 0.50

    /**
     * Corte de la carga biométrica agregada. Un valor de 0,40 equivale, en
     * términos prácticos, a un déficit sostenido en torno al 40 % respecto a
     * la propia línea base: por ejemplo, dormir hora y media menos de lo
     * habitual de forma recurrente.
     */
    const val BIOMETRIA_DESFAVORABLE = 0.40

    /**
     * Corte de la dimensión de Culpa, EN LA ESCALA ORIGINAL DEL CUESTIONARIO
     * (0–4), no normalizada: la culpa no se promedia en el índice global, se
     * usa tal cual como disparador del Perfil 2 (§2.2.7).
     *
     * Un valor de 2,5 equivale a responder, de media, entre «algunas veces al
     * mes» y «frecuentemente» a los ítems de remordimiento. Es un corte
     * deliberadamente sensible: la culpa identifica el perfil de mayor
     * gravedad clínica y aquí prima evitar el falso negativo (§7.5), aun a
     * costa de derivar a alguien que quizá no lo necesitaba.
     */
    const val CULPA_CRITICA = 2.5

    // ---------------------------------------------------------------
    // NORMALIZACIÓN DE LOS COMPONENTES BIOMÉTRICOS
    // ---------------------------------------------------------------

    /**
     * Caída de RMSSD que se considera desviación máxima (valor 1,0). Una
     * reducción del 50 % respecto a la media móvil individual representa una
     * pérdida marcada del tono parasimpático; más allá, no se distingue peor.
     */
    const val CAIDA_RMSSD_MAXIMA = 0.50

    /**
     * Déficit de sueño que se considera desviación máxima. Un 30 % sobre una
     * base de 7,5 h equivale a unas 2 h 15 min menos por noche de forma
     * sostenida.
     */
    const val DEFICIT_TST_MAXIMO = 0.30

    /**
     * Elevación de la frecuencia cardíaca en reposo, en latidos por minuto,
     * considerada desviación máxima sobre la línea base individual.
     *
     * FUNDAMENTO (Tarea 6). El valor de 10 bpm converge desde tres líneas de
     * evidencia independientes:
     *
     *  · Burnout y frecuencia cardíaca. Los estudios que comparan personas
     *    con y sin desgaste profesional describen de forma consistente una
     *    frecuencia cardíaca basal elevada junto a una variabilidad reducida,
     *    interpretadas como una menor actividad parasimpática sostenida
     *    (Föhr et al., 2022; De Vente et al., 2015).
     *  · Magnitud del cambio. Wang et al. (2024) estiman un incremento en
     *    torno a 0,33 bpm por unidad de estrés percibido, lo que sitúa una
     *    subida de 10 bpm en el extremo alto del rango observable.
     *  · Convención de la monitorización con wearables. En la práctica
     *    deportiva y de recuperación se toma una elevación de 5 bpm sobre la
     *    base propia como señal de carga y de 10 bpm como umbral de descanso;
     *    fijar aquí el máximo en 10 mantiene la coherencia con esa escala.
     *
     * Se usa como valor de referencia; el umbral efectivo puede
     * individualizarse mediante umbralElevacionRhr().
     */
    const val ELEVACION_RHR_MAXIMA = 10.0

    /**
     * UMBRAL DE ELEVACIÓN DE FC PERSONALIZADO (Tarea 6, «estudiar viabilidad»).
     *
     * Una misma subida de latidos no significa lo mismo en todas las personas.
     * El ajuste se apoya en la RESERVA CARDÍACA, es decir, el margen entre la
     * frecuencia máxima teórica (220 − edad) y la propia frecuencia en reposo:
     * cuanto menor es esa reserva, mayor proporción de ella consume un mismo
     * incremento, y por tanto más significativo resulta. Es el mismo criterio
     * que emplean los métodos de vigilancia de la carga cardiovascular en el
     * trabajo, que ajustan sus umbrales por grupos de edad.
     *
     * CONCLUSIÓN DEL ESTUDIO DE VIABILIDAD, declarada con honestidad: la edad
     * aporta un ajuste defendible por esta vía, mientras que el peso, la
     * altura y el sexo influyen sobre todo en el NIVEL basal de la frecuencia
     * —no en su reactividad—, y ese nivel ya queda absorbido por la línea base
     * individual de cada usuario. Se recogen igualmente, de forma opcional,
     * para permitir una calibración futura con datos reales, pero no se les
     * atribuye un efecto que la evidencia disponible no respalda.
     *
     * @param rhrBase  frecuencia en reposo habitual del usuario (lpm).
     * @param edad     edad declarada; null si no la ha aportado.
     */
    fun umbralElevacionRhr(rhrBase: Double, edad: Int?): Double {
        if (edad == null || edad !in 14..99 || rhrBase <= 0.0) return ELEVACION_RHR_MAXIMA
        val reservaPersona = (220.0 - edad) - rhrBase
        val reservaReferencia = (220.0 - 30.0) - 60.0     // adulto de 30 años, 60 lpm
        if (reservaPersona <= 0.0) return ELEVACION_RHR_MAXIMA
        val ajustado = ELEVACION_RHR_MAXIMA * (reservaPersona / reservaReferencia)
        // Se acota para que la personalización afine, nunca desvirtúe.
        return ajustado.coerceIn(6.0, 14.0)
    }
}
