package com.example.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.model.GeneratedMixResult
import com.example.model.ListeningBehaviorProfile
import com.example.model.ProactiveNotificationDraft
import com.example.model.Track
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TAG = "MusicAssistantManager"

class MusicAssistantManager(
    private val appContext: Context,
    private val groqApiKeyProvider: () -> String = { BuildConfig.GROQ_API_KEY }
) {

    companion object {
        const val GEMINI_PRIMARY_MODEL = "gemini-3.8-flash"
        const val GEMINI_LITE_MODEL = "gemini-3.5-flash-lite"
        const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        const val GROQ_PRIMARY_MODEL = "openai/gpt-oss-120b"
        const val GROQ_SECONDARY_MODEL = "llama-3.3-70b-versatile"

        const val EMMA_SYSTEM_INSTRUCTION =
            "Tu t'appelles Emma et tu es une experte en musique, l'intelligence artificielle et compagne musicale de l'application Em Player. " +
                "Tu as une personnalité chaleureuse, engageante, complice et concise (2 à 4 phrases maximum en français). " +
                "Tu n'es PAS une IA généraliste : tu es programmée et formée uniquement pour parler de musique " +
                "(morceaux de la bibliothèque locale, artistes, albums, genres, ambiances, playlists, enchaînements entre titres, habitudes d'écoute, tempo/BPM, audio, égaliseur, instruments, histoire et théorie musicale). " +
                "RÈGLE STRICTE : tu ne causes QUE musique et strictement musique. " +
                "Pour toute requête, question ou discussion qui ne porte pas sur la musique (code, mathématiques, cuisine, politique, actualités, santé, droit, météo, devoirs, finance, vie quotidienne sans lien musical, etc.) " +
                "ou si l'utilisateur tente de te faire sortir de ce cadre, refuse catégoriquement et poliment de traiter ce sujet. " +
                "Informe explicitement l'utilisateur, en variant naturellement ta formulation d'une réponse à l'autre (en d'autres mots), " +
                "que tu es Emma, que tu es uniquement programmée pour parler de musique et que tout autre sujet dépasse ton entendement et sort de tes compétences, " +
                "puis invite-le à te poser une question musicale."

        const val EMMA_PLAYLIST_INSTRUCTION =
            "Tu t'appelles Emma, l'experte musicale d'Em Player, programmée uniquement pour la musique. " +
                "Crée uniquement des playlists musicales cohérentes en français à partir des identifiants locaux fournis et du profil d'écoute."

        const val EMMA_PROACTIVE_INSTRUCTION =
            "Tu es Emma, la compagne musicale d'Em Player. Génère exactement UNE phrase de notification push " +
                "très courte (entre 30 et 105 caractères), chaleureuse, naturelle et personnalisée en français, " +
                "sans guillemets, sans hashtag et sans préfixe du type 'Notification :'."

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val playlistSchema = Schema.obj(
        properties = mapOf(
            "playlistTitle" to Schema.string(description = "Titre musical court en français de la sélection"),
            "rationale" to Schema.string(description = "Explication musicale concise signée Emma en français"),
            "selectedTrackIds" to Schema.array(
                items = Schema.integer(description = "Identifiant numérique d'une piste"),
                description = "Liste ordonnée d'IDs de pistes du catalogue"
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_PRIMARY_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = playlistSchema
                temperature = 0.7f
            },
            systemInstruction = content {
                text(EMMA_PLAYLIST_INSTRUCTION)
            }
        )
    }

    private val structuredLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_LITE_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = playlistSchema
                temperature = 0.7f
            },
            systemInstruction = content {
                text(EMMA_PLAYLIST_INSTRUCTION)
            }
        )
    }

    private val proactiveNotificationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_LITE_MODEL,
            generationConfig = generationConfig {
                temperature = 0.8f
                maxOutputTokens = 70
            },
            systemInstruction = content {
                text(EMMA_PROACTIVE_INSTRUCTION)
            }
        )
    }

    private val cachedChatModels = mutableMapOf<Pair<String, Int>, GenerativeModel>()

    internal fun buildDynamicSystemPrompt(behaviorProfile: ListeningBehaviorProfile?): String {
        val behaviorBlock = behaviorProfile?.systemPromptBlock?.trim().orEmpty()
        return if (behaviorBlock.isBlank()) {
            EMMA_SYSTEM_INSTRUCTION
        } else {
            "$EMMA_SYSTEM_INSTRUCTION\n\n$behaviorBlock"
        }
    }

    private fun getOrCreateChatModel(modelName: String, systemPrompt: String): GenerativeModel {
        val key = modelName to systemPrompt.hashCode()
        return synchronized(cachedChatModels) {
            cachedChatModels.getOrPut(key) {
                Firebase.ai.generativeModel(
                    modelName = modelName,
                    systemInstruction = content {
                        text(systemPrompt)
                    }
                )
            }
        }
    }

    internal fun isFirebaseReady(): Boolean {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            runCatching { FirebaseApp.initializeApp(appContext) }
                .onFailure { Log.w(TAG, "FirebaseApp init skipped: ${it.message}") }
        }
        return FirebaseApp.getApps(appContext).isNotEmpty()
    }

    internal fun hasGroqKey(): Boolean {
        val key = groqApiKeyProvider().trim()
        return key.isNotEmpty() && key != "GROQ_API_KEY_DEFAULT_VALUE"
    }

    internal fun buildMissingConfigMessage(): String {
        val firebaseOk = isFirebaseReady()
        val groqOk = hasGroqKey()
        return when {
            !firebaseOk && !groqOk ->
                "Service IA non configuré dans cet APK : ajoutez GROQ_API_KEY dans les secrets (AI Studio / GitHub Actions) et vérifiez google-services.json."
            !groqOk ->
                "Accès Firebase refusé sur ce build Debug et clé de secours GROQ_API_KEY absente."
            else ->
                "Emma est momentanément indisponible."
        }
    }

    private fun isNetworkUnavailable(e: Throwable): Boolean {
        return e is UnknownHostException ||
            e.cause is UnknownHostException ||
            (e.message.orEmpty().lowercase().contains("unable to resolve host"))
    }

    private fun mapUserError(e: Exception, fallback: String): String {
        val raw = "${e.message.orEmpty()} ${e.cause?.message.orEmpty()}".lowercase()
        return when {
            e is TimeoutCancellationException || raw.contains("timed out") || raw.contains("timeout") ->
                "Délai d'attente dépassé. Vérifiez votre connexion Internet et réessayez."
            isNetworkUnavailable(e) || e is IOException || raw.contains("network") ->
                "Erreur réseau : vérifiez votre connexion Internet pour joindre Emma."
            raw.contains("403") || raw.contains("app check") || raw.contains("attestation") || raw.contains("permission") || raw.contains("blocked") ->
                "Accès au service IA refusé. Réessayez ou configurez la clé de secours."
            raw.contains("429") || raw.contains("quota") || raw.contains("rate") ->
                "Emma est très sollicitée pour le moment. Réessayez plus tard."
            else -> {
                val safeHint = e.message.orEmpty()
                    .replace(Regex("firebase|google|vertex|gsk_[A-Za-z0-9_]+", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(75)
                if (safeHint.isNotBlank()) "$fallback ($safeHint)" else fallback
            }
        }
    }

    suspend fun generatePlaylist(
        prompt: String,
        targetBpm: Int,
        emotionalIntensity: Int,
        catalog: List<Track>,
        behaviorProfile: ListeningBehaviorProfile? = null
    ): GeneratedMixResult {
        if (catalog.isEmpty()) {
            throw IllegalStateException("Ajoutez d'abord des titres dans Biblio.")
        }

        val catalogSummary = catalog.take(60).joinToString("\n") { track ->
            val fmt = track.mimeFormat.ifBlank { "Audio" }
            "ID=${track.id} | Titre=\"${track.title}\" | Artiste=\"${track.artist}\" | Album=\"${track.album}\" | Format=$fmt | Écoutes=${track.playCount}${if (track.isLiked) " | Favori" else ""}"
        }
        val habitSection = behaviorProfile?.systemPromptBlock?.takeIf { it.isNotBlank() }?.let {
            "\n\nContexte d'écoute de l'utilisateur :\n$it"
        }.orEmpty()
        val fullPrompt = """
            Emma, crée une playlist musicale cohérente parmi ces morceaux locaux :
            - Demande musicale : "$prompt"
            - Rythme souhaité : $targetBpm BPM
            - Intensité : $emotionalIntensity%$habitSection

            Catalogue disponible (utilise UNIQUEMENT ces IDs) :
            $catalogSummary
        """.trimIndent()

        var primaryFailure: Exception? = null
        if (isFirebaseReady()) {
            val playlistModels = listOf(structuredModel, structuredLiteModel)
            for (model in playlistModels) {
                try {
                    val jsonString = withTimeout(45_000L) {
                        model.generateContent(fullPrompt).text
                    } ?: throw IllegalStateException("Réponse vide d'Emma.")
                    return parsePlaylistJson(jsonString, catalog)
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Primary Firebase playlist generation timed out, checking fallback", e)
                    primaryFailure = e
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Primary Firebase playlist generation failed, checking fallback", e)
                    primaryFailure = e
                    if (isNetworkUnavailable(e)) break
                    val lower = e.message.orEmpty().lowercase()
                    if (lower.contains("403") || lower.contains("app check") || lower.contains("attestation")) break
                }
            }
        }

        if (hasGroqKey()) {
            return try {
                generatePlaylistWithGroq(fullPrompt, catalog)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Groq playlist fallback failed", e)
                throw IllegalStateException(mapUserError(e, "Erreur lors de la création du mix."))
            }
        }

        val err = primaryFailure
        if (err != null) {
            throw IllegalStateException(mapUserError(err, "Erreur lors de la création du mix."))
        }
        throw IllegalStateException(buildMissingConfigMessage())
    }

    fun streamChatReply(
        userMessage: String,
        currentTrack: Track?,
        catalog: List<Track>,
        recentHistory: List<ChatMessage> = emptyList(),
        behaviorProfile: ListeningBehaviorProfile? = null
    ): Flow<String> = flow {
        val contextPrompt = buildChatContextHeader(currentTrack, catalog, behaviorProfile)
        val systemPrompt = buildDynamicSystemPrompt(behaviorProfile)
        val filteredHistory = recentHistory
            .filter { !it.isStreaming && it.text.isNotBlank() && it.id != "welcome" && it.id != "reset" }
            .takeLast(6)

        var primaryFailure: Exception? = null
        if (isFirebaseReady()) {
            val geminiPrompt = buildString {
                appendLine(contextPrompt)
                if (filteredHistory.isNotEmpty()) {
                    appendLine("Échanges récents :")
                    filteredHistory.forEach { msg ->
                        val speaker = if (msg.isUser) "Utilisateur" else "Emma"
                        appendLine("$speaker : ${msg.text}")
                    }
                }
                appendLine(
                    "Rappel strict pour Emma : Si la question ne porte pas sur la musique ou l'audio musical " +
                        "(hors salutations simples pour lancer la discussion), refuse poliment et dis clairement en d'autres mots " +
                        "que tu es Emma, uniquement formée pour parler de musique, et que tout autre sujet dépasse ton entendement."
                )
                append("Message utilisateur : \"$userMessage\"")
            }

            val geminiModels = listOf(
                getOrCreateChatModel(GEMINI_PRIMARY_MODEL, systemPrompt),
                getOrCreateChatModel(GEMINI_LITE_MODEL, systemPrompt)
            )
            for (model in geminiModels) {
                val accumulated = StringBuilder()
                try {
                    withTimeout(45_000L) {
                        model.generateContentStream(geminiPrompt).collect { chunk ->
                            val piece = chunk.text.orEmpty()
                            if (piece.isNotEmpty()) {
                                accumulated.append(piece)
                                emit(accumulated.toString())
                            }
                        }
                    }
                    if (accumulated.isNotBlank()) {
                        return@flow
                    }
                    primaryFailure = IllegalStateException("Réponse vide d'Emma.")
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Primary AI chat timed out, checking fallback", e)
                    primaryFailure = e
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Primary AI chat failed (${e.javaClass.simpleName}: ${e.message}), checking fallback", e)
                    primaryFailure = e
                    if (isNetworkUnavailable(e)) break
                    val lower = e.message.orEmpty().lowercase()
                    if (lower.contains("403") || lower.contains("app check") || lower.contains("attestation")) break
                }
            }
        }

        if (hasGroqKey()) {
            try {
                streamChatWithGroq(
                    systemPrompt = systemPrompt,
                    contextHeader = contextPrompt,
                    history = filteredHistory,
                    userMessage = userMessage,
                    onPartial = { partial -> emit(partial) }
                )
                return@flow
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Groq chat fallback failed", e)
                throw IllegalStateException(mapUserError(e, "Erreur de connexion avec Emma. Réessayez."))
            }
        }

        val err = primaryFailure
        if (err != null) {
            throw IllegalStateException(mapUserError(err, "Erreur de connexion avec Emma. Réessayez."))
        }
        throw IllegalStateException(buildMissingConfigMessage())
    }.flowOn(Dispatchers.IO)

    suspend fun generateProactiveNotificationCopy(profile: ListeningBehaviorProfile): ProactiveNotificationDraft? {
        val windowHabit = profile.dominantHabitForWindow
        val targetSlot = windowHabit?.slot ?: profile.currentSlot
        val suggestedTrackId = windowHabit?.topTrackId
            ?: profile.topTracks.firstOrNull()?.id
            ?: profile.likedTracks.firstOrNull()?.id
        val suggestedPlaylist = profile.playlists.firstOrNull()

        val habitCue = buildString {
            appendLine("Créneau ciblé : ${targetSlot.label} (${targetSlot.hoursLabel})")
            if (windowHabit?.topTrackTitle != null) {
                appendLine("Titre habituel sur ce créneau : \"${windowHabit.topTrackTitle}\" (${windowHabit.topTrackArtist.orEmpty()})")
            }
            if (suggestedPlaylist != null) {
                appendLine("Playlist disponible : \"${suggestedPlaylist.title}\" (${suggestedPlaylist.trackIds.size} titres)")
            }
            profile.topTransitions.firstOrNull()?.let { seq ->
                appendLine("Enchaînement favori : \"${seq.fromTitle}\" -> \"${seq.toTitle}\" (${seq.count} fois)")
            }
            if (profile.topTracks.isNotEmpty()) {
                appendLine("Top titres : ${profile.topTracks.take(3).joinToString(", ") { "\"${it.title}\"" }}")
            }
            if (profile.likedTracks.isNotEmpty()) {
                appendLine("Favoris : ${profile.likedTracks.take(3).joinToString(", ") { "\"${it.title}\"" }}")
            }
        }.trim()

        val prompt = """
            Rédige un court message de notification musicale personnalisé pour l'utilisateur d'Em Player.
            Indices comportementaux :
            $habitCue

            Exemples de ton attendu :
            - C'est l'heure de ta playlist chill du soir ?
            - Envie de lancer [Titre] comme d'habitude ?
            Contraintes : 1 phrase unique, max 105 caractères, français naturel, pas de guillemets.
        """.trimIndent()

        if (isFirebaseReady()) {
            try {
                val raw = withTimeout(30_000L) {
                    proactiveNotificationModel.generateContent(prompt).text
                }
                val cleaned = sanitizeNotificationText(raw)
                if (cleaned != null) {
                    return ProactiveNotificationDraft(
                        message = cleaned,
                        suggestedTrackId = suggestedTrackId,
                        suggestedPlaylistId = suggestedPlaylist?.id,
                        slot = targetSlot
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Firebase proactive copy generation failed, checking Groq", e)
            }
        }

        if (hasGroqKey()) {
            try {
                val raw = generateSingleLineWithGroq(EMMA_PROACTIVE_INSTRUCTION, prompt)
                val cleaned = sanitizeNotificationText(raw)
                if (cleaned != null) {
                    return ProactiveNotificationDraft(
                        message = cleaned,
                        suggestedTrackId = suggestedTrackId,
                        suggestedPlaylistId = suggestedPlaylist?.id,
                        slot = targetSlot
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Groq proactive copy generation failed", e)
            }
        }

        return null
    }

    internal fun sanitizeNotificationText(raw: String?): String? {
        val cleaned = raw.orEmpty()
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("«")
            .removeSurrounding("»")
            .trim()
        if (cleaned.length < 10) return null
        return if (cleaned.length <= 115) cleaned else cleaned.take(112).trimEnd() + "…"
    }

    private fun buildChatContextHeader(
        currentTrack: Track?,
        catalog: List<Track>,
        behaviorProfile: ListeningBehaviorProfile?
    ): String {
        val catalogBrief = if (catalog.isEmpty()) {
            "Aucun morceau scanné pour l'instant"
        } else {
            catalog.take(40).joinToString(", ") { "${it.title} (${it.artist})" }
        }
        val activeDesc = if (currentTrack != null) {
            "\"${currentTrack.title}\" par ${currentTrack.artist} (${currentTrack.album})"
        } else {
            "Aucun morceau en lecture"
        }
        val slotLabel = behaviorProfile?.currentSlot?.let { "${it.label} (${it.hoursLabel})" } ?: ""
        return buildString {
            appendLine("Morceau en cours : $activeDesc")
            if (slotLabel.isNotBlank()) {
                appendLine("Créneau horaire actuel : $slotLabel")
            }
            append("Bibliothèque locale : $catalogBrief")
        }
    }

    private suspend fun generatePlaylistWithGroq(
        fullPrompt: String,
        catalog: List<Track>
    ): GeneratedMixResult = withContext(Dispatchers.IO) {
        val modelsToTry = listOf(GROQ_PRIMARY_MODEL, GROQ_SECONDARY_MODEL)
        var lastException: Exception? = null
        val jsonSchemaGuide =
            "Réponds EXCLUSIVEMENT en JSON valide avec exactement les clés : " +
                "\"playlistTitle\" (String), \"rationale\" (String), \"selectedTrackIds\" (Array of Long IDs du catalogue)."

        for (modelId in modelsToTry) {
            try {
                val payload = JSONObject().apply {
                    put("model", modelId)
                    put("temperature", 0.6)
                    put("max_completion_tokens", 1400)
                    put("response_format", JSONObject().put("type", "json_object"))
                    put("messages", JSONArray().apply {
                        put(JSONObject().put("role", "system").put("content", "$EMMA_PLAYLIST_INSTRUCTION\n$jsonSchemaGuide"))
                        put(JSONObject().put("role", "user").put("content", fullPrompt))
                    })
                }
                val request = Request.Builder()
                    .url(GROQ_ENDPOINT)
                    .addHeader("Authorization", "Bearer ${groqApiKeyProvider().trim()}")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val errMsg = extractGroqErrorMessage(rawBody, response.code)
                        if (isModelFallbackEligible(response.code, errMsg) && modelId != modelsToTry.last()) {
                            Log.w(TAG, "Groq model $modelId unavailable ($errMsg), trying secondary model")
                            lastException = IOException(errMsg)
                            return@use
                        }
                        throw IOException(errMsg)
                    }

                    val root = JSONObject(rawBody)
                    val content = root.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .optString("content", "")
                    if (content.isBlank()) {
                        throw IOException("Réponse vide du modèle $modelId")
                    }
                    return@withContext parsePlaylistJson(content, catalog)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Groq playlist attempt failed for $modelId: ${e.message}")
                lastException = e
                if (modelId == modelsToTry.last() || isNetworkUnavailable(e)) {
                    throw e
                }
            }
        }

        throw (lastException ?: IllegalStateException("Erreur lors de la création du mix."))
    }

    private suspend fun generateSingleLineWithGroq(systemInstruction: String, userPrompt: String): String = withContext(Dispatchers.IO) {
        val modelsToTry = listOf(GROQ_SECONDARY_MODEL, GROQ_PRIMARY_MODEL)
        var lastException: Exception? = null
        for (modelId in modelsToTry) {
            try {
                val payload = JSONObject().apply {
                    put("model", modelId)
                    put("temperature", 0.75)
                    put("max_completion_tokens", 120)
                    put("messages", JSONArray().apply {
                        put(JSONObject().put("role", "system").put("content", systemInstruction))
                        put(JSONObject().put("role", "user").put("content", userPrompt))
                    })
                }
                val request = Request.Builder()
                    .url(GROQ_ENDPOINT)
                    .addHeader("Authorization", "Bearer ${groqApiKeyProvider().trim()}")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val errMsg = extractGroqErrorMessage(rawBody, response.code)
                        if (isModelFallbackEligible(response.code, errMsg) && modelId != modelsToTry.last()) {
                            lastException = IOException(errMsg)
                            return@use
                        }
                        throw IOException(errMsg)
                    }
                    val root = JSONObject(rawBody)
                    val text = root.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .optString("content", "")
                    if (text.isBlank()) {
                        throw IOException("Réponse vide du modèle $modelId")
                    }
                    return@withContext text
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (modelId == modelsToTry.last() || isNetworkUnavailable(e)) {
                    throw e
                }
            }
        }
        throw (lastException ?: IOException("Réponse vide d'Emma."))
    }

    private suspend fun streamChatWithGroq(
        systemPrompt: String,
        contextHeader: String,
        history: List<ChatMessage>,
        userMessage: String,
        onPartial: suspend (String) -> Unit
    ) {
        val modelsToTry = listOf(GROQ_PRIMARY_MODEL, GROQ_SECONDARY_MODEL)
        var lastException: Exception? = null

        val messagesArray = JSONArray().apply {
            put(
                JSONObject()
                    .put("role", "system")
                    .put("content", "$systemPrompt\n\nContexte audio immédiat :\n$contextHeader")
            )
            history.forEach { msg ->
                put(
                    JSONObject()
                        .put("role", if (msg.isUser) "user" else "assistant")
                        .put("content", msg.text)
                )
            }
            put(JSONObject().put("role", "user").put("content", userMessage))
        }

        for (modelId in modelsToTry) {
            val accumulated = StringBuilder()
            try {
                val payload = JSONObject().apply {
                    put("model", modelId)
                    put("temperature", 0.65)
                    put("max_completion_tokens", 1400)
                    put("stream", true)
                    put("messages", messagesArray)
                }
                val request = Request.Builder()
                    .url(GROQ_ENDPOINT)
                    .addHeader("Authorization", "Bearer ${groqApiKeyProvider().trim()}")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val call = httpClient.newCall(request)
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val rawBody = response.body?.string().orEmpty()
                        val errMsg = extractGroqErrorMessage(rawBody, response.code)
                        if (isModelFallbackEligible(response.code, errMsg) && modelId != modelsToTry.last()) {
                            Log.w(TAG, "Groq stream model $modelId unavailable ($errMsg), trying secondary model")
                            lastException = IOException(errMsg)
                            return@use
                        }
                        throw IOException(errMsg)
                    }

                    val source = response.body?.source() ?: throw IOException("Flux de réponse vide.")
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        if (data.isEmpty()) continue

                        val chunkJson = runCatching { JSONObject(data) }.getOrNull() ?: continue
                        val choices = chunkJson.optJSONArray("choices") ?: continue
                        if (choices.length() == 0) continue
                        val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                        val piece = if (delta.isNull("content")) "" else delta.optString("content", "")
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            onPartial(accumulated.toString())
                        }
                    }

                    if (accumulated.isBlank()) {
                        throw IOException("Réponse vide du modèle $modelId")
                    }
                    return
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Groq stream failed for $modelId: ${e.message}")
                lastException = e
                if (modelId == modelsToTry.last() || isNetworkUnavailable(e)) {
                    throw e
                }
            }
        }

        throw (lastException ?: IllegalStateException("Erreur de connexion avec Emma. Réessayez."))
    }

    internal fun parsePlaylistJson(rawText: String, catalog: List<Track>): GeneratedMixResult {
        val trimmed = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(trimmed)
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

        return GeneratedMixResult(
            title = title,
            rationale = rationale,
            trackIds = parsedIds
        )
    }

    private fun extractGroqErrorMessage(rawBody: String, httpCode: Int): String {
        val apiMsg = runCatching {
            JSONObject(rawBody).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        return if (apiMsg.isNotBlank()) "HTTP $httpCode: $apiMsg" else "HTTP $httpCode"
    }

    private fun isModelFallbackEligible(httpCode: Int, message: String): Boolean {
        if (httpCode == 401) return false
        val lower = message.lowercase()
        return httpCode in listOf(400, 404, 429, 500, 502, 503) ||
            lower.contains("model") ||
            lower.contains("decommissioned") ||
            lower.contains("rate") ||
            lower.contains("overload")
    }
}
