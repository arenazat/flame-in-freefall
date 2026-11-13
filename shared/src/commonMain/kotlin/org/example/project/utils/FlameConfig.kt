package org.example.project.utils

data class FlameConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String
) {
    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() &&
                supabaseAnonKey.isNotBlank() &&
                !supabaseUrl.contains("placeholder")

    companion object {
        fun default(): FlameConfig {
            val url = FlameBuildConfig.SUPABASE_URL.trim()
            val key = FlameBuildConfig.SUPABASE_ANON_KEY.trim()
            return FlameConfig(
                supabaseUrl = url.ifEmpty { "https://flameinfreefall.supabase.co" },
                supabaseAnonKey = key.ifEmpty { "anon-key-placeholder" }
            )
        }
    }
}
