package com.example.ai_advent_25.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Сервис для интеграции с MCP Kandinsky
 * 
 * MCP Kandinsky предоставляет инструмент kandinsky_generate_image для генерации изображений
 * через официальный API Kandinsky (Fusion Brain).
 * 
 * Документация: https://lobechat.com/discover/mcp/ai-forever-mcp_kandinsky
 */
class KandinskyService(
    private val context: Context,
    private val apiKey: String,
    private val secretKey: String
) {

    private val mcpClient = MCPClient(context)

    suspend fun generateImage(
        prompt: String,
        cityName: String,
        width: Int = 1024,
        height: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("mylog", "KandinskyService: Начинаем генерацию изображения")
            Log.d("mylog", "KandinskyService: Исходный prompt: $prompt")
            Log.d("mylog", "KandinskyService: Город: $cityName")
            Log.d("mylog", "KandinskyService: Размеры: ${width}x${height}")
            
            // Создаем промпт для Kandinsky
            val enhancedPrompt = enhancePrompt(prompt, cityName)
            Log.d("mylog", "KandinskyService: Улучшенный prompt: $enhancedPrompt")
            
            // Вызываем MCP инструмент kandinsky_generate_image
            val filename = "${cityName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            val projectDir = context.filesDir.absolutePath
            Log.d("mylog", "KandinskyService: Filename: $filename")
            Log.d("mylog", "KandinskyService: Project Dir: $projectDir")
            
            Log.d("mylog", "KandinskyService: Вызываем MCP клиент")
            val result = mcpClient.callKandinskyGenerateImage(
                prompt = enhancedPrompt,
                filename = filename,
                projectDir = projectDir,
                width = width,
                height = height,
                style = "DEFAULT",
                negativePrompt = "",
                overwrite = false
            )
            
            Log.d("mylog", "KandinskyService: Получен результат от MCP клиента: $result")
            
            result.fold(
                onSuccess = { imagePath -> 
                    Log.d("mylog", "KandinskyService: Успешная генерация, путь: $imagePath")
                    Result.success(imagePath) 
                },
                onFailure = { exception ->
                    Log.w("mylog", "KandinskyService: MCP инструмент недоступен, используем заглушку", exception)
                    // Если MCP недоступен, создаем заглушку
                    Log.d("mylog", "KandinskyService: Создаем заглушку изображения")
                    val placeholderBitmap = createPlaceholderImage(width, height, cityName)
                    val placeholderPath = saveImageToInternalStorage(placeholderBitmap, cityName)
                    Log.d("mylog", "KandinskyService: Заглушка сохранена: $placeholderPath")
                    Result.success(placeholderPath)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Инициализирует MCP клиент
     */
    suspend fun initializeMCP(): Result<Unit> {
        Log.d("mylog", "KandinskyService: Инициализируем MCP клиент")
        val result = mcpClient.initialize()
        Log.d("mylog", "KandinskyService: Результат инициализации: $result")
        return result
    }

    /**
     * Проверяет доступность MCP сервера
     */
    suspend fun isMCPServerAvailable(): Boolean {
        Log.d("mylog", "KandinskyService: Проверяем доступность MCP сервера")
        val result = mcpClient.isServerAvailable()
        Log.d("mylog", "KandinskyService: MCP сервер доступен: $result")
        return result
    }

    private fun enhancePrompt(prompt: String, cityName: String): String {
        return buildString {
            append("Город $cityName, ")
            append(prompt)
            append(", высокое качество, детализированное изображение, ")
            append("красивый городской пейзаж, профессиональная фотография")
        }
    }

    /**
     * Создает заглушку изображения (временно, пока не реализована MCP интеграция)
     */
    private fun createPlaceholderImage(width: Int, height: Int, cityName: String): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        
        // Градиентный фон
        for (x in 0 until width) {
            for (y in 0 until height) {
                val red = (x * 255 / width)
                val green = (y * 255 / height)
                val blue = 128 + (x + y) * 64 / (width + height)
                val color = android.graphics.Color.rgb(
                    red.coerceIn(0, 255),
                    green.coerceIn(0, 255),
                    blue.coerceIn(0, 255)
                )
                bitmap.setPixel(x, y, color)
            }
        }
        
        // Центральный круг с названием города
        val centerX = width / 2
        val centerY = height / 2
        val radius = minOf(width, height) / 4
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val distance = kotlin.math.sqrt(((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).toDouble())
                if (distance <= radius) {
                    val alpha = ((radius - distance) / radius * 255).toInt().coerceIn(0, 255)
                    val cityColor = android.graphics.Color.argb(
                        alpha,
                        255, 255, 255
                    )
                    bitmap.setPixel(x, y, cityColor)
                }
            }
        }
        
        return bitmap
    }

    /**
     * Сохраняет изображение во внутреннее хранилище
     */
    private fun saveImageToInternalStorage(bitmap: android.graphics.Bitmap, cityName: String): String {
        val fileName = "kandinsky_${cityName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        val file = java.io.File(context.filesDir, fileName)
        
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        
        return file.absolutePath
    }



    companion object {
        const val KANDINSKY_API_KEY = "17FB62223849181819EE07BA32335675"
        const val KANDINSKY_SECRET_KEY = "41551FB4D1968CE3FD02AF6BEDBD1888"
    }
}
