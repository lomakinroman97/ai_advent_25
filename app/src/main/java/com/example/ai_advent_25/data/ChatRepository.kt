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
    
    suspend fun sendMessage(messages: List<ChatMessage>): Result<ParseResult> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("API ключ не может быть пустым"))
        }
        
        return try {
            val messagesWithSystem = if (messages.none { it.role == "system" }) {
                listOf(
                    ChatMessage(
                        role = "system",
                        text = JsonResponseParser.createSystemPrompt()
                    )
                ) + messages
            } else {
                messages
            }
            
            val request = ChatRequest(messages = messagesWithSystem)
            
            val authHeader = "Api-Key $apiKey"
            
            val response = chatApi.sendMessage(
                authorization = authHeader,
                folderId = FOLDER_ID,
                request = request
            )
            
            val content = response.result.alternatives.firstOrNull()?.message?.text
            if (content != null) {
                val parseResult = JsonResponseParser.parseResponse(content)
                Result.success(parseResult)
            } else {
                Result.failure(Exception("Empty response from API"))
            }
        } catch (e: Exception) {
            when (e) {
                is retrofit2.HttpException -> {
                    when (e.code()) {
                        401 -> {
                            Result.failure(Exception("HTTP 401: Неверный API ключ или Folder ID. Проверьте настройки."))
                        }
                        403 -> {
                            Result.failure(Exception("HTTP 403: Доступ запрещен. Проверьте права доступа."))
                        }
                        429 -> {
                            Result.failure(Exception("HTTP 429: Превышен лимит запросов. Попробуйте позже."))
                        }
                        else -> {
                            Result.failure(Exception("HTTP ошибка ${e.code()}: ${e.message()}"))
                        }
                    }
                }
                else -> {
                    Result.failure(e)
                }
            }
        }
    }
}
