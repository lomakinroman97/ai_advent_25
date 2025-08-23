package com.example.ai_advent_25.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Простое хранилище для настроек приложения
 */
object AppSettings {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_YANDEX_API_KEY = "yandex_api_key"
    private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
    private const val TAG = "AppSettings"
    
    private var sharedPreferences: SharedPreferences? = null
    
    fun initialize(context: Context) {
        Log.d(TAG, "initialize: Инициализируем AppSettings")
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            Log.d(TAG, "initialize: SharedPreferences создан")
        } else {
            Log.d(TAG, "initialize: SharedPreferences уже существует")
        }
        
        // Проверяем текущие API ключи
        val yandexKey = getYandexApiKey()
        val deepseekKey = getDeepseekApiKey()
        Log.d(TAG, "initialize: Yandex API ключ: '${if (yandexKey.isBlank()) "пустой" else "установлен (длина: ${yandexKey.length})"}'")
        Log.d(TAG, "initialize: DeepSeek API ключ: '${if (deepseekKey.isBlank()) "пустой" else "установлен (длина: ${deepseekKey.length})"}'")
    }
    
    fun getYandexApiKey(): String {
        val key = sharedPreferences?.getString(KEY_YANDEX_API_KEY, "") ?: ""
        Log.d(TAG, "getYandexApiKey: Возвращаем ключ: '${if (key.isBlank()) "пустой" else "установлен (длина: ${key.length})"}'")
        return key
    }
    
    fun setYandexApiKey(apiKey: String) {
        Log.d(TAG, "setYandexApiKey: Сохраняем ключ: '${if (apiKey.isBlank()) "пустой" else "установлен (длина: ${apiKey.length})"}'")
        sharedPreferences?.edit()?.putString(KEY_YANDEX_API_KEY, apiKey)?.apply()
        Log.d(TAG, "setYandexApiKey: Ключ сохранен в SharedPreferences")
        
        // Проверяем, что ключ действительно сохранился
        val savedKey = getYandexApiKey()
        Log.d(TAG, "setYandexApiKey: Проверка сохранения: '${if (savedKey.isBlank()) "пустой" else "установлен (длина: ${savedKey.length})"}'")
    }
    
    fun getDeepseekApiKey(): String {
        val key = sharedPreferences?.getString(KEY_DEEPSEEK_API_KEY, "") ?: ""
        Log.d(TAG, "getDeepseekApiKey: Возвращаем ключ: '${if (key.isBlank()) "пустой" else "установлен (длина: ${key.length})"}'")
        return key
    }
    
    fun setDeepseekApiKey(apiKey: String) {
        Log.d(TAG, "setDeepseekApiKey: Сохраняем ключ: '${if (apiKey.isBlank()) "пустой" else "установлен (длина: ${apiKey.length})"}'")
        sharedPreferences?.edit()?.putString(KEY_DEEPSEEK_API_KEY, apiKey)?.apply()
        Log.d(TAG, "setDeepseekApiKey: Ключ сохранен в SharedPreferences")
        
        // Проверяем, что ключ действительно сохранился
        val savedKey = getDeepseekApiKey()
        Log.d(TAG, "setDeepseekApiKey: Проверка сохранения: '${if (savedKey.isBlank()) "пустой" else "установлен (длина: ${savedKey.length})"}'")
    }
    
    fun hasAnyApiKey(): Boolean {
        val hasYandexKey = getYandexApiKey().isNotBlank()
        val hasDeepseekKey = getDeepseekApiKey().isNotBlank()
        val hasAnyKey = hasYandexKey || hasDeepseekKey
        Log.d(TAG, "hasAnyApiKey: Yandex: ${if (hasYandexKey) "да" else "нет"}, DeepSeek: ${if (hasDeepseekKey) "да" else "нет"}, Общий: ${if (hasAnyKey) "да" else "нет"}")
        return hasAnyKey
    }
    
    // Обратная совместимость
    fun getApiKey(): String {
        return getYandexApiKey()
    }
    
    fun setApiKey(apiKey: String) {
        setYandexApiKey(apiKey)
    }
    
    fun hasApiKey(): Boolean {
        return hasAnyApiKey()
    }
}
