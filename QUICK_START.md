# Быстрый запуск AI Chat App

## Шаг 1: Получите API ключ OpenAI
1. Зайдите на [https://platform.openai.com/](https://platform.openai.com/)
2. Создайте аккаунт или войдите
3. Перейдите в "API Keys" → "Create new secret key"
4. Скопируйте ключ (начинается с `sk-`)

## Шаг 2: Вставьте API ключ в код
Откройте файл: `app/src/main/java/com/example/ai_advent_25/ui/ChatViewModel.kt`

Найдите строку:
```kotlin
private val apiKey = "YOUR_API_KEY_HERE"
```

Замените на ваш ключ:
```kotlin
private val apiKey = "sk-ваш_реальный_ключ_здесь"
```

## Шаг 3: Запустите приложение
```bash
./gradlew assembleDebug
```

APK файл будет создан в: `app/build/outputs/apk/debug/app-debug.apk`

## Шаг 4: Установите на устройство
1. Скопируйте APK на Android устройство
2. Установите APK (разрешите установку из неизвестных источников)
3. Запустите приложение
4. Начните чат!

## Возможные проблемы
- **Ошибка сети**: проверьте интернет соединение
- **Ошибка API**: убедитесь, что API ключ правильный
- **Ошибка сборки**: выполните `./gradlew clean` и попробуйте снова
