// file: network/KtorClient.kt

package com.mnvths.schoolclearance.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object KtorClient {
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                // ✅ ADD THIS LINE. This is the fix.
                // It forces all fields from your data class to be sent to the server.
                encodeDefaults = true
            })
        }
    }
}