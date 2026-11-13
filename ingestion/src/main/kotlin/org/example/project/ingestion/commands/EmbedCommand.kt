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
import org.example.project.ingestion.models.ExistingChunkReference
import org.example.project.ingestion.models.ExperimentChunkInsertDto
import org.example.project.ingestion.models.OpenAiEmbeddingRequest
import org.example.project.ingestion.models.OpenAiEmbeddingResponse

@Serializable
data class ExperimentEmbedCandidate(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("raw_text") val rawText: String? = null,
    @SerialName("needs_manual_review") val needsManualReview: Boolean = false
)

class EmbedCommand(
    private val httpClient: HttpClient,
    private val supabaseClient: IngestionSupabaseClient,
    private val config: IngestorConfig
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    suspend fun execute(args: List<String>) {
        println("[EMBED] Gerçek metin parçalama (chunking) ve 1536-boyutlu vektörleştirme başlatılıyor...")

        // 1. API anahtarı kontrolü (GEMINI_API_KEY öncelikli, yoksa OPENAI_API_KEY)
        val apiKey = config.openaiApiKey
        // Embedding artık Supabase yerleşik gte-small modeliyle (384 boyut, anahtarsız) üretilir: embed-batch Edge Function
        println("[EMBED] Sağlayıcı: Supabase gte-small (384 boyut) — embed-batch Edge Function")
        if (false) {
            throw IllegalStateException(
                """
                [HATA] Vektör embedding API anahtarı bulunamadı!
                Kural gereği sahte/mock vektör üretilmez. Gerçek 1536-boyutlu embedding vektörleri hesaplamak için
                lütfen local.properties dosyasına GEMINI_API_KEY (ücretsiz: aistudio.google.com/apikey) veya OPENAI_API_KEY ekleyiniz.

                Örnek:
                  GEMINI_API_KEY=...
                  .\gradlew.bat :ingestion:run --args="embed"
                """.trimIndent()
            )
        }

        // 2. Supabase'den raw_text'i dolu olan ve needs_manual_review=false olan kayıtları çek
        val candidates = fetchCandidatesFromSupabase()
        println("[EMBED] Toplam uygun deney kaydı sayısı: ${candidates.size}")

        if (candidates.isEmpty()) {
            println("[EMBED] İşlenecek aday kayıt bulunamadı. raw_text dolu ve needs_manual_review=false olmalıdır.")
            return
        }

        // 3. Halihazırda chunk'lanmış kayıtların experiment_id'lerini al (mükerrer chunking önleme)
        val alreadyEmbeddedIds = fetchAlreadyEmbeddedIds()
        val toProcess = candidates.filter { !alreadyEmbeddedIds.contains(it.id) }

        println("[EMBED] Halihazırda embedding'i olan kayıt sayısı : ${alreadyEmbeddedIds.size}")
        println("[EMBED] Yeni embedding üretilecek kayıt sayısı    : ${toProcess.size}")

        if (toProcess.isEmpty()) {
            println("[EMBED] Tüm aday kayıtların chunk ve embedding'leri veritabanında zaten mevcut.")
            return
        }

        var totalChunksCreated = 0

        for ((index, exp) in toProcess.withIndex()) {
            val text = exp.rawText ?: continue
            println("\n[${index + 1}/${toProcess.size}] İşleniyor: '${exp.title}' (ID: ${exp.id})")

            val chunks = splitIntoChunks(text, targetChunkLength = 1800, overlapLength = 200)
                .map { it.stripBrokenSurrogates() }
            println("    -> Üretilen metin parçası (chunk) sayısı: ${chunks.size}")

            if (chunks.isEmpty()) {
                println("    [UYARI] Metin çok kısa veya parçalanamadı, atlanıyor.")
                continue
            }

            // Gerçek 1536-boyutlu embedding API çağrısı
            val embeddings = fetchEmbeddingsFromSupabase(chunks)
            println("    -> Supabase'den ${embeddings.size} adet 384-boyutlu gerçek vektör alındı.")

            val chunkDtos = chunks.mapIndexed { i, chunkText ->
                ExperimentChunkInsertDto(
                    experimentId = exp.id,
                    chunkText = chunkText,
                    embedding = embeddings[i]
                )
            }

            // Supabase 'experiment_chunks' tablosuna kaydet
            insertChunksToSupabase(chunkDtos)
            totalChunksCreated += chunkDtos.size
            println("    [BAŞARILI] Parçalar ve vektörler 'experiment_chunks' tablosuna yazıldı.")

        }

        println("\n======================================================================")
        println("[EMBED TAMAMLANDI]")
        println("  İşlenen deney kaydı sayısı         : ${toProcess.size}")
        println("  Oluşturulan toplam vektör chunk'ı  : $totalChunksCreated")
        println("======================================================================")
    }

    private suspend fun fetchCandidatesFromSupabase(): List<ExperimentEmbedCandidate> {
        val records = supabaseClient.client.postgrest["experiments"]
            .select()
            .decodeList<ExperimentEmbedCandidate>()

        return records.filter { record ->
            !record.needsManualReview && !record.rawText.isNullOrBlank()
        }
    }

    private suspend fun fetchAlreadyEmbeddedIds(): Set<String> {
        return try {
            val existing = supabaseClient.client.postgrest["experiment_chunks"]
                .select(Columns.list("experiment_id"))
                .decodeList<ExistingChunkReference>()
            existing.map { it.experimentId }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Anlamlı metin parçalama (Chunking):
     * ~500 token hedefi (yaklaşık 1800 karakter) ve 200 karakter overlap ile
     * paragraf ve cümle sınırlarına sadık kalarak metni böler.
     */
    private fun splitIntoChunks(
        text: String,
        targetChunkLength: Int = 1800,
        overlapLength: Int = 200
    ): List<String> {
        val cleaned = text.trim()
        if (cleaned.length <= targetChunkLength) {
            return if (cleaned.length >= 50) listOf(cleaned) else emptyList()
        }

        val chunks = mutableListOf<String>()
        var startIndex = 0

        while (startIndex < cleaned.length) {
            var endIndex = (startIndex + targetChunkLength).coerceAtMost(cleaned.length)

            if (endIndex < cleaned.length) {
                // Paragraf veya cümle sonu sınırında bölmeye çalış
                val paragraphBreak = cleaned.lastIndexOf("\n\n", endIndex)
                val sentenceBreak = cleaned.lastIndexOf(". ", endIndex)

                if (paragraphBreak > startIndex + targetChunkLength / 2) {
                    endIndex = paragraphBreak + 2
                } else if (sentenceBreak > startIndex + targetChunkLength / 2) {
                    endIndex = sentenceBreak + 2
                }
            }

            val chunk = cleaned.substring(startIndex, endIndex).trim()
            if (chunk.length >= 50) {
                chunks.add(chunk)
            }

            if (endIndex >= cleaned.length) break
            startIndex = (endIndex - overlapLength).coerceAtLeast(startIndex + 1)
        }

        return chunks
    }

    /**
     * OpenAI Embeddings API (text-embedding-3-small -> 1536 dim) çağrısı.
     */
    private suspend fun fetchEmbeddingsFromApi(
        apiKey: String,
        chunks: List<String>
    ): List<List<Float>> {
        val url = "https://api.openai.com/v1/embeddings"
        val requestBody = OpenAiEmbeddingRequest(
            model = "text-embedding-3-small",
            input = chunks
        )

        val response = try {
            httpClient.post(url) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(requestBody))
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "[HATA] Embedding API sunucusuna ulaşılamadı: ${e.message}",
                e
            )
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException(
                "[HATA] Embedding API beklenmeyen HTTP durum kodu döndürdü: ${response.status}\n" +
                        "Yanıt: ${response.bodyAsText()}"
            )
        }

        val parsed = json.decodeFromString<OpenAiEmbeddingResponse>(response.bodyAsText())
        val dataList = parsed.data
            ?: throw IllegalStateException("[HATA] Embedding API yanıtında 'data' bulunamadı.")

        if (dataList.size != chunks.size) {
            throw IllegalStateException("[HATA] Embedding sayısı chunk sayısıyla eşleşmiyor (${dataList.size} != ${chunks.size})")
        }

        return dataList.sortedBy { it.index }.map { it.embedding }
    }

    /** Supabase 'embed-batch' Edge Function (gte-small, 384 boyut) üzerinden 8'li gruplar halinde embedding. */
    private suspend fun fetchEmbeddingsFromSupabase(chunks: List<String>): List<List<Float>> {
        val url = "${config.supabaseUrl}/functions/v1/embed-batch"
        val out = mutableListOf<List<Float>>()
        // Ücretsiz katman işlem gücü sınırı (WORKER_RESOURCE_LIMIT) nedeniyle her çağrıda tek metin
        for (group in chunks.chunked(1)) {
            var attempt = 0
            while (true) {
                attempt++
                val response = try {
                    httpClient.post(url) {
                        header("Authorization", "Bearer ${config.supabaseServiceRoleKey}")
                        header("apikey", config.supabaseServiceRoleKey)
                        contentType(ContentType.Application.Json)
                        setBody(json.encodeToString(mapOf("texts" to group)))
                    }
                } catch (e: Exception) {
                    if (attempt < 4) { kotlinx.coroutines.delay(5_000L * attempt); continue }
                    throw IllegalStateException("[HATA] embed-batch fonksiyonuna ulaşılamadı: ${e.message}", e)
                }
                val text = response.bodyAsText()
                if (response.status == HttpStatusCode.OK) {
                    val root = json.parseToJsonElement(text) as kotlinx.serialization.json.JsonObject
                    val arr = root["embeddings"] as kotlinx.serialization.json.JsonArray
                    arr.forEach { e ->
                        out += (e as kotlinx.serialization.json.JsonArray).map {
                            (it as kotlinx.serialization.json.JsonPrimitive).content.toFloat()
                        }
                    }
                    break
                }
                if (response.status == HttpStatusCode.NotFound) {
                    throw IllegalStateException("[HATA] 'embed-batch' Edge Function bulunamadı. Önce: supabase functions deploy embed-batch --no-verify-jwt")
                }
                if (attempt < 6) {
                    println("    [BEKLE] embed-batch ${response.status.value} — ${3 * attempt} sn sonra tekrar (deneme $attempt/6)")
                    kotlinx.coroutines.delay(3_000L * attempt); continue
                }
                throw IllegalStateException("[HATA] embed-batch ${response.status}: ${text.take(500)}")
            }
        }
        if (out.size != chunks.size) throw IllegalStateException("[HATA] Embedding sayısı eşleşmiyor (${out.size} != ${chunks.size})")
        return out
    }

    private suspend fun insertChunksToSupabase(chunks: List<ExperimentChunkInsertDto>) {
        try {
            supabaseClient.client.postgrest["experiment_chunks"]
                .insert(chunks)
        } catch (e: Exception) {
            throw IllegalStateException(
                "[HATA] 'experiment_chunks' tablosuna kayıtlar yazılamadı: ${e.message}",
                e
            )
        }
    }
}

/** Parçalama sırasında ikiye bölünmüş (yarım kalmış) Unicode karakterlerini temizler. */
private fun String.stripBrokenSurrogates(): String {
    val sb = StringBuilder(length)
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) {
            sb.append(c).append(this[i + 1]); i += 2; continue
        }
        if (!c.isSurrogate()) sb.append(c)
        i++
    }
    return sb.toString()
}
