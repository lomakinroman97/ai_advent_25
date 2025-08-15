package com.example.ai_advent_25.data

import com.google.gson.annotations.SerializedName

// Enum для типов агентов
enum class AgentType {
    TRAVEL_ASSISTANT,  // TravelAssistAgent
    EXPERT_REVIEWER,   // ExpertReviewerAgent
    IMAGE_GENERATOR    // GenerateImageAgent
}

data class ChatMessage(
    val role: String,
    val text: String,
    val systemContent: String? = null
)

data class ChatRequest(
    @SerializedName("modelUri")
    val modelUri: String = "gpt://b1gp9fidpabmov8j1rid/yandexgpt-lite",
    @SerializedName("messages")
    val messages: List<ChatMessage>
)

data class ChatResponse(
    val result: ChatResult
)

data class ChatResult(
    val alternatives: List<Alternative>,
    val usage: Usage
)

data class Alternative(
    val message: ChatMessage,
    val status: String
)

data class Usage(
    @SerializedName("inputTextTokens")
    val inputTextTokens: Int,
    @SerializedName("completionTokens")
    val completionTokens: Int,
    @SerializedName("totalTokens")
    val totalTokens: Int
)

data class ChatUiMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val structuredResponse: TravelRecommendation? = null,
    val questionData: QuestionData? = null,
    val expertOpinion: ExpertOpinion? = null,
    val generatedImage: GeneratedImage? = null,
    val agentType: AgentType? = null  // Тип агента, который отправил сообщение
)

// Модели для структурированного ответа от AI-агента согласно системному промпту
sealed class AgentResponse {
    data class Question(
        val type: String = "question",
        val questionNumber: Int,
        val nextQuestion: String,
        val collectedInfo: List<String>,
        val remainingQuestions: Int
    ) : AgentResponse()
    
    data class Recommendations(
        val type: String = "recommendations",
        val summary: String,
        val recommendations: List<CityRecommendation>,
        val totalBudget: String
    ) : AgentResponse()
}

// Модель для отображения вопроса в UI
data class QuestionData(
    val questionNumber: Int,
    val nextQuestion: String,
    val collectedInfo: List<String>,
    val remainingQuestions: Int
)

// Модель для рекомендаций по городам
data class CityRecommendation(
    val city: String,
    val description: String,
    val attractions: List<String>,
    val costs: String,
    val bestTime: String
)

// Модель для итоговых рекомендаций по путешествию
data class TravelRecommendation(
    val summary: String,
    val recommendations: List<CityRecommendation>,
    val totalBudget: String
)

// Модель для экспертного мнения Агента №2
data class ExpertOpinion(
    val analysis: String,
    val validation: String,
    val additionalRecommendations: List<String>,
    val travelTips: List<String>,
    val budgetAnalysis: String,
    val timingAnalysis: String,
    val riskAssessment: String
)

// Модель для сгенерированного изображения от Агента №3
data class GeneratedImage(
    val imageUrl: String,
    val cityName: String,
    val prompt: String,
    val timestamp: Long = System.currentTimeMillis()
)
