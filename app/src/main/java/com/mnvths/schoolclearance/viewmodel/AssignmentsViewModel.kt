package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AssignClassesRequest
import com.mnvths.schoolclearance.data.AssignedSubject
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.Subject
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch

class AssignmentViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    // ✅ RENAMED: from signatories to subjects
    private val _subjects = mutableStateOf<List<Subject>>(emptyList())
    val subjects: State<List<Subject>> = _subjects

    // ✅ RENAMED: from assignedSignatories to assignedSubjects
    private val _assignedSubjects = mutableStateOf<List<AssignedSubject>>(emptyList())
    val assignedSubjects: State<List<AssignedSubject>> = _assignedSubjects

    private val _sections = mutableStateOf<List<ClassSection>>(emptyList())
    val sections: State<List<ClassSection>> = _sections

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    // ✅ RENAMED: Fetches all available Subjects
    fun fetchSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ UPDATED: Endpoint to fetch subjects
                val response: HttpResponse = client.get("http://10.0.2.2:3000/subjects")
                if (response.status.isSuccess()) {
                    _subjects.value = response.body()
                } else {
                    _error.value = "Failed to load subjects."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ RENAMED: Fetches subjects assigned to a specific signatory
    fun fetchAssignedSubjectsForSignatory(signatoryId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // ✅ UPDATED: Endpoint for assignments. You will need to create this on your backend.
                val response: HttpResponse = client.get("http://10.0.2.2:3000/assignments/signatory-subjects/$signatoryId")
                if (response.status.isSuccess()) {
                    _assignedSubjects.value = response.body()
                } else {
                    _error.value = "Failed to load assigned subjects for this signatory."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // This function is unchanged
    fun fetchAllClassSections() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/students/class-sections") // Assuming this is the correct full path
                if (response.status.isSuccess()) {
                    val classSections: List<ClassSection> = response.body()
                    _sections.value = classSections
                } else {
                    _error.value = "Failed to load class sections."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ RENAMED: Assigns a subject to a signatory
    fun assignSubjectToSignatory(
        signatoryId: Int,
        subjectId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED: Endpoint for assignments. You will need to create this on your backend.
                val response: HttpResponse = client.post("http://10.0.2.2:3000/assignments/assign-subject") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "signatoryId" to signatoryId,
                            "subjectId" to subjectId
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    onError("Failed to assign subject: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    // ✅ UPDATED: Fetches sections assigned to a specific subject for a specific signatory
    fun fetchAssignedSections(
        signatoryId: Int,
        subjectId: Int,
        onResult: (List<ClassSection>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED: Endpoint for assignments. You will need to create this on your backend.
                val response: HttpResponse =
                    client.get("http://10.0.2.2:3000/assignments/sections/$signatoryId/$subjectId")
                if (response.status.isSuccess()) {
                    val assigned: List<ClassSection> = response.body()
                    onResult(assigned)
                } else {
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }

    // ✅ RENAMED: Assigns multiple classes to a subject/signatory pair
    fun assignClassesToSubject(
        signatoryId: Int,
        subjectId: Int,
        sectionIds: List<Int>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED: Endpoint for assignments. You will need to create this on your backend.
                val response: HttpResponse = client.post("http://10.0.2.2:3000/assignments/assign-classes") {
                    contentType(ContentType.Application.Json)
                    setBody(AssignClassesRequest(signatoryId, subjectId, sectionIds))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    onError("Failed to assign classes: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}