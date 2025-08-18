package com.example.ai_advent_25.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_advent_25.data.JsonResponseParser
import com.example.ai_advent_25.data.SimpleKandinskyReport
import com.example.ai_advent_25.data.agents.KandinskyDataRepository
import com.example.ai_advent_25.data.agents.KandinskyReportCreatorAgentRepository
import com.example.ai_advent_25.data.agents.GithubActivityRepository
import com.example.ai_advent_25.data.agents.RepositoryActivityStats
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
    private var githubActivityRepository: GithubActivityRepository? = null
    
    // Сохраняем токен в SharedPreferences
    private var githubToken: String = ""
    private var appContext: Context? = null
    
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
        
        githubActivityRepository = GithubActivityRepository(context)
        Log.d(TAG, "GithubActivityRepository создан")
        
        // Сохраняем контекст для использования в других методах
        appContext = context
        
        // Загружаем сохраненный токен
        loadGithubToken(context)
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
     * Генерирует график активности репозитория
     */
    fun generateActivityGraph() {
        Log.d(TAG, "generateActivityGraph вызван")
        
        if (githubActivityRepository == null) {
            Log.e(TAG, "GithubActivityRepository не инициализирован")
            _uiState.update { it.copy(error = "Github репозиторий не инициализирован") }
            return
        }
        
        if (!hasGithubToken()) {
            Log.e(TAG, "Github токен не установлен")
            _uiState.update { it.copy(error = "Github токен не установлен. Пожалуйста, добавьте токен в настройках.") }
            return
        }
        
        Log.d(TAG, "Начинаем генерацию графика активности репозитория")
        _uiState.update { it.copy(isGeneratingActivityGraph = true, activityGraphStep = ActivityGraphStep.FETCHING_COMMITS, error = null) }
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Получаем статистику активности репозитория...")
                val statsResult = githubActivityRepository!!.getRepositoryActivityStats()
                
                if (statsResult.isFailure) {
                    Log.e(TAG, "Ошибка получения статистики", statsResult.exceptionOrNull())
                    _uiState.update { 
                        it.copy(
                            error = "Ошибка получения статистики: ${statsResult.exceptionOrNull()?.message}", 
                            isGeneratingActivityGraph = false
                        ) 
                    }
                    return@launch
                }
                
                val stats = statsResult.getOrNull()
                Log.d(TAG, "Статистика получена: $stats")
                
                // Добавляем задержку в 3 секунды для демонстрации процесса
                Log.d(TAG, "Добавляем задержку для демонстрации процесса...")
                kotlinx.coroutines.delay(3000)
                
                Log.d(TAG, "Генерируем график активности...")
                _uiState.update { it.copy(activityGraphStep = ActivityGraphStep.GENERATING_GRAPH) }
                val imageResult = githubActivityRepository!!.generateActivityGraph()
                
                if (imageResult.isSuccess) {
                    val imagePath = imageResult.getOrNull()
                    Log.d(TAG, "График успешно сгенерирован: $imagePath")
                    
                    _uiState.update { 
                        it.copy(
                            activityGraphPath = imagePath,
                            repositoryStats = stats,
                            isGeneratingActivityGraph = false,
                            activityGraphStep = ActivityGraphStep.IDLE
                        ) 
                    }
                } else {
                    Log.e(TAG, "Ошибка генерации графика", imageResult.exceptionOrNull())
                    _uiState.update { 
                        it.copy(
                            error = "Ошибка генерации графика: ${imageResult.exceptionOrNull()?.message}", 
                            isGeneratingActivityGraph = false,
                            activityGraphStep = ActivityGraphStep.IDLE
                        ) 
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при генерации графика активности", e)
                _uiState.update { 
                    it.copy(
                        error = "Ошибка при генерации графика активности: ${e.message}", 
                        isGeneratingActivityGraph = false,
                        activityGraphStep = ActivityGraphStep.IDLE
                    ) 
                }
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
    
    /**
     * Устанавливает Github токен
     */
    fun setGithubToken(token: String) {
        githubToken = token
        githubActivityRepository?.setGithubToken(token)
        
        // Обновляем UI состояние
        _uiState.update { it.copy(githubToken = token) }
        
        // Сохраняем токен в SharedPreferences
        saveGithubToken(token)
    }
    
    /**
     * Получает текущий Github токен
     */
    fun getGithubToken(): String {
        return githubToken
    }
    
    /**
     * Проверяет, установлен ли Github токен
     */
    fun hasGithubToken(): Boolean {
        return githubToken.isNotBlank()
    }
    
    /**
     * Сохраняет токен в SharedPreferences
     */
    private fun saveGithubToken(token: String) {
        try {
            val context = appContext ?: return
            val sharedPrefs = context.getSharedPreferences("github_settings", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("github_token", token).apply()
            Log.d(TAG, "Github токен сохранен")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения токена", e)
        }
    }
    
    /**
     * Загружает токен из SharedPreferences
     */
    private fun loadGithubToken(context: Context) {
        try {
            val sharedPrefs = context.getSharedPreferences("github_settings", Context.MODE_PRIVATE)
            githubToken = sharedPrefs.getString("github_token", "") ?: ""
            
            if (githubToken.isNotBlank()) {
                githubActivityRepository?.setGithubToken(githubToken)
                Log.d(TAG, "Github токен загружен")
            }
            
            // Обновляем UI состояние
            _uiState.update { it.copy(githubToken = githubToken) }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки токена", e)
        }
    }
}

data class SettingsUiState(
    val isGeneratingReport: Boolean = false,
    val simpleReport: SimpleKandinskyReport? = null,
    val isGeneratingActivityGraph: Boolean = false,
    val activityGraphStep: ActivityGraphStep = ActivityGraphStep.IDLE,
    val activityGraphPath: String? = null,
    val repositoryStats: RepositoryActivityStats? = null,
    val error: String? = null,
    val githubToken: String = ""
)

enum class ActivityGraphStep {
    IDLE,
    FETCHING_COMMITS,
    GENERATING_GRAPH
}
