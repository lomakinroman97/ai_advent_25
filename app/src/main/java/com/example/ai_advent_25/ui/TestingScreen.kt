package com.example.ai_advent_25.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai_advent_25.data.testing.*
import com.example.ai_advent_25.data.agents.LLMTestGeneratorAgentRepository
import com.example.ai_advent_25.data.network.NetworkModule
import com.example.ai_advent_25.data.AppSettings
import kotlinx.coroutines.launch

/**
 * Экран для демонстрации работы микро-JUnit движка
 */
@Composable
fun TestingScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedClass by remember { mutableStateOf<Class<*>?>(null) }
    var testResults by remember { mutableStateOf<MicroJUnitTestResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isGeneratingTests by remember { mutableStateOf(false) }
    var generatedTestCode by remember { mutableStateOf("") }
    
    // Состояние для отслеживания инициализации
    var isAppSettingsInitialized by remember { mutableStateOf(false) }
    
    // Инициализируем AppSettings
    LaunchedEffect(context) {
        AppSettings.initialize(context)
        isAppSettingsInitialized = true
    }
    
    // Создаем агент для генерации и выполнения тестов только после инициализации
    val llmTestAgent = remember(isAppSettingsInitialized) {
        if (isAppSettingsInitialized) {
            val apiKey = AppSettings.getApiKey()
            println("🔑 TestingScreen: API ключ при создании агента: '${if (apiKey.isBlank()) "пустой" else "установлен (длина: ${apiKey.length})"}'")
            
            LLMTestGeneratorAgentRepository(
                apiKey = apiKey,
                networkProvider = NetworkModule,
                microJUnitEngine = MicroJUnitEngine()
            )
        } else {
            println("⏳ TestingScreen: AppSettings еще не инициализирован")
            null
        }
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    val availableClasses = listOf(
        Calculator::class.java
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackPressed,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Назад"
                                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        // Выбор класса для тестирования
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Выберите класс для тестирования:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                availableClasses.forEach { clazz ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedClass == clazz,
                            onClick = { selectedClass = clazz }
                        )
                        Text(
                            text = clazz.simpleName,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Кнопки действий
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    selectedClass?.let { clazz ->
                        llmTestAgent?.let { agent ->
                            isGeneratingTests = true
                            testResults = null // Сбрасываем старые результаты
                            
                            coroutineScope.launch {
                                try {
                                    println("🤖 Генерируем тесты через LLM для ${clazz.simpleName}")
                                    
                                    // Создаем промпт для LLM
                                    val prompt = agent.createLLMPrompt(
                                        targetClass = clazz,
                                        testRequirements = "Создай JUnit тесты для всех публичных методов класса ${clazz.simpleName}. Используй assertEquals и конструкторы."
                                    )
                                    
                                    // Генерируем и выполняем тесты
                                    val result = agent.generateAndRunTests(clazz, prompt)
                                    
                                    result.fold(
                                        onSuccess = { testResult ->
                                            println("✅ LLM тесты выполнены успешно!")
                                            testResults = testResult
                                            generatedTestCode = "Тесты сгенерированы LLM и выполнены успешно"
                                        },
                                        onFailure = { error ->
                                            println("❌ Ошибка при генерации/выполнении LLM тестов: ${error.message}")
                                            generatedTestCode = "Ошибка: ${error.message}"
                                        }
                                    )
                                } catch (e: Exception) {
                                    println("💥 Неожиданная ошибка: ${e.message}")
                                    generatedTestCode = "Неожиданная ошибка: ${e.message}"
                                } finally {
                                    isGeneratingTests = false
                                }
                            }
                        } ?: run {
                            println("⚠️ LLM агент еще не инициализирован")
                            generatedTestCode = "Ошибка: LLM агент еще не инициализирован"
                        }
                    }
                },
                enabled = selectedClass != null && !isGeneratingTests && llmTestAgent != null,
                modifier = Modifier.weight(1f)
            ) {
                if (isGeneratingTests) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("🤖 LLM генерирует...")
                    }
                } else {
                    Text("🤖 Генерировать тесты LLM")
                }
            }
            
            // Кнопка "Сбросить результаты" для возможности повторного запуска
            Button(
                onClick = {
                    testResults = null
                    generatedTestCode = ""
                },
                enabled = testResults != null && !isGeneratingTests,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("🔄 Сбросить")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Статус генерации (показываем когда идет генерация или есть ошибка)
        if (isGeneratingTests) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🤖 LLM генерирует и выполняет тесты...",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else if (generatedTestCode.isNotEmpty() && testResults == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📋 Сгенерированные тесты:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(8.dp)
                    ) {
                        item {
                            Text(
                                text = generatedTestCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Результаты тестов (показываем после выполнения)
        if (testResults != null) {
            val results = testResults!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📊 Результаты тестов:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Статистика
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatisticCard(
                            title = "Всего",
                            value = results.testCount.toString(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatisticCard(
                            title = "Пройдено",
                            value = results.passedTests.toString(),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        StatisticCard(
                            title = "Провалено",
                            value = results.failedTests.toString(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Детальные результаты
                    Text(
                        text = "Детали:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(results.testResults) { testResult ->
                            TestResultItem(testResult = testResult)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Общий статус
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (results.success) 
                                MaterialTheme.colorScheme.tertiaryContainer 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = if (results.success) "✅ Все тесты прошли успешно!" else "❌ Есть проваленные тесты",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun StatisticCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun TestResultItem(
    testResult: MicroJUnitSingleTestResult
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (testResult.success) 
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (testResult.success) "✅" else "❌",
                    fontSize = 16.sp
                )
                Text(
                    text = testResult.testName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                testResult.duration?.let { duration ->
                    Text(
                        text = "${duration}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (!testResult.success && testResult.errorMessage != null) {
                Text(
                    text = "Ошибка: ${testResult.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                )
            }
            
            // Результаты assert'ов
            if (testResult.assertionResults.isNotEmpty()) {
                testResult.assertionResults.forEach { assertion ->
                    Row(
                        modifier = Modifier.padding(start = 24.dp, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (assertion.success) "✓" else "✗",
                            fontSize = 12.sp,
                            color = if (assertion.success) 
                                MaterialTheme.colorScheme.tertiary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = assertion.message,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}


