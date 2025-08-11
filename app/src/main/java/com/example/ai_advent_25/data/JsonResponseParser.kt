package com.example.ai_advent_25.data

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

object JsonResponseParser {

    fun parseResponse(response: String): ParseResult {
        return try {
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}')
            
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                val jsonString = response.substring(jsonStart, jsonEnd + 1)
                val structuredResponse = Gson().fromJson(jsonString, StructuredResponse::class.java)
                ParseResult.Success(structuredResponse, response)
            } else {
                ParseResult.Fallback(response)
            }
        } catch (e: JsonSyntaxException) {
            ParseResult.Fallback(response)
        }
    }

    fun createSystemPrompt(): String {
        return """
            Ты - полезный ассистент. Отвечай на вопросы пользователя в следующем JSON формате:
            
            {
                "content": "основной ответ на вопрос пользователя",
                "metadata": {
                    "confidence": 0.95,
                    "category": "категория ответа",
                    "tags": ["тег1", "тег2"]
                }
            }
            
            Поле content должно содержать полный и полезный ответ на вопрос.
            Поле metadata опционально - заполняй его, если можешь определить уверенность, категорию или теги.
            
            Всегда возвращай валидный JSON. Если не можешь структурировать ответ, используй простой текст.
        """.trimIndent()
    }
}

sealed class ParseResult {
    data class Success(
        val structuredResponse: StructuredResponse,
        val originalResponse: String
    ) : ParseResult()
    
    data class Fallback(
        val originalResponse: String
    ) : ParseResult()
}
