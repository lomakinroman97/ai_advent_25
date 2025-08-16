package com.example.ai_advent_25.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.example.ai_advent_25.data.CityStat
import com.example.ai_advent_25.data.SimpleKandinskyReport

object JsonResponseParser {

    fun parseResponse(response: String): ParseResult? {
        return try {
            // Очищаем markdown и нормализуем ответ
            val cleanedResponse = cleanMarkdownResponse(response)
            
            // Ищем JSON объект
            val jsonObject = findJsonObject(cleanedResponse)
            if (jsonObject != null) {
                // Определяем тип через regex
                val typePattern = Regex("\"type\"\\s*:\\s*\"([^\"]+)\"")
                val typeMatch = typePattern.find(jsonObject)
                
                if (typeMatch != null) {
                    val actualType = typeMatch.groupValues[1]
                    
                    return when (actualType) {
                        "question" -> {
                            val questionResponse = Gson().fromJson(jsonObject, AgentResponse.Question::class.java)
                            ParseResult.QuestionResponse(questionResponse, response)
                        }
                        "recommendations" -> {
                            val recommendationsResponse = Gson().fromJson(jsonObject, AgentResponse.Recommendations::class.java)
                            ParseResult.RecommendationsResponse(recommendationsResponse, response)
                        }
                        "expert_opinion" -> {
                            val expertOpinion = Gson().fromJson(jsonObject, ExpertOpinion::class.java)
                            ParseResult.ExpertOpinionResponse(expertOpinion, response)
                        }
                        else -> null
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanMarkdownResponse(response: String): String {
        return response.replace(Regex("```[\\w]*\\s*"), "").replace("```", "").trim()
    }

    private fun findJsonObject(response: String): String? {
        val start = response.indexOf('{')
        if (start == -1) return null
        
        var braceCount = 0
        var inString = false
        var escapeNext = false
        
        for (i in start until response.length) {
            val char = response[i]
            
            if (escapeNext) {
                escapeNext = false
                continue
            }
            
            when (char) {
                '\\' -> escapeNext = true
                '"' -> inString = !inString
                '{' -> if (!inString) braceCount++
                '}' -> if (!inString) {
                    braceCount--
                    if (braceCount == 0) {
                        return response.substring(start, i + 1)
                    }
                }
            }
        }
        
        return null
    }

    /**
     * Парсит структурированный отчет о работе Kandinsky
     */
    fun parseKandinskyReport(jsonResponse: String): Result<KandinskyStructuredReport> {
        return try {
            val report = Gson().fromJson(jsonResponse, KandinskyStructuredReport::class.java)
            Result.success(report)
        } catch (e: Exception) {
            Log.e("JsonResponseParser", "Ошибка парсинга отчета Kandinsky", e)
            Result.failure(e)
        }
    }

    /**
     * Парсит упрощенный отчет о работе Kandinsky от Агента №4
     */
    fun parseSimpleKandinskyReport(jsonResponse: String): Result<SimpleKandinskyReport> {
        return try {
            Log.d("JsonResponseParser", "Начинаем парсинг ответа от Агента №4")
            Log.d("JsonResponseParser", "Длина ответа: ${jsonResponse.length}")
            Log.d("JsonResponseParser", "Первые 200 символов: ${jsonResponse.take(200)}")
            
            // Пытаемся найти JSON в ответе
            val cleanResponse = findJsonObject(jsonResponse)
            Log.d("JsonResponseParser", "Очищенный ответ: $cleanResponse")
            
            val report = Gson().fromJson(cleanResponse, SimpleKandinskyReport::class.java)
            Log.d("JsonResponseParser", "Отчет успешно распарсен: $report")
            Result.success(report)
        } catch (e: Exception) {
            Log.e("JsonResponseParser", "Ошибка парсинга упрощенного отчета Kandinsky", e)
            Log.e("JsonResponseParser", "Полный ответ: $jsonResponse")
            Result.failure(e)
        }
    }

    /**
     * Создает простой отчет на основе базовых данных если JSON не найден
     */
    fun createFallbackReport(workDataList: List<KandinskyWorkData>): SimpleKandinskyReport {
        val totalRequests = workDataList.size
        val successfulRequests = workDataList.count { it.status == "success" }
        val failedRequests = workDataList.count { it.status == "failed" }
        
        val cityStats = workDataList
            .groupBy { it.cityName }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { CityStat(it.first, it.second) }
        
        val briefAnalysis = when {
            workDataList.isEmpty() -> "Нет данных для анализа"
            successfulRequests == totalRequests -> "Все запросы выполнены успешно"
            failedRequests > successfulRequests -> "Много ошибок, требуется диагностика"
            else -> "Система работает стабильно"
        }
        
        return SimpleKandinskyReport(
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            cityStats = cityStats,
            briefAnalysis = briefAnalysis
        )
    }
}

sealed class ParseResult {
    data class QuestionResponse(
        val question: AgentResponse.Question,
        val originalResponse: String
    ) : ParseResult()

    data class RecommendationsResponse(
        val recommendations: AgentResponse.Recommendations,
        val originalResponse: String
    ) : ParseResult()

    data class ExpertOpinionResponse(
        val expertOpinion: ExpertOpinion,
        val originalResponse: String
    ) : ParseResult()
}
