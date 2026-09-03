package com.tfg.burnout.domain.engine

import com.tfg.burnout.data.local.dao.SedeCopDao
import com.tfg.burnout.data.local.entity.SedeCopEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las dos denominaciones de la línea 024.
 *
 * El recurso es el mismo por los dos caminos —filtro de crisis y derivación
 * por puntuación—, pero el encabezado no puede serlo: un CESQT alto describe
 * un burnout severo, que no es una crisis, y abrir ahí con el término clínico
 * desborda lo que el sistema ha medido. Lo que NO puede cambiar entre ambos
 * es el acceso: mismo teléfono y misma web.
 */
class LineaCrisisTest {

    private val daoVacio = object : SedeCopDao {
        override suspend fun insertarTodas(sedes: List<SedeCopEntity>) = Unit
        override suspend fun contar(): Int = 0
        override suspend fun buscarPorProvincia(provincia: String): SedeCopEntity? = null
        override suspend fun listarProvincias(): List<String> = emptyList()
    }

    private val recurso = ModuloEticoRuteo(daoVacio).lineaCrisis()

    @Test
    fun `el acceso al recurso es identico por los dos caminos`() {
        assertEquals("024", recurso.telefono)
        assertEquals("https://www.sanidad.gob.es/linea024/home.htm", recurso.web)
        // Ambas denominaciones nombran la misma línea, para que quien la haya
        // visto por un camino la reconozca por el otro.
        assertTrue(recurso.nombre.contains("024"))
        assertTrue(recurso.nombreNeutro.contains("024"))
    }

    @Test
    fun `la denominacion explicita se reserva al camino de crisis`() {
        assertTrue(
            "el camino de crisis mantiene la formulación explícita",
            recurso.nombre.contains("conducta suicida", ignoreCase = true)
        )
    }

    @Test
    fun `la denominacion de derivacion no encabeza con el termino clinico`() {
        assertFalse(
            "la derivación por puntuación no debe nombrar la conducta suicida",
            recurso.nombreNeutro.contains("suicid", ignoreCase = true)
        )
        // Se presenta por lo que la hace útil: gratuita y siempre disponible.
        assertTrue(recurso.nombreNeutro.contains("gratuita", ignoreCase = true))
        assertTrue(recurso.nombreNeutro.contains("veinticuatro horas", ignoreCase = true))
    }
}
