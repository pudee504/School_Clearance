package com.mnvths.schoolclearance.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AssignedSubject
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.Signatory
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch

class SignatoryViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _signatoryList = mutableStateOf<List<Signatory>>(emptyList())
    val signatoryList: State<List<Signatory>> = _signatoryList

    private val _assignedSubjects = mutableStateOf<List<AssignedSubject>>(emptyList())
    val assignedSubjects: State<List<AssignedSubject>> = _assignedSubjects

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _assignedSections = mutableStateOf<Map<Int, List<ClassSection>>>(emptyMap())
    val assignedSections: State<Map<Int, List<ClassSection>>> = _assignedSections

    fun fetchSignatoryList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.get("/signatories")
                if (response.status.isSuccess()) {
                    val signatoryData: List<Signatory> = response.body()
                    _signatoryList.value = signatoryData
                } else {
                    val errorBody = response.bodyAsText()
                    _error.value = "Server error: ${response.status.description}. Details: $errorBody"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAssignedSection(
        signatoryId: Int,
        subjectId: Int,
        sectionId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.delete("/faculty/$signatoryId/signatory/$subjectId/section/$sectionId")

                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchAssignedSections(signatoryId, subjectId)
                } else {
                    onError("Failed to delete assignment: ${response.bodyAsText()}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun fetchAssignedSubjects(signatoryId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.get("/faculty-signatory/$signatoryId")
                if (response.status.isSuccess()) {
                    val subjects: List<AssignedSubject> = response.body()
                    _assignedSubjects.value = subjects
                    _assignedSections.value = emptyMap()
                } else {
                    _error.value = "Failed to load assigned subjects."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAssignedSections(signatoryId: Int, subjectId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // ✅ UPDATED
                val response: HttpResponse = client.get("/faculty-signatory-sections/$signatoryId/$subjectId")
                if (response.status.isSuccess()) {
                    val sections: List<ClassSection> = response.body()
                    _assignedSections.value = _assignedSections.value + (subjectId to sections)
                } else {
                    // Handle error case
                }
            } catch (e: Exception) {
                // Handle network error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAssignedSubject(signatoryId: Int, subjectId: Int) {
        viewModelScope.launch {
            try {
                Log.d("SignatoryViewModel", "Attempting to delete subject $subjectId for signatory $signatoryId")
                // ✅ UPDATED
                val response: HttpResponse = client.delete("/faculty-signatory/$signatoryId/$subjectId")
                if (response.status.isSuccess()) {
                    fetchAssignedSubjects(signatoryId)
                } else {
                    _error.value = "Failed to delete subject assignment."
                    Log.e("SignatoryViewModel", "Deletion failed: ${response.status.description}")
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                Log.e("SignatoryViewModel", "Deletion network error: ${e.stackTraceToString()}")
            }
        }
    }

    fun addSignatory(
        username: String,
        password: String,
        firstName: String,
        middleName: String?,
        lastName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.post("/signatories") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "username" to username,
                            "password" to password,
                            "firstName" to firstName,
                            "middleName" to middleName,
                            "lastName" to lastName
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSignatoryList()
                } else {
                    val errorBody = response.bodyAsText()
                    onError("Failed to add signatory: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun deleteSignatory(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.delete("/signatories/$id")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSignatoryList()
                } else {
                    onError("Failed to delete signatory: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
    fun editSignatory(
        id: Int,
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        middleName: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val bodyMap = mutableMapOf<String, String?>(
                    "username" to username,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "middleName" to middleName
                )

                if (password.isNotBlank()) {
                    bodyMap["password"] = password
                }

                // ✅ UPDATED
                val response: HttpResponse = client.put("/signatories/${id}") {
                    contentType(ContentType.Application.Json)
                    setBody(bodyMap)
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSignatoryList()
                } else {
                    onError("Failed to update signatory: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}