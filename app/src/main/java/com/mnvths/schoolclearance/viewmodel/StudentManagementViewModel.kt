package com.mnvths.schoolclearance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AddSectionRequest
import com.mnvths.schoolclearance.data.AdminStudentProfile
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.StudentListItem
import com.mnvths.schoolclearance.data.UpdateSectionRequest
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
    private val _students = MutableStateFlow<List<StudentListItem>>(emptyList())
    val students: StateFlow<List<StudentListItem>> = _students.asStateFlow()
    private val _sections = MutableStateFlow<List<ClassSection>>(emptyList())
    val sections: StateFlow<List<ClassSection>> = _sections.asStateFlow()
    private val _gradeLevels = MutableStateFlow<List<String>>(emptyList())
    val gradeLevels: StateFlow<List<String>> = _gradeLevels.asStateFlow()
    private val _studentProfile = MutableStateFlow<AdminStudentProfile?>(null)
    val studentProfile: StateFlow<AdminStudentProfile?> = _studentProfile.asStateFlow()

    // --- Student Management ---
    fun fetchAllStudents() {
        viewModelScope.launch {
            isLoading.value = true
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
        studentId: String, firstName: String, middleName: String?, lastName: String, password: String,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val studentData = mapOf(
                    "studentId" to studentId,
                    "firstName" to firstName,
                    "middleName" to middleName,
                    "lastName" to lastName,
                    "password" to password
                )
                val response: HttpResponse = client.post("http://10.0.2.2:3000/students") {
                    contentType(ContentType.Application.Json)
                    setBody(studentData)
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchAllStudents()
                } else {
                    onError(response.bodyAsText().ifBlank { "Failed to add student." })
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

    fun fetchStudentProfile(studentId: String) {
        viewModelScope.launch {
            isLoading.value = true
            _studentProfile.value = null
            error.value = null // Good practice to clear old errors first

            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/api/student-profile/$studentId")

                if (response.status.isSuccess()) {
                    _studentProfile.value = response.body()
                } else {
                    // ✅ THIS IS THE MISSING PART
                    // This block now handles errors like "404 Not Found"
                    error.value = "Could not load student profile. (Error: ${response.status})"
                }

            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearStudentProfile() {
        _studentProfile.value = null
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
                    onError(response.bodyAsText().ifBlank { "Failed to update student." })
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    // --- Section & Grade Level Management ---
    fun fetchSections() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                _sections.value = client.get("http://10.0.2.2:3000/students/class-sections").body()
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun fetchAllGradeLevels() {
        viewModelScope.launch {
            try {
                _gradeLevels.value = client.get("http://10.0.2.2:3000/students/grade-levels").body()
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
            }
        }
    }

    fun addSection(
        gradeLevel: String,
        sectionName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:3000/students/sections") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        AddSectionRequest(
                            gradeLevel = "Grade $gradeLevel",
                            sectionName = sectionName
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSections()
                } else {
                    onError(response.bodyAsText().ifBlank { "Failed to add section." })
                }
            } catch (e: Exception) {
                onError("Network Error: ${e.message}")
            }
        }
    }

    fun updateSection(
        sectionId: Int,
        gradeLevel: String,
        sectionName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.put("http://10.0.2.2:3000/students/sections/$sectionId") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpdateSectionRequest(
                            gradeLevel = gradeLevel,
                            sectionName = sectionName
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSections()
                } else {
                    onError(response.bodyAsText().ifBlank { "Failed to update section." })
                }
            } catch (e: Exception) {
                onError("Network Error: ${e.message}")
            }
        }
    }

    fun deleteSection(
        sectionId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.delete("http://10.0.2.2:3000/students/sections/$sectionId")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSections()
                } else {
                    onError(response.bodyAsText().ifBlank { "Failed to delete section." })
                }
            } catch (e: Exception) {
                onError("Network Error: ${e.message}")
            }
        }
    }
}