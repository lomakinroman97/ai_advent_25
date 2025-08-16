package com.example.ai_advent_25.data.agents

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.ai_advent_25.data.KandinskyWorkData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий для хранения данных о работе Kandinsky
 * Использует SharedPreferences для персистентного хранения данных
 */
class KandinskyDataRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "kandinsky_data", 
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    private val workDataListType = object : TypeToken<List<KandinskyWorkData>>() {}.type
    
    companion object {
        private const val KEY_WORK_DATA = "kandinsky_work_data"
        private const val TAG = "KandinskyDataRepository"
    }
    
    /**
     * Сохраняет данные о работе Kandinsky
     */
    suspend fun saveWorkData(workData: KandinskyWorkData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Сохраняем данные о работе Kandinsky: ${workData.id}")
            
            val existingData = getWorkDataList().toMutableList()
            existingData.add(workData)
            
            val jsonData = gson.toJson(existingData)
            sharedPreferences.edit().putString(KEY_WORK_DATA, jsonData).apply()
            
            Log.d(TAG, "Данные успешно сохранены. Всего записей: ${existingData.size}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сохранении данных", e)
            Result.failure(e)
        }
    }
    
    /**
     * Обновляет существующие данные о работе Kandinsky
     */
    suspend fun updateWorkData(workData: KandinskyWorkData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Обновляем данные о работе Kandinsky: ${workData.id}")
            
            val existingData = getWorkDataList().toMutableList()
            val index = existingData.indexOfFirst { it.id == workData.id }
            
            if (index != -1) {
                existingData[index] = workData
                val jsonData = gson.toJson(existingData)
                sharedPreferences.edit().putString(KEY_WORK_DATA, jsonData).apply()
                
                Log.d(TAG, "Данные успешно обновлены")
                Result.success(Unit)
            } else {
                Log.w(TAG, "Запись для обновления не найдена: ${workData.id}")
                Result.failure(IllegalArgumentException("Запись не найдена"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении данных", e)
            Result.failure(e)
        }
    }
    
    /**
     * Получает все данные о работе Kandinsky
     */
    suspend fun getAllWorkData(): Result<List<KandinskyWorkData>> = withContext(Dispatchers.IO) {
        try {
            val data = getWorkDataList()
            Log.d(TAG, "Получены данные о работе Kandinsky. Количество записей: ${data.size}")
            Result.success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении данных", e)
            Result.failure(e)
        }
    }
    
    /**
     * Получает данные о работе Kandinsky по ID
     */
    suspend fun getWorkDataById(id: String): Result<KandinskyWorkData?> = withContext(Dispatchers.IO) {
        try {
            val data = getWorkDataList().find { it.id == id }
            Result.success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении данных по ID", e)
            Result.failure(e)
        }
    }
    
    /**
     * Получает статистику по городам
     */
    suspend fun getCityStats(): Result<Map<String, Int>> = withContext(Dispatchers.IO) {
        try {
            val data = getWorkDataList()
            val cityStats = data.groupBy { it.cityName }.mapValues { it.value.size }
            Result.success(cityStats)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении статистики по городам", e)
            Result.failure(e)
        }
    }
    
    /**
     * Получает количество успешных генераций
     */
    suspend fun getSuccessfulGenerationsCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allData = getAllWorkData().getOrNull() ?: emptyList()
            val count = allData.count { it.status == "success" }
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении количества успешных генераций", e)
            Result.failure(e)
        }
    }

    /**
     * Получает количество неудачных генераций
     */
    suspend fun getFailedGenerationsCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allData = getAllWorkData().getOrNull() ?: emptyList()
            val count = allData.count { it.status == "failed" }
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении количества неудачных генераций", e)
            Result.failure(e)
        }
    }

    /**
     * Получает среднее время обработки (всегда 0, так как поле убрано)
     */
    suspend fun getAverageProcessingTime(): Result<Long> = withContext(Dispatchers.IO) {
        Result.success(0L) // Упрощенная версия
    }
    
    /**
     * Очищает все данные (используется только для тестирования)
     */
    suspend fun clearAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sharedPreferences.edit().remove(KEY_WORK_DATA).apply()
            Log.d(TAG, "Все данные очищены")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке данных", e)
            Result.failure(e)
        }
    }
    
    /**
     * Получает список данных из SharedPreferences
     */
    private fun getWorkDataList(): List<KandinskyWorkData> {
        val jsonData = sharedPreferences.getString(KEY_WORK_DATA, "[]")
        return try {
            gson.fromJson(jsonData, workDataListType) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при парсинге JSON данных", e)
            emptyList()
        }
    }
}
