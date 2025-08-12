package com.example.ai_advent_25.data

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

object JsonResponseParser {

    fun parseResponse(response: String): ParseResult {
        return try {
            // Ищем JSON объекты более точно
            val jsonObjects = findJsonObjects(response)
            
            if (jsonObjects.isNotEmpty()) {
                // Пытаемся найти первый валидный JSON с типом
                for (jsonString in jsonObjects) {
                    try {
                        if (jsonString.contains("\"type\"")) {
                            when {
                                jsonString.contains("\"type\":\"question\"") -> {
                                    val questionResponse = Gson().fromJson(jsonString, AgentResponse.Question::class.java)
                                    return ParseResult.QuestionResponse(questionResponse, response)
                                }
                                jsonString.contains("\"type\":\"recommendations\"") -> {
                                    val recommendationsResponse = Gson().fromJson(jsonString, AgentResponse.Recommendations::class.java)
                                    return ParseResult.RecommendationsResponse(recommendationsResponse, response)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Продолжаем поиск следующего JSON
                        continue
                    }
                }
            }

            ParseResult.RegularResponse(response)
        } catch (e: Exception) {
            ParseResult.RegularResponse(response)
        }
    }

    private fun findJsonObjects(response: String): List<String> {
        val jsonObjects = mutableListOf<String>()
        var start = 0
        
        while (start < response.length) {
            val jsonStart = response.indexOf('{', start)
            if (jsonStart == -1) break
            
            var braceCount = 0
            var jsonEnd = -1
            
            for (i in jsonStart until response.length) {
                when (response[i]) {
                    '{' -> braceCount++
                    '}' -> {
                        braceCount--
                        if (braceCount == 0) {
                            jsonEnd = i
                            break
                        }
                    }
                }
            }
            
            if (jsonEnd != -1) {
                val jsonString = response.substring(jsonStart, jsonEnd + 1)
                if (jsonString.contains("\"type\"")) {
                    jsonObjects.add(jsonString)
                }
                start = jsonEnd + 1
            } else {
                break
            }
        }
        
        return jsonObjects
    }

    fun createSystemPrompt(): String {
        return """
            Ты - туристический агент. Твоя задача: задать 4 вопроса и дать рекомендации.

            ПРАВИЛА (НЕ ИЗМЕНЯЙ):
            1. Задаешь ТОЛЬКО ОДИН вопрос за раз
            2. После ответа пользователя задаешь следующий вопрос
            3. Всего 4 вопроса
            4. После 4-го ответа даешь рекомендации
            5. НИКОГДА не отвечаешь за пользователя
            6. НИКОГДА не задаешь несколько вопросов сразу
            7. ВСЕГДА отвечаешь в JSON формате

            ПОРЯДОК ВОПРОСОВ:
            Вопрос 1: "Сколько человек планирует путешествие?"
            Вопрос 2: "Какой бюджет планируете на поездку?"
            Вопрос 3: "Какое время года предпочитаете?"
            Вопрос 4: "Какие достопримечательности вас интересуют больше всего?"

            ФОРМАТЫ (ИСПОЛЬЗУЙ ТОЧНО):

            Для вопроса:
            {"type":"question","questionNumber":1,"nextQuestion":"Сколько человек планирует путешествие?","collectedInfo":[],"remainingQuestions":3}

            Для рекомендаций:
            {"type":"recommendations","summary":"Резюме","recommendations":[{"city":"Город","description":"Описание","attractions":["Дост1","Дост2"],"costs":"1000","bestTime":"Лето"}],"totalBudget":"1000"}

            ВАЖНО: 
            - Отвечай ТОЛЬКО в JSON формате
            - Никаких слов вне JSON
            - Никаких дополнительных символов
            - Только один JSON объект за раз
        """.trimIndent()
    }

    // Функция для тестирования парсинга
    fun testParsing() {
        val questionJson = """{"type":"question","questionNumber":1,"nextQuestion":"Сколько человек планирует путешествие?","collectedInfo":[],"remainingQuestions":3}"""
        val questionResult = parseResponse(questionJson)
        println("Question parsing result: $questionResult")

        val recommendationsJson = """{"type":"recommendations","summary":"Тестовое резюме","recommendations":[{"city":"Тестовый город 1","description":"Тестовое описание 1","attractions":["Дост1","Дост2","Дост3"],"costs":"1000","bestTime":"Лето"},{"city":"Тестовый город 2","description":"Тестовое описание 2","attractions":["Дост1","Дост2","Дост3"],"costs":"1200","bestTime":"Осень"},{"city":"Тестовый город 3","description":"Тестовое описание 3","attractions":["Дост1","Дост2","Дост3"],"costs":"800","bestTime":"Весна"}],"totalBudget":"3000"}"""
        val recommendationsResult = parseResponse(recommendationsJson)
        println("Recommendations parsing result: $recommendationsResult")
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

    data class RegularResponse(
        val response: String
    ) : ParseResult()
}
