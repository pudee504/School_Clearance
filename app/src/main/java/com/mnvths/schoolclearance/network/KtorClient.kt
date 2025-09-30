// file: network/KtorClient.kt
package com.mnvths.schoolclearance.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object KtorClient {
    val httpClient = HttpClient(CIO) {
        // This plugin handles JSON conversion automatically.
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }

        // ✅ ADDED: This block sets the base URL for every request.
        // It reads the dynamic URL from your ApiConfig object.
        defaultRequest {
            url(ApiConfig.BASE_URL)
        }
    }
}