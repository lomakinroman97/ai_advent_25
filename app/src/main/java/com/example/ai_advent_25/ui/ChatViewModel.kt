package com.example.ai_advent_25.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_advent_25.data.*
import com.example.ai_advent_25.data.agents.factory.AgentRepositoryFactory
import com.example.ai_advent_25.data.agents.ExpertReviewerAgentRepository
import com.example.ai_advent_25.data.agents.TravelAssistAgentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var travelAssistAgentRepository: TravelAssistAgentRepository? = null
    private var expertReviewerAgentRepository: ExpertReviewerAgentRepository? = null

    init {
        _uiState.update { 
            it.copy(
                messages = listOf(
                    ChatUiMessage(
                        content = "Готов подобрать для вас путешествие! Отправьте \"В путь!\" и ответьте на несколько вопросов.",
                        isUser = false,
                        agentType = AgentType.TRAVEL_ASSISTANT
                    )
                )
            )
        }
    }

    fun setApiKey(apiKey: String) {
        travelAssistAgentRepository = AgentRepositoryFactory.createTravelAssistAgentRepository(apiKey)
        expertReviewerAgentRepository = AgentRepositoryFactory.createExpertReviewerAgentRepository(apiKey)
        _uiState.update { it.copy(apiKeySet = true) }
    }

    fun sendMessage(message: String) {
        if (message.isBlank() || travelAssistAgentRepository == null) {
            if (travelAssistAgentRepository == null) {
                _uiState.update { it.copy(error = "API ключ не установлен. Пожалуйста, введите ваш API ключ.") }
            }
            return
        }

        val userMessage = ChatUiMessage(content = message, isUser = true)
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true, currentAgent = AgentType.TRAVEL_ASSISTANT) }

        viewModelScope.launch {
            try {
                val chatMessages = _uiState.value.messages.map { msg ->
                    ChatMessage(role = if (msg.isUser) "user" else "assistant", text = msg.content)
                }
                
                travelAssistAgentRepository?.sendMessage(chatMessages)?.fold(
                    onSuccess = { result ->
                        val assistantMessage = when (result) {
                            is ParseResult.QuestionResponse -> ChatUiMessage(
                                content = result.question.nextQuestion,
                                isUser = false,
                                agentType = AgentType.TRAVEL_ASSISTANT,
                                questionData = QuestionData(
                                    questionNumber = result.question.questionNumber,
                                    nextQuestion = result.question.nextQuestion,
                                    collectedInfo = result.question.collectedInfo,
                                    remainingQuestions = result.question.remainingQuestions
                                )
                            )
                            is ParseResult.RecommendationsResponse -> ChatUiMessage(
                                content = result.originalResponse,
                                isUser = false,
                                agentType = AgentType.TRAVEL_ASSISTANT,
                                structuredResponse = TravelRecommendation(
                                    summary = result.recommendations.summary,
                                    recommendations = result.recommendations.recommendations,
                                    totalBudget = result.recommendations.totalBudget
                                )
                            )
                            is ParseResult.ExpertOpinionResponse -> ChatUiMessage(
                                content = "Экспертное мнение по вашим рекомендациям",
                                isUser = false,
                                agentType = AgentType.EXPERT_REVIEWER,
                                expertOpinion = result.expertOpinion
                            )
                        }
                        
                        _uiState.update { it.copy(messages = it.messages + assistantMessage, isLoading = false, currentAgent = null) }
                    },
                    onFailure = { exception ->
                        _uiState.update { 
                            it.copy(error = exception.message ?: "Unknown error occurred", isLoading = false, currentAgent = null) 
                        }
                    }
                ) ?: run {
                    _uiState.update { 
                        it.copy(error = "TravelAgentRepository недоступен", isLoading = false, currentAgent = null) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Unknown error occurred", isLoading = false, currentAgent = null) 
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    
    fun retryLastMessage() {
        _uiState.value.messages.lastOrNull { it.isUser }?.let { sendMessage(it.content) }
    }
    
    fun clearChat() {
        _uiState.update { 
            ChatUiState(
                messages = listOf(
                    ChatUiMessage(
                        content = "Привет! Я агент по подбору городов для путешествий по России. Введите \"В путь!\" и я начну задавать вам вопросы для подбора идеальных вариантов. Я буду задавать вопросы по одному, чтобы собрать всю необходимую информацию о ваших предпочтениях.",
                        isUser = false,
                        agentType = AgentType.TRAVEL_ASSISTANT
                    )
                ),
                expertButtonClicked = false
            )
        }
    }

    fun getExpertOpinion(travelRecommendation: TravelRecommendation) {
        if (expertReviewerAgentRepository == null) {
            _uiState.update { it.copy(error = "API ключ не установлен. Пожалуйста, введите ваш API ключ.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, currentAgent = AgentType.EXPERT_REVIEWER, expertButtonClicked = true) }

        viewModelScope.launch {
            try {
                val chatHistory = _uiState.value.messages.map { msg ->
                    ChatMessage(role = if (msg.isUser) "user" else "assistant", text = msg.content)
                }
                
                expertReviewerAgentRepository?.getExpertOpinion(travelRecommendation, chatHistory)?.fold(
                    onSuccess = { result ->
                        when (result) {
                            is ParseResult.ExpertOpinionResponse -> {
                                val assistantMessage = ChatUiMessage(
                                    content = "Экспертное мнение по вашим рекомендациям",
                                    isUser = false,
                                    agentType = AgentType.EXPERT_REVIEWER,
                                    expertOpinion = result.expertOpinion
                                )
                                _uiState.update { it.copy(messages = it.messages + assistantMessage, isLoading = false, currentAgent = null) }
                            }
                            else -> _uiState.update { 
                                it.copy(error = "Неожиданный ответ от эксперта", isLoading = false, currentAgent = null) 
                            }
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { 
                            it.copy(error = exception.message ?: "Ошибка при получении экспертного мнения", isLoading = false, currentAgent = null) 
                        }
                    }
                ) ?: run {
                    _uiState.update { 
                        it.copy(error = "ExpertReviewerRepository недоступен", isLoading = false, currentAgent = null) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Неизвестная ошибка при получении экспертного мнения", isLoading = false, currentAgent = null) 
                }
            }
        }
    }
}

data class ChatUiState(
    val messages: List<ChatUiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val apiKeySet: Boolean = false,
    val currentAgent: AgentType? = null,  // Текущий активный агент для отображения правильного текста загрузки
    val expertButtonClicked: Boolean = false  // Флаг, указывающий, что кнопка "Подключить эксперта" была нажата
)
