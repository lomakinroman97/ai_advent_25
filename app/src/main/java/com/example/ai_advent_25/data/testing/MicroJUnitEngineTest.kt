package com.example.ai_advent_25.data.testing

import android.util.Log
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/**
 * Тест для проверки работы микро-JUnit движка
 */
class MicroJUnitEngineTest {
    
    companion object {
        private const val TAG = "MicroJUnitEngineTest"
    }
    
    /**
     * Тест: парсинг простого JUnit кода
     */
    @Test
    fun testParseSimpleJUnitCode() {
        Log.d(TAG, "Тестируем парсинг простого JUnit кода")
        
        val testCode = """
            @Test
            fun `should be valid for correct password`() {
                val result = PasswordValidator.isValid("SecurePass123")
                assertThat(result).isTrue()
            }
        """.trimIndent()
        
        val engine = MicroJUnitEngine()
        
        runBlocking {
            val result = engine.runLLMGeneratedTests(testCode, PasswordValidator::class.java)
            
            assertTrue("Результат должен быть успешным", result.isSuccess)
            
            result.onSuccess { testResult ->
                assertEquals("Должен быть 1 тест", 1, testResult.testCount)
                assertEquals("Должен быть 1 пройденный тест", 1, testResult.passedTests)
                assertEquals("Должно быть 0 проваленных тестов", 0, testResult.failedTests)
                assertTrue("Общий статус должен быть успешным", testResult.success)
                
                val singleTest = testResult.testResults.first()
                assertEquals("Название теста должно совпадать", "should be valid for correct password", singleTest.testName)
                assertTrue("Тест должен быть успешным", singleTest.success)
            }
        }
        
        Log.d(TAG, "Тест testParseSimpleJUnitCode прошел успешно")
    }
    
    /**
     * Тест: выполнение тестов для Calculator
     */
    @Test
    fun testCalculatorTests() {
        Log.d(TAG, "Тестируем выполнение тестов для Calculator")
        
        val testCode = """
            @Test
            fun `should add two numbers`() {
                val result = Calculator().add(2, 3)
                assertEquals(5, result)
            }
            
            @Test
            fun `should multiply numbers`() {
                val result = Calculator().multiply(4, 5)
                assertEquals(20, result)
            }
        """.trimIndent()
        
        val engine = MicroJUnitEngine()
        
        runBlocking {
            val result = engine.runLLMGeneratedTests(testCode, Calculator::class.java)
            
            assertTrue("Результат должен быть успешным", result.isSuccess)
            
            result.onSuccess { testResult ->
                assertEquals("Должно быть 2 теста", 2, testResult.testCount)
                assertEquals("Должно быть 2 пройденных теста", 2, testResult.passedTests)
                assertEquals("Должно быть 0 проваленных тестов", 0, testResult.failedTests)
                assertTrue("Общий статус должен быть успешным", testResult.success)
                
                // Проверяем первый тест
                val firstTest = testResult.testResults.find { it.testName == "should add two numbers" }
                assertNotNull("Первый тест должен существовать", firstTest)
                assertTrue("Первый тест должен быть успешным", firstTest!!.success)
                
                // Проверяем второй тест
                val secondTest = testResult.testResults.find { it.testName == "should multiply numbers" }
                assertNotNull("Второй тест должен существовать", secondTest)
                assertTrue("Второй тест должен быть успешным", secondTest!!.success)
            }
        }
        
        Log.d(TAG, "Тест testCalculatorTests прошел успешно")
    }
    
    /**
     * Тест: обработка проваленных тестов
     */
    @Test
    fun testFailedTests() {
        Log.d(TAG, "Тестируем обработку проваленных тестов")
        
        val testCode = """
            @Test
            fun `should fail intentionally`() {
                val result = PasswordValidator.isValid("short")
                assertThat(result).isTrue() // Должно быть false
            }
        """.trimIndent()
        
        val engine = MicroJUnitEngine()
        
        runBlocking {
            val result = engine.runLLMGeneratedTests(testCode, PasswordValidator::class.java)
            
            assertTrue("Результат должен быть успешным", result.isSuccess)
            
            result.onSuccess { testResult ->
                assertEquals("Должен быть 1 тест", 1, testResult.testCount)
                assertEquals("Должно быть 0 пройденных тестов", 0, testResult.passedTests)
                assertEquals("Должен быть 1 проваленный тест", 1, testResult.failedTests)
                assertFalse("Общий статус должен быть неуспешным", testResult.success)
                
                val singleTest = testResult.testResults.first()
                assertEquals("Название теста должно совпадать", "should fail intentionally", singleTest.testName)
                assertFalse("Тест должен быть неуспешным", singleTest.success)
                assertNotNull("Должно быть сообщение об ошибке", singleTest.errorMessage)
            }
        }
        
        Log.d(TAG, "Тест testFailedTests прошел успешно")
    }
    
    /**
     * Тест: проверка различных типов assert'ов
     */
    @Test
    fun testDifferentAssertTypes() {
        Log.d(TAG, "Тестируем различные типы assert'ов")
        
        val testCode = """
            @Test
            fun `test various assertions`() {
                val calculator = Calculator()
                
                // assertThat
                val sum = calculator.add(2, 3)
                assertThat(sum).isEqualTo(5)
                
                // assertTrue
                val isPositive = sum > 0
                assertTrue(isPositive)
                
                // assertFalse
                val isNegative = sum < 0
                assertFalse(isNegative)
            }
        """.trimIndent()
        
        val engine = MicroJUnitEngine()
        
        runBlocking {
            val result = engine.runLLMGeneratedTests(testCode, Calculator::class.java)
            
            assertTrue("Результат должен быть успешным", result.isSuccess)
            
            result.onSuccess { testResult ->
                assertEquals("Должен быть 1 тест", 1, testResult.testCount)
                assertTrue("Тест должен быть успешным", testResult.success)
                
                val singleTest = testResult.testResults.first()
                assertTrue("Тест должен быть успешным", singleTest.success)
                assertTrue("Должно быть несколько assert'ов", singleTest.assertionResults.size >= 3)
            }
        }
        
        Log.d(TAG, "Тест testDifferentAssertTypes прошел успешно")
    }
}
