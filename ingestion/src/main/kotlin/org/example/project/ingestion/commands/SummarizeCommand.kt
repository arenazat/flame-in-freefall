package org.example.project.ingestion.commands

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.ingestion.IngestionSupabaseClient
import org.example.project.ingestion.IngestorConfig
import org.example.project.ingestion.models.ExistingSummaryReference
import org.example.project.ingestion.models.ExperimentSummaryInsertDto
import org.example.project.ingestion.models.LlmSummaryResult
import org.example.project.ingestion.models.OpenAiChatMessage
import org.example.project.ingestion.models.OpenAiChatRequest
import org.example.project.ingestion.models.OpenAiChatResponse

@Serializable
data class ExperimentSummaryCandidate(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("raw_text") val rawText: String? = null,
    @SerialName("needs_manual_review") val needsManualReview: Boolean = false
)

class SummarizeCommand(
    private val httpClient: HttpClient,
    private val supabaseClient: IngestionSupabaseClient,
    private val config: IngestorConfig
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val systemPrompt = """
        Sen NASA mikro-yerçekimi yanma deneyleri (FLEX, SAFFIRE, BASS, ACME vb.) uzmanı bir yapay zeka analitik sistemisin.
        Görevin, sana sunulan GERÇEK deney metnini analiz ederek JSON formatında özet, ana bulgular ve yangın/güvenlik risk skoru üretmektir.

        KESİN KURALLAR:
        1. Sadece sana verilen gerçek metindeki bilgiyi kullan; metinde yeterli bilgi yoksa 'bu deney için yeterli bilgi çıkarılamadı' de, bilgi uydurma.
        2. Kurgusal veya tahmin edilen hiçbir veri ekleme; metinde açıkça yer almayan hiçbir deneyi veya parametreyi icat etme.
        3. Metin yeterli ayrıntı içermiyorsa veya taranmış/eksik bir dökümansa status alanını 'insufficient_data' yap.
        4. Risk skoru (0-100) mikro-yerçekiminde yanıcılık, alev yayılma hızı, toksik gaz salımı ve uzay aracı yangın güvenliği tehlikesini yansıtmalıdır.
           - 0-25  : Düşük Risk (Düşük tutuşabilirlik, kendini söndüren alev)
           - 26-50 : Orta Risk (Kararlı yanan numuneler, kontrol edilebilir sönme)
           - 51-75 : Yüksek Risk (Hızlı alev yayılması, yüksek ısı salımı)
           - 76-100: Kritik Risk (Yüksek oksijende hızlı yanma, patlayıcı yayılma, uzay aracı için yüksek yangın tehlikesi)
        5. 1g (Dünya) vs Mikro-yerçekimi (ug) Sayısal Karşılaştırması:
           Eğer deneyin gerçek metninde hem 1g hem de mikro-yerçekimi (ug) için ölçülmüş sayısal bir değer (özellikle alev yayılma hızı, yanma hızı veya sönme limiti vb.) AÇIKÇA ve DOĞRUDAN yer alıyorsa:
           - 'one_g_value': 1g'deki sayısal ölçüm (ondalık sayı, örn: 2.8)
           - 'micro_g_value': mikro-yerçekimindeki sayısal ölçüm (ondalık sayı, örn: 0.9)
           - 'comparison_unit': ölçüm birimi (örn: 'mm/s', 'cm/s')
           - 'comparison_metric_name': ölçülen metrik (örn: 'Alev Yayılma Hızı', 'Yanma Hızı')
           - 'comparison_notes': metindeki bağlam ve bulgu (örn: '1g ortamında 2.8 mm/s, mikroyerçekiminde 0.9 mm/s ölçülmüştür.')
           Eğer metinde her iki ortam için de açık sayısal değerler YOKSA, KESİNLİKLE SAYI UYDURMA; bu alanları null bırak.

        JSON Formatı:
        {
          "status": "ok" veya "insufficient_data",
          "ai_summary": "Deneyin amacı, yöntemi ve sonucuna dair gerçek metne dayalı 2-3 cümlelik özeti.",
          "key_finding": "Deneyde elde edilen en kritik bilimsel veya fiziksel bulgu.",
          "safety_implication": "Uzay aracı yangın güvenliği veya malzeme standartlarına dair çıkarım.",
          "risk_score": 0 ile 100 arasında bir tamsayı,
          "risk_label": "Düşük Risk" | "Orta Risk" | "Yüksek Risk" | "Kritik Risk",
          "one_g_value": null veya ondalık sayı,
          "micro_g_value": null veya ondalık sayı,
          "comparison_unit": null veya "mm/s",
          "comparison_metric_name": null veya "Alev Yayılma Hızı",
          "comparison_notes": null veya metin
        }
    """.trimIndent()

    suspend fun execute(args: List<String>) {
        println("[SUMMARIZE] Gerçek metinlere dayalı AI özetleme ve risk skoru analizi başlatılıyor...")

        val apiKey = config.openaiApiKey
        val gemini = if (config.useGemini) org.example.project.ingestion.GeminiClient(httpClient, config.geminiApiKey, config.geminiChatModel) else null
        println("[SUMMARIZE] Sağlayıcı: " + if (gemini != null) "Google Gemini (${config.geminiChatModel})" else "OpenAI (gpt-4o-mini)")
        if (!config.hasAiKey) {
            throw IllegalStateException(
                """
                [HATA] LLM API anahtarı bulunamadı!
                Kural gereği uydurma/mock veri yazılamaz. Gerçek AI analizi ve risk skoru hesaplamak için
                lütfen local.properties dosyasına GEMINI_API_KEY (ücretsiz: aistudio.google.com/apikey) veya OPENAI_API_KEY ekleyiniz.

                Örnek:
                  GEMINI_API_KEY=...
                  .\gradlew.bat :ingestion:run --args="summarize"
                """.trimIndent()
            )
        }

        // 1. Supabase'den raw_text'i dolu olan ve needs_manual_review=false olan kayıtları çek
        val candidates = fetchCandidatesFromSupabase()
        println("[SUMMARIZE] Toplam uygun deney kaydı sayısı: ${candidates.size}")

        if (candidates.isEmpty()) {
            println("[SUMMARIZE] İşlenecek aday kayıt bulunamadı. raw_text dolu ve needs_manual_review=false olmalıdır.")
            return
        }

        // 2. Halihazırda özeti çıkarılmış kayıtların experiment_id'lerini al (mükerrerliği önleme)
        val alreadySummarizedIds = fetchAlreadySummarizedIds()
        val toProcess = candidates.filter { !alreadySummarizedIds.contains(it.id) }

        println("[SUMMARIZE] Halihazırda özeti bulunan kayıt sayısı : ${alreadySummarizedIds.size}")
        println("[SUMMARIZE] Yeni özet çıkarılacak kayıt sayısı     : ${toProcess.size}")

        if (toProcess.isEmpty()) {
            println("[SUMMARIZE] Tüm aday kayıtların özetleri veritabanında zaten mevcut.")
            return
        }

        var successCount = 0
        var insufficientCount = 0

        for ((index, exp) in toProcess.withIndex()) {
            val rawText = exp.rawText ?: continue
            println("\n[${index + 1}/${toProcess.size}] Analiz ediliyor: '${exp.title}' (ID: ${exp.id})")

            // Gerçek metin üzerinden LLM çağrısı
            val llmResult = if (gemini != null) {
                if (index > 0) kotlinx.coroutines.delay(6_000) // ücretsiz katman dakika limiti için
                callGeminiForSummary(gemini, exp.title, rawText)
            } else {
                callLlmForSummary(apiKey, exp.title, rawText)
            }

            val summaryDto = if (llmResult.status.lowercase() == "insufficient_data" || llmResult.aiSummary.isBlank()) {
                println("    [YETERSİZ VERİ] Gerçek metinde yeterli detay bulunamadı -> status: 'insufficient_data'.")
                insufficientCount++
                ExperimentSummaryInsertDto(
                    experimentId = exp.id,
                    aiSummary = "Bu deney için yeterli bilgi çıkarılamadı.",
                    riskScore = 0,
                    riskLabel = "Yetersiz Veri",
                    keyFinding = "Gerçek metin analiz için yetersiz ayrıntı içermektedir.",
                    status = "insufficient_data",
                    oneGValue = null,
                    microGValue = null,
                    comparisonUnit = null,
                    comparisonMetricName = null,
                    comparisonNotes = null
                )
            } else {
                println("    [BAŞARILI] Özet üretildi | Risk Skoru: ${llmResult.riskScore} (${llmResult.riskLabel})")
                val combinedFinding = buildString {
                    append(llmResult.keyFinding.trim())
                    if (llmResult.safetyImplication.isNotBlank()) {
                        append(" [Güvenlik Çıkarımı: ${llmResult.safetyImplication.trim()}]")
                    }
                }
                successCount++
                ExperimentSummaryInsertDto(
                    experimentId = exp.id,
                    aiSummary = llmResult.aiSummary.trim(),
                    riskScore = llmResult.riskScore.coerceIn(0, 100),
                    riskLabel = llmResult.riskLabel.trim(),
                    keyFinding = combinedFinding,
                    status = "ok",
                    oneGValue = llmResult.oneGValue,
                    microGValue = llmResult.microGValue,
                    comparisonUnit = llmResult.comparisonUnit,
                    comparisonMetricName = llmResult.comparisonMetricName,
                    comparisonNotes = llmResult.comparisonNotes
                )
            }

            // Supabase 'experiment_summaries' tablosuna kaydet
            insertSummaryToSupabase(summaryDto)
            println("    -> Supabase 'experiment_summaries' tablosuna kaydedildi.")
        }

        println("\n======================================================================")
        println("[SUMMARIZE TAMAMLANDI]")
        println("  Başarıyla özetlenen kayıt sayısı (status=ok)             : $successCount")
        println("  Yetersiz veri nedeniyle işaretlenen (status=insufficient): $insufficientCount")
        println("======================================================================")
    }

    private suspend fun fetchCandidatesFromSupabase(): List<ExperimentSummaryCandidate> {
        val records = supabaseClient.client.postgrest["experiments"]
            .select()
            .decodeList<ExperimentSummaryCandidate>()

        return records.filter { record ->
            !record.needsManualReview && !record.rawText.isNullOrBlank()
        }
    }

    private suspend fun fetchAlreadySummarizedIds(): Set<String> {
        return try {
            val existing = supabaseClient.client.postgrest["experiment_summaries"]
                .select(Columns.list("experiment_id"))
                .decodeList<ExistingSummaryReference>()
            existing.map { it.experimentId }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private suspend fun callLlmForSummary(
        apiKey: String,
        title: String,
        rawText: String
    ): LlmSummaryResult {
        val url = "https://api.openai.com/v1/chat/completions"

        // Token limitini korumak için metni gerekirse en kritik ilk 12.000 karaktere sınırla
        val trimmedText = if (rawText.length > 12000) rawText.take(12000) + "\n...[METİN DEVAMI KESİLDİ]..." else rawText

        val request = OpenAiChatRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                OpenAiChatMessage(role = "system", content = systemPrompt),
                OpenAiChatMessage(
                    role = "user",
                    content = "DENEY BAŞLIĞI: $title\n\nGERÇEK METİN:\n$trimmedText"
                )
            ),
            temperature = 0.2
        )

        val response = try {
            httpClient.post(url) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
        } catch (e: Exception) {
            throw IllegalStateException("[HATA] LLM API sunucusuna ulaşılamadı: ${e.message}", e)
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException(
                "[HATA] LLM API beklenmeyen HTTP durum kodu döndürdü: ${response.status}\n" +
                        "Yanıt: ${response.bodyAsText()}"
            )
        }

        val chatResponse = json.decodeFromString<OpenAiChatResponse>(response.bodyAsText())
        val content = chatResponse.choices?.firstOrNull()?.message?.content
            ?: throw IllegalStateException("[HATA] LLM API boş yanıt döndürdü.")

        return try {
            json.decodeFromString<LlmSummaryResult>(content)
        } catch (e: Exception) {
            println("    [UYARI] JSON ayrıştırma hatası, içerik: $content")
            LlmSummaryResult(
                status = "insufficient_data",
                aiSummary = "",
                keyFinding = "JSON ayrıştırma hatası",
                riskScore = 0,
                riskLabel = "Yetersiz Veri"
            )
        }
    }

    private suspend fun callGeminiForSummary(
        gemini: org.example.project.ingestion.GeminiClient,
        title: String,
        rawText: String
    ): LlmSummaryResult {
        val trimmedText = if (rawText.length > 12000) rawText.take(12000) + "\n...[METİN DEVAMI KESİLDİ]..." else rawText
        val content = gemini.generateJson(systemPrompt, "DENEY BAŞLIĞI: $title\n\nGERÇEK METİN:\n$trimmedText")
        return try {
            json.decodeFromString<LlmSummaryResult>(content)
        } catch (e: Exception) {
            println("    [UYARI] JSON ayrıştırma hatası, içerik: $content")
            LlmSummaryResult(
                status = "insufficient_data",
                aiSummary = "",
                keyFinding = "JSON ayrıştırma hatası",
                riskScore = 0,
                riskLabel = "Yetersiz Veri"
            )
        }
    }

    private suspend fun insertSummaryToSupabase(summary: ExperimentSummaryInsertDto) {
        try {
            supabaseClient.client.postgrest["experiment_summaries"]
                .insert(summary)
        } catch (e: Exception) {
            throw IllegalStateException(
                "[HATA] 'experiment_summaries' tablosuna kayıt yazılamadı: ${e.message}",
                e
            )
        }
    }
}
