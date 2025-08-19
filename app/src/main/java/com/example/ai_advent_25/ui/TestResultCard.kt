package com.example.ai_advent_25.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ai_advent_25.data.agents.TestResult

@Composable
fun TestResultCard(testResult: TestResult) {
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
                        .background(
                            color = if (testResult.success) Color(0xFF4CAF50) else Color(0xFFF44336),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Test Result",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Результат тестов",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (testResult.success) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            // Основные метрики
            TestResultMetrics(testResult)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Краткий вывод
            if (testResult.output.isNotBlank()) {
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = testResult.output,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(12.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun TestResultMetrics(testResult: TestResult) {
    Column {
        // Основные числа
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SimpleMetricItem("Всего", "${testResult.testCount}", Color(0xFF2196F3))
            SimpleMetricItem("Пройдено", "${testResult.passedTests}", Color(0xFF4CAF50))
            SimpleMetricItem("Провалено", "${testResult.failedTests}", Color(0xFFF44336))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Статус выполнения
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (testResult.success) Color(0xFF4CAF50) else Color(0xFFF44336),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (testResult.success) "Все тесты пройдены успешно!" else "Есть проваленные тесты",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (testResult.success) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
        
        // Код завершения
        if (testResult.exitCode != 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Код завершения: ${testResult.exitCode}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
