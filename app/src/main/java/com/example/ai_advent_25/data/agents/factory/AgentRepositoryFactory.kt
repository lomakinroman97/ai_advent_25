package com.example.ai_advent_25.data.agents.factory

import android.content.Context
import com.example.ai_advent_25.data.agents.ExpertReviewerAgentRepository
import com.example.ai_advent_25.data.agents.TravelAssistAgentRepository
import com.example.ai_advent_25.data.agents.GenerateImageAgentRepository
import com.example.ai_advent_25.data.agents.KandinskyReportCreatorAgentRepository
import com.example.ai_advent_25.data.network.KandinskyService
import com.example.ai_advent_25.data.network.NetworkModule
import com.example.ai_advent_25.data.network.NetworkProvider

object AgentRepositoryFactory {
    
    fun createExpertReviewerAgentRepository(
        apiKey: String,
        networkProvider: NetworkProvider = NetworkModule
    ): ExpertReviewerAgentRepository {
        return ExpertReviewerAgentRepository(apiKey, networkProvider)
    }
    
    fun createTravelAssistAgentRepository(
        apiKey: String,
        networkProvider: NetworkProvider = NetworkModule
    ): TravelAssistAgentRepository {
        return TravelAssistAgentRepository(apiKey, networkProvider)
    }
    
    fun createGenerateImageAgentRepository(
        context: Context,
        kandinskyApiKey: String = KandinskyService.KANDINSKY_API_KEY,
        kandinskySecretKey: String = KandinskyService.KANDINSKY_SECRET_KEY
    ): GenerateImageAgentRepository {
        return GenerateImageAgentRepository(context, kandinskyApiKey, kandinskySecretKey)
    }
    
    fun createKandinskyReportCreatorAgentRepository(
        apiKey: String,
        networkProvider: NetworkProvider = NetworkModule
    ): KandinskyReportCreatorAgentRepository {
        return KandinskyReportCreatorAgentRepository(apiKey, networkProvider)
    }
}
