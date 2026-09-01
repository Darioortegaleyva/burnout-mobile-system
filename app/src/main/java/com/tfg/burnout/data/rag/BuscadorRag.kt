package com.tfg.burnout.data.rag

import java.text.Normalizer

/**
 * RECUPERADOR LÉXICO (Tarea 4).
 *
 * Implementa la parte de «retrieval» del RAG con un esquema TF-IDF ligero
 * sobre la base de conocimiento local. Se descarta el uso de embeddings
 * vectoriales de forma deliberada: con un corpus de unas decenas de
 * fragmentos, una búsqueda léxica bien normalizada alcanza una precisión
 * equivalente sin arrastrar un segundo modelo al dispositivo, lo que
 * mantiene el APK y el consumo dentro de lo razonable para gama media.
 *
 * El proceso: normalizar (minúsculas, sin tildes), descartar palabras vacías,
 * puntuar cada fragmento por coincidencia de términos ponderada por su rareza
 * en el corpus, y devolver los mejores SIEMPRE QUE superen un umbral mínimo.
 * Si nada lo supera, se devuelve lista vacía y el asistente admite que no lo
 * sabe: es preferible reconocerlo a responder con un fragmento irrelevante.
 */
object BuscadorRag {

    /** Umbral mínimo de relevancia; por debajo, se considera «no lo sé». */
    private const val UMBRAL_RELEVANCIA = 0.35

    private val vacias = setOf(
        "el","la","los","las","un","una","unos","unas","de","del","al","a","ante","con",
        "en","para","por","sin","sobre","tras","y","o","u","que","qué","como","cómo",
        "cuando","cuándo","donde","dónde",
        // Interrogativos y verbos huecos: sin ellos, «cuánto cuesta un coche»
        // coincidía con la etiqueta «cada cuánto» del fragmento de periodicidad.
        "cuanto","cuánto","cuantos","cuántos","cuanta","cuánta","cuantas","cuántas",
        "cuesta","quiero","dime","sirve","pasa",
        "es","son","ser","estar","esta","este","esto",
        "me","te","se","mi","tu","su","lo","le","yo","tengo","tiene","hay","hace","muy",
        "mas","más","pero","si","sí","no","ya","porque","cual","cuál","quien","quién",
        "puedo","puede","debo","debe","hacer","tener","sobre","algo","todo","nada"
    )

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    /**
     * Longitud de la raíz usada para emparejar derivaciones de una misma
     * palabra. Sin ella, «agobiado» no encontraría el fragmento etiquetado
     * como «agobio». Cinco caracteres es un compromiso razonable en español:
     * recoge la familia léxica sin llegar a confundir palabras distintas.
     */
    private const val LONGITUD_RAIZ = 5

    /**
     * Términos que solo aparecen como parte de NOMBRES DE INSTITUCIONES y
     * carecen de significado propio dentro del dominio.
     *
     * Contribuyen a la puntuación —forman parte del texto— pero no acreditan
     * pertinencia por sí solos. Sin esta distinción, una consulta sobre la
     * «Organización Mundial del Comercio» resultaba pertinente frente a la
     * definición de burnout, porque «organización» y «mundial» figuran ambos
     * en «Organización Mundial de la Salud»: dos coincidencias de cuerpo que
     * bastaban para superar el filtro sin que hubiera relación temática
     * alguna. Nótese que se excluyen únicamente los términos vacíos de
     * contenido; «salud», «trabajo» o «psicología» sí son del dominio y no
     * figuran aquí.
     */
    private val institucionales = setOf(
        "organizacion", "mundial", "internacional", "nacional", "instituto",
        "consejo", "general", "oficial", "ministerio", "europea", "europeo",
        "espanola", "espanol", "agencia", "sociedad", "asociacion", "comision"
    )

    /**
     * Etiquetas que nombran un ACTO DE HABLA y no un tema.
     *
     * «Recomendación» describe lo que el usuario pide que se haga, no aquello
     * de lo que pregunta, y por eso casa con «recomiéndame» sea cual sea el
     * asunto: sin esta distinción, «recomiéndame una película» resultaba
     * pertinente frente a las 24 pautas del catálogo. Se le aplica el mismo
     * criterio que a institucionales: sigue puntuando —forma parte del texto
     * indexado— pero no acredita pertinencia por sí sola.
     *
     * Nótese que las otras etiquetas de la plantilla de actividades
     * («actividad», «ejercicio», «pauta») NO figuran aquí: cuando alguien
     * pregunta por el catálogo, esas palabras sí son el tema de su consulta,
     * y excluirlas dejaba «qué actividades hay» sin respuesta pese a que el
     * mensaje de respaldo del asistente invita expresamente a preguntarlo.
     *
     * Se excluye por raíz, no por igualdad, para no romper los plurales.
     */
    private val andamiaje = setOf("recomendacion")

    private fun raiz(palabra: String): String =
        if (palabra.length >= LONGITUD_RAIZ) palabra.take(LONGITUD_RAIZ) else palabra

    /** Coincidencias de un término en una lista, exactas o por raíz común. */
    private fun coincidencias(lista: List<String>, termino: String): Int =
        lista.count { it == termino || raiz(it) == raiz(termino) }

    /** Raíces de las etiquetas de andamiaje, para excluirlas por familia léxica. */
    private val raicesAndamiaje: Set<String> = andamiaje.map(::raiz).toSet()

    /**
     * Visible para BaseConocimiento, que lo usa al decidir si el título de una
     * pauta aporta un único término con contenido.
     */
    internal fun tokenizar(texto: String): List<String> =
        normalizar(texto)
            .split(Regex("[^a-z0-9ñ]+"))
            .filter { it.length > 2 && it !in vacias }

    /** Índice de frecuencia documental: en cuántos fragmentos aparece cada término. */
    private val frecuenciaDocumental: Map<String, Int> by lazy {
        val mapa = mutableMapOf<String, Int>()
        BaseConocimiento.fragmentos.forEach { f ->
            tokenizar(f.titulo + " " + f.texto + " " + f.etiquetas.joinToString(" "))
                .map(::raiz).toSet()
                .forEach { r -> mapa[r] = (mapa[r] ?: 0) + 1 }
        }
        mapa
    }

    /**
     * Devuelve los fragmentos más relevantes para la consulta, o lista vacía
     * si ninguno alcanza el umbral.
     *
     * @param maximo número máximo de fragmentos a devolver (contexto acotado:
     *        un modelo pequeño se degrada si se le pasa demasiado texto).
     */
    fun buscar(consulta: String, maximo: Int = 2): List<Fragmento> {
        val terminos = tokenizar(consulta)
        if (terminos.isEmpty()) return emptyList()
        val total = BaseConocimiento.fragmentos.size.toDouble()

        val puntuados = BaseConocimiento.fragmentos.map { f ->
            // Las etiquetas pesan el doble: son los términos con los que la
            // gente pregunta de verdad ("estoy quemado", "dormir mejor").
            val cuerpo = tokenizar(f.titulo + " " + f.texto)
            val etiquetas = tokenizar(f.etiquetas.joinToString(" "))
            var puntos = 0.0
            terminos.distinct().forEach { t ->
                val apariciones = coincidencias(cuerpo, t) + 2 * coincidencias(etiquetas, t)
                if (apariciones > 0) {
                    val df = (frecuenciaDocumental[raiz(t)] ?: 1).toDouble()
                    val idf = kotlin.math.ln(1 + total / df)     // término raro = más peso
                    puntos += kotlin.math.ln(1.0 + apariciones) * idf
                }
            }
            // Normalizado por el tamaño de la consulta, para que preguntas
            // largas no puntúen artificialmente alto.
            puntos /= kotlin.math.sqrt(terminos.distinct().size.toDouble())

            // FILTRO DE PERTINENCIA. Una puntuación alta no basta: puede
            // proceder de una coincidencia casual con un término raro pero
            // irrelevante. Caso real detectado en pruebas: «¿quién ganó el
            // mundial?» puntuaba alto contra la definición de burnout porque
            // el corpus contiene «Organización Mundial de la Salud». Se exige
            // que la coincidencia sea temática: o el término figura entre las
            // etiquetas del fragmento, que recogen cómo se pregunta por ese
            // tema, o coinciden al menos dos términos distintos del cuerpo.
            // Solo los términos con contenido propio acreditan pertinencia, y
            // solo frente a las etiquetas TEMÁTICAS: las que nombran un acto
            // de habla casan con la petición, no con el asunto preguntado.
            val conContenido = terminos.distinct().filter { it !in institucionales }
            val tematicas = etiquetas.filter { raiz(it) !in raicesAndamiaje }
            val enEtiquetas = conContenido.count { coincidencias(tematicas, it) > 0 }
            val enCuerpo = conContenido.count { coincidencias(cuerpo, it) > 0 }
            val pertinente = enEtiquetas >= 1 || enCuerpo >= 2

            Triple(f, puntos, pertinente)
        }

        return puntuados
            .filter { it.second >= UMBRAL_RELEVANCIA && it.third }
            .sortedByDescending { it.second }
            .take(maximo)
            .map { it.first }
    }
}
