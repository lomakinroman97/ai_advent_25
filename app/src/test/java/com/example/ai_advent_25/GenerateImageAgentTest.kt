package com.example.ai_advent_25

import com.example.ai_advent_25.data.CityRecommendation
import com.example.ai_advent_25.data.GeneratedImage
import com.example.ai_advent_25.data.agents.GenerateImageAgentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.mock
import android.content.Context

class GenerateImageAgentTest {

    @Test
    fun `generateCityImage should create valid prompt`() = runTest {
        // Arrange
        val mockContext = mock(Context::class.java)
        val repository = GenerateImageAgentRepository(
            context = mockContext,
            kandinskyApiKey = "test_key",
            kandinskySecretKey = "test_secret"
        )
        
        val cityRecommendation = CityRecommendation(
            city = "Сочи",
            description = "Курортный город на Черноморском побережье",
            attractions = listOf("Красная Поляна", "Олимпийский парк"),
            costs = "50000",
            bestTime = "Лето"
        )
        
        // Act
        val result = repository.generateCityImage(cityRecommendation)
        
        // Assert
        assertTrue(result.isSuccess)
        val generatedImage = result.getOrNull()
        assertNotNull(generatedImage)
        assertEquals("Сочи", generatedImage!!.cityName)
        assertTrue(generatedImage.prompt.contains("Сочи"))
        assertTrue(generatedImage.prompt.contains("Курортный город на Черноморском побережье"))
        assertTrue(generatedImage.prompt.contains("Красная Поляна"))
        assertTrue(generatedImage.prompt.contains("Лето"))
    }

    @Test
    fun `generateCityImage should handle empty attractions`() = runTest {
        // Arrange
        val mockContext = mock(Context::class.java)
        val repository = GenerateImageAgentRepository(
            context = mockContext,
            kandinskyApiKey = "test_key",
            kandinskySecretKey = "test_secret"
        )
        
        val cityRecommendation = CityRecommendation(
            city = "Москва",
            description = "Столица России",
            attractions = emptyList(),
            costs = "100000",
            bestTime = "Весна"
        )
        
        // Act
        val result = repository.generateCityImage(cityRecommendation)
        
        // Assert
        assertTrue(result.isSuccess)
        val generatedImage = result.getOrNull()
        assertNotNull(generatedImage)
        assertEquals("Москва", generatedImage!!.cityName)
        assertTrue(generatedImage.prompt.contains("Москва"))
        assertTrue(generatedImage.prompt.contains("Столица России"))
    }
}
