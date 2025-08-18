package com.example.ai_advent_25.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

/**
 * Github MCP клиент для получения данных о коммитах репозитория
 */
class GithubMCPClient(
    private val context: Context,
    private var githubToken: String? = null
) {
    companion object {
        private const val TAG = "GithubMCPClient"
        private const val GITHUB_API_BASE_URL = "https://api.github.com"
        private const val REPO_OWNER = "lomakinroman97"
        private const val REPO_NAME = "ai_advent_25"
    }

    private val httpClient = OkHttpClient()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Устанавливает Github токен
     */
    fun setToken(token: String) {
        this.githubToken = token
    }

    /**
     * Проверяет, установлен ли токен
     */
    fun hasToken(): Boolean {
        return !githubToken.isNullOrBlank()
    }

    /**
     * Получает список всех коммитов репозитория
     */
    suspend fun getRepositoryCommits(): Result<List<CommitInfo>> = withContext(Dispatchers.IO) {
        try {
            if (!hasToken()) {
                return@withContext Result.failure(Exception("Github токен не установлен. Пожалуйста, добавьте токен в настройках."))
            }
            
            Log.d(TAG, "Получаем коммиты для репозитория: $REPO_OWNER/$REPO_NAME")
            
            val url = "$GITHUB_API_BASE_URL/repos/$REPO_OWNER/$REPO_NAME/commits"
            Log.d(TAG, "URL для получения коммитов: $url")
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "token ${githubToken!!}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "ai_advent_25_app")
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    Log.d(TAG, "Получен ответ от Github API, длина: ${responseBody.length}")
                    
                    val commits = parseCommitsResponse(responseBody)
                    Log.d(TAG, "Распарсено коммитов: ${commits.size}")
                    
                    Result.success(commits)
                } else {
                    Log.e(TAG, "Пустой ответ от Github API")
                    Result.failure(Exception("Пустой ответ от Github API"))
                }
            } else {
                Log.e(TAG, "Ошибка получения коммитов: ${response.code}")
                Result.failure(Exception("Ошибка получения коммитов: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении коммитов", e)
            Result.failure(e)
        }
    }

    /**
     * Парсит ответ от Github API с коммитами
     */
    private fun parseCommitsResponse(responseBody: String): List<CommitInfo> {
        val commits = mutableListOf<CommitInfo>()
        
        try {
            val jsonArray = JSONArray(responseBody)
            
            for (i in 0 until jsonArray.length()) {
                val commitObj = jsonArray.getJSONObject(i)
                val commit = commitObj.getJSONObject("commit")
                val author = commit.getJSONObject("author")
                val committer = commit.getJSONObject("committer")
                
                val commitInfo = CommitInfo(
                    sha = commitObj.getString("sha"),
                    message = commit.getString("message"),
                    authorName = author.getString("name"),
                    authorEmail = author.getString("email"),
                    commitDate = author.getString("date"),
                    committerName = committer.getString("name"),
                    committerEmail = committer.getString("email"),
                    commitCommitterDate = committer.getString("date")
                )
                
                commits.add(commitInfo)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга ответа Github API", e)
        }
        
        return commits
    }

    /**
     * Формирует запрос для Kandinsky MCP на основе дат коммитов
     */
    fun createActivityGraphPrompt(commits: List<CommitInfo>): String {
        if (commits.isEmpty()) {
            return "Empty repository with no commits"
        }
        
        // Группируем коммиты по дням
        val commitsByDay = commits.groupBy { commit ->
            val date = commit.commitDate.substring(0, 10) // Берем только дату без времени
            date
        }
        
        // Сортируем по дате
        val sortedDays = commitsByDay.keys.sorted()
        
        // Создаем описание активности
        val activityDescription = buildString {
            append("GitHub repository activity graph showing commit frequency over time. ")
            append("Repository: $REPO_OWNER/$REPO_NAME. ")
            append("Total commits: ${commits.size}. ")
            
            if (sortedDays.size > 1) {
                val firstDay = sortedDays.first()
                val lastDay = sortedDays.last()
                append("Activity period: from $firstDay to $lastDay. ")
            }
            
            append("Style: modern, clean, professional chart with bars or line graph. ")
            append("Colors: blue and green theme. ")
            append("Background: white or light gray. ")
            append("Include commit count labels and date axis.")
        }
        
        Log.d(TAG, "Создан промпт для Kandinsky: $activityDescription")
        return activityDescription
    }

    /**
     * Получает статистику активности по дням недели
     */
    fun getActivityStatsByDayOfWeek(commits: List<CommitInfo>): Map<String, Int> {
        val dayStats = mutableMapOf<String, Int>()
        
        commits.forEach { commit ->
            try {
                val date = commit.commitDate.substring(0, 10)
                val calendar = Calendar.getInstance()
                calendar.time = dateFormat.parse(date) ?: return@forEach
                
                val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SUNDAY -> "Sunday"
                    Calendar.MONDAY -> "Monday"
                    Calendar.TUESDAY -> "Tuesday"
                    Calendar.WEDNESDAY -> "Wednesday"
                    Calendar.THURSDAY -> "Thursday"
                    Calendar.FRIDAY -> "Friday"
                    Calendar.SATURDAY -> "Saturday"
                    else -> "Unknown"
                }
                
                dayStats[dayOfWeek] = (dayStats[dayOfWeek] ?: 0) + 1
            } catch (e: Exception) {
                Log.w(TAG, "Ошибка парсинга даты коммита: ${commit.commitDate}", e)
            }
        }
        
        return dayStats
    }
}

/**
 * Информация о коммите
 */
data class CommitInfo(
    val sha: String,
    val message: String,
    val authorName: String,
    val authorEmail: String,
    val commitDate: String,
    val committerName: String,
    val committerEmail: String,
    val commitCommitterDate: String
)
