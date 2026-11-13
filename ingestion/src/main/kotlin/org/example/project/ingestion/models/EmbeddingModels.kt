package org.example.project.ingestion.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiEmbeddingRequest(
    val model: String = "text-embedding-3-small",
    val input: List<String>
)

@Serializable
data class OpenAiEmbeddingResponse(
    val data: List<OpenAiEmbeddingData>? = null
)

@Serializable
data class OpenAiEmbeddingData(
    val index: Int = 0,
    val embedding: List<Float>
)

@Serializable
data class ExperimentChunkInsertDto(
    @SerialName("experiment_id") val experimentId: String,
    @SerialName("chunk_text") val chunkText: String,
    @SerialName("embedding") val embedding: List<Float>
)

@Serializable
data class ExistingChunkReference(
    @SerialName("experiment_id") val experimentId: String
)
