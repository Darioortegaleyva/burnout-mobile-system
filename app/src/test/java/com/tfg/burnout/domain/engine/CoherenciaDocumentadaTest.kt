package com.tfg.burnout.domain.engine

import com.tfg.burnout.domain.cesqt.CatalogoCesqt
import com.tfg.burnout.domain.cesqt.DimensionCesqt
import com.tfg.burnout.domain.cesqt.FrecuenciaCesqt
import com.tfg.burnout.domain.model.Escenario
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ata el código a lo que documenta la memoria, en los puntos donde ambos
 * habían empezado a llevar cuentas separadas. No comprueba comportamiento
 * nuevo: comprueba que no vuelva a haber dos fuentes de verdad.
 */
class CoherenciaDocumentadaTest {

    @Test
    fun `el CESQT reparte los veinte items como declara cada dimension`() {
        assertEquals(20, CatalogoCesqt.items.size)
        assertEquals(20, DimensionCesqt.entries.sumOf { it.numItems })
        DimensionCesqt.entries.forEach { dim ->
            assertEquals(
                "Reparto de ${dim.etiqueta}",
                dim.numItems,
                CatalogoCesqt.itemsDe(dim).size
            )
        }
    }

    @Test
    fun `el reparto documentado es cinco cuatro seis cinco`() {
        assertEquals(5, DimensionCesqt.ILUSION_POR_TRABAJO.numItems)
        assertEquals(4, DimensionCesqt.DESGASTE_PSIQUICO.numItems)
        assertEquals(6, DimensionCesqt.INDOLENCIA.numItems)
        assertEquals(5, DimensionCesqt.CULPA.numItems)
    }

    @Test
    fun `la escala de respuesta tiene cinco grados de cero a cuatro`() {
        assertEquals(5, FrecuenciaCesqt.entries.size)
        assertEquals(listOf(0, 1, 2, 3, 4), FrecuenciaCesqt.entries.map { it.valor })
    }

    @Test
    fun `las bandas del indice usan las etiquetas documentadas`() {
        assertEquals("Buen momento", UmbralesRiesgo.Banda.BUENO.etiqueta)
        assertEquals("Vas haciendo camino", UmbralesRiesgo.Banda.INTERMEDIO.etiqueta)
        assertEquals("Hoy toca cuidarse", UmbralesRiesgo.Banda.CUIDARSE.etiqueta)
    }

    @Test
    fun `el gestor de coping corta en los umbrales documentados`() {
        // Con el constructor por defecto: los cortes deben ser exactamente
        // CESQT_DESFAVORABLE y BIOMETRIA_DESFAVORABLE, no unos literales
        // repetidos que puedan alejarse de la memoria.
        val gestor = GestorCoping()
        val cesqt = UmbralesRiesgo.CESQT_DESFAVORABLE
        val bio = UmbralesRiesgo.BIOMETRIA_DESFAVORABLE

        // Justo en el corte ya cuenta como desfavorable (comparación >=).
        assertEquals(Escenario.B_ACTITUDINAL, gestor.clasificar(cesqt, bio - 0.01))
        assertEquals(Escenario.A_OPTIMO, gestor.clasificar(cesqt - 0.01, bio - 0.01))
        assertEquals(Escenario.C_PRESENTISMO, gestor.clasificar(cesqt - 0.01, bio))
        assertEquals(Escenario.D_SEVERO, gestor.clasificar(cesqt, bio))
    }
}
