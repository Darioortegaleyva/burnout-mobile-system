package com.tfg.burnout.domain.engine

import com.tfg.burnout.data.local.entity.SedeCopEntity
import com.tfg.burnout.data.local.dao.SedeCopDao

/**
 * Recurso de ayuda inmediata para situaciones de crisis aguda, diferenciado
 * de la derivación profesional ordinaria (que tiene horario de oficina).
 */
data class RecursoCrisis(
    val nombre: String,
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
        telefono = "024",
        web = "https://www.sanidad.gob.es/linea024/home.htm"
    )
}
