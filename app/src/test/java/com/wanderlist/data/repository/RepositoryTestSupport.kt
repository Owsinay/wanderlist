package com.wanderlist.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wanderlist.data.local.cache.ApiCacheDatabase
import com.wanderlist.data.remote.NetworkFactory
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Zegar, którym test steruje ręcznie. */
class TestClock(var nowMillis: Long = 1_000_000L) : Clock() {
    override fun getZone() = ZoneOffset.UTC
    override fun withZone(zone: java.time.ZoneId?): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(nowMillis)
}

fun inMemoryCache(): ApiCacheDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ApiCacheDatabase::class.java)
        .allowMainThreadQueries()
        .build()

fun sample(name: String): String =
    checkNotNull(object {}.javaClass.getResource("/api-samples/$name")) { "brak $name" }.readText()

fun MockWebServer.enqueueJson(body: String, code: Int = 200) {
    enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
}

inline fun <reified T> MockWebServer.api(client: okhttp3.OkHttpClient = NetworkFactory.clientBuilder().retryOnConnectionFailure(false).build()): T =
    NetworkFactory.retrofit(url("/").toString(), client).create(T::class.java)

/** Symuluje brak internetu: gdy [offline] = true, każde zapytanie kończy się IOException. */
class OfflineSwitch : okhttp3.Interceptor {
    @Volatile var offline = false
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        if (offline) throw java.io.IOException("offline")
        return chain.proceed(chain.request())
    }
}
