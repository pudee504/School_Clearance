// In a new file: ClearanceViewModel.kt
package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

// Data model for the list items
@Serializable
data class StudentClearanceStatus(
    val userId: Int,
    val studentId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val isCleared: Boolean
)

// Data model for the update request
@Serializable
data class UpdateClearanceRequest(
    val userId: Int,
    val subjectId: Int,
    val sectionId: Int,
    val isCleared: Boolean
)

class ClearanceViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    private val _students = mutableStateOf<List<StudentClearanceStatus>>(emptyList())
    val students: State<List<StudentClearanceStatus>> = _students

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    fun fetchStudentClearanceStatus(sectionId: Int, subjectId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: List<StudentClearanceStatus> = client.get("http://10.0.2.2:3000/clearance/section/$sectionId/subject/$subjectId").body()
                _students.value = response
            } catch (e: Exception) {
                _error.value = "Error fetching students: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ In ClearanceViewModel.kt

// ... inside the ClearanceViewModel class

    fun clearAllNotClearedStudents(subjectId: Int, sectionId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // Find all students who are not yet cleared
            val notClearedStudents = _students.value.filter { !it.isCleared }
            if (notClearedStudents.isEmpty()) {
                onSuccess() // Nothing to do
                return@launch
            }

            // Use a loop to call the update function for each student
            // In a real-world app, you might create a dedicated "batch update" API endpoint
            var hasErrorOccurred = false
            for (student in notClearedStudents) {
                try {
                    val response: HttpResponse = client.post("http://10.0.2.2:3000/clearance/update") {
                        contentType(ContentType.Application.Json)
                        setBody(UpdateClearanceRequest(student.userId, subjectId, sectionId, true))
                    }
                    if (!response.status.isSuccess()) {
                        hasErrorOccurred = true
                        break // Stop if one of the updates fails
                    }
                } catch (e: Exception) {
                    hasErrorOccurred = true
                    break
                }
            }

            if (hasErrorOccurred) {
                onError("An error occurred while clearing all students.")
            } else {
                onSuccess()
            }
            // Always refresh the list at the end
            fetchStudentClearanceStatus(sectionId, subjectId)
        }
    }

    fun updateStudentClearance(
        userId: Int,
        subjectId: Int,
        sectionId: Int,
        isCleared: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:3000/clearance/update") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateClearanceRequest(userId, subjectId, sectionId, isCleared))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    // Refresh the list with the updated status
                    fetchStudentClearanceStatus(sectionId, subjectId)
                } else {
                    onError("Failed to update status: ${response.bodyAsText()}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}

