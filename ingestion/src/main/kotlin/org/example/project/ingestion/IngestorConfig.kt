package org.example.project.ingestion

import java.io.File
import java.util.Properties

data class IngestorConfig(
    val supabaseUrl: String,
    val supabaseServiceRoleKey: String,
    val openaiApiKey: String = "",
    val geminiApiKey: String = "",
    val geminiChatModel: String = "gemini-flash-latest"
) {
    /** Gemini anahtarı varsa Gemini kullanılır; yoksa OpenAI. */
    val useGemini: Boolean get() = geminiApiKey.isNotBlank()
    val hasAiKey: Boolean get() = geminiApiKey.isNotBlank() || openaiApiKey.isNotBlank()

    companion object {
        fun fromEnvironment(): IngestorConfig {
            val localProps = Properties().apply {
                val candidateFiles = listOf(
                    File("local.properties"),
                    File("../local.properties"),
                    File("../../local.properties")
                )
                candidateFiles.firstOrNull { it.exists() }?.let { file ->
                    file.inputStream().use { load(it) }
                }
            }

            val rawUrl = System.getenv("SUPABASE_URL")
                ?: System.getProperty("SUPABASE_URL")
                ?: localProps.getProperty("SUPABASE_URL")
                ?: ""

            val normalizedUrl = rawUrl.trim()
                .removeSuffix("/")
                .removeSuffix("/rest/v1")
                .removeSuffix("/")

            val serviceRoleKey = System.getenv("SUPABASE_SERVICE_ROLE_KEY")
                ?: System.getProperty("SUPABASE_SERVICE_ROLE_KEY")
                ?: System.getenv("SUPABASE_KEY")
                ?: localProps.getProperty("SUPABASE_SERVICE_ROLE_KEY")
                ?: ""

            val openaiKey = System.getenv("OPENAI_API_KEY")
                ?: System.getenv("EMBEDDING_API_KEY")
                ?: System.getProperty("OPENAI_API_KEY")
                ?: localProps.getProperty("OPENAI_API_KEY")
                ?: ""

            val geminiKey = System.getenv("GEMINI_API_KEY")
                ?: System.getenv("GOOGLE_API_KEY")
                ?: System.getProperty("GEMINI_API_KEY")
                ?: localProps.getProperty("GEMINI_API_KEY")
                ?: ""

            val geminiModel = System.getenv("GEMINI_CHAT_MODEL")
                ?: localProps.getProperty("GEMINI_CHAT_MODEL")
                ?: "gemini-flash-latest"

            return IngestorConfig(
                supabaseUrl = normalizedUrl,
                supabaseServiceRoleKey = serviceRoleKey.trim(),
                openaiApiKey = openaiKey.trim(),
                geminiApiKey = geminiKey.trim(),
                geminiChatModel = geminiModel.trim()
            )
        }
    }

    val isValid: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseServiceRoleKey.isNotBlank()
}
