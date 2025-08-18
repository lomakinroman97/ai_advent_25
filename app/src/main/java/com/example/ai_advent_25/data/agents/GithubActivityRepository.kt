package com.example.ai_advent_25.data.agents

import android.content.Context
import android.util.Log
import com.example.ai_advent_25.data.network.GithubMCPClient
import com.example.ai_advent_25.data.network.MCPClient
import java.io.File

/**
 * Репозиторий для работы с Github активностью и генерацией графиков
 */
class GithubActivityRepository(
    private val context: Context
) {
    companion object {
        private const val TAG = "GithubActivityRepository"
    }

    private val githubClient = GithubMCPClient(context)
    private val mcpClient = MCPClient(context)

    /**
     * Устанавливает Github токен
     */
    fun setGithubToken(token: String) {
        githubClient.setToken(token)
    }

    /**
     * Проверяет, установлен ли Github токен
     */
    fun hasGithubToken(): Boolean {
        return githubClient.hasToken()
    }

    /**
     * Получает активность репозитория и генерирует график
     */
    suspend fun generateActivityGraph(): Result<String> {
        return try {
            Log.d(TAG, "Начинаем генерацию графика активности репозитория")
            
            // 1. Получаем коммиты из Github
            Log.d(TAG, "Получаем коммиты из Github...")
            val commitsResult = githubClient.getRepositoryCommits()
            
            if (commitsResult.isFailure) {
                Log.e(TAG, "Ошибка получения коммитов", commitsResult.exceptionOrNull())
                return Result.failure(commitsResult.exceptionOrNull() ?: Exception("Не удалось получить коммиты"))
            }
            
            val commits = commitsResult.getOrNull() ?: emptyList()
            Log.d(TAG, "Получено коммитов: ${commits.size}")
            
            if (commits.isEmpty()) {
                Log.w(TAG, "Репозиторий пустой, нет коммитов")
                return Result.failure(Exception("Репозиторий пустой, нет коммитов для анализа"))
            }
            
            // 2. Формируем промпт для Kandinsky
            Log.d(TAG, "Формируем промпт для Kandinsky...")
            val prompt = githubClient.createActivityGraphPrompt(commits)
            Log.d(TAG, "Промпт сформирован: $prompt")
            
            // 3. Генерируем изображение через Kandinsky MCP
            Log.d(TAG, "Генерируем изображение через Kandinsky MCP...")
            val projectDir = context.filesDir.parentFile?.absolutePath ?: "/data/user/0/com.example.ai_advent_25"
            val filename = "github_activity_${System.currentTimeMillis()}.png"
            
            val imageResult = mcpClient.callKandinskyGenerateImage(
                prompt = prompt,
                filename = filename,
                projectDir = projectDir,
                width = 1024,
                height = 768,
                style = "DEFAULT",
                negativePrompt = "blurry, low quality, distorted, text overlay",
                overwrite = false
            )
            
            if (imageResult.isSuccess) {
                val imagePath = imageResult.getOrNull()
                Log.d(TAG, "Изображение успешно сгенерировано: $imagePath")
                Result.success(imagePath ?: "")
            } else {
                Log.e(TAG, "Ошибка генерации изображения", imageResult.exceptionOrNull())
                Result.failure(imageResult.exceptionOrNull() ?: Exception("Не удалось сгенерировать изображение"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при генерации графика активности", e)
            Result.failure(e)
        }
    }

    /**
     * Получает статистику активности репозитория
     */
    suspend fun getRepositoryActivityStats(): Result<RepositoryActivityStats> {
        return try {
            Log.d(TAG, "Получаем статистику активности репозитория")
            
            val commitsResult = githubClient.getRepositoryCommits()
            if (commitsResult.isFailure) {
                return Result.failure(commitsResult.exceptionOrNull() ?: Exception("Не удалось получить коммиты"))
            }
            
            val commits = commitsResult.getOrNull() ?: emptyList()
            
            // Группируем коммиты по дням
            val commitsByDay = commits.groupBy { commit ->
                commit.commitDate.substring(0, 10)
            }
            
            // Статистика по дням недели
            val dayOfWeekStats = githubClient.getActivityStatsByDayOfWeek(commits)
            
            // Находим самый активный день
            val mostActiveDay = commitsByDay.maxByOrNull { it.value.size }?.key ?: ""
            val mostActiveDayCount = commitsByDay[mostActiveDay]?.size ?: 0
            
            // Находим последний коммит
            val lastCommit = commits.firstOrNull()
            val lastCommitDate = lastCommit?.commitDate ?: ""
            
            val stats = RepositoryActivityStats(
                totalCommits = commits.size,
                totalDays = commitsByDay.size,
                mostActiveDay = mostActiveDay,
                mostActiveDayCount = mostActiveDayCount,
                lastCommitDate = lastCommitDate,
                commitsByDay = commitsByDay.mapValues { it.value.size },
                dayOfWeekStats = dayOfWeekStats,
                averageCommitsPerDay = if (commitsByDay.isNotEmpty()) commits.size.toFloat() / commitsByDay.size else 0f
            )
            
            Log.d(TAG, "Статистика получена: $stats")
            Result.success(stats)
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении статистики активности", e)
            Result.failure(e)
        }
    }

    /**
     * Проверяет доступность Github API
     */
    suspend fun isGithubApiAvailable(): Boolean {
        return try {
            val result = githubClient.getRepositoryCommits()
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки доступности Github API", e)
            false
        }
    }
}

/**
 * Статистика активности репозитория
 */
data class RepositoryActivityStats(
    val totalCommits: Int,
    val totalDays: Int,
    val mostActiveDay: String,
    val mostActiveDayCount: Int,
    val lastCommitDate: String,
    val commitsByDay: Map<String, Int>,
    val dayOfWeekStats: Map<String, Int>,
    val averageCommitsPerDay: Float
)
