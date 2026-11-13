package org.example.project.data.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import org.example.project.utils.FlameConfig

class SupabaseProvider(val config: FlameConfig) {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = config.supabaseUrl,
        supabaseKey = config.supabaseAnonKey
    ) {
        install(Postgrest)
        install(Storage)
        install(Functions)
    }

    val isInitialized: Boolean = true
    val isReadyForQueries: Boolean = config.isConfigured
}
