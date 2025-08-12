package com.example.ai_advent_25.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_advent_25.data.ChatMessage
import com.example.ai_advent_25.data.ChatRepository
import com.example.ai_advent_25.data.ChatUiMessage
import com.example.ai_advent_25.data.ParseResult
import com.example.ai_advent_25.data.TravelRecommendation
import com.example.ai_advent_25.data.QuestionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import com.example.ai_advent_25.data.JsonResponseParser

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var chatRepository: ChatRepository? = null
    
    companion object {
        private const val TAG = "ChatViewModel"
    }

    init {
        Log.d(TAG, "ChatViewModel инициализирован")
        // Добавляем стартовое сообщение от LLM
        _uiState.update { currentState ->
            currentState.copy(
                messages = listOf(
                    ChatUiMessage(
                        content = "Привет! Я агент по подбору городов для путешествий по России. Введите \"В путь!\" и я начну задавать вам вопросы для подбора идеальных вариантов. Я буду задавать вопросы по одному, чтобы собрать всю необходимую информацию о ваших предпочтениях.",
                        isUser = false
                    )
                )
            )
        }
        Log.d(TAG, "Добавлено стартовое сообщение")
    }

    fun setApiKey(apiKey: String) {
        Log.d(TAG, "Устанавливаем API ключ: ${apiKey.take(10)}...")
        chatRepository = ChatRepository(apiKey)
        _uiState.update { it.copy(apiKeySet = true) }
        Log.d(TAG, "API ключ установлен")
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        Log.d(TAG, "Отправляем сообщение: $message")
        
        if (chatRepository == null) {
            Log.e(TAG, "ChatRepository не инициализирован")
            _uiState.update { currentState ->
                currentState.copy(
                    error = "API ключ не установлен. Пожалуйста, введите ваш API ключ."
                )
            }
            return
        }

        val userMessage = ChatUiMessage(
            content = message,
            isUser = true
        )

        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                isLoading = true
            )
        }
        
        Log.d(TAG, "Сообщение пользователя добавлено в UI, начинаем обработку")

        viewModelScope.launch {
            try {
                Log.d(TAG, "Создаем список сообщений для API")
                val chatMessages = mutableListOf<ChatMessage>()

                _uiState.value.messages.forEach { msg ->
                    val role = if (msg.isUser) "user" else "assistant"
                    chatMessages.add(ChatMessage(role = role, text = msg.content))
                    Log.d(TAG, "Добавлено сообщение: role=$role, text=${msg.content.take(50)}...")
                }
                
                Log.d(TAG, "Отправляем запрос в ChatRepository")
                val response = chatRepository!!.sendMessage(chatMessages)
                
                Log.d(TAG, "Получен ответ от ChatRepository")

                response.fold(
                    onSuccess = { result ->
                        Log.d(TAG, "Успешный ответ, тип: ${result::class.simpleName}")
                        when (result) {
                            is ParseResult.QuestionResponse -> {
                                Log.d(TAG, "Получен вопрос от агента")
                                val assistantMessage = ChatUiMessage(
                                    content = result.question.nextQuestion,
                                    isUser = false,
                                    questionData = QuestionData(
                                        questionNumber = result.question.questionNumber,
                                        nextQuestion = result.question.nextQuestion,
                                        collectedInfo = result.question.collectedInfo,
                                        remainingQuestions = result.question.remainingQuestions
                                    )
                                )
                                
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        messages = currentState.messages + assistantMessage,
                                        isLoading = false
                                    )
                                }
                                Log.d(TAG, "Вопрос агента добавлен в UI")
                            }
                            is ParseResult.RecommendationsResponse -> {
                                Log.d(TAG, "Получены рекомендации от агента")
                                val assistantMessage = ChatUiMessage(
                                    content = result.originalResponse,
                                    isUser = false,
                                    structuredResponse = TravelRecommendation(
                                        summary = result.recommendations.summary,
                                        recommendations = result.recommendations.recommendations,
                                        totalBudget = result.recommendations.totalBudget
                                    )
                                )
                                
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        messages = currentState.messages + assistantMessage,
                                        isLoading = false
                                    )
                                }
                                Log.d(TAG, "Рекомендации агента добавлены в UI")
                            }
                            is ParseResult.RegularResponse -> {
                                Log.d(TAG, "Обычный ответ получен")
                                
                                // Дополнительная проверка на JSON в обычном ответе
                                val additionalParseResult = JsonResponseParser.parseResponse(result.response)
                                val finalContent = when (additionalParseResult) {
                                    is ParseResult.QuestionResponse -> additionalParseResult.question.nextQuestion
                                    is ParseResult.RecommendationsResponse -> "Получены рекомендации по путешествию"
                                    is ParseResult.RegularResponse -> result.response
                                }
                                
                                val assistantMessage = ChatUiMessage(
                                    content = finalContent,
                                    isUser = false,
                                    questionData = if (additionalParseResult is ParseResult.QuestionResponse) {
                                        QuestionData(
                                            questionNumber = additionalParseResult.question.questionNumber,
                                            nextQuestion = additionalParseResult.question.nextQuestion,
                                            collectedInfo = additionalParseResult.question.collectedInfo,
                                            remainingQuestions = additionalParseResult.question.remainingQuestions
                                        )
                                    } else null,
                                    structuredResponse = if (additionalParseResult is ParseResult.RecommendationsResponse) {
                                        TravelRecommendation(
                                            summary = additionalParseResult.recommendations.summary,
                                            recommendations = additionalParseResult.recommendations.recommendations,
                                            totalBudget = additionalParseResult.recommendations.totalBudget
                                        )
                                    } else null
                                )
                                
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        messages = currentState.messages + assistantMessage,
                                        isLoading = false
                                    )
                                }
                                Log.d(TAG, "Обычное сообщение добавлено в UI")
                            }
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Ошибка при получении ответа", exception)
                        _uiState.update { currentState ->
                            currentState.copy(
                                error = exception.message ?: "Unknown error occurred",
                                isLoading = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Исключение в sendMessage", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message ?: "Unknown error occurred",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearError() {
        Log.d(TAG, "Очищаем ошибку")
        _uiState.update { it.copy(error = null) }
    }
    
    fun retryLastMessage() {
        val lastUserMessage = _uiState.value.messages.lastOrNull { it.isUser }
        if (lastUserMessage != null) {
            Log.d(TAG, "Повторяем отправку последнего сообщения: ${lastUserMessage.content}")
            sendMessage(lastUserMessage.content)
        }
    }
    
    fun clearChat() {
        Log.d(TAG, "Очищаем чат")
        // При очистке чата снова добавляем стартовое сообщение
        _uiState.update { 
            ChatUiState(
                messages = listOf(
                    ChatUiMessage(
                        content = "Привет! Я агент по подбору городов для путешествий по России. Введите \"В путь!\" и я начну задавать вам вопросы для подбора идеальных вариантов. Я буду задавать вопросы по одному, чтобы собрать всю необходимую информацию о ваших предпочтениях.",
                        isUser = false
                    )
                )
            )
        }
        Log.d(TAG, "Чат очищен, добавлено стартовое сообщение")
    }
}

data class ChatUiState(
    val messages: List<ChatUiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val apiKeySet: Boolean = false
)
