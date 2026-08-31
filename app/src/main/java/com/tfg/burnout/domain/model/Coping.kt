package com.tfg.burnout.domain.model

/**
 * Categorías del catálogo de pautas de mitigación (§2.3.2).
 */
enum class CategoriaCoping(val etiqueta: String) {
    HIGIENE_SUENO("Higiene del sueño y desconexión digital"),
    MINDFULNESS_ACT("Mindfulness y Terapia de Aceptación y Compromiso"),
    REESTRUCTURACION("Reestructuración cognitiva y resiliencia"),
    APOYO_SOCIAL("Apoyo social y reglas de oro")
}

/**
 * Una pauta concreta del catálogo. La base de datos alberga un repositorio
 * amplio dentro de cada categoría para evitar la habituación (§2.3.2).
 */
data class PautaCoping(
    val id: Long,
    val categoria: CategoriaCoping,
    val titulo: String,
    val descripcion: String
)

/**
 * Los cuatro escenarios de la Matriz de Categorización (§2.3.4), que cruzan
 * el resultado subjetivo (CESQT) con el objetivo (biometría).
 */
enum class Escenario(val etiqueta: String) {
    A_OPTIMO("A — Óptimo (test BIEN / biometría BIEN)"),
    B_ACTITUDINAL("B — Deterioro actitudinal (test MAL / biometría BIEN)"),
    C_PRESENTISMO("C — Presentismo invisible (test BIEN / biometría MAL)"),
    D_SEVERO("D — Desgaste sistémico severo (test MAL / biometría MAL)")
}
