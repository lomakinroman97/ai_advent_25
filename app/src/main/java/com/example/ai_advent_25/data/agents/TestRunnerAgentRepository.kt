package com.example.ai_advent_25.data.agents

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Агент для запуска Unit-тестов приложения
 * Запускает тесты локально в приложении без внешних зависимостей
 */
class TestRunnerAgentRepository(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "TestRunnerAgentRepository"
    }

    /**
     * Запускает все доступные тесты в приложении
     */
    suspend fun runAllTests(): Result<TestResult> {
        Log.d(TAG, "Начинаем запуск всех тестов")
        
        return try {
            withContext(Dispatchers.IO) {
                val testResults = mutableListOf<SingleTestResult>()
                var totalTests = 0
                var passedTests = 0
                var failedTests = 0
                
                // Получаем список всех тестовых классов
                val testClasses = findTestClasses()
                Log.d(TAG, "Найдено тестовых классов: ${testClasses.size}")
                
                // Запускаем тесты в каждом классе
                testClasses.forEach { testClass ->
                    val classResults = runTestClass(testClass)
                    testResults.addAll(classResults)
                    
                    classResults.forEach { result ->
                        totalTests++
                        if (result.success) {
                            passedTests++
                        } else {
                            failedTests++
                        }
                    }
                }
                
                val overallSuccess = failedTests == 0
                
                Log.d(TAG, "Все тесты завершены. Всего: $totalTests, Пройдено: $passedTests, Провалено: $failedTests")
                
                val result = TestResult(
                    success = overallSuccess,
                    exitCode = if (overallSuccess) 0 else 1,
                    output = buildTestOutput(testResults),
                    testCount = totalTests,
                    passedTests = passedTests,
                    failedTests = failedTests,
                    testResults = testResults
                )
                
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запуске тестов", e)
            Result.failure(e)
        }
    }

    /**
     * Запускает конкретный тест по имени класса
     */
    suspend fun runSpecificTest(testClassName: String): Result<TestResult> {
        Log.d(TAG, "Запускаем конкретный тест: $testClassName")
        
        return try {
            withContext(Dispatchers.IO) {
                val testClass = try {
                    Class.forName(testClassName)
                } catch (e: ClassNotFoundException) {
                    Log.e(TAG, "Тестовый класс не найден: $testClassName", e)
                    return@withContext Result.failure(Exception("Тестовый класс не найден: $testClassName"))
                }
                
                val testResults = runTestClass(testClass)
                val totalTests = testResults.size
                val passedTests = testResults.count { it.success }
                val failedTests = totalTests - passedTests
                val overallSuccess = failedTests == 0
                
                Log.d(TAG, "Тест $testClassName завершен. Всего: $totalTests, Пройдено: $passedTests, Провалено: $failedTests")
                
                val result = TestResult(
                    success = overallSuccess,
                    exitCode = if (overallSuccess) 0 else 1,
                    output = buildTestOutput(testResults),
                    testCount = totalTests,
                    passedTests = passedTests,
                    failedTests = failedTests,
                    testClassName = testClassName,
                    testResults = testResults
                )
                
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запуске теста $testClassName", e)
            Result.failure(e)
        }
    }

    /**
     * Ищет все тестовые классы в приложении
     */
    private fun findTestClasses(): List<Class<*>> {
        val testClasses = mutableListOf<Class<*>>()
        
        try {
            // Добавляем runtime тесты, которые доступны в main коде
            val runtimeTestClass = SimpleTests::class.java
            testClasses.add(runtimeTestClass)
            Log.d(TAG, "Добавлен runtime тестовый класс: ${runtimeTestClass.simpleName}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при поиске тестовых классов", e)
        }
        
        return testClasses
    }

    /**
     * Запускает все тесты в конкретном классе
     */
    private fun runTestClass(testClass: Class<*>): List<SingleTestResult> {
        val results = mutableListOf<SingleTestResult>()
        
        try {
            // Создаем экземпляр тестового класса
            val testInstance = testClass.getDeclaredConstructor().newInstance()
            
            // Ищем методы с аннотацией @Test
            val testMethods = testClass.declaredMethods.filter { method ->
                method.isAnnotationPresent(org.junit.Test::class.java) &&
                Modifier.isPublic(method.modifiers) &&
                method.parameterCount == 0
            }
            
            Log.d(TAG, "Найдено тестовых методов в ${testClass.simpleName}: ${testMethods.size}")
            
            // Запускаем каждый тест
            testMethods.forEach { method ->
                val result = runSingleTest(testInstance, method)
                results.add(result)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запуске тестового класса ${testClass.simpleName}", e)
            results.add(
                SingleTestResult(
                    className = testClass.simpleName,
                    methodName = "class_initialization",
                    success = false,
                    errorMessage = "Ошибка инициализации класса: ${e.message}"
                )
            )
        }
        
        return results
    }

    /**
     * Запускает один тест
     */
    private fun runSingleTest(testInstance: Any, method: Method): SingleTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            Log.d(TAG, "Запускаем тест: ${method.name}")
            
            // Запускаем JUnit тест
            method.invoke(testInstance)
            
            val duration = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Тест ${method.name} прошел успешно за ${duration}ms")
            
            SingleTestResult(
                className = testInstance.javaClass.simpleName,
                methodName = method.name,
                success = true,
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMessage = e.cause?.message ?: e.message ?: "Неизвестная ошибка"
            
            Log.e(TAG, "Тест ${method.name} провалился: $errorMessage", e)
            
            SingleTestResult(
                className = testInstance.javaClass.simpleName,
                methodName = method.name,
                success = false,
                errorMessage = errorMessage,
                duration = duration
            )
        }
    }

    /**
     * Строит текстовый вывод результатов тестов
     */
    private fun buildTestOutput(testResults: List<SingleTestResult>): String {
        val output = StringBuilder()
        
        output.append("=== РЕЗУЛЬТАТЫ ТЕСТОВ ===\n\n")
        
        // Группируем результаты по классам
        val groupedResults = testResults.groupBy { it.className }
        
        groupedResults.forEach { (className, results) ->
            output.append("📁 $className:\n")
            
            results.forEach { result ->
                val status = if (result.success) "✅" else "❌"
                val duration = if (result.duration != null) " (${result.duration}ms)" else ""
                
                output.append("  $status ${result.methodName}$duration\n")
                
                if (!result.success && result.errorMessage != null) {
                    output.append("    Ошибка: ${result.errorMessage}\n")
                }
            }
            
            output.append("\n")
        }
        
        // Общая статистика
        val total = testResults.size
        val passed = testResults.count { it.success }
        val failed = total - passed
        
        output.append("=== СТАТИСТИКА ===\n")
        output.append("Всего тестов: $total\n")
        output.append("Пройдено: $passed\n")
        output.append("Провалено: $failed\n")
        output.append("Статус: ${if (failed == 0) "УСПЕХ" else "ЕСТЬ ОШИБКИ"}\n")
        
        return output.toString()
    }
}

/**
 * Результат выполнения одного теста
 */
data class SingleTestResult(
    val className: String,
    val methodName: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val duration: Long? = null
)

/**
 * Результат выполнения всех тестов
 */
data class TestResult(
    val success: Boolean,
    val exitCode: Int,
    val output: String,
    val testCount: Int,
    val passedTests: Int,
    val failedTests: Int,
    val testClassName: String? = null,
    val testResults: List<SingleTestResult> = emptyList()
) {
    val summary: String
        get() = buildString {
            append("Тесты: $testCount")
            append(", Пройдено: $passedTests")
            append(", Провалено: $failedTests")
            append(", Статус: ${if (success) "УСПЕХ" else "ОШИБКА"}")
        }
}
