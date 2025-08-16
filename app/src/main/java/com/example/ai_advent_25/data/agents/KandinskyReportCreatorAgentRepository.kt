package com.example.ai_advent_25.data.agents

import android.util.Log
import com.example.ai_advent_25.data.ChatMessage
import com.example.ai_advent_25.data.ChatRequest
import com.example.ai_advent_25.data.KandinskyWorkData
import com.example.ai_advent_25.data.network.api.ChatApi
import com.example.ai_advent_25.data.network.NetworkConstants
import com.example.ai_advent_25.data.network.NetworkProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Агент №4: Создатель отчетов о работе Kandinsky
 * Анализирует собранные данные и создает подробный отчет через LLM
 */
class KandinskyReportCreatorAgentRepository(
    private val apiKey: String,
    private val networkProvider: NetworkProvider
) {

    private val chatApi: ChatApi by lazy {
        networkProvider.getChatApi()
    }
    
    companion object {
        private const val TAG = "KandinskyReportAgent"
    }
    
    /**
     * Создает отчет о работе Kandinsky на основе собранных данных
     */
    suspend fun createKandinskyReport(workDataList: List<KandinskyWorkData>): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "createKandinskyReport: Начинаем создание отчета")
            Log.d(TAG, "createKandinskyReport: Количество записей: ${workDataList.size}")
            
            if (workDataList.isEmpty()) {
                Log.d(TAG, "createKandinskyReport: Список данных пуст")
                return@withContext Result.success("Нет данных для анализа. Kandinsky еще не использовался в приложении.")
            }
            
            if (apiKey.isBlank()) {
                Log.e(TAG, "createKandinskyReport: API ключ пустой")
                return@withContext Result.failure(Exception("API ключ не может быть пустым"))
            }
            
            Log.d(TAG, "createKandinskyReport: API ключ получен, длина: ${apiKey.length}")
            
            val systemPrompt = createSystemPrompt(workDataList)
            Log.d(TAG, "createKandinskyReport: Системный промпт создан, длина: ${systemPrompt.length}")
            
            val messages = listOf(
                ChatMessage(role = "system", text = systemPrompt),
                ChatMessage(role = "user", text = """
                    Проанализируй данные о работе Kandinsky и создай краткий отчет.
                    
                    ВАЖНО: Твой ответ ДОЛЖЕН БЫТЬ ТОЛЬКО JSON объектом, без какого-либо текста до или после.
                    
                    Пример правильного ответа:
                    {
                      "totalRequests": 42,
                      "successfulRequests": 38,
                      "failedRequests": 4,
                      "cityStats": [
                        {"cityName": "Париж", "requestCount": 5},
                        {"cityName": "Рим", "requestCount": 3}
                      ],
                      "briefAnalysis": "Система работает стабильно, 90% успешных генераций"
                    }
                    
                    ОБЯЗАТЕЛЬНЫЕ ПОЛЯ:
                    - totalRequests: общее количество запросов (число)
                    - successfulRequests: количество успешных запросов (число)
                    - failedRequests: количество неудачных запросов (число)
                    - cityStats: массив городов с количеством запросов
                    - briefAnalysis: краткий анализ в одно предложение (строка)
                    
                    НЕ ДОБАВЛЯЙ никаких комментариев, объяснений или текста. ТОЛЬКО JSON.
                    Если не можешь создать JSON, верни пустую строку.
                """.trimIndent())
            )
            
            val request = ChatRequest(messages = messages)
            val authHeader = "Api-Key $apiKey"
            
            Log.d(TAG, "createKandinskyReport: Отправляем запрос к LLM...")
            Log.d(TAG, "createKandinskyReport: folderId: ${NetworkConstants.FOLDER_ID}")
            Log.d(TAG, "createKandinskyReport: authHeader: ${authHeader.take(20)}...")

            val response = chatApi.sendMessage(
                authorization = authHeader,
                folderId = NetworkConstants.FOLDER_ID,
                request = request
            )
            
            Log.d(TAG, "createKandinskyReport: Получен ответ от LLM")
            Log.d(TAG, "createKandinskyReport: response.result.alternatives.size: ${response.result.alternatives.size}")

            if (response.result.alternatives.isNotEmpty()) {
                val report = response.result.alternatives.first().message.text
                Log.d(TAG, "createKandinskyReport: Отчет создан успешно, длина: ${report.length}")
                Result.success(report)
            } else {
                Log.e(TAG, "createKandinskyReport: Пустой ответ от LLM")
                Result.failure(Exception("Пустой ответ от LLM"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createKandinskyReport: Произошла ошибка", e)
            Result.failure(e)
        }
    }
    
    /**
     * Создает системный промпт для анализа данных Kandinsky
     */
    private fun createSystemPrompt(workDataList: List<KandinskyWorkData>): String {
        val totalRequests = workDataList.size
        val successfulRequests = workDataList.count { it.status == "success" }
        val failedRequests = workDataList.count { it.status == "failed" }
        
        val cityStats = workDataList
            .groupBy { it.cityName }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
        
        val cityStatsText = cityStats.joinToString("\n") { "- ${it.first}: ${it.second} запросов" }
        
        return """
            Ты - эксперт по анализу данных. Проанализируй информацию о работе системы генерации изображений Kandinsky.
            
            ДАННЫЕ ДЛЯ АНАЛИЗА:
            - Общее количество запросов: $totalRequests
            - Успешных генераций: $successfulRequests
            - Неудачных генераций: $failedRequests
            
            Статистика по городам:
            $cityStatsText
            
            ВАЖНО: Ты ДОЛЖЕН вернуть ТОЛЬКО JSON объект без какого-либо текста.
            
            ТРЕБОВАНИЯ К JSON:
            1. totalRequests: общее количество запросов ($totalRequests)
            2. successfulRequests: количество успешных запросов ($successfulRequests)
            3. failedRequests: количество неудачных запросов ($failedRequests)
            4. cityStats: массив городов с количеством запросов
            5. briefAnalysis: краткий анализ в одно предложение
            
            НЕ ДОБАВЛЯЙ комментарии или объяснения. ТОЛЬКО JSON.
        """.trimIndent()
    }
}
