package com.mnvths.schoolclearance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AdminStudentProfile
import com.mnvths.schoolclearance.data.StudentDetailsForEdit
import com.mnvths.schoolclearance.data.StudentListItem
import com.mnvths.schoolclearance.data.UpdateStudentRequest
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.body
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudentManagementViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    // For the main student list screen
    private val _students = MutableStateFlow<List<StudentListItem>>(emptyList())
    val students: StateFlow<List<StudentListItem>> = _students.asStateFlow()

    // For the EditStudentScreen
    private val _studentDetails = MutableStateFlow<StudentDetailsForEdit?>(null)
    val studentDetails: StateFlow<StudentDetailsForEdit?> = _studentDetails.asStateFlow()

    // For the AdminStudentDetailScreen
    private val _adminStudentProfile = MutableStateFlow<AdminStudentProfile?>(null)
    val adminStudentProfile: StateFlow<AdminStudentProfile?> = _adminStudentProfile.asStateFlow()

    init {
        fetchAllStudents()
    }

    fun fetchAllStudents() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/students")
                if(response.status.isSuccess()) {
                    _students.value = response.body()
                } else {
                    error.value = "Failed to load students"
                }
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addStudent(
        studentId: String,
        firstName: String,
        middleName: String?,
        lastName: String,
        password: String,
        sectionId: Int?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val studentData = mapOf(
                    "studentId" to studentId,
                    "firstName" to firstName,
                    "middleName" to middleName,
                    "lastName" to lastName,
                    "password" to password,
                    "sectionId" to sectionId
                )
                val response: HttpResponse = client.post("http://10.0.2.2:3000/students") {
                    contentType(ContentType.Application.Json)
                    setBody(studentData)
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchAllStudents()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    onError(errorMessage.ifBlank { "Failed to add student." })
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun deleteStudent(studentId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.delete("http://10.0.2.2:3000/students/$studentId")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchAllStudents()
                } else {
                    onError(response.bodyAsText())
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun fetchStudentDetailsForEdit(studentId: String) {
        viewModelScope.launch {
            isLoading.value = true
            _studentDetails.value = null
            error.value = null

            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/students/$studentId")
                if (response.status.isSuccess()) {
                    _studentDetails.value = response.body()
                } else {
                    error.value = "Could not load student details. (Error: ${response.status})"
                }
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearStudentDetails() {
        _studentDetails.value = null
    }

    fun fetchAdminStudentProfile(studentId: String) {
        viewModelScope.launch {
            isLoading.value = true
            _adminStudentProfile.value = null
            error.value = null

            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/students/admin-profile/$studentId")
                if (response.status.isSuccess()) {
                    _adminStudentProfile.value = response.body()
                } else {
                    error.value = "Could not load student profile. (Error: ${response.status})"
                }
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearAdminStudentProfile() {
        _adminStudentProfile.value = null
    }

    fun updateStudent(
        originalStudentId: String, updatedDetails: UpdateStudentRequest,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.put("http://10.0.2.2:3000/students/$originalStudentId") {
                    contentType(ContentType.Application.Json)
                    setBody(updatedDetails)
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchAllStudents()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    onError(errorMessage.ifBlank { "Failed to update student." })
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}