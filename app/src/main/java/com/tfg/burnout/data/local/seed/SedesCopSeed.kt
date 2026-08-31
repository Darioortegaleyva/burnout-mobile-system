package com.tfg.burnout.data.local.seed

import com.tfg.burnout.data.local.entity.SedeCopEntity

/**
 * Directorio de delegaciones del Consejo General de la Psicología de España,
 * organizado por provincia (asset seeding — §4.4).
 *
 * Las 50 provincias + Ceuta y Melilla (52 entradas) se mapean al colegio
 * territorial que les corresponde. Téngase en cuenta la estructura real:
 *   - Colegios autonómicos que cubren TODA su comunidad.
 *   - Colegios que agrupan varias provincias (Andalucía Occidental/Oriental).
 *   - Colegios provinciales (Álava, Bizkaia, Gipuzkoa, Las Palmas, Tenerife).
 *
 * FUENTE de nombres, webs y teléfonos: documento oficial "Colegios
 * Territoriales" del Consejo General de la Psicología de España
 * (https://www.cop.es/uploads/pdf/ColegiosTerritoriales.pdf).
 *
 * NOTA: verifica los datos antes de la entrega final, ya que teléfonos y
 * URLs pueden cambiar. Algunas webs del documento oficial usan http://;
 * se recomienda comprobar si el colegio ya sirve por https://.
 */
object SedesCopSeed {

    // ─── Definición de cada colegio territorial (para no repetir datos) ───
    private val ANDALUCIA_OCC = Triple(
        "Colegio Oficial de Psicología de Andalucía Occidental",
        "955 540 018", "http://www.copao.cop.es"
    )
    private val ANDALUCIA_OR = Triple(
        "Ilustre Colegio Oficial de Psicología de Andalucía Oriental",
        "958 535 148", "http://www.copao.com"
    )
    private val ARAGON = Triple(
        "Colegio Profesional de Psicología de Aragón",
        "976 201 982", "http://www.coppa.es"
    )
    private val ASTURIAS = Triple(
        "Colegio Oficial de Psicólogos del Principado de Asturias",
        "985 285 778", "http://www.cop-asturias.org"
    )
    private val BALEARS = Triple(
        "Col·legi Oficial de Psicologia de les Illes Balears",
        "971 764 469", "http://www.copib.es"
    )
    private val LAS_PALMAS = Triple(
        "Colegio Oficial de la Psicología de Las Palmas",
        "928 249 613", "http://www.coplaspalmas.org"
    )
    private val TENERIFE = Triple(
        "Ilustre Colegio Oficial de Psicología de Santa Cruz de Tenerife",
        "922 289 060", "http://www.copsctenerife.es"
    )
    private val CANTABRIA = Triple(
        "Ilustre Colegio Oficial de Psicología de Cantabria",
        "942 273 450", "http://www.copcantabria.es"
    )
    private val CLM = Triple(
        "Colegio Oficial de la Psicología de Castilla-La Mancha",
        "967 219 802", "http://www.copclm.com"
    )
    private val CYL = Triple(
        "Colegio Oficial de Psicología de Castilla y León",
        "983 210 329", "http://www.copcyl.es"
    )
    private val CATALUNYA = Triple(
        "Col·legi Oficial de Psicologia de Catalunya",
        "932 478 650", "http://www.copc.cat"
    )
    private val CEUTA = Triple(
        "Colegio Oficial de Psicología de Ceuta",
        "856 208 001", "http://www.copceuta.org"
    )
    private val VALENCIA = Triple(
        "Col·legi Oficial de Psicologia de la Comunitat Valenciana",
        "963 922 595", "http://www.cop-cv.org"
    )
    private val EXTREMADURA = Triple(
        "Colegio Oficial de Psicólogos de Extremadura",
        "924 317 660", "http://www.copex.es"
    )
    private val GALICIA = Triple(
        "Colexio Oficial de Psicoloxía de Galicia",
        "981 534 049", "http://www.copgalicia.gal"
    )
    private val MADRID = Triple(
        "Colegio Oficial de la Psicología de Madrid",
        "915 419 999", "http://www.copmadrid.org"
    )
    private val MELILLA = Triple(
        "Colegio Oficial de la Psicología de Melilla",
        "952 684 149", "http://www.copmelilla.org"
    )
    private val NAVARRA = Triple(
        "Colegio Oficial de Psicología de Navarra",
        "948 175 133", "http://www.colpsinavarra.org"
    )
    private val MURCIA = Triple(
        "Colegio Oficial de Psicólogos de la Región de Murcia",
        "968 248 816", "http://www.colegiopsicologos-murcia.org"
    )
    private val RIOJA = Triple(
        "Colegio Oficial de Psicólogos de La Rioja",
        "941 254 763", "http://www.copsrioja.org"
    )
    private val ALAVA = Triple(
        "Colegio Oficial de Psicólogos de Álava",
        "945 234 336", "http://www.cop-alava.org"
    )
    private val BIZKAIA = Triple(
        "Colegio de Psicología de Bizkaia",
        "944 795 270", "http://www.copbizkaia.org"
    )
    private val GIPUZKOA = Triple(
        "Colegio Oficial de la Psicología de Gipuzkoa",
        "943 278 712", "http://www.copgipuzkoa.eus"
    )

    private fun sede(provincia: String, colegio: Triple<String, String, String>) =
        SedeCopEntity(
            provincia = provincia,
            nombreColegio = colegio.first,
            telefono = colegio.second,
            web = colegio.third
        )

    /** Las 52 demarcaciones, ordenadas alfabéticamente por provincia. */
    val sedes: List<SedeCopEntity> = listOf(
        sede("A Coruña", GALICIA),
        sede("Álava", ALAVA),
        sede("Albacete", CLM),
        sede("Alicante", VALENCIA),
        sede("Almería", ANDALUCIA_OR),
        sede("Asturias", ASTURIAS),
        sede("Ávila", CYL),
        sede("Badajoz", EXTREMADURA),
        sede("Barcelona", CATALUNYA),
        sede("Bizkaia", BIZKAIA),
        sede("Burgos", CYL),
        sede("Cáceres", EXTREMADURA),
        sede("Cádiz", ANDALUCIA_OCC),
        sede("Cantabria", CANTABRIA),
        sede("Castellón", VALENCIA),
        sede("Ceuta", CEUTA),
        sede("Ciudad Real", CLM),
        sede("Córdoba", ANDALUCIA_OCC),
        sede("Cuenca", CLM),
        sede("Gipuzkoa", GIPUZKOA),
        sede("Girona", CATALUNYA),
        sede("Granada", ANDALUCIA_OR),
        sede("Guadalajara", CLM),
        sede("Huelva", ANDALUCIA_OCC),
        sede("Huesca", ARAGON),
        sede("Illes Balears", BALEARS),
        sede("Jaén", ANDALUCIA_OR),
        sede("La Rioja", RIOJA),
        sede("Las Palmas", LAS_PALMAS),
        sede("León", CYL),
        sede("Lleida", CATALUNYA),
        sede("Lugo", GALICIA),
        sede("Madrid", MADRID),
        sede("Málaga", ANDALUCIA_OR),
        sede("Melilla", MELILLA),
        sede("Murcia", MURCIA),
        sede("Navarra", NAVARRA),
        sede("Ourense", GALICIA),
        sede("Palencia", CYL),
        sede("Pontevedra", GALICIA),
        sede("Salamanca", CYL),
        sede("Santa Cruz de Tenerife", TENERIFE),
        sede("Segovia", CYL),
        sede("Sevilla", ANDALUCIA_OCC),
        sede("Soria", CYL),
        sede("Tarragona", CATALUNYA),
        sede("Teruel", ARAGON),
        sede("Toledo", CLM),
        sede("Valencia", VALENCIA),
        sede("Valladolid", CYL),
        sede("Zamora", CYL),
        sede("Zaragoza", ARAGON)
    )
}
