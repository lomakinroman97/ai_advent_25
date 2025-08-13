package com.example.ai_advent_25.data

import com.google.gson.Gson

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
