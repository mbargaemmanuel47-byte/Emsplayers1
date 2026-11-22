package com.example.ai

import android.content.Context
import android.util.Log
import com.example.model.GeneratedMixResult
import com.example.model.Track
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AuraAiRepository"

class AuraAiRepository(private val appContext: Context) {

    private fun ensureServiceReady() {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            throw IllegalStateException(
                "Erreur d'accès au Studio IA : service indisponible pour le moment."
            )
        }
    }

    private val playlistSchema = Schema.obj(
        properties = mapOf(
            "playlistTitle" to Schema.string(description = "Titre court en français de la sélection"),
            "rationale" to Schema.string(description = "Explication musicale concise en français"),
            "selectedTrackIds" to Schema.array(
                items = Schema.integer(description = "Identifiant numérique d'une piste"),
                description = "Liste ordonnée d'IDs de pistes du catalogue"
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = playlistSchema
                temperature = 0.7f
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "Tu es l'assistant musical de Em Player. " +
                        "Réponds toujours en français, de manière simple, claire et concise (2 à 4 phrases max)."
                )
            }
        )
    }

    suspend fun generateCustomPlaylist(
        prompt: String,
        targetBpm: Int,
        emotionalIntensity: Int,
        catalog: List<Track>
    ): GeneratedMixResult {
        ensureServiceReady()
        if (catalog.isEmpty()) {
            throw IllegalStateException("Ajoutez d'abord des titres depuis votre stockage.")
        }
        val catalogSummary = catalog.take(60).joinToString("\n") { track ->
            val fmt = track.mimeFormat.ifBlank { "Audio" }
            "ID=${track.id} | Titre=\"${track.title}\" | Artiste=\"${track.artist}\" | Album=\"${track.album}\" | Format=$fmt | Écoutes=${track.playCount}"
        }
        val fullPrompt = """
            Crée une playlist cohérente parmi ces morceaux locaux :
            - Demande : "$prompt"
            - Rythme souhaité : $targetBpm BPM
            - Intensité : $emotionalIntensity%

            Catalogue disponible (utilise UNIQUEMENT ces IDs) :
            $catalogSummary
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(fullPrompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Erreur de réponse du Studio IA.")

            val json = JSONObject(jsonString)
            val title = json.getString("playlistTitle")
            val rationale = json.getString("rationale")
            val idsArray = json.getJSONArray("selectedTrackIds")
            val validIds = catalog.map { it.id }.toSet()
            val parsedIds = (0 until idsArray.length())
                .map { idsArray.getLong(it) }
                .filter { it in validIds }
                .distinct()

            if (parsedIds.isEmpty()) {
                throw IllegalStateException("Aucun titre correspondant trouvé dans votre bibliothèque.")
            }

            GeneratedMixResult(
                title = title,
                rationale = rationale,
                trackIds = parsedIds
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI playlist generation timed out", e)
            throw IllegalStateException("Erreur de connexion : délai d'attente dépassé.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI playlist generation failed", e)
            throw IllegalStateException("Erreur lors de la création du mix. Vérifiez votre connexion.")
        }
    }

    fun streamCopilotReply(
        userMessage: String,
        currentTrack: Track?,
        catalog: List<Track>
    ): Flow<String> = flow {
        ensureServiceReady()
        val catalogBrief = catalog.take(40).joinToString(", ") { "${it.title} (${it.artist})" }
        val activeDesc = if (currentTrack != null) {
            "\"${currentTrack.title}\" par ${currentTrack.artist} (${currentTrack.album})"
        } else {
            "Aucun morceau en lecture"
        }
        val prompt = """
            Morceau actuel : $activeDesc
            Bibliothèque : $catalogBrief
            Question : "$userMessage"
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chatModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Erreur de réponse de l'assistant.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI copilot streaming timed out", e)
            throw IllegalStateException("Erreur de connexion : délai d'attente dépassé.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI copilot streaming failed", e)
            throw IllegalStateException("Erreur de connexion à l'assistant. Réessayez plus tard.")
        }
    }
}
