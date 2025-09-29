package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.StudentClearanceStatus
import com.mnvths.schoolclearance.data.UpdateClearanceStatusRequest
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// Assumed API Response Wrapper
@Serializable
data class SignatoryClearanceResponse(
    val students: List<StudentClearanceStatus>,
    val schoolYear: String,
    val term: String,
    val requirementId: Int
)

class ClearanceViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _students = mutableStateOf<List<StudentClearanceStatus>>(emptyList())
    val students: State<List<StudentClearanceStatus>> = _students

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    // Store context fetched with the student list
    private var _schoolYear: String? = null
    private var _term: String? = null
    private var _requirementId: Int? = null

    fun fetchStudentClearanceStatus(sectionId: Int, subjectId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // This API endpoint is now assumed to return the response wrapper
                val response: SignatoryClearanceResponse = client.get("http://10.0.2.2:3000/clearance/section/$sectionId/subject/$subjectId").body()

                // Store the context
                _students.value = response.students
                _schoolYear = response.schoolYear
                _term = response.term
                _requirementId = response.requirementId

            } catch (e: Exception) {
                _error.value = "Error fetching students: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ MODIFIED: Updated to use the new robust clearance update logic.
    fun updateStudentClearance(
        userId: Int,
        isCleared: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // Check if we have the necessary context
            if (_schoolYear == null || _term == null || _requirementId == null) {
                onError("Clearance context not loaded. Please refresh.")
                return@launch
            }

            try {
                // Use the new, consistent request model and endpoint
                val requestBody = UpdateClearanceStatusRequest(
                    userId = userId,
                    requirementId = _requirementId!!,
                    schoolYear = _schoolYear!!,
                    term = _term!!,
                    isCleared = isCleared
                )
                val response: HttpResponse = client.put("http://10.0.2.2:3000/students/clearance/status") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                if (response.status.isSuccess()) {
                    onSuccess()
                    // Locally update the student's status for immediate UI feedback
                    _students.value = _students.value.map {
                        if (it.userId == userId) it.copy(isCleared = isCleared) else it
                    }
                } else {
                    onError("Failed to update status: ${response.bodyAsText()}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    // ✅ MODIFIED: This function now uses the corrected update logic.
    fun clearAllNotClearedStudents(sectionId: Int, subjectId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val notClearedStudents = _students.value.filter { !it.isCleared }
            if (notClearedStudents.isEmpty()) {
                onSuccess()
                return@launch
            }

            if (_schoolYear == null || _term == null || _requirementId == null) {
                onError("Clearance context not loaded. Please refresh.")
                return@launch
            }

            var hasErrorOccurred = false
            // This is a simplified batch update. A dedicated backend endpoint would be more efficient.
            for (student in notClearedStudents) {
                try {
                    val requestBody = UpdateClearanceStatusRequest(
                        userId = student.userId,
                        requirementId = _requirementId!!,
                        schoolYear = _schoolYear!!,
                        term = _term!!,
                        isCleared = true
                    )
                    val response: HttpResponse = client.put("http://10.0.2.2:3000/students/clearance/status") {
                        contentType(ContentType.Application.Json)
                        setBody(requestBody)
                    }
                    if (!response.status.isSuccess()) {
                        hasErrorOccurred = true
                        break
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
            // Always refresh the list from the server to ensure consistency.
            fetchStudentClearanceStatus(sectionId, subjectId)
        }
    }
}