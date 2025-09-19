package com.mnvths.schoolclearance.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AssignedSignatory
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.FacultyMember
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.collections.plus
import com.mnvths.schoolclearance.network.KtorClient

class FacultyViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _facultyList = mutableStateOf<List<FacultyMember>>(emptyList())
    val facultyList: State<List<FacultyMember>> = _facultyList

    private val _assignedSignatories = mutableStateOf<List<AssignedSignatory>>(emptyList())
    val assignedSignatories: State<List<AssignedSignatory>> = _assignedSignatories

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _assignedSections = mutableStateOf<Map<Int, List<ClassSection>>>(emptyMap())
    val assignedSections: State<Map<Int, List<ClassSection>>> = _assignedSections

    fun fetchFacultyList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/faculty")
                if (response.status.isSuccess()) {
                    val facultyData: List<FacultyMember> = response.body()
                    _facultyList.value = facultyData
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
        facultyId: Int,
        signatoryId: Int,
        sectionId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.delete("http://10.0.2.2:3000/faculty/$facultyId/signatory/$signatoryId/section/$sectionId")

                if (response.status.isSuccess()) {
                    onSuccess()
                    // After deleting, refresh the list of sections for this signatory
                    fetchAssignedSections(facultyId, signatoryId)
                } else {
                    onError("Failed to delete assignment: ${response.bodyAsText()}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun fetchAssignedSignatories(facultyId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/faculty-signatory/$facultyId")
                if (response.status.isSuccess()) {
                    val signatories: List<AssignedSignatory> = response.body()
                    _assignedSignatories.value = signatories
                    _assignedSections.value = emptyMap()
                } else {
                    _error.value = "Failed to load assigned signatories."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAssignedSections(facultyId: Int, signatoryId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response: HttpResponse = client.get("http://10.0.2.2:3000/faculty-signatory-sections/$facultyId/$signatoryId")
                if (response.status.isSuccess()) {
                    val sections: List<ClassSection> = response.body()
                    _assignedSections.value = _assignedSections.value + (signatoryId to sections)
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

    // New function to handle the deletion of a signatory assignment
    fun deleteAssignedSignatory(facultyId: Int, signatoryId: Int) {
        viewModelScope.launch {
            try {
                Log.d("FacultyViewModel", "Attempting to delete signatory $signatoryId for faculty $facultyId")
                val response: HttpResponse = client.delete("http://10.0.2.2:3000/faculty-signatory/$facultyId/$signatoryId")
                if (response.status.isSuccess()) {
                    // Refresh the list of assigned signatories after a successful deletion
                    fetchAssignedSignatories(facultyId)
                } else {
                    _error.value = "Failed to delete signatory assignment."
                    Log.e("FacultyViewModel", "Deletion failed: ${response.status.description}")
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                Log.e("FacultyViewModel", "Deletion network error: ${e.stackTraceToString()}")
            }
        }
    }

    // New function to add a faculty member
    fun addFaculty(
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
                val response: HttpResponse = client.post("http://10.0.2.2:3000/faculty") {
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
                    fetchFacultyList() // Refresh the list after adding a new user
                } else {
                    val errorBody = response.bodyAsText()
                    onError("Failed to add faculty: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun deleteFaculty(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.delete("http://10.0.2.2:3000/faculty/$id")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchFacultyList() // Refresh the list
                } else {
                    onError("Failed to delete faculty: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
    fun editFaculty(
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

                val response: HttpResponse = client.put("http://10.0.2.2:3000/faculty/${id}") {
                    contentType(ContentType.Application.Json)
                    setBody(bodyMap)
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchFacultyList() // Refresh the list
                } else {
                    onError("Failed to update faculty: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}