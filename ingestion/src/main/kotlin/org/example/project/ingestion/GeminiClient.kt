package org.example.project.ingestion

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.math.sqrt

/**
 * Google Gemini API istemcisi (ücretsiz katman uyumlu).
 * - Embedding: gemini-embedding-001, 1536 boyut (veritabanındaki vector(1536) ile birebir uyumlu)
 * - Metin üretimi: GEMINI_CHAT_MODEL (varsayılan gemini-flash-latest), JSON çıktı modu
 * Ücretsiz katman hız limitleri için 429 / 503 yanıtlarında bekleyip yeniden dener.
 */
class GeminiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val chatModel: String
) {
    private val base = "https://generativelanguage.googleapis.com/v1beta/models"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    companion object {
        const val EMBED_MODEL = "gemini-embedding-001"
        const val EMBED_DIM = 1536
    }

    private suspend fun postWithRetry(url: String, body: JsonObject, what: String): String {
        var wait = 20_000L
        repeat(10) { attempt ->
            val response: HttpResponse = try {
                httpClient.post(url) {
                    header("x-goog-api-key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
            } catch (e: Exception) {
                throw IllegalStateException("[HATA] Gemini API'ye ulaşılamadı ($what): ${e.message}", e)
            }
            val text = response.bodyAsText()
            when (response.status) {
                HttpStatusCode.OK -> return text
                HttpStatusCode.TooManyRequests, HttpStatusCode.ServiceUnavailable, HttpStatusCode.InternalServerError -> {
                    println("    [BEKLE] Gemini ${response.status.value} ($what) — ${wait / 1000} sn sonra tekrar (deneme ${attempt + 1}/10)")
                    if (attempt == 0) println("    [DETAY] ${text.take(300).replace("\n", " ")}")
                    delay(wait)
                    wait = (wait * 2).coerceAtMost(90_000L)
                }
                else -> throw IllegalStateException("[HATA] Gemini API ${response.status} ($what)\nYanıt: ${text.take(800)}")
            }
        }
        throw IllegalStateException("[HATA] Gemini API hız limiti aşıldı ($what). Birkaç dakika sonra komutu tekrar çalıştırın; işlenmiş kayıtlar atlanır.")
    }

    private fun normalize(v: List<Float>): List<Float> {
        val n = sqrt(v.sumOf { (it * it).toDouble() }).toFloat()
        return if (n > 0f) v.map { it / n } else v
    }

    /** Metin listesi için 1536 boyutlu, normalize edilmiş embedding'ler (en fazla 100'lük gruplar). */
    suspend fun embed(texts: List<String>, taskType: String = "RETRIEVAL_DOCUMENT"): List<List<Float>> {
        val out = mutableListOf<List<Float>>()
        texts.chunked(100).forEach { group ->
            val body = buildJsonObject {
                putJsonArray("requests") {
                    group.forEach { t ->
                        addJsonObject {
                            put("model", "models/$EMBED_MODEL")
                            putJsonObject("content") {
                                putJsonArray("parts") { addJsonObject { put("text", t) } }
                            }
                            put("taskType", taskType)
                            put("outputDimensionality", EMBED_DIM)
                        }
                    }
                }
            }
            val text = postWithRetry("$base/$EMBED_MODEL:batchEmbedContents", body, "embedding")
            val arr = json.parseToJsonElement(text).jsonObject["embeddings"]?.jsonArray
                ?: throw IllegalStateException("[HATA] Gemini embedding yanıtında 'embeddings' yok: $text")
            arr.forEach { e ->
                val values = e.jsonObject["values"]!!.jsonArray.map { it.jsonPrimitive.content.toFloat() }
                if (values.size != EMBED_DIM) throw IllegalStateException("[HATA] Beklenen $EMBED_DIM boyut, gelen ${values.size}")
                out += normalize(values)
            }
        }
        if (out.size != texts.size) throw IllegalStateException("[HATA] Embedding sayısı eşleşmiyor (${out.size} != ${texts.size})")
        return out
    }

    /** Sistem talimatı + kullanıcı mesajı ile JSON metin üretir; modelin döndürdüğü ham JSON metnini verir. */
    suspend fun generateJson(systemPrompt: String, userText: String, temperature: Double = 0.2): String {
        val body = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { addJsonObject { put("text", systemPrompt) } }
            }
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") { addJsonObject { put("text", userText) } }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", temperature)
                put("responseMimeType", "application/json")
            }
        }
        val text = postWithRetry("$base/$chatModel:generateContent", body, "metin üretimi")
        val parts = json.parseToJsonElement(text).jsonObject["candidates"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray
        return parts?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
            ?.trim()?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim()
            ?: throw IllegalStateException("[HATA] Gemini boş yanıt döndürdü: $text")
    }
}
