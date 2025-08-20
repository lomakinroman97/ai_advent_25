package com.example.ai_advent_25.data.testing

/**
 * Простой класс для валидации паролей
 * Используется для демонстрации микро-JUnit движка
 */
object PasswordValidator {
    
    /**
     * Проверяет, является ли пароль валидным
     * @param password пароль для проверки
     * @return true если пароль валиден, false иначе
     */
    fun isValid(password: String): Boolean {
        if (password.isBlank()) {
            return false
        }
        return password.length >= 8
    }
    
    /**
     * Проверяет, содержит ли пароль цифры
     * @param password пароль для проверки
     * @return true если пароль содержит цифры, false иначе
     */
    fun containsDigits(password: String): Boolean {
        return password.any { it.isDigit() }
    }
    
    /**
     * Проверяет, содержит ли пароль заглавные буквы
     * @param password пароль для проверки
     * @return true если пароль содержит заглавные буквы, false иначе
     */
    fun containsUppercase(password: String): Boolean {
        return password.any { it.isUpperCase() }
    }
}

/**
 * Простой калькулятор для демонстрации тестирования
 */
class Calculator {
    
    /**
     * Складывает два числа
     * @param a первое число
     * @param b второе число
     * @return сумма чисел
     */
    fun add(a: Int, b: Int): Int {
        return a + b
    }
    
    /**
     * Вычитает второе число из первого
     * @param a первое число
     * @param b второе число
     * @return разность чисел
     */
    fun subtract(a: Int, b: Int): Int {
        return a - b
    }
    
    /**
     * Умножает два числа
     * @param a первое число
     * @param b второе число
     * @return произведение чисел
     */
    fun multiply(a: Int, b: Int): Int {
        return a * b
    }
    
    /**
     * Делит первое число на второе
     * @param a делимое
     * @param b делитель
     * @return частное чисел
     * @throws IllegalArgumentException если делитель равен нулю
     */
    fun divide(a: Int, b: Int): Double {
        if (b == 0) {
            throw IllegalArgumentException("Деление на ноль недопустимо")
        }
        return a.toDouble() / b
    }
    
    /**
     * Возводит число в степень
     * @param base основание
     * @param exponent показатель степени
     * @return результат возведения в степень
     */
    fun power(base: Int, exponent: Int): Int {
        if (exponent < 0) {
            throw IllegalArgumentException("Отрицательная степень не поддерживается")
        }
        return if (exponent == 0) 1 else base * power(base, exponent - 1)
    }
}

/**
 * Простой класс для работы со строками
 */
class StringUtils {
    
    /**
     * Проверяет, является ли строка палиндромом
     * @param text строка для проверки
     * @return true если строка является палиндромом, false иначе
     */
    fun isPalindrome(text: String): Boolean {
        if (text.isBlank()) return false
        val cleanText = text.lowercase().replace(Regex("[^a-zA-Zа-яА-Я0-9]"), "")
        return cleanText == cleanText.reversed()
    }
    
    /**
     * Подсчитывает количество слов в строке
     * @param text строка для подсчета
     * @return количество слов
     */
    fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).size
    }
    
    /**
     * Переворачивает строку
     * @param text строка для переворота
     * @return перевернутая строка
     */
    fun reverse(text: String): String {
        return text.reversed()
    }
    
    /**
     * Удаляет дублирующиеся символы из строки
     * @param text строка для обработки
     * @return строка без дублирующихся символов
     */
    fun removeDuplicates(text: String): String {
        return text.toSet().joinToString("")
    }
}
