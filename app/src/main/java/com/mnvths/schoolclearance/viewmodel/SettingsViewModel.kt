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
import timber.log.Timber // --- LOGGING ADDED ---


class SettingsViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // --- LOGGING ADDED ---
        Timber.i("SettingsViewModel initialized.")
        fetchSettings()
    }

    fun fetchSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            // --- LOGGING ADDED ---
            Timber.i("Fetching app settings.")
            try {
                // ✅ UPDATED: This MUST have /api/ because of server.js
                val response: HttpResponse = client.get("/api/settings")
                if (response.status.isSuccess()) {
                    _settings.value = response.body()
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully fetched app settings.")
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error fetching settings.")
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
            // --- LOGGING ADDED ---
            Timber.i("Attempting to update app settings.")
            try {
                // ✅ UPDATED: This MUST have /api/ because of server.js
                val response: HttpResponse = client.put("/api/settings") {
                    contentType(ContentType.Application.Json)
                    setBody(newSettings)
                }
                if (response.status.isSuccess()) {
                    _settings.value = newSettings
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully updated app settings.")
                    onSuccess()
                } else {
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to update settings. Server responded with status: %s", response.status)
                    onError("Failed to update settings.")
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during update settings network call.")
                onError("Network error: ${e.message}")
            }
        }
    }
}