package com.example.ai_advent_25.data.agents

import android.util.Log
import com.example.ai_advent_25.data.testing.MicroJUnitEngine
import com.example.ai_advent_25.data.testing.MicroJUnitTestResult
import com.example.ai_advent_25.data.network.api.ChatApi
import com.example.ai_advent_25.data.*
import com.example.ai_advent_25.data.network.NetworkConstants
import com.example.ai_advent_25.data.network.NetworkProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Агент для генерации и выполнения тестов через LLM
 * Использует микро-JUnit движок для выполнения тестов в runtime
 */
class LLMTestGeneratorAgentRepository(
    private val apiKey: String,
    private val networkProvider: NetworkProvider,
    private val microJUnitEngine: MicroJUnitEngine
) {
    
    private val chatApi: ChatApi by lazy {
        networkProvider.getChatApi()
    }
    
    companion object {
        private const val TAG = "LLMTestGeneratorAgentRepository"
    }
    
    /**
     * Генерирует и выполняет тесты для указанного класса
     */
    suspend fun generateAndRunTests(
        targetClass: Class<*>,
        llmPrompt: String
    ): Result<MicroJUnitTestResult> {
        Log.d(TAG, "Генерируем и выполняем тесты для класса: ${targetClass.simpleName}")
        
        return try {
            withContext(Dispatchers.IO) {
                // 1. Генерируем тесты через LLM
                val generatedTestCode = generateTestsViaLLM(targetClass, llmPrompt)
                
                // 2. Выполняем сгенерированные тесты через микро-JUnit движок
                val testResult = microJUnitEngine.runLLMGeneratedTests(generatedTestCode, targetClass)
                
                testResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при генерации и выполнении тестов", e)
            Result.failure(e)
        }
    }
    
    /**
     * Генерирует тесты для указанного класса через LLM
     */
    private suspend fun generateTestsViaLLM(
        targetClass: Class<*>,
        prompt: String
    ): String {
        return try {
            Log.d(TAG, "Отправляем запрос в LLM для генерации тестов для ${targetClass.simpleName}")
            
            // Создаем детальный промпт для LLM
            val llmPrompt = createLLMPrompt(targetClass, prompt)
            
            // Отправляем запрос в LLM
            if (apiKey.isBlank()) {
                Log.w(TAG, "API ключ пустой, используем заглушку")
                return getFallbackTests(targetClass)
            }
            
            val response = chatApi.sendMessage(
                authorization = "Api-Key $apiKey",
                folderId = NetworkConstants.FOLDER_ID,
                request = ChatRequest(
                    messages = listOf(
                        ChatMessage(
                            role = "user",
                            text = llmPrompt
                        )
                    )
                )
            )
            
            val generatedCode = response.result.alternatives.firstOrNull()?.message?.text ?: ""
            
            Log.d(TAG, "LLM сгенерировал тесты: $generatedCode")
            
            // Если LLM ответ пустой, используем заглушку
            if (generatedCode.isBlank()) {
                Log.w(TAG, "LLM вернул пустой ответ, используем заглушку")
                return getFallbackTests(targetClass)
            }
            
            generatedCode
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при генерации тестов через LLM: ${e.message}", e)
            // В случае ошибки используем заглушку
            return getFallbackTests(targetClass)
        }
    }
    
    /**
     * Возвращает заглушку тестов в случае ошибки LLM
     */
    private fun getFallbackTests(targetClass: Class<*>): String {
        return when (targetClass.simpleName) {
            "PasswordValidator" -> generatePasswordValidatorTests()
            "Calculator" -> generateCalculatorTests()
            else -> generateGenericTests(targetClass)
        }
    }
    
    /**
     * Генерирует тесты для PasswordValidator
     */
    private fun generatePasswordValidatorTests(): String {
        return """
            @Test
            fun `should be valid for correct password`() {
                val result = PasswordValidator.isValid("SecurePass123")
                assertThat(result).isTrue()
            }
            
            @Test
            fun `should be invalid for empty string`() {
                val result = PasswordValidator.isValid("")
                assertThat(result).isFalse()
            }
            
            @Test
            fun `should be invalid for blank string`() {
                val result = PasswordValidator.isValid("     ")
                assertThat(result).isFalse()
            }
            
            @Test
            fun `should be invalid for short password`() {
                val result = PasswordValidator.isValid("123")
                assertThat(result).isFalse()
            }
            
            @Test
            fun `should be valid for password with exactly 8 characters`() {
                val result = PasswordValidator.isValid("12345678")
                assertThat(result).isTrue()
            }
        """.trimIndent()
    }
    
    /**
     * Генерирует тесты для Calculator
     */
    private fun generateCalculatorTests(): String {
        return """
            @Test
            fun `should add two positive numbers`() {
                assertEquals(5, Calculator().add(2, 3))
            }
            
            @Test
            fun `should add negative and positive numbers`() {
                assertEquals(0, Calculator().add(-1, 1))
            }
            
            @Test
            fun `should multiply two numbers`() {
                assertEquals(20, Calculator().multiply(4, 5))
            }
            
            @Test
            fun `should subtract numbers correctly`() {
                assertEquals(3, Calculator().subtract(8, 5))
            }
            
            @Test
            fun `should handle zero multiplication`() {
                assertEquals(0, Calculator().multiply(10, 0))
            }
        """.trimIndent()
    }
    
    /**
     * Генерирует общие тесты для любого класса
     */
    private fun generateGenericTests(targetClass: Class<*>): String {
        return """
            @Test
            fun `should create instance successfully`() {
                val instance = ${targetClass.simpleName}()
                assertThat(instance).isNotNull()
            }
            
            @Test
            fun `should have correct class type`() {
                val instance = ${targetClass.simpleName}()
                assertThat(instance.javaClass).isEqualTo(${targetClass.simpleName}::class.java)
            }
        """.trimIndent()
    }
    
    /**
     * Получает информацию о классе для генерации тестов
     */
    suspend fun getClassInfo(targetClass: Class<*>): String {
        return try {
            withContext(Dispatchers.IO) {
                val methods = targetClass.declaredMethods
                    .filter { java.lang.reflect.Modifier.isPublic(it.modifiers) }
                    .map { "${it.name}(${it.parameterTypes.joinToString { it.simpleName }})" }
                
                val fields = targetClass.declaredFields
                    .filter { java.lang.reflect.Modifier.isPublic(it.modifiers) }
                    .map { it.name }
                
                buildString {
                    appendLine("Класс: ${targetClass.simpleName}")
                    appendLine("Пакет: ${targetClass.`package`.name}")
                    appendLine("Методы: ${methods.joinToString(", ")}")
                    appendLine("Поля: ${fields.joinToString(", ")}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении информации о классе", e)
            "Не удалось получить информацию о классе: ${e.message}"
        }
    }
    
    /**
     * Создает промпт для LLM на основе информации о классе
     */
    suspend fun createLLMPrompt(
        targetClass: Class<*>,
        testRequirements: String = "Создай JUnit тесты для проверки всех публичных методов"
    ): String {
        val classInfo = getClassInfo(targetClass)
        
        return """
            Создай JUnit тесты для следующего класса:
            
            $classInfo
            
            Требования к тестам:
            $testRequirements
            
            ВАЖНО! Используй ТОЛЬКО следующий синтаксис:
            
            @Test
            fun `название теста`() {
                assertEquals(ожидаемое_значение, ${targetClass.simpleName}().методКласса(параметры))
            }
            
            Пример правильного формата:
            @Test
            fun `should add two numbers`() {
                assertEquals(5, Calculator().add(2, 3))
            }
            
            @Test
            fun `should multiply numbers`() {
                assertEquals(20, Calculator().multiply(4, 5))
            }
            
            ВАЖНО для типов данных:
            - add/subtract/multiply/power возвращают Int: assertEquals(5, Calculator().add(2, 3))
            - divide возвращает Double: assertEquals(2.0, Calculator().divide(4, 2))
            
            НЕ используй переменные, НЕ используй статические вызовы!
            Каждый assert должен быть прямым вызовом assertEquals с конструктором класса.
            
            Верни только код тестов без дополнительных комментариев и форматирования.
        """.trimIndent()
    }
}
