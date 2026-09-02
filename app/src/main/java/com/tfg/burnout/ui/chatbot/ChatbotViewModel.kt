package com.tfg.burnout.ui.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tfg.burnout.data.repository.BurnoutRepository
import com.tfg.burnout.domain.cesqt.CatalogoCesqt
import com.tfg.burnout.domain.cesqt.FrecuenciaCesqt
import com.tfg.burnout.domain.cesqt.ItemCesqt
import com.tfg.burnout.domain.engine.CatalogoPautas
import com.tfg.burnout.domain.engine.GestorCoping
import com.tfg.burnout.domain.model.CategoriaCoping
import com.tfg.burnout.domain.cesqt.BorradorCuestionario
import com.tfg.burnout.domain.chat.FiltroCrisis
import com.tfg.burnout.domain.chat.FiltroManipulacion
import com.tfg.burnout.domain.chat.FrasesEntrada
import com.tfg.burnout.domain.engine.ComparadorCiclos
import com.tfg.burnout.domain.engine.GeneradorMensajes
import com.tfg.burnout.domain.engine.ModuloEticoRuteo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Un mensaje del chat. */
data class MensajeChat(
    val id: Long,
    val texto: String,
    val esBot: Boolean
)

/**
 * Fases del chat. El enrutamiento (Tarea 0) decide entre dos comportamientos:
 *
 *  - BOT A (fases OFERTA_CUESTIONARIO → CESQT → METAS): estático, guiado por
 *    chips, sin modelo de lenguaje en las preguntas. Administra el
 *    cuestionario y cierra dando pautas.
 *  - BOT B (fase ASISTENTE): conversacional con texto libre, respondido
 *    mediante recuperación documental (RAG) sobre la base de conocimiento.
 *
 * DERIVACION es transversal: puede interrumpir cualquier fase.
 */
enum class FaseChat { OFERTA_CUESTIONARIO, REANUDAR, CESQT, METAS, ASISTENTE, EMA, DERIVACION, LIBRE }

/** Estado del chatbot, con los chips de respuesta disponibles. */
data class ChatbotUiState(
    /** true cuando el BOT B está activo y procede mostrar el campo de texto. */
    val permiteTextoLibre: Boolean = false,
    /** true mientras el asistente documental está pensando. */
    val pensando: Boolean = false,
    val mensajes: List<MensajeChat> = emptyList(),
    val chips: List<String> = emptyList(),
    val fase: FaseChat = FaseChat.ASISTENTE,
    val progresoCesqt: Pair<Int, Int>? = null  // (ítem actual, total) para barra de progreso
)

/**
 * ViewModel del chatbot (§2.3.1, §6.3). Orquesta tres flujos:
 *   1) Micro-interacción EMA de fin de jornada.
 *   2) Administración completa del cuestionario CESQT (20 ítems).
 *   3) Cláusula de derivación al COP (§2.2.7).
 *
 * El chatbot NO reescribe el CESQT: presenta cada ítem literalmente con su
 * escala de frecuencia de cinco grados e introduce únicamente puentes de
 * transición empáticos entre dimensiones (§2.3.1).
 */
class ChatbotViewModel(
    private val repository: BurnoutRepository,
    private val gestorCoping: GestorCoping,
    private val moduloEtico: ModuloEticoRuteo,
    private val generadorMensajes: GeneradorMensajes = GeneradorMensajes(),
    /**
     * Reformulación opcional por IA local (§2.3.6): recibe la plantilla ya
     * validada y devuelve una variante, o null para usar la original. Solo
     * se aplica a los mensajes largos de acompañamiento; NUNCA a los ítems
     * del cuestionario, a los acuses breves ni al flujo de derivación.
     */
    private val reformular: (suspend (String) -> String?)? = null,
    /** BOT B: asistente documental. Null = solo plantillas. */
    private val asistente: com.tfg.burnout.data.rag.AsistenteRag? = null,
    /** Contexto de aplicación, para persistir el borrador del cuestionario. */
    private val contexto: android.content.Context? = null
) : ViewModel() {

    private val _ui = MutableStateFlow(ChatbotUiState())
    val ui: StateFlow<ChatbotUiState> = _ui.asStateFlow()

    private var siguienteId = 0L
    private var pasoEma = 0

    // Último mensaje de afrontamiento mostrado, para no repetirlo (variación).
    private var ultimoMensajeCoping: String? = null

    // Variación de acuses de recibo y estado de la micro-interacción EMA.
    private var ultimoAcuse: String? = null
    private var emaDiaBueno: Boolean = true

    // --- Estado del recorrido CESQT ---
    private val itemsCesqt: List<ItemCesqt> = CatalogoCesqt.items
    private var indiceItemActual = 0
    private val respuestasCesqt = mutableMapOf<Int, Int>()
    private var dimensionAnterior: String? = null

    // CU-02: opciones de reto propuestas en la negociación (título -> categoría)
    private var opcionesMeta: List<Pair<String, String>> = emptyList()

    /** Recomendación cuya valoración está esperando respuesta (BOT B). */
    private var recomendacionEnEspera: Long? = null

    init {
        // ENRUTAMIENTO DEL CHAT (Tarea 0).
        //
        //   ¿Está pendiente el cuestionario?
        //     Sí → se ofrece (OFERTA_CUESTIONARIO)
        //            · acepta  → BOT A
        //            · rechaza → BOT B
        //     No → BOT B directamente
        //
        // La invitación se lanza tanto si el usuario llega desde la
        // notificación como si entra por su cuenta: en ambos casos pasa por
        // el mismo punto (*) del enunciado.
        viewModelScope.launch {
            presentarse()

            // El enrutamiento consulta la base de datos, y un fallo ahí
            // dejaría la conversación sin opciones ni campo de texto, es
            // decir, inutilizable. Ante cualquier error se opta por la
            // alternativa más segura: ofrecer el cuestionario, que es la
            // acción principal del sistema.
            runCatching { enrutar() }.onFailure {
                ofrecerCuestionario(primeraVez = true)
            }
        }
    }

    private suspend fun enrutar() {
        // ¿Quedó una evaluación a medias? Se ofrece retomarla antes que
            // nada: hacerle repetir quince respuestas ya dadas sería la vía
            // más rápida al abandono.
        val hayBorrador = contexto?.let { BorradorCuestionario.hayBorrador(it) } == true
        if (hayBorrador && repository.tocaCuestionario()) {
            val hechas = contexto?.let { BorradorCuestionario.respuestas(it).size } ?: 0
            addBot(
                "Veo que dejamos una evaluación a medias: llevabas " + hechas +
                " de " + itemsCesqt.size + " preguntas. ¿La retomamos donde lo dejaste?"
            )
            _ui.value = _ui.value.copy(
                fase = FaseChat.REANUDAR,
                chips = listOf("Seguir donde iba", "Empezar de nuevo", "Ahora no"),
                permiteTextoLibre = false
            )
            return
        }

        if (repository.tocaCuestionario()) {
            ofrecerCuestionario(primeraVez = repository.fechaUltimaEvaluacion() == null)
        } else {
            iniciarAsistente()
        }
    }

    /** Punto (*) del enrutamiento: se pregunta, nunca se impone. */
    private fun ofrecerCuestionario(primeraVez: Boolean) {
        val lista = if (primeraVez) FrasesEntrada.invitacionPrimeraVez
                    else FrasesEntrada.invitacionCuestionario
        addBot(FrasesEntrada.aleatoria(lista))
        _ui.value = _ui.value.copy(
            fase = FaseChat.OFERTA_CUESTIONARIO,
            chips = listOf("Vamos allá", "Ahora no"),
            permiteTextoLibre = false
        )
    }

    private fun manejarReanudacion(opcion: String) {
        when {
            opcion.startsWith("Seguir") -> reanudarCesqt()
            opcion.startsWith("Empezar") -> iniciarCesqt()
            else -> {
                addBot("De acuerdo, sigue guardada por si la retomas más adelante.")
                iniciarAsistente(trasDeclinar = true)
            }
        }
    }

    /** Retoma el cuestionario desde el punto en que se interrumpió. */
    private fun reanudarCesqt() {
        val ctx = contexto ?: return iniciarCesqt()
        respuestasCesqt.clear()
        respuestasCesqt.putAll(BorradorCuestionario.respuestas(ctx))
        indiceItemActual = BorradorCuestionario.indice(ctx).coerceIn(0, itemsCesqt.size)
        addBot("Perfecto, seguimos donde lo dejamos.")
        _ui.value = _ui.value.copy(fase = FaseChat.CESQT, permiteTextoLibre = false)
        presentarItemActual()
    }

    private fun manejarOfertaCuestionario(opcion: String) {
        if (opcion == "Vamos allá") {
            iniciarCesqt()
        } else {
            addBot(
                "Sin problema, lo dejamos para otro momento. Si quieres preguntarme " +
                "cualquier cosa mientras tanto, aquí estoy."
            )
            iniciarAsistente(trasDeclinar = true)
        }
    }

    // ====================================================================
    // BOT B — ASISTENTE DOCUMENTAL (Tarea 4)
    // ====================================================================

    /**
     * Abre el asistente documental.
     *
     * @param trasDeclinar cierto cuando el usuario acaba de rechazar el
     *        cuestionario. En ese caso NO se le encadenan más preguntas: si ha
     *        dicho que ahora no le apetece responder, insistir con la
     *        micro-interacción diaria o con la valoración de la última pauta
     *        sería exactamente lo contrario de respetar esa respuesta.
     */
    private fun iniciarAsistente(trasDeclinar: Boolean = false) {
        viewModelScope.launch {
            if (trasDeclinar) {
                addBot(FrasesEntrada.aleatoria(FrasesEntrada.saludoAsistente))
                _ui.value = _ui.value.copy(
                    fase = FaseChat.ASISTENTE, chips = emptyList(), permiteTextoLibre = true
                )
                return@launch
            }
            // Si hay una pauta propuesta y sin valorar, el asistente abre
            // preguntando qué tal fue: cierra el bucle de las recomendaciones.
            val pendiente = repository.ultimaRecomendacionSinValorar()
            if (pendiente != null) {
                recomendacionEnEspera = pendiente.id
                addBot(
                    "Por cierto, la última vez te propuse una pauta. " +
                    "¿Qué tal te fue con ella?"
                )
                _ui.value = _ui.value.copy(
                    fase = FaseChat.ASISTENTE,
                    chips = listOf("Bien", "Regular", "No llegué a hacerla"),
                    permiteTextoLibre = true
                )
            } else if (repository.tocaEmaHoy()) {
                // Capa táctica (§5.4): la micro-interacción diaria vive dentro
                // del asistente, no en un flujo aparte. Una vez al día como
                // máximo, y siempre se puede ignorar escribiendo otra cosa.
                iniciarEma()
            } else {
                addBot(FrasesEntrada.aleatoria(FrasesEntrada.saludoAsistente))

                // Si no toca evaluación, conviene decirlo: de lo contrario el
                // usuario no sabe si el sistema se ha olvidado de él o si
                // simplemente no le corresponde responder nada todavía. Saber
                // cuándo volverán a pedirle algo forma parte de tratarle como
                // a alguien que decide, no como a alguien a quien se le
                // interrumpe sin previo aviso.
                repository.diasHastaProximaEvaluacion()?.let { dias ->
                    addBot(
                        when {
                            dias == 1 -> "Tu próxima evaluación toca mañana; te avisaré."
                            dias <= 7 -> "Tu próxima evaluación toca en $dias días; te avisaré."
                            else -> "Tu próxima evaluación toca dentro de unas semanas, " +
                                    "así que por ahora no tienes que responder nada."
                        }
                    )
                }

                _ui.value = _ui.value.copy(
                    fase = FaseChat.ASISTENTE, chips = emptyList(), permiteTextoLibre = true
                )
            }
        }
    }

    /** Entrada de texto libre del usuario (solo activa en el BOT B). */
    fun enviarTexto(texto: String) {
        val consulta = texto.trim()
        if (consulta.isBlank() || _ui.value.pensando) return
        addUser(consulta)

        // BARRERA DE SEGURIDAD (§7.4): antes de recuperar o generar nada.
        when (FiltroCrisis.evaluar(consulta)) {
            FiltroCrisis.Nivel.CRISIS -> {
                FiltroCrisis.respuestaCrisis.forEach { addBot(it) }
                iniciarDerivacion()
                return
            }
            FiltroCrisis.Nivel.MALESTAR_INTENSO -> {
                FiltroCrisis.respuestaMalestar.forEach { addBot(it) }
                iniciarDerivacion()
                return
            }
            FiltroCrisis.Nivel.NINGUNO -> Unit
        }

        // SEGUNDA BARRERA: intentos de manipulación o de arrancar un
        // diagnóstico. Tampoco pasan por la base documental ni por el modelo.
        when (FiltroManipulacion.evaluar(consulta)) {
            FiltroManipulacion.Intento.INSTRUCCION_ADVERSA -> {
                FiltroManipulacion.respuestaInstruccionAdversa.forEach { addBot(it) }
                return
            }
            FiltroManipulacion.Intento.PETICION_DIAGNOSTICO -> {
                FiltroManipulacion.respuestaPeticionDiagnostico.forEach { addBot(it) }
                return
            }
            FiltroManipulacion.Intento.NINGUNO -> Unit
        }

        _ui.value = _ui.value.copy(pensando = true, chips = emptyList())
        viewModelScope.launch {
            val respuesta = asistente?.responder(consulta)
            if (respuesta == null) {
                addBot(
                    "Ahora mismo no puedo consultar mis fuentes. Puedes echar un " +
                    "vistazo a la pestaña de Actividades mientras tanto."
                )
            } else {
                addBot(respuesta.texto)
                if (respuesta.fundamentada && respuesta.fuentes.isNotEmpty()) {
                    // Trazabilidad: el usuario ve de dónde sale la información y,
                    // cuando la fuente tiene identificador estable, puede acudir
                    // al original para ampliar por su cuenta.
                    val cita = buildString {
                        append("Fuente: ").append(respuesta.fuentes.joinToString("; "))
                        if (respuesta.enlaces.isNotEmpty()) {
                            append("\n").append(respuesta.enlaces.joinToString("\n"))
                        }
                    }
                    addBot(cita)
                }
            }
            _ui.value = _ui.value.copy(pensando = false)
        }
    }

    private fun valorarPautaPendiente(opcion: String) {
        val id = recomendacionEnEspera ?: return
        val valor = when (opcion) {
            "Bien" -> "BIEN"
            "Regular" -> "REGULAR"
            else -> "NO_LA_HICE"
        }
        viewModelScope.launch {
            repository.valorarRecomendacion(id, valor)
            recomendacionEnEspera = null
            val cierre = when (valor) {
                "BIEN" -> "Me alegra oírlo. Lo tendré en cuenta para lo que te proponga."
                "REGULAR" -> "Gracias por decírmelo; probaré con otra cosa la próxima vez."
                else -> "No pasa nada, tampoco hay que forzarlo. Buscaremos algo que encaje mejor."
            }
            addBot(cierre)
            addBot("¿Te ayudo con algo más? Pregúntame lo que quieras.")
            _ui.value = _ui.value.copy(chips = emptyList(), permiteTextoLibre = true)
        }
    }

    private fun presentarse() {
        addBot(
            "Hola, soy tu asistente de bienestar. Un apunte de transparencia: " +
            "no soy una inteligencia artificial que improvisa; sigo guiones " +
            "cuidadosamente preparados y cuestionarios validados. Lo que me " +
            "cuentes se queda en tu móvil."
        )
    }

    private fun addBot(texto: String) = añadir(texto, esBot = true)
    private fun addUser(texto: String) = añadir(texto, esBot = false)

    private fun añadir(texto: String, esBot: Boolean) {
        _ui.value = _ui.value.copy(
            mensajes = _ui.value.mensajes + MensajeChat(siguienteId++, texto, esBot)
        )
    }

    // ====================================================================
    // FLUJO EMA (fin de jornada)
    // ====================================================================
    private fun iniciarEma() {
        addBot("¿Qué tal ha ido la jornada de hoy?")
        _ui.value = _ui.value.copy(
            chips = listOf("Bien", "Regular", "Agotadora"),
            fase = FaseChat.EMA
        )
    }

    /** Punto de entrada cuando el usuario pulsa un chip. */
    fun responder(opcion: String) {
        addUser(opcion)
        _ui.value = _ui.value.copy(chips = emptyList())

        when (_ui.value.fase) {
            FaseChat.EMA -> avanzarEma(opcion)
            FaseChat.CESQT -> registrarRespuestaCesqt(opcion)
            FaseChat.OFERTA_CUESTIONARIO -> manejarOfertaCuestionario(opcion)
            FaseChat.REANUDAR -> manejarReanudacion(opcion)
            FaseChat.ASISTENTE -> valorarPautaPendiente(opcion)
            FaseChat.METAS -> manejarEleccionMeta(opcion)
            FaseChat.DERIVACION -> resolverProvincia(opcion)
            FaseChat.LIBRE -> { /* fuera del alcance de este TFG */ }
        }
    }

    private fun avanzarEma(opcion: String) {
        when (pasoEma) {
            0 -> {
                // El asistente ENTIENDE la respuesta: no asume que el día fue
                // malo. La valencia condiciona el acuse y la respuesta final.
                emaDiaBueno = opcion.equals("Bien", ignoreCase = true)
                val acuse = when {
                    emaDiaBueno -> "¡Qué bien! Me alegra leerlo."
                    opcion.equals("Regular", ignoreCase = true) -> "Vale, día de los normales. Gracias por contármelo."
                    else -> "Vaya. Gracias por contármelo."
                }
                addBot(acuse)
                addBot("¿Dirías que hoy las exigencias han superado lo que podías abarcar?")
                _ui.value = _ui.value.copy(chips = listOf("Sí, bastante", "Algo", "No"))
                pasoEma = 1
            }
            1 -> {
                val exigenciasAltas = opcion.startsWith("Sí", ignoreCase = true)
                // Día "Bien" + exigencias altas => tono MIXTO (no el duro):
                // el generador combina ambas señales; no se colapsan aquí.
                val respuesta = generadorMensajes.respuestaEma(
                    diaBueno = emaDiaBueno,
                    exigenciasAltas = exigenciasAltas,
                    evitarTexto = ultimoAcuse
                )
                // El estado avanza ANTES de lanzar la corrutina: así un
                // segundo toque del usuario no re-ejecuta este paso mientras
                // el reformulador (suspend) trabaja.
                ultimoAcuse = respuesta.texto
                pasoEma = 2
                viewModelScope.launch {
                    addBot(reformular?.invoke(respuesta.texto) ?: respuesta.texto)

                    // En días duros, además del acuse: UNA pauta táctica
                    // concreta e inmediata (§5.4, capa táctica).
                    if (!emaDiaBueno && exigenciasAltas) {
                        val p = CatalogoPautas.elegirSinRepetir(
                            CategoriaCoping.MINDFULNESS_ACT, repository.pautasRecientes()
                        )
                        if (p != null) {
                            addBot("Si te apetece algo ahora mismo: " + p.titulo + ". " + p.descripcion)
                            repository.registrarRecomendacion(
                                p.id.toString(), CategoriaCoping.MINDFULNESS_ACT.name
                            )
                        }
                    }

                    // ¿Procede derivación según la última evaluación?
                    val cesqt = repository.ultimoCesqt()
                    val esc = cesqt?.let {
                        gestorCoping.clasificar(it.scoreGlobalNormalizado, cargaBiometrica = null)
                    }
                    if (cesqt != null && esc != null &&
                        gestorCoping.requiereDerivacion(esc, cesqt.subscoreCulpa)
                    ) {
                        iniciarDerivacion()
                        return@launch
                    }

                    // Cerrada la micro-interacción, el chat queda en modo
                    // asistente para lo que el usuario quiera preguntar.
                    repository.registrarEmaHoy()
                    addBot("Si quieres preguntarme algo, aquí sigo.")
                    _ui.value = _ui.value.copy(
                        fase = FaseChat.ASISTENTE, chips = emptyList(), permiteTextoLibre = true
                    )
                }
            }
        }
    }

    fun iniciarCesqt() {
        indiceItemActual = 0
        respuestasCesqt.clear()
        contexto?.let { BorradorCuestionario.limpiar(it) }
        dimensionAnterior = null
        addBot(
            "Vamos a hacer una evaluación. Para las siguientes preguntas usaremos una " +
            "escala del 0 al 4, donde 0 significa «Nunca» y 4 «Muy frecuentemente: todos los días»."
        )
        if (com.tfg.burnout.domain.cesqt.TextosCesqt.ITEMS_PROVISIONALES) {
            addBot(
                "Nota de esta versión: los enunciados que verás son provisionales " +
                "de desarrollo; el cuestionario oficial CESQT se integrará con su licencia."
            )
        }
        _ui.value = _ui.value.copy(fase = FaseChat.CESQT)
        presentarItemActual()
    }

    private fun presentarItemActual() {
        if (indiceItemActual >= itemsCesqt.size) {
            finalizarCesqt()
            return
        }
        val item = itemsCesqt[indiceItemActual]

        // Puente de transición empático al cambiar de dimensión (§2.3.1),
        // sin alterar la formulación de los ítems.
        val dimActual = item.dimension.etiqueta
        if (dimensionAnterior != null && dimensionAnterior != dimActual) {
            addBot("Cambiamos de tercio: ahora, unas preguntas de otro tipo…")
        }
        dimensionAnterior = dimActual

        addBot(item.texto)  // ítem del cuestionario (provisional en esta versión; ver Cesqt.kt)
        _ui.value = _ui.value.copy(
            chips = FrecuenciaCesqt.entries.map { "${it.valor} · ${it.etiqueta}" },
            progresoCesqt = (indiceItemActual + 1) to itemsCesqt.size
        )
    }

    private fun registrarRespuestaCesqt(opcionElegida: String) {
        // El chip tiene el formato "0 · Nunca"; extraemos el valor numérico.
        val valor = opcionElegida.substringBefore(" ").trim().toIntOrNull()
            ?: opcionElegida.firstOrNull { it.isDigit() }?.digitToInt()
            ?: 0
        val item = itemsCesqt[indiceItemActual]
        respuestasCesqt[item.id] = valor.coerceIn(0, 4)

        // Se persiste el avance en cuanto se emite: si la aplicación se
        // cierra ahora, el usuario podrá retomar donde lo dejó (§2.3.6).
        contexto?.let {
            BorradorCuestionario.guardar(it, respuestasCesqt, indiceItemActual + 1)
        }

        // Acuse de recibo sensible a la respuesta (§2.3.5): puntuar alto en
        // Ilusión es buena noticia; puntuar alto en Desgaste o Culpa, no.
        val acuse = generadorMensajes.acuseCesqt(item.dimension, valor, ultimoAcuse)
        addBot(acuse.texto)
        ultimoAcuse = acuse.texto

        indiceItemActual++
        presentarItemActual()
    }

    private fun finalizarCesqt() {
        _ui.value = _ui.value.copy(chips = emptyList(), progresoCesqt = null)
        viewModelScope.launch {
            // Persiste el cuestionario y recalcula el índice.
            val resultado = repository.registrarCesqt(respuestasCesqt)
            repository.calcularIndiceActual()

            contexto?.let { BorradorCuestionario.limpiar(it) }
            addBot("¡Hemos terminado! Gracias por tu sinceridad. Ya he actualizado tu seguimiento.")

            // CIERRE DEL CICLO (CU-04). Si existe una evaluación anterior, se
            // devuelve al usuario la lectura de su evolución. Es lo que da
            // sentido a repetir el cuestionario cada mes: sin esta devolución,
            // el usuario responde veinte preguntas sin recibir nada a cambio.
            // La comparación es siempre cualitativa, nunca numérica (§6.2).
            devolverEvolucion(resultado)

            // Comprueba si procede la derivación con el resultado recién calculado.
            val indice = repository.calcularIndiceActual()
            val cargaBio = indice?.cargaBiometrica
            val escenario = gestorCoping.clasificar(resultado.scoreGlobalNormalizado, cargaBio)
            if (gestorCoping.requiereDerivacion(escenario, resultado.subscoreCulpa)) {
                iniciarDerivacion()
            } else if (indice != null) {
                // Sin derivación: el asistente ofrece una pauta de afrontamiento.
                // El texto NO es fijo: se elige del banco según escenario y
                // componente dominante, evitando repetir el último mostrado
                // (§2.3.5). Antes, un mensaje que desculpabiliza (§2.3.1).
                val perfil = repository.perfilContexto()
                val desculpa = generadorMensajes.mensajeDesculpabilizador(ultimoMensajeCoping, perfil)
                addBot(reformular?.invoke(desculpa.texto) ?: desculpa.texto)
                ultimoMensajeCoping = desculpa.texto

                val pauta = generadorMensajes.mensajeDeAfrontamiento(
                    escenario, indice, evitarTexto = ultimoMensajeCoping, perfil = perfil
                )
                addBot(reformular?.invoke(pauta.texto) ?: pauta.texto)
                ultimoMensajeCoping = pauta.texto

                // Historial: se anota qué se ha propuesto, para no repetirlo
                // en las próximas semanas y poder preguntar qué tal fue.
                val categoria = gestorCoping
                    .categoriasPriorizadas(escenario, indice.componenteDominante)
                    .firstOrNull()
                if (categoria != null) {
                    CatalogoPautas.elegirSinRepetir(categoria, repository.pautasRecientes())
                        ?.let { repository.registrarRecomendacion(it.id.toString(), categoria.name) }
                }

                proponerMetas(escenario, indice.componenteDominante)
            }
        }
    }

    // ====================================================================
    // CLÁUSULA DE DERIVACIÓN AL COP (§2.2.7)
    // ====================================================================
    private fun comprobarDerivacionTrasUltimoCesqt() {
        viewModelScope.launch {
            val cesqt = repository.ultimoCesqt() ?: return@launch
            val escenario = gestorCoping.clasificar(cesqt.scoreGlobalNormalizado, cargaBiometrica = null)
            if (gestorCoping.requiereDerivacion(escenario, cesqt.subscoreCulpa)) {
                iniciarDerivacion()
            }
        }
    }

    /**
     * CU-02 — NEGOCIACIÓN DE PAUTAS (§5.2): tras la evaluación, el asistente
     * no impone un plan; propone retos concretos del catálogo, priorizados
     * por el escenario (y el contexto laboral), y es el usuario quien elige
     * los que ve realistas. Lo elegido se guarda como meta y aparece en
     * Inicio para su seguimiento diario (CU-03, refuerzo positivo).
     */
    private fun proponerMetas(
        escenario: com.tfg.burnout.domain.model.Escenario,
        dominante: com.tfg.burnout.domain.model.IndiceRiesgo.Componente? = null
    ) {
        val categorias = gestorCoping.categoriasPriorizadas(escenario, dominante)
        opcionesMeta = categorias.take(3).mapNotNull { cat ->
            CatalogoPautas.de(cat).firstOrNull()?.let { it.titulo to cat.name }
        }
        if (opcionesMeta.isEmpty()) {
            // Escenario óptimo: no procede proponer retos, porque no hay nada
            // que corregir y saturar sería contraproducente. Pero el chat NO
            // debe quedar en un estado muerto: se abre el asistente para que
            // el usuario pueda seguir preguntando lo que quiera.
            addBot(
                "Por mi parte no hace falta que cambies nada: sigue como hasta " +
                "ahora. Si te apetece consultarme algo, aquí estoy."
            )
            abrirAsistenteTrasCuestionario()
            return
        }
        addBot(
            "Y ahora, cerremos el círculo: de estas ideas, ¿qué te ves capaz " +
            "de hacer esta semana? Elige la que mejor encaje con tu vida."
        )
        _ui.value = _ui.value.copy(
            chips = opcionesMeta.map { it.first }, fase = FaseChat.METAS
        )
    }

    private fun manejarEleccionMeta(opcion: String) {
        if (opcion == "Con esto me basta") {
            addBot(
                "Genial. Tus retos ya están en Inicio: márcalos cada día que " +
                "los cumplas. Y en la pestaña Actividades tienes más ideas " +
                "para cuando te apetezca."
            )
            addBot("Si quieres preguntarme algo, aquí sigo.")
            abrirAsistenteTrasCuestionario()
            return
        }
        val elegida = opcionesMeta.find { it.first == opcion } ?: return
        viewModelScope.launch {
            repository.crearMeta(categoria = elegida.second, titulo = elegida.first)
            addBot("¡Apuntado! Lo verás en Inicio para marcarlo cuando lo cumplas.")
            opcionesMeta = opcionesMeta - elegida
            if (opcionesMeta.isEmpty()) {
                addBot("Con esto tenemos un buen plan. Nos vemos en el día a día.")
                addBot("Si quieres preguntarme algo, aquí sigo.")
                abrirAsistenteTrasCuestionario()
            } else {
                _ui.value = _ui.value.copy(
                    chips = opcionesMeta.map { it.first } + "Con esto me basta",
                    fase = FaseChat.METAS
                )
            }
        }
    }

    /** Cerrado el BOT A, el chat queda en modo asistente (BOT B). */
    private fun abrirAsistenteTrasCuestionario() {
        _ui.value = _ui.value.copy(
            fase = FaseChat.ASISTENTE, chips = emptyList(), permiteTextoLibre = true
        )
    }

    /**
     * Comunica la evolución respecto al ciclo anterior. No se ejecuta en la
     * evaluación de línea base, cuando no hay nada con lo que comparar.
     */
    private suspend fun devolverEvolucion(
        actual: com.tfg.burnout.data.local.entity.CesqtResponseEntity
    ) {
        val resumen = repository.resumenDelCiclo() ?: return
        val previo = resumen.anterior

        // La Ilusión se invierte para que, en las tres dimensiones que entran
        // en el global, un valor mayor signifique siempre «peor» y la
        // comparación resulte homogénea.
        val dims = listOf(
            ComparadorCiclos.Dimension(
                "el agotamiento", previo.mediaDesgaste, actual.mediaDesgaste
            ),
            ComparadorCiclos.Dimension(
                "la distancia con el trabajo", previo.mediaIndolencia, actual.mediaIndolencia
            ),
            ComparadorCiclos.Dimension(
                "la ilusión", 4.0 - previo.mediaIlusion, 4.0 - actual.mediaIlusion
            ),
        )
        val cmp = ComparadorCiclos.comparar(
            previo.scoreGlobalNormalizado, actual.scoreGlobalNormalizado, dims
        )

        val semanas = ((java.time.LocalDate.now().toEpochDay() - previo.fechaEpochDay) / 7).toInt()
        addBot(
            "Han pasado " + (if (semanas <= 1) "unas semanas" else "$semanas semanas") +
            " desde tu evaluación anterior, así que ya puedo decirte algo sobre tu evolución."
        )

        addBot(
            when (cmp.tendencia) {
                ComparadorCiclos.Tendencia.MEJORA ->
                    "En conjunto estás mejor que entonces. No es casualidad: algo de " +
                    "lo que has ido haciendo estas semanas está funcionando."
                ComparadorCiclos.Tendencia.ESTABLE ->
                    "En conjunto te mantienes en una situación parecida a la de entonces. " +
                    "Sostenerse ya es un resultado cuando las circunstancias no han cambiado."
                ComparadorCiclos.Tendencia.EMPEORA ->
                    "En conjunto la situación ha ido a peor desde entonces. Conviene saberlo " +
                    "sin dramatizar: es información para ajustar el rumbo, no un reproche."
            }
        )

        cmp.dimensionDestacada?.let { d ->
            addBot(
                if (cmp.mejoraLaDestacada)
                    "Donde más se nota el cambio es en " + d.nombre + ", que ha mejorado."
                else
                    "Lo que más ha cambiado es " + d.nombre + ", y no en buena dirección. " +
                    "Merece la pena que le prestemos atención este mes."
            )
        }

        if (resumen.retosActivos > 0) {
            addBot(
                if (resumen.diasCumplidos == 0)
                    "De los retos que elegiste no llegaste a marcar ninguno. No pasa nada: " +
                    "quizá no encajaban con tu día a día, y esta vez buscamos otros."
                else
                    "Además, marcaste tus retos en " + resumen.diasCumplidos +
                    " días durante este ciclo. Ahí está buena parte del mérito."
            )
        }
    }

    private fun iniciarDerivacion() {
        viewModelScope.launch {
            addBot("Por lo que me cuentas, creo que te vendría bien el apoyo de un profesional. No estás solo en esto.")

            // SALVAGUARDA DE CRISIS AGUDA: además de la derivación ordinaria,
            // se ofrece SIEMPRE una línea de ayuda inmediata 24/7 (la app no es
            // un servicio de emergencias; ver ModuloEticoRuteo.lineaCrisis()).
            val crisis = moduloEtico.lineaCrisis()
            addBot(
                "Si en algún momento sientes que no puedes más y necesitas hablar " +
                "con alguien ahora mismo, tienes la ${crisis.nombre}. " +
                "Llama al ${crisis.telefono}, a cualquier hora."
            )

            addBot("Y para un acompañamiento profesional continuado: ¿en qué provincia estás? Así te paso el contacto más cercano.")
            // Los chips muestran algunas provincias frecuentes + "Otra"; en la
            // app real, "Otra" desplegaría el listado completo de 52 provincias.
            val provincias = moduloEtico.provinciasDisponibles().take(3) + "Otra"
            _ui.value = _ui.value.copy(chips = provincias, fase = FaseChat.DERIVACION)
        }
    }

    private fun resolverProvincia(provincia: String) {
        viewModelScope.launch {
            val sede = moduloEtico.resolverSede(provincia)
            if (sede != null) {
                addBot("${sede.nombreColegio}\nTel.: ${sede.telefono}\nWeb: ${sede.web}")
            } else {
                addBot("Puedes localizar tu colegio profesional en la web del Consejo General de la Psicología (cop.es).")
            }
            addBot("Sigo por aquí si quieres preguntarme cualquier cosa.")
            _ui.value = _ui.value.copy(
                chips = emptyList(), fase = FaseChat.ASISTENTE, permiteTextoLibre = true
            )
        }
    }

    class Factory(
        private val repository: BurnoutRepository,
        private val gestorCoping: GestorCoping,
        private val moduloEtico: ModuloEticoRuteo,
        private val reformular: (suspend (String) -> String?)? = null,
        private val asistente: com.tfg.burnout.data.rag.AsistenteRag? = null,
        private val contexto: android.content.Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatbotViewModel(
                repository, gestorCoping, moduloEtico,
                reformular = reformular, asistente = asistente, contexto = contexto
            ) as T
    }
}
