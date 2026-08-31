package com.tfg.burnout.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tfg.burnout.BurnoutApp
import java.time.LocalDate

/**
 * Comprueba una vez al día si toca la reevaluación (Tarea 2).
 *
 * No hace nada más: si el ciclo de cuatro semanas (§2.2.6) no se ha cumplido,
 * termina en silencio. Un solo aviso por ciclo, sin insistir a diario: si el
 * usuario lo ignora, el recordatorio no vuelve hasta pasada una semana, para
 * no convertir la app en una fuente de presión.
 */
class RecordatorioCuestionarioWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? BurnoutApp ?: return Result.success()
        return runCatching {
            val usuario = app.repository.obtenerUsuario()
            // Sin consentimiento aceptado no se notifica nada.
            if (usuario?.consentimientoAceptado != true) return Result.success()

            val ultima = app.repository.fechaUltimaEvaluacion()
            val hoy = LocalDate.now().toEpochDay()
            val toca = ultima == null || (hoy - ultima) >= DIAS_CICLO

            if (toca && PreferenciasRecordatorio.puedeAvisarHoy(applicationContext, hoy)) {
                NotificadorCuestionario.mostrar(applicationContext)
                PreferenciasRecordatorio.registrarAviso(applicationContext, hoy)
            }
            Result.success()
        }.getOrElse { Result.success() }   // nunca reintentar: no es crítico
    }

    companion object {
        const val NOMBRE_TRABAJO = "recordatorio_cuestionario"
        /** Ciclo de reevaluación en días (§2.2.6). */
        const val DIAS_CICLO = com.tfg.burnout.domain.engine.ConfiguracionRitmos.CICLO_CUESTIONARIO_DIAS
        /** Días mínimos entre dos avisos consecutivos. */
        const val DIAS_ENTRE_AVISOS = com.tfg.burnout.domain.engine.ConfiguracionRitmos.DIAS_ENTRE_RECORDATORIOS
    }
}

/** Memoria del último aviso, para no repetirlo a diario. */
object PreferenciasRecordatorio {
    private const val PREFS = "recordatorios"
    private const val CLAVE_ULTIMO = "ultimo_aviso_epoch_day"

    fun puedeAvisarHoy(context: Context, hoyEpochDay: Long): Boolean {
        val ultimo = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(CLAVE_ULTIMO, 0L)
        return ultimo == 0L ||
            (hoyEpochDay - ultimo) >= RecordatorioCuestionarioWorker.DIAS_ENTRE_AVISOS
    }

    fun registrarAviso(context: Context, hoyEpochDay: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(CLAVE_ULTIMO, hoyEpochDay).apply()
    }
}
