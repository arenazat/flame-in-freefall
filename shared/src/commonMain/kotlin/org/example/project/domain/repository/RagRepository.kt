package org.example.project.domain.repository

import org.example.project.models.RagAnswer

interface RagRepository {
    /**
     * Supabase "ask" Edge Function'ına Ktor client üzerinden istek atar.
     * Soru embedding'i, pgvector benzerlik araması ve gerçek deney kaynakları ile
     * LLM tarafından üretilen yanıtı döner.
     */
    suspend fun askQuestion(question: String): Result<RagAnswer>
}
