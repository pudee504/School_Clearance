package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AddSubjectWithGradeRequest
import com.mnvths.schoolclearance.data.CurriculumManagementSubject
import com.mnvths.schoolclearance.data.GradeLevelItem
import com.mnvths.schoolclearance.data.UpdateRequirementStatusRequest
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber // --- LOGGING ADDED ---

class SubjectViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>((null))
    val error: State<String?> = _error

    private val _gradeLevels = MutableStateFlow<List<GradeLevelItem>>(emptyList())
    val gradeLevels = _gradeLevels.asStateFlow()

    private val _managementSubjects = MutableStateFlow<List<CurriculumManagementSubject>>(emptyList())
    val managementSubjects = _managementSubjects.asStateFlow()

    fun fetchGradeLevels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching grade levels.")
            try {
                _gradeLevels.value = client.get("/curriculum/grade-levels").body()
                // --- LOGGING ADDED ---
                Timber.i("Successfully fetched %d grade levels.", _gradeLevels.value.size)
            } catch (e: Exception) {
                _error.value = "Failed to load grade levels: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error fetching grade levels.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ MODIFIED: This function now accepts a semester.
    fun fetchSubjectsForGradeLevel(gradeLevelId: Int, semester: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching subjects for grade level ID: %d, semester: %d", gradeLevelId, semester)
            try {
                // The semester is now passed as a query parameter to the API call.
                val rawSubjects: List<CurriculumManagementSubject> = client.get("/curriculum/$gradeLevelId/subjects?semester=$semester").body()
                // --- LOGGING ADDED ---
                Timber.d("Fetched %d raw subjects from API.", rawSubjects.size)

                // De-duplication logic for SHS is still necessary and works correctly here.
                if (gradeLevelId > 4) { // Apply only for SHS
                    val subjectsGroupedById = rawSubjects.groupBy { it.subjectId }
                    val uniqueSubjects = subjectsGroupedById.map { (_, subjectList) ->
                        subjectList.first()
                    }.sortedBy { it.subjectName }
                    _managementSubjects.value = uniqueSubjects
                } else {
                    _managementSubjects.value = rawSubjects
                }
                // --- LOGGING ADDED ---
                Timber.i("Processed and loaded %d subjects into state.", _managementSubjects.value.size)
            } catch (e: Exception) {
                _error.value = "Failed to load subjects: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error fetching subjects for grade level.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ CHANGED: Function signature now accepts an optional subjectCode
    // ✅ CHANGED: The function signature now requires a subjectCode
    fun addSubjectToCurriculum(
        name: String,
        gradeLevelId: Int,
        semester: Int,
        subjectCode: String, // ✅ CHANGED: No longer nullable (String? -> String)
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to add subject '%s' to curriculum.", name)
            try {
                val requestBody = AddSubjectWithGradeRequest(
                    subjectName = name,
                    gradeLevelId = gradeLevelId,
                    semester = semester,
                    subjectCode = subjectCode
                )
                // ... (rest of the function is the same)
                val response = client.post("/subjects") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully added subject '%s' to curriculum.", name)
                    onSuccess()
                    fetchSubjectsForGradeLevel(gradeLevelId, semester)
                } else {
                    val errorBody = response.body<Map<String, String>>()
                    val errorMessage = errorBody["error"] ?: "Failed to add subject."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to add subject. Server error: %s", errorMessage)
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during add subject to curriculum network call.")
                onError("Network error: ${e.message}")
            }
        }
    }

    fun setRequirementStatus(requirementId: Int, status: String, gradeLevelId: Int, semester: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Setting requirement ID %d to status '%s'", requirementId, status)
            try {
                val response = client.patch("/curriculum/requirements/$requirementId/status") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateRequirementStatusRequest(status))
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully set requirement status.")
                    onSuccess()
                    fetchSubjectsForGradeLevel(gradeLevelId, semester) // Refresh with the current semester
                } else {
                    val errorBody = response.body<Map<String, String>>()
                    val errorMessage = errorBody["error"] ?: "Failed to update status."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to set requirement status. Server error: %s", errorMessage)
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error setting requirement status.")
                onError("Network error: ${e.message}")
            }
        }
    }
}