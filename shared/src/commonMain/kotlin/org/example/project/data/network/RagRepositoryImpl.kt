package org.example.project.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.domain.repository.RagRepository
import org.example.project.models.RagAnswer
import org.example.project.models.RagQuestionRequest
import org.example.project.utils.FlameConfig

class RagRepositoryImpl(
    private val httpClient: HttpClient,
    private val config: FlameConfig
) : RagRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun askQuestion(question: String): Result<RagAnswer> {
        val trimmed = question.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("The question cannot be empty."))
        }

        if (!config.isConfigured) {
            return Result.failure(IllegalStateException("Supabase configuration is missing (SUPABASE_URL or SUPABASE_ANON_KEY not found)."))
        }

        val endpoint = "${config.supabaseUrl.trimEnd('/')}/functions/v1/ask"

        return try {
            val requestPayload = json.encodeToString(RagQuestionRequest(question = trimmed))

            val response = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                header("apikey", config.supabaseAnonKey)
                header(HttpHeaders.Authorization, "Bearer ${config.supabaseAnonKey}")
                setBody(requestPayload)
            }

            val responseBody = response.bodyAsText()

            if (!response.status.isSuccess()) {
                val errorMsg = try {
                    val parsed = json.decodeFromString<RagAnswer>(responseBody)
                    parsed.error ?: parsed.answer
                } catch (_: Exception) {
                    "Edge Function HTTP error (${response.status}): $responseBody"
                }
                return Result.failure(IllegalStateException(errorMsg))
            }

            val ragAnswer = json.decodeFromString<RagAnswer>(responseBody)
            Result.success(ragAnswer)
        } catch (e: Exception) {
            println("[HATA] RagRepository.askQuestion başarısız: ${e.message}")
            Result.failure(e)
        }
    }
}
