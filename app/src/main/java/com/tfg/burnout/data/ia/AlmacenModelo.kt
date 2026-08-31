package com.tfg.burnout.data.ia

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * ALMACÉN DEL MODELO LOCAL (§2.3.6).
 *
 * El fichero .task (p. ej. Gemma 3 1B cuantizado, ~529 MB) no se distribuye
 * dentro del APK: su licencia exige aceptación individual y su tamaño lo
 * desaconseja. El usuario lo importa UNA vez desde el almacenamiento (SAF) y
 * queda en el directorio privado de la app; a partir de ahí, todo offline.
 */
object AlmacenModelo {

    private fun fichero(context: Context) =
        File(File(context.filesDir, "modelos").apply { mkdirs() }, "modelo_local.task")

    fun ruta(context: Context): String = fichero(context).absolutePath

    /** Hay modelo importado (umbral mínimo para descartar ficheros basura). */
    fun disponible(context: Context): Boolean =
        fichero(context).let { it.exists() && it.length() > 50L * 1024 * 1024 }

    fun tamanoMb(context: Context): Int =
        (fichero(context).length() / (1024 * 1024)).toInt()

    /** Copia el .task elegido por el usuario al directorio privado. */
    fun importar(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            fichero(context).outputStream().use { salida -> entrada.copyTo(salida) }
        } != null
    }.getOrDefault(false)

    fun eliminar(context: Context) { fichero(context).delete() }

    /**
     * APROVISIONAMIENTO AUTOMÁTICO (§2.3.6): si la compilación incluye el
     * modelo en assets/ (lo coloca el desarrollador una única vez, tras
     * aceptar la licencia del modelo), se copia al directorio privado en el
     * primer arranque y la IA queda operativa SIN NINGUNA acción del
     * usuario. Devuelve true si el modelo quedó disponible.
     */
    private const val ASSET = "modelo_local.task"

    fun asegurarDesdeAssets(context: Context): Boolean {
        if (disponible(context)) return true
        return runCatching {
            val nombres = context.assets.list("") ?: emptyArray()
            if (ASSET !in nombres) return false
            context.assets.open(ASSET).use { entrada ->
                fichero(context).outputStream().use { salida -> entrada.copyTo(salida) }
            }
            disponible(context)
        }.getOrDefault(false)
    }
}

/** Preferencia simple (no sensible) que activa o desactiva la IA local. */
object AjustesIa {
    private const val PREFS = "ajustes_ia"; private const val CLAVE = "activada"
    private const val INIT = "config_inicial"

    fun activada(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLAVE, false)

    /** Al aprovisionar el modelo integrado, la IA se activa por defecto UNA
     *  sola vez; después manda siempre el interruptor del usuario. */
    fun activarPorDefectoSiPrimeraVez(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(INIT, false)) {
            prefs.edit().putBoolean(CLAVE, true).putBoolean(INIT, true).apply()
        }
    }
    fun setActivada(context: Context, valor: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(CLAVE, valor).apply()
    }
}
