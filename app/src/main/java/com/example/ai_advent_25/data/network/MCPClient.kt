package com.example.ai_advent_25.data.network

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import org.json.JSONObject

/**
 * MCP клиент для интеграции с MCP протоколом
 * 
 * MCP (Model Context Protocol) позволяет вызывать инструменты MCP серверов
 * В нашем случае - инструмент kandinsky_generate_image от MCP Kandinsky
 */
class MCPClient(
    private val context: Context
) {
    companion object {
        private const val TAG = "MCPClient"
        // URL к Fusion Brain API (из документации)
        private const val MCP_BASE_URL = "https://api-key.fusionbrain.ai"
    }

    private var mcpClient: MCPProtocol.MCPClient? = null
    private var isInitialized = false

    /**
     * Инициализирует MCP клиент
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("mylog", "MCPClient: Начинаем инициализацию")
            if (isInitialized) {
                Log.d("mylog", "MCPClient: Уже инициализирован")
                Result.success(Unit)
            } else {
                Log.d("mylog", "MCPClient: Создаем новый MCP клиент")
                // Создаем MCP клиент для HTTP подключения к Fusion Brain API
                // Получаем API ключи из конфигурации
                val apiKey = "17FB62223849181819EE07BA32335675"
                val secretKey = "41551FB4D1968CE3FD02AF6BEDBD1888"
                
                Log.d("mylog", "MCPClient: API Key: ${apiKey.take(8)}...")
                Log.d("mylog", "MCPClient: Secret Key: ${secretKey.take(8)}...")
                                       Log.d("mylog", "MCPClient: Base URL: $MCP_BASE_URL")

                       mcpClient = MCPProtocol.MCPClient(MCP_BASE_URL, apiKey, secretKey)
                
                // Подключаемся к Fusion Brain API через HTTP
                Log.d("mylog", "MCPClient: Подключаемся к API")
                val connectResult = mcpClient?.connect()
                if (connectResult?.isSuccess == true) {
                    isInitialized = true
                    Log.d("mylog", "MCPClient: Успешно подключен к Fusion Brain API")
                    Result.success(Unit)
                } else {
                    Log.e("mylog", "MCPClient: Ошибка подключения к Fusion Brain API")
                    Result.failure(connectResult?.exceptionOrNull() ?: Exception("Не удалось подключиться к Fusion Brain API"))
                }
            }
        } catch (e: Exception) {
            Log.e("mylog", "MCPClient: Ошибка инициализации", e)
            Result.failure(e)
        }
    }

    /**
     * Вызывает MCP инструмент kandinsky_generate_image
     */
    suspend fun callKandinskyGenerateImage(
        prompt: String,
        filename: String,
        projectDir: String,
        width: Int = 1024,
        height: Int = 1024,
        style: String = "DEFAULT",
        negativePrompt: String = "",
        overwrite: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("mylog", "MCPClient: Вызываем kandinsky_generate_image")
            Log.d("mylog", "MCPClient: Prompt: $prompt")
            Log.d("mylog", "MCPClient: Filename: $filename")
            Log.d("mylog", "MCPClient: Project Dir: $projectDir")
            Log.d("mylog", "MCPClient: Width: $width, Height: $height")
            Log.d("mylog", "MCPClient: Style: $style")
            Log.d("mylog", "MCPClient: Negative Prompt: $negativePrompt")
            Log.d("mylog", "MCPClient: Overwrite: $overwrite")
            
            // Проверяем инициализацию
            if (!isInitialized) {
                Log.d("mylog", "MCPClient: Не инициализирован, инициализируем")
                val initResult = initialize()
                if (initResult.isFailure) {
                    Log.e("mylog", "MCPClient: Ошибка инициализации")
                    return@withContext Result.failure(initResult.exceptionOrNull() ?: Exception("Ошибка инициализации"))
                }
            } else {
                Log.d("mylog", "MCPClient: Уже инициализирован")
            }

            // Создаем параметры для вызова инструмента
            val arguments = mapOf(
                "prompt" to prompt,
                "filename" to filename,
                "project_dir" to projectDir,
                "width" to width,
                "height" to height,
                "style" to style,
                "negative_prompt" to negativePrompt,
                "overwrite" to overwrite
            )
            Log.d("mylog", "MCPClient: Созданы аргументы: $arguments")

            // Вызываем MCP инструмент kandinsky_generate_image
            Log.d("mylog", "MCPClient: Вызываем callTool с аргументами")
            val result = mcpClient?.callTool("kandinsky_generate_image", arguments)
            Log.d("mylog", "MCPClient: Результат callTool: $result")
            
            if (result?.isSuccess == true) {
                val response = result.getOrNull() ?: ""
                Log.d("mylog", "MCPClient: Успешный ответ: $response")
                
                if (response.contains("успешно") || response.contains("successfully") || response.contains("Изображение готово")) {
                    Log.d("mylog", "MCPClient: Генерация успешна")
                    
                    // Если ответ содержит путь к файлу, используем его
                    if (response.contains("Изображение готово:")) {
                        val imagePath = response.substringAfter("Изображение готово: ").trim()
                        Log.d("mylog", "MCPClient: Получен путь к изображению: $imagePath")
                        
                        // Проверяем, что файл существует
                        val imageFile = File(imagePath)
                        if (imageFile.exists()) {
                            Log.d("mylog", "MCPClient: Возвращаем успешный результат: $imagePath")
                            return@withContext Result.success(imagePath)
                        } else {
                            Log.e("mylog", "MCPClient: Файл не найден по пути: $imagePath")
                            return@withContext Result.failure(Exception("Файл не найден: $imagePath"))
                        }
                    } else {
                        // Ищем файл по старой логике
                        Log.d("mylog", "MCPClient: Ищем файл по старой логике")
                        val imagePath = findGeneratedImage(projectDir, filename)
                        Log.d("mylog", "MCPClient: Найденный путь: $imagePath")
                        
                        if (imagePath != null) {
                            Log.d("mylog", "MCPClient: Возвращаем успешный результат: $imagePath")
                            return@withContext Result.success(imagePath)
                        } else {
                            Log.e("mylog", "MCPClient: Изображение не найдено: $response")
                            return@withContext Result.failure(Exception("Изображение сгенерировано, но не найдено: $response"))
                        }
                    }
                } else {
                    Log.e("mylog", "MCPClient: Ошибка в ответе: $response")
                    return@withContext Result.failure(Exception("Ошибка генерации: $response"))
                }
            } else {
                Log.e("mylog", "MCPClient: Ошибка callTool: ${result?.exceptionOrNull()}")
                return@withContext Result.failure(result?.exceptionOrNull() ?: Exception("Неизвестная ошибка MCP"))
            }
        } catch (e: Exception) {
            Log.e("mylog", "MCPClient: Ошибка при вызове MCP инструмента", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Ищет сгенерированное изображение в папке kandinsky
     */
    fun findGeneratedImage(projectDir: String, filename: String): String? {
        Log.d("mylog", "MCPClient: Ищем изображение: $filename в $projectDir")
        val kandinskyDir = File(projectDir, "kandinsky")
        Log.d("mylog", "MCPClient: Папка kandinsky: ${kandinskyDir.absolutePath}")
        Log.d("mylog", "MCPClient: Папка существует: ${kandinskyDir.exists()}")
        
        val imageFile = File(kandinskyDir, filename)
        Log.d("mylog", "MCPClient: Файл изображения: ${imageFile.absolutePath}")
        Log.d("mylog", "MCPClient: Файл существует: ${imageFile.exists()}")
        
        return if (imageFile.exists()) {
            Log.d("mylog", "MCPClient: Изображение найдено: ${imageFile.absolutePath}")
            imageFile.absolutePath
        } else {
            Log.e("mylog", "MCPClient: Изображение не найдено")
            null
        }
    }

    /**
     * Проверяет доступность удаленного MCP сервера
     */
    suspend fun isServerAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("mylog", "MCPClient: Проверяем доступность сервера")
            val result = mcpClient?.isRunning() ?: false
            Log.d("mylog", "MCPClient: Сервер доступен: $result")
            result
        } catch (e: Exception) {
            Log.e("mylog", "MCPClient: Ошибка при проверке доступности сервера", e)
            false
        }
    }

    /**
     * Останавливает MCP клиент
     */
    suspend fun shutdown(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val stopResult = mcpClient?.stop()
            mcpClient = null
            isInitialized = false
            
            if (stopResult?.isSuccess == true) {
                Log.d("mylog", "MCPClient: Успешно остановлен")
                Result.success(Unit)
            } else {
                Log.w("mylog", "MCPClient: Предупреждение при остановке")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("mylog", "MCPClient: Ошибка при остановке", e)
            Result.failure(e)
        }
    }
}
