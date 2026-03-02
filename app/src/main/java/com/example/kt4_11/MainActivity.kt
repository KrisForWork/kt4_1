package com.example.kt4_11

import kotlinx.coroutines.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.system.measureTimeMillis
import kotlin.random.Random

data class User(val id: Int, val name: String)
data class SaleItem(val product: String, val qty: Int, val revenue: Int)
data class SalesData(val today: String, val items: List<SaleItem>)
data class Weather(val city: String, val temp: Int, val condition: String)

suspend fun main() = runBlocking {
    println("Starting parallel data loading...")

    val totalTime = measureTimeMillis {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            println("Coroutine error: ${throwable.message}")
        }

        val deferredResults = supervisorScope {
            val usersDeferred = async {
                try {
                    loadUsersWithDelay()
                } catch (e: Exception) {
                    println("Users task failed: ${e.message}")
                    emptyList()
                }
            }

            val salesDeferred = async {
                try {
                    loadSalesWithDelay()
                } catch (e: Exception) {
                    println("Sales task failed: ${e.message}")
                    emptyMap()
                }
            }

            val weatherDeferred = async {
                try {
                    loadWeatherWithDelay()
                } catch (e: Exception) {
                    println("Weather task failed: ${e.message}")
                    emptyList()
                }
            }

            try {
                val users = usersDeferred.await()
                val sales = salesDeferred.await()
                val weather = weatherDeferred.await()

                Triple(users, sales, weather)
            } catch (e: Exception) {
                println("Error waiting for results: ${e.message}")
                Triple(emptyList(), emptyMap(), emptyList())
            }
        }

        with(deferredResults) {
            val (users, sales, weather) = this
            println("\n--- RESULTS ---")
            println("Users (${users.size}): ${users.joinToString()}")
            println("Sales: ${sales.map { (product, qty) -> "$product: $qty pcs" }.joinToString()}")
            println("Weather: ${weather.joinToString()}")
        }
    }

    println("\nTotal execution time: ${totalTime}ms")
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

        try {
            val gson = Gson()
            val listType = object : TypeToken<List<User>>() {}.type
            val users: List<User> = gson.fromJson(json, listType)
            users.map { it.name }
        } catch (e: Exception) {
            println("Error parsing users JSON: ${e.message}")
            emptyList()
        }
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

        try {
            val gson = Gson()
            val salesData = gson.fromJson(json, SalesData::class.java)
            salesData.items.associate { it.product to it.qty }
        } catch (e: Exception) {
            println("Error parsing sales JSON: ${e.message}")
            emptyMap()
        }
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

        try {
            val gson = Gson()
            val listType = object : TypeToken<List<Weather>>() {}.type
            val weatherList: List<Weather> = gson.fromJson(json, listType)
            weatherList.map { "${it.city}: ${it.temp} C" }
        } catch (e: Exception) {
            println("Error parsing weather JSON: ${e.message}")
            emptyList()
        }
    }
}

suspend fun simulateNetworkDelay(timeMs: Long) {
    delay(timeMs)
}

fun simulateRandomFailure(source: String) {
    if (Random.nextInt(1000) < 20) {
        throw RuntimeException("Random failure loading $source")
    }
}