package com.example.ai_advent_25.data.agents

import android.content.Context
import android.util.Log
import com.example.ai_advent_25.data.CityRecommendation
import com.example.ai_advent_25.data.GeneratedImage
import com.example.ai_advent_25.data.KandinskyWorkData
import com.example.ai_advent_25.data.network.KandinskyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GenerateImageAgentRepository(
    private val context: Context,
    private val kandinskyApiKey: String,
    private val kandinskySecretKey: String
) {

    val kandinskyService by lazy {
        KandinskyService(context, kandinskyApiKey, kandinskySecretKey)
    }
    
    private val kandinskyDataRepository by lazy {
        KandinskyDataRepository(context)
    }

    suspend fun generateCityImage(cityRecommendation: CityRecommendation): Result<GeneratedImage> {
        Log.d("mylog", "GenerateImageAgentRepository: Начинаем генерацию изображения")
        Log.d("mylog", "GenerateImageAgentRepository: Город: ${cityRecommendation.city}")
        Log.d("mylog", "GenerateImageAgentRepository: Описание: ${cityRecommendation.description}")
        
        // Create initial work data entry
        val workDataId = java.util.UUID.randomUUID().toString()
        val workData = KandinskyWorkData(
            id = workDataId,
            cityName = cityRecommendation.city,
            requestTimestamp = System.currentTimeMillis(),
            status = "pending"
        )
        kandinskyDataRepository.saveWorkData(workData).fold(
            onSuccess = { Log.d("mylog", "GenerateImageAgentRepository: Данные о работе сохранены") },
            onFailure = { exception -> Log.e("mylog", "GenerateImageAgentRepository: Ошибка сохранения данных", exception) }
        )
        
        return try {
            withContext(Dispatchers.IO) {
                // Создаем промпт для генерации изображения
                val prompt = createImagePrompt(cityRecommendation)
                Log.d("mylog", "GenerateImageAgentRepository: Создан промпт: $prompt")
                
                val imagePath = kandinskyService.generateImage(
                    prompt = prompt,
                    cityName = cityRecommendation.city
                )
                Log.d("mylog", "GenerateImageAgentRepository: Результат generateImage: $imagePath")
                
                imagePath.fold(
                    onSuccess = { path ->
                        val updatedWorkData = workData.copy(status = "success")
                        kandinskyDataRepository.updateWorkData(updatedWorkData).fold(
                            onSuccess = { Log.d("mylog", "GenerateImageAgentRepository: Данные о работе обновлены") },
                            onFailure = { exception -> Log.e("mylog", "GenerateImageAgentRepository: Ошибка обновления данных", exception) }
                        )
                        Result.success(GeneratedImage(imageUrl = path, cityName = cityRecommendation.city, prompt = prompt))
                    },
                    onFailure = { exception ->
                        val updatedWorkData = workData.copy(status = "failed", errorMessage = exception.message ?: "Unknown error")
                        kandinskyDataRepository.updateWorkData(updatedWorkData).fold(
                            onSuccess = { Log.d("mylog", "GenerateImageAgentRepository: Данные об ошибке сохранены") },
                            onFailure = { updateException -> Log.e("mylog", "GenerateImageAgentRepository: Ошибка сохранения данных об ошибке", updateException) }
                        )
                        Result.failure(exception)
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("mylog", "GenerateImageAgentRepository: Исключение при генерации изображения", e)
            
            // Обновляем данные о работе - исключение
            val updatedWorkData = workData.copy(
                status = "failed",
                errorMessage = e.message ?: "Exception occurred"
            )
            
            kandinskyDataRepository.updateWorkData(updatedWorkData).fold(
                onSuccess = {
                    Log.d("mylog", "GenerateImageAgentRepository: Данные об исключении сохранены")
                },
                onFailure = { updateException ->
                    Log.e("mylog", "GenerateImageAgentRepository: Ошибка сохранения данных об исключении", updateException)
                }
            )
            
            Result.failure(e)
        }
    }

    private fun createImagePrompt(cityRecommendation: CityRecommendation): String {
        return buildString {
            append("${cityRecommendation.description}, ")
            append("достопримечательности: ${cityRecommendation.attractions.joinToString(", ")}, ")
            append("лучшее время: ${cityRecommendation.bestTime}")
        }
    }
}
