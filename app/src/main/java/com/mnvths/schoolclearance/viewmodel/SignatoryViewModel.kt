// In SignatoryViewModel.kt
package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AddSignatoryRequest
import com.mnvths.schoolclearance.data.Signatory
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


class SignatoryViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _signatories = mutableStateOf<List<Signatory>>(emptyList())
    val signatories: State<List<Signatory>> = _signatories

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        fetchSignatories()
    }

    fun fetchSignatories() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: List<Signatory> = client.get("http://10.0.2.2:3000/signatories").body()
                // ✅ Sort the list alphabetically by name before updating the state
                _signatories.value = response.sortedBy { it.signatoryName }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSignatory(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = client.post("http://10.0.2.2:3000/signatories") {
                    contentType(ContentType.Application.Json)
                    setBody(AddSignatoryRequest(name))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSignatories() // Refresh list
                } else {
                    onError(response.bodyAsText())
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun updateSignatory(id: Int, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = client.put("http://10.0.2.2:3000/signatories/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(AddSignatoryRequest(name))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSignatories() // Refresh list
                } else {
                    onError(response.bodyAsText())
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun deleteSignatory(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = client.delete("http://10.0.2.2:3000/signatories/$id")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSignatories() // Refresh list
                } else {
                    val errorBody = response.body<Map<String, String>>()
                    onError(errorBody["error"] ?: "Failed to delete.")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}