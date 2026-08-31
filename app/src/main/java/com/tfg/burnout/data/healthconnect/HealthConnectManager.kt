package com.tfg.burnout.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tfg.burnout.domain.model.LecturaBiometrica
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Fachada sobre la API Google Health Connect (§2.2.3, §4.4).
 *
 * La app NUNCA habla por Bluetooth con el reloj: lee del almacén centralizado
 * de Health Connect, donde las apps de fabricante (Zepp, Samsung Health...)
 * vuelcan los datos. Esto materializa el agnosticismo de hardware.
 */
class HealthConnectManager(private val context: Context) {

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * PERMISOS DE ESCRITURA — SOLO PARA PRUEBAS SIN WEARABLE (build debug).
     * Permiten insertar una "noche simulada" en Health Connect y validar el
     * flujo completo (lectura → motor → índice → UI) sin el reloj físico.
     * La release no los solicita ni los declara.
     */
    val permisosEscrituraPrueba = setOf(
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(RestingHeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class),
    )

    /**
     * Inserta en Health Connect una noche simulada verosímil (sueño 23:40–07:15,
     * FC reposo 58 bpm, RMSSD 42 ms) fechada ayer→hoy. Incluye RMSSD a
     * propósito: permite probar el cálculo completo del índice, aunque Zepp
     * no exporte ese dato en la cadena real (limitación documentada).
     */
    suspend fun insertarNocheDePrueba(): Boolean = runCatching {
        val zona = java.time.ZoneId.systemDefault()
        val hoy = java.time.LocalDate.now()
        val inicioSueno = hoy.minusDays(1).atTime(23, 40).atZone(zona)
        val finSueno = hoy.atTime(7, 15).atZone(zona)
        val despertar = hoy.atTime(7, 20).atZone(zona)
        client.insertRecords(
            listOf(
                SleepSessionRecord(
                    startTime = inicioSueno.toInstant(),
                    startZoneOffset = inicioSueno.offset,
                    endTime = finSueno.toInstant(),
                    endZoneOffset = finSueno.offset,
                ),
                RestingHeartRateRecord(
                    time = despertar.toInstant(),
                    zoneOffset = despertar.offset,
                    beatsPerMinute = 58,
                ),
                HeartRateVariabilityRmssdRecord(
                    time = despertar.toInstant(),
                    zoneOffset = despertar.offset,
                    heartRateVariabilityMillis = 42.0,
                ),
            )
        )
        true
    }.getOrDefault(false)

    /**
     * Aplicaciones que han escrito datos de salud en los últimos días
     * (Tarea 5). Health Connect expone el paquete de origen de cada registro;
     * es la única visibilidad disponible sobre "qué dispositivo" alimenta la
     * cadena, ya que la API no da acceso al wearable en sí.
     */
    suspend fun fuentesDetectadas(dias: Long = 14): List<String> = runCatching {
        val fin = java.time.Instant.now()
        val inicio = fin.minus(java.time.Duration.ofDays(dias))
        val filtro = TimeRangeFilter.between(inicio, fin)
        val orígenes = mutableSetOf<String>()
        client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = filtro)
        ).records.forEach { orígenes += it.metadata.dataOrigin.packageName }
        client.readRecords(
            ReadRecordsRequest(RestingHeartRateRecord::class, timeRangeFilter = filtro)
        ).records.forEach { orígenes += it.metadata.dataOrigin.packageName }
        orígenes.filter { it.isNotBlank() }.sorted()
    }.getOrDefault(emptyList())

    /**
     * Inserta SIETE noches consecutivas para poder demostrar el índice
     * multimodal completo sin esperar una semana real (solo compilación
     * debug).
     *
     * Las seis primeras describen un descanso estable —que es lo que forma la
     * línea base individual— y la última reproduce una noche claramente peor:
     * menos sueño, variabilidad caída y pulso en reposo elevado. Así la
     * desviación existe y las tres ramas biométricas contribuyen al índice,
     * que es justo lo que no puede verse con una sola noche.
     */
    suspend fun insertarSemanaDePrueba(): Boolean = runCatching {
        // Primero se limpian los registros de prueba anteriores: Health
        // Connect permite a cada aplicación borrar lo que ella misma escribió,
        // y sin esta limpieza las noches simuladas se acumularían unas sobre
        // otras generando totales imposibles.
        borrarDatosDePruebaPropios()
        val zona = java.time.ZoneId.systemDefault()
        val registros = mutableListOf<androidx.health.connect.client.records.Record>()

        // Noches 7 a 2: patrón habitual con pequeñas variaciones naturales.
        val minutosHabituales = listOf(455, 470, 448, 462, 440, 458)
        val rmssdHabitual = listOf(42.0, 45.0, 40.0, 44.0, 41.0, 43.0)
        val rhrHabitual = listOf(58, 57, 59, 58, 60, 57)

        for (i in 0 until 6) {
            val dia = java.time.LocalDate.now().minusDays((6 - i).toLong())
            val inicio = dia.minusDays(1).atTime(23, 40).atZone(zona)
            val fin = inicio.plusMinutes(minutosHabituales[i].toLong())
            val despertar = fin.plusMinutes(5)
            registros += SleepSessionRecord(
                startTime = inicio.toInstant(), startZoneOffset = inicio.offset,
                endTime = fin.toInstant(), endZoneOffset = fin.offset
            )
            registros += RestingHeartRateRecord(
                time = despertar.toInstant(), zoneOffset = despertar.offset,
                beatsPerMinute = rhrHabitual[i].toLong()
            )
            registros += HeartRateVariabilityRmssdRecord(
                time = despertar.toInstant(), zoneOffset = despertar.offset,
                heartRateVariabilityMillis = rmssdHabitual[i]
            )
        }

        // Última noche: claramente peor que su propia base.
        val hoy = java.time.LocalDate.now()
        // Se acuesta de madrugada y se levanta pronto: 5 h 05 min de sueño,
        // muy por debajo de sus ~7 h 35 habituales. Ambos extremos deben caer
        // en el MISMO día natural; si el inicio se fechara la víspera, la
        // sesión abarcaría más de un día completo, superaría el límite de
        // cordura de la lectura y se descartaría, además de contaminar la
        // ventana de la noche anterior.
        val inicioMala = hoy.atTime(1, 15).atZone(zona)
        val finMala = hoy.atTime(6, 20).atZone(zona)
        val despertarMala = finMala.plusMinutes(5)
        registros += SleepSessionRecord(
            startTime = inicioMala.toInstant(), startZoneOffset = inicioMala.offset,
            endTime = finMala.toInstant(), endZoneOffset = finMala.offset
        )
        registros += RestingHeartRateRecord(
            time = despertarMala.toInstant(), zoneOffset = despertarMala.offset,
            beatsPerMinute = 67L                       // ~9 lpm por encima de su base
        )
        registros += HeartRateVariabilityRmssdRecord(
            time = despertarMala.toInstant(), zoneOffset = despertarMala.offset,
            heartRateVariabilityMillis = 27.0          // caída marcada frente a ~42
        )

        client.insertRecords(registros)
        true
    }.getOrDefault(false)

    /**
     * Borra los registros que ESTA aplicación escribió en Health Connect
     * durante los últimos 30 días (solo pruebas: la app en producción nunca
     * escribe). Health Connect no permite borrar datos de otras apps, de modo
     * que los del wearable real quedan intactos.
     */
    suspend fun borrarDatosDePruebaPropios(dias: Long = 30) {
        runCatching {
            val fin = java.time.Instant.now().plus(java.time.Duration.ofDays(1))
            val inicio = fin.minus(java.time.Duration.ofDays(dias + 1))
            val filtro = TimeRangeFilter.between(inicio, fin)
            client.deleteRecords(SleepSessionRecord::class, filtro)
            client.deleteRecords(RestingHeartRateRecord::class, filtro)
            client.deleteRecords(HeartRateVariabilityRmssdRecord::class, filtro)
        }
    }

    /** Permisos de SOLO LECTURA que necesita la app (coherente con el Manifest). */
    val permisos = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    )

    /** ¿Está disponible Health Connect en este dispositivo? */
    fun disponibilidad(): Int =
        HealthConnectClient.getSdkStatus(context)

    suspend fun tienePermisos(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(permisos)

    /**
     * Lee la biometría consolidada de una jornada (ventana nocturna del día
     * indicado). El RMSSD se toma de la ventana de sueño para evitar artefactos
     * de actividad diurna (§2.2.6).
     */
    suspend fun leerJornada(dia: LocalDate): LecturaBiometrica {
        val zona = ZoneId.systemDefault()
        val inicio = dia.minusDays(1).atTime(20, 0).atZone(zona).toInstant() // tarde anterior
        val fin = dia.atTime(12, 0).atZone(zona).toInstant()                  // mediodía

        val tst = leerTstMin(inicio, fin)
        val rmssd = leerRmssdMedio(inicio, fin)
        val rhr = leerRhrMedio(inicio, fin)

        return LecturaBiometrica(
            fechaEpochDay = dia.toEpochDay(),
            rmssdMs = rmssd,
            tstMin = tst,
            rhrBpm = rhr
        )
    }

    private suspend fun leerTstMin(inicio: Instant, fin: Instant): Double? {
        val req = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(inicio, fin)
        )
        val sesiones = client.readRecords(req).records
        if (sesiones.isEmpty()) return null

        // Las sesiones NO se suman sin más: hay que fusionar los intervalos
        // que se solapan. Una misma noche puede estar registrada en varios
        // tramos (despertares) y, sobre todo, dos aplicaciones distintas
        // pueden haber escrito la misma noche por duplicado. Sumar a ciegas
        // produciría totales imposibles (más de veinticuatro horas de sueño).
        val intervalos = sesiones
            .map { it.startTime to it.endTime }
            .sortedBy { it.first }

        var totalMin = 0L
        var inicioTramo = intervalos.first().first
        var finTramo = intervalos.first().second

        for ((ini, f) in intervalos.drop(1)) {
            if (ini <= finTramo) {
                // Solapado o contiguo: se extiende el tramo en curso.
                if (f.isAfter(finTramo)) finTramo = f
            } else {
                totalMin += ChronoUnit.MINUTES.between(inicioTramo, finTramo)
                inicioTramo = ini
                finTramo = f
            }
        }
        totalMin += ChronoUnit.MINUTES.between(inicioTramo, finTramo)

        // Salvaguarda final: un valor por encima de las 16 h indica datos
        // corruptos o duplicados irreconciliables; mejor descartarlo que
        // envenenar la línea base.
        return if (totalMin in 1..960) totalMin.toDouble() else null
    }

    private suspend fun leerRmssdMedio(inicio: Instant, fin: Instant): Double? {
        val req = ReadRecordsRequest(
            recordType = HeartRateVariabilityRmssdRecord::class,
            timeRangeFilter = TimeRangeFilter.between(inicio, fin)
        )
        val registros = client.readRecords(req).records
        if (registros.isEmpty()) return null
        return registros.map { it.heartRateVariabilityMillis }.average()
    }

    private suspend fun leerRhrMedio(inicio: Instant, fin: Instant): Double? {
        val req = ReadRecordsRequest(
            recordType = RestingHeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(inicio, fin)
        )
        val registros = client.readRecords(req).records
        if (registros.isEmpty()) return null
        return registros.map { it.beatsPerMinute.toDouble() }.average()
    }
}
