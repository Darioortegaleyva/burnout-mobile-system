package com.tfg.burnout.domain.engine

import com.tfg.burnout.domain.model.CategoriaCoping
import com.tfg.burnout.domain.model.Escenario
import com.tfg.burnout.domain.model.IndiceRiesgo
import com.tfg.burnout.domain.model.PautaCoping

/**
 * GESTOR DE ESTRATEGIAS DE COPING (§2.3.4, §4.3).
 *
 * Clasifica al usuario en uno de los cuatro escenarios cruzando el plano
 * subjetivo (CESQT) con el objetivo (biometría) y selecciona el bloque de
 * pautas pertinente. El escenario D activa, además, la derivación profesional
 * a través del ModuloEticoRuteo.
 */
class GestorCoping(
    // Los cortes viven en UmbralesRiesgo, que es donde están documentados y
    // razonados; aquí solo se consumen. Antes se repetían sus valores como
    // literales, de modo que las constantes quedaban sin usar y podían
    // divergir de la memoria sin que nada lo advirtiera.
    private val umbralCesqt: Double = UmbralesRiesgo.CESQT_DESFAVORABLE,
    private val umbralBiometria: Double = UmbralesRiesgo.BIOMETRIA_DESFAVORABLE
) {

    /**
     * Determina el escenario.
     *
     * @param scoreCesqt   puntuación psicométrica normalizada [0,1].
     * @param cargaBiometrica desviación biométrica agregada [0,1] (combinación
     *        de ΔRMSSD y ΔTST). Puede ser null si no hay datos frescos.
     */
    fun clasificar(scoreCesqt: Double, cargaBiometrica: Double?): Escenario {
        val testMal = scoreCesqt >= umbralCesqt
        // Si no hay biometría, asumimos "BIEN" objetivo de forma conservadora
        // (no inventamos un riesgo físico que no podemos medir).
        val bioMal = (cargaBiometrica ?: 0.0) >= umbralBiometria

        return when {
            !testMal && !bioMal -> Escenario.A_OPTIMO
            testMal && !bioMal  -> Escenario.B_ACTITUDINAL
            !testMal && bioMal  -> Escenario.C_PRESENTISMO
            else                -> Escenario.D_SEVERO
        }
    }

    /**
     * Categorías priorizadas teniendo en cuenta TAMBIÉN qué componente del
     * índice está peor.
     *
     * El escenario A–D fija la estrategia general, pero no dice cuál de las
     * ramas biométricas está tirando del riesgo. Si el sueño es el término
     * dominante, ofrecer solo desactivación y apoyo social deja fuera
     * precisamente lo que más falta hace, y el usuario lo percibe como
     * incoherente: «he dormido fatal toda la semana, ¿y no me dices nada del
     * sueño?». Esta versión antepone la categoría que corresponde al
     * componente dominante sin alterar el resto de la estrategia.
     */
    fun categoriasPriorizadas(
        escenario: Escenario,
        componenteDominante: IndiceRiesgo.Componente?
    ): List<CategoriaCoping> {
        val base = categoriasPriorizadas(escenario)
        val porDominante = when (componenteDominante) {
            IndiceRiesgo.Componente.SUENO -> CategoriaCoping.HIGIENE_SUENO
            IndiceRiesgo.Componente.HRV -> CategoriaCoping.MINDFULNESS_ACT
            else -> null
        } ?: return base
        // En el escenario óptimo no se sugiere nada: no hay que arreglar nada.
        if (escenario == Escenario.A_OPTIMO) return base
        return (listOf(porDominante) + base).distinct()
    }

    fun categoriasPriorizadas(escenario: Escenario): List<CategoriaCoping> = when (escenario) {
        Escenario.A_OPTIMO -> emptyList() // refuerzo positivo, sin saturar
        Escenario.B_ACTITUDINAL -> listOf(
            CategoriaCoping.REESTRUCTURACION,
            CategoriaCoping.APOYO_SOCIAL
        )
        Escenario.C_PRESENTISMO -> listOf(
            CategoriaCoping.HIGIENE_SUENO,
            CategoriaCoping.MINDFULNESS_ACT
        )
        Escenario.D_SEVERO -> listOf(
            CategoriaCoping.MINDFULNESS_ACT,   // desactivación fisiológica de urgencia
            CategoriaCoping.APOYO_SOCIAL
        )
    }

    /** ¿Debe activarse la cláusula de derivación automática? */
    fun requiereDerivacion(escenario: Escenario, subscoreCulpa: Double, umbralCulpa: Double = UmbralesRiesgo.CULPA_CRITICA): Boolean {
        // Se deriva si el escenario es severo o si la dimensión Culpa, por sí
        // sola, cruza el umbral crítico del Perfil 2 (§2.2.7).
        return escenario == Escenario.D_SEVERO || subscoreCulpa >= umbralCulpa
    }
}
