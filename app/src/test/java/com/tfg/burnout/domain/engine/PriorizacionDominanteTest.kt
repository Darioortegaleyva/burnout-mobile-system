package com.tfg.burnout.domain.engine

import com.tfg.burnout.domain.model.CategoriaCoping
import com.tfg.burnout.domain.model.Escenario
import com.tfg.burnout.domain.model.IndiceRiesgo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Si el sueño es el componente que más tira del riesgo, la higiene del sueño
 * debe aparecer entre las pautas sugeridas AUNQUE el escenario sea severo:
 * lo contrario resulta incoherente para quien lleva una semana durmiendo mal.
 */
class PriorizacionDominanteTest {

    private val gestor = GestorCoping()

    @Test
    fun `escenario severo con sueno dominante incluye higiene del sueno primero`() {
        val cats = gestor.categoriasPriorizadas(
            Escenario.D_SEVERO, IndiceRiesgo.Componente.SUENO
        )
        assertEquals(CategoriaCoping.HIGIENE_SUENO, cats.first())
        assertTrue(cats.contains(CategoriaCoping.MINDFULNESS_ACT))
    }

    @Test
    fun `sin componente dominante se mantiene la lista del escenario`() {
        assertEquals(
            gestor.categoriasPriorizadas(Escenario.D_SEVERO),
            gestor.categoriasPriorizadas(Escenario.D_SEVERO, null)
        )
    }

    @Test
    fun `el escenario optimo no sugiere nada aunque haya dominante`() {
        assertTrue(
            gestor.categoriasPriorizadas(
                Escenario.A_OPTIMO, IndiceRiesgo.Componente.SUENO
            ).isEmpty()
        )
    }

    @Test
    fun `no se duplican categorias ya presentes`() {
        val cats = gestor.categoriasPriorizadas(
            Escenario.C_PRESENTISMO, IndiceRiesgo.Componente.SUENO
        )
        assertEquals(cats.size, cats.distinct().size)
    }
}
