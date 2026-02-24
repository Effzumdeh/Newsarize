package com.example.newsarize.domain.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaPipeAiService(private val context: Context) {

    private var llmInference: LlmInference? = null
    private var isInitialized = false

    suspend fun initialize(modelFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isInitialized) return@withContext true

                Log.d("MediaPipeAiService", "Initializing MediaPipe LlmInference")
                
                // WORKAROUND: Force OpenCL driver cache invalidation on warm restarts
                // The underlying GPU driver (especially Qualcomm Adreno) crashes when loading serialized 
                // pipeline caches across sessions. Updating file stat and clearing the local code cache prevents it.
                modelFile.setLastModified(System.currentTimeMillis())
                try {
                    context.codeCacheDir.listFiles()?.forEach { it.deleteRecursively() }
                } catch (e: Exception) { }
                
                try {
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .setMaxTokens(300)
                        .setPreferredBackend(LlmInference.Backend.GPU)
                        .build()
                    llmInference = LlmInference.createFromOptions(context, options)
                } catch (e: Throwable) {
                    Log.w("MediaPipeAiService", "GPU Init failed, retrying on CPU...", e)
                    // FALLBACK TO CPU: If GPU drivers are corrupted/locked, CPU is the only safe harbor
                    val cpuOptions = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .setMaxTokens(300)
                        .setPreferredBackend(LlmInference.Backend.CPU)
                        .build()
                    llmInference = LlmInference.createFromOptions(context, cpuOptions)
                }
                
                isInitialized = true
                true
            } catch (e: Exception) {
                Log.e("MediaPipeAiService", "Complete Init failed (incl. CPU)", e)
                throw e
            }
        }
    }

    // A very loose chunking representation, normally we would run the exact extension tokenizer to count tokens.
    // For large texts, we divide them safely.
    fun chunkText(text: String, maxLengthChars: Int = 3000): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = start + maxLengthChars
            if (end > text.length) {
                end = text.length
            } else {
                // Try to find a space to not cut mid-word
                val spaceIndex = text.lastIndexOf(" ", end)
                if (spaceIndex > start) {
                    end = spaceIndex
                }
            }
            chunks.add(text.substring(start, end).trim())
            start = end
        }
        return chunks
    }

    suspend fun summarize(text: String): String {
        return withContext(Dispatchers.IO) {
            if (!isInitialized || llmInference == null) {
                return@withContext "Error: Model not initialized"
            }
            // Using Gemma typical formatting with strict German grammar instructions
            val prompt = "<start_of_turn>user\nDu bist ein professioneller Redakteur. Fasse den folgenden Text in 3 bis 5 Sätzen ausführlich und verständlich zusammen. Nenne konkrete Namen, Orte, Fakten und Hintergründe, damit der Leser den Sachverhalt vollumfänglich versteht und kein Vorwissen durch den ursprünglichen Artikel benötigt. Vermeide Clickbait-Formulierungen. Achte auf absolut fehlerfreie Grammatik. Text: $text<end_of_turn>\n<start_of_turn>model\n"
            
            try {
                val responseText = llmInference?.generateResponse(prompt)
                responseText?.trim() ?: "Keine Antwort generiert."
            } catch (e: Exception) {
                Log.e("MediaPipeAiService", "Inference failed", e)
                "Fehler bei der Zusammenfassung: ${e.message}"
            }
        }
    }

    suspend fun categorize(text: String, tags: List<String>): String {
        return withContext(Dispatchers.IO) {
            if (!isInitialized || llmInference == null) {
                return@withContext "#Sonstiges"
            }
            
            val tagsString = tags.joinToString(", ")
            // Prevent context window crashes for extremely long texts (like Gamestar RSS descriptions)
            val safeText = if (text.length > 2500) text.substring(0, 2500) + "..." else text
            val prompt = "<start_of_turn>user\nDu bist ein strenger Klassifizierer. Lese den folgenden Text und ordne ihn EXAKT EINEM der folgenden Tags zu: [$tagsString]. Antworte NUR mit dem Namen des Tags, ohne weitere Erklärungen. Wenn keiner passt, antworte mit '#Sonstiges'.\n\nText: $safeText<end_of_turn>\n<start_of_turn>model\n"
            
            try {
                val responseText = llmInference?.generateResponse(prompt)
                val cleanedResponse = responseText?.trim() ?: "#Sonstiges"
                // Ensure the response is actually in the tags list or #Sonstiges
                if (tags.contains(cleanedResponse) || cleanedResponse == "#Sonstiges") {
                    cleanedResponse
                } else {
                    "#Sonstiges"
                }
            } catch (e: Exception) {
                Log.e("MediaPipeAiService", "Categorization failed", e)
                "#Sonstiges"
            }
        }
    }

    fun close() {
        try {
            llmInference?.close()
        } catch (e: Exception) {}
        llmInference = null
        isInitialized = false
    }
}
