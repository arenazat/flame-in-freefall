package org.example.project.ingestion.commands

import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.example.project.ingestion.IngestionSupabaseClient
import org.example.project.ingestion.models.NtrsCitationItem

@Serializable
data class ExperimentExtractTarget(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("raw_text") val rawText: String? = null,
    @SerialName("needs_manual_review") val needsManualReview: Boolean = false,
    @SerialName("material") val material: String = "",
    @SerialName("oxygen_concentration") val oxygenConcentration: Double? = null
)

@Serializable
data class ExperimentUpdateTextPayload(
    @SerialName("raw_text") val rawText: String?,
    @SerialName("needs_manual_review") val needsManualReview: Boolean,
    @SerialName("material") val material: String? = null,
    @SerialName("oxygen_concentration") val oxygenConcentration: Double? = null
)

class ExtractCommand(
    private val httpClient: HttpClient,
    private val supabaseClient: IngestionSupabaseClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun execute(args: List<String>) {
        println("[EXTRACT] Supabase 'experiments' tablosundaki PDF/doküman kayıtları taranıyor...")

        // 1. Supabase'den raw_text'i boş olan veya doğrudan PDF'e işaret eden kayıtları çek
        val allRecords = try {
            supabaseClient.client.postgrest["experiments"]
                .select()
                .decodeList<ExperimentExtractTarget>()
        } catch (e: Exception) {
            throw IllegalStateException("[HATA] Supabase 'experiments' tablosu sorgulanamadı: ${e.message}", e)
        }

        if (allRecords.isEmpty()) {
            println("[EXTRACT] Tabloda henüz hiç deney kaydı bulunamadı. Önce 'fetch' komutunu çalıştırınız.")
            return
        }

        // raw_text'i boş olan veya source_url'i doğrudan .pdf uzantılı olan kayıtları hedefle
        val candidates = allRecords.filter { record ->
            record.sourceUrl.endsWith(".pdf", ignoreCase = true) ||
                    record.rawText.isNullOrBlank() ||
                    isOnlyMetadata(record.rawText)
        }

        println("[EXTRACT] Metin çıkarımı yapılması gereken hedef kayıt sayısı: ${candidates.size}")

        if (candidates.isEmpty()) {
            println("[EXTRACT] Tüm kayıtların metinleri halihazırda çıkarılmış. Yapılacak işlem yok.")
            return
        }

        var successCount = 0
        var manualReviewCount = 0

        for ((index, record) in candidates.withIndex()) {
            println("\n[${index + 1}/${candidates.size}] İnceleniyor: '${record.title}' (ID: ${record.id})")

            val pdfUrl = resolvePdfUrl(record)
            if (pdfUrl == null) {
                println("    [UYARI] PDF bağlantısı bulunamadı -> needs_manual_review = true olarak işaretleniyor.")
                updateRecordManualReview(record.id, reason = "PDF linki bulunamadı")
                manualReviewCount++
                continue
            }

            println("    PDF indiriliyor: $pdfUrl")
            val pdfBytes = downloadPdf(pdfUrl)

            if (pdfBytes == null || pdfBytes.isEmpty()) {
                println("    [UYARI] PDF indirilemedi veya boş dosya -> needs_manual_review = true.")
                updateRecordManualReview(record.id, reason = "PDF indirilemedi")
                manualReviewCount++
                continue
            }

            // Apache PDFBox ile GERÇEK metin çıkarımı
            val extractedText = extractTextWithPdfBox(pdfBytes)

            if (extractedText.isNullOrBlank() || extractedText.length < 100) {
                println("    [UYARI] PDF'ten metin çıkarılamadı (Taranmış görsel / OCR gerekli).")
                println("    -> Kural gereği uydurma özet üretilmiyor, needs_manual_review = true yapılıyor.")
                updateRecordManualReview(record.id, reason = "Metin çıkarılamadı (taranmış görüntü / OCR)")
                manualReviewCount++
            } else {
                println("    [BAŞARILI] Gerçek metin çıkarıldı (${extractedText.length} karakter).")

                // Varsa metin içinden oksijen konsantrasyonu ve malzeme detayını çıkar
                val detectedOxygen = extractOxygenConcentration(extractedText) ?: record.oxygenConcentration
                val detectedMaterial = if (record.material == "Combustion Specimen" || record.material.isBlank()) {
                    refineMaterial(extractedText) ?: record.material
                } else {
                    record.material
                }

                updateRecordSuccess(
                    recordId = record.id,
                    text = extractedText,
                    material = detectedMaterial,
                    oxygenConcentration = detectedOxygen
                )
                successCount++
            }
        }

        println("\n======================================================================")
        println("[EXTRACT TAMAMLANDI]")
        println("  Başarıyla metni çıkarılan kayıt sayısı : $successCount")
        println("  Manuel incelemeye ayrılan (OCR vb.)  : $manualReviewCount")
        println("======================================================================")
    }

    private fun isOnlyMetadata(rawText: String?): Boolean {
        if (rawText.isNullOrBlank()) return true
        // Yalnızca NTRS başlık ve özetinden ibaretse (gerçek tam makale metni henüz çıkarılmamışsa)
        return rawText.startsWith("TITLE:") && rawText.length < 2500
    }

    private suspend fun resolvePdfUrl(record: ExperimentExtractTarget): String? {
        val sourceUrl = record.sourceUrl.trim()

        // 1. source_url doğrudan .pdf ile bitiyorsa
        if (sourceUrl.endsWith(".pdf", ignoreCase = true)) {
            return sourceUrl
        }

        // 2. raw_text içinde "PDF DOWNLOAD:" satırı kayıtlıysa
        val rawText = record.rawText ?: ""
        val pdfLine = rawText.lines().firstOrNull { it.startsWith("PDF DOWNLOAD:") }
        if (pdfLine != null) {
            val url = pdfLine.removePrefix("PDF DOWNLOAD:").trim()
            if (url.isNotBlank()) return url
        }

        // 3. NTRS kayıt bağlantısı ise (https://ntrs.nasa.gov/citations/{id})
        if (sourceUrl.contains("ntrs.nasa.gov/citations/")) {
            val citationId = sourceUrl.substringAfterLast("/").trim()
            if (citationId.isNotBlank()) {
                val apiUrl = "https://ntrs.nasa.gov/api/citations/$citationId"
                try {
                    val response = httpClient.get(apiUrl) {
                        header("User-Agent", "FlameInFreefall-Ingestor/1.0 (NASA Microgravity Analytics)")
                        header("Accept", "application/json")
                    }
                    if (response.status == HttpStatusCode.OK) {
                        val bodyText = response.bodyAsText()
                        val citation = json.decodeFromString<NtrsCitationItem>(bodyText)
                        val relativePdf = citation.downloads
                            ?.firstOrNull { it.mimetype == "application/pdf" }
                            ?.links?.pdf
                        if (relativePdf != null) {
                            return "https://ntrs.nasa.gov$relativePdf"
                        }
                    }
                } catch (e: Exception) {
                    println("    [HATA] NTRS indirme linki sorgulanamadı: ${e.message}")
                }
            }
        }

        return null
    }

    private suspend fun downloadPdf(pdfUrl: String): ByteArray? {
        return try {
            val response = httpClient.get(pdfUrl) {
                header("User-Agent", "FlameInFreefall-Ingestor/1.0 (NASA Research PDF Downloader)")
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<ByteArray>()
            } else {
                println("    [HATA] PDF indirme HTTP hatası: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("    [HATA] PDF indirilemedi: ${e.message}")
            null
        }
    }

    private fun extractTextWithPdfBox(bytes: ByteArray): String? {
        return try {
            Loader.loadPDF(bytes).use { document ->
                if (document.isEncrypted) {
                    println("    [UYARI] PDF şifreli veya kilitli.")
                    return null
                }
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                }
                stripper.getText(document).trim()
            }
        } catch (e: Exception) {
            println("    [HATA] PDFBox metin ayrıştırma hatası: ${e.message}")
            null
        }
    }

    private suspend fun updateRecordManualReview(recordId: String, reason: String) {
        try {
            val payload = ExperimentUpdateTextPayload(
                rawText = null, // Metin çıkarılamadığında raw_text boş bırakılır
                needsManualReview = true
            )
            supabaseClient.client.postgrest["experiments"]
                .update(payload) {
                    filter { eq("id", recordId) }
                }
            println("    -> Supabase güncellendi (needs_manual_review = true, Neden: $reason)")
        } catch (e: Exception) {
            System.err.println("    [HATA] Supabase güncellenemedi: ${e.message}")
        }
    }

    private suspend fun updateRecordSuccess(
        recordId: String,
        text: String,
        material: String,
        oxygenConcentration: Double?
    ) {
        try {
            val payload = ExperimentUpdateTextPayload(
                rawText = text,
                needsManualReview = false,
                material = material,
                oxygenConcentration = oxygenConcentration
            )
            supabaseClient.client.postgrest["experiments"]
                .update(payload) {
                    filter { eq("id", recordId) }
                }
            println("    -> Supabase güncellendi (raw_text kaydedildi, needs_manual_review = false)")
        } catch (e: Exception) {
            System.err.println("    [HATA] Supabase güncellenemedi: ${e.message}")
        }
    }

    private fun extractOxygenConcentration(text: String): Double? {
        val regex = Regex("""(?i)(\d{1,2}(?:\.\d{1,2})?)\s*%\s*(?:O2|oxygen|oksijen)""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun refineMaterial(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("pmma") || lower.contains("polymethyl methacrylate") -> "PMMA (Acrylic)"
            lower.contains("heptane") -> "Heptane (Droplet)"
            lower.contains("methanol") -> "Methanol (Droplet)"
            lower.contains("cotton") || lower.contains("cotton fabric") -> "Cotton Fabric"
            lower.contains("silicone") -> "Silicone"
            lower.contains("nomex") -> "Nomex Fabric"
            lower.contains("polyethylene") -> "Polyethylene"
            else -> null
        }
    }
}
