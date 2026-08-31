package com.tfg.burnout.domain.engine

/**
 * RITMOS DEL SISTEMA — punto único de configuración temporal (§2.2.6, §5.4).
 *
 * Todas las cadencias del sistema se definen aquí para que cambiarlas sea una
 * sola edición y no una búsqueda por el código. Cada valor lleva su
 * justificación, porque modificarlo tiene consecuencias sobre la validez de
 * lo que se mide.
 */
object ConfiguracionRitmos {

    /**
     * Días entre administraciones del cuestionario completo.
     *
     * 28 días (cuatro semanas) es el valor documentado en la memoria: las
     * dimensiones del burnout describen un deterioro lento (Gil-Monte, 2005) y
     * repetir un cuestionario de veinte ítems con demasiada frecuencia produce
     * fatiga de encuestas sin aportar variación clínica (de Vries et al., 2023).
     *
     * ATENCIÓN AL CAMBIARLO: bajarlo a 7 días proporciona una lectura semanal,
     * pero (a) contradice la justificación del apartado 2.2.6, que habría que
     * reescribir, y (b) arriesga el abandono por saturación, el problema que
     * el diseño trata de evitar. La alternativa recomendada es mantener el
     * ciclo mensual y ofrecer el progreso semanal a partir de las metas
     * cumplidas y la biometría, que no requieren volver a preguntar
     * (ver RESUMEN_PROGRESO_DIAS).
     */
    const val CICLO_CUESTIONARIO_DIAS = 28L

    /** Días mínimos entre dos recordatorios, para no insistir a diario. */
    const val DIAS_ENTRE_RECORDATORIOS = 7L

    /**
     * Ventana del resumen de progreso que se ofrece al usuario sin volver a
     * pasarle el cuestionario: se apoya en los retos cumplidos y en la
     * evolución del descanso, datos que ya se recogen solos.
     */
    const val RESUMEN_PROGRESO_DIAS = 7L

    /** Ventana de la media móvil que define la línea base individual. */
    const val VENTANA_INDICE_DIAS = 28

    /**
     * Ventana corta que representa el «estado reciente» y se contrasta con la
     * línea base. Siete días promedian las fluctuaciones de una semana sin
     * diluir una tendencia real, y evitan que una noche aislada mueva el
     * índice, que es el motivo por el que la valoración es mensual (§2.2.6).
     */
    const val VENTANA_RECIENTE_DIAS = 7

    /**
     * Días de histórico mínimos para que la línea base individual sea fiable.
     * Por debajo, el sistema no computa desviaciones biométricas: prefiere
     * decir «aún no lo sé» a inventarse una referencia.
     */
    const val MIN_DIAS_LINEA_BASE = 3

    /** Días durante los cuales no se repite una misma pauta. */
    const val NO_REPETIR_PAUTA_DIAS = 21L
}
