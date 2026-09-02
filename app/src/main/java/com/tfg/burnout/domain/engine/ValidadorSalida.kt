package com.tfg.burnout.domain.engine

/**
 * VALIDADOR DE SALIDA DEL MODELO LOCAL (§2.3.6).
 *
 * Última barrera antes de mostrar una reformulación generada: si la frase no
 * supera TODAS las comprobaciones, el sistema descarta la reformulación y
 * muestra la plantilla original validada. Reglas deliberadamente
 * conservadoras: en salud mental, ante la duda, plantilla.
 */
object ValidadorSalida {

    // Raíces de términos clínicos/alarmistas que una reformulación de
    // bienestar jamás debería introducir por su cuenta.
    private val prohibidas = listOf(
        "diagnos", "depres", "suicid", "trastorno", "patolog",
        "medicac", "fármaco", "farmaco", "pastilla", "enfermedad",
        "http://", "https://", "www."
    )

    fun validar(original: String, candidata: String?): Boolean {
        if (candidata.isNullOrBlank()) return false
        val c = candidata.trim()
        if (c.equals(original.trim(), ignoreCase = true)) return false
        // El tope alto se mide contra la escala real de los fragmentos
        // documentales (entre 350 y 550 caracteres): con 240 se
        // rechazaban por longitud las respuestas de dos o tres frases
        // que el propio prompt pide, y todo acababa en el respaldo.
        if (c.length < 20 || c.length > 600) return false
        if (c.length > original.length * 2.5) return false
        if (c.contains("\n\n")) return false
        val minus = c.lowercase()
        if (prohibidas.any { minus.contains(it) }) return false
        return true
    }
}
