package com.tfg.burnout.domain.engine

import com.tfg.burnout.data.local.entity.SedeCopEntity
import com.tfg.burnout.data.local.dao.SedeCopDao

/**
 * Recurso de ayuda inmediata para situaciones de crisis aguda, diferenciado
 * de la derivación profesional ordinaria (que tiene horario de oficina).
 *
 * El recurso es UNO SOLO —mismo teléfono, misma web—, pero se nombra de dos
 * maneras según por dónde se llegue a él, porque los dos caminos no son
 * equivalentes:
 *
 *  · Filtro de crisis: la persona ACABA de expresar malestar agudo. Ahí el
 *    nombre explícito es informativo y necesario; nombrar la conducta
 *    suicida no la induce, y omitirla sí puede impedir que se reconozca en
 *    el recurso quien lo necesita.
 *  · Derivación por puntuación: un CESQT alto (o una Culpa que cruza el
 *    corte del Perfil 2) describe un burnout severo, que no es lo mismo que
 *    una crisis. Encabezar ahí con el término clínico resulta
 *    desproporcionado y puede leerse como un diagnóstico que el sistema no
 *    ha hecho ni puede hacer.
 *
 * La accesibilidad NO cambia entre ambos: el teléfono y la web son idénticos
 * y se ofrecen con la misma prominencia. Lo único que varía es el encabezado.
 */
data class RecursoCrisis(
    /** Denominación explícita del recurso, para el camino de crisis. */
    val nombre: String,
    /**
     * Misma línea, presentada por su disponibilidad y gratuidad en lugar de
     * por su objeto clínico. Para la derivación ordinaria.
     */
    val nombreNeutro: String,
    val telefono: String,
    val web: String
)

/**
 * MÓDULO ÉTICO DE RUTEO (§2.2.7, §4.3).
 *
 * Implementa la Cláusula de Derivación Automática. Cuando se detecta el
 * Perfil 2, NO usa GPS ni red: solicita la provincia al usuario por chat y
 * resuelve la sede del Colegio Oficial de Psicología (COP) mediante una
 * consulta LOCAL a la tabla SedesCopEntity de Room (asset seeding).
 *
 * Esto materializa el principio de minimización de datos (RGPD art. 5.1.c) y
 * el carácter offline-first del sistema.
 */
class ModuloEticoRuteo(
    private val sedeCopDao: SedeCopDao
) {
    /**
     * Devuelve la sede del COP correspondiente a una provincia.
     * @return la sede, o null si la provincia no está en el directorio local.
     */
    suspend fun resolverSede(provincia: String): SedeCopEntity? {
        return sedeCopDao.buscarPorProvincia(provincia.trim())
    }

    /** Lista de provincias disponibles para construir los quick-reply chips. */
    suspend fun provinciasDisponibles(): List<String> =
        sedeCopDao.listarProvincias()

    companion object {
        /**
         * Etiqueta del chip que despliega el directorio completo. No es una
         * provincia: quien la pulsa recibe el listado íntegro, no el recurso
         * genérico de la web.
         */
        const val OTRA_PROVINCIA = "Otra"

        /**
         * Provincias ofrecidas de entrada, por número de habitantes: resuelven
         * de un toque el caso mayoritario sin convertir la pregunta en un muro
         * de cincuenta y dos opciones. No son un privilegio de esas
         * demarcaciones —cualquiera de las restantes se alcanza igualmente a
         * través de OTRA_PROVINCIA—, solo un atajo para las más pobladas.
         */
        private val FRECUENTES = listOf(
            "Madrid", "Barcelona", "Valencia", "Sevilla", "Alicante", "Málaga"
        )

        /**
         * Chips con los que se abre la pregunta por la provincia: las
         * frecuentes que el directorio contenga de verdad, más «Otra».
         *
         * El filtro contra [disponibles] importa: si una provincia dejara de
         * estar en la semilla, su chip llevaría a la rama de «no encontrada»
         * y el usuario acabaría en la web sin entender por qué. Y si el
         * directorio viniera vacío por un fallo de la base, quedan cero
         * atajos y solo «Otra», que ya sabe degradar.
         */
        fun chipsIniciales(disponibles: List<String>): List<String> {
            val existentes = disponibles.toSet()
            return FRECUENTES.filter { it in existentes } + OTRA_PROVINCIA
        }
    }

    /**
     * SALVAGUARDA DE CRISIS AGUDA. La derivación al COP cubre el apoyo
     * profesional ordinario, pero el burnout severo (Perfil 2) puede coexistir
     * con crisis de ansiedad o ideación. La app no es un servicio de
     * emergencias; ante una posible crisis, ofrece SIEMPRE además una línea de
     * atención inmediata disponible 24/7.
     *
     * En España: Línea 024 de atención a la conducta suicida (Ministerio de
     * Sanidad), gratuita y confidencial. Verifica el recurso vigente antes del
     * despliegue.
     */
    fun lineaCrisis(): RecursoCrisis = RecursoCrisis(
        nombre = "Línea 024 — Atención a la conducta suicida (24 h, gratuita)",
        nombreNeutro = "Línea 024, de atención gratuita y confidencial " +
            "disponible las veinticuatro horas",
        telefono = "024",
        web = "https://www.sanidad.gob.es/linea024/home.htm"
    )
}
