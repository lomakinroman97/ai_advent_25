package com.example.ai_advent_25.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val BASE_URL = "https://llm.api.cloud.yandex.net/"
private const val FOLDER_ID = "b1gp9fidpabmov8j1rid"


class ChatRepository(private val apiKey: String) {
    
    private val chatApi: ChatApi by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        retrofit.create(ChatApi::class.java)
    }
    
    suspend fun sendMessage(messages: List<ChatMessage>): Result<String> {
        return try {
            val request = ChatRequest(messages = messages)
            
            val response = chatApi.sendMessage(
                authorization = "Api-Key $apiKey",
                folderId = FOLDER_ID,
                request = request
            )
            
            val content = response.result.alternatives.firstOrNull()?.message?.text
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("Empty response from API"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
