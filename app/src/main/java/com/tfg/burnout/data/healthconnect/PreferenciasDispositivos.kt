package com.tfg.burnout.data.healthconnect

import android.content.Context

/**
 * CONFIGURACIÓN DE FUENTES Y MÉTRICAS (Tarea 5).
 *
 * Health Connect no permite controlar el wearable directamente: la app solo
 * ve el ORIGEN de cada registro (el paquete de la aplicación que lo escribió).
 * Sobre esa base, este panel ofrece al usuario dos controles reales:
 *
 *  1. Qué métricas quiere que se lean (granularidad del consentimiento: puede
 *     compartir el sueño y no el pulso, por ejemplo). Es minimización de
 *     datos llevada a la práctica (RGPD art. 5.1.c).
 *  2. Qué fuente tiene prioridad si varias aplicaciones escriben el mismo
 *     dato, evitando duplicidades y lecturas contradictorias.
 */
object PreferenciasDispositivos {

    private const val PREFS = "dispositivos"
    private const val K_SUENO = "leer_sueno"
    private const val K_FC = "leer_fc"
    private const val K_HRV = "leer_hrv"
    private const val K_FUENTE = "fuente_prioritaria"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Por defecto se leen todas: el consentimiento global ya se ha dado, y
    // estos interruptores sirven para restringir, no para ampliar.
    fun leerSueno(c: Context) = prefs(c).getBoolean(K_SUENO, true)
    fun leerFc(c: Context) = prefs(c).getBoolean(K_FC, true)
    fun leerHrv(c: Context) = prefs(c).getBoolean(K_HRV, true)

    fun setLeerSueno(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_SUENO, v).apply()
    fun setLeerFc(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_FC, v).apply()
    fun setLeerHrv(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_HRV, v).apply()

    /** Paquete de la app cuya lectura prevalece; null = cualquiera. */
    fun fuentePrioritaria(c: Context): String? = prefs(c).getString(K_FUENTE, null)
    fun setFuentePrioritaria(c: Context, paquete: String?) =
        prefs(c).edit().putString(K_FUENTE, paquete).apply()

    /** Nombre legible de las aplicaciones de origen más habituales. */
    fun nombreLegible(paquete: String): String = when {
        paquete.contains("zepp", true) || paquete.contains("huami", true) -> "Zepp (Amazfit)"
        paquete.contains("samsung", true) -> "Samsung Health"
        paquete.contains("fitbit", true) -> "Fitbit"
        paquete.contains("garmin", true) -> "Garmin Connect"
        paquete.contains("oura", true) -> "Oura"
        paquete.contains("xiaomi", true) || paquete.contains("mi.health", true) -> "Mi Fitness"
        paquete.contains("google", true) -> "Google Fit"
        paquete.contains("tfg.burnout", true) -> "Datos de prueba (esta app)"
        else -> paquete.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
}
