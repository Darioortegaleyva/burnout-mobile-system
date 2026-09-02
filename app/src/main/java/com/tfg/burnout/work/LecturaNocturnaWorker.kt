package com.tfg.burnout.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tfg.burnout.BurnoutApp

/**
 * CRON SCHEDULER — Worker que se ejecuta de madrugada (§2.2.6, §4.3).
 *
 * Responsabilidades:
 *   1) Consolidar la biometría de la jornada desde Health Connect.
 *   2) Recalcular la media móvil de la línea base.
 *   3) (Mensualmente) refrescar el Índice de Riesgo R sobre la ventana de 4
 *      semanas. El recálculo diario se evita para no generar falsos positivos.
 *
 * Se implementa como CoroutineWorker porque las operaciones de Health Connect
 * y Room son suspendidas.
 */
class LecturaNocturnaWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = (applicationContext as BurnoutApp).repository

            // 1 + 2: lectura biométrica y actualización de línea base.
            //
            // Se releen los ÚLTIMOS DÍAS, no solo hoy. El worker corre a las
            // 03:00, cuando la persona todavía está durmiendo: la sesión de
            // sueño de esa noche no está cerrada y la aplicación del reloj no
            // la habrá volcado a Health Connect hasta el despertar. Leyendo
            // únicamente `hoy` se consolidaba una jornada vacía que ya nunca
            // se revisaba, porque ninguna otra ruta automática vuelve sobre un
            // día pasado —la relectura de varios días solo estaba cableada al
            // botón de modo desarrollo—. Con la ventana corta, la noche entra
            // en la ejecución de la madrugada siguiente y la clave primaria
            // por día evita duplicados.
            repo.sincronizarUltimosDias(DIAS_A_RELEER)

            // 3: el recálculo del índice global lo decide el repositorio según
            // la ventana temporal; aquí simplemente lo invocamos y persistimos.
            repo.calcularIndiceActual()

            Result.success()
        } catch (e: Exception) {
            // Reintentar más tarde si, p. ej., Health Connect no respondió.
            Result.retry()
        }
    }

    companion object {
        const val NOMBRE_TRABAJO = "lectura_nocturna_biometrica"

        /**
         * Ventana de relectura. Tres días cubren la noche en curso, la última
         * ya cerrada y un día de margen para un reloj que sincronice tarde,
         * sin llegar a reescribir histórico antiguo en cada ejecución.
         */
        private const val DIAS_A_RELEER = 3
    }
}
