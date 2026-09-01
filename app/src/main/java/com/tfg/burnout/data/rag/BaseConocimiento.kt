package com.tfg.burnout.data.rag

/**
 * Un fragmento de conocimiento recuperable, con su fuente.
 *
 * @param id        identificador estable (para trazar qué se usó).
 * @param titulo    encabezado corto, útil para depurar y para citar.
 * @param texto     contenido en lenguaje llano; es lo que se le pasa al modelo.
 * @param fuente    referencia bibliográfica visible al usuario.
 * @param etiquetas términos de indexación adicionales (sinónimos, coloquiales).
 */
data class Fragmento(
    val id: String,
    val titulo: String,
    val texto: String,
    val fuente: String,
    /**
     * Enlace estable a la fuente —DOI cuando existe— para que el usuario
     * pueda acudir al original si quiere profundizar. Null cuando la fuente
     * no dispone de identificador persistente.
     */
    val enlace: String? = null,
    val etiquetas: List<String> = emptyList()
)

/**
 * BASE DE CONOCIMIENTO LOCAL DEL ASISTENTE (Tarea 4).
 *
 * El BOT B no responde de memoria del modelo: responde a partir de estos
 * fragmentos, redactados a partir de las fuentes oficiales que sustentan el
 * marco teórico de este trabajo (INSST, OMS, Gil-Monte y la literatura
 * revisada en el Capítulo 2). Si ninguna entrada cubre la pregunta, el
 * asistente lo dice en lugar de improvisar.
 *
 * DECISIÓN DE DISEÑO: se emplean fragmentos de texto curados en lugar de
 * indexar los PDF originales. Motivos: (a) un modelo de 270M rinde mucho
 * mejor con contexto breve y limpio que con extractos crudos de PDF;
 * (b) evita empaquetar documentos de terceros en el APK, con sus
 * implicaciones de licencia y de tamaño; (c) hace auditable el contenido,
 * porque cada fragmento es revisable de un vistazo.
 */
object BaseConocimiento {

    /**
     * Corpus completo: los fragmentos teóricos redactados a mano MÁS las
     * actividades del catálogo, indexadas automáticamente.
     *
     * De este modo el asistente puede responder tanto «¿qué es el burnout?»
     * como «¿qué puedo hacer para dormir mejor?» o «¿en qué consiste el
     * apagón de pantallas?», sin duplicar el contenido de las pautas ni
     * arriesgarse a que queden desincronizadas: si se añade una actividad al
     * catálogo, el asistente la conoce de inmediato.
     */
    val fragmentos: List<Fragmento> by lazy { fragmentosTeoricos + fragmentosDeActividades() }

    /**
     * Convierte cada pauta del catálogo en un fragmento recuperable. El
     * nombre de la categoría se añade como etiqueta para que preguntas
     * genéricas («dime algo de mindfulness») también las encuentren.
     */
    private fun fragmentosDeActividades(): List<Fragmento> =
        com.tfg.burnout.domain.model.CategoriaCoping.entries.flatMap { categoria ->
            com.tfg.burnout.domain.engine.CatalogoPautas.de(categoria).map { pauta ->
                Fragmento(
                    id = "actividad_" + pauta.id,
                    titulo = pauta.titulo,
                    texto = pauta.titulo + ": " + pauta.descripcion,
                    fuente = "Catálogo de actividades del sistema (" + categoria.etiqueta + ")",
                    etiquetas = listOf(
                        categoria.etiqueta, "actividad", "ejercicio", "pauta",
                        "que puedo hacer", "recomendacion"
                    ) + etiquetaDeTituloEscueto(pauta.titulo)
                )
            }
        }

    /**
     * Las palabras del título NO se indexan como etiquetas: ya viajan en el
     * cuerpo, porque `texto` es el título más la descripción. Duplicarlas les
     * daba peso doble y, sobre todo, permitía que una palabra incidental
     * acreditara pertinencia ella sola: «mañana», del título «Luz por la
     * mañana», hacía que el asistente respondiera a «¿qué tiempo hace mañana?».
     *
     * Única excepción, la que aquí se resuelve: los títulos que aportan UN
     * SOLO término con contenido. En ellos la vía del cuerpo no basta, porque
     * el filtro de pertinencia exige dos coincidencias distintas, y la pauta
     * dejaría de encontrarse por su nombre («Nombrar lo que pasa»,
     * «Recuperar el porqué», «Algo que no rinda»). Al tratarse de un único
     * término sustantivo, no reintroduce el problema anterior.
     */
    private fun etiquetaDeTituloEscueto(titulo: String): List<String> {
        val conContenido = BuscadorRag.tokenizar(titulo)
        return if (conContenido.size == 1) conContenido else emptyList()
    }

    private val fragmentosTeoricos: List<Fragmento> = listOf(

        // ---------- QUÉ ES EL BURNOUT ----------
        Fragmento(
            id = "def_burnout",
            titulo = "Qué es el burnout",
            texto = "El burnout, o síndrome de quemarse por el trabajo, es la consecuencia " +
                "de un estrés laboral crónico que no se ha conseguido manejar. La " +
                "Organización Mundial de la Salud lo reconoce como un fenómeno " +
                "ocupacional, no como una enfermedad: es decir, tiene su origen en las " +
                "condiciones de trabajo, no en un defecto de la persona. Se manifiesta " +
                "en tres señales: falta de energía o agotamiento, distancia mental " +
                "respecto al trabajo (incluidos el cinismo y la indiferencia), y una " +
                "sensación de ineficacia o de no llegar a lo que uno espera de sí mismo.",
            fuente = "OMS, CIE-11 (2019)",
            enlace = "https://icd.who.int/browse11/",
            etiquetas = listOf("burnout", "quemado", "definicion", "que es", "sindrome", "desgaste")
        ),
        Fragmento(
            id = "burnout_vs_fatiga",
            titulo = "Diferencia entre burnout y cansancio normal",
            texto = "No es lo mismo estar cansado que estar quemado. La fatiga es un " +
                "estado temporal que se recupera con el descanso normal, como un fin de " +
                "semana, y suele convivir con la sensación de hacer bien el trabajo. El " +
                "burnout, en cambio, es un deterioro lento y sostenido: la recuperación " +
                "es mucho más lenta y viene acompañado de una valoración negativa de uno " +
                "mismo y de actitudes de distancia hacia el trabajo.",
            fuente = "INSST, NTP 732 (2006)",
            enlace = "https://www.insst.es/documents/94886/7850762/ntp_732.pdf",
            etiquetas = listOf("cansancio", "fatiga", "diferencia", "estres", "agotado")
        ),
        Fragmento(
            id = "culpa_perfil2",
            titulo = "El papel de la culpa",
            texto = "Cuando al desgaste y a la indiferencia se les suma el remordimiento " +
                "por cómo uno está tratando a los demás en el trabajo, se describe el " +
                "cuadro más severo. La culpa alimenta un círculo que intensifica los " +
                "síntomas, y en ese punto el acompañamiento de un profesional deja de " +
                "ser recomendable para ser necesario.",
            fuente = "Gil-Monte (2005); Gil-Monte, Unda y Sandoval (2009)",
            enlace = "https://www.scielo.org.mx/pdf/sm/v32n3/v32n3a4.pdf",
            etiquetas = listOf("culpa", "remordimiento", "perfil 2", "grave", "peor")
        ),
        Fragmento(
            id = "causas_organizativas",
            titulo = "De dónde viene el burnout",
            texto = "El burnout no aparece por falta de fortaleza personal. Nace de la " +
                "interacción entre la persona y unas condiciones de trabajo concretas: " +
                "sobrecarga, poco margen para decidir cómo se hace el trabajo, falta de " +
                "apoyo de compañeros o superiores, ambigüedad sobre lo que se espera de " +
                "uno, o escaso reconocimiento. Por eso la prevención corresponde en " +
                "buena parte a la organización, no solo al trabajador.",
            fuente = "INSST, NTP 704 y 705 (2005); método FPSICO",
            enlace = "https://www.insst.es/documents/94886/7852705/ntp_704.pdf",
            etiquetas = listOf("causas", "por que", "origen", "culpa mia", "organizacion", "empresa", "jefe")
        ),

        // ---------- CONSECUENCIAS ----------
        Fragmento(
            id = "efectos_salud",
            titulo = "Cómo afecta a la salud",
            texto = "El desgaste sostenido deja huella en el cuerpo: alteraciones del " +
                "sueño e insomnio, aumento de la frecuencia cardíaca en reposo, dolores " +
                "de cabeza, tensión muscular en cuello y espalda, y molestias " +
                "digestivas. En el plano psicológico aparecen irritabilidad, baja " +
                "tolerancia a la frustración y pérdida de ilusión, y con frecuencia " +
                "termina afectando también a la vida familiar y social.",
            fuente = "INSST (2005); Joffre-Velázquez et al. (2008)",
            enlace = "https://www.insst.es/documents/94886/7852705/ntp_705.pdf",
            etiquetas = listOf("salud", "sintomas", "cuerpo", "efectos", "consecuencias", "insomnio", "dolor")
        ),

        // ---------- SUEÑO ----------
        Fragmento(
            id = "sueno_importancia",
            titulo = "Por qué importa el sueño",
            texto = "El sueño es el mejor indicador de recuperación que se puede medir sin " +
                "molestar a la persona. Dormir mal reduce la capacidad de afrontar el día " +
                "siguiente, y la mala calidad del sueño acompaña a una gran mayoría de " +
                "los casos de desgaste profesional. Por eso el sistema le da un peso " +
                "mayor que al resto de señales.",
            fuente = "Rodríguez Torres et al. (2023); de Vries et al. (2023)",
            enlace = "https://doi.org/10.3390/s23010332",
            etiquetas = listOf("sueño", "dormir", "descanso", "insomnio", "por que sueño")
        ),
        Fragmento(
            id = "higiene_sueno",
            titulo = "Cómo mejorar el descanso",
            texto = "Lo que más ayuda a dormir mejor no es un truco aislado, sino la " +
                "constancia: mantener horarios parecidos también el fin de semana, " +
                "apagar las pantallas al menos media hora antes de acostarse, preparar " +
                "una habitación oscura, silenciosa y algo fresca, evitar cafeína desde " +
                "media tarde y cenas copiosas, y reservar los últimos minutos del día a " +
                "algo tranquilo que marque el final de la jornada.",
            fuente = "Blasco Espinosa et al. (2002)",
            etiquetas = listOf("dormir mejor", "higiene del sueño", "consejos sueño", "insomnio", "pantallas")
        ),

        // ---------- VARIABILIDAD Y PULSO ----------
        Fragmento(
            id = "hrv_que_es",
            titulo = "Qué es la variabilidad cardíaca",
            texto = "La variabilidad de la frecuencia cardíaca mide las pequeñas " +
                "diferencias de tiempo entre un latido y el siguiente. Aunque suene " +
                "contraintuitivo, que esas diferencias sean amplias es buena señal: " +
                "indica que el sistema nervioso está descansado y sabe adaptarse. " +
                "Cuando baja de forma sostenida respecto a lo habitual en esa persona, " +
                "suele reflejar tensión acumulada. Se mide durante la noche, porque " +
                "durante el día el movimiento y el ejercicio la distorsionan.",
            fuente = "Li et al. (2022); Stone et al. (2021)",
            enlace = "https://doi.org/10.3389/fpubh.2021.810577",
            etiquetas = listOf("variabilidad", "hrv", "rmssd", "pulso", "corazon", "latido")
        ),

        // ---------- AFRONTAMIENTO ----------
        Fragmento(
            id = "coping_multicomponente",
            titulo = "Qué funciona para afrontarlo",
            texto = "Las estrategias individuales, como cuidar el sueño, hacer ejercicio " +
                "o practicar atención plena, son útiles y necesarias, pero pierden " +
                "fuerza si el entorno laboral sigue siendo adverso. Las intervenciones " +
                "que mejor funcionan combinan ambos planos: lo que uno puede cambiar en " +
                "su día a día y lo que corresponde ajustar en la organización del " +
                "trabajo.",
            fuente = "Sari-Nieves et al. (2026); Schaufeli (2006)",
            enlace = "https://doi.org/10.46296/yc.v10i18.0850",
            etiquetas = listOf("que hago", "que puedo hacer", "afrontar", "solucion",
                "mejorar", "estrategias", "deporte", "ejercicio", "actividad fisica",
                "correr", "gimnasio", "funciona", "sirve de algo")
        ),
        Fragmento(
            id = "limites_apoyo",
            titulo = "Poner límites y pedir apoyo",
            texto = "Dos de las medidas con más respaldo son también las que más cuestan: " +
                "poner límites y pedir ayuda. Poner límites significa negociar plazos " +
                "realistas y decir que no a lo que no cabe, en lugar de absorberlo en " +
                "silencio. Pedir apoyo, hablar con alguien de confianza dentro o fuera " +
                "del trabajo, protege frente al aislamiento, que es uno de los factores " +
                "que más agrava el desgaste.",
            fuente = "Schaufeli (2006), «10 reglas de oro»",
            etiquetas = listOf("limites", "decir no", "apoyo", "hablar", "compañeros", "soledad")
        ),
        Fragmento(
            id = "mindfulness_act",
            titulo = "Atención plena y aceptación",
            texto = "Las prácticas de atención plena y las terapias de aceptación ayudan " +
                "sobre todo con aquello que no se puede cambiar. No se trata de " +
                "resignarse, sino de dejar de gastar energía peleando con lo " +
                "inmodificable para reservarla donde sí hay margen. Bastan ejercicios " +
                "breves de respiración pausada o un repaso corporal de unos minutos.",
            fuente = "OMS (2022); Quiñones Ramírez y Arreola Medina (2022)",
            enlace = "https://www.who.int/publications/i/item/9789240053052",
            etiquetas = listOf("mindfulness", "respiracion", "meditar", "relajacion", "aceptacion", "calma")
        ),
        Fragmento(
            id = "reestructuracion",
            titulo = "Replantear la mirada sobre el trabajo",
            texto = "La reestructuración cognitiva consiste en revisar la interpretación " +
                "automática que hacemos de lo que pasa. Ante una jornada que se " +
                "percibe como un fracaso completo, ayuda separar los hechos concretos " +
                "de la conclusión general, y priorizar por escrito lo que de verdad " +
                "importa frente a lo que solo parece urgente.",
            fuente = "Quiñones Ramírez y Arreola Medina (2022)",
            etiquetas = listOf("pensamientos", "reencuadre", "priorizar", "agobio", "sobrecarga")
        ),
        Fragmento(
            id = "desconexion",
            titulo = "Desconectar del trabajo",
            texto = "En trabajos digitalizados la frontera entre jornada y descanso se " +
                "difumina, y esa continuidad impide recuperarse. Ayuda fijar una hora " +
                "de cierre y respetarla, sacar las notificaciones del trabajo del móvil " +
                "personal fuera de horario, y crear un pequeño ritual que marque el " +
                "final: un paseo corto, una ducha, cambiar de ropa.",
            fuente = "Blasco Espinosa et al. (2002); INSST (2005)",
            etiquetas = listOf("desconectar", "movil", "correos", "fuera de horario", "descansar")
        ),

        // ---------- AYUDA PROFESIONAL ----------
        Fragmento(
            id = "cuando_profesional",
            titulo = "Cuándo acudir a un profesional",
            texto = "Conviene buscar ayuda profesional cuando el malestar se sostiene en " +
                "el tiempo pese a haber intentado cambios, cuando interfiere con el " +
                "sueño, la salud o las relaciones, o cuando aparecen sentimientos de " +
                "culpa persistentes. La evidencia muestra que la orientación de " +
                "especialistas en salud laboral acelera de forma notable la " +
                "recuperación. En España se puede acudir al Colegio Oficial de " +
                "Psicología de cada provincia, al servicio de prevención de la propia " +
                "empresa o al médico de familia.",
            fuente = "Van der Klink et al. (2003); Consejo General de la Psicología",
            enlace = "https://doi.org/10.1136/oem.60.6.429",
            // «ayuda» a secas se retiró: aparecía en consultas de todo tipo
            // («¿el deporte ayuda?») y arrastraba este fragmento sin venir a
            // cuento. Se conservan las formas específicas.
            etiquetas = listOf("psicologo", "psicologa", "ayuda profesional", "terapia",
                "medico", "cuando pedir ayuda", "buscar ayuda", "derivacion", "colegiado")
        ),

        // ---------- SOBRE LA PROPIA APP ----------
        // ---------- IDENTIDAD Y ALCANCE DEL ASISTENTE ----------
        Fragmento(
            id = "asistente_quien_soy",
            titulo = "Quién soy",
            texto = "Soy el asistente de esta aplicación de bienestar laboral. No soy " +
                "una persona ni un profesional sanitario, y tampoco una inteligencia " +
                "artificial que improvise: sigo guiones preparados y respondo a partir " +
                "de fuentes contrastadas sobre desgaste profesional. Puedo acompañarte, " +
                "explicarte cosas y proponerte pautas, pero no puedo diagnosticarte.",
            fuente = "Documentación del sistema",
            // OJO: nada de «quien soy». Esa expresión la usa el USUARIO para
            // hablar de sí mismo, y etiquetarla aquí hacía que preguntar
            // «¿quién soy yo?» devolviera la presentación del asistente.
            etiquetas = listOf("hola","buenas","buenos dias","buenas tardes",
                "quien eres","que eres","eres humano","eres una persona",
                "como te llamas","preseNtate","eres real","eres un bot")
        ),
        Fragmento(
            id = "asistente_que_puedo_preguntar",
            titulo = "Con qué puedo ayudarte",
            texto = "Puedo hablarte de qué es el burnout y en qué se diferencia del " +
                "cansancio normal, de por qué importan el sueño y el pulso, de las " +
                "actividades que propone la aplicación y cómo hacerlas, de cuándo " +
                "conviene acudir a un profesional, y de cómo funciona la propia " +
                "aplicación y qué pasa con tus datos. Si me preguntas otra cosa te lo " +
                "diré con franqueza en lugar de improvisar una respuesta.",
            fuente = "Documentación del sistema",
            etiquetas = listOf("ayuda","que puedes hacer","para que sirves","opciones",
                "que sabes","de que hablas","temas","que puedo preguntarte","funciones")
        ),
        Fragmento(
            id = "app_que_hace",
            titulo = "Qué hace esta aplicación",
            texto = "Esta aplicación cruza dos fuentes de información: lo que tú cuentas " +
                "en el cuestionario y lo que registra tu reloj sobre el sueño y el " +
                "pulso. Con ello calcula una orientación sobre cómo va tu nivel de " +
                "energía y te propone pautas ajustadas a tu situación. No es una " +
                "herramienta de diagnóstico: no dice si tienes o no burnout, sino que " +
                "te ayuda a observarte y, si hace falta, te acerca a ayuda profesional.",
            fuente = "Documentación del sistema",
            etiquetas = listOf("app", "aplicacion", "que hace", "para que sirve", "como funciona")
        ),
        Fragmento(
            id = "app_privacidad",
            titulo = "Qué pasa con mis datos",
            texto = "Todo se queda en tu móvil. La aplicación no envía nada a ningún " +
                "servidor: funciona sin conexión y la base de datos está cifrada, con " +
                "la clave protegida por el sistema de seguridad de Android. Nadie más " +
                "puede ver tus respuestas, y menos aún tu empresa. Puedes exportar tus " +
                "datos cuando quieras, y si desinstalas la aplicación desaparecen.",
            fuente = "RGPD, arts. 5, 9, 20 y 25; AEPD",
            enlace = "https://www.aepd.es/guias/guia-profesionales-sector-sanitario.pdf",
            etiquetas = listOf("datos", "privacidad", "seguridad", "empresa", "quien ve", "cifrado", "rgpd")
        ),
        Fragmento(
            id = "app_periodicidad",
            titulo = "Cada cuánto se mide",
            texto = "El cuestionario completo se repite una vez al mes, porque el " +
                "desgaste profesional evoluciona despacio y preguntarlo a diario solo " +
                "generaría cansancio sin aportar información nueva. Los datos de sueño " +
                "y pulso se recogen solos cada madrugada, y la valoración general se " +
                "recalcula cada cuatro semanas sobre esa media, para que una mala noche " +
                "suelta no altere el resultado.",
            fuente = "de Vries et al. (2023); Gil-Monte (2005)",
            enlace = "https://doi.org/10.3390/s23010332",
            etiquetas = listOf("cada cuanto", "frecuencia", "cuestionario", "cuando", "periodicidad", "test")
        ),
    )
}
