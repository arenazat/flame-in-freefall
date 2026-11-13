package org.example.project.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.domain.repository.ReportRepository
import org.example.project.models.ReportRequest
import org.example.project.models.ReportResult

class ReportRepositoryImpl(
    private val httpClient: HttpClient,
    private val supabaseProvider: SupabaseProvider
) : ReportRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun generateReport(experimentIds: List<String>, title: String?): Result<ReportResult> {
        if (experimentIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("Select at least one record."))
        }

        val config = supabaseProvider.config
        val url = "${config.supabaseUrl}/functions/v1/generate-report"

        val request = ReportRequest(
            experimentIds = experimentIds,
            title = title
        )

        return try {
            val response = httpClient.post(url) {
                header("Authorization", "Bearer ${config.supabaseAnonKey}")
                header("apikey", config.supabaseAnonKey)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }

            val bodyText = response.bodyAsText()
            if (response.status.value in 200..299) {
                val reportResult = json.decodeFromString<ReportResult>(bodyText)
                Result.success(reportResult)
            } else {
                Result.failure(Exception("Could not generate the report (${response.status.value}): $bodyText"))
            }
        } catch (e: Exception) {
            println("[HATA] generateReport çağrısı başarısız: ${e.message}")
            Result.failure(e)
        }
    }
}
