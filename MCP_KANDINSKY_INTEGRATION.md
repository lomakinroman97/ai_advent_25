# Интеграция MCP Kandinsky в приложение AI Advent 25

## Обзор

Этот документ описывает интеграцию MCP (Model Context Protocol) Kandinsky в приложение для генерации изображений городов, рекомендованных пользователю.

## Архитектура

### Агенты

1. **Агент №1 (TravelAssistAgent)** - Опрашивает пользователя и выдает рекомендации по городам
2. **Агент №2 (ExpertReviewerAgent)** - Проверяет работу Агента №1 и дает расширенные рекомендации
3. **Агент №3 (GenerateImageAgent)** - Генерирует изображения городов через MCP Kandinsky

### Поток работы

1. Пользователь получает рекомендации от Агента №1
2. Пользователь нажимает "Подключить эксперта" → Агент №2 выдает результат
3. Появляется кнопка "Ваш город глазами ИИ"
4. По тапу на кнопку Агент №3 генерирует изображение через MCP Kandinsky

## MCP Kandinsky

### Конфигурация

MCP Kandinsky настроен в файле `~/.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "mcp-kandinsky": {
      "command": "/Users/lomakin_r/.local/bin/uvx",
      "args": [
        "--python", "3.10",
        "--from", "mcp_kandinsky", "mcp-kandinsky"
      ],
      "enabled": true,
      "env": {
        "KANDINSKY_API_KEY": "17FB62223849181819EE07BA32335675",
        "KANDINSKY_SECRET_KEY": "41551FB4D1968CE3FD02AF6BEDBD1888"
      }
    }
  }
}
```

### API Ключи

- **API Key**: `17FB62223849181819EE07BA32335675`
- **Secret Key**: `41551FB4D1968CE3FD02AF6BEDBD1888`

## Компоненты

### 1. GenerateImageAgentRepository

```kotlin
class GenerateImageAgentRepository(
    private val context: Context,
    private val kandinskyApiKey: String,
    private val kandinskySecretKey: String
)
```

**Функции:**
- `generateCityImage(cityRecommendation: CityRecommendation): Result<GeneratedImage>`
- Создает промпт для генерации изображения
- Вызывает MCP Kandinsky через KandinskyService
- Возвращает результат с путем к изображению

### 2. KandinskyService

```kotlin
class KandinskyService(
    private val context: Context,
    private val apiKey: String,
    private val secretKey: String
)
```

**Функции:**
- `generateImage(prompt: String, cityName: String, width: Int = 1024, height: Int = 1024): Result<String>`
- Интегрируется с Kandinsky API
- Создает заглушки изображений при недоступности API
- Сохраняет изображения во внутреннем хранилище

### 3. UI Компоненты

#### GenerateImageButton
- Кнопка "Ваш город глазами ИИ"
- Появляется после экспертного мнения
- Анимированная с пульсацией

#### GeneratedImageCard
- Отображает сгенерированное изображение
- Показывает промпт и время генерации
- Стилизован в синих тонах

## Интеграция

### 1. Инициализация

```kotlin
// В ChatViewModel
fun initializeImageGenerator(context: Context) {
    generateImageAgentRepository = AgentRepositoryFactory.createGenerateImageAgentRepository(context)
}

// В ChatScreen
LaunchedEffect(Unit) {
    viewModel.initializeImageGenerator(LocalContext.current)
}
```

### 2. Генерация изображения

```kotlin
fun generateCityImage(cityRecommendation: CityRecommendation) {
    viewModelScope.launch {
        generateImageAgentRepository?.generateCityImage(cityRecommendation)?.fold(
            onSuccess = { generatedImage ->
                // Добавляем сообщение с изображением
            },
            onFailure = { exception ->
                // Обрабатываем ошибку
            }
        )
    }
}
```

### 3. Отображение

```kotlin
// В ChatScreen
message.generatedImage?.let { generatedImage ->
    GeneratedImageCard(generatedImage = generatedImage)
}
```

## Зависимости

### Добавленные

```kotlin
// JSON support
implementation("org.json:json:20231013")
```

### Существующие

- OkHttp для HTTP запросов
- Coroutines для асинхронности
- Compose для UI

## Обработка ошибок

1. **API недоступен** - Создается заглушка изображения
2. **Ошибка сети** - Показывается сообщение об ошибке
3. **Неверные ключи** - Логируется ошибка аутентификации

## Безопасность

- API ключи хранятся в конфигурации MCP
- Изображения сохраняются во внутреннем хранилище приложения
- Нет передачи чувствительных данных в логи

## Тестирование

### Unit тесты

```kotlin
@Test
fun `generateCityImage should return success with valid city`() {
    // Тест генерации изображения
}
```

### UI тесты

```kotlin
@Test
fun `GenerateImageButton should be visible after expert opinion`() {
    // Тест видимости кнопки
}
```

## Будущие улучшения

1. **Кэширование** - Сохранение изображений для повторного использования
2. **Множественные стили** - Выбор стиля генерации (DEFAULT, KANDINSKY, UHD, ANIME)
3. **Галерея** - Просмотр всех сгенерированных изображений
4. **Поделиться** - Возможность поделиться изображением

## Устранение неполадок

### MCP сервер не запущен

```bash
# Проверить статус
ps aux | grep mcp-kandinsky

# Перезапустить
uvx --from mcp_kandinsky mcp-kandinsky
```

### Python версия

```bash
# Установить Python 3.10+
uv python install 3.10

# Проверить версию
python3.10 --version
```

### API ключи

- Проверить правильность ключей в `~/.cursor/mcp.json`
- Убедиться, что переменные окружения установлены

## Заключение

MCP Kandinsky успешно интегрирован в приложение AI Advent 25, предоставляя пользователям возможность визуализировать рекомендованные города через генерацию изображений ИИ. Архитектура поддерживает расширяемость и легкость добавления новых функций.
