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

    // ✅ RENAMED: from _sections and sections to _classSections and classSections to match the UI
    private val _classSections = MutableStateFlow<List<ClassSection>>(emptyList())
    val classSections: StateFlow<List<ClassSection>> = _classSections.asStateFlow()

    private val _gradeLevels = MutableStateFlow<List<String>>(emptyList())
    val gradeLevels: StateFlow<List<String>> = _gradeLevels.asStateFlow()
    private val _studentProfile = MutableStateFlow<AdminStudentProfile?>(null)
    val studentProfile: StateFlow<AdminStudentProfile?> = _studentProfile.asStateFlow()

    // ✅ ADDED: init block to automatically load data for the dropdowns
    init {
        fetchAllStudents()
        fetchClassSections()
        fetchAllGradeLevels()
    }

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

    // ✅ MODIFIED: Added 'sectionId: Int?' to the function signature
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
                // ✅ MODIFIED: Added 'sectionId' to the data being sent to the server
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
            error.value = null

            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/api/student-profile/$studentId")
                if (response.status.isSuccess()) {
                    _studentProfile.value = response.body()
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
    // ✅ RENAMED: from fetchSections to fetchClassSections
    fun fetchClassSections() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // ✅ UPDATED: The response body is now assigned to the renamed state flow
                _classSections.value = client.get("http://10.0.2.2:3000/students/class-sections").body()
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
                    fetchClassSections() // Ensures the sections list is updated after adding
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
                    fetchClassSections() // Ensures the sections list is updated
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
                    fetchClassSections() // Ensures the sections list is updated
                } else {
                    onError(response.bodyAsText().ifBlank { "Failed to delete section." })
                }
            } catch (e: Exception) {
                onError("Network Error: ${e.message}")
            }
        }
    }
}