package com.example.ai_advent_25

import com.example.ai_advent_25.data.agents.factory.AgentRepositoryFactory
import com.example.ai_advent_25.data.network.NetworkModule
import com.example.ai_advent_25.data.network.NetworkProvider
import org.junit.Test
import org.junit.Assert.*

class NetworkArchitectureTest {
    
    @Test
    fun testNetworkModuleImplementsNetworkProvider() {
        // Проверяем, что NetworkModule реализует NetworkProvider
        assertTrue(NetworkModule is NetworkProvider)
    }
    
    @Test
    fun testAgentRepositoryFactoryCreatesRepositories() {
        val apiKey = "test_api_key"
        
        // Проверяем создание репозиториев через фабрику
        val travelRepository = AgentRepositoryFactory.createTravelAssistAgentRepository(apiKey)
        val expertRepository = AgentRepositoryFactory.createExpertReviewerAgentRepository(apiKey)
        
        assertNotNull(travelRepository)
        assertNotNull(expertRepository)
    }
    
    @Test
    fun testNetworkModuleSingleton() {
        // Проверяем, что NetworkModule является singleton
        val instance1 = NetworkModule
        val instance2 = NetworkModule
        
        assertSame(instance1, instance2)
    }
    
    @Test
    fun testNetworkProviderInterface() {
        // Проверяем, что NetworkProvider является интерфейсом
        assertTrue(NetworkProvider::class.java.isInterface)
    }
}
