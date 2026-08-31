package com.tfg.burnout.domain.cesqt

/**
 * Modelo del Cuestionario para la Evaluación del Síndrome de Quemarse por el
 * Trabajo (CESQT) de Gil-Monte (2005).
 *
 * ─────────────────────────────────────────────────────────────────────────
 * AVISO LEGAL / ACADÉMICO — LÉEME ANTES DE TOCAR ESTE ARCHIVO
 * ─────────────────────────────────────────────────────────────────────────
 * El CESQT es un instrumento psicométrico VALIDADO Y PROTEGIDO. El texto
 * literal de sus 20 ítems NO puede incluirse en este repositorio sin la
 * licencia de uso correspondiente (distribuida por TEA Ediciones / la entidad
 * titular de los derechos).
 *
 * Por eso este archivo modela la ESTRUCTURA del cuestionario (4 dimensiones,
 * reparto de ítems, escala de respuesta 0–4 y reglas de puntuación), pero el
 * texto de cada ítem se centraliza en el objeto TextosCesqt como MARCADOR.
 *
 * CÓMO COMPLETARLO (una vez obtenida la autorización de uso):
 *   1) Solicita la licencia del CESQT (TEA Ediciones / UNIPSICO).
 *   2) Sustituye en TextosCesqt los 20 marcadores por el texto autorizado,
 *      respetando el orden de dimensiones declarado más abajo.
 *   3) No cambies la asignación de dimensiones ni el número de ítems.
 *
 * La estructura aquí reflejada (Ilusión 5 ítems inversos · Desgaste 4 ·
 * Indolencia 6 · Culpa 5) se corresponde con lo descrito en tu §2.2.2.
 * ─────────────────────────────────────────────────────────────────────────
 */

/** Las cuatro dimensiones teóricas del CESQT. */
enum class DimensionCesqt(val etiqueta: String, val numItems: Int, val inversa: Boolean) {
    ILUSION_POR_TRABAJO("Ilusión por el trabajo", 5, inversa = true),
    DESGASTE_PSIQUICO("Desgaste psíquico", 4, inversa = false),
    INDOLENCIA("Indolencia", 6, inversa = false),
    CULPA("Culpa", 5, inversa = false)
}

/**
 * Escala de respuesta de frecuencia de cinco grados (0–4) del CESQT.
 * El chatbot debe presentarla literalmente (véase §2.3.1).
 */
enum class FrecuenciaCesqt(val valor: Int, val etiqueta: String) {
    NUNCA(0, "Nunca"),
    RARAMENTE(1, "Raramente: algunas veces al año"),
    A_VECES(2, "A veces: algunas veces al mes"),
    FRECUENTEMENTE(3, "Frecuentemente: algunas veces por semana"),
    MUY_FRECUENTEMENTE(4, "Muy frecuentemente: todos los días")
}

/**
 * ÚNICO PUNTO A EDITAR cuando tengas la licencia del CESQT.
 *
 * Cada entrada es el texto LITERAL del ítem correspondiente. El orden importa:
 * los índices 0..4 son Ilusión, 5..8 Desgaste, 9..14 Indolencia, 15..19 Culpa
 * (coherente con el reparto de DimensionCesqt y con CatalogoCesqt).
 *
 * ÍTEMS PROVISIONALES DE DESARROLLO: los enunciados siguientes son
 * redacciones ORIGINALES de este proyecto, inspiradas en los constructos
 * públicos de cada dimensión (ilusión, desgaste, indolencia, culpa). NO son
 * los ítems oficiales del CESQT, que están protegidos y requieren licencia
 * (TEA Ediciones / UNIPSICO). Permiten que la app aplique el cuestionario
 * completo de extremo a extremo (flujo, escala, puntuación y perfiles) en
 * desarrollo y demostración; al obtener la licencia, basta con sustituir
 * estas 20 cadenas por los enunciados oficiales. El sistema
 * compila y funciona, pero mostrará texto provisional en lugar de los ítems.
 */
object TextosCesqt {
    /** true mientras los enunciados sean los provisionales de desarrollo. */
    const val ITEMS_PROVISIONALES = true

    val textos: List<String> = listOf(
        // Ilusión por el trabajo (5)
        "Siento ilusión por el trabajo que hago.",
        "Mi trabajo me hace sentir realizado/a.",
        "Encuentro sentido a las tareas que realizo cada día.",
        "Me motiva pensar en los proyectos que tengo entre manos.",
        "Siento que lo que hago en mi trabajo merece la pena.",
        // Desgaste psíquico (4)
        "Me siento agotado/a emocionalmente por mi trabajo.",
        "Termino la jornada sin energía.",
        "Me siento saturado/a por las exigencias de mi trabajo.",
        "Noto que el trabajo me está desgastando físicamente.",
        // Indolencia (6)
        "Trato algunos asuntos del trabajo con indiferencia.",
        "Me molesta tener que atender a algunas personas en el trabajo.",
        "Últimamente me implico menos de lo que solía en mi trabajo.",
        "Me muestro más frío/a o distante con la gente del trabajo.",
        "Hago algunas tareas de forma mecánica, sin importarme el resultado.",
        "Prefiero evitar el contacto con algunas personas de mi entorno laboral.",
        // Culpa (5)
        "Me siento culpable por alguna de mis actitudes en el trabajo.",
        "Me arrepiento de cómo he tratado a alguien en el trabajo.",
        "Pienso que merezco reproches por mi comportamiento en el trabajo.",
        "Me remuerde la conciencia por haberme desentendido de asuntos del trabajo.",
        "Me preocupa haber perdido la sensibilidad con la gente del trabajo."
    )

    init {
        require(textos.size == 20) { "TextosCesqt debe contener exactamente 20 entradas." }
    }
}

/** Un ítem del cuestionario. */
data class ItemCesqt(
    val id: Int,                 // 1..20
    val dimension: DimensionCesqt,
    val texto: String
)

/**
 * Catálogo de los 20 ítems con su dimensión y su texto (de TextosCesqt).
 */
object CatalogoCesqt {

    val items: List<ItemCesqt> = buildList {
        var idx = 0
        fun add(dim: DimensionCesqt, n: Int) {
            repeat(n) {
                add(ItemCesqt(idx + 1, dim, TextosCesqt.textos[idx]))
                idx++
            }
        }
        add(DimensionCesqt.ILUSION_POR_TRABAJO, 5)
        add(DimensionCesqt.DESGASTE_PSIQUICO, 4)
        add(DimensionCesqt.INDOLENCIA, 6)
        add(DimensionCesqt.CULPA, 5)
    }

    init {
        require(items.size == 20) { "El CESQT debe tener exactamente 20 ítems." }
    }

    fun itemsDe(dim: DimensionCesqt): List<ItemCesqt> = items.filter { it.dimension == dim }
}
