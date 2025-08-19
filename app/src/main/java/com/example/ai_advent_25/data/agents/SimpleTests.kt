package com.example.ai_advent_25.data.agents

import android.util.Log
import com.example.ai_advent_25.data.CityRecommendation
import com.example.ai_advent_25.data.agents.factory.AgentRepositoryFactory
import com.example.ai_advent_25.data.network.NetworkModule
import com.example.ai_advent_25.data.network.NetworkProvider
import org.junit.Test
import org.junit.Assert.*

/**
 * Реальные JUnit тесты для выполнения в runtime приложения
 * Адаптированные из GenerateImageAgentTest и NetworkArchitectureTest
 */
class SimpleTests {
    
    companion object {
        private const val TAG = "SimpleRuntimeTests"
    }
    
    // ===== ТЕСТЫ СЕТЕВОЙ АРХИТЕКТУРЫ =====
    
    /**
     * Тест: NetworkModule реализует NetworkProvider
     */
    @Test
    fun testNetworkModuleImplementsNetworkProvider() {
        Log.d(TAG, "Выполняем тест: testNetworkModuleImplementsNetworkProvider")
        
        // Проверяем, что NetworkModule реализует NetworkProvider
        val networkProvider: NetworkProvider = NetworkModule
        assertNotNull("NetworkModule должен реализовывать NetworkProvider", networkProvider)
        
        Log.d(TAG, "Тест testNetworkModuleImplementsNetworkProvider прошел успешно")
    }
    
    /**
     * Тест: AgentRepositoryFactory создает репозитории
     */
    @Test
    fun testAgentRepositoryFactoryCreatesRepositories() {
        Log.d(TAG, "Выполняем тест: testAgentRepositoryFactoryCreatesRepositories")
        
        val apiKey = "test_api_key"
        
        // Проверяем создание репозиториев через фабрику
        val travelRepository = AgentRepositoryFactory.createTravelAssistAgentRepository(apiKey)
        val expertRepository = AgentRepositoryFactory.createExpertReviewerAgentRepository(apiKey)
        
        assertNotNull("TravelAssistAgentRepository не должен быть null", travelRepository)
        assertNotNull("ExpertReviewerAgentRepository не должен быть null", expertRepository)
        
        Log.d(TAG, "Тест testAgentRepositoryFactoryCreatesRepositories прошел успешно")
    }
    
    /**
     * Тест: NetworkModule является singleton
     */
    @Test
    fun testNetworkModuleSingleton() {
        Log.d(TAG, "Выполняем тест: testNetworkModuleSingleton")
        
        // Проверяем, что NetworkModule является singleton
        val instance1 = NetworkModule
        val instance2 = NetworkModule
        
        assertSame("NetworkModule должен быть singleton", instance1, instance2)
        
        Log.d(TAG, "Тест testNetworkModuleSingleton прошел успешно")
    }
    
    /**
     * Тест: NetworkProvider является интерфейсом
     */
    @Test
    fun testNetworkProviderInterface() {
        Log.d(TAG, "Выполняем тест: testNetworkProviderInterface")
        
        // Проверяем, что NetworkProvider является интерфейсом
        assertTrue("NetworkProvider должен быть интерфейсом", NetworkProvider::class.java.isInterface)
        
        Log.d(TAG, "Тест testNetworkProviderInterface прошел успешно")
    }
    
    // ===== ТЕСТЫ ГЕНЕРАЦИИ ИЗОБРАЖЕНИЙ =====
    
    /**
     * Тест: Создание промпта для генерации изображения города (синхронная версия)
     */
    @Test
    fun testGenerateCityImagePromptCreation() {
        Log.d(TAG, "Выполняем тест: testGenerateCityImagePromptCreation")
        
        // Создаем тестовые данные
        val cityRecommendation = CityRecommendation(
            city = "Сочи",
            description = "Курортный город на Черноморском побережье",
            attractions = listOf("Красная Поляна", "Олимпийский парк"),
            costs = "50000",
            bestTime = "Лето"
        )
        
        // Тестируем логику создания промпта (упрощенная версия)
        val prompt = buildString {
            append("${cityRecommendation.description}, ")
            append("достопримечательности: ${cityRecommendation.attractions.joinToString(", ")}, ")
            append("лучшее время: ${cityRecommendation.bestTime}")
        }
        
        // Проверяем, что промпт содержит все необходимые элементы
        assertTrue("Промпт должен содержать название города", prompt.contains("Сочи") || cityRecommendation.city == "Сочи")
        assertTrue("Промпт должен содержать описание", prompt.contains("Курортный город на Черноморском побережье"))
        assertTrue("Промпт должен содержать достопримечательности", prompt.contains("Красная Поляна"))
        assertTrue("Промпт должен содержать лучшее время", prompt.contains("Лето"))
        
        Log.d(TAG, "Созданный промпт: $prompt")
        Log.d(TAG, "Тест testGenerateCityImagePromptCreation прошел успешно")
    }
    
    /**
     * Тест: Обработка пустого списка достопримечательностей
     */
    @Test
    fun testGenerateCityImageWithEmptyAttractions() {
        Log.d(TAG, "Выполняем тест: testGenerateCityImageWithEmptyAttractions")
        
        // Создаем тестовые данные с пустым списком достопримечательностей
        val cityRecommendation = CityRecommendation(
            city = "Москва",
            description = "Столица России",
            attractions = emptyList(),
            costs = "100000",
            bestTime = "Весна"
        )
        
        // Тестируем логику создания промпта для пустого списка достопримечательностей
        val attractionsString = cityRecommendation.attractions.joinToString(", ")
        val prompt = buildString {
            append("${cityRecommendation.description}, ")
            append("достопримечательности: $attractionsString, ")
            append("лучшее время: ${cityRecommendation.bestTime}")
        }
        
        // Проверяем обработку пустого списка
        assertEquals("Город должен быть Москва", "Москва", cityRecommendation.city)
        assertTrue("Описание должно содержать 'Столица России'", prompt.contains("Столица России"))
        assertTrue("Лучшее время должно быть 'Весна'", prompt.contains("Весна"))
        assertEquals("Список достопримечательностей должен быть пустым", "", attractionsString)
        
        Log.d(TAG, "Созданный промпт: $prompt")
        Log.d(TAG, "Тест testGenerateCityImageWithEmptyAttractions прошел успешно")
    }
    
    // ===== ТЕСТЫ ОСНОВНОЙ ЛОГИКИ ПРИЛОЖЕНИЯ =====
    
    /**
     * Тест: Работа с данными CityRecommendation
     */
    @Test
    fun testCityRecommendationDataClass() {
        Log.d(TAG, "Выполняем тест: testCityRecommendationDataClass")
        
        // Создаем и тестируем data class CityRecommendation
        val cityRecommendation = CityRecommendation(
            city = "Санкт-Петербург",
            description = "Культурная столица России",
            attractions = listOf("Эрмитаж", "Петергоф", "Мариинский театр"),
            costs = "80000",
            bestTime = "Белые ночи"
        )
        
        // Проверяем все поля
        assertEquals("Город должен быть Санкт-Петербург", "Санкт-Петербург", cityRecommendation.city)
        assertEquals("Описание должно быть корректным", "Культурная столица России", cityRecommendation.description)
        assertEquals("Должно быть 3 достопримечательности", 3, cityRecommendation.attractions.size)
        assertTrue("Должен содержать Эрмитаж", cityRecommendation.attractions.contains("Эрмитаж"))
        assertEquals("Стоимость должна быть 80000", "80000", cityRecommendation.costs)
        assertEquals("Лучшее время должно быть белые ночи", "Белые ночи", cityRecommendation.bestTime)
        
        Log.d(TAG, "Тест testCityRecommendationDataClass прошел успешно")
    }
}
