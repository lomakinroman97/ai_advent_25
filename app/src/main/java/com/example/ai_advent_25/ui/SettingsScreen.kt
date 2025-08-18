package com.example.ai_advent_25.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.ai_advent_25.data.SimpleKandinskyReport
import com.example.ai_advent_25.data.agents.RepositoryActivityStats
import com.example.ai_advent_25.ui.ActivityGraphStep

@Composable
fun SettingsScreen(
    apiKey: String,
    onBackPressed: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context, apiKey)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF8F0), Color(0xFFFFF0E6))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF0000), Color(0xFFFF6B35))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBackPressed,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Настройки",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            
                            // Кнопка изменения токена (показываем только если токен установлен)
                            if (uiState.githubToken.isNotBlank()) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = { viewModel.setGithubToken("") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = "Изменить токен",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Показываем карточку ввода токена только если токен не установлен
                if (uiState.githubToken.isBlank()) {
                    item {
                        GithubTokenInputCard(
                            currentToken = uiState.githubToken,
                            onTokenSet = { token -> viewModel.setGithubToken(token) }
                        )
                    }
                }
                
                item {
                    MinimalReportButton(
                        onClick = { viewModel.generateKandinskyReport() },
                        isLoading = uiState.isGeneratingReport,
                        title = "Отчет Kandinsky MCP",
                        icon = Icons.Default.PlayArrow,
                        step = ActivityGraphStep.IDLE
                    )
                }
                
                item {
                    // Временное логирование для отладки
                    LaunchedEffect(uiState.activityGraphStep) {
                        android.util.Log.d("SettingsScreen", "ActivityGraphStep изменился: ${uiState.activityGraphStep}")
                    }
                    
                    MinimalReportButton(
                        onClick = { viewModel.generateActivityGraph() },
                        isLoading = uiState.isGeneratingActivityGraph,
                        title = getActivityGraphButtonTitle(uiState.activityGraphStep),
                        icon = Icons.Default.PlayArrow,
                        step = uiState.activityGraphStep
                    )
                }
                if (uiState.simpleReport != null) {
                    item { 
                        MinimalReportCard(report = uiState.simpleReport!!) 
                    }
                }
                
                if (uiState.repositoryStats != null) {
                    item {
                        RepositoryActivityCard(
                            stats = uiState.repositoryStats!!,
                            imagePath = uiState.activityGraphPath
                        )
                    }
                }
            }
        }
        
        // Error Card
        if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Error",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Close",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalReportButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    title: String,
    icon: ImageVector,
    step: ActivityGraphStep = ActivityGraphStep.IDLE
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = getButtonColor(isLoading, step),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Generate",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Текст
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
fun MinimalReportCard(report: SimpleKandinskyReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = Color(0xFFFFF3E0), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Report",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Отчет о работе Kandinsky",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFF9800)
                )
            }
            
            // Основные метрики
            SimpleReportMetrics(report)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Краткий анализ
            if (report.briefAnalysis.isNotBlank()) {
                Text(
                    text = report.briefAnalysis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF333333),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SimpleReportMetrics(report: SimpleKandinskyReport) {
    Column {
        // Основные числа
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SimpleMetricItem("Всего", "${report.totalRequests}", Color(0xFF2196F3))
            SimpleMetricItem("Успешно", "${report.successfulRequests}", Color(0xFF4CAF50))
            SimpleMetricItem("Ошибки", "${report.failedRequests}", Color(0xFFF44336))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Статистика по городам
        if (report.cityStats.isNotEmpty()) {
            Text(
                text = "🏙️ Города",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF7B1FA2)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            report.cityStats.take(5).forEach { city ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = city.cityName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = "${city.requestCount} раз",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun SimpleMetricItem(title: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RepositoryActivityCard(
    stats: RepositoryActivityStats,
    imagePath: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = Color(0xFFE3F2FD), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Activity",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Активность репозитория",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2196F3)
                )
            }
            
            // Основные метрики
            RepositoryActivityMetrics(stats)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Изображение графика
            if (imagePath != null) {
                Text(
                    text = "График активности:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val context = LocalContext.current
                val painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(imagePath)
                        .build()
                )
                
                Image(
                    painter = painter,
                    contentDescription = "Repository Activity Graph",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun RepositoryActivityMetrics(stats: RepositoryActivityStats) {
    Column {
        // Основные числа
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SimpleMetricItem("Коммитов", "${stats.totalCommits}", Color(0xFF2196F3))
            SimpleMetricItem("Дней", "${stats.totalDays}", Color(0xFF4CAF50))
            SimpleMetricItem("В день", "%.1f".format(stats.averageCommitsPerDay), Color(0xFFFF9800))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Статистика по дням недели
        if (stats.dayOfWeekStats.isNotEmpty()) {
            Text(
                text = "📅 Активность по дням недели",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF7B1FA2)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            stats.dayOfWeekStats.entries.sortedByDescending { it.value }.take(7).forEach { (day, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = "$count коммитов",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        

    }
}

/**
 * Возвращает текст кнопки в зависимости от текущего этапа генерации графика
 */
@Composable
fun getActivityGraphButtonTitle(step: ActivityGraphStep): String {
    val title = when (step) {
        ActivityGraphStep.IDLE -> "График активности репозитория"
        ActivityGraphStep.FETCHING_COMMITS -> "Получение коммитов (Github MCP)"
        ActivityGraphStep.GENERATING_GRAPH -> "Генерация графика (Kandinsky MCP)"
    }
    
    // Временное логирование для отладки
    android.util.Log.d("SettingsScreen", "getActivityGraphButtonTitle вызван с step=$step, возвращает: $title")
    
    return title
}

/**
 * Возвращает цвет кнопки в зависимости от этапа и состояния загрузки
 */
@Composable
fun getButtonColor(isLoading: Boolean, step: ActivityGraphStep): Color {
    return when {
        isLoading -> when (step) {
            ActivityGraphStep.FETCHING_COMMITS -> Color(0xFFFF9800) // Оранжевый для Github MCP
            ActivityGraphStep.GENERATING_GRAPH -> Color(0xFF9C27B0) // Фиолетовый для Kandinsky MCP
            else -> Color(0xFFE0E0E0) // Серый по умолчанию
        }
        else -> Color(0xFF4CAF50) // Зеленый для готового состояния
    }
}

@Composable
fun GithubTokenInputCard(
    currentToken: String,
    onTokenSet: (String) -> Unit
) {
    var tokenInput by remember { mutableStateOf(currentToken) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = Color(0xFFE8F5E8), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Github",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Github токен",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF4CAF50)
                )
            }
            
            if (currentToken.isBlank()) {
                // Поле ввода токена
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Введите Github токен") },
                    placeholder = { Text("github_pat_...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { 
                        if (tokenInput.isNotBlank()) {
                            onTokenSet(tokenInput)
                        }
                    },
                    enabled = tokenInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Добавить токен")
                }
            } else {
                // Показываем текущий токен (скрытый)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Токен установлен: ${currentToken.take(8)}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50)
                    )
                    
                    Button(
                        onClick = { onTokenSet("") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Убрать", color = Color.White)
                    }
                }
            }
        }
    }
}
