package org.example.project.ingestion

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

class IngestionSupabaseClient(private val config: IngestorConfig) {

    init {
        require(config.isValid) {
            "Geçersiz yapılandırma: SUPABASE_URL ve SUPABASE_SERVICE_ROLE_KEY ortam değişkenleri tanımlanmalıdır."
        }
    }

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = config.supabaseUrl,
        supabaseKey = config.supabaseServiceRoleKey
    ) {
        install(Postgrest)
        install(Storage)
        install(Functions)
    }
}
