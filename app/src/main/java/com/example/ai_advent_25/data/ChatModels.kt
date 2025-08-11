package com.example.ai_advent_25.data

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    val role: String,
    val text: String,
    val systemContent: String? = null
)

data class ChatRequest(
    @SerializedName("modelUri")
    val modelUri: String = "gpt://b1gp9fidpabmov8j1rid/yandexgpt-lite",
    @SerializedName("messages")
    val messages: List<ChatMessage>
)

data class ChatResponse(
    val result: ChatResult
)

data class ChatResult(
    val alternatives: List<Alternative>,
    val usage: Usage
)

data class Alternative(
    val message: ChatMessage,
    val status: String
)

data class Usage(
    @SerializedName("inputTextTokens")
    val inputTextTokens: Int,
    @SerializedName("completionTokens")
    val completionTokens: Int,
    @SerializedName("totalTokens")
    val totalTokens: Int
)

data class ChatUiMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: ResponseMetadata? = null,
    val originalJson: String? = null
)

// Модели для структурированного ответа
data class StructuredResponse(
    val content: String,
    val metadata: ResponseMetadata? = null
)

data class ResponseMetadata(
    val confidence: Double? = null,
    val category: String? = null,
    val tags: List<String>? = null
)
