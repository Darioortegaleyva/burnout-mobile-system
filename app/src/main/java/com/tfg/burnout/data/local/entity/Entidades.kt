package com.tfg.burnout.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Perfil técnico del usuario y su línea base individual (§4.4).
 * Se asume un único usuario local (la app es personal y offline).
 */
@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey val id: Int = 1,           // único usuario local
    val nombre: String = "",
    val provincia: String? = null,         // recordada tras una derivación
    val rmssdMedioBase: Double = 0.0,      // línea base HRV
    val tstMedioBaseMin: Double = 0.0,     // línea base sueño
    val rhrMedioBase: Double = 0.0,        // línea base FC en reposo (Tarea 6)
    val ultimaEmaEpochDay: Long = 0L,      // control de la micro-interacción diaria
    val fechaUltimoIndiceEpochDay: Long = 0L,
    val ultimoR: Double = 0.0,
    /** Consentimiento informado del onboarding (qué se mide, para qué,
     *  y que los datos no abandonan el dispositivo). RGPD art. 7. */
    val consentimientoAceptado: Boolean = false,
    /** PERFIL DE CONTEXTO LABORAL (§5.5): tres respuestas de una sola vez,
     *  inspiradas en factores del FPSICO (INSST): carga percibida, autonomía
     *  y apoyo social. Escala 1=Baja, 2=Media, 3=Alta. Null = sin responder.
     *  Modulan la selección de pautas; NO diagnostican. */
    val cargaPercibida: Int? = null,
    val autonomiaPercibida: Int? = null,
    val apoyoPercibido: Int? = null,
    /**
     * PERFIL FÍSICO OPCIONAL (Tarea 6). Solo se recoge si el usuario lo
     * autoriza expresamente; sirve para contextualizar la FC en reposo.
     * Se mantiene opcional por minimización de datos (RGPD art. 5.1.c): el
     * sistema funciona sin ellos, porque razona sobre la línea base
     * individual y no sobre baremos poblacionales.
     */
    val edad: Int? = null,
    val sexoBiologico: String? = null,     // "M", "F", "OTRO" o null
    val alturaCm: Int? = null,
    val pesoKg: Double? = null
)

/**
 * Histórico de respuestas al CESQT, indexado por marca temporal (§4.4).
 * Guardamos las puntuaciones agregadas; las respuestas ítem a ítem pueden
 * serializarse en [respuestasJson] si se desea auditoría completa.
 */
@Entity(tableName = "cesqt_response")
data class CesqtResponseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fechaEpochDay: Long,
    val scoreGlobalNormalizado: Double,
    val subscoreCulpa: Double,
    val mediaIlusion: Double,
    val mediaDesgaste: Double,
    val mediaIndolencia: Double,
    val respuestasJson: String          // {"1":3,"2":0,...}
)

/**
 * Registro biométrico diario consolidado desde Health Connect (§4.4).
 */
@Entity(tableName = "biometria")
data class BiometriaEntity(
    @PrimaryKey val fechaEpochDay: Long,
    val rmssdMs: Double?,
    val tstMin: Double?,
    val rhrBpm: Double?
)

/**
 * Directorio estático de sedes del Consejo General de la Psicología, precargado
 * desde los recursos de la app (asset seeding). Es inmutable en tiempo de
 * ejecución (§2.2.7, §4.4).
 */
@Entity(tableName = "sede_cop")
data class SedeCopEntity(
    @PrimaryKey val provincia: String,
    val nombreColegio: String,
    val telefono: String,
    val web: String
)

/**
 * Metas/objetivos negociados con el usuario (§5.2) y su seguimiento (§5.3).
 */
@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoria: String,            // CategoriaCoping.name
    val titulo: String,
    val activa: Boolean = true,
    val fechaCreacionEpochDay: Long,
    val cumplimientosJson: String = "[]"  // ["2026-06-01","2026-06-02",...]
)

/**
 * HISTORIAL DE PAUTAS RECOMENDADAS (bloque «Pautas»).
 *
 * Registra qué actividad se le ha propuesto al usuario y cuándo, para no
 * repetir siempre las mismas y poder preguntarle más adelante qué tal le fue
 * (entrada conversacional del BOT B).
 */
@Entity(tableName = "recomendacion")
data class RecomendacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pautaId: String,
    val categoria: String,
    val fechaEpochDay: Long,
    /** Valoración del usuario cuando se le pregunta: null, "BIEN", "REGULAR", "NO_LA_HICE". */
    val valoracion: String? = null
)
