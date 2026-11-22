package com.example.ai

import android.content.Context
import android.util.Log
import com.example.model.GeneratedMixResult
import com.example.model.LyricLine
import com.example.model.SynthProfile
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

    private fun ensureFirebaseReady() {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            throw IllegalStateException(
                "Service IA Firebase non configuré dans cette compilation. Le lecteur local reste 100 % fonctionnel."
            )
        }
    }

    private val playlistSchema = Schema.obj(
        properties = mapOf(
            "playlistTitle" to Schema.string(description = "Titre élégant en français de la playlist générée"),
            "rationale" to Schema.string(description = "Explication musicale et harmonique concise en français"),
            "selectedTrackIds" to Schema.array(
                items = Schema.integer(description = "Identifiant numérique d'une piste du catalogue"),
                description = "Liste ordonnée de 3 à 5 IDs de pistes du catalogue"
            )
        )
    )

    private val remixSchema = Schema.obj(
        properties = mapOf(
            "remixTitle" to Schema.string(description = "Titre créatif du remix en français"),
            "genre" to Schema.string(description = "Genre musical du remix"),
            "mood" to Schema.enumeration(
                values = listOf("focus", "zouk", "studio", "chill"),
                description = "Catégorie d'ambiance"
            ),
            "bpm" to Schema.integer(description = "Tempo entre 80 et 145 BPM"),
            "musicalKey" to Schema.string(description = "Tonalité musicale (ex: Fm, Cm, Am, Dm)"),
            "energy" to Schema.integer(description = "Niveau d'énergie entre 40 et 99"),
            "waveType" to Schema.enumeration(
                values = listOf("sine", "triangle", "sawtooth", "square"),
                description = "Forme d'onde du synthétiseur 48kHz"
            ),
            "scaleHz" to Schema.array(
                items = Schema.double(description = "Fréquence mélodique en Hz (entre 160.0 et 880.0)"),
                description = "4 à 6 fréquences harmoniques de la gamme"
            ),
            "bassHz" to Schema.array(
                items = Schema.double(description = "Fréquence de basse en Hz (entre 45.0 et 130.0)"),
                description = "3 à 4 fréquences de ligne de basse"
            ),
            "rhythmPattern" to Schema.array(
                items = Schema.double(description = "Vélocité rythmique entre 0.0 et 1.0"),
                description = "Motif rythmique à 8 pas"
            ),
            "aiInsight" to Schema.string(description = "Analyse musicale de l'arrangement généré"),
            "lyrics" to Schema.array(
                items = Schema.string(description = "Ligne lyrique ou indication harmonique en français"),
                description = "4 lignes lyriques synchronisées"
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

    private val remixModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = remixSchema
                temperature = 0.85f
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "Tu es Aura, le co-pilote DJ et ingénieur du son IA de l'application Ems Player. " +
                        "Réponds toujours en français, de manière élégante, précise et concise (3 à 5 phrases max). " +
                        "Tu analyses le tempo (BPM), la tonalité, la chaleur harmonique et proposes des transitions fluides."
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
        ensureFirebaseReady()
        if (catalog.isEmpty()) {
            throw IllegalStateException("Le catalogue audio est vide.")
        }
        val catalogSummary = catalog.joinToString("\n") { track ->
            val bpmText = if (track.bpm > 0) "${track.bpm} BPM" else "Audio local"
            "ID=${track.id} | Titre=\"${track.title}\" | Artiste=\"${track.artist}\" | Genre=\"${track.genre}\" | Tempo=$bpmText | Tonalité=${track.musicalKey} | Énergie=${track.energy}%"
        }
        val fullPrompt = """
            Crée une playlist harmonique cohérente pour la demande utilisateur suivante :
            - Prompt : "$prompt"
            - Tempo cible : $targetBpm BPM
            - Intensité émotionnelle souhaitée : $emotionalIntensity%

            Catalogue disponible (utilise UNIQUEMENT ces IDs existants) :
            $catalogSummary
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(fullPrompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Le modèle IA a renvoyé une réponse vide.")

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
                throw IllegalStateException("Le modèle IA n'a sélectionné aucune piste valide du catalogue.")
            }

            GeneratedMixResult(
                title = title,
                rationale = rationale,
                trackIds = parsedIds
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI playlist generation timed out", e)
            throw IllegalStateException("La génération IA a expiré après 60 secondes. Veuillez réessayer.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI playlist generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Erreur lors de la génération de la playlist IA.")
        }
    }

    suspend fun generateTrackRemix(baseTrack: Track): Track {
        ensureFirebaseReady()
        val bpmHint = if (baseTrack.bpm > 0) baseTrack.bpm else 104
        val prompt = """
            Compose un nouvel arrangement synthétique 48kHz original inspiré de la piste suivante :
            - Titre d'origine : "${baseTrack.title}" par ${baseTrack.artist}
            - Genre : ${baseTrack.genre}
            - Tempo de référence : $bpmHint BPM
            - Tonalité de référence : ${baseTrack.musicalKey}
            - Ambiance : ${baseTrack.mood}

            Génère des fréquences harmoniques réalistes (Hz), un motif rythmique à 8 pas, et 4 lignes poétiques originales en français.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                remixModel.generateContent(prompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Le modèle IA n'a renvoyé aucun arrangement.")

            val json = JSONObject(jsonString)
            val remixTitle = json.getString("remixTitle")
            val genre = json.optString("genre", baseTrack.genre)
            val mood = json.optString("mood", baseTrack.mood)
            val bpm = json.optInt("bpm", (bpmHint + 4).coerceIn(80, 145)).coerceIn(70, 160)
            val musicalKey = json.optString("musicalKey", baseTrack.musicalKey)
            val energy = json.optInt("energy", 82).coerceIn(35, 99)
            val waveType = json.optString("waveType", "triangle")

            val scaleArr = json.optJSONArray("scaleHz")
            val scaleHz = if (scaleArr != null && scaleArr.length() >= 3) {
                (0 until scaleArr.length()).map { scaleArr.getDouble(it).coerceIn(130.0, 950.0) }
            } else {
                baseTrack.synthProfile.scaleHz
            }

            val bassArr = json.optJSONArray("bassHz")
            val bassHz = if (bassArr != null && bassArr.length() >= 2) {
                (0 until bassArr.length()).map { bassArr.getDouble(it).coerceIn(40.0, 150.0) }
            } else {
                baseTrack.synthProfile.bassHz
            }

            val rhythmArr = json.optJSONArray("rhythmPattern")
            val rhythmPattern = if (rhythmArr != null && rhythmArr.length() >= 4) {
                (0 until rhythmArr.length()).map { rhythmArr.getDouble(it).toFloat().coerceIn(0f, 1f) }
            } else {
                baseTrack.synthProfile.rhythmPattern
            }

            val aiInsight = json.optString("aiInsight", "Arrangement synthétique généré par Gemini 3.8 Flash.")
            val lyricsArr = json.optJSONArray("lyrics")
            val lyrics = if (lyricsArr != null && lyricsArr.length() > 0) {
                (0 until lyricsArr.length()).map { idx ->
                    LyricLine(timeSec = idx * 24, text = lyricsArr.getString(idx))
                }
            } else {
                emptyList()
            }

            Track(
                id = System.currentTimeMillis(),
                title = remixTitle,
                artist = "Ems Player IA • Gemini",
                album = "Studio Pro Sessions",
                genre = genre,
                mood = mood,
                bpm = bpm,
                musicalKey = musicalKey,
                energy = energy,
                durationSec = 195,
                isLiked = true,
                primaryColorHex = baseTrack.primaryColorHex,
                secondaryColorHex = baseTrack.secondaryColorHex,
                tertiaryColorHex = baseTrack.tertiaryColorHex,
                synthProfile = SynthProfile(
                    scaleHz = scaleHz,
                    bassHz = bassHz,
                    waveType = waveType,
                    rhythmPattern = rhythmPattern,
                    groove = (energy + 6).coerceAtMost(99),
                    warmth = 90,
                    clarity = 92
                ),
                aiInsight = aiInsight,
                lyrics = lyrics,
                localUri = null,
                isAiGenerated = true
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI remix generation timed out", e)
            throw IllegalStateException("La génération du remix IA a expiré après 60 secondes.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI remix generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Échec de la génération du remix IA.")
        }
    }

    fun streamCopilotReply(
        userMessage: String,
        currentTrack: Track,
        catalog: List<Track>
    ): Flow<String> = flow {
        ensureFirebaseReady()
        val catalogBrief = catalog.joinToString(", ") {
            val tempo = if (it.bpm > 0) "${it.bpm} BPM" else "Audio local"
            "${it.title} ($tempo, ${it.musicalKey})"
        }
        val activeBpm = if (currentTrack.bpm > 0) "${currentTrack.bpm} BPM" else "Fichier local"
        val prompt = """
            Piste actuellement en lecture : "${currentTrack.title}" par ${currentTrack.artist}
            Genre : ${currentTrack.genre} | Tempo : $activeBpm | Tonalité : ${currentTrack.musicalKey} | Énergie : ${currentTrack.energy}%
            Catalogue disponible : $catalogBrief

            Question de l'utilisateur : "$userMessage"
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
                throw IllegalStateException("Le co-pilote IA n'a retourné aucune réponse.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI copilot streaming timed out", e)
            throw IllegalStateException("Délai dépassé (60s) lors de la réponse du co-pilote IA.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI copilot streaming failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Impossible de joindre le co-pilote IA.")
        }
    }
}
