package com.example.kt4_11

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.system.measureTimeMillis
import kotlin.random.Random

@Serializable
data class User(val id: Int, val name: String)

@Serializable
data class SaleItem(val product: String, val qty: Int, val revenue: Int)

@Serializable
data class SalesData(val today: String, val items: List<SaleItem>)

@Serializable
data class Weather(val city: String, val temp: Int, val condition: String)

suspend fun main() = runBlocking {
    println("Начинаем параллельную загрузку данных...")

    val totalTime = measureTimeMillis {
        try {
            val results = coroutineScope {
                val usersDeferred = async { loadUsersWithDelay() }
                val salesDeferred = async { loadSalesWithDelay() }
                val weatherDeferred = async { loadWeatherWithDelay() }

                try {
                    val users = usersDeferred.await()
                    val sales = salesDeferred.await()
                    val weather = weatherDeferred.await()

                    Triple(users, sales, weather)
                } catch (e: Exception) {
                    println("Ошибка при выполнении задач: ${e.message}")
                    null
                }
            }

            results?.let { (users, sales, weather) ->
                println("\n--- РЕЗУЛЬТАТЫ ЗАГРУЗКИ ---")
                println("Пользователи (${users.size}): ${users.joinToString()}")
                println("Продажи за день: ${sales.map { (product, qty) -> "$product: $qty шт" }.joinToString()}")
                println("Погода: ${weather.joinToString()}")
            }

        } catch (e: Exception) {
            println("Критическая ошибка: ${e.message}")
        }
    }

    println("\nОбщее время выполнения: ${totalTime}мс")
}

suspend fun loadUsersWithDelay(): List<String> {
    return withContext(Dispatchers.IO) {
        simulateNetworkDelay(1800)
        simulateRandomFailure("users.json")

        val json = """
            [
                {"id":1, "name":"Alice"},
                {"id":2, "name":"Bob"},
                {"id":3, "name":"Ivan"},
                {"id":4, "name":"Olga"}
            ]
        """.trimIndent()

        val users = Json.decodeFromString<List<User>>(json)
        users.map { it.name }
    }
}

suspend fun loadSalesWithDelay(): Map<String, Int> {
    return withContext(Dispatchers.IO) {
        simulateNetworkDelay(1200)
        simulateRandomFailure("sales.json")

        val json = """
            {
                "today": "2025-12-01",
                "items": [
                    {"product":"Coffee", "qty":42, "revenue":1680},
                    {"product":"Tea", "qty":19, "revenue":475}
                ]
            }
        """.trimIndent()

        val salesData = Json.decodeFromString<SalesData>(json)
        salesData.items.associate { it.product to it.qty }
    }
}

suspend fun loadWeatherWithDelay(): List<String> {
    return withContext(Dispatchers.IO) {
        simulateNetworkDelay(2500)
        simulateRandomFailure("weather.json")

        val json = """
            [
                {"city":"Moscow", "temp":-18, "condition":"snow"},
                {"city":"New York", "temp":-5, "condition":"cloudy"},
                {"city":"Tokyo", "temp":11, "condition":"rain"}
            ]
        """.trimIndent()

        val weatherList = Json.decodeFromString<List<Weather>>(json)
        weatherList.map { "${it.city}: ${it.temp}°C" }
    }
}

suspend fun simulateNetworkDelay(timeMs: Long) {
    delay(timeMs)
}

fun simulateRandomFailure(source: String) {
    if (Random.nextInt(100) < 20) { // 20% chance of failure
        throw RuntimeException("Сбой при загрузке $source (случайная ошибка)")
    }
}