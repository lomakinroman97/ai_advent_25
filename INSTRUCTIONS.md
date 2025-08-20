# Инструкции по использованию микро-JUnit движка

## 🚀 Быстрый старт

### 1. Запуск приложения
1. Откройте проект в Android Studio
2. Синхронизируйте Gradle файлы
3. Запустите приложение на эмуляторе или устройстве

### 2. Демонстрация работы
1. В приложении перейдите на экран тестирования
2. Выберите класс для тестирования (PasswordValidator, Calculator, StringUtils)
3. Нажмите "📝 Сгенерировать тесты" - появятся примеры тестов
4. Нажмите "▶️ Запустить тесты" - тесты выполнятся через микро-JUnit движок
5. Просмотрите результаты выполнения

## 🧪 Тестирование микро-JUnit движка

### Запуск unit-тестов
```bash
./gradlew testDebugUnitTest
```

### Запуск конкретного теста
```bash
./gradlew testDebugUnitTest --tests MicroJUnitEngineTest.testParseSimpleJUnitCode
```

## 📁 Структура проекта

```
app/src/main/java/com/example/ai_advent_25/
├── data/
│   ├── testing/                    # Микро-JUnit движок
│   │   ├── MicroJUnitEngine.kt    # Основной движок
│   │   ├── MicroJUnitModels.kt    # Модели данных
│   │   ├── TestClasses.kt         # Классы для тестирования
│   │   └── MicroJUnitEngineTest.kt # Тесты движка
│   └── agents/
│       └── LLMTestGeneratorAgentRepository.kt # Агент для LLM
└── ui/
    └── TestingScreen.kt           # UI для демонстрации
```

## 🔧 Использование в коде

### Базовое использование

```kotlin
val engine = MicroJUnitEngine()

val testCode = """
    @Test
    fun `should validate password`() {
        val result = PasswordValidator.isValid("SecurePass123")
        assertThat(result).isTrue()
    }
"""

val result = engine.runLLMGeneratedTests(testCode, PasswordValidator::class.java)
result.onSuccess { testResult ->
    println("Тестов выполнено: ${testResult.testCount}")
    println("Успешно: ${testResult.passedTests}")
    println("Провалено: ${testResult.failedTests}")
}
```

### Интеграция с LLM

```kotlin
val agent = LLMTestGeneratorAgentRepository(context, engine)

// Создание промпта для LLM
val prompt = agent.createLLMPrompt(
    targetClass = PasswordValidator::class.java,
    testRequirements = "Создай тесты для проверки валидации паролей"
)

// Генерация и выполнение тестов
val result = agent.generateAndRunTests(PasswordValidator::class.java, prompt)
```

## 📝 Поддерживаемые assert'ы

### assertThat
```kotlin
assertThat(result).isTrue()
assertThat(result).isFalse()
assertThat(result).isEqualTo(expectedValue)
```

### assertEquals
```kotlin
assertEquals(expectedValue, actualValue)
```

### assertTrue/assertFalse
```kotlin
assertTrue(condition)
assertFalse(condition)
```

## 🎯 Примеры тестов

### Тест для PasswordValidator
```kotlin
@Test
fun `should be valid for correct password`() {
    val result = PasswordValidator.isValid("SecurePass123")
    assertThat(result).isTrue()
}

@Test
fun `should be invalid for short password`() {
    val result = PasswordValidator.isValid("123")
    assertThat(result).isFalse()
}
```

### Тест для Calculator
```kotlin
@Test
fun `should add two numbers`() {
    val result = Calculator().add(2, 3)
    assertEquals(5, result)
}

@Test
fun `should handle division by zero`() {
    try {
        Calculator().divide(10, 0)
        assertTrue(false) // Должно было выбросить исключение
    } catch (e: IllegalArgumentException) {
        assertThat(e.message).contains("ноль")
    }
}
```

## 🔍 Отладка

### Логи
Все операции логируются с тегом:
- `MicroJUnitEngine` - основной движок
- `LLMTestGeneratorAgentRepository` - агент для LLM
- `MicroJUnitEngineTest` - тесты движка

### Ошибки парсинга
Если тест не парсится корректно:
1. Проверьте синтаксис JUnit
2. Убедитесь, что используются поддерживаемые assert'ы
3. Проверьте логи на наличие ошибок парсинга

### Ошибки выполнения
Если тест падает при выполнении:
1. Проверьте, что класс имеет публичный конструктор
2. Убедитесь, что методы публичные
3. Проверьте корректность параметров

## 🚧 Ограничения

### Поддерживаемые типы
- ✅ String, Int, Double, Boolean
- ✅ Простые объекты
- ❌ Сложные generic типы
- ❌ Лямбды и функции высшего порядка

### Поддерживаемые операции
- ✅ Вызов методов
- ✅ Обращение к полям
- ✅ Простые выражения
- ❌ Сложные вычисления
- ❌ Циклы и условия

## 🔮 Планы развития

### Краткосрочные
- [ ] Поддержка большего количества assert'ов
- [ ] Улучшение парсинга сложных выражений
- [ ] Кэширование результатов выполнения

### Долгосрочные
- [ ] Интеграция с реальным LLM сервисом
- [ ] Поддержка параллельного выполнения тестов
- [ ] Генерация отчетов в различных форматах
- [ ] Интеграция с CI/CD системами

## 📞 Поддержка

При возникновении проблем:
1. Проверьте логи приложения
2. Убедитесь, что код компилируется
3. Проверьте, что все зависимости подключены
4. Создайте issue в репозитории проекта

## 📚 Дополнительные ресурсы

- [README_TESTING.md](README_TESTING.md) - Подробное описание реализации
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/) - Документация JUnit
- [Kotlin Reflection](https://kotlinlang.org/docs/reflection.html) - Документация по reflection
