package com.example.ai_advent_25.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_advent_25.data.JsonResponseParser
import com.example.ai_advent_25.data.SimpleKandinskyReport
import com.example.ai_advent_25.data.agents.KandinskyDataRepository
import com.example.ai_advent_25.data.agents.KandinskyReportCreatorAgentRepository
import com.example.ai_advent_25.data.agents.factory.AgentRepositoryFactory
import com.example.ai_advent_25.data.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private var kandinskyDataRepository: KandinskyDataRepository? = null
    private var kandinskyReportCreatorAgentRepository: KandinskyReportCreatorAgentRepository? = null
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    /**
     * Инициализирует репозитории
     */
    fun initialize(context: Context, apiKey: String) {
        Log.d(TAG, "Инициализируем SettingsViewModel")
        Log.d(TAG, "API ключ получен: '${if (apiKey.isBlank()) "пустой" else "установлен (длина: ${apiKey.length})"}'")
        
        kandinskyDataRepository = KandinskyDataRepository(context)
        Log.d(TAG, "KandinskyDataRepository создан")

        if (apiKey.isBlank()) {
            Log.e(TAG, "API ключ пустой! Пользователь не должен видеть этот экран")
        }
        
        kandinskyReportCreatorAgentRepository = AgentRepositoryFactory.createKandinskyReportCreatorAgentRepository(apiKey, NetworkModule)
        Log.d(TAG, "Репозиторий отчетов инициализирован с API ключом: '${if (apiKey.isBlank()) "пустой" else "установлен"}'")
    }
    
    /**
     * Генерирует отчет о работе Kandinsky
     */
    fun generateKandinskyReport() {
        Log.d(TAG, "generateKandinskyReport вызван")
        
        if (kandinskyDataRepository == null || kandinskyReportCreatorAgentRepository == null) {
            Log.e(TAG, "Репозитории не инициализированы")
            _uiState.update { it.copy(error = "Репозитории не инициализированы") }
            return
        }
        
        Log.d(TAG, "Начинаем генерацию отчета о работе Kandinsky")
        _uiState.update { it.copy(isGeneratingReport = true, error = null) }
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Получаем данные о работе Kandinsky...")
                val workData = kandinskyDataRepository!!.getAllWorkData().getOrNull() ?: emptyList()
                Log.d(TAG, "Получены данные для анализа: ${workData.size} записей")
                
                if (workData.isEmpty()) {
                    Log.d(TAG, "Нет данных для анализа")
                    _uiState.update { it.copy(error = "Нет данных для анализа. Kandinsky еще не использовался в приложении.", isGeneratingReport = false) }
                    return@launch
                }
                
                Log.d(TAG, "Вызываем Агента №4 для создания отчета...")
                val reportJson = kandinskyReportCreatorAgentRepository!!.createKandinskyReport(workData).getOrNull()
                
                if (reportJson != null) {
                    Log.d(TAG, "JSON отчет получен, длина: ${reportJson.length}")
                    
                    // Парсим JSON в упрощенный отчет
                    val parsedReport = JsonResponseParser.parseSimpleKandinskyReport(reportJson)
                    parsedReport.fold(
                        onSuccess = { simpleReport ->
                            Log.d(TAG, "Упрощенный отчет успешно распарсен")
                            _uiState.update { it.copy(simpleReport = simpleReport, isGeneratingReport = false) }
                        },
                        onFailure = { parseException ->
                            Log.e(TAG, "Ошибка парсинга отчета, используем fallback", parseException)
                            
                            // Создаем fallback отчет на основе базовых данных
                            val fallbackReport = JsonResponseParser.createFallbackReport(workData)
                            Log.d(TAG, "Fallback отчет создан: $fallbackReport")
                            
                            _uiState.update { it.copy(simpleReport = fallbackReport, isGeneratingReport = false) }
                        }
                    )
                } else {
                    Log.e(TAG, "Агент №4 вернул null")
                    _uiState.update { it.copy(error = "Не удалось создать отчет", isGeneratingReport = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при генерации отчета", e)
                _uiState.update { it.copy(error = "Ошибка при генерации отчета: ${e.message}", isGeneratingReport = false) }
            }
        }
    }
    
    /**
     * Очищает ошибку
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Обновляет статистику
     */
    fun refreshStats() {
        // This function is no longer needed as statistics are removed from UI state
    }
}

data class SettingsUiState(
    val isGeneratingReport: Boolean = false,
    val simpleReport: SimpleKandinskyReport? = null,
    val error: String? = null
)
