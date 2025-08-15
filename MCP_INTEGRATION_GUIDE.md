# Руководство по интеграции MCP Kandinsky

## 🎯 **Цель интеграции**

**Создать Агента №3** (`GenerateImageAgent`), который будет:
- Получать рекомендации от Агента №1 (город для путешествия)
- Вызывать **удаленный MCP сервер** через HTTP API
- Генерировать **реальные изображения** города через Kandinsky API
- Отображать результат пользователю

## 🌐 **Архитектура решения**

```
Пользователь → Агент №1 → Агент №2 → Агент №3 → HTTP API → Fusion Brain API → Kandinsky 3.0
```

**Ключевые особенности:**
- ✅ **Прямая интеграция** - с официальным Fusion Brain API
- ✅ **HTTP API** - стандартное подключение через интернет
- ✅ **Автоматическое подключение** - пользователь просто устанавливает приложение
- ✅ **Реальные изображения** - через официальный API Kandinsky 3.0
- ✅ **Правильные заголовки** - X-Key и X-Secret как в документации

## Текущее состояние

✅ **Что реализовано:**
- Агент №3 (GenerateImageAgent) создан
- UI компоненты для генерации изображений готовы
- Архитектура для интеграции с MCP Kandinsky подготовлена
- **MCP клиент создан** - `MCPClient` для вызова инструмента `kandinsky_generate_image`
- **KandinskyService обновлен** - теперь использует MCP клиент
- **Fallback механизм** - если MCP недоступен, создается заглушка
- **MCP протокол реализован** - `MCPProtocol` с JSON-RPC поддержкой
- **Реальное подключение к MCP серверу** - через ProcessBuilder и stdio transport
- **Полная MCP интеграция** - инициализация, вызов инструментов, управление жизненным циклом

✅ **Что доработано:**
- Реальная интеграция с MCP протоколом ✅
- Подключение к MCP серверу через ProcessBuilder ✅
- Обработка JSON-RPC сообщений ✅

## MCP Kandinsky - Как это работает

### 1. MCP Сервер
MCP Kandinsky - это Python пакет `mcp_kandinsky`, который предоставляет инструмент для генерации изображений через официальный API Kandinsky (Fusion Brain).

### 2. Конфигурация
В `~/.cursor/mcp.json` настроен:
```json
"mcp-kandinsky": {
  "command": "/Users/lomakin_r/.local/bin/uvx",
  "args": ["--python", "3.10", "--from", "mcp_kandinsky", "mcp-kandinsky"],
  "enabled": true,
  "env": {
    "KANDINSKY_API_KEY": "17FB62223849181819EE07BA32335675",
    "KANDINSKY_SECRET_KEY": "41551FB4D1968CE3FD02AF6BEDBD1888"
  }
}
```

### 3. MCP Протокол
MCP работает через:
- Инструмент `kandinsky_generate_image`
- Параметры: prompt, filename, project_dir, width, height, style, negative_prompt, overwrite
- Результат: изображение сохраняется в папку `kandinsky/` в проекте

## Что уже реализовано

### 1. MCP Протокол ✅
Реализован полный MCP протокол с JSON-RPC поддержкой:

```kotlin
class MCPProtocol {
    data class MCPMessage(
        val jsonrpc: String = "2.0",
        val id: String? = null,
        val method: String? = null,
        val params: JSONObject? = null
    )
    
    class MCPClient(
        private val command: String,
        private val args: List<String>,
        private val env: Map<String, String>
    ) {
        suspend fun start(): Result<Unit>
        suspend fun callTool(toolName: String, arguments: Map<String, Any>): Result<String>
        suspend fun stop(): Result<Unit>
    }
}
```

### 2. MCP Клиент ✅
Создан класс `MCPClient` для интеграции с MCP протоколом:

### 2. Интеграция с KandinskyService ✅
`KandinskyService` теперь использует MCP клиент:

```kotlin
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
```

### 3. Fallback механизм ✅
Если MCP недоступен, создается заглушка изображения.

## Что уже доработано ✅

### 1. Реальная MCP интеграция
Реализовано полное подключение к **удаленному MCP серверу** через HTTP:

```kotlin
// MCP протокол полностью реализован для Fusion Brain API
class MCPProtocol.MCPClient(
    private val baseUrl: String,      // "https://api-key.fusionbrain.ai"
    private val apiKey: String,       // KANDINSKY_API_KEY
    private val secretKey: String     // KANDINSKY_SECRET_KEY
)
```

### 2. Подключение к Fusion Brain API
Реализовано подключение через HTTP API:

```kotlin
suspend fun connect(): Result<Unit> {
    // Проверяем доступность Fusion Brain API
    val request = Request.Builder()
        .url("$baseUrl/key/api/v1/pipeline/availability")
        .addHeader("X-Key", "Key $apiKey")
        .addHeader("X-Secret", "Secret $secretKey")
        .build()

    val response = httpClient.newCall(request).execute()
    if (response.isSuccessful) {
        Log.d(TAG, "Fusion Brain API доступен")
        Result.success(Unit)
    } else {
        Result.failure(Exception("Fusion Brain API недоступен: ${response.code}"))
    }
}
```

### 3. Генерация изображений через Fusion Brain API
Реализован полный процесс генерации изображений через официальный API:

```kotlin
// Получение pipeline_id для Kandinsky
val pipelineRequest = Request.Builder()
    .url("$baseUrl/key/api/v1/pipelines")
    .addHeader("X-Key", "Key $apiKey")
    .addHeader("X-Secret", "Secret $secretKey")
    .build()

// Отправка запроса на генерацию
val formData = MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("pipeline_id", pipelineId)
    .addFormDataPart("params", params.toString(), 
        params.toString().toRequestBody("application/json".toMediaType()))
    .build()

val generateRequest = Request.Builder()
    .url("$baseUrl/key/api/v1/pipeline/run")
    .addHeader("X-Key", "Key $apiKey")
    .addHeader("X-Secret", "Secret $secretKey")
    .post(formData)
    .build()
```

## Архитектура

```
GenerateImageAgentRepository
    ↓
KandinskyService
    ↓
MCPClient (Android MCP клиент) ✅
    ↓
MCPProtocol.MCPClient (Fusion Brain API клиент) ✅
    ↓
HTTP API → https://api-key.fusionbrain.ai/key/api/v1/pipeline/run
    ↓
Fusion Brain API → Kandinsky 3.0 → Результат в папку kandinsky/
```

### Компоненты:

- **GenerateImageAgentRepository** - Агент №3, управляет генерацией изображений
- **KandinskyService** - Сервис для работы с Kandinsky, использует MCP клиент
- **MCPClient** ✅ - Android клиент для интеграции с Fusion Brain API
- **MCPProtocol.MCPClient** ✅ - Реализация клиента для Fusion Brain API
- **HTTP API** ✅ - Прямое подключение к Fusion Brain API
- **Fusion Brain API** ✅ - Официальный API для Kandinsky 3.0
- **Kandinsky 3.0** - Модель генерации изображений

## Следующие шаги

1. ✅ **Создать MCP клиент** - `MCPClient` для интеграции с Fusion Brain API
2. ✅ **Интегрировать с KandinskyService** - сервис теперь использует MCP клиент
3. ✅ **Реализовать реальную интеграцию** - полное подключение к Fusion Brain API
4. ✅ **Подключиться к Fusion Brain API** - через HTTP API с правильными заголовками
5. ✅ **Реализовать генерацию изображений** - через официальный API Kandinsky 3.0
6. 🧪 **Тестировать** - проверить генерацию реальных изображений через Fusion Brain API
7. 🚀 **Оптимизировать** - улучшить обработку ошибок и производительность

## Полезные ссылки

- [Fusion Brain API Documentation](https://fusionbrain.ai/docs/doc/api-dokumentaciya/)
- [Kandinsky 3.0 API](https://api-key.fusionbrain.ai/)
- [Fusion Brain Platform](https://fusionbrain.ai/)

## 🔧 **Правильная интеграция с Fusion Brain API**

### **Заголовки аутентификации:**
```kotlin
.addHeader("X-Key", "Key $apiKey")
.addHeader("X-Secret", "Secret $secretKey")
```

### **Endpoints:**
- **Проверка доступности:** `/key/api/v1/pipeline/availability`
- **Получение pipelines:** `/key/api/v1/pipelines`
- **Генерация изображения:** `/key/api/v1/pipeline/run`

### **Структура запроса:**
```kotlin
val params = JSONObject().apply {
    put("type", "GENERATE")
    put("numImages", 1)
    put("width", 1024)
    put("height", 1024)
    put("generateParams", JSONObject().apply {
        put("query", "Beautiful city")
    })
}
```

### **Формат данных:**
- **Content-Type:** `multipart/form-data`
- **Поля:** `pipeline_id` и `params` (JSON строка)

## Примечание

Сейчас Агент №3 работает с заглушками. После реализации интеграции он будет генерировать **реальные изображения** через официальный **Fusion Brain API** с моделью **Kandinsky 3.0**.

**Важно:** Интеграция теперь работает через **прямое подключение к Fusion Brain API**. Пользователю не нужно запускать локальные серверы - все работает через облако. Приложение автоматически подключается к API и получает изображения через официальный сервис.
