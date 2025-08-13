# Сетевая архитектура приложения

## Обзор

Приложение было рефакторено согласно принципам чистой архитектуры для устранения дублирования кода создания Retrofit в репозиториях.

## Структура

### 1. NetworkProvider (Интерфейс)
- **Файл**: `app/src/main/java/com/example/ai_advent_25/data/network/NetworkProvider.kt`
- **Назначение**: Абстракция для сетевого слоя
- **Методы**: `getChatApi(): ChatApi`

### 2. NetworkModule (Реализация)
- **Файл**: `app/src/main/java/com/example/ai_advent_25/data/network/NetworkModule.kt`
- **Назначение**: Централизованное создание сетевых зависимостей
- **Реализует**: `NetworkProvider`
- **Особенности**: 
  - Singleton объект
  - Lazy инициализация компонентов
  - Централизованная конфигурация

### 3. NetworkConstants (Константы)
- **Файл**: `app/src/main/java/com/example/ai_advent_25/data/network/NetworkConstants.kt`
- **Назначение**: Централизованное хранение сетевых констант
- **Константы**:
  - `BASE_URL`: Базовый URL для API
  - `FOLDER_ID`: ID папки для Yandex Cloud

### 4. AgentRepositoryFactory (Фабрика)
- **Файл**: `app/src/main/java/com/example/ai_advent_25/data/agents/AgentRepositoryFactory.kt`
- **Назначение**: Создание репозиториев с внедрением зависимостей
- **Методы**:
  - `createExpertReviewerAgentRepository()`
  - `createTravelAssistAgentRepository()`

## Преимущества новой архитектуры

### 1. Устранение дублирования
- Код создания Retrofit больше не дублируется в репозиториях
- Единая точка конфигурации сетевого слоя

### 2. Принципы SOLID
- **Single Responsibility**: Каждый класс имеет одну ответственность
- **Dependency Inversion**: Репозитории зависят от абстракции `NetworkProvider`
- **Open/Closed**: Легко расширять без изменения существующего кода

### 3. Тестируемость
- Возможность легко подменять `NetworkProvider` для unit-тестов
- Изоляция сетевого слоя

### 4. Поддерживаемость
- Централизованное управление сетевыми настройками
- Легко изменять конфигурацию для всех репозиториев

## Использование

### Создание репозитория
```kotlin
// Через фабрику (рекомендуется)
val repository = AgentRepositoryFactory.createTravelAssistAgentRepository(apiKey)

// Напрямую
val repository = TravelAssistAgentRepository(apiKey, NetworkModule)
```

### Подмена для тестирования
```kotlin
// Создание mock NetworkProvider для тестов
val mockNetworkProvider = object : NetworkProvider {
    override fun getChatApi(): ChatApi = mockChatApi
}

val repository = TravelAssistAgentRepository(apiKey, mockNetworkProvider)
```

## Миграция

### До рефакторинга
```kotlin
class TravelAssistAgentRepository(private val apiKey: String) {
    private val chatApi: ChatApi by lazy {
        // Дублированный код создания Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://llm.api.cloud.yandex.net/")
            // ... остальная конфигурация
            .build()
        retrofit.create(ChatApi::class.java)
    }
}
```

### После рефакторинга
```kotlin
class TravelAssistAgentRepository(
    private val apiKey: String,
    private val networkProvider: NetworkProvider
) {
    private val chatApi: ChatApi by lazy {
        networkProvider.getChatApi()
    }
}
```

## Расширение

Для добавления новых сетевых сервисов:

1. Добавить новый метод в `NetworkProvider`
2. Реализовать в `NetworkModule`
3. Использовать через внедрение зависимостей

```kotlin
interface NetworkProvider {
    fun getChatApi(): ChatApi
    fun getNewService(): NewService // Новый сервис
}
```
