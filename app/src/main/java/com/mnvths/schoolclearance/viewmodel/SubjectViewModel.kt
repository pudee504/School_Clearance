package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AddSubjectRequest
import com.mnvths.schoolclearance.data.Subject
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch

class SubjectViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _subjects = mutableStateOf<List<Subject>>(emptyList())
    val subjects: State<List<Subject>> = _subjects

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        fetchSubjects()
    }

    fun fetchSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ Fetches from the correct '/subjects' endpoint
                val response: List<Subject> = client.get("http://10.0.2.2:3000/subjects").body()
                _subjects.value = response.sortedBy { it.name }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSubject(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = client.post("http://10.0.2.2:3000/subjects") {
                    contentType(ContentType.Application.Json)
                    setBody(AddSubjectRequest(name))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSubjects() // Refresh list
                } else {
                    onError(response.bodyAsText())
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun updateSubject(id: Int, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = client.put("http://10.0.2.2:3000/subjects/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(AddSubjectRequest(name))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSubjects() // Refresh list
                } else {
                    onError(response.bodyAsText())
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun deleteSubject(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = client.delete("http://10.0.2.2:3000/subjects/$id")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSubjects() // Refresh list
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