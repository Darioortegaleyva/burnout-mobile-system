package com.tfg.burnout.ui.devices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Semáforo de sincronización (Tabla 8, §6.4). La memoria documenta TRES
 * estados con semántica basada en datos; hasta ahora el código decidía el
 * color solo por los permisos, de modo que el verde podía convivir con «sin
 * datos» en las tres métricas y ÁMBAR era inalcanzable. Estos tests fijan la
 * regla documentada para que no vuelva a divergir.
 */
class EstadoSincronizacionTest {

    private val hoy = 20_000L   // día epoch arbitrario; solo importan las distancias

    @Test
    fun `sin permisos el semaforo es gris aunque haya lectura fresca`() {
        assertEquals(
            EstadoSync.GRIS,
            estadoDeSincronizacion(tienePermisos = false, fechaUltimaEpochDay = hoy, hoyEpochDay = hoy)
        )
    }

    @Test
    fun `con lectura de hoy o de ayer el semaforo es verde`() {
        assertEquals(
            EstadoSync.VERDE,
            estadoDeSincronizacion(tienePermisos = true, fechaUltimaEpochDay = hoy, hoyEpochDay = hoy)
        )
        // La lectura de ayer es el caso NORMAL: el trabajador nocturno
        // consolida a las 03:00 la noche anterior (§2.2.6).
        assertEquals(
            EstadoSync.VERDE,
            estadoDeSincronizacion(tienePermisos = true, fechaUltimaEpochDay = hoy - 1, hoyEpochDay = hoy)
        )
    }

    @Test
    fun `con permisos pero sin ninguna lectura el semaforo es ambar`() {
        // Es el caso que antes salía verde contradiciendo a «sin datos».
        assertEquals(
            EstadoSync.AMBAR,
            estadoDeSincronizacion(tienePermisos = true, fechaUltimaEpochDay = null, hoyEpochDay = hoy)
        )
    }

    @Test
    fun `con permisos y lectura vieja el semaforo es ambar`() {
        assertEquals(
            EstadoSync.AMBAR,
            estadoDeSincronizacion(tienePermisos = true, fechaUltimaEpochDay = hoy - 2, hoyEpochDay = hoy)
        )
        assertEquals(
            EstadoSync.AMBAR,
            estadoDeSincronizacion(tienePermisos = true, fechaUltimaEpochDay = hoy - 30, hoyEpochDay = hoy)
        )
    }

    @Test
    fun `los tres estados son alcanzables`() {
        val alcanzados = setOf(
            estadoDeSincronizacion(false, hoy, hoy),
            estadoDeSincronizacion(true, hoy, hoy),
            estadoDeSincronizacion(true, null, hoy)
        )
        assertEquals(EstadoSync.entries.toSet(), alcanzados)
    }

    @Test
    fun `el mensaje de ambar sugiere la accion de la tabla`() {
        val m = mensajeDe(EstadoSync.AMBAR).lowercase()
        assertTrue("Debería invitar a abrir la app del reloj o a actualizar: $m",
            m.contains("abre la app") || m.contains("actualizar"))
        // Y no puede seguir afirmando que están llegando datos.
        assertTrue(!m.contains("recibiendo datos"))
    }
}
