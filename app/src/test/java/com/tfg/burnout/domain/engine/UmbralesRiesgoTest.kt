package com.tfg.burnout.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Bandas del índice: qué es bueno y qué es malo (bloque «Score general»). */
class UmbralesRiesgoTest {

    @Test
    fun `las bandas cubren todo el rango sin solapes`() {
        assertEquals(UmbralesRiesgo.Banda.BUENO, UmbralesRiesgo.bandaDe(0.0))
        assertEquals(UmbralesRiesgo.Banda.BUENO, UmbralesRiesgo.bandaDe(0.34))
        assertEquals(UmbralesRiesgo.Banda.INTERMEDIO, UmbralesRiesgo.bandaDe(0.35))
        assertEquals(UmbralesRiesgo.Banda.INTERMEDIO, UmbralesRiesgo.bandaDe(0.59))
        assertEquals(UmbralesRiesgo.Banda.CUIDARSE, UmbralesRiesgo.bandaDe(0.60))
        assertEquals(UmbralesRiesgo.Banda.CUIDARSE, UmbralesRiesgo.bandaDe(1.0))
    }
}
