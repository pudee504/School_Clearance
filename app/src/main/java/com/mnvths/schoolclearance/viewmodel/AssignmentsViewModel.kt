package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AssignClassesRequest
import com.mnvths.schoolclearance.data.AssignedSubject
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.Subject
import com.mnvths.schoolclearance.data.SubjectGroup
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch

class AssignmentViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _subjects = mutableStateOf<List<Subject>>(emptyList())
    val subjects: State<List<Subject>> = _subjects

    private val _assignedSubjects = mutableStateOf<List<AssignedSubject>>(emptyList())
    val assignedSubjects: State<List<AssignedSubject>> = _assignedSubjects

    private val _sections = mutableStateOf<List<ClassSection>>(emptyList())
    val sections: State<List<ClassSection>> = _sections

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    fun fetchSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ UPDATED: Endpoint to fetch subjects
                val response: HttpResponse = client.get("/subjects")
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

    fun fetchAssignedSubjectsForSignatory(signatoryId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // ✅ UPDATED to match your existing server route
                val response: HttpResponse = client.get("/assignments/faculty-signatory/$signatoryId")
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

    fun fetchAllClassSections() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.get("/students/class-sections")
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

    fun assignSubjectToSignatory(
        signatoryId: Int,
        subjectId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED: Endpoint for assignments. You will need to create this on your backend.
                val response: HttpResponse = client.post("/assignments/assign-subject") {
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

    fun fetchAssignedSections(
        signatoryId: Int,
        subjectId: Int,
        onResult: (List<ClassSection>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED: Endpoint for assignments. You will need to create this on your backend.
                val response: HttpResponse =
                    client.get("/assignments/sections/$signatoryId/$subjectId")
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
                val response: HttpResponse = client.post("/assignments/assign-classes") {
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

    // ✅ THIS IS THE NEW FUNCTION THAT FIXES THE 'Unresolved reference' ERROR
    fun assignMultipleSubjectsToSignatory(
        signatoryId: Int,
        subjects: List<Subject>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            var allSuccessful = true
            for (subject in subjects) {
                try {
                    // ✅ ADD the "/assignments" prefix here
                    val response: HttpResponse = client.post("/assignments/faculty-signatory") {
                        contentType(ContentType.Application.Json)
                        setBody(mapOf("signatoryId" to signatoryId, "subjectId" to subject.id))
                    }
                    if (!response.status.isSuccess()) {
                        allSuccessful = false
                        onError("Error assigning ${subject.name}.")
                        break // Stop on the first error
                    }
                } catch (e: Exception) {
                    allSuccessful = false
                    onError("Network error while assigning ${subject.name}.")
                    break // Stop on the first error
                }
            }

            if (allSuccessful) {
                onSuccess()
                fetchAssignedSubjectsForSignatory(signatoryId)
            }
        }
    }

}