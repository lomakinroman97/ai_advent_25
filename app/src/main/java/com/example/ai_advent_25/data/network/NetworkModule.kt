package com.example.ai_advent_25.data.network

import com.example.ai_advent_25.data.network.api.ChatApi
import com.example.ai_advent_25.data.network.api.DeepseekApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule : NetworkProvider {
    
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    private val yandexRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val deepseekRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConstants.DEEPSEEK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    override fun getChatApi(): ChatApi {
        return yandexRetrofit.create(ChatApi::class.java)
    }
    
    override fun getDeepseekApi(): DeepseekApi {
        return deepseekRetrofit.create(DeepseekApi::class.java)
    }
}
