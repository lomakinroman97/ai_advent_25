package com.example.ai_advent_25.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpandableContent(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        // Заголовок с иконкой
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF666666),
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = Color(0xFF666666),
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Выпадающий контент
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
                animationSpec = tween(200),
                initialOffsetY = { -it }
            ),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                animationSpec = tween(200),
                targetOffsetY = { -it }
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Start,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
