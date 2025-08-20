package com.example.ai_advent_25.data.testing

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Микро-JUnit движок для выполнения тестов в runtime
 * Парсит JUnit-подобный код от LLM и выполняет тесты
 */
class MicroJUnitEngine {
    
    companion object {
        private const val TAG = "MicroJUnitEngine"
    }
    
    /**
     * Запускает тесты, сгенерированные LLM
     */
    suspend fun runLLMGeneratedTests(
        testCode: String,
        targetClass: Class<*>
    ): Result<MicroJUnitTestResult> {
        Log.d(TAG, "Запускаем тесты, сгенерированные LLM")
        
        return try {
            withContext(Dispatchers.IO) {
                // 1. Парсим код тестов от LLM
                val parsedTests = parseTestCode(testCode)
                Log.d(TAG, "Парсинг завершен, найдено тестов: ${parsedTests.size}")
                
                // 2. Выполняем каждый тест
                val testResults = mutableListOf<MicroJUnitSingleTestResult>()
                var passedTests = 0
                var failedTests = 0
                
                parsedTests.forEach { testCase ->
                    val result = executeTestCase(testCase, targetClass)
                    testResults.add(result)
                    
                    if (result.success) {
                        passedTests++
                    } else {
                        failedTests++
                    }
                }
                
                val overallSuccess = failedTests == 0
                
                Log.d(TAG, "Тесты завершены. Всего: ${parsedTests.size}, Пройдено: $passedTests, Провалено: $failedTests")
                
                val result = MicroJUnitTestResult(
                    success = overallSuccess,
                    testCount = parsedTests.size,
                    passedTests = passedTests,
                    failedTests = failedTests,
                    testResults = testResults,
                    output = buildTestOutput(testResults)
                )
                
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запуске тестов LLM", e)
            Result.failure(e)
        }
    }
    
    /**
     * Парсит JUnit-подобный код от LLM
     */
    private fun parseTestCode(testCode: String): List<MicroJUnitTestCase> {
        val testCases = mutableListOf<MicroJUnitTestCase>()
        
        try {
            // Разбиваем код на строки
            val lines = testCode.lines()
            var currentTest: MicroJUnitTestCase? = null
            var currentLineIndex = 0
            
            while (currentLineIndex < lines.size) {
                val line = lines[currentLineIndex]
                val trimmedLine = line.trim()
                
                // Ищем аннотацию @Test
                if (trimmedLine.startsWith("@Test")) {
                    // Завершаем предыдущий тест, если есть
                    currentTest?.let { testCases.add(it) }
                    
                    // Ищем название теста в следующих строках
                    var testName = extractTestName(trimmedLine)
                    
                    // Если название не найдено в аннотации, ищем в следующей строке с функцией
                    if (testName.startsWith("test_")) {
                        for (i in (currentLineIndex + 1) until minOf(currentLineIndex + 3, lines.size)) {
                            val nextLine = lines[i].trim()
                            if (nextLine.contains("fun `") && nextLine.contains("`()")) {
                                // Извлекаем название из backticks
                                val nameRegex = """fun `([^`]+)`\(\)""".toRegex()
                                val nameMatch = nameRegex.find(nextLine)
                                if (nameMatch != null) {
                                    testName = nameMatch.groupValues[1]
                                    break
                                }
                            } else if (nextLine.contains("fun ") && nextLine.contains("()")) {
                                // Извлекаем обычное название функции
                                val nameRegex = """fun (\w+)\(\)""".toRegex()
                                val nameMatch = nameRegex.find(nextLine)
                                if (nameMatch != null) {
                                    testName = nameMatch.groupValues[1]
                                    break
                                }
                            }
                        }
                    }
                    
                    // Начинаем новый тест
                    currentTest = MicroJUnitTestCase(
                        name = testName,
                        assertions = mutableListOf()
                    )
                }
                
                // Ищем assertThat вызовы
                if (trimmedLine.contains("assertThat") && currentTest != null) {
                    val assertion = parseAssertion(trimmedLine)
                    currentTest.assertions.add(assertion)
                }
                
                // Ищем assertEquals вызовы
                if (trimmedLine.contains("assertEquals") && currentTest != null) {
                    val assertion = parseEqualsAssertion(trimmedLine)
                    currentTest.assertions.add(assertion)
                }
                
                // Ищем assertTrue/assertFalse вызовы
                if ((trimmedLine.contains("assertTrue") || trimmedLine.contains("assertFalse")) && currentTest != null) {
                    val assertion = parseBooleanAssertion(trimmedLine)
                    currentTest.assertions.add(assertion)
                }
                
                currentLineIndex++
            }
            
            // Добавляем последний тест
            currentTest?.let { testCases.add(it) }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при парсинге кода тестов", e)
        }
        
        return testCases
    }
    
    /**
     * Извлекает название теста из аннотации @Test и следующей строки с функцией
     */
    private fun extractTestName(testLine: String): String {
        return try {
            // Ищем название теста в кавычках после @Test
            val regex = """@Test\s*(?:\(["']([^"']+)["']\))?\s*""".toRegex()
            val matchResult = regex.find(testLine)
            
            if (matchResult != null && matchResult.groupValues.size > 1 && matchResult.groupValues[1].isNotEmpty()) {
                matchResult.groupValues[1]
            } else {
                "test_${System.currentTimeMillis()}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при извлечении названия теста", e)
            "test_${System.currentTimeMillis()}"
        }
    }
    
    /**
     * Парсит assertThat вызов
     */
    private fun parseAssertion(assertLine: String): MicroJUnitAssertion {
        return try {
            // Простой парсинг assertThat
            when {
                assertLine.contains(".isTrue()") -> {
                    MicroJUnitAssertion(
                        type = AssertionType.IS_TRUE,
                        expectedValue = true,
                        actualExpression = extractActualExpression(assertLine)
                    )
                }
                assertLine.contains(".isFalse()") -> {
                    MicroJUnitAssertion(
                        type = AssertionType.IS_FALSE,
                        expectedValue = false,
                        actualExpression = extractActualExpression(assertLine)
                    )
                }
                assertLine.contains(".isEqualTo(") -> {
                    val expectedValue = extractExpectedValue(assertLine)
                    MicroJUnitAssertion(
                        type = AssertionType.IS_EQUAL_TO,
                        expectedValue = expectedValue,
                        actualExpression = extractActualExpression(assertLine)
                    )
                }
                else -> {
                    MicroJUnitAssertion(
                        type = AssertionType.UNKNOWN,
                        expectedValue = null,
                        actualExpression = "unknown"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при парсинге assertThat", e)
            MicroJUnitAssertion(
                type = AssertionType.UNKNOWN,
                expectedValue = null,
                actualExpression = "error"
            )
        }
    }
    
    /**
     * Парсит assertEquals вызов
     */
    private fun parseEqualsAssertion(assertLine: String): MicroJUnitAssertion {
        return try {
            Log.d(TAG, "Parsing assertEquals: $assertLine")
            
            // assertEquals(expected, actual) - извлекаем оба значения
            // Нужно правильно обработать вложенные скобки в actual
            val trimmedLine = assertLine.trim()
            if (!trimmedLine.startsWith("assertEquals(")) {
                throw IllegalArgumentException("Not an assertEquals call")
            }
            
            // Убираем "assertEquals(" в начале
            val content = trimmedLine.substring("assertEquals(".length)
            
            // Ищем запятую, которая разделяет expected и actual
            var commaIndex = -1
            var bracketCount = 0
            for (i in content.indices) {
                when (content[i]) {
                    '(' -> bracketCount++
                    ')' -> bracketCount--
                    ',' -> {
                        // Запятая на уровне 0 скобок - это разделитель
                        if (bracketCount == 0) {
                            commaIndex = i
                            break
                        }
                    }
                }
            }
            
            if (commaIndex == -1) {
                throw IllegalArgumentException("No comma found in assertEquals")
            }
            
            val expectedStr = content.substring(0, commaIndex).trim()
            val actualStr = content.substring(commaIndex + 1).trim()
            
            // Убираем последнюю закрывающую скобку из actual
            val actualExpression = if (actualStr.endsWith(")")) {
                actualStr.substring(0, actualStr.length - 1)
            } else {
                actualStr
            }
            
            val expectedValue = parseValue(expectedStr)
            
            Log.d(TAG, "assertEquals parsed - expected: $expectedValue, actual: $actualExpression")
            
            MicroJUnitAssertion(
                type = AssertionType.IS_EQUAL_TO,
                expectedValue = expectedValue,
                actualExpression = actualExpression
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing assertEquals: $assertLine", e)
            MicroJUnitAssertion(
                type = AssertionType.UNKNOWN,
                expectedValue = null,
                actualExpression = "error"
            )
        }
    }
    
    /**
     * Парсит значение из строки
     */
    private fun parseValue(valueStr: String): Any? {
        return try {
            when {
                valueStr.startsWith("\"") && valueStr.endsWith("\"") -> {
                    valueStr.substring(1, valueStr.length - 1) // Убираем кавычки
                }
                valueStr == "true" -> true
                valueStr == "false" -> false
                valueStr.toIntOrNull() != null -> valueStr.toInt()
                valueStr.toDoubleOrNull() != null -> valueStr.toDouble()
                else -> valueStr
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing value: $valueStr", e)
            valueStr
        }
    }
    
    /**
     * Парсит assertTrue/assertFalse вызов
     */
    private fun parseBooleanAssertion(assertLine: String): MicroJUnitAssertion {
        return try {
            val isTrue = assertLine.contains("assertTrue")
            MicroJUnitAssertion(
                type = if (isTrue) AssertionType.IS_TRUE else AssertionType.IS_FALSE,
                expectedValue = isTrue,
                actualExpression = extractActualExpression(assertLine)
            )
        } catch (e: Exception) {
            MicroJUnitAssertion(
                type = AssertionType.UNKNOWN,
                expectedValue = null,
                actualExpression = "error"
            )
        }
    }
    
    /**
     * Извлекает выражение для проверки из assert вызова
     */
    private fun extractActualExpression(assertLine: String): String {
        return try {
            Log.d(TAG, "Extracting actual expression from: $assertLine")
            
            // Ищем выражение в assertThat(expression) или assertTrue(expression)
            val regex = """(?:assertThat|assertTrue|assertFalse)\(([^)]+)\)""".toRegex()
            val matchResult = regex.find(assertLine)
            val result = matchResult?.groupValues?.get(1)?.trim() ?: "unknown"
            
            Log.d(TAG, "Extracted expression: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting expression from: $assertLine", e)
            "unknown"
        }
    }
    
    /**
     * Извлекает ожидаемое значение из assert вызова
     */
    private fun extractExpectedValue(assertLine: String): Any? {
        return try {
            // Ищем значение в isEqualTo(value) или assertEquals(expected, actual)
            val regex = """(?:isEqualTo|assertEquals)\(([^,)]+)(?:,\s*[^)]+)?\)""".toRegex()
            val matchResult = regex.find(assertLine)
            val valueStr = matchResult?.groupValues?.get(1)?.trim() ?: return null
            
            // Пытаемся преобразовать в соответствующий тип
            when {
                valueStr.startsWith("\"") && valueStr.endsWith("\"") -> {
                    valueStr.substring(1, valueStr.length - 1) // Убираем кавычки
                }
                valueStr == "true" -> true
                valueStr == "false" -> false
                valueStr.toIntOrNull() != null -> valueStr.toInt()
                valueStr.toDoubleOrNull() != null -> valueStr.toDouble()
                else -> valueStr
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Выполняет один тестовый случай
     */
    private fun executeTestCase(
        testCase: MicroJUnitTestCase,
        targetClass: Class<*>
    ): MicroJUnitSingleTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            Log.d(TAG, "Выполняем тест: ${testCase.name}")
            
            // Создаем экземпляр целевого класса
            val targetInstance = targetClass.getDeclaredConstructor().newInstance()
            
            // Выполняем все проверки в тесте
            val assertionResults = mutableListOf<AssertionResult>()
            var allPassed = true
            
            testCase.assertions.forEach { assertion ->
                val result = executeAssertion(assertion, targetInstance, targetClass)
                assertionResults.add(result)
                
                if (!result.success) {
                    allPassed = false
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "Тест ${testCase.name} завершен. Успех: $allPassed, время: ${duration}ms")
            
            MicroJUnitSingleTestResult(
                testName = testCase.name,
                success = allPassed,
                duration = duration,
                assertionResults = assertionResults,
                errorMessage = if (allPassed) null else "Один или несколько assert'ов не прошли"
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMessage = e.cause?.message ?: e.message ?: "Неизвестная ошибка"
            
            Log.e(TAG, "Тест ${testCase.name} провалился: $errorMessage", e)
            
            MicroJUnitSingleTestResult(
                testName = testCase.name,
                success = false,
                duration = duration,
                assertionResults = emptyList(),
                errorMessage = errorMessage
            )
        }
    }
    
    /**
     * Выполняет одну проверку (assert)
     */
    private fun executeAssertion(
        assertion: MicroJUnitAssertion,
        targetInstance: Any,
        targetClass: Class<*>
    ): AssertionResult {
        return try {
            Log.d(TAG, "Executing assertion: ${assertion.type}, expected: ${assertion.expectedValue}, expression: ${assertion.actualExpression}")
            
            // Вычисляем реальное значение выражения
            val actualValue = evaluateExpression(assertion.actualExpression, targetInstance, targetClass)
            Log.d(TAG, "Actual value computed: $actualValue")
            
            // Выполняем проверку в зависимости от типа
            val (success, message) = when (assertion.type) {
                AssertionType.IS_TRUE -> {
                    val isTrue = actualValue == true
                    isTrue to if (isTrue) "Значение равно true" else "Значение не равно true: $actualValue"
                }
                AssertionType.IS_FALSE -> {
                    val isFalse = actualValue == false
                    isFalse to if (isFalse) "Значение равно false" else "Значение не равно false: $actualValue"
                }
                AssertionType.IS_EQUAL_TO -> {
                    val isEqual = compareValues(assertion.expectedValue, actualValue)
                    isEqual to if (isEqual) "Значения равны: $actualValue" else "Ожидалось: ${assertion.expectedValue}, получено: $actualValue"
                }
                AssertionType.IS_NOT_EQUAL_TO -> {
                    val isNotEqual = actualValue != assertion.expectedValue
                    isNotEqual to if (isNotEqual) "Значения не равны: $actualValue != ${assertion.expectedValue}" else "Значения равны: $actualValue"
                }
                AssertionType.IS_NULL -> {
                    val isNull = actualValue == null
                    isNull to if (isNull) "Значение равно null" else "Значение не равно null: $actualValue"
                }
                AssertionType.IS_NOT_NULL -> {
                    val isNotNull = actualValue != null
                    isNotNull to if (isNotNull) "Значение не равно null: $actualValue" else "Значение равно null"
                }
                AssertionType.CONTAINS -> {
                    val contains = when {
                        actualValue is String && assertion.expectedValue is String -> 
                            actualValue.contains(assertion.expectedValue)
                        actualValue is Collection<*> && assertion.expectedValue != null -> 
                            actualValue.contains(assertion.expectedValue)
                        else -> false
                    }
                    contains to if (contains) "Содержит: ${assertion.expectedValue}" else "Не содержит: ${assertion.expectedValue}"
                }
                AssertionType.UNKNOWN -> {
                    false to "Неизвестный тип проверки"
                }
            }
            
            AssertionResult(
                type = assertion.type,
                success = success,
                expectedValue = assertion.expectedValue,
                actualValue = actualValue,
                message = message
            )
            
        } catch (e: Exception) {
            AssertionResult(
                type = assertion.type,
                success = false,
                expectedValue = assertion.expectedValue,
                actualValue = null,
                message = "Ошибка выполнения: ${e.message}"
            )
        }
    }
    
    /**
     * Вычисляет значение выражения
     */
    private fun evaluateExpression(
        expression: String,
        targetInstance: Any,
        targetClass: Class<*>
    ): Any? {
        return try {
            when {
                // Простые значения
                expression == "true" -> true
                expression == "false" -> false
                expression == "null" -> null
                expression.toIntOrNull() != null -> expression.toInt()
                expression.toDoubleOrNull() != null -> expression.toDouble()
                expression.startsWith("\"") && expression.endsWith("\"") -> 
                    expression.substring(1, expression.length - 1)
                
                // Вызовы методов (включая конструкторы с методами)
                expression.contains("(") -> {
                    evaluateMethodCall(expression, targetInstance, targetClass)
                }
                
                // Обращение к полям
                expression.contains(".") -> {
                    evaluateFieldAccess(expression, targetInstance, targetClass)
                }
                
                // Простые переменные (если есть в контексте)
                else -> {
                    // Пытаемся найти поле с таким именем
                    try {
                        val field = targetClass.getDeclaredField(expression)
                        field.isAccessible = true
                        field.get(targetInstance)
                    } catch (e: Exception) {
                        // Если не нашли, возвращаем как строку
                        expression
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при вычислении выражения: $expression", e)
            null
        }
    }
    
    /**
     * Вычисляет вызов метода
     */
    private fun evaluateMethodCall(
        expression: String,
        targetInstance: Any,
        targetClass: Class<*>
    ): Any? {
        return try {
            Log.d(TAG, "Evaluating method call: $expression")
            
            // Проверяем, есть ли вызов конструктора с методом (например, Calculator().add(2, 3))
            if (expression.contains("().")) {
                return evaluateConstructorMethodCall(expression)
            }
            
            // Проверяем, есть ли вызов через точку (например, PasswordValidator.isValid())
            if (expression.contains(".") && expression.contains("(")) {
                return evaluateStaticMethodCall(expression)
            }
            
            // Простой парсинг вызова метода: methodName(param1, param2)
            val regex = """(\w+)\(([^)]*)\)""".toRegex()
            val matchResult = regex.find(expression)
            
            if (matchResult != null) {
                val methodName = matchResult.groupValues[1]
                val paramsStr = matchResult.groupValues[2]
                
                // Парсим параметры
                val params = parseMethodParameters(paramsStr, targetInstance, targetClass)
                
                // Ищем метод
                val method = findMethod(targetClass, methodName, params.size)
                if (method != null) {
                    method.isAccessible = true
                    val result = method.invoke(targetInstance, *params.toTypedArray())
                    Log.d(TAG, "Method $methodName returned: $result")
                    result
                } else {
                    Log.w(TAG, "Метод не найден: $methodName с ${params.size} параметрами в классе ${targetClass.simpleName}")
                    null
                }
            } else {
                Log.w(TAG, "Не удалось распарсить вызов метода: $expression")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выполнении вызова метода: $expression", e)
            null
        }
    }
    
    /**
     * Вычисляет вызов метода через конструктор (например, Calculator().add(2, 3))
     */
    private fun evaluateConstructorMethodCall(expression: String): Any? {
        return try {
            Log.d(TAG, "Evaluating constructor method call: $expression")
            
            // Парсим выражение типа ClassName().methodName(params)
            val regex = """(\w+)\(\)\.(\w+)\(([^)]*)\)""".toRegex()
            val matchResult = regex.find(expression)
            
            if (matchResult != null) {
                val className = matchResult.groupValues[1]
                val methodName = matchResult.groupValues[2]
                val paramsStr = matchResult.groupValues[3]
                
                Log.d(TAG, "Parsed: class=$className, method=$methodName, params=$paramsStr")
                
                // Находим класс по имени
                val targetClass = findClassByName(className)
                if (targetClass != null) {
                    // Создаем экземпляр класса
                    val instance = targetClass.getDeclaredConstructor().newInstance()
                    
                    // Парсим параметры
                    val params = parseStaticMethodParameters(paramsStr)
                    
                    // Ищем метод
                    val method = findMethod(targetClass, methodName, params.size)
                    if (method != null) {
                        method.isAccessible = true
                        val result = method.invoke(instance, *params.toTypedArray())
                        Log.d(TAG, "Constructor method $className().$methodName returned: $result")
                        result
                    } else {
                        Log.w(TAG, "Метод не найден: $className.$methodName с ${params.size} параметрами")
                        null
                    }
                } else {
                    Log.w(TAG, "Класс не найден для конструктора: $className")
                    null
                }
            } else {
                Log.w(TAG, "Не удалось распарсить вызов конструктора: $expression")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выполнении вызова конструктора: $expression", e)
            null
        }
    }
    
    /**
     * Вычисляет статический вызов метода (например, PasswordValidator.isValid())
     */
    private fun evaluateStaticMethodCall(expression: String): Any? {
        return try {
            Log.d(TAG, "Evaluating static method call: $expression")
            
            // Парсим выражение типа ClassName.methodName(params)
            val regex = """(\w+)\.(\w+)\(([^)]*)\)""".toRegex()
            val matchResult = regex.find(expression)
            
            if (matchResult != null) {
                val className = matchResult.groupValues[1]
                val methodName = matchResult.groupValues[2]
                val paramsStr = matchResult.groupValues[3]
                
                // Находим класс по имени
                val targetClass = findClassByName(className)
                if (targetClass != null) {
                    // Парсим параметры
                    val params = parseStaticMethodParameters(paramsStr)
                    
                    // Ищем метод
                    val method = findMethod(targetClass, methodName, params.size)
                    if (method != null) {
                        method.isAccessible = true
                        
                        // Для object классов или статических методов
                        val instance = if (targetClass.kotlin.objectInstance != null) {
                            targetClass.kotlin.objectInstance
                        } else {
                            null
                        }
                        
                        val result = method.invoke(instance, *params.toTypedArray())
                        Log.d(TAG, "Static method $className.$methodName returned: $result")
                        result
                    } else {
                        Log.w(TAG, "Статический метод не найден: $className.$methodName с ${params.size} параметрами")
                        null
                    }
                } else {
                    Log.w(TAG, "Класс не найден: $className")
                    null
                }
            } else {
                Log.w(TAG, "Не удалось распарсить статический вызов: $expression")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при выполнении статического вызова: $expression", e)
            null
        }
    }
    
    /**
     * Находит класс по имени
     */
    private fun findClassByName(className: String): Class<*>? {
        return try {
            when (className) {
                "PasswordValidator" -> com.example.ai_advent_25.data.testing.PasswordValidator::class.java
                "Calculator" -> com.example.ai_advent_25.data.testing.Calculator::class.java
                "StringUtils" -> com.example.ai_advent_25.data.testing.StringUtils::class.java
                else -> {
                    Log.w(TAG, "Неизвестный класс: $className")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка поиска класса: $className", e)
            null
        }
    }
    
    /**
     * Парсит параметры для статических методов
     */
    private fun parseStaticMethodParameters(paramsStr: String): List<Any?> {
        if (paramsStr.isBlank()) return emptyList()
        
        return try {
            paramsStr.split(",").map { param ->
                val trimmedParam = param.trim()
                when {
                    trimmedParam.startsWith("\"") && trimmedParam.endsWith("\"") -> 
                        trimmedParam.substring(1, trimmedParam.length - 1)
                    trimmedParam == "true" -> true
                    trimmedParam == "false" -> false
                    trimmedParam.toIntOrNull() != null -> trimmedParam.toInt()
                    trimmedParam.toDoubleOrNull() != null -> trimmedParam.toDouble()
                    else -> trimmedParam
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при парсинге статических параметров: $paramsStr", e)
            emptyList()
        }
    }
    
    /**
     * Парсит параметры метода
     */
    private fun parseMethodParameters(
        paramsStr: String,
        targetInstance: Any,
        targetClass: Class<*>
    ): List<Any?> {
        if (paramsStr.isBlank()) return emptyList()
        
        return try {
            paramsStr.split(",").map { param ->
                val trimmedParam = param.trim()
                evaluateExpression(trimmedParam, targetInstance, targetClass)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при парсинге параметров: $paramsStr", e)
            emptyList()
        }
    }
    
    /**
     * Ищет метод по имени и количеству параметров
     */
    private fun findMethod(
        targetClass: Class<*>,
        methodName: String,
        paramCount: Int
    ): Method? {
        return targetClass.declaredMethods.find { method ->
            method.name == methodName && method.parameterCount == paramCount
        }
    }
    
    /**
     * Вычисляет обращение к полю
     */
    private fun evaluateFieldAccess(
        expression: String,
        targetInstance: Any,
        targetClass: Class<*>
    ): Any? {
        return try {
            val parts = expression.split(".")
            var currentInstance = targetInstance
            var currentClass = targetClass
            
            for (part in parts) {
                try {
                    val field = currentClass.getDeclaredField(part)
                    field.isAccessible = true
                    currentInstance = field.get(currentInstance) ?: return null
                    currentClass = currentInstance.javaClass
                } catch (e: Exception) {
                    Log.w(TAG, "Не удалось получить доступ к полю: $part", e)
                    return null
                }
            }
            
            currentInstance
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обращении к полю: $expression", e)
            null
        }
    }
    
    /**
     * Умно сравнивает значения разных типов чисел
     */
    private fun compareValues(expected: Any?, actual: Any?): Boolean {
        if (expected == actual) return true
        
        // Если оба значения null
        if (expected == null && actual == null) return true
        
        // Если одно из значений null
        if (expected == null || actual == null) return false
        
        // Специальная обработка для чисел
        return when {
            // Int vs Double: 2 == 2.0
            expected is Int && actual is Double -> expected.toDouble() == actual
            expected is Double && actual is Int -> expected == actual.toDouble()
            
            // Int vs Long: 2 == 2L
            expected is Int && actual is Long -> expected.toLong() == actual
            expected is Long && actual is Int -> expected == actual.toLong()
            
            // Double vs Long: 2.0 == 2L
            expected is Double && actual is Long -> expected == actual.toDouble()
            expected is Long && actual is Double -> expected.toDouble() == actual
            
            // Float vs Double: 2.0f == 2.0
            expected is Float && actual is Double -> expected.toDouble() == actual
            expected is Double && actual is Float -> expected == actual.toDouble()
            
            // Float vs Int: 2.0f == 2
            expected is Float && actual is Int -> expected == actual.toFloat()
            expected is Int && actual is Float -> expected.toFloat() == actual
            
            // Float vs Long: 2.0f == 2L
            expected is Float && actual is Long -> expected == actual.toFloat()
            expected is Long && actual is Float -> expected.toFloat() == actual
            
            // Для остальных случаев используем обычное сравнение
            else -> expected == actual
        }
    }
    
    /**
     * Строит текстовый вывод результатов тестов
     */
    private fun buildTestOutput(testResults: List<MicroJUnitSingleTestResult>): String {
        val output = StringBuilder()
        
        output.append("=== РЕЗУЛЬТАТЫ МИКРО-JUNIT ТЕСТОВ ===\n\n")
        
        testResults.forEach { result ->
            val status = if (result.success) "✅" else "❌"
            val duration = if (result.duration != null) " (${result.duration}ms)" else ""
            
            output.append("$status ${result.testName}$duration\n")
            
            if (!result.success && result.errorMessage != null) {
                output.append("    Ошибка: ${result.errorMessage}\n")
            }
            
            // Показываем результаты assert'ов
            result.assertionResults.forEach { assertion ->
                val assertStatus = if (assertion.success) "✓" else "✗"
                output.append("    $assertStatus ${assertion.type.name}: ${assertion.message}\n")
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
