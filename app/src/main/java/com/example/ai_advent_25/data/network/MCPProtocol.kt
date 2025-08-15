package com.example.ai_advent_25.data.network

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import org.json.JSONObject as OrgJSONObject
import org.json.JSONArray as OrgJSONArray
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PipedInputStream
import java.io.PipedOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * MCP протокол для интеграции с MCP серверами
 * 
 * MCP (Model Context Protocol) работает через stdio transport
 * и использует JSON-RPC для обмена сообщениями
 */
class MCPProtocol {
    companion object {
        private const val TAG = "MCPProtocol"
        
        // MCP JSON-RPC версия
        private const val JSON_RPC_VERSION = "2.0"
        
        // MCP сообщения
        private const val MESSAGE_TYPE_INIT = "initialize"
        private const val MESSAGE_TYPE_TOOLS = "tools/list"
        private const val MESSAGE_TYPE_CALL = "tools/call"
        private const val MESSAGE_TYPE_RESPONSE = "response"
        private const val MESSAGE_TYPE_ERROR = "error"
    }

    /**
     * MCP сообщение
     */
    data class MCPMessage(
        val jsonrpc: String = JSON_RPC_VERSION,
        val id: String? = null,
        val method: String? = null,
        val params: OrgJSONObject? = null,
        val result: Any? = null,
        val error: MCPError? = null
    )

    /**
     * MCP ошибка
     */
    data class MCPError(
        val code: Int,
        val message: String,
        val data: Any? = null
    )

    /**
     * MCP инструмент
     */
    data class MCPTool(
        val name: String,
        val description: String,
        val inputSchema: OrgJSONObject? = null
    )

    /**
     * MCP клиент для работы с удаленным MCP сервером через HTTP
     */
    class MCPClient(
        private val baseUrl: String,
        private val apiKey: String,
        private val secretKey: String
    ) {
        private val httpClient = OkHttpClient()
        private var messageId = 0

        /**
         * Инициализирует HTTP подключение к Fusion Brain API
         */
        suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
            try {
                Log.d("mylog", "MCPProtocol: Начинаем подключение к Fusion Brain API")
                Log.d("mylog", "MCPProtocol: URL: $baseUrl")
                Log.d("mylog", "MCPProtocol: API Key: ${apiKey.take(8)}...")
                Log.d("mylog", "MCPProtocol: Secret Key: ${secretKey.take(8)}...")
                
                // Пропускаем проверку доступности - сразу пробуем использовать API
                Log.d("mylog", "MCPProtocol: Пропускаем проверку доступности, сразу используем API")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("mylog", "MCPProtocol: Ошибка подключения к Fusion Brain API", e)
                Result.failure(e)
            }
        }



        /**
         * Закрывает HTTP соединение
         */
        suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
            try {
                httpClient.dispatcher.executorService.shutdown()
                httpClient.connectionPool.evictAll()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка закрытия HTTP соединения", e)
                Result.failure(e)
            }
        }

        /**
         * Инициализирует MCP сервер
         */
        private suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
            try {
                val initMessage = MCPMessage(
                    id = generateMessageId(),
                    method = MESSAGE_TYPE_INIT,
                    params = OrgJSONObject().apply {
                        put("protocolVersion", "2024-11-05")
                        put("capabilities", OrgJSONObject())
                        put("clientInfo", OrgJSONObject().apply {
                            put("name", "ai_advent_25")
                            put("version", "1.0.0")
                        })
                    }
                )

                val response = sendMessage(initMessage)
                if (response.isSuccess) {
                    Log.d(TAG, "MCP сервер инициализирован")
                    Result.success(Unit)
                } else {
                    Result.failure(response.exceptionOrNull() ?: Exception("Ошибка инициализации"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка инициализации MCP", e)
                Result.failure(e)
            }
        }

        /**
         * Получает список доступных инструментов
         */
        suspend fun listTools(): Result<List<MCPTool>> = withContext(Dispatchers.IO) {
            try {
                val toolsMessage = MCPMessage(
                    id = generateMessageId(),
                    method = MESSAGE_TYPE_TOOLS
                )

                val response = sendMessage(toolsMessage)
                if (response.isSuccess) {
                    val result = response.getOrNull()
                    if (result is OrgJSONObject && result.has("tools")) {
                        val toolsArray = result.getJSONArray("tools")
                        val tools = mutableListOf<MCPTool>()
                        
                        for (i in 0 until toolsArray.length()) {
                            val toolObj = toolsArray.getJSONObject(i)
                            tools.add(MCPTool(
                                name = toolObj.getString("name"),
                                description = toolObj.optString("description", ""),
                                inputSchema = toolObj.optJSONObject("inputSchema")
                            ))
                        }
                        
                        Result.success(tools)
                    } else {
                        Result.failure(Exception("Неверный формат ответа tools/list"))
                    }
                } else {
                    Result.failure(response.exceptionOrNull() ?: Exception("Ошибка получения инструментов"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка получения списка инструментов", e)
                Result.failure(e)
            }
        }

        /**
         * Вызывает MCP инструмент
         */
        suspend fun callTool(
            toolName: String,
            arguments: Map<String, Any>
        ): Result<String> = withContext(Dispatchers.IO) {
            try {
                val callMessage = MCPMessage(
                    id = generateMessageId(),
                    method = MESSAGE_TYPE_CALL,
                    params = OrgJSONObject().apply {
                        put("name", toolName)
                        put("arguments", OrgJSONObject(arguments))
                    }
                )

                val response = sendMessage(callMessage)
                if (response.isSuccess) {
                    val result = response.getOrNull()
                    if (result is OrgJSONObject) {
                        // Обрабатываем ответ от Fusion Brain API
                        if (result.has("uuid") && result.has("status")) {
                            val uuid = result.getString("uuid")
                            val status = result.getString("status")
                            Log.d("mylog", "MCPProtocol: Получен ответ от Fusion Brain API: uuid=$uuid, status=$status")
                            
                            when (status) {
                                "INITIAL" -> {
                                    // Задача создана успешно, возвращаем uuid для отслеживания
                                    Result.success("Задача создана успешно: $uuid")
                                }
                                "DONE" -> {
                                    // Задача уже завершена, возвращаем успех
                                    Log.d("mylog", "MCPProtocol: Задача уже завершена: $uuid")
                                    
                                    // Получаем изображение из результата
                                    if (result.has("result") && result.getJSONObject("result").has("files")) {
                                        val files = result.getJSONObject("result").getJSONArray("files")
                                        if (files.length() > 0) {
                                            val base64Image = files.getString(0)
                                            Log.d("mylog", "MCPProtocol: Получено изображение в base64 (${base64Image.length} символов)")
                                            
                                            // Сохраняем изображение локально
                                            val imagePath = saveBase64Image(base64Image, uuid)
                                            if (imagePath != null) {
                                                Log.d("mylog", "MCPProtocol: Изображение сохранено: $imagePath")
                                                Result.success("Изображение готово: $imagePath")
                                            } else {
                                                Result.failure(Exception("Не удалось сохранить изображение"))
                                            }
                                        } else {
                                            Result.failure(Exception("Файлы изображения не найдены"))
                                        }
                                    } else {
                                        Result.failure(Exception("Результат не содержит файлы изображения"))
                                    }
                                }
                                else -> {
                                    Result.failure(Exception("Неожиданный статус: $status"))
                                }
                            }
                        } else {
                            Result.failure(Exception("Неверный формат ответа Fusion Brain API"))
                        }
                    } else {
                        Result.failure(Exception("Неверный тип ответа"))
                    }
                } else {
                    Result.failure(response.exceptionOrNull() ?: Exception("Ошибка вызова инструмента"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка вызова инструмента $toolName", e)
                Result.failure(e)
            }
        }

        /**
         * Отправляет запрос к Fusion Brain API для генерации изображения
         */
        private suspend fun sendMessage(message: MCPMessage): Result<OrgJSONObject> = withContext(Dispatchers.IO) {
            try {
                Log.d("mylog", "MCPProtocol: Начинаем генерацию изображения")
                Log.d("mylog", "MCPProtocol: Параметры сообщения: ${message.params}")
                
                // Получаем pipeline_id для Kandinsky
                Log.d("mylog", "MCPProtocol: Запрашиваем список pipelines")
                val pipelinesUrl = "${baseUrl}/key/api/v1/pipelines"
                Log.d("mylog", "MCPProtocol: URL pipelines: $pipelinesUrl")
                
                val pipelineRequest = Request.Builder()
                    .url(pipelinesUrl)
                    .addHeader("X-Key", "Key $apiKey")
                    .addHeader("X-Secret", "Secret $secretKey")
                    .build()

                val pipelineResponse = httpClient.newCall(pipelineRequest).execute()
                Log.d("mylog", "MCPProtocol: Ответ pipelines: ${pipelineResponse.code}")
                
                if (!pipelineResponse.isSuccessful) {
                    Log.e("mylog", "MCPProtocol: Ошибка получения pipeline: ${pipelineResponse.code}")
                    return@withContext Result.failure(Exception("Ошибка получения pipeline: ${pipelineResponse.code}"))
                }

                val pipelineData = pipelineResponse.body?.string()
                Log.d("mylog", "MCPProtocol: Данные pipeline: $pipelineData")
                
                if (pipelineData == null) {
                    Log.e("mylog", "MCPProtocol: Пустой ответ pipeline")
                    return@withContext Result.failure(Exception("Пустой ответ pipeline"))
                }

                // API возвращает массив напрямую, а не объект с полем "pipelines"
                val pipelineArray = OrgJSONArray(pipelineData)
                val pipelineId = pipelineArray.getJSONObject(0).getString("id") // используем "id" вместо "uuid"
                Log.d("mylog", "MCPProtocol: Получен pipeline_id: $pipelineId")

                // Создаем параметры для генерации изображения
                val prompt = if (message.params != null) {
                    try {
                        // message.params - это JSONObject, но нужно получить prompt из arguments
                        val arguments = message.params.getJSONObject("arguments")
                        arguments.getString("prompt")
                    } catch (e: Exception) {
                        Log.w("mylog", "MCPProtocol: Не удалось получить prompt из arguments, используем fallback", e)
                        "Beautiful city"
                    }
                } else {
                    "Beautiful city"
                }
                Log.d("mylog", "MCPProtocol: Используем prompt: $prompt")
                
                val params = OrgJSONObject().apply {
                    put("type", "GENERATE")
                    put("numImages", 1)
                    put("width", 1024)
                    put("height", 1024)
                    put("generateParams", OrgJSONObject().apply {
                        put("query", prompt)
                    })
                }
                Log.d("mylog", "MCPProtocol: Созданы параметры генерации: $params")

                // Отправляем запрос на генерацию изображения
                Log.d("mylog", "MCPProtocol: Отправляем запрос на генерацию")
                val formData = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("pipeline_id", pipelineId)
                    .addFormDataPart("params", params.toString(), 
                        params.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val generateUrl = "${baseUrl}/key/api/v1/pipeline/run"
                Log.d("mylog", "MCPProtocol: URL генерации: $generateUrl")
                
                val generateRequest = Request.Builder()
                    .url(generateUrl)
                    .addHeader("X-Key", "Key $apiKey")
                    .addHeader("X-Secret", "Secret $secretKey")
                    .post(formData)
                    .build()

                Log.d("mylog", "MCPProtocol: URL генерации: $baseUrl/key/api/v1/pipeline/run")
                val generateResponse = httpClient.newCall(generateRequest).execute()
                Log.d("mylog", "MCPProtocol: Ответ генерации: ${generateResponse.code}")
                
                if (generateResponse.isSuccessful) {
                    val responseBody = generateResponse.body?.string()
                    Log.d("mylog", "MCPProtocol: Тело ответа генерации: $responseBody")
                    
                    if (responseBody != null) {
                        Log.d("mylog", "MCPProtocol: Генерация успешна")
                        val responseJson = OrgJSONObject(responseBody)
                        
                        // Теперь нужно дождаться завершения генерации и получить изображение
                        val uuid = responseJson.getString("uuid")
                        Log.d("mylog", "MCPProtocol: Ожидаем завершения генерации для uuid: $uuid")
                        
                        val finalResult = waitForImageGeneration(uuid)
                        if (finalResult.isSuccess) {
                            Log.d("mylog", "MCPProtocol: Изображение успешно сгенерировано")
                            Result.success(finalResult.getOrNull() ?: responseJson)
                        } else {
                            Log.e("mylog", "MCPProtocol: Ошибка ожидания генерации")
                            Result.failure(finalResult.exceptionOrNull() ?: Exception("Ошибка ожидания генерации"))
                        }
                    } else {
                        Log.e("mylog", "MCPProtocol: Пустой ответ генерации")
                        Result.failure(Exception("Пустой ответ генерации"))
                    }
                } else {
                    Log.e("mylog", "MCPProtocol: Ошибка генерации: ${generateResponse.code}")
                    Result.failure(Exception("Ошибка генерации: ${generateResponse.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка отправки запроса к Fusion Brain API", e)
                Result.failure(e)
            }
        }

        /**
         * Генерирует уникальный ID сообщения
         */
        private fun generateMessageId(): String {
            return "msg_${++messageId}"
        }

        /**
         * Сохраняет base64 изображение в файл
         */
        private fun saveBase64Image(base64Data: String, uuid: String): String? {
            try {
                Log.d("mylog", "MCPProtocol: Сохраняем base64 изображение")
                
                // Декодируем base64
                val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                Log.d("mylog", "MCPProtocol: Декодировано ${imageBytes.size} байт")
                
                // Создаем имя файла
                val filename = "kandinsky_${uuid}.png"
                val file = java.io.File("/data/user/0/com.example.ai_advent_25/files", filename)
                
                // Создаем директорию если не существует
                file.parentFile?.mkdirs()
                
                // Записываем файл
                file.writeBytes(imageBytes)
                Log.d("mylog", "MCPProtocol: Файл сохранен: ${file.absolutePath}")
                
                return file.absolutePath
            } catch (e: Exception) {
                Log.e("mylog", "MCPProtocol: Ошибка сохранения изображения", e)
                return null
            }
        }

        /**
         * Ожидает завершения генерации изображения и получает результат
         */
        private suspend fun waitForImageGeneration(uuid: String): Result<OrgJSONObject> = withContext(Dispatchers.IO) {
            try {
                Log.d("mylog", "MCPProtocol: Начинаем ожидание генерации для uuid: $uuid")
                
                var attempts = 30 // максимум 30 попыток (5 минут)
                var delay = 10000L // 10 секунд между попытками
                
                while (attempts > 0) {
                    Log.d("mylog", "MCPProtocol: Проверяем статус генерации, попытка ${31 - attempts}")
                    
                    val statusUrl = "${baseUrl}/key/api/v1/pipeline/status/$uuid"
                    val statusRequest = Request.Builder()
                        .url(statusUrl)
                        .addHeader("X-Key", "Key $apiKey")
                        .addHeader("X-Secret", "Secret $secretKey")
                        .build()
                    
                    val statusResponse = httpClient.newCall(statusRequest).execute()
                    if (statusResponse.isSuccessful) {
                        val statusBody = statusResponse.body?.string()
                        Log.d("mylog", "MCPProtocol: Статус генерации: $statusBody")
                        
                        if (statusBody != null) {
                            val statusJson = OrgJSONObject(statusBody)
                            val status = statusJson.getString("status")
                            
                            when (status) {
                                "DONE" -> {
                                    Log.d("mylog", "MCPProtocol: Генерация завершена успешно")
                                    return@withContext Result.success(statusJson)
                                }
                                "FAIL" -> {
                                    val errorDescription = statusJson.optString("errorDescription", "Неизвестная ошибка")
                                    Log.e("mylog", "MCPProtocol: Генерация завершилась с ошибкой: $errorDescription")
                                    return@withContext Result.failure(Exception("Генерация завершилась с ошибкой: $errorDescription"))
                                }
                                "PROCESSING" -> {
                                    Log.d("mylog", "MCPProtocol: Генерация в процессе, ждем...")
                                }
                                "INITIAL" -> {
                                    Log.d("mylog", "MCPProtocol: Генерация в очереди, ждем...")
                                }
                                else -> {
                                    Log.d("mylog", "MCPProtocol: Неожиданный статус: $status")
                                }
                            }
                        }
                    } else {
                        Log.w("mylog", "MCPProtocol: Ошибка проверки статуса: ${statusResponse.code}")
                    }
                    
                    attempts--
                    if (attempts > 0) {
                        delay(delay)
                    }
                }
                
                Log.e("mylog", "MCPProtocol: Превышено время ожидания генерации")
                Result.failure(Exception("Превышено время ожидания генерации"))
            } catch (e: Exception) {
                Log.e("mylog", "MCPProtocol: Ошибка ожидания генерации", e)
                Result.failure(e)
            }
        }

        /**
         * Проверяет, доступен ли Fusion Brain API
         */
        suspend fun isRunning(): Boolean = withContext(Dispatchers.IO) {
            try {
                Log.d("mylog", "MCPProtocol: Проверяем доступность API")
                val availabilityUrl = "${baseUrl}/key/api/v1/pipeline/availability"
                Log.d("mylog", "MCPProtocol: URL проверки: $availabilityUrl")
                
                val request = Request.Builder()
                    .url(availabilityUrl)
                    .addHeader("X-Key", "Key $apiKey")
                    .addHeader("X-Secret", "Secret $secretKey")
                    .build()

                val response = httpClient.newCall(request).execute()
                Log.d("mylog", "MCPProtocol: Результат проверки доступности: ${response.isSuccessful}")
                response.isSuccessful
            } catch (e: Exception) {
                Log.e("mylog", "MCPProtocol: Ошибка проверки доступности Fusion Brain API", e)
                false
            }
        }
    }
}
