package com.mnvths.schoolclearance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AppSettings
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar


class SettingsViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchSettings()
    }

    fun fetchSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // ✅ UPDATED: This MUST have /api/ because of server.js
                val response: HttpResponse = client.get("/api/settings")
                if (response.status.isSuccess()) {
                    _settings.value = response.body()
                }
            } catch (e: Exception) {
                println("Error fetching settings: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSettings(
        newSettings: AppSettings,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED: This MUST have /api/ because of server.js
                val response: HttpResponse = client.put("/api/settings") {
                    contentType(ContentType.Application.Json)
                    setBody(newSettings)
                }
                if (response.status.isSuccess()) {
                    _settings.value = newSettings
                    onSuccess()
                } else {
                    onError("Failed to update settings.")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}