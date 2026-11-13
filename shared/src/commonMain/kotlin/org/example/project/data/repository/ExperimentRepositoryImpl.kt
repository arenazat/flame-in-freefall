package org.example.project.data.repository

import io.ktor.client.call.body
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.data.network.SupabaseProvider
import org.example.project.data.network.dto.ExperimentDetailDto
import org.example.project.domain.repository.ExperimentRepository
import org.example.project.models.Experiment
import org.example.project.models.ExperimentFilter
import org.example.project.models.FilterOptions

@Serializable
private data class FilterSourceDto(
    @SerialName("material") val material: String = "",
    @SerialName("series") val series: String = "",
    @SerialName("gravity_level") val gravityLevel: String = "",
    @SerialName("year") val year: Int? = null
)

class ExperimentRepositoryImpl(
    private val supabaseProvider: SupabaseProvider
) : ExperimentRepository {

    override fun getExperiments(filter: ExperimentFilter): Flow<List<Experiment>> = flow {
        if (!supabaseProvider.isReadyForQueries) {
            emit(emptyList())
            return@flow
        }

        try {
            val dtoList = supabaseProvider.client.postgrest["experiment_details_view"]
                .select {
                    filter {
                        filter.series?.takeIf { it.isNotBlank() && it != "ALL" }?.let { eq("series", it) }
                        filter.material?.takeIf { it.isNotBlank() && it != "ALL" }?.let { eq("material", it) }
                        filter.gravityLevel?.takeIf { it.isNotBlank() && it != "ALL" }?.let { eq("gravity_level", it) }
                        filter.minYear?.let { gte("year", it) }
                        filter.maxYear?.let { lte("year", it) }
                        filter.minRiskScore?.let { gte("risk_score", it) }
                        filter.maxRiskScore?.let { lte("risk_score", it) }
                        filter.query?.takeIf { it.isNotBlank() }?.let { ilike("title", "%$it%") }
                    }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<ExperimentDetailDto>()

            emit(dtoList.map { it.toDomain() })
        } catch (e: Exception) {
            println("[HATA] getExperiments sorgusu başarısız: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getRiskRankings(filter: ExperimentFilter): Flow<List<Experiment>> = flow {
        if (!supabaseProvider.isReadyForQueries) {
            emit(emptyList())
            return@flow
        }

        try {
            val dtoList = supabaseProvider.client.postgrest["experiment_details_view"]
                .select {
                    filter {
                        // Sadece geçerli risk skoru olan ve özet durumu ok olanları önceliklendir
                        gte("risk_score", 0)
                        filter.series?.takeIf { it.isNotBlank() && it != "ALL" }?.let { eq("series", it) }
                        filter.material?.takeIf { it.isNotBlank() && it != "ALL" }?.let { eq("material", it) }
                        filter.minRiskScore?.let { gte("risk_score", it) }
                        filter.maxRiskScore?.let { lte("risk_score", it) }
                    }
                    order(column = "risk_score", order = Order.DESCENDING)
                }
                .decodeList<ExperimentDetailDto>()

            emit(dtoList.map { it.toDomain() })
        } catch (e: Exception) {
            println("[HATA] getRiskRankings sorgusu başarısız: ${e.message}")
            emit(emptyList())
        }
    }

    /**
     * Filtre seçeneklerini veritabanındaki GERÇEK kayıtlardan dinamik olarak çeker.
     * Sabit/hardcoded liste İÇERMEZ.
     */
    override suspend fun getFilterOptions(): FilterOptions {
        if (!supabaseProvider.isReadyForQueries) {
            return FilterOptions()
        }

        return try {
            val rawFilters = supabaseProvider.client.postgrest["experiments"]
                .select(Columns.list("material", "series", "gravity_level", "year"))
                .decodeList<FilterSourceDto>()

            val materials = rawFilters.map { it.material.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            val series = rawFilters.map { it.series.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            val gravityLevels = rawFilters.map { it.gravityLevel.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            val years = rawFilters.mapNotNull { it.year }
            val minYear = years.minOrNull()
            val maxYear = years.maxOrNull()

            FilterOptions(
                materials = materials,
                series = series,
                gravityLevels = gravityLevels,
                minYear = minYear,
                maxYear = maxYear
            )
        } catch (e: Exception) {
            println("[HATA] getFilterOptions sorgusu başarısız: ${e.message}")
            FilterOptions()
        }
    }

    override suspend fun queryExperiments(naturalLanguageQuery: String): List<Experiment> {
        if (!supabaseProvider.isReadyForQueries || naturalLanguageQuery.isBlank()) {
            return emptyList()
        }

        return try {
            val dtoList = supabaseProvider.client.postgrest["experiment_details_view"]
                .select {
                    filter {
                        ilike("title", "%$naturalLanguageQuery%")
                    }
                }
                .decodeList<ExperimentDetailDto>()

            dtoList.map { it.toDomain() }
        } catch (e: Exception) {
            println("[HATA] queryExperiments sorgusu başarısız: ${e.message}")
            emptyList()
        }
    }

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    override suspend fun askRagQuestion(question: String): org.example.project.models.RagResponse {
        if (!supabaseProvider.isReadyForQueries || question.isBlank()) {
            return org.example.project.models.RagResponse(answer = "The question is empty or the Supabase connection is not ready.")
        }

        return try {
            val response = supabaseProvider.client.functions(
                function = "rag-query",
                body = kotlinx.serialization.json.buildJsonObject {
                    put("query", kotlinx.serialization.json.JsonPrimitive(question))
                }
            )
            val jsonString = response.body<String>()
            json.decodeFromString<org.example.project.models.RagResponse>(jsonString)
        } catch (e: Exception) {
            println("[HATA] askRagQuestion çağrısı başarısız: ${e.message}")
            org.example.project.models.RagResponse(
                answer = "A problem occurred while getting the RAG answer: ${e.message}",
                error = e.message
            )
        }
    }

    override fun isSupabaseConnected(): Boolean {
        return supabaseProvider.isReadyForQueries
    }
}
