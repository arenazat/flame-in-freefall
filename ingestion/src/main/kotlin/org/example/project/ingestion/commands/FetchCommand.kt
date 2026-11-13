package org.example.project.ingestion.commands

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.ingestion.IngestionSupabaseClient
import org.example.project.ingestion.models.ExperimentInsertDto
import org.example.project.ingestion.models.NtrsCitationItem
import org.example.project.ingestion.models.NtrsSearchResponse

@Serializable
private data class SourceUrlHolder(
    @SerialName("source_url") val sourceUrl: String
)

class FetchCommand(
    private val httpClient: HttpClient,
    private val supabaseClient: IngestionSupabaseClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // NASA NTRS üzerinde aranacak resmi mikro-yerçekimi yanma anahtar kelimeleri
    private val defaultTargetQueries = listOf(
        "microgravity combustion",
        "FLEX experiment",
        "SAFFIRE",
        "BASS combustion"
    )

    suspend fun execute(args: List<String>) {
        val targetQueries = if (args.isNotEmpty() && !args.first().equals("ALL", ignoreCase = true)) {
            listOf(args.joinToString(" "))
        } else {
            defaultTargetQueries
        }

        println("[FETCH] NASA Technical Reports Server (NTRS) üzerinden gerçek kayıtlar taranıyor...")
        println("[FETCH] Hedef sorgular: ${targetQueries.joinToString(", ")}")

        val allCitations = mutableMapOf<Long, NtrsCitationItem>()

        for (query in targetQueries) {
            println("\n--> NTRS API Sorgulanıyor: '$query'")
            val citations = fetchFromNtrsApi(query)
            println("    Alınan gerçek kayıt sayısı: ${citations.size}")

            for (citation in citations) {
                allCitations[citation.id] = citation
            }
        }

        if (allCitations.isEmpty()) {
            throw IllegalStateException("[HATA] NTRS API hiçbir kayıt döndürmedi. Ağ bağlantısını veya arama parametrelerini kontrol edin.")
        }

        println("\n[FETCH] Toplam tekil NASA NTRS kaydı: ${allCitations.size}")

        // 1. Supabase'de halihazırda mevcut olan kayıtların source_url'lerini kontrol et (mükerrer kayıt önleme)
        val existingUrls = fetchExistingSourceUrls()
        println("[FETCH] Veritabanında kayıtlı mevcut NTRS kaynak sayısı: ${existingUrls.size}")

        // 2. Sadece yeni gerçek kayıtları filtrele ve modelle
        val newExperiments = allCitations.values
            .map { mapCitationToExperiment(it) }
            .filter { !existingUrls.contains(it.sourceUrl) }

        if (newExperiments.isEmpty()) {
            println("[FETCH] Tüm kayıtlar veritabanında zaten mevcut. Yeni eklenecek kayıt yok.")
            return
        }

        println("[FETCH] Supabase 'experiments' tablosuna yazılacak yeni kayıt sayısı: ${newExperiments.size}")

        // 3. Supabase 'experiments' tablosuna service_role yetkisiyle yaz
        insertExperimentsToSupabase(newExperiments)

        println("\n[BAŞARILI] ${newExperiments.size} adet gerçek NASA NTRS deney kaydı Supabase'e kaydedildi!")
    }

    private suspend fun fetchFromNtrsApi(query: String): List<NtrsCitationItem> {
        val url = "https://ntrs.nasa.gov/api/citations/search?q=${query.encodeURLParameter()}"

        val response = try {
            httpClient.get(url) {
                header("User-Agent", "FlameInFreefall-Ingestor/1.0 (NASA Microgravity Research Dashboard)")
                header("Accept", "application/json")
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "[HATA] NASA NTRS API bağlantı hatası: ${e.message}. " +
                        "API'ye ulaşılamadığı için işlem durduruldu (kurgusal veri üretilmez).",
                e
            )
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException(
                "[HATA] NASA NTRS API beklenmeyen HTTP yanıtı döndürdü: ${response.status}. " +
                        "URL: $url. API yapısı değişmiş veya erişim engellenmiş olabilir."
            )
        }

        val responseText = response.bodyAsText()

        val parsed = try {
            json.decodeFromString<NtrsSearchResponse>(responseText)
        } catch (e: Exception) {
            throw IllegalStateException(
                "[HATA] NASA NTRS yanıtı JSON modeline dönüştürülemedi. API şeması değişmiş olabilir. " +
                        "Ham yanıt başlangıcı: ${responseText.take(300)}",
                e
            )
        }

        val results = parsed.results
            ?: throw IllegalStateException("[HATA] NASA NTRS yanıtında 'results' alanı bulunamadı!")

        return results
    }

    private suspend fun fetchExistingSourceUrls(): Set<String> {
        return try {
            val response = supabaseClient.client.postgrest["experiments"]
                .select(Columns.list("source_url"))
                .decodeList<SourceUrlHolder>()
            response.map { it.sourceUrl }.toSet()
        } catch (e: Exception) {
            println("[BİLGİ] Mevcut kayıtlar okunamadı veya tablo henüz boş: ${e.message}")
            emptySet()
        }
    }

    private suspend fun insertExperimentsToSupabase(experiments: List<ExperimentInsertDto>) {
        try {
            supabaseClient.client.postgrest["experiments"]
                .insert(experiments)
        } catch (e: Exception) {
            throw IllegalStateException(
                "[HATA] Supabase 'experiments' tablosuna kayıtlar yazılamadı: ${e.message}. " +
                        "Lütfen SUPABASE_URL ve SUPABASE_SERVICE_ROLE_KEY değerlerinizi kontrol ediniz.",
                e
            )
        }
    }

    private fun mapCitationToExperiment(item: NtrsCitationItem): ExperimentInsertDto {
        val title = item.title.trim()
        val abstractText = item.abstract?.trim() ?: ""
        val combinedText = "$title $abstractText"

        // Seri tespiti
        val series = when {
            combinedText.contains("FLEX", ignoreCase = true) -> "FLEX"
            combinedText.contains("SAFFIRE", ignoreCase = true) || combinedText.contains("Saffire", ignoreCase = false) -> "SAFFIRE"
            combinedText.contains("BASS", ignoreCase = true) -> "BASS"
            combinedText.contains("ACME", ignoreCase = true) -> "ACME"
            combinedText.contains("CIR", ignoreCase = true) -> "CIR"
            else -> "MICROGRAVITY_COMBUSTION"
        }

        // Malzeme tespiti (Ön tespit; sonraki aşamadaki 'extract' komutu bunu detaylandırır)
        val material = detectMaterial(combinedText)

        // Yıl tespiti
        val year = extractYear(item.distributionDate)
            ?: extractYear(item.created)
            ?: extractYear(item.modified)

        // Gerçek NASA NTRS erişim bağlantısı (ZORUNLU)
        val sourceUrl = "https://ntrs.nasa.gov/citations/${item.id}"

        val rawText = buildString {
            appendLine("TITLE: $title")
            appendLine("SERIES: $series")
            if (item.distributionDate != null) appendLine("DISTRIBUTION DATE: ${item.distributionDate}")
            if (!item.keywords.isNullOrEmpty()) appendLine("KEYWORDS: ${item.keywords.joinToString(", ")}")
            if (abstractText.isNotBlank()) {
                appendLine()
                appendLine("ABSTRACT:")
                appendLine(abstractText)
            }
            val pdfUrl = item.downloads?.firstOrNull { it.mimetype == "application/pdf" }?.links?.pdf
            if (pdfUrl != null) {
                appendLine()
                appendLine("PDF DOWNLOAD: https://ntrs.nasa.gov$pdfUrl")
            }
        }

        return ExperimentInsertDto(
            title = title,
            series = series,
            material = material,
            gravityLevel = "microgravity",
            oxygenConcentration = null, // Faz 1.3 'extract' komutunda analiz edilecek
            year = year,
            sourceUrl = sourceUrl,
            sourceType = "ntrs",
            rawText = rawText,
            needsManualReview = abstractText.isBlank()
        )
    }

    private fun detectMaterial(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("pmma") || lower.contains("polymethyl methacrylate") || lower.contains("plexiglas") -> "PMMA (Acrylic)"
            lower.contains("heptane") -> "Heptane"
            lower.contains("methanol") -> "Methanol"
            lower.contains("ethanol") -> "Ethanol"
            lower.contains("decane") -> "Decane"
            lower.contains("silicone") -> "Silicone"
            lower.contains("cotton") || lower.contains("fabric") -> "Cotton Fabric"
            lower.contains("polyethylene") -> "Polyethylene"
            lower.contains("methane") -> "Methane (Gaseous)"
            lower.contains("propane") -> "Propane (Gaseous)"
            lower.contains("droplet") -> "Fuel Droplet"
            lower.contains("solid") -> "Solid Fuel Specimen"
            else -> "Combustion Specimen"
        }
    }

    private fun extractYear(dateString: String?): Int? {
        if (dateString.isNullOrBlank()) return null
        val regex = Regex("""(19\d{2}|20\d{2})""")
        return regex.find(dateString)?.value?.toIntOrNull()
    }
}
