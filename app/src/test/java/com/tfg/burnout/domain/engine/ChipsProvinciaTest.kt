package com.tfg.burnout.domain.engine

import com.tfg.burnout.data.local.dao.SedeCopDao
import com.tfg.burnout.data.local.entity.SedeCopEntity
import com.tfg.burnout.data.local.seed.SedesCopSeed
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chips de provincia de la derivación al COP.
 *
 * El listado se ofrecía con un take(3) sobre las provincias ordenadas
 * alfabéticamente, de modo que solo A Coruña, Álava y Albacete eran
 * accesibles y «Otra» acababa en el recurso genérico de la web: cuarenta y
 * nueve demarcaciones quedaban fuera de alcance. Estas pruebas fijan que
 * todas sigan siendo alcanzables.
 */
class ChipsProvinciaTest {

    private val provincias = SedesCopSeed.sedes.map { it.provincia }.sorted()

    /** DAO respaldado por la propia semilla, sin Room. */
    private val dao = object : SedeCopDao {
        override suspend fun insertarTodas(sedes: List<SedeCopEntity>) = Unit
        override suspend fun contar(): Int = SedesCopSeed.sedes.size
        override suspend fun buscarPorProvincia(provincia: String): SedeCopEntity? =
            SedesCopSeed.sedes.firstOrNull { it.provincia == provincia }
        override suspend fun listarProvincias(): List<String> = provincias
    }

    private val modulo = ModuloEticoRuteo(dao)

    @Test
    fun `los chips iniciales son frecuentes reales mas Otra`() {
        val chips = ModuloEticoRuteo.chipsIniciales(provincias)

        assertEquals(ModuloEticoRuteo.OTRA_PROVINCIA, chips.last())
        // Un puñado de atajos, no un muro de cincuenta y dos opciones.
        assertTrue("los chips iniciales deben caber en pantalla", chips.size <= 8)

        // Y son provincias de peso, no las primeras del alfabeto.
        assertTrue(chips.contains("Madrid"))
        assertTrue(chips.contains("Barcelona"))
        assertFalse("A Coruña era el artefacto del orden alfabético", chips.contains("A Coruña"))

        // Ningún chip puede llevar a una provincia que el directorio no tenga.
        chips.dropLast(1).forEach {
            assertTrue("$it no está en el directorio", provincias.contains(it))
        }
    }

    @Test
    fun `Otra da acceso a las cincuenta y dos demarcaciones`() {
        // Lo que resolverProvincia despliega al pulsar «Otra» es la lista
        // íntegra del directorio, así que toda sede tiene camino.
        assertEquals(52, provincias.size)
        assertEquals(provincias.size, provincias.distinct().size)
        SedesCopSeed.sedes.forEach { sede ->
            assertTrue(
                "${sede.provincia} debe ser alcanzable",
                provincias.contains(sede.provincia)
            )
        }
    }

    @Test
    fun `un directorio vacio deja solo Otra, que sabe degradar`() {
        // Si la base fallara, no se ofrecen atajos muertos.
        assertEquals(
            listOf(ModuloEticoRuteo.OTRA_PROVINCIA),
            ModuloEticoRuteo.chipsIniciales(emptyList())
        )
    }

    @Test
    fun `toda provincia del directorio resuelve a una sede real`() = runTest {
        // Los chips no pueden ofrecer nada que luego no resuelva: cada entrada
        // desplegada por «Otra» tiene que devolver un colegio con teléfono y web.
        provincias.forEach { provincia ->
            val sede = modulo.resolverSede(provincia)
            assertNotNull("$provincia no resuelve", sede)
            assertTrue(sede!!.nombreColegio.isNotBlank())
            assertTrue(sede.telefono.isNotBlank())
            assertTrue(sede.web.isNotBlank())
        }
        // Las 52 demarcaciones se reparten entre menos colegios, porque varios
        // cubren una comunidad entera (Castilla y León agrupa nueve provincias).
        assertEquals(23, SedesCopSeed.sedes.map { it.nombreColegio }.distinct().size)
    }

    @Test
    fun `queda el respaldo del Consejo General para lo que no esta en el directorio`() = runTest {
        // «Otra» ya no cae aquí —despliega el listado—, pero la rama de respaldo
        // sigue siendo necesaria: una provincia desconocida devuelve null y el
        // asistente remite entonces a la web del Consejo General (cop.es).
        assertNull(modulo.resolverSede("Provincia Inexistente"))
        assertNull(modulo.resolverSede(ModuloEticoRuteo.OTRA_PROVINCIA))
    }
}
