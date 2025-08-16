package com.example.ai_advent_25.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Простое хранилище для настроек приложения
 */
object AppSettings {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_API_KEY = "api_key"
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
        
        // Проверяем текущий API ключ
        val currentKey = getApiKey()
        Log.d(TAG, "initialize: Текущий API ключ: '${if (currentKey.isBlank()) "пустой" else "установлен (длина: ${currentKey.length})"}'")
    }
    
    fun getApiKey(): String {
        val key = sharedPreferences?.getString(KEY_API_KEY, "") ?: ""
        Log.d(TAG, "getApiKey: Возвращаем ключ: '${if (key.isBlank()) "пустой" else "установлен (длина: ${key.length})"}'")
        return key
    }
    
    fun setApiKey(apiKey: String) {
        Log.d(TAG, "setApiKey: Сохраняем ключ: '${if (apiKey.isBlank()) "пустой" else "установлен (длина: ${apiKey.length})"}'")
        sharedPreferences?.edit()?.putString(KEY_API_KEY, apiKey)?.apply()
        Log.d(TAG, "setApiKey: Ключ сохранен в SharedPreferences")
        
        // Проверяем, что ключ действительно сохранился
        val savedKey = getApiKey()
        Log.d(TAG, "setApiKey: Проверка сохранения: '${if (savedKey.isBlank()) "пустой" else "установлен (длина: ${savedKey.length})"}'")
    }
    
    fun hasApiKey(): Boolean {
        val hasKey = getApiKey().isNotBlank()
        Log.d(TAG, "hasApiKey: ${if (hasKey) "да" else "нет"}")
        return hasKey
    }
}
