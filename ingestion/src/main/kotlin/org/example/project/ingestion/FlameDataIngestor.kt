package org.example.project.ingestion

import kotlinx.coroutines.runBlocking
import org.example.project.ingestion.commands.EmbedCommand
import org.example.project.ingestion.commands.ExtractCommand
import org.example.project.ingestion.commands.FetchCommand
import org.example.project.ingestion.commands.SummarizeCommand

fun main(args: Array<String>) = runBlocking {
    printBanner()

    val command = args.firstOrNull()?.lowercase() ?: "help"
    val commandArgs = if (args.size > 1) args.drop(1) else emptyList()

    if (command == "help" || command == "--help" || command == "-h") {
        printUsage()
        return@runBlocking
    }

    val config = IngestorConfig.fromEnvironment()
    if (!config.isValid) {
        System.err.println(
            """
            [UYARI] Ortam değişkenleri eksik!
            Bu araç Supabase veritabanına gerçek yazma işlemi yapabilmek için 'service_role' yetkisine ihtiyaç duyar.
            Lütfen aşağıdaki ortam değişkenlerini tanımlayınız:
              - SUPABASE_URL (ör. https://xyz.supabase.co)
              - SUPABASE_SERVICE_ROLE_KEY (veya SUPABASE_KEY)
            
            Örnek çalıştırma:
              PowerShell:
                ${'$'}env:SUPABASE_URL="https://your-project.supabase.co"
                ${'$'}env:SUPABASE_SERVICE_ROLE_KEY="your-service-role-key"
                .\gradlew.bat :ingestion:run --args="fetch"
            """.trimIndent()
        )
        return@runBlocking
    }

    val supabaseClient = IngestionSupabaseClient(config)
    val httpClient = IngestionHttpClient.client

    when (command) {
        "fetch" -> {
            val fetchCommand = FetchCommand(httpClient, supabaseClient)
            fetchCommand.execute(commandArgs)
        }

        "extract" -> {
            val extractCommand = ExtractCommand(httpClient, supabaseClient)
            extractCommand.execute(commandArgs)
        }

        "embed" -> {
            val embedCommand = EmbedCommand(httpClient, supabaseClient, config)
            embedCommand.execute(commandArgs)
        }

        "summarize" -> {
            val summarizeCommand = SummarizeCommand(httpClient, supabaseClient, config)
            summarizeCommand.execute(commandArgs)
        }

        "status" -> {
            println("[STATUS] Supabase bağlantısı hazır.")
            println("         URL: ${config.supabaseUrl}")
            println("         Service Role Yetkisi: Tanımlı")
        }

        else -> {
            System.err.println("[HATA] Bilinmeyen komut: '$command'")
            printUsage()
        }
    }
}

private fun printBanner() {
    println(
        """
        ======================================================================
        FLAME DATA INGESTOR — NASA MİKRO-YERÇEKİMİ YANMA VERİ TOPLAMA ARACI
        Kaynaklar: NASA Physical Sciences Informatics (PSI) & NTRS
        Hedef: Supabase (PostgreSQL + pgvector)
        ======================================================================
        """.trimIndent()
    )
}

private fun printUsage() {
    println(
        """
        KULLANIM:
          .\gradlew.bat :ingestion:run --args="<komut> [argümanlar]"

        ALT KOMUTLAR:
          fetch [SERI]   : NASA NTRS / PSI üzerinden ham deney kayıtlarını çeker (FLEX, SAFFIRE, BASS).
          extract        : Ham metinlerden malzeme, yerçekimi ve oksijen parametrelerini ayıklar.
          embed          : Deney metin parçalarını 1536-boyutlu vektörlere dönüştürüp 'experiment_chunks'a yazar.
          summarize      : AI ile özet ve 0-100 risk skoru hesaplayıp 'experiment_summaries'e kaydeder.
          status         : Supabase bağlantı durumunu ve ortam değişkeni yapılandırmasını kontrol eder.
          help           : Bu yardım mesajını görüntüler.
        """.trimIndent()
    )
}
