package com.tfg.burnout.domain.cesqt

import com.tfg.burnout.domain.engine.ComparadorCiclos
import com.tfg.burnout.domain.engine.GestorCoping
import com.tfg.burnout.domain.engine.MotorRiesgo
import com.tfg.burnout.domain.engine.UmbralesRiesgo
import com.tfg.burnout.domain.model.Escenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * BATERÍA SISTEMÁTICA DE PUNTUACIÓN E INTERPRETACIÓN DEL CESQT.
 *
 * CalculadoraCesqtTest cubre las reglas una a una. Esta batería hace otra
 * cosa: recorre patrones COMPLETOS de respuesta de extremo a extremo y deja
 * por escrito qué puntuación y qué interpretación produce el sistema para
 * cada uno, imprimiendo la tabla junto a las comprobaciones. La tabla es
 * parte del resultado: sirve para contrastar el comportamiento real contra
 * los baremos de la memoria sin tener que reconstruirlo a mano.
 *
 * Recorrido: puntuación (CalculadoraCesqt) → índice de riesgo (MotorRiesgo,
 * degradado a la rama psicométrica por ausencia de biometría) → banda
 * cualitativa (UmbralesRiesgo) → escenario y derivación (GestorCoping) →
 * comparación entre ciclos (ComparadorCiclos).
 *
 * Sin biometría, el motor reparte el peso entre las ramas disponibles y solo
 * queda la psicométrica, de modo que R coincide con el score global. Por eso
 * la banda que aquí se tabula es la que vería un usuario que ha respondido el
 * cuestionario y no tiene pulsera vinculada.
 */
class BateriaPuntuacionCesqtTest {

    private val motor = MotorRiesgo()
    private val gestor = GestorCoping()

    // ─────────────────────────────────────────────────────────────────
    // Utilidades
    // ─────────────────────────────────────────────────────────────────

    /** Mapa de las 20 respuestas con un valor fijo por dimensión. */
    private fun respuestas(
        ilusion: Int, desgaste: Int, indolencia: Int, culpa: Int
    ): MutableMap<Int, Int> = CatalogoCesqt.items.associate { item ->
        item.id to when (item.dimension) {
            DimensionCesqt.ILUSION_POR_TRABAJO -> ilusion
            DimensionCesqt.DESGASTE_PSIQUICO -> desgaste
            DimensionCesqt.INDOLENCIA -> indolencia
            DimensionCesqt.CULPA -> culpa
        }
    }.toMutableMap()

    /**
     * Reparte [suma] puntos entre los cinco ítems de Culpa, tan repartidos
     * como permita la escala 0–4. Necesario para explorar el entorno del
     * corte: con cinco ítems la media solo puede tomar múltiplos de 0,2.
     */
    private fun conSumaCulpa(base: MutableMap<Int, Int>, suma: Int): Map<Int, Int> {
        val items = CatalogoCesqt.itemsDe(DimensionCesqt.CULPA)
        require(suma in 0..(items.size * 4))
        var resto = suma
        items.forEach { item ->
            val v = minOf(4, resto)
            base[item.id] = v
            resto -= v
        }
        return base
    }

    /** Todo lo que el sistema deduce de un patrón de respuestas. */
    private data class Lectura(
        val resultado: ResultadoCesqt,
        val r: Double,
        val energia: Int,
        val banda: UmbralesRiesgo.Banda,
        val escenario: Escenario,
        val deriva: Boolean
    ) {
        fun media(dim: DimensionCesqt) = resultado.mediasPorDimension.getValue(dim)
    }

    private fun leer(respuestas: Map<Int, Int>): Lectura {
        val res = CalculadoraCesqt.calcular(respuestas)
        // Sin biometría: el motor degrada a la rama psicométrica.
        val indice = motor.calcular(res.scoreGlobalNormalizado, null, null)
        val escenario = gestor.clasificar(res.scoreGlobalNormalizado, cargaBiometrica = null)
        return Lectura(
            resultado = res,
            r = indice.r,
            energia = indice.energia,
            banda = UmbralesRiesgo.bandaDe(indice.r),
            escenario = escenario,
            deriva = gestor.requiereDerivacion(escenario, res.subscoreCulpa)
        )
    }

    private fun d(x: Double) = String.format(Locale.ROOT, "%5.2f", x)

    private fun cabecera(titulo: String) {
        println()
        println("── $titulo ".padEnd(104, '─'))
        println(
            "patrón (Ilu/Des/Ind/Cul)".padEnd(26) +
                "Ilu*".padStart(6) + "Des".padStart(7) + "Ind".padStart(7) + "Cul".padStart(7) +
                "  │ " + "global".padStart(6) + "R".padStart(7) + "    E" +
                "   " + "banda".padEnd(20) + "escenario".padEnd(16) + "deriva"
        )
    }

    private fun fila(etiqueta: String, l: Lectura) {
        println(
            etiqueta.padEnd(26) +
                d(l.media(DimensionCesqt.ILUSION_POR_TRABAJO)).padStart(6) +
                d(l.media(DimensionCesqt.DESGASTE_PSIQUICO)).padStart(7) +
                d(l.media(DimensionCesqt.INDOLENCIA)).padStart(7) +
                d(l.media(DimensionCesqt.CULPA)).padStart(7) +
                "  │ " + String.format(Locale.ROOT, "%6.3f", l.resultado.scoreGlobalNormalizado) +
                String.format(Locale.ROOT, "%7.3f", l.r) +
                String.format(Locale.ROOT, "%5d", l.energia) +
                "   " + l.banda.etiqueta.padEnd(20) +
                l.escenario.name.substringBefore('_').padEnd(16) +
                (if (l.deriva) "SÍ" else "no")
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // 1) Patrones uniformes: la misma respuesta a los veinte ítems
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `patrones uniformes de todo 0 a todo 4`() {
        cabecera("1. PATRONES UNIFORMES (misma respuesta en los 20 ítems)")
        val lecturas = (0..4).map { v -> v to leer(respuestas(v, v, v, v)) }
        lecturas.forEach { (v, l) -> fila("todo $v", l) }
        println(
            "Ilu* = media de Ilusión YA INVERTIDA (4 − v): mayor = peor. " +
                "R sin biometría = score global."
        )

        // La inversión hace que ningún patrón uniforme sea un extremo: quien
        // responde 4 a todo declara a la vez desgaste máximo e ilusión máxima,
        // y quien responde 0 a todo declara calma total y ninguna ilusión.
        lecturas.forEach { (v, l) ->
            assertEquals(
                "el global de «todo $v» debe ser (4+v)/12",
                (4.0 + v) / 12.0, l.resultado.scoreGlobalNormalizado, 1e-9
            )
            assertEquals("la Culpa uniforme es v", v.toDouble(), l.resultado.subscoreCulpa, 1e-9)
        }

        // Ni el mínimo ni el máximo de la escala se alcanzan por esta vía.
        assertEquals(4.0 / 12.0, lecturas.first().second.resultado.scoreGlobalNormalizado, 1e-9)
        assertEquals(8.0 / 12.0, lecturas.last().second.resultado.scoreGlobalNormalizado, 1e-9)

        // El global es monótono creciente en v pese a que la Ilusión tira en
        // sentido contrario: dos dimensiones directas ganan a una inversa.
        val globales = lecturas.map { it.second.resultado.scoreGlobalNormalizado }
        assertEquals(globales.sorted(), globales)

        // Derivación: la manda la Culpa (corte 2,5), no el escenario, porque
        // sin biometría nunca se llega al escenario D.
        assertEquals(
            listOf(false, false, false, true, true),
            lecturas.map { it.second.deriva }
        )
        assertTrue(lecturas.none { it.second.escenario == Escenario.D_SEVERO })
    }

    // ─────────────────────────────────────────────────────────────────
    // 2) Extremos reales y patrones con una sola dimensión alta
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `extremos reales y dimensiones aisladas`() {
        cabecera("2. EXTREMOS REALES Y UNA SOLA DIMENSIÓN ALTA")

        // Polo sano: ilusión máxima (4) y el resto a cero.
        val minimo = leer(respuestas(4, 0, 0, 0))
        val maximo = leer(respuestas(0, 4, 4, 4))
        val soloDesgaste = leer(respuestas(4, 4, 0, 0))
        val soloIndolencia = leer(respuestas(4, 0, 4, 0))
        val soloSinIlusion = leer(respuestas(0, 0, 0, 0))
        val soloCulpa = leer(respuestas(4, 0, 0, 4))

        fila("mínimo (4/0/0/0)", minimo)
        fila("solo desgaste (4/4/0/0)", soloDesgaste)
        fila("solo indolencia (4/0/4/0)", soloIndolencia)
        fila("sin ilusión (0/0/0/0)", soloSinIlusion)
        fila("solo culpa (4/0/0/4)", soloCulpa)
        fila("máximo (0/4/4/4)", maximo)

        // Los extremos verdaderos requieren orientar cada dimensión a su polo.
        assertEquals(0.0, minimo.resultado.scoreGlobalNormalizado, 1e-9)
        assertEquals(1.0, maximo.resultado.scoreGlobalNormalizado, 1e-9)

        // INVERSIÓN DE ILUSIÓN, EN LOS DOS SENTIDOS.
        // Sentido 1: ilusión máxima (respuesta 4) no aporta nada al burnout.
        assertEquals(0.0, minimo.media(DimensionCesqt.ILUSION_POR_TRABAJO), 1e-9)
        // Sentido 2: ilusión nula (respuesta 0) aporta el máximo.
        assertEquals(4.0, soloSinIlusion.media(DimensionCesqt.ILUSION_POR_TRABAJO), 1e-9)
        // Y perder la ilusión pesa lo mismo que ganar desgaste o indolencia:
        // las tres son ramas de igual peso en el promedio global.
        assertEquals(
            soloDesgaste.resultado.scoreGlobalNormalizado,
            soloSinIlusion.resultado.scoreGlobalNormalizado, 1e-9
        )
        assertEquals(
            soloIndolencia.resultado.scoreGlobalNormalizado,
            soloSinIlusion.resultado.scoreGlobalNormalizado, 1e-9
        )
        assertEquals(1.0 / 3.0, soloDesgaste.resultado.scoreGlobalNormalizado, 1e-9)

        // CULPA: fuera del promedio global, disparador por su cuenta.
        // Mismo global que el mínimo absoluto —cero— y aun así deriva.
        assertEquals(
            minimo.resultado.scoreGlobalNormalizado,
            soloCulpa.resultado.scoreGlobalNormalizado, 1e-9
        )
        assertEquals(0.0, soloCulpa.resultado.scoreGlobalNormalizado, 1e-9)
        assertEquals(UmbralesRiesgo.Banda.BUENO, soloCulpa.banda)
        assertEquals(Escenario.A_OPTIMO, soloCulpa.escenario)
        assertTrue("la Culpa debe derivar por sí sola", soloCulpa.deriva)
        assertFalse("sin culpa, el mismo global no deriva", minimo.deriva)
    }

    // ─────────────────────────────────────────────────────────────────
    // 3) El corte de Culpa, ítem a ítem
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `entorno del corte de Culpa`() {
        println()
        println("── 3. CORTE DE CULPA (CULPA_CRITICA = ${UmbralesRiesgo.CULPA_CRITICA}) ".padEnd(104, '─'))
        println(
            "suma Culpa (0–20)".padEnd(20) + "media".padStart(7) +
                "   global".padStart(10) + "   escenario".padEnd(18) + "deriva"
        )

        // Base sana en las tres dimensiones del global: lo único que se mueve
        // es la Culpa, que no entra en él.
        val filas = (10..14).map { suma ->
            val l = leer(conSumaCulpa(respuestas(4, 0, 0, 0), suma))
            println(
                "$suma".padEnd(20) + d(l.resultado.subscoreCulpa).padStart(7) +
                    String.format(Locale.ROOT, "%10.3f", l.resultado.scoreGlobalNormalizado) +
                    "   " + l.escenario.name.substringBefore('_').padEnd(15) +
                    (if (l.deriva) "SÍ" else "no")
            )
            suma to l
        }
        println(
            "Con cinco ítems la media solo toma múltiplos de 0,2: el corte 2,5 " +
                "no es alcanzable y la frontera efectiva son 13 puntos (2,6)."
        )

        // El global no se mueve con la Culpa, por mucho que ésta suba.
        val globales = filas.map { it.second.resultado.scoreGlobalNormalizado }.distinct()
        assertEquals(listOf(0.0), globales)

        // La frontera cae entre 12 (2,4) y 13 (2,6).
        assertEquals(
            listOf(false, false, false, true, true),
            filas.map { it.second.deriva }
        )
        assertEquals(2.4, filas.first { it.first == 12 }.second.resultado.subscoreCulpa, 1e-9)
        assertEquals(2.6, filas.first { it.first == 13 }.second.resultado.subscoreCulpa, 1e-9)
    }

    // ─────────────────────────────────────────────────────────────────
    // 4) Comparación entre dos evaluaciones sucesivas
    // ─────────────────────────────────────────────────────────────────

    /**
     * Reproduce el cierre de ciclo (CU-04) con el MISMO cableado que usa el
     * asistente: las medias que persiste el repositorio llegan ya orientadas
     * «mayor = peor» y ComparadorCiclos.dimensionesDeCiclo no las vuelve a
     * invertir.
     */
    private fun comparar(antes: Map<Int, Int>, ahora: Map<Int, Int>): ComparadorCiclos.Resultado {
        val a = CalculadoraCesqt.calcular(antes)
        val b = CalculadoraCesqt.calcular(ahora)
        fun m(r: ResultadoCesqt, dim: DimensionCesqt) = r.mediasPorDimension.getValue(dim)
        return ComparadorCiclos.comparar(
            a.scoreGlobalNormalizado,
            b.scoreGlobalNormalizado,
            ComparadorCiclos.dimensionesDeCiclo(
                ilusion = m(a, DimensionCesqt.ILUSION_POR_TRABAJO) to
                    m(b, DimensionCesqt.ILUSION_POR_TRABAJO),
                desgaste = m(a, DimensionCesqt.DESGASTE_PSIQUICO) to
                    m(b, DimensionCesqt.DESGASTE_PSIQUICO),
                indolencia = m(a, DimensionCesqt.INDOLENCIA) to
                    m(b, DimensionCesqt.INDOLENCIA),
            )
        )
    }

    @Test
    fun `direccion de la comparacion entre ciclos`() {
        println()
        println("── 4. COMPARACIÓN ENTRE DOS EVALUACIONES SUCESIVAS ".padEnd(104, '─'))
        println(
            "caso".padEnd(30) + "antes".padEnd(14) + "ahora".padEnd(14) +
                "tendencia".padEnd(11) + "dimensión destacada".padEnd(28) + "lectura"
        )

        fun caso(
            nombre: String, antes: Map<Int, Int>, etiquetaAntes: String,
            ahora: Map<Int, Int>, etiquetaAhora: String
        ): ComparadorCiclos.Resultado {
            val r = comparar(antes, ahora)
            println(
                nombre.padEnd(30) + etiquetaAntes.padEnd(14) + etiquetaAhora.padEnd(14) +
                    r.tendencia.name.padEnd(11) +
                    (r.dimensionDestacada?.nombre ?: "—").padEnd(28) +
                    when {
                        r.dimensionDestacada == null -> "—"
                        r.mejoraLaDestacada -> "ha mejorado"
                        else -> "ha empeorado"
                    }
            )
            return r
        }

        // ILUSIÓN: el caso que importa, y el que estaba invertido.
        val ilusionCae = caso(
            "la ilusión se derrumba",
            respuestas(4, 0, 0, 0), "4/0/0/0", respuestas(0, 0, 0, 0), "0/0/0/0"
        )
        val ilusionVuelve = caso(
            "la ilusión se recupera",
            respuestas(0, 0, 0, 0), "0/0/0/0", respuestas(4, 0, 0, 0), "4/0/0/0"
        )
        val desgasteSube = caso(
            "sube el agotamiento",
            respuestas(4, 0, 0, 0), "4/0/0/0", respuestas(4, 4, 0, 0), "4/4/0/0"
        )
        val desgasteBaja = caso(
            "baja el agotamiento",
            respuestas(4, 4, 0, 0), "4/4/0/0", respuestas(4, 0, 0, 0), "4/0/0/0"
        )
        val indolenciaSube = caso(
            "sube la indolencia",
            respuestas(4, 0, 0, 0), "4/0/0/0", respuestas(4, 0, 4, 0), "4/0/4/0"
        )
        val identico = caso(
            "sin cambios",
            respuestas(2, 2, 2, 2), "2/2/2/2", respuestas(2, 2, 2, 2), "2/2/2/2"
        )
        val soloCulpa = caso(
            "solo cambia la Culpa",
            respuestas(4, 0, 0, 0), "4/0/0/0", respuestas(4, 0, 0, 4), "4/0/0/4"
        )
        // Un único ítem de Desgaste sube un grado: mueve la dimensión (0,25)
        // pero no el global (0,021), por debajo del margen de estabilidad.
        val cambioMenor = caso(
            "un ítem de desgaste +1",
            respuestas(4, 0, 0, 0), "4/0/0/0",
            respuestas(4, 0, 0, 0).also { it[CatalogoCesqt.itemsDe(DimensionCesqt.DESGASTE_PSIQUICO).first().id] = 1 },
            "4/0+/0/0"
        )
        println(
            "El margen de estabilidad (0,05 sobre el global normalizado) absorbe " +
                "los cambios de un solo ítem."
        )

        // Perder la ilusión es empeorar, y debe decirse así.
        assertEquals(ComparadorCiclos.Tendencia.EMPEORA, ilusionCae.tendencia)
        assertEquals("la ilusión", ilusionCae.dimensionDestacada?.nombre)
        assertFalse(
            "una ilusión que se derrumba no puede anunciarse como mejoría",
            ilusionCae.mejoraLaDestacada
        )

        // Y recuperarla es mejorar: la inversión funciona en ambos sentidos.
        assertEquals(ComparadorCiclos.Tendencia.MEJORA, ilusionVuelve.tendencia)
        assertEquals("la ilusión", ilusionVuelve.dimensionDestacada?.nombre)
        assertTrue(ilusionVuelve.mejoraLaDestacada)

        // Las dimensiones directas, en sus dos sentidos.
        assertEquals(ComparadorCiclos.Tendencia.EMPEORA, desgasteSube.tendencia)
        assertEquals("el agotamiento", desgasteSube.dimensionDestacada?.nombre)
        assertFalse(desgasteSube.mejoraLaDestacada)

        assertEquals(ComparadorCiclos.Tendencia.MEJORA, desgasteBaja.tendencia)
        assertEquals("el agotamiento", desgasteBaja.dimensionDestacada?.nombre)
        assertTrue(desgasteBaja.mejoraLaDestacada)

        assertEquals(ComparadorCiclos.Tendencia.EMPEORA, indolenciaSube.tendencia)
        assertEquals("la distancia con el trabajo", indolenciaSube.dimensionDestacada?.nombre)
        assertFalse(indolenciaSube.mejoraLaDestacada)

        // Sin movimiento no se inventa una lectura.
        assertEquals(ComparadorCiclos.Tendencia.ESTABLE, identico.tendencia)
        assertNull(identico.dimensionDestacada)

        // La Culpa no participa en la comparación de ciclos: no está entre las
        // tres dimensiones del global y no mueve ni la tendencia ni la destacada.
        assertEquals(ComparadorCiclos.Tendencia.ESTABLE, soloCulpa.tendencia)
        assertNull(soloCulpa.dimensionDestacada)

        // Un cambio menor deja el global estable pero sí señala la dimensión.
        assertEquals(ComparadorCiclos.Tendencia.ESTABLE, cambioMenor.tendencia)
        assertNotNull(cambioMenor.dimensionDestacada)
        assertEquals("el agotamiento", cambioMenor.dimensionDestacada?.nombre)
        assertFalse(cambioMenor.mejoraLaDestacada)
    }
}
