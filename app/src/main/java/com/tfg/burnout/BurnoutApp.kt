package com.tfg.burnout

import android.app.Application
import com.tfg.burnout.data.healthconnect.HealthConnectManager
import com.tfg.burnout.data.ia.AjustesIa
import com.tfg.burnout.data.ia.AlmacenModelo
import com.tfg.burnout.data.ia.ReformuladorLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.tfg.burnout.data.local.AppDatabase
import com.tfg.burnout.data.repository.BurnoutRepository
import com.tfg.burnout.domain.engine.GestorCoping
import com.tfg.burnout.domain.engine.ModuloEticoRuteo
import com.tfg.burnout.work.SchedulerConfig

/**
 * Clase Application. Hace de "contenedor" de dependencias mediante inyección
 * manual (suficiente y didáctico para un TFG; en producción se usaría Hilt).
 */
class BurnoutApp : Application() {

    val appScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.obtener(this, appScope) }
    val healthConnect by lazy { HealthConnectManager(this) }

    val repository by lazy {
        BurnoutRepository(
            usuarioDao = database.usuarioDao(),
            cesqtDao = database.cesqtDao(),
            biometriaDao = database.biometriaDao(),
            metaDao = database.metaDao(),
            recomendacionDao = database.recomendacionDao(),
            healthConnect = healthConnect
        )
    }

    val gestorCoping by lazy { GestorCoping() }
    val reformulador by lazy { ReformuladorLocal(this) }
    val moduloEtico by lazy { ModuloEticoRuteo(database.sedeCopDao()) }

    override fun onCreate() {
        super.onCreate()

        // Ninguna de las tareas de arranque es imprescindible para que la
        // aplicación funcione: el aprovisionamiento del modelo es opcional,
        // y la planificación en segundo plano puede rehacerse más adelante.
        // Se aíslan por tanto de modo que un fallo en cualquiera de ellas
        // —por ejemplo, restricciones del fabricante sobre el planificador—
        // no impida arrancar. Una aplicación de bienestar que no llega ni a
        // abrirse es el peor resultado posible.

        // Modelo integrado de serie (§2.3.6): si la compilación trae el
        // .task en assets, se copia en segundo plano y la IA queda lista
        // sin que el usuario haga nada.
        appScope.launch(Dispatchers.IO) {
            runCatching {
                if (AlmacenModelo.asegurarDesdeAssets(this@BurnoutApp)) {
                    AjustesIa.activarPorDefectoSiPrimeraVez(this@BurnoutApp)
                }
            }
        }

        // Lectura nocturna, canal de notificación y recordatorio periódico.
        runCatching { SchedulerConfig.programarLecturaNocturna(this) }
        runCatching { com.tfg.burnout.work.NotificadorCuestionario.crearCanal(this) }
        runCatching { SchedulerConfig.programarRecordatorioCuestionario(this) }
    }
}
