package com.tfg.burnout.domain.engine

import com.tfg.burnout.domain.model.Escenario
import com.tfg.burnout.domain.model.IndiceRiesgo
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Verifica el comportamiento del generador de mensajes por plantillas:
 * (1) elige mensajes del bloque correcto según el escenario,
 * (2) produce VARIACIÓN (no repite el último mensaje si hay alternativas),
 * (3) adapta el escenario C al componente biométrico dominante.
 */
class GeneradorMensajesTest {

    private fun indice(comp: IndiceRiesgo.Componente) =
        IndiceRiesgo(r = 0.6, energia = 40, componenteDominante = comp)

    @Test
    fun `no repite el ultimo mensaje mostrado cuando hay alternativas`() {
        // Semilla fija para reproducibilidad del test.
        val gen = GeneradorMensajes(Random(42))
        val primero = gen.mensajeDeAfrontamiento(
            Escenario.B_ACTITUDINAL, indice(IndiceRiesgo.Componente.PSICOMETRICO)
        )
        val segundo = gen.mensajeDeAfrontamiento(
            Escenario.B_ACTITUDINAL, indice(IndiceRiesgo.Componente.PSICOMETRICO),
            evitarTexto = primero.texto
        )
        assertNotEquals(primero.texto, segundo.texto)
    }

    @Test
    fun `el escenario A devuelve mensajes de refuerzo positivo`() {
        val gen = GeneradorMensajes(Random(1))
        val m = gen.mensajeDeAfrontamiento(
            Escenario.A_OPTIMO, indice(IndiceRiesgo.Componente.PSICOMETRICO)
        )
        assertTrue(m.texto.isNotBlank())
        assertTrue(m.fuente.isNotBlank())
    }

    @Test
    fun `el escenario C se adapta al componente de sueno`() {
        val gen = GeneradorMensajes(Random(7))
        val m = gen.mensajeDeAfrontamiento(
            Escenario.C_PRESENTISMO, indice(IndiceRiesgo.Componente.SUENO)
        )
        // El mensaje de sueño menciona descanso/pantallas/dormir.
        val t = m.texto.lowercase()
        assertTrue(
            t.contains("descans") || t.contains("pantalla") ||
            t.contains("sueño") || t.contains("acostar")
        )
    }

    @Test
    fun `hay variedad real recorriendo muchas extracciones`() {
        val gen = GeneradorMensajes(Random(123))
        val vistos = mutableSetOf<String>()
        var ultimo: String? = null
        repeat(20) {
            val m = gen.mensajeDeAfrontamiento(
                Escenario.B_ACTITUDINAL,
                indice(IndiceRiesgo.Componente.PSICOMETRICO),
                evitarTexto = ultimo
            )
            vistos += m.texto
            ultimo = m.texto
        }
        // El banco B tiene 4 frases: deberían aparecer al menos 2 distintas.
        assertTrue("Debería haber variación de mensajes", vistos.size >= 2)
    }
}

class ModulacionContextoTest {
    private fun indice() = com.tfg.burnout.domain.model.IndiceRiesgo(
        r = 0.6, energia = 40,
        componenteDominante = com.tfg.burnout.domain.model.IndiceRiesgo.Componente.PSICOMETRICO
    )

    @Test
    fun `con poca autonomia y mucha carga se priorizan pautas de limites`() {
        val gen = GeneradorMensajes(Random(5))
        val perfil = com.tfg.burnout.domain.model.PerfilContexto(carga = 3, autonomia = 1, apoyo = 2)
        repeat(6) {
            val m = gen.mensajeDeAfrontamiento(Escenario.B_ACTITUDINAL, indice(), perfil = perfil)
            assertTrue("Esperaba tema LIMITES", m.tema == GeneradorMensajes.Tema.LIMITES)
        }
    }

    @Test
    fun `con poco apoyo social se prioriza la pauta de apoyo`() {
        val gen = GeneradorMensajes(Random(9))
        val perfil = com.tfg.burnout.domain.model.PerfilContexto(carga = 2, autonomia = 2, apoyo = 1)
        val m = gen.mensajeDeAfrontamiento(Escenario.B_ACTITUDINAL, indice(), perfil = perfil)
        assertTrue(m.tema == GeneradorMensajes.Tema.APOYO)
    }

    @Test
    fun `la desculpabilizacion nombra la carga cuando esta es alta`() {
        val gen = GeneradorMensajes(Random(3))
        val perfil = com.tfg.burnout.domain.model.PerfilContexto(carga = 3, autonomia = 2, apoyo = 2)
        val m = gen.mensajeDesculpabilizador(perfil = perfil)
        assertTrue(m.texto.contains("carga"))
    }
}

class AcusesValenciaTest {
    private val gen = GeneradorMensajes(Random(11))
    private val D = com.tfg.burnout.domain.cesqt.DimensionCesqt.DESGASTE_PSIQUICO
    private val I = com.tfg.burnout.domain.cesqt.DimensionCesqt.ILUSION_POR_TRABAJO

    @Test
    fun `ilusion alta es valencia positiva y desgaste alto preocupante`() {
        assertTrue(gen.valenciaDe(I, 4) == GeneradorMensajes.Valencia.POSITIVA)
        assertTrue(gen.valenciaDe(I, 0) == GeneradorMensajes.Valencia.PREOCUPANTE)
        assertTrue(gen.valenciaDe(D, 4) == GeneradorMensajes.Valencia.PREOCUPANTE)
        assertTrue(gen.valenciaDe(D, 0) == GeneradorMensajes.Valencia.POSITIVA)
        assertTrue(gen.valenciaDe(D, 2) == GeneradorMensajes.Valencia.NEUTRA)
    }

    @Test
    fun `los acuses varian y no repiten el anterior`() {
        val a = gen.acuseCesqt(D, 4)
        val b = gen.acuseCesqt(D, 4, evitarTexto = a.texto)
        assertNotEquals(a.texto, b.texto)
    }

    @Test
    fun `la respuesta EMA de un buen dia no habla de sentirse mal`() {
        val m = gen.respuestaEma(diaBueno = true, exigenciasAltas = false)
        assertTrue(!m.texto.contains("normal sentirse así"))
    }
}
