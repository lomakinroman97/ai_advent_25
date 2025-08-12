package com.example.ai_advent_25.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import com.example.ai_advent_25.data.TravelRecommendation
import com.example.ai_advent_25.data.CityRecommendation
import com.example.ai_advent_25.data.QuestionData

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("В путь!") }
    var showApiKeyDialog by remember { mutableStateOf(!uiState.apiKeySet) }
    var apiKeyText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8F0),
                        Color(0xFFFFF0E6)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF0000),
                                    Color(0xFFFF6B35)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = Color.White,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Я",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFFF0000),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "YandexGPT",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.clearChat() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear chat"
                            )
                        }
                    }
                }
            }

            // Messages
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages) { message ->
                    // Message Item
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.isUser)
                                Color(0xFFFFE6E6)
                            else
                                Color.White.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = if (message.isUser) 
                                            Color(0xFFFF0000) 
                                        else 
                                            Color(0xFFFF6B35),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (message.isUser) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "User",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Я",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Message Content
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (message.isUser) "Вы" else "YandexGPT",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (message.isUser) 
                                        Color(0xFFCC0000) 
                                    else 
                                        Color(0xFFFF6B35)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Основной ответ - показываем только если нет структурированных рекомендаций
                                if (message.structuredResponse == null) {
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Start,
                                        color = Color(0xFF333333)
                                    )
                                }
                                
                                // Отображение структурированных рекомендаций
                                message.structuredResponse?.let { recommendation ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TravelRecommendationCard(recommendation = recommendation)
                                }
                                
                                // Отображение данных о вопросе
                                message.questionData?.let { questionData ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    QuestionDataCard(questionData = questionData)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = formatTimestamp(message.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF999999)
                                )
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    item {
                        // Loading Indicator
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.8f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFFF0000),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "AI-агент анализирует ваши предпочтения...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Это может занять несколько секунд",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF999999),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Input - в стиле Telegram
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(), // Автоматический отступ от клавиатуры
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Input Field - компактное поле в стиле Telegram
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color(0xFF333333),
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(
                                Color(0xFFFF0000)
                            )
                        )
                        
                        if (messageText.isEmpty()) {
                            Text(
                                text = "В путь!",
                                color = Color(0xFF999999),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 16.dp),
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button - компактная кнопка
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        },
                        enabled = messageText.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (messageText.isNotBlank() && !uiState.isLoading) 
                                Color(0xFFFF0000) 
                            else 
                                Color(0xFFE0E0E0),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE0E0E0),
                            disabledContentColor = Color(0xFF999999)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Error Card
        if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFE6E6)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFFF0000),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.error!!,
                            color = Color(0xFFCC0000),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFFCC0000)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.retryLastMessage() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0000),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Retry",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Повторить")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { viewModel.clearChat() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF666666),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Начать заново")
                        }
                    }
                }
            }
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "Введите API ключ YandexGPT",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF0000)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Для работы с YandexGPT необходимо ввести ваш API ключ.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("API ключ") },
                        placeholder = { Text("Введите ваш API ключ...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF0000),
                            unfocusedBorderColor = Color(0xFFCCCCCC),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (apiKeyText.isNotBlank()) {
                            viewModel.setApiKey(apiKeyText)
                            showApiKeyDialog = false
                        }
                    },
                    enabled = apiKeyText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF0000),
                        contentColor = Color.White
                    )
                ) {
                    Text("OK")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun QuestionDataCard(questionData: QuestionData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок вопроса
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "❓",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Вопрос ${questionData.questionNumber} из 4",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1976D2)
                )
            }
            
            // Прогресс-бар
            LinearProgressIndicator(
                progress = { (questionData.questionNumber - 1) / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF1976D2),
                trackColor = Color(0xFFE0E0E0)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Собранная информация
            if (questionData.collectedInfo.isNotEmpty()) {
                Text(
                    text = "📋 Уже известно:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                questionData.collectedInfo.forEach { info ->
                    Text(
                        text = "• $info",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Следующий вопрос
            Text(
                text = questionData.nextQuestion,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}

@Composable
fun TravelRecommendationCard(recommendation: TravelRecommendation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок
            Text(
                text = "🎯 Рекомендации для путешествия",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Рекомендуемые города
            recommendation.recommendations.forEach { city ->
                CityRecommendationItem(city = city)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Общий бюджет
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3E5F5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💰",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Общий бюджет: ${recommendation.totalBudget}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF7B1FA2)
                    )
                }
            }
        }
    }
}

@Composable
fun CityRecommendationItem(city: CityRecommendation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Название города
            Text(
                text = "🏙️ ${city.city}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Описание
            Text(
                text = city.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Достопримечательности
            Text(
                text = "🎯 Достопримечательности:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            city.attractions.forEach { attraction ->
                Text(
                    text = "• $attraction",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Стоимости
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CostItem("💰", "Общая стоимость", city.costs)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Время
            Text(
                text = "⏰ ${city.bestTime}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun CostItem(icon: String, label: String, cost: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
        Text(
            text = cost,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF1976D2),
            textAlign = TextAlign.Center
        )
    }
}
