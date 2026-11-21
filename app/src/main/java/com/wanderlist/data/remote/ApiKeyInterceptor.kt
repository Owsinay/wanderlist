package com.wanderlist.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/** Dodaje klucz ORS do każdego zapytania (nagłówek Authorization). */
class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Authorization", apiKey)
            .build()
        return chain.proceed(request)
    }
}
