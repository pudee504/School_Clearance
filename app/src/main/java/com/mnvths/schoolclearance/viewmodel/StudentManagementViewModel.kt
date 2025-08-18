package com.mnvths.schoolclearance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.AddSectionRequest
import com.mnvths.schoolclearance.AddStudentRequest
import com.mnvths.schoolclearance.ClassSection
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class AddSectionRequest(val gradeLevel: String, val sectionName: String)

// Add this data class for updating a section
@Serializable
data class UpdateSectionRequest(val gradeLevel: String, val sectionName: String)


@Serializable
data class StudentListItem(
    val id: String,
    val firstName: String,
    // Middle name can be null
    val middleName: String?,
    val lastName: String
)

@Serializable
data class AddStudentRequest(
    val studentId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val sectionId: Int,
    val password: String
)
class StudentManagementViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    private val _sections = MutableStateFlow<List<ClassSection>>(emptyList())
    val sections: StateFlow<List<ClassSection>> = _sections

    private val _students = MutableStateFlow<List<StudentListItem>>(emptyList())
    val students: StateFlow<List<StudentListItem>> = _students

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        fetchSections()
    }

    fun fetchSections() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/class-sections")
                if (response.status.isSuccess()) {
                    _sections.value = response.body()
                } else {
                    error.value = "Failed to load sections: ${response.status.description}"
                }
            } catch (e: Exception) {
                error.value = "Network error: ${e.message}"
            } finally {
                isLoading.value = false
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
                val response: HttpResponse = client.post("http://10.0.2.2:3000/sections") {
                    contentType(ContentType.Application.Json)
                    setBody(AddSectionRequest(gradeLevel, sectionName))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSections()
                } else {
                    onError("Failed to add section: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun fetchStudentsForSection(sectionId: Int) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val response: HttpResponse =
                    client.get("http://10.0.2.2:3000/students/section/$sectionId")

                if (response.status.isSuccess()) {
                    // 2. Explicitly tell Ktor to parse the response into a List of the new class
                    _students.value = response.body<List<StudentListItem>>()
                } else {
                    error.value = "Failed to load students: ${response.status.description}"
                }
            } catch (e: Exception) {
                error.value = "Error parsing student list: ${e.message}"
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
        sectionId: Int,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:3000/students") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        AddStudentRequest(
                            studentId,
                            firstName,
                            middleName,
                            lastName,
                            sectionId,
                            password
                        )
                    )
                }

                // This new logic first checks the status, then handles the response text.
                // This prevents Ktor from trying to deserialize the response into the wrong object.
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchStudentsForSection(sectionId)
                } else {
                    // Read the raw error message from the server as plain text
                    val errorMessage = response.bodyAsText()
                    if (errorMessage.contains("Student ID already exists")) {
                        onError("Error: Student ID already exists.")
                    } else {
                        onError("Failed to add student: ${response.status.description}")
                    }
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun deleteStudent(
        studentId: String,
        sectionId: Int, // To know which list to refresh
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.delete("http://10.0.2.2:3000/students/$studentId")
                if (response.status.isSuccess()) {
                    onSuccess()
                    // Refresh the student list for the current section
                    fetchStudentsForSection(sectionId)
                } else {
                    val errorBody = response.bodyAsText()
                    onError(errorBody)
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    // --- NEW FUNCTION FOR DELETING ---
    fun deleteSection(
        sectionId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.delete("http://10.0.2.2:3000/sections/$sectionId")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSections() // Refresh the list after deleting
                } else {
                    onError("Failed to delete section: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    // --- NEW FUNCTION FOR UPDATING ---
    fun updateSection(
        sectionId: Int,
        gradeLevel: String,
        sectionName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.put("http://10.0.2.2:3000/sections/$sectionId") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateSectionRequest(gradeLevel, sectionName))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSections() // Refresh list on success
                } else {
                    onError("Failed to update section: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}