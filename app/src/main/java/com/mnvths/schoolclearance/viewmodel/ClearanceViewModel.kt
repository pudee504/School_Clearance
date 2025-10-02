package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.ClearMultipleRequest
import com.mnvths.schoolclearance.data.StudentClearanceStatus
import com.mnvths.schoolclearance.data.UpdateClearanceStatusRequest
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

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

    private var _schoolYear: String? = null
    private var _term: String? = null
    private var _requirementId: Int? = null

    fun fetchStudentClearanceStatus(sectionId: Int, subjectId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ UPDATED: The endpoint URL now matches our new backend route
                val response: SignatoryClearanceResponse = client.get("/clearance/students/$sectionId/$subjectId").body()

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

    fun updateStudentClearance(
        userId: Int,
        isCleared: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (_schoolYear == null || _term == null || _requirementId == null) {
                onError("Clearance context not loaded. Please refresh.")
                return@launch
            }

            // Optimistic UI update for a snappy feel
            val originalList = students.value
            _students.value = students.value.map {
                if (it.userId == userId) it.copy(isCleared = isCleared) else it
            }

            try {
                val requestBody = UpdateClearanceStatusRequest(
                    userId = userId,
                    requirementId = _requirementId!!,
                    schoolYear = _schoolYear!!,
                    term = _term!!,
                    isCleared = isCleared
                )
                // ✅ UPDATED: Use the new POST route and correct HTTP method
                val response: HttpResponse = client.post("/clearance/update") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                if (response.status.isSuccess()) {
                    onSuccess()
                } else {
                    _students.value = originalList // Revert UI change on failure
                    onError("Failed to update status: ${response.bodyAsText()}")
                }
            } catch (e: Exception) {
                _students.value = originalList // Revert UI change on failure
                onError("Network error: ${e.message}")
            }
        }
    }

    // ✅ NEW: Function to fetch students for an ACCOUNT clearance
    fun fetchStudentClearanceStatusForAccount(sectionId: Int, accountId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // This endpoint URL matches our new backend route
                val response: SignatoryClearanceResponse = client.get("/clearance/students-account/$sectionId/$accountId").body()

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

    fun clearAllNotClearedStudents(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val notClearedStudentIds = _students.value
                .filter { !it.isCleared }
                .map { it.userId }

            if (notClearedStudentIds.isEmpty()) {
                onSuccess()
                return@launch
            }

            if (_schoolYear == null || _term == null || _requirementId == null) {
                onError("Clearance context not loaded. Please refresh.")
                return@launch
            }

            try {
                val requestBody = ClearMultipleRequest(
                    requirementId = _requirementId!!,
                    schoolYear = _schoolYear!!,
                    term = _term!!,
                    studentUserIds = notClearedStudentIds
                )

                val response: HttpResponse = client.post("/clearance/clear-multiple") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                if (response.status.isSuccess()) {
                    onSuccess()
                    // Manually update the local state for an instant UI refresh
                    _students.value = _students.value.map { it.copy(isCleared = true) }
                } else {
                    onError("Failed to clear all students: ${response.bodyAsText()}")
                }
            } catch (e: Exception) {
                onError("Network error during bulk clear: ${e.message}")
            }
        }
    }
}