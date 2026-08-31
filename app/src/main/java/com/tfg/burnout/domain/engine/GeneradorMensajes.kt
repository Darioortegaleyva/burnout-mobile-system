package com.tfg.burnout.domain.engine

import com.tfg.burnout.domain.cesqt.DimensionCesqt
import com.tfg.burnout.domain.model.Escenario
import com.tfg.burnout.domain.model.IndiceRiesgo
import com.tfg.burnout.domain.model.PerfilContexto
import kotlin.random.Random

/**
 * GENERADOR DE MENSAJES DEL ASISTENTE (§2.3.5).
 *
 * Resuelve la pregunta "¿cómo se genera la frase que se le dice al usuario?".
 * El asistente NO es un modelo de lenguaje generativo: es un motor de
 * plantillas basado en reglas, coherente con la arquitectura offline-first y
 * con el principio de no diagnóstico (§2.3.3). Esta decisión es deliberada:
 * en un dominio de salud mental, un texto generado libremente podría producir
 * un consejo inadecuado a una persona vulnerable; un banco de frases validadas
 * garantiza que TODO lo que se dice ha sido revisado y tiene respaldo en la
 * literatura.
 *
 * La variación (que el sistema "no diga siempre la misma frase") se consigue
 * eligiendo, dentro del bloque que corresponde al estado del usuario, una
 * plantilla concreta en función de:
 *   1. el ESCENARIO A–D (Matriz de Categorización, §2.3.4),
 *   2. el COMPONENTE dominante del Índice R (sueño, HRV o psicométrico),
 *   3. una rotación pseudo-aleatoria que evita la repetición inmediata.
 *
 * Cada plantilla lleva asociada su fuente en el comentario, de modo que la
 * frase mostrada es trazable hasta la evidencia que la respalda.
 */
class GeneradorMensajes(
    private val aleatorio: Random = Random.Default
) {

    /**
     * Un mensaje del asistente y la fuente que lo respalda (para trazabilidad
     * y para la defensa; la fuente no se muestra al usuario).
     */
    data class Mensaje(val texto: String, val fuente: String, val tema: Tema = Tema.GENERAL)

    /** Tema del mensaje, usado para la modulación por contexto laboral (§5.5). */
    enum class Tema { GENERAL, LIMITES, APOYO }

    /**
     * Devuelve un mensaje de afrontamiento adecuado al estado actual.
     *
     * @param escenario   escenario A–D ya clasificado por GestorCoping.
     * @param indice      índice de riesgo (aporta el componente dominante).
     * @param evitarTexto último mensaje mostrado, para no repetirlo (rotación).
     */
    fun mensajeDeAfrontamiento(
        escenario: Escenario,
        indice: IndiceRiesgo,
        evitarTexto: String? = null,
        perfil: PerfilContexto? = null
    ): Mensaje {
        val banco = when (escenario) {
            Escenario.A_OPTIMO -> bancoA
            // MODULACIÓN POR CONTEXTO (§5.5): ante el mismo escenario, el
            // contexto laboral prioriza el tema de la pauta. Con poca
            // autonomía y mucha carga -> límites; con poco apoyo -> apoyo
            // social. Si el filtro vaciara el banco, se mantiene completo.
            Escenario.B_ACTITUDINAL -> when {
                perfil == null -> bancoB
                perfil.autonomia <= PerfilContexto.BAJA && perfil.carga >= PerfilContexto.ALTA ->
                    bancoB.filter { it.tema == Tema.LIMITES }.ifEmpty { bancoB }
                perfil.apoyo <= PerfilContexto.BAJA ->
                    bancoB.filter { it.tema == Tema.APOYO }.ifEmpty { bancoB }
                else -> bancoB
            }
            Escenario.C_PRESENTISMO -> bancoC(indice.componenteDominante)
            Escenario.D_SEVERO -> bancoD
        }
        return elegirVariando(banco, evitarTexto)
    }

    /**
     * Mensaje de desculpabilización previo a cualquier pauta (§2.3.1): recuerda
     * que el desgaste tiene raíz organizacional, no personal, para no "echar la
     * culpa a la víctima" (Schaufeli, 2006).
     */
    fun mensajeDesculpabilizador(
        evitarTexto: String? = null,
        perfil: PerfilContexto? = null
    ): Mensaje {
        // Si el usuario declaró carga alta, se prioriza la variante que la
        // nombra explícitamente ("con la carga que describes..."), reforzando
        // el enfoque estructural (§2.3.1) con su propio contexto.
        if (perfil != null && perfil.carga >= PerfilContexto.ALTA) {
            val contextual = bancoDesculpabilizacion.firstOrNull {
                it.texto != evitarTexto && it.texto.contains("carga")
            }
            if (contextual != null) return contextual
        }
        return elegirVariando(bancoDesculpabilizacion, evitarTexto)
    }

    // ------------------------------------------------------------------
    // Selección con variación: elige una plantilla distinta a la última.
    // ------------------------------------------------------------------
    private fun elegirVariando(banco: List<Mensaje>, evitar: String?): Mensaje {
        if (banco.isEmpty()) return Mensaje("", "")
        val candidatos = banco.filter { it.texto != evitar }.ifEmpty { banco }
        return candidatos[aleatorio.nextInt(candidatos.size)]
    }

    // ==================================================================
    // BANCOS DE FRASES POR ESCENARIO
    // Mismo significado clínico, distinta redacción => variación real.
    // ==================================================================

    /** Escenario A (Óptimo): refuerzo positivo, sin saturar. */
    private val bancoA = listOf(
        Mensaje(
            "Tus datos y tus respuestas apuntan a una buena etapa. Sigue cuidándote como hasta ahora.",
            "Refuerzo positivo / prevención primaria (INSST, 2005)"
        ),
        Mensaje(
            "Todo indica que estás manteniendo un buen equilibrio. Me alegra ver que lo llevas bien.",
            "Prevención primaria (INSST, 2005)"
        ),
        Mensaje(
            "Vas por buen camino. No hace falta que cambies nada; solo seguir con tus buenos hábitos.",
            "Refuerzo positivo (Schaufeli, 2006)"
        ),
    )

    /** Escenario B (Deterioro actitudinal): foco cognitivo-social, sin sueño. */
    private val bancoB = listOf(
        Mensaje(
            "Parece que el trabajo te está pesando más de lo normal últimamente. ¿Y si hoy intentas reenfocar una de tus tareas o hablarlo con alguien de confianza?",
            "Reestructuración cognitiva (Quiñones y Arreola, 2022)"
        ),
        Mensaje(
            "Noto cierto desencanto en cómo describes tu día a día. A veces ayuda mirar una situación concreta desde otro ángulo, en vez de cargar con todo el conjunto.",
            "Reencuadre cognitivo (Quiñones y Arreola, 2022)"
        ),
        Mensaje(
            "Cuando la ilusión baja, apoyarse en los demás marca la diferencia. ¿Hay algún compañero o amigo con quien puedas soltar un poco lo de hoy?",
            "Apoyo social (Schaufeli, 2006)", Tema.APOYO
        ),
        Mensaje(
            "Cuando la carga es alta y el margen es poco, proteger tu tiempo es la pauta que más rinde: elige una tarea que hoy puedas decir que no, o negociar para otro día.",
            "Límites asertivos / 10 reglas de oro (Schaufeli, 2006)", Tema.LIMITES
        ),
        Mensaje(
            "Descansas bien, y eso es una buena base. Lo que hoy conviene cuidar es el ánimo con el que afrontas las tareas: prueba a priorizar solo lo que de verdad importa.",
            "Matriz de prioridades / afrontamiento activo (Quiñones y Arreola, 2022)", Tema.LIMITES
        ),
    )

    /**
     * Escenario C (Presentismo invisible): foco fisiológico. El mensaje
     * concreto depende del componente biométrico dominante del índice.
     */
    private fun bancoC(componente: IndiceRiesgo.Componente): List<Mensaje> = when (componente) {
        IndiceRiesgo.Componente.SUENO -> listOf(
            Mensaje(
                "Aunque te sientas con energía, tu descanso de estos días dice que tu cuerpo necesita recuperarse. Hoy vendría bien apagar pantallas pronto y darte una pausa de verdad.",
                "Higiene del sueño (Blasco Espinosa et al., 2002)"
            ),
            Mensaje(
                "Por dentro parece que todo va bien, pero tu sueño se está resintiendo. Una rutina tranquila antes de acostarte puede ayudarte a recuperar terreno.",
                "Rutina de wind-down (Blasco Espinosa et al., 2002)"
            ),
        )
        IndiceRiesgo.Componente.HRV -> listOf(
            Mensaje(
                "Tu cuerpo muestra señales de tensión acumulada aunque no las notes del todo. Unos minutos de respiración pausada hoy pueden ayudarte a soltar esa carga.",
                "Desactivación fisiológica / mindfulness (OMS, 2022)"
            ),
            Mensaje(
                "Aunque de ánimo estés bien, tu organismo lleva un ritmo alto. Prueba a hacer una pausa breve y consciente a media jornada.",
                "Pausa plena (OMS, 2022)"
            ),
        )
        IndiceRiesgo.Componente.PSICOMETRICO -> listOf(
            Mensaje(
                "Tus datos físicos piden algo de recuperación aunque tú no lo percibas. Hoy prioriza el descanso por encima de una tarea más.",
                "Higiene del sueño y desconexión (Blasco Espinosa et al., 2002)"
            ),
        )
    }

    /** Escenario D (Desgaste severo): acompañamiento + antesala de derivación. */
    private val bancoD = listOf(
        Mensaje(
            "Lo que me cuentas y lo que veo en tus datos coinciden en una cosa: llevas una temporada difícil, y no tienes por qué atravesarla en solitario.",
            "Prevención terciaria / derivación (Schaufeli, 2006)"
        ),
        Mensaje(
            "Tanto tus sensaciones como tu cuerpo están dando señales claras de agotamiento. En momentos así, el mejor paso es apoyarse en un profesional.",
            "Derivación a salud ocupacional (Van der Klink et al., 2003)"
        ),
    )

    // ==================================================================
    // ACUSES DE RECIBO SENSIBLES A LA RESPUESTA (§2.3.1 / §2.3.5)
    // La "inteligencia" del asistente durante el cuestionario no está en las
    // preguntas (que son inmutables: alterarlas invalidaría el instrumento),
    // sino en entender lo que el usuario acaba de responder. Una puntuación
    // alta en Ilusión por el trabajo es una BUENA noticia; la misma
    // puntuación en Desgaste o Culpa es preocupante. El acuse se elige según
    // esa valencia, con variación para no sonar a guion.
    // ==================================================================

    enum class Valencia { POSITIVA, NEUTRA, PREOCUPANTE }

    /** Traduce (dimensión, valor 0–4) a la valencia de la respuesta. */
    fun valenciaDe(dimension: DimensionCesqt, valor: Int): Valencia {
        val positivoEsAlto = dimension == DimensionCesqt.ILUSION_POR_TRABAJO
        val v = valor.coerceIn(0, 4)
        return when {
            positivoEsAlto && v >= 3 -> Valencia.POSITIVA
            positivoEsAlto && v <= 1 -> Valencia.PREOCUPANTE
            !positivoEsAlto && v <= 1 -> Valencia.POSITIVA
            !positivoEsAlto && v >= 3 -> Valencia.PREOCUPANTE
            else -> Valencia.NEUTRA
        }
    }

    /** Acuse de recibo tras un ítem del cuestionario, acorde a la respuesta. */
    fun acuseCesqt(dimension: DimensionCesqt, valor: Int, evitarTexto: String? = null): Mensaje {
        val banco = when (valenciaDe(dimension, valor)) {
            Valencia.POSITIVA -> acusesPositivos
            Valencia.NEUTRA -> acusesNeutros
            Valencia.PREOCUPANTE -> acusesPreocupantes
        }
        return elegirVariando(banco, evitarTexto)
    }

    private val acusesPositivos = listOf(
        Mensaje("Me alegra leer eso.", "Validación empática (§2.3.1)"),
        Mensaje("Qué bien; eso suma.", "Validación empática (§2.3.1)"),
        Mensaje("Buena señal. Gracias.", "Validación empática (§2.3.1)"),
        Mensaje("Estupendo, lo anoto.", "Validación empática (§2.3.1)"),
    )
    private val acusesNeutros = listOf(
        Mensaje("Anotado, gracias.", "Validación empática (§2.3.1)"),
        Mensaje("De acuerdo, seguimos.", "Validación empática (§2.3.1)"),
        Mensaje("Entendido.", "Validación empática (§2.3.1)"),
        Mensaje("Vale, lo tengo.", "Validación empática (§2.3.1)"),
    )
    private val acusesPreocupantes = listOf(
        Mensaje("Gracias por tu sinceridad; lo tengo en cuenta.", "Validación empática (§2.3.1)"),
        Mensaje("Entiendo. Contarlo ya es un paso.", "Validación empática (§2.3.1)"),
        Mensaje("Lo apunto. Gracias por ser honesto.", "Validación empática (§2.3.1)"),
        Mensaje("Vale, lo tengo presente.", "Validación empática (§2.3.1)"),
    )

    /**
     * Respuesta final de la micro-interacción EMA, acorde a cómo ha ido el
     * día y a si las exigencias superaron a la persona. Nada de "es normal
     * sentirse así" cuando el día ha ido bien.
     */
    fun respuestaEma(diaBueno: Boolean, exigenciasAltas: Boolean, evitarTexto: String? = null): Mensaje {
        val banco = when {
            diaBueno && !exigenciasAltas -> emaPositiva
            !diaBueno && exigenciasAltas -> emaDura
            else -> emaMixta
        }
        return elegirVariando(banco, evitarTexto)
    }

    private val emaPositiva = listOf(
        Mensaje("¡Me alegra que haya ido bien! Que se repita mañana.", "Refuerzo positivo (INSST, 2005)"),
        Mensaje("Qué bien suena eso. Disfruta el resto del día.", "Refuerzo positivo (INSST, 2005)"),
        Mensaje("Genial. Días así también cuentan, y mucho.", "Refuerzo positivo (INSST, 2005)"),
    )
    private val emaMixta = listOf(
        Mensaje("Gracias por contármelo. Ni todos los días son redondos ni tienen por qué serlo.", "Validación empática (§2.3.1)"),
        Mensaje("Anotado. Un día regular no dice nada malo de ti.", "Validación empática (§2.3.1)"),
        Mensaje("Lo tengo. Mañana volvemos a mirarlo con calma.", "Validación empática (§2.3.1)"),
    )
    private val emaDura = listOf(
        Mensaje("Gracias por contármelo. Es comprensible acabar así cuando la carga aprieta. Lo tengo en cuenta.", "Desculpabilización (Schaufeli, 2006)"),
        Mensaje("Vaya día. Que sepas que sentirse superado en jornadas así es una reacción esperable, no un fallo tuyo.", "Desculpabilización (Schaufeli, 2006)"),
        Mensaje("Lo siento. Apunto que hoy ha sido de los duros; si te apetece, en Actividades hay alguna pauta corta para soltar el día.", "Pauta táctica (§5.4)"),
    )

    /** Desculpabilización previa a las pautas (aplica sobre todo al Perfil 2). */
    private val bancoDesculpabilizacion = listOf(
        Mensaje(
            "Antes de nada: sentirse así cuando las exigencias son altas es una respuesta normal, no una falla tuya.",
            "Anti \u201cculpar a la víctima\u201d (Schaufeli, 2006)"
        ),
        Mensaje(
            "Que estés agotado no significa que lo estés haciendo mal. El desgaste casi siempre nace de las condiciones, no de la persona.",
            "Origen organizacional del burnout (INSST, 2005)"
        ),
        Mensaje(
            "Esto no va de aguantar más ni de gestionarlo mejor tú solo. Buena parte del problema está en el entorno, y conviene recordarlo.",
            "Enfoque estructural (Schaufeli, 2006)"
        ),
        Mensaje(
            "Con la carga que describes, es esperable sentirse así. No es un fallo tuyo: es una situación exigente.",
            "Desculpabilización contextualizada (INSST, 2005; Schaufeli, 2006)"
        ),
    )
}
