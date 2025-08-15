package com.example.ai_advent_25.data.agents

import android.content.Context
import android.util.Log
import com.example.ai_advent_25.data.CityRecommendation
import com.example.ai_advent_25.data.GeneratedImage
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

    suspend fun generateCityImage(cityRecommendation: CityRecommendation): Result<GeneratedImage> {
        Log.d("mylog", "GenerateImageAgentRepository: Начинаем генерацию изображения")
        Log.d("mylog", "GenerateImageAgentRepository: Город: ${cityRecommendation.city}")
        Log.d("mylog", "GenerateImageAgentRepository: Описание: ${cityRecommendation.description}")
        
        return try {
            withContext(Dispatchers.IO) {
                // Создаем промпт для генерации изображения
                val prompt = createImagePrompt(cityRecommendation)
                Log.d("mylog", "GenerateImageAgentRepository: Создан промпт: $prompt")
                
                // Генерируем изображение через KandinskyService
                Log.d("mylog", "GenerateImageAgentRepository: Вызываем kandinskyService.generateImage")
                val imagePath = kandinskyService.generateImage(
                    prompt = prompt,
                    cityName = cityRecommendation.city
                )
                Log.d("mylog", "GenerateImageAgentRepository: Результат generateImage: $imagePath")
                
                imagePath.fold(
                    onSuccess = { path ->
                        Log.d("mylog", "GenerateImageAgentRepository: Успешная генерация, путь: $path")
                        val generatedImage = GeneratedImage(
                            imageUrl = path,
                            cityName = cityRecommendation.city,
                            prompt = prompt
                        )
                        Log.d("mylog", "GenerateImageAgentRepository: Создан GeneratedImage: $generatedImage")
                        Result.success(generatedImage)
                    },
                    onFailure = { exception ->
                        Log.e("mylog", "GenerateImageAgentRepository: Ошибка в generateImage", exception)
                        Result.failure(exception)
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("mylog", "GenerateImageAgentRepository: Исключение при генерации изображения", e)
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
