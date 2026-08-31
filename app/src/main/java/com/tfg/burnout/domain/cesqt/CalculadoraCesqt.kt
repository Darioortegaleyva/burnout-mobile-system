package com.tfg.burnout.domain.cesqt

/**
 * Resultado del cuestionario CESQT una vez respondido.
 *
 * @param mediasPorDimension media (0–4) de cada dimensión.
 * @param scoreGlobalNormalizado puntuación combinada normalizada a [0,1] que
 *        alimenta el término Score_CESQT de la ecuación de riesgo (§2.2.5).
 * @param subscoreCulpa media (0–4) de la dimensión Culpa, usada como
 *        disparador del Perfil 2 (§2.2.7). NO se promedia en el global.
 */
data class ResultadoCesqt(
    val mediasPorDimension: Map<DimensionCesqt, Double>,
    val scoreGlobalNormalizado: Double,
    val subscoreCulpa: Double
)

/**
 * Calcula las puntuaciones del CESQT a partir de las respuestas crudas.
 *
 * Reglas (coherentes con §2.2.2):
 *  - Ilusión por el trabajo es una escala INVERSA: una puntuación baja indica
 *    MÁS burnout, por lo que se invierte (4 − x) antes de combinar.
 *  - La dimensión Culpa NO entra en el score global, pero se devuelve aparte
 *    como diferenciador clínico del Perfil 2.
 *  - El global se normaliza dividiendo entre 4 (rango máximo de la escala).
 */
object CalculadoraCesqt {

    /**
     * @param respuestas mapa idÍtem(1..20) -> valor 0..4
     */
    fun calcular(respuestas: Map<Int, Int>): ResultadoCesqt {
        require(respuestas.size == 20) { "Faltan respuestas: se esperan 20." }

        val medias = DimensionCesqt.entries.associateWith { dim ->
            val itemsDim = CatalogoCesqt.itemsDe(dim)
            val suma = itemsDim.sumOf { item ->
                val v = respuestas[item.id]
                    ?: error("Falta respuesta del ítem ${item.id}")
                // Ilusión es inversa: 4 − v para que "alto" siempre signifique más burnout
                if (dim.inversa) (4 - v) else v
            }
            suma.toDouble() / itemsDim.size
        }

        // El global combina las tres dimensiones de carga (Ilusión invertida,
        // Desgaste e Indolencia). Culpa queda fuera del promedio global.
        val dimsGlobales = listOf(
            DimensionCesqt.ILUSION_POR_TRABAJO,
            DimensionCesqt.DESGASTE_PSIQUICO,
            DimensionCesqt.INDOLENCIA
        )
        val mediaGlobal0a4 = dimsGlobales.map { medias.getValue(it) }.average()
        val globalNormalizado = (mediaGlobal0a4 / 4.0).coerceIn(0.0, 1.0)

        return ResultadoCesqt(
            mediasPorDimension = medias,
            scoreGlobalNormalizado = globalNormalizado,
            subscoreCulpa = medias.getValue(DimensionCesqt.CULPA)
        )
    }
}
