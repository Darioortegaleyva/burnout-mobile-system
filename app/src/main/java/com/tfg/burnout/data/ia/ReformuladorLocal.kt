package com.tfg.burnout.data.ia

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.tfg.burnout.domain.engine.ValidadorSalida

/**
 * REFORMULADOR LOCAL — «el motor decide, el modelo redacta» (§2.3.6).
 *
 * Envuelve la API LLM Inference de Google AI Edge (MediaPipe) para ejecutar
 * un modelo pequeño (Gemma 3 1B int4, ~529 MB) POR COMPLETO en el
 * dispositivo. Su única función es reescribir con otras palabras un mensaje
 * que el motor de reglas ya ha seleccionado y validado; jamás decide qué
 * decir, jamás toca los ítems del cuestionario y jamás interviene en el
 * flujo de derivación o crisis. Si el modelo no está importado, falla, o su
 * salida no supera el ValidadorSalida, se muestra la plantilla original.
 */
class ReformuladorLocal(private val context: Context) {

    private companion object { const val TAG = "ReformuladorLocal" }

    @Volatile private var motor: LlmInference? = null

    fun disponible(): Boolean = AlmacenModelo.disponible(context)

    private fun obtenerMotor(): LlmInference? {
        if (!disponible()) return null
        motor?.let { return it }
        synchronized(this) {
            motor?.let { return it }
            return runCatching {
                val opciones = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(AlmacenModelo.ruta(context))
                    .setMaxTokens(96)
                    .build()
                LlmInference.createFromOptions(context, opciones)
            }.onFailure {
                // El fallo sigue siendo silencioso para el usuario —se usa el
                // texto documental íntegro, que siempre es seguro—, pero deja
                // rastro: sin él, un modelo que no carga es indistinguible en
                // pantalla de uno que carga y redacta.
                Log.w(TAG, "No se pudo crear el motor de inferencia local", it)
            }.getOrNull()?.also { motor = it }
        }
    }

    /**
     * Devuelve una reformulación válida del mensaje, o null si no procede
     * (sin modelo, error de inferencia o salida no válida) — en cuyo caso el
     * llamador usa la plantilla original.
     */
    fun reformular(original: String): String? {
        val llm = obtenerMotor() ?: return null
        val prompt =
            "Reescribe el siguiente mensaje de una app de bienestar con otras " +
            "palabras, en español, con el mismo significado y el mismo tono " +
            "cercano. Una sola frase o dos como máximo. No añadas consejos " +
            "nuevos, ni términos médicos, ni alarmismo. Responde solo con el " +
            "mensaje reescrito.\n\nMensaje: \"" + original + "\""
        val salida = runCatching { llm.generateResponse(prompt) }.getOrNull()
            ?.trim()?.trim('"')
        return if (ValidadorSalida.validar(original, salida)) salida else null
    }

    /**
     * Genera texto a partir de un prompt completo (usado por el RAG del BOT B:
     * el prompt ya incorpora los fragmentos documentales recuperados).
     * Devuelve null si no hay modelo o la inferencia falla.
     */
    fun generarConPrompt(prompt: String): String? {
        val llm = obtenerMotor() ?: return null
        return runCatching { llm.generateResponse(prompt) }.getOrNull()?.trim()?.trim('"')
    }

    fun cerrar() { runCatching { motor?.close() }; motor = null }
}
