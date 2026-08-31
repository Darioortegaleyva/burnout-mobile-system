package com.tfg.burnout.domain.engine

import com.tfg.burnout.domain.model.CategoriaCoping
import com.tfg.burnout.domain.model.Escenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la Matriz de Categorización (§2.3.4) y de la cláusula de
 * derivación (§2.2.7). Umbrales por defecto: CESQT 0.50, biometría 0.40,
 * Culpa 2.5.
 */
class GestorCopingTest {

    private val gestor = GestorCoping()

    @Test
    fun `escenario A optimo cuando test y biometria estan bien`() {
        assertEquals(Escenario.A_OPTIMO, gestor.clasificar(0.2, 0.1))
    }

    @Test
    fun `escenario B actitudinal cuando solo el test esta mal`() {
        assertEquals(Escenario.B_ACTITUDINAL, gestor.clasificar(0.7, 0.1))
    }

    @Test
    fun `escenario C presentismo cuando solo la biometria esta mal`() {
        assertEquals(Escenario.C_PRESENTISMO, gestor.clasificar(0.2, 0.6))
    }

    @Test
    fun `escenario D severo cuando ambos planos estan mal`() {
        assertEquals(Escenario.D_SEVERO, gestor.clasificar(0.7, 0.6))
    }

    @Test
    fun `sin biometria se asume plano objetivo BIEN de forma conservadora`() {
        // No se inventa un riesgo físico que no se puede medir.
        assertEquals(Escenario.B_ACTITUDINAL, gestor.clasificar(0.7, null))
        assertEquals(Escenario.A_OPTIMO, gestor.clasificar(0.2, null))
    }

    @Test
    fun `el escenario D siempre requiere derivacion`() {
        assertTrue(gestor.requiereDerivacion(Escenario.D_SEVERO, subscoreCulpa = 0.0))
    }

    @Test
    fun `la Culpa elevada dispara la derivacion incluso fuera del escenario D`() {
        // Perfil 2 de Gil-Monte: la Culpa cruza el umbral por sí sola.
        assertTrue(gestor.requiereDerivacion(Escenario.B_ACTITUDINAL, subscoreCulpa = 3.0))
        assertTrue(gestor.requiereDerivacion(Escenario.A_OPTIMO, subscoreCulpa = 2.5))
    }

    @Test
    fun `sin Culpa elevada ni escenario D no se deriva`() {
        assertFalse(gestor.requiereDerivacion(Escenario.A_OPTIMO, subscoreCulpa = 1.0))
        assertFalse(gestor.requiereDerivacion(Escenario.C_PRESENTISMO, subscoreCulpa = 2.0))
    }

    @Test
    fun `el escenario C prioriza la higiene del sueno`() {
        val cats = gestor.categoriasPriorizadas(Escenario.C_PRESENTISMO)
        assertEquals(CategoriaCoping.HIGIENE_SUENO, cats.first())
    }

    @Test
    fun `el escenario A no satura con pautas`() {
        assertTrue(gestor.categoriasPriorizadas(Escenario.A_OPTIMO).isEmpty())
    }
}
