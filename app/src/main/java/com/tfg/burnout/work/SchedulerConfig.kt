package com.tfg.burnout.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Configura la planificación periódica de la lectura nocturna con WorkManager.
 */
object SchedulerConfig {

    /**
     * Programa el worker para que se ejecute una vez al día, de madrugada.
     * WorkManager garantiza la persistencia entre reinicios del dispositivo.
     */
    /**
     * Programa la comprobación diaria del ciclo de reevaluación (Tarea 2).
     * El worker decide si toca notificar; aquí solo se garantiza que se
     * ejecute una vez al día y que sobreviva a reinicios.
     */
    fun programarRecordatorioCuestionario(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecordatorioCuestionarioWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = java.util.concurrent.TimeUnit.DAYS
        ).setInitialDelay(6, java.util.concurrent.TimeUnit.HOURS).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RecordatorioCuestionarioWorker.NOMBRE_TRABAJO,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun programarLecturaNocturna(context: Context) {
        val constraints = Constraints.Builder()
            // No requiere red (offline-first); sí conviene que el dispositivo
            // no esté en modo de ahorro extremo para poder leer Health Connect.
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<LecturaNocturnaWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = java.util.concurrent.TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(retrasoHastaProximaMadrugada(), java.util.concurrent.TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LecturaNocturnaWorker.NOMBRE_TRABAJO,
            ExistingPeriodicWorkPolicy.KEEP,   // no reprograma si ya existe
            request
        )
    }

    /** Minutos desde ahora hasta las 03:00 de la próxima madrugada. */
    private fun retrasoHastaProximaMadrugada(): Long {
        val ahora = LocalDateTime.now()
        var objetivo = ahora.toLocalDate().atTime(LocalTime.of(3, 0))
        if (!objetivo.isAfter(ahora)) objetivo = objetivo.plusDays(1)
        return ChronoUnit.MINUTES.between(ahora, objetivo)
    }
}
