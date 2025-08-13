package com.example.ai_advent_25.data.agents

import com.example.ai_advent_25.data.network.api.ChatApi
import com.example.ai_advent_25.data.ChatMessage
import com.example.ai_advent_25.data.ChatRequest
import com.example.ai_advent_25.data.JsonResponseParser
import com.example.ai_advent_25.data.ParseResult
import com.example.ai_advent_25.data.TravelRecommendation
import com.example.ai_advent_25.data.network.NetworkConstants
import com.example.ai_advent_25.data.network.NetworkProvider

class ExpertReviewerAgentRepository(
    private val apiKey: String,
    private val networkProvider: NetworkProvider
) {

    private val chatApi: ChatApi by lazy {
        networkProvider.getChatApi()
    }

    suspend fun getExpertOpinion(travelRecommendation: TravelRecommendation, chatHistory: List<ChatMessage>): Result<ParseResult> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("API ключ не может быть пустым"))
        }

        return try {
            val expertContext = buildString {
                appendLine("Ты эксперт-аналитик туристических рекомендаций. Проанализируй следующие данные:")
                appendLine()
                appendLine("=== РЕКОМЕНДАЦИИ ОТ Трэвел ассистента===")
                appendLine("Резюме: ${travelRecommendation.summary}")
                appendLine("Общий бюджет: ${travelRecommendation.totalBudget}")
                appendLine()
                appendLine("=== ГОРОДА И РЕКОМЕНДАЦИИ ===")
                travelRecommendation.recommendations.forEachIndexed { index, city ->
                    appendLine("Город ${index + 1}: ${city.city}")
                    appendLine("Описание: ${city.description}")
                    appendLine("Достопримечательности: ${city.attractions.joinToString(", ")}")
                    appendLine("Стоимость: ${city.costs}")
                    appendLine("Лучшее время: ${city.bestTime}")
                    appendLine()
                }
                appendLine()
                appendLine("=== ИСТОРИЯ ДИАЛОГА С ПОЛЬЗОВАТЕЛЕМ ===")
                chatHistory.forEach { message ->
                    if (message.role != "system") {
                        appendLine("${message.role.uppercase()}: ${message.text}")
                    }
                }
                appendLine()
                appendLine("=== ЗАДАЧА ЭКСПЕРТА ===")
                appendLine("1. Проанализируй качество рекомендаций Трэвел ассистента")
                appendLine("2. Оцени валидность и точность предложений")
                appendLine("3. Дай дополнительные рекомендации и советы")
                appendLine("4. Проанализируй бюджет и время")
                appendLine("5. Оцени возможные риски")
                appendLine()
                appendLine("ОТВЕТЬ В JSON ФОРМАТЕ согласно системному промпту")
            }

            val expertMessages = listOf(
                ChatMessage(
                    role = "system",
                    text = createSystemPrompt(),
                ),
                ChatMessage(
                    role = "user",
                    text = expertContext
                )
            )

            val request = ChatRequest(messages = expertMessages)
            val authHeader = "Api-Key $apiKey"

            val response = chatApi.sendMessage(
                authorization = authHeader,
                folderId = NetworkConstants.FOLDER_ID,
                request = request
            )

            val content = response.result.alternatives.firstOrNull()?.message?.text
            content?.let { JsonResponseParser.parseResponse(it) }?.let { Result.success(it) }
                ?: Result.failure(Exception("Invalid response format from expert"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createSystemPrompt(): String {
        return """
            Ты - эксперт-аналитик туристических рекомендаций.

            ЗАДАЧА:
            Проанализируй рекомендации от Агента №1 и дай экспертное мнение.

            ФОРМАТ ОТВЕТА:
            {
                "type": "expert_opinion",
                "analysis": "Анализ качества рекомендаций Агента №1",
                "validation": "Оценка валидности и точности предложений",
                "additionalRecommendations": ["Рекомендация 1", "Рекомендация 2", "Рекомендация 3"],
                "travelTips": ["Совет 1", "Совет 2", "Совет 3"],
                "budgetAnalysis": "Анализ предложенного бюджета",
                "timingAnalysis": "Анализ выбранного времени года",
                "riskAssessment": "Оценка возможных рисков"
            }

            ТРЕБОВАНИЯ:
            - Отвечай только в JSON формате
            - Не используй markdown-разметку
            - Не добавляй текст вне JSON
            - Все поля обязательны
        """.trimIndent()
    }
}
