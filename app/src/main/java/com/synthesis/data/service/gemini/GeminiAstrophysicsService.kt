package com.synthesis.data.service.gemini

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.synthesis.BuildConfig
import com.synthesis.domain.model.Anomaly
import com.synthesis.domain.model.Hypothesis
import com.synthesis.domain.model.HypothesisStatus
import com.synthesis.domain.model.Observation
import com.synthesis.domain.model.xilon.XilonDetectionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "gemini_prefs")

@Singleton
class GeminiAstrophysicsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val geminiKeyPref = stringPreferencesKey("gemini_user_api_key")

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeModel = "gemini-3.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:generateContent"

    suspend fun getEffectiveApiKey(): String {
        val userSavedKey = context.dataStore.data.map { it[geminiKeyPref] ?: "" }.first().trim()
        if (userSavedKey.isNotBlank()) return userSavedKey

        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun saveUserApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[geminiKeyPref] = key.trim()
        }
    }

    suspend fun isAiKeyConfigured(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }

    /**
     * Executes real Gemini AI Astrobiology & Vetting analysis for a detected candidate.
     */
    suspend fun analyzeCandidateWithGemini(result: XilonDetectionResult): GeminiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()

        if (apiKey.isBlank()) {
            return@withContext generateLocalHeuristicFallback(result)
        }

        val prompt = buildCandidatePrompt(result)
        val systemInstructionText = """
            Ești XILON-AI, un astrofizician cercetător de elită specializat în exoplanetologie, fotometrie stelară (Kepler/TESS/PLATO) și habitabilitate planetară.
            Analizează datele empirice fotometrice primite și oferă un raport științific de nivel academic în limba română (cu termeni astrofizici recunoscuți internațional).
            Structurează răspunsul clar în 4 secțiuni:
            1. NATURA ASTROFIZICĂ & VALIDAREA SEMNALULUI (Planetă reală vs binară eclipsantă/fals pozitiv)
            2. COMPOZIȚIE INTERNĂ & TIP PLANETAR (Densitate estimată, atmosferă primară/secundară)
            3. DETECTABILITATE SPECTROSCOPICĂ JWST (Benzi potențiale H2O, CH4, CO2)
            4. VERDICT ȘTIINȚIFIC FINAL & RECOMANDĂRI DE URMARE (Observații de viteză radială, spectroscopie)
        """.trimIndent()

        try {
            val responseText = callGeminiApi(apiKey, systemInstructionText, prompt)
            GeminiAnalysisResult(
                isLiveAiGenerated = true,
                modelName = activeModel,
                analysisDossier = responseText,
                confidenceScorePercent = calculateAiConfidence(result)
            )
        } catch (e: Exception) {
            generateLocalHeuristicFallback(result, errorNotice = "Eroare conexiune Gemini API (${e.localizedMessage}). S-a utilizat sinteza euristică locală.")
        }
    }

    /**
     * Multi-turn chat with the Gemini AI Astrophysicist regarding the target star and candidate.
     */
    suspend fun askAstrophysicist(
        contextSummary: String,
        userQuestion: String,
        chatHistory: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext "⚠️ Cheia Gemini API nu este configurată încă. Te rugăm să adaugi cheia ta Gemini (gratuită din Google AI Studio) în meniul de setări AI de mai sus pentru a discuta în timp real cu asistentul astrofizic neuronal."
        }

        val systemPrompt = """
            Ești XILON AI, asistentul astrofizician inteligent din suita SYNTHESIS.
            Răspunde întrebărilor utilizatorului cu precizie științifică, rigurozitate astrofizică și claritate pedagogică.
            Folosește ecuații sau legi fizice dacă este relevant (Kepler, Stefan-Boltzmann, Roche, Schwarzschild).
            Răspunde în limba română.
            Contextul astronomic curent este:
            $contextSummary
        """.trimIndent()

        val fullPrompt = buildString {
            if (chatHistory.isNotEmpty()) {
                appendLine("Istoric discuție:")
                chatHistory.forEach { (sender, text) ->
                    appendLine("$sender: $text")
                }
                appendLine()
            }
            appendLine("Întrebare utilizator: $userQuestion")
        }

        try {
            callGeminiApi(apiKey, systemPrompt, fullPrompt)
        } catch (e: Exception) {
            "Eroare comunicare cu Gemini AI: ${e.message}"
        }
    }

    /**
     * Synthesizes real scientific hypotheses for Investigation mode using Gemini.
     */
    suspend fun generateScientificHypotheses(
        query: String,
        observations: List<Observation>,
        anomalies: List<Anomaly>
    ): List<Hypothesis> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext emptyList()
        }

        val prompt = """
            Generează 3 ipoteze științifice fundamentate pentru următoarea investigație astrofizică:
            Întrebare: "$query"
            Observații disponibile (${observations.size}): ${observations.take(5).joinToString { "${it.sourceName}: ${it.entity}" }}
            Anomalii detectate (${anomalies.size}): ${anomalies.take(3).joinToString { "${it.severity} (dev: ${it.deviationPercent}%)" }}
            
            Pentru fiecare ipoteză returnează exact în formatul:
            TITLU: [Titlu scurt]
            DESCRIERE: [Descriere fizică detaliată]
            CONFIDENCE: [procent 0-100]
            ---
        """.trimIndent()

        try {
            val response = callGeminiApi(apiKey, "Ești un motor de raționament științific ipotetico-deductiv.", prompt)
            parseHypothesesFromGemini(response, observations.firstOrNull()?.investigationId ?: "")
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun callGeminiApi(apiKey: String, systemInstruction: String, userPrompt: String): String {
        val url = "$baseUrl?key=$apiKey"

        val requestBodyMap = mapOf(
            "systemInstruction" to mapOf(
                "parts" to listOf(mapOf("text" to systemInstruction))
            ),
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(mapOf("text" to userPrompt))
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 0.35,
                "topP" to 0.9,
                "maxOutputTokens" to 1500
            )
        )

        val jsonBody = gson.toJson(requestBodyMap)
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw RuntimeException("HTTP ${response.code}: $errBody")
            }

            val respString = response.body?.string() ?: throw RuntimeException("Empty response from Gemini")
            val root = gson.fromJson(respString, Map::class.java)
            val candidates = root["candidates"] as? List<*>
            val firstCandidate = candidates?.firstOrNull() as? Map<*, *>
            val content = firstCandidate?.get("content") as? Map<*, *>
            val parts = content?.get("parts") as? List<*>
            val firstPart = parts?.firstOrNull() as? Map<*, *>
            val text = firstPart?.get("text") as? String

            return text?.trim() ?: "Nu s-a putut extrage textul generat."
        }
    }

    private fun buildCandidatePrompt(res: XilonDetectionResult): String {
        val radiusEarth = res.derivedPlanetRadiusEarth ?: 1.0
        val depthPpm = res.transitDepthPpm
        val period = res.orbitalPeriodDays
        val duration = res.transitDurationHours
        val teq = res.derivedEquilibriumTempK ?: 300.0
        val semiMajor = res.derivedSemiMajorAxisAu ?: 0.1
        val oddEvenSigma = res.oddEvenTransitDeltaSigma
        val secondaryDepth = res.secondaryEclipseDepthPpm
        val starTeff = res.star.teff ?: 5778.0
        val starRadius = res.star.radiusSun ?: 1.0
        val starMag = res.star.tMag ?: 10.0

        return """
            Date fotometrice analizate:
            - Stea Gazdă: TIC ${res.star.ticId} (Teff = $starTeff K, Rază = $starRadius R_sun, Magnitudine = $starMag)
            - Semnal Tranzit: Perioadă orbitală = ${String.format(java.util.Locale.US, "%.4f", period)} zile, Adâncime = ${depthPpm.toInt()} ppm (${String.format(java.util.Locale.US, "%.3f", depthPpm / 10000.0)}%)
            - Durată Tranzit: ${String.format(java.util.Locale.US, "%.2f", duration)} ore
            - Rază Planetară Dedusă: ${String.format(java.util.Locale.US, "%.2f", radiusEarth)} R_Earth (${String.format(java.util.Locale.US, "%.2f", radiusEarth / 11.209)} R_Jupiter)
            - Distanță Semiaxă Mare: ${String.format(java.util.Locale.US, "%.4f", semiMajor)} AU
            - Temperatură de Echilibru (Teq): ${teq.toInt()} K
            - Raport Semnal/Zgomot (SNR): ${String.format(java.util.Locale.US, "%.1f", res.signalToNoiseRatio)} sigma
            - Test Par/Impar (Odd-Even difference): ${String.format(java.util.Locale.US, "%.2f", oddEvenSigma)} sigma
            - Eclipsă Secundară (Max depth): ${secondaryDepth.toInt()} ppm
            - Verdict Euristic: ${res.verdict.label}
            - Potrivire Arhivă NASA: ${res.archivalCrossMatch.canonicalName}
        """.trimIndent()
    }

    private fun calculateAiConfidence(res: XilonDetectionResult): Int {
        var conf = 85
        if (res.signalToNoiseRatio > 15) conf += 8
        if (res.oddEvenTransitDeltaSigma < 2.0) conf += 4
        if (res.secondaryEclipseDepthPpm < 80) conf += 3
        return conf.coerceIn(50, 99)
    }

    private fun generateLocalHeuristicFallback(res: XilonDetectionResult, errorNotice: String? = null): GeminiAnalysisResult {
        val radiusEarth = res.derivedPlanetRadiusEarth ?: 1.0
        val teq = res.derivedEquilibriumTempK ?: 300.0
        val dossier = buildString {
            if (errorNotice != null) {
                appendLine("ℹ️ $errorNotice\n")
            } else {
                appendLine("💡 *Analiză Euristică Locală (Configurează cheia Gemini API pentru sinteză neuronală live)*\n")
            }
            appendLine("1. NATURA ASTROFIZICĂ:")
            appendLine("Semnalul pe TIC ${res.star.ticId} prezintă o adâncime de ${res.transitDepthPpm.toInt()} ppm cu o simetrie pronunțată a tranzitului. Diferența de adâncime par/impar de ${String.format(java.util.Locale.US, "%.1f", res.oddEvenTransitDeltaSigma)} sigma este sub pragul critic de binară eclipsantă de contact.")
            appendLine()
            appendLine("2. STRUCTURĂ ȘI CLASĂ PLANETARĂ:")
            if (radiusEarth > 8.0) {
                appendLine("Raza de ${String.format(java.util.Locale.US, "%.1f", radiusEarth)} R⊕ indică un gigant gazos Jovian cu anvelopă masivă de Hidrogen și Heliu.")
            } else if (radiusEarth in 2.0..8.0) {
                appendLine("Raza de ${String.format(java.util.Locale.US, "%.1f", radiusEarth)} R⊕ plasează corpul în clasa Sub-Neptuniană / Lume Oceanică.")
            } else {
                appendLine("Raza de ${String.format(java.util.Locale.US, "%.1f", radiusEarth)} R⊕ corespunde unei Super-Terestre sau planete telurice compacte.")
            }
            appendLine()
            appendLine("3. POTENȚIAL SPECTROSCOPIC:")
            appendLine("La o temperatură de echilibru de ${teq.toInt()} K, scara de înălțime atmosferică este propice pentru spectroscopie de transmisie cu JWST (NIRSpec/PRISM).")
            appendLine()
            appendLine("4. VERDICT & ETAPE URMATOARE:")
            appendLine("Candidat recomandat pentru spectrografie de viteză radială de înaltă precizie (HARPS / ESPRESSO) pentru determinarea masei reale.")
        }

        return GeminiAnalysisResult(
            isLiveAiGenerated = false,
            modelName = "XILON-Heuristic (Offline)",
            analysisDossier = dossier,
            confidenceScorePercent = calculateAiConfidence(res)
        )
    }

    private fun parseHypothesesFromGemini(text: String, investigationId: String): List<Hypothesis> {
        val list = mutableListOf<Hypothesis>()
        val blocks = text.split("---")
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            val titleLine = lines.firstOrNull { it.startsWith("TITLU:", ignoreCase = true) }?.removePrefix("TITLU:")?.trim()
            val descLine = lines.firstOrNull { it.startsWith("DESCRIERE:", ignoreCase = true) }?.removePrefix("DESCRIERE:")?.trim()
            val confLine = lines.firstOrNull { it.startsWith("CONFIDENCE:", ignoreCase = true) }?.removePrefix("CONFIDENCE:")?.trim()

            if (titleLine != null && descLine != null) {
                val conf = confLine?.filter { it.isDigit() }?.toFloatOrNull() ?: 75f
                list.add(
                    Hypothesis(
                        id = UUID.randomUUID().toString(),
                        investigationId = investigationId,
                        title = titleLine,
                        description = descLine,
                        evidenceCount = 3,
                        contradictionCount = 0,
                        confidence = conf,
                        discriminatingTests = emptyList(),
                        status = HypothesisStatus.ACTIVE
                    )
                )
            }
        }
        return list
    }
}

data class GeminiAnalysisResult(
    val isLiveAiGenerated: Boolean,
    val modelName: String,
    val analysisDossier: String,
    val confidenceScorePercent: Int
)
