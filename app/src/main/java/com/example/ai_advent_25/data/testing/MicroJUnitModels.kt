package com.example.ai_advent_25.data.testing

/**
 * Тип проверки (assert)
 */
enum class AssertionType {
    IS_TRUE,
    IS_FALSE,
    IS_EQUAL_TO,
    IS_NOT_EQUAL_TO,
    IS_NULL,
    IS_NOT_NULL,
    CONTAINS,
    UNKNOWN
}

/**
 * Тестовый случай, извлеченный из кода LLM
 */
data class MicroJUnitTestCase(
    val name: String,
    val assertions: MutableList<MicroJUnitAssertion>
)

/**
 * Проверка (assert) в тесте
 */
data class MicroJUnitAssertion(
    val type: AssertionType,
    val expectedValue: Any?,
    val actualExpression: String
)

/**
 * Результат выполнения одной проверки
 */
data class AssertionResult(
    val type: AssertionType,
    val success: Boolean,
    val expectedValue: Any?,
    val actualValue: Any?,
    val message: String
)

/**
 * Результат выполнения одного теста
 */
data class MicroJUnitSingleTestResult(
    val testName: String,
    val success: Boolean,
    val duration: Long? = null,
    val assertionResults: List<AssertionResult> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Общий результат выполнения всех тестов
 */
data class MicroJUnitTestResult(
    val success: Boolean,
    val testCount: Int,
    val passedTests: Int,
    val failedTests: Int,
    val testResults: List<MicroJUnitSingleTestResult>,
    val output: String
) {
    val summary: String
        get() = buildString {
            append("Тесты: $testCount")
            append(", Пройдено: $passedTests")
            append(", Провалено: $failedTests")
            append(", Статус: ${if (success) "УСПЕХ" else "ОШИБКА"}")
        }
}
