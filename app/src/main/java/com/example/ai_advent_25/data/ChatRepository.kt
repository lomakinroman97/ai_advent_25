package com.example.ai_advent_25.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.util.Log

private const val BASE_URL = "https://llm.api.cloud.yandex.net/"
private const val FOLDER_ID = "b1gp9fidpabmov8j1rid"
private const val TAG = "ChatRepository"

class ChatRepository(private val apiKey: String) {
    
    private val chatApi: ChatApi by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)  // Увеличиваем timeout для подключения
            .readTimeout(120, TimeUnit.SECONDS)    // Увеличиваем timeout для чтения
            .writeTimeout(60, TimeUnit.SECONDS)    // Увеличиваем timeout для записи
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
            Log.e(TAG, "API ключ пустой")
            return Result.failure(Exception("API ключ не может быть пустым"))
        }
        
        Log.d(TAG, "Отправляем сообщение. Количество сообщений: ${messages.size}")
        Log.d(TAG, "API ключ: ${apiKey.take(10)}...")
        
        return try {
            val messagesWithSystem = if (messages.none { it.role == "system" }) {
                Log.d(TAG, "Добавляем системный промпт")
                listOf(
                    ChatMessage(
                        role = "system",
                        text = JsonResponseParser.createSystemPrompt()
                    )
                ) + messages
            } else {
                Log.d(TAG, "Системный промпт уже есть")
                messages
            }
            
            Log.d(TAG, "Итоговое количество сообщений: ${messagesWithSystem.size}")
            
            val request = ChatRequest(messages = messagesWithSystem)
            
            val authHeader = "Api-Key $apiKey"
            Log.d(TAG, "Отправляем запрос к API...")
            
            val response = chatApi.sendMessage(
                authorization = authHeader,
                folderId = FOLDER_ID,
                request = request
            )
            
            Log.d(TAG, "Получен ответ от API")
            
            val content = response.result.alternatives.firstOrNull()?.message?.text
            if (content != null) {
                Log.d(TAG, "Длина ответа: ${content.length}")
                Log.d(TAG, "Ответ: ${content.take(200)}...")
                
                val parseResult = JsonResponseParser.parseResponse(content)
                Log.d(TAG, "Ответ успешно распарсен")
                Result.success(parseResult)
            } else {
                Log.e(TAG, "Пустой ответ от API")
                Result.failure(Exception("Empty response from API"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отправке сообщения", e)
            when (e) {
                is retrofit2.HttpException -> {
                    Log.e(TAG, "HTTP ошибка: ${e.code()}")
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
                        408 -> {
                            Result.failure(Exception("HTTP 408: Timeout запроса. Попробуйте еще раз."))
                        }
                        504 -> {
                            Result.failure(Exception("HTTP 504: Gateway Timeout. Сервер не отвечает."))
                        }
                        else -> {
                            Result.failure(Exception("HTTP ошибка ${e.code()}: ${e.message()}"))
                        }
                    }
                }
                is java.net.SocketTimeoutException -> {
                    Log.e(TAG, "Socket timeout")
                    Result.failure(Exception("Timeout соединения. Проверьте интернет-соединение и попробуйте еще раз."))
                }
                is java.net.UnknownHostException -> {
                    Log.e(TAG, "Unknown host")
                    Result.failure(Exception("Не удается подключиться к серверу. Проверьте интернет-соединение."))
                }
                else -> {
                    Result.failure(e)
                }
            }
        }
    }
}
