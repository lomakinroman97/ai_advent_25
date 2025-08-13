package com.example.ai_advent_25.data.agents

import com.example.ai_advent_25.data.network.api.ChatApi
import com.example.ai_advent_25.data.ChatMessage
import com.example.ai_advent_25.data.ChatRequest
import com.example.ai_advent_25.data.JsonResponseParser
import com.example.ai_advent_25.data.ParseResult
import com.example.ai_advent_25.data.network.NetworkConstants
import com.example.ai_advent_25.data.network.NetworkProvider

class TravelAssistAgentRepository(
    private val apiKey: String,
    private val networkProvider: NetworkProvider
) {

    private val chatApi: ChatApi by lazy {
        networkProvider.getChatApi()
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
                        text = createSystemPrompt(),
                    )
                ) + messages
            } else {
                messages
            }

            val request = ChatRequest(messages = messagesWithSystem)
            val authHeader = "Api-Key $apiKey"

            val response = chatApi.sendMessage(
                authorization = authHeader,
                folderId = NetworkConstants.FOLDER_ID,
                request = request
            )

            val content = response.result.alternatives.firstOrNull()?.message?.text
            content?.let { JsonResponseParser.parseResponse(it) }?.let { Result.success(it) }
                ?: Result.failure(Exception("Invalid response format from API"))
        } catch (e: Exception) {
            when (e) {
                is retrofit2.HttpException -> {
                    val errorMessage = when (e.code()) {
                        401 -> "HTTP 401: Неверный API ключ или Folder ID. Проверьте настройки."
                        403 -> "HTTP 403: Доступ запрещен. Проверьте права доступа."
                        429 -> "HTTP 429: Превышен лимит запросов. Попробуйте позже."
                        408 -> "HTTP 408: Timeout запроса. Попробуйте еще раз."
                        504 -> "HTTP 504: Gateway Timeout. Сервер не отвечает."
                        else -> "HTTP ошибка ${e.code()}: ${e.message()}"
                    }
                    Result.failure(Exception(errorMessage))
                }
                is java.net.SocketTimeoutException -> {
                    Result.failure(Exception("Timeout соединения. Проверьте интернет-соединение и попробуйте еще раз."))
                }
                is java.net.UnknownHostException -> {
                    Result.failure(Exception("Не удается подключиться к серверу. Проверьте интернет-соединение."))
                }
                else -> Result.failure(e)
            }
        }
    }

    private fun createSystemPrompt(): String {
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
}