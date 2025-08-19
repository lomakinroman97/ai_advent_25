package com.example.ai_advent_25

import org.junit.Test
import org.junit.Assert.*

class SimpleAppTest {

    @Test
    fun `test basic arithmetic operations`() {
        // Простой тест для проверки базовых математических операций
        assertEquals(4, 2 + 2)
        assertEquals(10, 5 * 2)
        assertEquals(3, 9 / 3)
        assertEquals(1, 5 % 2)
    }

    @Test
    fun `test string operations`() {
        // Тест для проверки строковых операций
        val hello = "Hello"
        val world = "World"
        val combined = "$hello $world"
        
        assertEquals("Hello World", combined)
        assertEquals(11, combined.length)
        assertTrue(combined.contains("Hello"))
        assertTrue(combined.contains("World"))
    }

    @Test
    fun `test list operations`() {
        // Тест для проверки операций со списками
        val numbers = listOf(1, 2, 3, 4, 5)
        
        assertEquals(5, numbers.size)
        assertEquals(1, numbers.first())
        assertEquals(5, numbers.last())
        assertEquals(15, numbers.sum())
        assertEquals(3, numbers[2])
    }

    @Test
    fun `test boolean logic`() {
        // Тест для проверки булевой логики
        assertTrue(true)
        assertFalse(false)
        assertTrue(5 > 3)
        assertFalse(2 > 10)
        assertTrue("test".isNotEmpty())
        assertFalse("".isNotEmpty())
    }
}
