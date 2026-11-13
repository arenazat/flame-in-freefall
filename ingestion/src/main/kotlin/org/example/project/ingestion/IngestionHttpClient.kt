package org.example.project.ingestion

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

object IngestionHttpClient {
    /**
     * Ktor Client configured with CIO engine for high-performance HTTP requests
     * targeting NASA NTRS and PSI API endpoints.
     */
    val client: HttpClient by lazy {
        HttpClient(CIO) {
            expectSuccess = false
        }
    }
}
