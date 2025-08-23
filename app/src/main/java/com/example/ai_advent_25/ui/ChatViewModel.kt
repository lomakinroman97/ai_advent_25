package com.example.ai_advent_25.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_advent_25.data.*
import com.example.ai_advent_25.data.AppSettings
import com.example.ai_advent_25.data.agents.factory.AgentRepositoryFactory
import com.example.ai_advent_25.data.agents.ExpertReviewerAgentRepository
import com.example.ai_advent_25.data.agents.TravelAssistAgentRepository
import com.example.ai_advent_25.data.agents.GenerateImageAgentRepository
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
    private var generateImageAgentRepository: GenerateImageAgentRepository? = null

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

    fun setApiKeys(yandexApiKey: String, deepseekApiKey: String) {
        // Сохраняем API ключи в AppSettings
        AppSettings.setYandexApiKey(yandexApiKey)
        AppSettings.setDeepseekApiKey(deepseekApiKey)
        Log.d("ChatViewModel", "API ключи сохранены в AppSettings: Yandex: '${if (yandexApiKey.isBlank()) "пустой" else "установлен"}', DeepSeek: '${if (deepseekApiKey.isBlank()) "пустой" else "установлен"}'")
        
        // Создаем репозитории с текущим выбранным LLM
        updateTravelAssistRepository()
        expertReviewerAgentRepository = AgentRepositoryFactory.createExpertReviewerAgentRepository(yandexApiKey)
        _uiState.update { it.copy(apiKeySet = true) }
    }
    
    fun setSelectedLLM(selectedLLM: LLMProvider) {
        android.util.Log.d("Debug77", "ChatViewModel: setSelectedLLM вызван с моделью: $selectedLLM")
        _uiState.update { it.copy(selectedLLM = selectedLLM) }
        updateTravelAssistRepository()
    }
    
    private fun updateTravelAssistRepository() {
        val yandexApiKey = AppSettings.getYandexApiKey()
        val deepseekApiKey = AppSettings.getDeepseekApiKey()
        val selectedLLM = _uiState.value.selectedLLM
        
        android.util.Log.d("Debug77", "ChatViewModel: updateTravelAssistRepository - Yandex ключ: ${if (yandexApiKey.isNotBlank()) "есть" else "нет"}, DeepSeek ключ: ${if (deepseekApiKey.isNotBlank()) "есть" else "нет"}, Выбранная модель: $selectedLLM")
        
        travelAssistAgentRepository = AgentRepositoryFactory.createTravelAssistAgentRepository(
            yandexApiKey, 
            deepseekApiKey, 
            selectedLLM
        )
        
        android.util.Log.d("Debug77", "ChatViewModel: TravelAssistAgentRepository создан с моделью: $selectedLLM")
    }

    // Обратная совместимость
    fun setApiKey(apiKey: String) {
        setApiKeys(apiKey, "")
    }

        fun initializeImageGenerator(context: Context) {
        Log.d("mylog", "ChatViewModel: Инициализируем генератор изображений")
        generateImageAgentRepository = AgentRepositoryFactory.createGenerateImageAgentRepository(context)
        Log.d("mylog", "ChatViewModel: GenerateImageAgentRepository создан")

        // Инициализируем MCP клиент
        viewModelScope.launch {
            try {
                Log.d("mylog", "ChatViewModel: Начинаем инициализацию MCP клиента")
                val kandinskyService = generateImageAgentRepository?.kandinskyService
                Log.d("mylog", "ChatViewModel: KandinskyService получен: ${kandinskyService != null}")
                
                kandinskyService?.initializeMCP()?.fold(
                    onSuccess = {
                        Log.d("mylog", "ChatViewModel: MCP клиент успешно инициализирован")
                    },
                    onFailure = { exception ->
                        Log.e("mylog", "ChatViewModel: Ошибка инициализации MCP клиента", exception)
                    }
                )
            } catch (e: Exception) {
                Log.e("mylog", "ChatViewModel: Ошибка при инициализации MCP", e)
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank() || travelAssistAgentRepository == null) {
            if (travelAssistAgentRepository == null) {
                _uiState.update { it.copy(error = "API ключи не установлены. Пожалуйста, введите ваши API ключи.") }
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
                expertButtonClicked = false,
                imageGenerationRequested = false,
                selectedLLM = _uiState.value.selectedLLM
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
                                _uiState.update { it.copy(messages = it.messages + assistantMessage, isLoading = false, currentAgent = null, expertButtonClicked = true) }
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

    fun generateCityImage(cityRecommendation: CityRecommendation) {
        Log.d("mylog", "ChatViewModel: Генерируем изображение для города: ${cityRecommendation.city}")
        Log.d("mylog", "ChatViewModel: Описание города: ${cityRecommendation.description}")
        
        if (generateImageAgentRepository == null) {
            Log.e("mylog", "ChatViewModel: Генератор изображений не инициализирован")
            _uiState.update { it.copy(error = "Генератор изображений не инициализирован") }
            return
        }

        Log.d("mylog", "ChatViewModel: Обновляем UI состояние - загрузка")
        _uiState.update { it.copy(isLoading = true, currentAgent = AgentType.IMAGE_GENERATOR, imageGenerationRequested = true) }

        viewModelScope.launch {
            try {
                Log.d("mylog", "ChatViewModel: Вызываем generateCityImage в репозитории")
                generateImageAgentRepository?.generateCityImage(cityRecommendation)?.fold(
                    onSuccess = { generatedImage ->
                        Log.d("mylog", "ChatViewModel: Успешная генерация изображения: $generatedImage")
                        val assistantMessage = ChatUiMessage(
                            content = "Изображение города ${cityRecommendation.city} сгенерировано с помощью ИИ",
                            isUser = false,
                            agentType = AgentType.IMAGE_GENERATOR,
                            generatedImage = generatedImage
                        )
                        Log.d("mylog", "ChatViewModel: Создано сообщение с изображением")
                        _uiState.update { 
                            it.copy(
                                messages = it.messages + assistantMessage, 
                                isLoading = false, 
                                currentAgent = null
                            ) 
                        }
                        Log.d("mylog", "ChatViewModel: UI состояние обновлено")
                    },
                    onFailure = { exception ->
                        Log.e("mylog", "ChatViewModel: Ошибка при генерации изображения", exception)
                        _uiState.update { 
                            it.copy(
                                error = "Ошибка при генерации изображения: ${exception.message}", 
                                isLoading = false, 
                                currentAgent = null
                            ) 
                        }
                    }
                ) ?: run {
                    Log.e("mylog", "ChatViewModel: GenerateImageAgentRepository недоступен")
                    _uiState.update { 
                        it.copy(
                            error = "GenerateImageAgentRepository недоступен", 
                            isLoading = false, 
                            currentAgent = null
                        ) 
                    }
                }
            } catch (e: Exception) {
                Log.e("mylog", "ChatViewModel: Исключение при генерации изображения", e)
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Неизвестная ошибка при генерации изображения", 
                        isLoading = false, 
                        currentAgent = null
                    ) 
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
    val expertButtonClicked: Boolean = false,  // Флаг, указывающий, что кнопка "Подключить эксперта" была нажата
    val imageGenerationRequested: Boolean = false,  // Флаг, указывающий, что кнопка генерации изображения была нажата
    val selectedLLM: LLMProvider = LLMProvider.YANDEX_GPT  // Выбранная LLM модель
)
