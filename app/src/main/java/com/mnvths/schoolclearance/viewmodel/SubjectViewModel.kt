package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AddSubjectRequest
import com.mnvths.schoolclearance.data.AddSubjectWithGradeRequest
import com.mnvths.schoolclearance.data.CurriculumResponse
import com.mnvths.schoolclearance.data.CurriculumSubject
import com.mnvths.schoolclearance.data.GradeLevelItem
import com.mnvths.schoolclearance.data.Subject
import com.mnvths.schoolclearance.data.SubjectGroup
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubjectViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _subjects = mutableStateOf<List<Subject>>(emptyList())
    val subjects: State<List<Subject>> = _subjects

    private val _groupedSubjects = mutableStateOf<List<SubjectGroup>>(emptyList())
    val groupedSubjects: State<List<SubjectGroup>> = _groupedSubjects

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    // ✅ ADDED: State for grade levels
    private val _gradeLevels = MutableStateFlow<List<GradeLevelItem>>(emptyList())
    val gradeLevels = _gradeLevels.asStateFlow()
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        fetchSubjects()
    }

    // ✅ ADDED: Function to fetch grade levels
    fun fetchGradeLevels() {
        viewModelScope.launch {
            try {
                _gradeLevels.value = client.get("/curriculum/grade-levels").body()
            } catch (e: Exception) {
                _error.value = "Failed to load grade levels: ${e.message}"
            }
        }
    }

    fun fetchGroupedSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ UPDATED
                val response: CurriculumResponse = client.get("/subjects/curriculum").body()
                val activeSem = response.activeSemester.toIntOrNull() ?: 1

                val allGroups = mutableListOf<SubjectGroup>()

                response.subjects
                    .filter { it.gradeLevelId != null && it.gradeLevelId <= 4 }
                    .groupBy { it.gradeLevel!! }
                    .toSortedMap(compareBy { it.filter { char -> char.isDigit() }.toInt() })
                    .forEach { (gradeLevel, subjects) ->
                        allGroups.add(SubjectGroup(gradeLevel, subjects))
                    }

                val shsSubjectsByGrade = response.subjects
                    .filter { it.gradeLevelId != null && it.gradeLevelId > 4 && it.semester == activeSem }
                    .groupBy { it.gradeLevel!! }
                    .toSortedMap()

                shsSubjectsByGrade.forEach { (gradeLevel, subjects) ->
                    val subjectsGroupedById = subjects.groupBy { it.subjectId }

                    val uniqueSubjects = subjectsGroupedById.map { (_, subjectList) ->
                        if (subjectList.size > 1) {
                            subjectList.first().copy(strandName = null)
                        } else {
                            subjectList.first()
                        }
                    }.sortedBy { it.display_order }

                    allGroups.add(SubjectGroup("$gradeLevel - Semester $activeSem", uniqueSubjects))
                }

                _groupedSubjects.value = allGroups

            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun fetchSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ UPDATED: Fetches from the correct '/subjects' endpoint
                val response: List<Subject> = client.get("/subjects").body()
                _subjects.value = response.sortedBy { it.name }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ MODIFIED: The addSubject function now accepts gradeLevelId
    fun addSubject(name: String, gradeLevelId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val requestBody = AddSubjectWithGradeRequest(
                    subjectName = name,
                    gradeLevelId = gradeLevelId
                )
                val response = client.post("/subjects") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody) // Use the new request body
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchGroupedSubjects() // Refresh the main list
                } else {
                    val errorBody = response.body<Map<String, String>>()
                    onError(errorBody["error"] ?: "Failed to add subject.")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun updateSubject(id: Int, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED
                val response = client.put("/subjects/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(AddSubjectRequest(name))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSubjects() // Refresh list
                } else {
                    onError(response.bodyAsText())
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun deleteSubject(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED
                val response = client.delete("/subjects/$id")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSubjects() // Refresh list
                } else {
                    val errorBody = response.body<Map<String, String>>()
                    onError(errorBody["error"] ?: "Failed to delete.")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}