package com.tfg.burnout.domain.cesqt

import android.content.Context

/**
 * BORRADOR DE UN CUESTIONARIO A MEDIAS.
 *
 * Un cuestionario de veinte ítems no se responde siempre de una sentada: el
 * usuario puede recibir una llamada, cambiar de aplicación o cerrarla sin
 * querer. Si al volver hubiera de empezar de cero, la probabilidad de que
 * abandone definitivamente sería alta, y con ella se perdería la evaluación
 * de ese ciclo completo.
 *
 * Por eso las respuestas se van guardando conforme se emiten y el asistente
 * ofrece retomar la evaluación donde se dejó. Se emplea almacenamiento de
 * preferencias y no la base de datos porque se trata de un estado transitorio
 * y no de un dato de salud consolidado: solo cuando el cuestionario se
 * completa se calcula la puntuación y se persiste con su marca temporal.
 */
object BorradorCuestionario {

    private const val PREFS = "borrador_cesqt"
    private const val K_RESPUESTAS = "respuestas"
    private const val K_INDICE = "indice"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Guarda el avance actual. Formato: "idItem:valor;idItem:valor;…". */
    fun guardar(context: Context, respuestas: Map<Int, Int>, indiceActual: Int) {
        val serializado = respuestas.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs(context).edit()
            .putString(K_RESPUESTAS, serializado)
            .putInt(K_INDICE, indiceActual)
            .apply()
    }

    /** Respuestas guardadas, o mapa vacío si no hay borrador. */
    fun respuestas(context: Context): Map<Int, Int> {
        val s = prefs(context).getString(K_RESPUESTAS, null) ?: return emptyMap()
        return s.split(";").mapNotNull { par ->
            val partes = par.split(":")
            val id = partes.getOrNull(0)?.toIntOrNull()
            val valor = partes.getOrNull(1)?.toIntOrNull()
            if (id != null && valor != null) id to valor else null
        }.toMap()
    }

    fun indice(context: Context): Int = prefs(context).getInt(K_INDICE, 0)

    /** ¿Hay una evaluación a medias que merezca la pena retomar? */
    fun hayBorrador(context: Context): Boolean = respuestas(context).isNotEmpty()

    fun limpiar(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
