package com.example.ai_advent_25.data.network

import com.example.ai_advent_25.data.network.api.ChatApi

interface NetworkProvider {
    fun getChatApi(): ChatApi
}
