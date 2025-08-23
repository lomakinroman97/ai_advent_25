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

// Модель для хранения минимальной информации о работе Kandinsky
data class KandinskyWorkData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val cityName: String,
    val status: String = "pending", // pending, success, failed
    val requestTimestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

// Модель для отчета о работе Kandinsky
data class KandinskyReport(
    val totalRequests: Int,
    val successfulGenerations: Int,
    val failedGenerations: Int,
    val averageProcessingTime: Long,
    val mostRequestedCities: List<CityStats>,
    val popularStyles: List<StyleStats>,
    val errorAnalysis: List<ErrorInfo>,
    val performanceMetrics: LegacyPerformanceMetrics,
    val recommendations: List<String>,
    val generatedAt: Long = System.currentTimeMillis()
)

data class CityStats(
    val cityName: String,
    val requestCount: Int,
    val successRate: Double
)

data class StyleStats(
    val style: String,
    val usageCount: Int,
    val averageProcessingTime: Long
)

data class ErrorStats(
    val errorType: String,
    val occurrenceCount: Int,
    val lastOccurrence: Long
)

data class LegacyPerformanceMetrics(
    val totalProcessingTime: Long,
    val fastestGeneration: Long,
    val slowestGeneration: Long,
    val successRate: Double
)

// Структурированный отчет о работе Kandinsky (JSON ответ от Агента №4)
data class KandinskyStructuredReport(
    val summary: ReportSummary,
    val statistics: ReportStatistics,
    val performance: PerformanceMetrics,
    val recommendations: List<String>,
    val generatedAt: Long = System.currentTimeMillis()
)

data class ReportSummary(
    val totalRequests: Int,
    val successfulGenerations: Int,
    val failedGenerations: Int,
    val successRate: Double
)

data class ReportStatistics(
    val averageProcessingTime: Long, // в миллисекундах
    val fastestGeneration: Long, // в миллисекундах
    val slowestGeneration: Long, // в миллисекундах
    val mostRequestedCities: List<CityUsage>,
    val popularStyles: List<StyleUsage>,
    val errorAnalysis: List<ErrorInfo>
)

data class CityUsage(
    val cityName: String,
    val requestCount: Int,
    val successRate: Double
)

data class StyleUsage(
    val styleName: String,
    val usageCount: Int,
    val averageProcessingTime: Long
)

data class ErrorInfo(
    val errorType: String,
    val occurrenceCount: Int,
    val description: String
)

// Новое PerformanceMetrics для структурированного отчета
data class PerformanceMetrics(
    val overallEfficiency: Double, // 0.0 - 1.0
    val reliabilityScore: Double, // 0.0 - 1.0
    val speedRating: String // "fast", "medium", "slow"
)

// Упрощенный отчет от Агента №4
data class SimpleKandinskyReport(
    val totalRequests: Int,
    val successfulRequests: Int,
    val failedRequests: Int,
    val cityStats: List<CityStat>,
    val briefAnalysis: String
)

data class CityStat(
    val cityName: String,
    val requestCount: Int
)

// OpenAI-compatible API Models for DeepSeek via OpenRouter
data class OpenAIChatRequest(
    val model: String = "deepseek/deepseek-r1:free",
    val messages: List<OpenAIChatMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1000
)

data class OpenAIChatMessage(
    val role: String,
    val content: String
)

data class OpenAIChatResponse(
    val id: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage
)

data class OpenAIChoice(
    val message: OpenAIChatMessage,
    val finish_reason: String
)

data class OpenAIUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

enum class LLMProvider {
    YANDEX_GPT,
    DEEPSEEK_R1
}
