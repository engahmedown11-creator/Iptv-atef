package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.time.Duration

data class AiSynopsisResult(
    val title: String,
    val cleanTitle: String,
    val arabicOverview: String,
    val cast: List<String> = emptyList(), // أبطال العمل
    val keyThemes: List<String> = emptyList(),
    val viewerGuidance: String = "",
    val isGeneratedByAi: Boolean = false
)

object AiMetadataEngine {
    private const val TAG = "AiMetadataEngine"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(6))
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(5))
            .build()
    }

    private val generativeModel by lazy {
        try {
            Firebase.ai.generativeModel("gemini-3.8-flash")
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Checks if a given text is genuinely and predominantly written in Arabic.
     * Prevents false positives where an English text contains 1-2 Arabic characters.
     */
    fun isGenuinelyArabic(text: String): Boolean {
        val clean = text.trim()
        if (clean.length < 6) return false
        val arabicChars = clean.count { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\u08A0'..'\u08FF' }
        val latinChars = clean.count { it in 'A'..'Z' || it in 'a'..'z' }
        return arabicChars >= 12 && (latinChars == 0 || arabicChars >= latinChars * 2)
    }

    /**
     * Translates any foreign language (English, French, Turkish, etc.) synopsis
     * into fluent, natural Arabic via Google Translate API over HTTPS.
     */
    suspend fun translateToArabic(text: String): String = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isEmpty() || isGenuinelyArabic(clean)) return@withContext clean
        try {
            val encodedText = URLEncoder.encode(clean, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=ar&dt=t&q=$encodedText"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val jsonArray = JSONArray(body)
                    val sentencesArray = jsonArray.optJSONArray(0)
                    if (sentencesArray != null) {
                        val sb = StringBuilder()
                        for (i in 0 until sentencesArray.length()) {
                            val sentence = sentencesArray.optJSONArray(i)
                            val trans = sentence?.optString(0, "") ?: ""
                            sb.append(trans)
                        }
                        val result = sb.toString().trim()
                        if (result.isNotEmpty() && isGenuinelyArabic(result)) {
                            return@withContext result
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fast translation failed: ${e.message}")
        }
        return@withContext ""
    }

    /**
     * Cleans up IPTV raw titles (removes stream tags, resolutions, brackets)
     * to extract the actual movie or series name and year for precise AI lookup.
     */
    fun cleanTitleForSearch(rawTitle: String): Pair<String, String> {
        var title = rawTitle

        // Extract year like (2023) or [2023] or 2023
        val yearRegex = Regex("""\b(19\d{2}|20\d{2})\b""")
        val yearMatch = yearRegex.find(title)
        val extractedYear = yearMatch?.value ?: ""

        // Strip common IPTV prefixes like "AR:", "EN:", "TR:", "VIP:", "NETFLIX:", etc.
        title = title.replace(Regex("""^[A-Za-z0-9_]{1,8}\s*[:|\-]\s*"""), "")
        // Remove quality, codec, source, and language keywords
        title = title.replace(
            Regex("""\b(4K|UHD|1080p|720p|480p|HD|FHD|WEB-?DL|BluRay|BRRip|HDR|HEVC|x264|x265|AAC|DTS|MULTI|AR|EN|DUAL|REMUX|WEB|H264|H265|TRUEHD|PRO|TV)\b""", RegexOption.IGNORE_CASE),
            ""
        )
        // Remove content inside brackets [ ] or ( )
        title = title.replace(Regex("""\[.*?\]|\(.*?\)|[\{|\}]"""), " ")
        // Replace dots, underscores, dashes with spaces
        title = title.replace(Regex("""[\._\-]"""), " ")
        // Remove extra whitespace
        title = title.replace(Regex("""\s+"""), " ").trim()

        // Remove standalone year if present in clean title
        if (extractedYear.isNotEmpty()) {
            title = title.replace(extractedYear, "").trim()
        }
        return Pair(title.ifEmpty { rawTitle }, extractedYear)
    }

    /**
     * Fetches or generates a 100% Arabic synopsis and cast members:
     * 1. If description is already genuinely in Arabic: keeps it unchanged and fetches cast (أبطال العمل).
     * 2. If description is in another language (e.g. English): accurately translates it to Arabic immediately.
     * 3. If description is missing or translation fails: generates a rich, non-spoiler Arabic story synopsis.
     * GUARANTEE: Never returns raw English text to the user!
     */
    suspend fun getOrGenerateArabicSynopsis(
        title: String,
        category: String,
        year: String = "",
        existingPlot: String = "",
        isSeries: Boolean = false
    ): AiSynopsisResult = withContext(Dispatchers.IO) {
        val cleanPlot = existingPlot.trim()
        val (cleanTitle, detectedYear) = cleanTitleForSearch(title)
        val actualYear = if (year.isNotEmpty()) year else detectedYear
        val isAlreadyArabic = isGenuinelyArabic(cleanPlot)

        // Step 1: If existing plot is foreign/English, translate it to Arabic immediately
        var arabicTranslation = ""
        if (!isAlreadyArabic && cleanPlot.isNotEmpty()) {
            arabicTranslation = translateToArabic(cleanPlot)
        }

        val baseArabicPlot = if (isAlreadyArabic) cleanPlot else arabicTranslation

        // Step 2: Try Firebase AI Logic (Gemini) if available for deep cast & non-spoiler plot
        val model = generativeModel
        if (model != null) {
            try {
                val prompt = when {
                    baseArabicPlot.isNotEmpty() -> {
                        """
                        أنت قاعدة بيانات سينمائية ذكية وموثوقة لمشغل ATEF IPTV.
                        المطلوب بدقة:
                        اسم العمل الفني: "$cleanTitle"
                        سنة الإنتاج: "$actualYear"
                        التصنيف: "$category"
                        الوصف المتوفر حالياً باللغة العربية: "$baseArabicPlot"
                        
                        التعليمات الصارمة:
                        1. احتفظ بالوصف العربي المتوفر كما هو دون حرق للأحداث.
                        2. اذكر أسماء أبطال العمل وطاقم التمثيل الرئيسيين الحقيقيين لهذا العمل (أهم 3 إلى 5 ممثلين حقيقيين بالاسم العربي أو المعرب).
                        3. أجب بدقة بالصيغة التالية فقط:
                        [الملخص]: $baseArabicPlot
                        [الأبطال]: ممثل 1، ممثل 2، ممثل 3
                        [الثيمات]: ثيمة 1، ثيمة 2
                        """.trimIndent()
                    }
                    else -> {
                        """
                        أنت موسوعة سينمائية وقاعدة بيانات دقيقة لمشغل ATEF IPTV.
                        المطلوب بدقة:
                        اسم ${if (isSeries) "المسلسل" else "الفيلم"} الحقيقي: "$cleanTitle"
                        سنة الإنتاج: "$actualYear"
                        التصنيف: "$category"
                        
                        التعليمات الصارمة:
                        1. ابحث وتعرف بدقة على هذا العمل الحقيقي بالاسم والسنة (ممنوع كتابة أي كلمة باللغة الإنجليزية).
                        2. اكتب ملخصاً سردياً حقيقياً لقصة هذا العمل تحديداً في فقرتين شيقتين باللغة العربية الفصحى دون أي حرق للأحداث أو النهاية.
                        3. اذكر أسماء أبطال العمل وطاقم التمثيل الرئيسيين الحقيقيين (أهم 3 إلى 5 ممثلين باللغة العربية).
                        4. أجب بدقة بالصيغة التالية:
                        [الملخص]: (الملخص العربي الدقيق للعمل دون حرق)
                        [الأبطال]: ممثل 1، ممثل 2، ممثل 3
                        [الثيمات]: ثيمة 1، ثيمة 2
                        """.trimIndent()
                    }
                }

                val response = withTimeout(12_000L) {
                    model.generateContent(prompt)
                }

                val aiText = response.text?.trim()
                if (!aiText.isNullOrEmpty()) {
                    val parsed = parseAiResponse(aiText, cleanTitle, baseArabicPlot)
                    if (parsed.arabicOverview.isNotEmpty() && isGenuinelyArabic(parsed.arabicOverview)) {
                        return@withContext parsed.copy(
                            title = title,
                            cleanTitle = cleanTitle,
                            viewerGuidance = "مناسب لعشاق $category • إنتاج ${actualYear.ifEmpty { "حديث" }}",
                            isGeneratedByAi = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini lookup failed, using translated or contextual Arabic fallback: ${e.message}")
            }
        }

        // Step 3: Reliable 100% Arabic Fallback (GUARANTEED NO ENGLISH)
        val finalArabicPlot = when {
            isAlreadyArabic -> cleanPlot
            arabicTranslation.isNotEmpty() -> arabicTranslation
            else -> generateContextualFallback(cleanTitle, category, actualYear, isSeries)
        }

        AiSynopsisResult(
            title = title,
            cleanTitle = cleanTitle,
            arabicOverview = finalArabicPlot,
            cast = listOf("طاقم تمثيل عمل $cleanTitle"),
            keyThemes = extractThemesFromCategory(category),
            viewerGuidance = "عمل ${if (isSeries) "درامي" else "سينمائي"} مميز ضمن تصنيف $category",
            isGeneratedByAi = arabicTranslation.isNotEmpty()
        )
    }

    private fun parseAiResponse(
        rawText: String,
        cleanTitle: String,
        baseArabicPlot: String
    ): AiSynopsisResult {
        var overview = ""
        val castList = mutableListOf<String>()
        val themesList = mutableListOf<String>()

        val lines = rawText.lines()
        var currentSection = ""
        val overviewBuilder = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("[الملخص]") || trimmed.startsWith("الملخص:") -> {
                    currentSection = "overview"
                    val content = trimmed.substringAfter(":").trim().removePrefix("]").trim()
                    if (content.isNotEmpty()) overviewBuilder.append(content).append("\n")
                }
                trimmed.startsWith("[الأبطال]") || trimmed.startsWith("الأبطال:") || trimmed.startsWith("[أبطال العمل]") -> {
                    currentSection = "cast"
                    val content = trimmed.substringAfter(":").trim().removePrefix("]").trim()
                    if (content.isNotEmpty()) {
                        castList.addAll(content.split(',', '،', '-').map { it.trim() }.filter { it.isNotEmpty() })
                    }
                }
                trimmed.startsWith("[الثيمات]") || trimmed.startsWith("الثيمات:") -> {
                    currentSection = "themes"
                    val content = trimmed.substringAfter(":").trim().removePrefix("]").trim()
                    if (content.isNotEmpty()) {
                        themesList.addAll(content.split(',', '،', '-').map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() })
                    }
                }
                else -> {
                    if (currentSection == "overview") {
                        overviewBuilder.append(trimmed).append("\n")
                    } else if (currentSection == "cast" && trimmed.isNotEmpty()) {
                        castList.addAll(trimmed.split(',', '،', '-').map { it.trim() }.filter { it.isNotEmpty() })
                    }
                }
            }
        }

        overview = overviewBuilder.toString().trim()
        if (overview.isEmpty() || !isGenuinelyArabic(overview)) {
            overview = if (baseArabicPlot.isNotEmpty()) baseArabicPlot else generateContextualFallback(cleanTitle, "", "")
        }

        return AiSynopsisResult(
            title = cleanTitle,
            cleanTitle = cleanTitle,
            arabicOverview = overview,
            cast = castList.distinct(),
            keyThemes = if (themesList.isNotEmpty()) themesList.distinct() else listOf("دراما", "تشويق وإثارة"),
            isGeneratedByAi = true
        )
    }

    private fun generateContextualFallback(title: String, category: String, year: String, isSeries: Boolean = false): String {
        val yearText = if (year.isNotEmpty()) " لعام $year" else ""
        val typeText = if (isSeries) "مسلسل" else "فيلم"
        val categoryText = if (category.isNotEmpty()) " في تصنيف $category" else ""
        return "تدور أحداث $typeText \"$title\"$yearText ضمن إطار سردي مشوق ومثير$categoryText. يتتبع العمل صراعات ومغامرات محورية تأخذ المشاهد في تجربة بصرية ودرامية شيقة مليئة بالمفاجآت دون الكشف عن تفاصيل النهاية."
    }

    private fun extractThemesFromCategory(category: String): List<String> {
        val cat = category.lowercase()
        return when {
            cat.contains("action") || cat.contains("أكشن") -> listOf("إثارة ومطاردات", "شجاعة وتحدي", "معارك بطولية")
            cat.contains("drama") || cat.contains("دراما") -> listOf("علاقات إنسانية", "صراعات اجتماعية", "واقعية")
            cat.contains("comedy") || cat.contains("كوميدي") -> listOf("مفارقات ساخرة", "أجواء مرحة", "مواقف طريفة")
            cat.contains("horror") || cat.contains("رعب") -> listOf("غموض وظلام", "ترقب نفسي", "أحداث خارقة")
            cat.contains("sci-fi") || cat.contains("خيال") -> listOf("مستقبل وتكنولوجيا", "عوالم موازية", "استكشاف فضاء")
            else -> listOf("تشويق عالي", "حبكة متقنة", "جودة سينمائية")
        }
    }
}
