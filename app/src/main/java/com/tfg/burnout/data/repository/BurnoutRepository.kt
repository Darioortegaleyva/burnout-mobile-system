package com.tfg.burnout.data.repository

import com.tfg.burnout.data.healthconnect.HealthConnectManager
import com.tfg.burnout.data.local.dao.BiometriaDao
import com.tfg.burnout.data.local.dao.CesqtDao
import com.tfg.burnout.data.local.dao.MetaDao
import com.tfg.burnout.data.local.dao.RecomendacionDao
import com.tfg.burnout.data.local.entity.RecomendacionEntity
import com.tfg.burnout.data.local.entity.MetaEntity
import com.tfg.burnout.data.local.dao.UsuarioDao
import com.tfg.burnout.data.local.entity.BiometriaEntity
import com.tfg.burnout.data.local.entity.CesqtResponseEntity
import com.tfg.burnout.domain.cesqt.CalculadoraCesqt
import com.tfg.burnout.domain.cesqt.DimensionCesqt
import com.tfg.burnout.domain.engine.ConfiguracionRitmos
import com.tfg.burnout.domain.engine.MotorRiesgo
import com.tfg.burnout.domain.engine.UmbralesRiesgo
import com.tfg.burnout.domain.model.IndiceRiesgo
import com.tfg.burnout.domain.model.PerfilContexto
import com.tfg.burnout.domain.model.LecturaBiometrica
import com.tfg.burnout.domain.model.LineaBase
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.time.LocalDate

/**
 * Repositorio que une la capa de datos (Room + Health Connect) con la lógica
 * de dominio (MotorRiesgo). Es la única vía que los ViewModels usan para
 * acceder a datos, en estricto cumplimiento de MVVM (§4.2).
 */
class BurnoutRepository(
    private val usuarioDao: UsuarioDao,
    private val cesqtDao: CesqtDao,
    private val biometriaDao: BiometriaDao,
    private val metaDao: MetaDao,
    private val recomendacionDao: RecomendacionDao,
    private val healthConnect: HealthConnectManager,
    private val motorRiesgo: MotorRiesgo = MotorRiesgo()
) {
    /**
     * Borra por completo los datos del usuario y devuelve la app al estado de
     * primer arranque. Sirve tanto para ensayar la demostración como para
     * materializar el derecho de supresión (RGPD art. 17) sin desinstalar.
     */
    suspend fun borrarTodosLosDatos() {
        cesqtDao.borrarTodo()
        biometriaDao.borrarTodo()
        metaDao.borrarTodo()
        recomendacionDao.borrarTodo()
        usuarioDao.obtener()?.let { usuarioDao.actualizar(
            it.copy(
                consentimientoAceptado = false,
                cargaPercibida = null, autonomiaPercibida = null, apoyoPercibido = null,
                rmssdMedioBase = 0.0, tstMedioBaseMin = 0.0, rhrMedioBase = 0.0,
                ultimaEmaEpochDay = 0L, ultimoR = 0.0
            )
        ) }
    }

    /**
     * Promedia los últimos N días de biometría en una lectura única. Devuelve
     * null si no hay ningún registro en la ventana; cada métrica se promedia
     * de forma independiente, de modo que la ausencia puntual de una de ellas
     * no invalida las demás.
     */
    private suspend fun promedioReciente(dias: Int): LecturaBiometrica? {
        val registros = biometriaDao.ultimos(dias)
        if (registros.isEmpty()) return null
        fun media(sel: (com.tfg.burnout.data.local.entity.BiometriaEntity) -> Double?): Double? =
            registros.mapNotNull(sel).takeIf { it.isNotEmpty() }?.average()
        return LecturaBiometrica(
            fechaEpochDay = registros.first().fechaEpochDay,
            rmssdMs = media { it.rmssdMs },
            tstMin = media { it.tstMin },
            rhrBpm = media { it.rhrBpm }
        )
    }

    /** Lectura puntual del usuario (la usan los workers en segundo plano). */
    suspend fun obtenerUsuario(): com.tfg.burnout.data.local.entity.UsuarioEntity? =
        usuarioDao.obtener()

    fun observarUsuario(): Flow<com.tfg.burnout.data.local.entity.UsuarioEntity?> =
        usuarioDao.observar()

    fun observarMetasActivas() = metaDao.observarActivas()

    /**
     * ¿Toca la micro-interacción diaria? Como máximo una vez al día (§5.4).
     * Se guarda en la propia base de datos para que no dependa de ficheros
     * externos ni sobreviva a un borrado de datos de la app.
     */
    suspend fun tocaEmaHoy(): Boolean {
        val u = usuarioDao.obtener() ?: return false
        return u.ultimaEmaEpochDay != java.time.LocalDate.now().toEpochDay()
    }

    suspend fun registrarEmaHoy() {
        val u = usuarioDao.obtener() ?: return
        usuarioDao.actualizar(u.copy(ultimaEmaEpochDay = java.time.LocalDate.now().toEpochDay()))
    }

    /**
     * ¿Procede administrar el cuestionario? (Tarea 0). Cierto si nunca se ha
     * hecho (línea base) o si se ha cumplido el ciclo de cuatro semanas
     * (§2.2.6). El chat lo consulta al abrirse, venga de donde venga.
     */
    suspend fun tocaCuestionario(): Boolean {
        val ultima = fechaUltimaEvaluacion() ?: return true
        return (java.time.LocalDate.now().toEpochDay() - ultima) >=
            com.tfg.burnout.domain.engine.ConfiguracionRitmos.CICLO_CUESTIONARIO_DIAS
    }

    /**
     * Datos necesarios para cerrar el ciclo mensual (CU-04): la evaluación
     * anterior con la que comparar y el recuento de retos cumplidos durante
     * el período. Devuelve null en la evaluación de línea base, cuando aún no
     * hay nada con lo que comparar.
     */
    data class ResumenCiclo(
        val anterior: com.tfg.burnout.data.local.entity.CesqtResponseEntity,
        val diasCumplidos: Int,
        val retosActivos: Int,
    )

    suspend fun resumenDelCiclo(): ResumenCiclo? {
        val anterior = cesqtDao.penultimo() ?: return null
        val metas = metaDao.activas()
        val desde = java.time.LocalDate.ofEpochDay(anterior.fechaEpochDay)
        // Se cuentan los días en que se marcó al menos un reto desde la
        // evaluación anterior: es la medida de constancia más honesta, ya que
        // no penaliza a quien eligió más retos de los que podía sostener.
        val dias = metas
            .flatMap { extraerFechas(it.cumplimientosJson) }
            .filter { !it.isBefore(desde) }
            .distinct()
            .size
        return ResumenCiclo(anterior, dias, metas.size)
    }

    private fun extraerFechas(json: String): List<java.time.LocalDate> =
        json.trim().removePrefix("[").removeSuffix("]")
            .split(",").map { it.trim().trim('"') }.filter { it.isNotBlank() }
            .mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }

    /**
     * Días que restan para la próxima evaluación, o null si ya toca o si
     * nunca se ha hecho ninguna. Sirve para que el asistente informe al
     * usuario de cuándo volverá a preguntarle, en lugar de dejarlo sin saber
     * cuándo se le pedirá algo.
     */
    suspend fun diasHastaProximaEvaluacion(): Int? {
        val ultima = fechaUltimaEvaluacion() ?: return null
        val transcurridos = java.time.LocalDate.now().toEpochDay() - ultima
        val restantes =
            com.tfg.burnout.domain.engine.ConfiguracionRitmos.CICLO_CUESTIONARIO_DIAS - transcurridos
        return if (restantes > 0) restantes.toInt() else null
    }

    /** Día (epoch) de la última evaluación CESQT, o null si nunca se hizo. */
    suspend fun fechaUltimaEvaluacion(): Long? = cesqtDao.ultimo()?.fechaEpochDay

    /** CU-02: guarda un reto negociado en el chat. */
    suspend fun crearMeta(categoria: String, titulo: String) {
        metaDao.insertar(
            MetaEntity(
                categoria = categoria, titulo = titulo,
                fechaCreacionEpochDay = java.time.LocalDate.now().toEpochDay()
            )
        )
    }

    /** CU-03: marca o desmarca el cumplimiento de hoy (refuerzo positivo). */
    suspend fun alternarCumplimientoHoy(meta: MetaEntity) {
        val hoy = java.time.LocalDate.now().toString()
        val fechas = meta.cumplimientosJson.trim().removePrefix("[").removeSuffix("]")
            .split(",").map { it.trim().trim('"') }.filter { it.isNotBlank() }.toMutableList()
        if (hoy in fechas) fechas.remove(hoy) else fechas.add(hoy)
        val json = "[" + fechas.joinToString(",") { "\"" + it + "\"" } + "]"
        metaDao.actualizar(meta.copy(cumplimientosJson = json))
    }

    /**
     * Lee la jornada de hoy desde Health Connect y la persiste en Room.
     * Lo llama el Worker nocturno (§2.2.6).
     */
    /** Última fila biométrica guardada (diagnóstico de la cadena Zepp→HC). */
    suspend fun ultimaBiometria() = biometriaDao.masReciente()

    /**
     * Sincroniza los últimos N días (no solo hoy). Necesario cuando se
     * insertan datos históricos de golpe: sin esto, la línea base seguiría
     * vacía porque solo se habría leído la jornada actual.
     */
    suspend fun sincronizarUltimosDias(dias: Int) {
        if (!healthConnect.tienePermisos()) return
        for (i in (dias - 1) downTo 0) {
            guardarJornada(LocalDate.now().minusDays(i.toLong()))
        }
        actualizarLineaBase()
        calcularIndiceActual()
    }

    /**
     * Lee una jornada y la persiste, SALVO que venga completamente vacía.
     *
     * Una lectura sin ninguna de las tres métricas no aporta información, y
     * guardarla sí hace daño por partida doble: la clave primaria es el día,
     * así que sobrescribiría con nulos una jornada ya consolidada (basta con
     * que Health Connect deje de devolverla, o con que se revoquen permisos a
     * medias), y además ocupa una plaza en la ventana de la línea base, que
     * cuenta filas para exigir su mínimo de días pero promedia solo los
     * valores no nulos.
     */
    private suspend fun guardarJornada(fecha: LocalDate) {
        val lectura = healthConnect.leerJornada(fecha)
        if (lectura.rmssdMs == null && lectura.tstMin == null && lectura.rhrBpm == null) return
        biometriaDao.guardar(
            BiometriaEntity(
                fechaEpochDay = lectura.fechaEpochDay,
                rmssdMs = lectura.rmssdMs,
                tstMin = lectura.tstMin,
                rhrBpm = lectura.rhrBpm
            )
        )
    }

    suspend fun sincronizarBiometriaHoy() {
        if (!healthConnect.tienePermisos()) return
        guardarJornada(LocalDate.now())
        actualizarLineaBase()
        // El índice debe recalcularse en cuanto entra biometría nueva: de lo
        // contrario seguiría reflejando solo la rama psicométrica del último
        // cuestionario, como si no hubiera datos del reloj.
        calcularIndiceActual()
    }

    /** Recalcula la media móvil de los últimos 28 días como línea base. */
    /**
     * Recalcula la línea base individual del usuario.
     *
     * DOS REGLAS IMPORTANTES:
     *
     *  1. Se EXCLUYE el día en curso. Si se incluyera, la lectura de hoy
     *     entraría en su propia referencia y la desviación tendería a cero:
     *     el sistema nunca detectaría una mala noche porque esa mala noche
     *     ya habría desplazado la media.
     *  2. Se exige un MÍNIMO de días de histórico. Con uno o dos registros la
     *     media no representa el hábito de la persona, y compararse contra ella
     *     daría desviaciones sin sentido. Mientras no se alcance ese mínimo, la
     *     línea base permanece a cero y el motor opera solo con la rama
     *     psicométrica, que es el comportamiento honesto (§4.4).
     */
    private suspend fun actualizarLineaBase() {
        val hoy = LocalDate.now().toEpochDay()
        val ultimos = biometriaDao.ultimos(ConfiguracionRitmos.VENTANA_INDICE_DIAS + 1)
            .filter { it.fechaEpochDay != hoy }
        if (ultimos.size < ConfiguracionRitmos.MIN_DIAS_LINEA_BASE) return
        val rmssd = ultimos.mapNotNull { it.rmssdMs }
        val tst = ultimos.mapNotNull { it.tstMin }
        val rhr = ultimos.mapNotNull { it.rhrBpm }
        val usuario = usuarioDao.obtener() ?: return
        usuarioDao.actualizar(
            usuario.copy(
                rmssdMedioBase = if (rmssd.isNotEmpty()) rmssd.average() else usuario.rmssdMedioBase,
                tstMedioBaseMin = if (tst.isNotEmpty()) tst.average() else usuario.tstMedioBaseMin,
                rhrMedioBase = if (rhr.isNotEmpty()) rhr.average() else usuario.rhrMedioBase
            )
        )
    }

    /**
     * Procesa un cuestionario CESQT recién respondido: calcula puntuaciones,
     * las persiste y devuelve el resultado de dominio.
     */
    suspend fun registrarCesqt(respuestas: Map<Int, Int>): CesqtResponseEntity {
        val resultado = CalculadoraCesqt.calcular(respuestas)
        val json = JSONObject().apply {
            respuestas.forEach { (k, v) -> put(k.toString(), v) }
        }.toString()

        val entidad = CesqtResponseEntity(
            fechaEpochDay = LocalDate.now().toEpochDay(),
            scoreGlobalNormalizado = resultado.scoreGlobalNormalizado,
            subscoreCulpa = resultado.subscoreCulpa,
            mediaIlusion = resultado.mediasPorDimension.getValue(DimensionCesqt.ILUSION_POR_TRABAJO),
            mediaDesgaste = resultado.mediasPorDimension.getValue(DimensionCesqt.DESGASTE_PSIQUICO),
            mediaIndolencia = resultado.mediasPorDimension.getValue(DimensionCesqt.INDOLENCIA),
            respuestasJson = json
        )
        val id = cesqtDao.insertar(entidad)
        return entidad.copy(id = id)
    }

    /**
     * Calcula el Índice de Riesgo actual cruzando el último CESQT con la
     * biometría reciente y la línea base (§2.2.5). Persiste el R resultante.
     */
    suspend fun calcularIndiceActual(): IndiceRiesgo? {
        val cesqt = cesqtDao.ultimo() ?: return null
        val usuario = usuarioDao.obtener()
        // ESTADO RECIENTE, NO ÚLTIMA NOCHE.
        //
        // El índice no compara la noche de ayer contra la media del mes: eso
        // haría que una sola mala noche desplazara la valoración de fondo,
        // precisamente lo que el diseño quiere evitar (§2.2.6). Se promedia
        // una ventana corta —la última semana— y ESE promedio es el que se
        // contrasta con la línea base de cuatro semanas. Así el índice
        // responde a una tendencia reciente, no a un episodio aislado.
        val bioReciente = promedioReciente(ConfiguracionRitmos.VENTANA_RECIENTE_DIAS)

        val lineaBase = usuario?.let {
            if (it.rmssdMedioBase > 0 && it.tstMedioBaseMin > 0)
                LineaBase(it.rmssdMedioBase, it.tstMedioBaseMin, it.rhrMedioBase)
            else null
        }

        val lectura = bioReciente

        val indice = motorRiesgo.calcular(
            scoreCesqtNormalizado = cesqt.scoreGlobalNormalizado,
            biometriaReciente = lectura,
            lineaBase = lineaBase,
            edad = usuario?.edad
        )

        usuario?.let {
            usuarioDao.actualizar(
                it.copy(
                    ultimoR = indice.r,
                    fechaUltimoIndiceEpochDay = LocalDate.now().toEpochDay()
                )
            )
        }
        return indice
    }

    suspend fun ultimoCesqt(): CesqtResponseEntity? = cesqtDao.ultimo()

    /** Guarda el perfil de contexto laboral del onboarding (§5.5). */
    suspend fun guardarPerfilContexto(carga: Int, autonomia: Int, apoyo: Int) {
        val u = usuarioDao.obtener() ?: return
        usuarioDao.actualizar(u.copy(
            cargaPercibida = carga, autonomiaPercibida = autonomia, apoyoPercibido = apoyo
        ))
    }

    /**
     * PERFIL FÍSICO OPCIONAL (Tarea 6) — SOLO LA EDAD.
     *
     * Del estudio de viabilidad se concluyó que únicamente la edad aporta un
     * ajuste defendible, a través de la reserva cardíaca. El peso, la altura y
     * el sexo influyen en el nivel basal de la frecuencia y no en su
     * reactividad, y ese nivel ya lo absorbe la línea base individual, de modo
     * que se retiraron del formulario para no pedir datos personales sin
     * contrapartida (minimización, RGPD art. 5.1.c). Las columnas
     * correspondientes permanecen en el esquema, sin uso, para no forzar una
     * migración destructiva que borraría el histórico biométrico ya recogido.
     */
    suspend fun guardarPerfilFisico(edad: Int?) {
        val u = usuarioDao.obtener() ?: return
        usuarioDao.actualizar(u.copy(edad = edad))
    }

    suspend fun borrarPerfilFisico() = guardarPerfilFisico(null)

    suspend fun tienePerfilFisico(): Boolean =
        usuarioDao.obtener()?.edad != null

    /** Perfil de contexto, o null si aún no se ha respondido. */
    suspend fun perfilContexto(): PerfilContexto? {
        val u = usuarioDao.obtener() ?: return null
        val c = u.cargaPercibida ?: return null
        val a = u.autonomiaPercibida ?: return null
        val ap = u.apoyoPercibido ?: return null
        return PerfilContexto(c, a, ap)
    }

    // ---------------------------------------------------------------
    // HISTORIAL DE PAUTAS (para no repetir y para preguntar qué tal fue)
    // ---------------------------------------------------------------

    /** Días durante los cuales no se vuelve a proponer la misma pauta. */
    private val diasSinRepetir =
        com.tfg.burnout.domain.engine.ConfiguracionRitmos.NO_REPETIR_PAUTA_DIAS

    /** Identificadores de pautas propuestas recientemente. */
    suspend fun pautasRecientes(): Set<String> {
        val desde = java.time.LocalDate.now().toEpochDay() - diasSinRepetir
        return recomendacionDao.desde(desde).map { it.pautaId }.toSet()
    }

    suspend fun registrarRecomendacion(pautaId: String, categoria: String) {
        recomendacionDao.insertar(
            RecomendacionEntity(
                pautaId = pautaId, categoria = categoria,
                fechaEpochDay = java.time.LocalDate.now().toEpochDay()
            )
        )
    }

    /** Última pauta propuesta y aún sin valorar (BOT B pregunta por ella). */
    suspend fun ultimaRecomendacionSinValorar(): RecomendacionEntity? =
        recomendacionDao.ultima()?.takeIf { it.valoracion == null }

    suspend fun valorarRecomendacion(id: Long, valoracion: String) {
        val todas = recomendacionDao.todas()
        todas.find { it.id == id }?.let {
            recomendacionDao.actualizar(it.copy(valoracion = valoracion))
        }
    }

    /** Registra la aceptación del consentimiento informado (RGPD art. 7). */
    suspend fun aceptarConsentimiento() {
        val u = usuarioDao.obtener() ?: return
        usuarioDao.actualizar(u.copy(consentimientoAceptado = true))
    }

    /**
     * Derecho de PORTABILIDAD (RGPD art. 20): exporta todos los datos del
     * usuario en un JSON legible que él puede guardar o compartir. Como el
     * sistema es offline-first, esta es la única vía de salida de los datos,
     * y siempre la inicia el propio usuario.
     */
    suspend fun exportarDatosJson(): String {
        val usuario = usuarioDao.obtener()
        val biometria = biometriaDao.ultimos(365)
        val raiz = JSONObject()
        raiz.put("exportadoEn", LocalDate.now().toString())
        usuario?.let {
            raiz.put("usuario", JSONObject().apply {
                put("rmssdMedioBase", it.rmssdMedioBase)
                put("tstMedioBaseMin", it.tstMedioBaseMin)
                put("rhrMedioBase", it.rhrMedioBase)
                put("ultimoIndiceR", it.ultimoR)
                put("bandaActual", UmbralesRiesgo.bandaDe(it.ultimoR).etiqueta)
                // Contexto laboral declarado (§5.5); null si no se respondió.
                put("cargaPercibida", it.cargaPercibida ?: JSONObject.NULL)
                put("autonomiaPercibida", it.autonomiaPercibida ?: JSONObject.NULL)
                put("apoyoPercibido", it.apoyoPercibido ?: JSONObject.NULL)
            })
        }
        val arrBio = org.json.JSONArray()
        biometria.forEach { b ->
            arrBio.put(JSONObject().apply {
                put("fechaEpochDay", b.fechaEpochDay)
                put("rmssdMs", b.rmssdMs ?: JSONObject.NULL)
                put("tstMin", b.tstMin ?: JSONObject.NULL)
                put("rhrBpm", b.rhrBpm ?: JSONObject.NULL)
            })
        }
        raiz.put("biometria", arrBio)
        cesqtDao.ultimo()?.let { c ->
            raiz.put("ultimoCesqt", JSONObject().apply {
                put("fechaEpochDay", c.fechaEpochDay)
                put("scoreGlobalNormalizado", c.scoreGlobalNormalizado)
                put("subscoreCulpa", c.subscoreCulpa)
            })
        }
        return raiz.toString(2)
    }
}
