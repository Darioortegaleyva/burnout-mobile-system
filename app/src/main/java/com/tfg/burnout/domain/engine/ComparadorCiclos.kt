package com.tfg.burnout.domain.engine

/**
 * COMPARACIÓN ENTRE DOS EVALUACIONES CONSECUTIVAS (CU-04).
 *
 * Cerrar el ciclo mensual no consiste solo en volver a medir: consiste en
 * devolverle a la persona una lectura de su propia evolución. Sin esa
 * devolución, repetir el cuestionario cada mes carece de sentido para quien
 * lo responde.
 *
 * La comparación se expresa siempre en términos cualitativos, nunca
 * numéricos, en coherencia con el principio de comunicación no
 * estigmatizante (§6.2): al usuario le importa saber si va mejor, igual o
 * peor, y en qué aspecto concreto, no un porcentaje.
 */
object ComparadorCiclos {

    /**
     * Margen por debajo del cual dos evaluaciones se consideran equivalentes.
     * Sobre una escala normalizada a [0,1], un cambio inferior a cinco
     * centésimas no es interpretable como mejoría ni como empeoramiento: es
     * la variabilidad propia del instrumento, y presentarlo como un cambio
     * real induciría a error.
     */
    private const val MARGEN_ESTABILIDAD = 0.05

    enum class Tendencia { MEJORA, ESTABLE, EMPEORA }

    data class Dimension(val nombre: String, val antes: Double, val ahora: Double) {
        /** Variación en la dirección «a peor». La Ilusión se invierte fuera. */
        val delta: Double get() = ahora - antes
    }

    data class Resultado(
        val tendencia: Tendencia,
        /** Dimensión con la variación más acusada, o null si todo estable. */
        val dimensionDestacada: Dimension?,
        val mejoraLaDestacada: Boolean,
    )

    /**
     * @param globalAntes  puntuación normalizada de la evaluación anterior.
     * @param globalAhora  puntuación normalizada de la actual.
     * @param dimensiones  medias por dimensión, ya orientadas de modo que
     *        un valor mayor signifique siempre «peor».
     */
    fun comparar(
        globalAntes: Double,
        globalAhora: Double,
        dimensiones: List<Dimension>,
    ): Resultado {
        val diff = globalAhora - globalAntes
        val tendencia = when {
            diff < -MARGEN_ESTABILIDAD -> Tendencia.MEJORA
            diff > MARGEN_ESTABILIDAD -> Tendencia.EMPEORA
            else -> Tendencia.ESTABLE
        }
        val destacada = dimensiones
            .filter { kotlin.math.abs(it.delta) > MARGEN_ESTABILIDAD }
            .maxByOrNull { kotlin.math.abs(it.delta) }
        return Resultado(
            tendencia = tendencia,
            dimensionDestacada = destacada,
            mejoraLaDestacada = (destacada?.delta ?: 0.0) < 0
        )
    }
}
