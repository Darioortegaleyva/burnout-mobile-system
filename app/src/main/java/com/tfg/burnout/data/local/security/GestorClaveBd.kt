package com.tfg.burnout.data.local.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * GESTIÓN DE LA CLAVE DE CIFRADO DE LA BASE DE DATOS (§7.3).
 *
 * La base de datos Room se cifra con SQLCipher (AES-256). La passphrase:
 *  1. Se genera aleatoriamente (32 bytes, SecureRandom) la primera vez.
 *  2. Se guarda en EncryptedSharedPreferences, cuyo material de clave está
 *     protegido por el Android Keystore (MasterKey AES256-GCM), de modo que
 *     la passphrase nunca se almacena en claro en el dispositivo.
 *
 * Resultado: aunque se extraiga el fichero de la BD (dispositivo perdido,
 * copia del almacenamiento), los datos de salud permanecen ilegibles sin la
 * clave, que solo es accesible desde esta app en este dispositivo.
 */
object GestorClaveBd {

    private const val PREFS = "seguridad_bd"
    private const val CLAVE = "passphrase_sqlcipher"

    fun passphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context, PREFS, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existente = prefs.getString(CLAVE, null)
        if (existente != null) return existente.toByteArray(Charsets.ISO_8859_1)

        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val codificada = String(bytes, Charsets.ISO_8859_1)
        prefs.edit().putString(CLAVE, codificada).apply()
        return bytes
    }
}
