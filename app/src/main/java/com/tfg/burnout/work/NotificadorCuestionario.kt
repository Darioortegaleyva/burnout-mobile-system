package com.tfg.burnout.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tfg.burnout.MainActivity
import com.tfg.burnout.R

/**
 * NOTIFICACIÓN DE REEVALUACIÓN (Tarea 2).
 *
 * Único aviso que emite el sistema, y deliberadamente sobrio: ni alarmista ni
 * insistente, coherente con el principio de no generar enganche (§4.2). Al
 * pulsarla, el usuario aterriza en la pestaña del asistente con la invitación
 * ya escrita (Tarea 3); nunca se le abre el cuestionario sin preguntar.
 */
object NotificadorCuestionario {

    const val CANAL_ID = "recordatorio_evaluacion"
    const val EXTRA_ABRIR_CHAT = "abrir_chat_cuestionario"
    private const val ID_NOTIFICACION = 1001

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Recordatorios de evaluación",
                // IMPORTANCE_DEFAULT: aparece, pero sin sonido intrusivo ni
                // interrupción de pantalla completa.
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Aviso mensual para repasar cómo te encuentras."
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(canal)
        }
    }

    /** Lanza el recordatorio. Silencioso si el usuario no dio permiso. */
    fun mostrar(context: Context) {
        crearCanal(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ABRIR_CHAT, true)
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¿Repasamos cómo vas?")
            .setContentText("Ha pasado un mes desde tu última evaluación.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Ha pasado un mes desde tu última evaluación. " +
                    "Cuando te venga bien, lo miramos juntos: son unos minutos."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(ID_NOTIFICACION, notificacion)
        }   // sin permiso (Android 13+) lanza SecurityException: se ignora en silencio
    }
}
