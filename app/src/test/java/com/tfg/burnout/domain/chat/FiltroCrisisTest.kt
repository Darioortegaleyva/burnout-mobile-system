package com.tfg.burnout.domain.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Barrera de seguridad (§7.4). Se comprueba que detecta las expresiones
 * críticas —incluso sin tildes y con distinta capitalización— y que no
 * dispara con conversación ordinaria, para no interrumpir sin motivo.
 */
class FiltroCrisisTest {

    @Test
    fun `detecta crisis aunque falten tildes o sobren mayusculas`() {
        assertEquals(FiltroCrisis.Nivel.CRISIS, FiltroCrisis.evaluar("no quiero vivir mas"))
        assertEquals(FiltroCrisis.Nivel.CRISIS, FiltroCrisis.evaluar("NO QUIERO SEGUIR VIVIENDO"))
        assertEquals(FiltroCrisis.Nivel.CRISIS, FiltroCrisis.evaluar("pienso en quitarme la vida"))
    }

    @Test
    fun `detecta malestar intenso sin confundirlo con crisis`() {
        assertEquals(
            FiltroCrisis.Nivel.MALESTAR_INTENSO,
            FiltroCrisis.evaluar("tengo mucha ansiedad ultimamente")
        )
        assertEquals(
            FiltroCrisis.Nivel.MALESTAR_INTENSO,
            FiltroCrisis.evaluar("estoy hundido con el trabajo")
        )
    }

    @Test
    fun `no dispara con conversacion normal`() {
        assertEquals(FiltroCrisis.Nivel.NINGUNO, FiltroCrisis.evaluar("¿cómo puedo dormir mejor?"))
        assertEquals(FiltroCrisis.Nivel.NINGUNO, FiltroCrisis.evaluar("qué es el burnout"))
        assertEquals(FiltroCrisis.Nivel.NINGUNO, FiltroCrisis.evaluar("hoy me ha ido bien el día"))
    }
}
