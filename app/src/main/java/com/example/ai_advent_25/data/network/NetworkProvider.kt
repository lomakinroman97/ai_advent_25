package com.example.ai_advent_25.data.network

import com.example.ai_advent_25.data.network.api.ChatApi
import com.example.ai_advent_25.data.network.api.DeepseekApi

interface NetworkProvider {
    fun getChatApi(): ChatApi
    fun getDeepseekApi(): DeepseekApi
}
