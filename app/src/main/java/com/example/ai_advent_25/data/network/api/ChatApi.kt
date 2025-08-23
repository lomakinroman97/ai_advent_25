package com.example.ai_advent_25.data.network.api

import com.example.ai_advent_25.data.ChatRequest
import com.example.ai_advent_25.data.ChatResponse
import com.example.ai_advent_25.data.OpenAIChatRequest
import com.example.ai_advent_25.data.OpenAIChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ChatApi {
    @POST("foundationModels/v1/completion")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String,
        @Header("x-folder-id") folderId: String,
        @Body request: ChatRequest
    ): ChatResponse
}

interface DeepseekApi {
    @POST("v1/chat/completions")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIChatRequest
    ): OpenAIChatResponse
}
