package com.example.ai_advent_25.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_advent_25.data.ChatMessage
import com.example.ai_advent_25.data.ChatRepository
import com.example.ai_advent_25.data.ChatUiMessage
import com.example.ai_advent_25.data.ParseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var chatRepository: ChatRepository? = null

    fun setApiKey(apiKey: String) {
        chatRepository = ChatRepository(apiKey)
        _uiState.update { it.copy(apiKeySet = true) }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        if (chatRepository == null) {
            _uiState.update { currentState ->
                currentState.copy(
                    error = "API ключ не установлен. Пожалуйста, введите ваш API ключ."
                )
            }
            return
        }

        val userMessage = ChatUiMessage(
            content = message,
            isUser = true
        )

        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                isLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val chatMessages = mutableListOf<ChatMessage>()

                _uiState.value.messages.forEach { msg ->
                    val role = if (msg.isUser) "user" else "assistant"
                    chatMessages.add(ChatMessage(role = role, text = msg.content))
                }
                val response = chatRepository!!.sendMessage(chatMessages)

                response.fold(
                    onSuccess = { result ->
                        
                        val assistantMessage = when (result) {
                            is ParseResult.Success -> {
                                ChatUiMessage(
                                    content = result.structuredResponse.content,
                                    isUser = false,
                                    metadata = result.structuredResponse.metadata,
                                    originalJson = result.originalResponse
                                )
                            }
                            is ParseResult.Fallback -> {
                                ChatUiMessage(
                                    content = result.originalResponse,
                                    isUser = false
                                )
                            }
                        }
                        
                        _uiState.update { currentState ->
                            currentState.copy(
                                messages = currentState.messages + assistantMessage,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                error = exception.message ?: "Unknown error occurred",
                                isLoading = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message ?: "Unknown error occurred",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearChat() {
        _uiState.update { ChatUiState() }
    }
}

data class ChatUiState(
    val messages: List<ChatUiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val apiKeySet: Boolean = false
)
