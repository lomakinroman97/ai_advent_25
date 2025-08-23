package com.example.ai_advent_25.data.agents

import com.example.ai_advent_25.data.ChatMessage
import com.example.ai_advent_25.data.ChatRequest
import com.example.ai_advent_25.data.OpenAIChatMessage
import com.example.ai_advent_25.data.OpenAIChatRequest
import com.example.ai_advent_25.data.JsonResponseParser
import com.example.ai_advent_25.data.LLMProvider
import com.example.ai_advent_25.data.ParseResult
import com.example.ai_advent_25.data.network.NetworkConstants
import com.example.ai_advent_25.data.network.NetworkProvider
import com.example.ai_advent_25.data.network.api.ChatApi
import com.example.ai_advent_25.data.network.api.DeepseekApi

class TravelAssistAgentRepository(
    private val yandexApiKey: String,
    private val deepseekApiKey: String,
    private val networkProvider: NetworkProvider,
    private val selectedLLM: LLMProvider = LLMProvider.YANDEX_GPT
) {

    private val chatApi: ChatApi by lazy {
        networkProvider.getChatApi()
    }
    
    private val deepseekApi: DeepseekApi by lazy {
        networkProvider.getDeepseekApi()
    }

    suspend fun sendMessage(messages: List<ChatMessage>): Result<ParseResult> {
        android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessage вызван с выбранной моделью: $selectedLLM")
        return when (selectedLLM) {
            LLMProvider.YANDEX_GPT -> {
                android.util.Log.d("Debug77", "TravelAssistAgentRepository: Используем YandexGPT API")
                sendMessageToYandex(messages)
            }
            LLMProvider.DEEPSEEK_R1 -> {
                android.util.Log.d("Debug77", "TravelAssistAgentRepository: Используем DeepSeek R1 API")
                sendMessageToDeepseek(messages)
            }
        }
    }
    
    private suspend fun sendMessageToYandex(messages: List<ChatMessage>): Result<ParseResult> {
        android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessageToYandex - начало отправки")
        if (yandexApiKey.isBlank()) {
            android.util.Log.e("Debug77", "TravelAssistAgentRepository: sendMessageToYandex - API ключ YandexGPT пустой")
            return Result.failure(Exception("API ключ YandexGPT не может быть пустым"))
        }

        android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessageToYandex - API ключ найден, отправляем запрос")
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
            val authHeader = "Api-Key $yandexApiKey"

            android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessageToYandex - вызываем YandexGPT API")
            val response = chatApi.sendMessage(
                authorization = authHeader,
                folderId = NetworkConstants.FOLDER_ID,
                request = request
            )

            val content = response.result.alternatives.firstOrNull()?.message?.text
            android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessageToYandex - получен ответ от YandexGPT API")
            content?.let { responseContent: String -> 
                JsonResponseParser.parseResponse(responseContent)?.let { parseResult: ParseResult -> 
                    android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessageToYandex - успешно распарсен ответ")
                    Result.success(parseResult) 
                }
            } ?: Result.failure(Exception("Invalid response format from YandexGPT API"))
        } catch (e: Exception) {
            android.util.Log.e("Debug77", "TravelAssistAgentRepository: sendMessageToYandex - ошибка: ${e.message}")
            handleYandexError(e)
        }
    }
    
    private suspend fun sendMessageToDeepseek(messages: List<ChatMessage>): Result<ParseResult> {
        android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessageToDeepseek - начало отправки")
        if (deepseekApiKey.isBlank()) {
            android.util.Log.e("Debug77", "TravelAssistAgentRepository: sendMessageToDeepseek - API ключ DeepSeek пустой")
            return Result.failure(Exception("API ключ DeepSeek не может быть пустым"))
        }

        android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessageToDeepseek - API ключ найден, отправляем запрос")
        return try {
            val messagesWithSystem = if (messages.none { it.role == "system" }) {
                listOf(
                    OpenAIChatMessage(
                        role = "system",
                        content = createSystemPrompt(),
                    )
                ) + messages.map { chatMessage -> 
                    OpenAIChatMessage(role = chatMessage.role, content = chatMessage.text) 
                }
            } else {
                messages.map { chatMessage -> 
                    OpenAIChatMessage(role = chatMessage.role, content = chatMessage.text) 
                }
            }

            val request = OpenAIChatRequest(messages = messagesWithSystem)
            val authHeader = "Bearer $deepseekApiKey"

            android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessageToDeepseek - вызываем DeepSeek API")
            val response = deepseekApi.sendMessage(
                authorization = authHeader,
                request = request
            )

            val content = response.choices.firstOrNull()?.message?.content
            android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessageToDeepseek - получен ответ от DeepSeek API")
            content?.let { responseContent: String -> 
                JsonResponseParser.parseResponse(responseContent)?.let { parseResult: ParseResult -> 
                    android.util.Log.d("Debug77", "TravelAssistAgentRepository: sendMessageToDeepseek - успешно распарсен ответ")
                    Result.success(parseResult) 
                }
            } ?: Result.failure(Exception("Invalid response format from DeepSeek API"))
        } catch (e: Exception) {
            android.util.Log.e("Debug77", "TravelAssistAgentRepository: sendMessageToDeepseek - ошибка: ${e.message}")
            handleDeepseekError(e)
        }
    }
    
    private fun handleYandexError(e: Exception): Result<ParseResult> {
        return when (e) {
            is retrofit2.HttpException -> {
                val errorMessage = when (e.code()) {
                    401 -> "HTTP 401: Неверный API ключ YandexGPT или Folder ID. Проверьте настройки."
                    403 -> "HTTP 403: Доступ запрещен. Проверьте права доступа."
                    429 -> "HTTP 429: Превышен лимит запросов. Попробуйте позже."
                    408 -> "HTTP 408: Timeout запроса. Попробуйте еще раз."
                    504 -> "HTTP 504: Gateway Timeout. Сервер не отвечает."
                    else -> "HTTP ошибка ${e.code()}: ${e.message}"
                }
                Result.failure(Exception(errorMessage))
            }
            is java.net.SocketTimeoutException -> {
                Result.failure(Exception("Timeout соединения с YandexGPT. Проверьте интернет-соединение и попробуйте еще раз."))
            }
            is java.net.UnknownHostException -> {
                Result.failure(Exception("Не удается подключиться к серверу YandexGPT. Проверьте интернет-соединение."))
            }
            else -> Result.failure(e)
        }
    }
    
    private fun handleDeepseekError(e: Exception): Result<ParseResult> {
        return when (e) {
            is retrofit2.HttpException -> {
                val errorMessage = when (e.code()) {
                    401 -> "HTTP 401: Неверный API ключ DeepSeek. Проверьте настройки."
                    403 -> "HTTP 403: Доступ запрещен. Проверьте права доступа."
                    429 -> "HTTP 429: Превышен лимит запросов. Попробуйте позже."
                    408 -> "HTTP 408: Timeout запроса. Попробуйте еще раз."
                    504 -> "HTTP 504: Gateway Timeout. Сервер не отвечает."
                    else -> "HTTP ошибка ${e.code()}: ${e.message}"
                }
                Result.failure(Exception(errorMessage))
            }
            is java.net.SocketTimeoutException -> {
                Result.failure(Exception("Timeout соединения с DeepSeek. Проверьте интернет-соединение и попробуйте еще раз."))
            }
            is java.net.UnknownHostException -> {
                Result.failure(Exception("Не удается подключиться к серверу DeepSeek. Проверьте интернет-соединение."))
            }
            else -> Result.failure(e)
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