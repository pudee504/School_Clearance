package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AddSubjectRequest
import com.mnvths.schoolclearance.data.CurriculumResponse
import com.mnvths.schoolclearance.data.CurriculumSubject
import com.mnvths.schoolclearance.data.Subject
import com.mnvths.schoolclearance.data.SubjectGroup
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch

class SubjectViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _subjects = mutableStateOf<List<Subject>>(emptyList())
    val subjects: State<List<Subject>> = _subjects

    // ✅ START: New state for the grouped curriculum list
    private val _groupedSubjects = mutableStateOf<List<SubjectGroup>>(emptyList())
    val groupedSubjects: State<List<SubjectGroup>> = _groupedSubjects
    // ✅ END: New state

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        fetchSubjects()
    }

    // ✅ START: New function to fetch and process grouped subjects
    // ✅ MODIFIED a function to implement the new grouping logic
    fun fetchGroupedSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: CurriculumResponse = client.get("http://10.0.2.2:3000/subjects/curriculum").body()
                val activeSem = response.activeSemester.toIntOrNull() ?: 1

                val allGroups = mutableListOf<SubjectGroup>()

                // JHS Grouping (no change)
                response.subjects
                    .filter { it.gradeLevelId != null && it.gradeLevelId <= 4 }
                    .groupBy { it.gradeLevel!! }
                    .toSortedMap(compareBy { it.filter { char -> char.isDigit() }.toInt() })
                    .forEach { (gradeLevel, subjects) ->
                        allGroups.add(SubjectGroup(gradeLevel, subjects))
                    }

                // ✅ START: New logic for SHS Grouping
                val shsSubjectsByGrade = response.subjects
                    .filter { it.gradeLevelId != null && it.gradeLevelId > 4 && it.semester == activeSem }
                    .groupBy { it.gradeLevel!! }
                    .toSortedMap()

                shsSubjectsByGrade.forEach { (gradeLevel, subjects) ->
                    // This block processes the list to handle duplicates and identify specialized subjects
                    val subjectsGroupedById = subjects.groupBy { it.subjectId }

                    val uniqueSubjects = subjectsGroupedById.map { (_, subjectList) ->
                        // If a subject appears more than once, it's a Core/Applied subject. Don't show a strand.
                        if (subjectList.size > 1) {
                            subjectList.first().copy(strandName = null)
                        } else {
                            // If it appears only once, it's specialized. Keep the strand name.
                            subjectList.first()
                        }
                    }.sortedBy { it.display_order } // Re-sort after processing

                    allGroups.add(SubjectGroup("$gradeLevel - Semester $activeSem", uniqueSubjects))
                }
                // ✅ END: New logic for SHS Grouping

                _groupedSubjects.value = allGroups

            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    // ✅ END: New fetch function


    fun fetchSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ Fetches from the correct '/subjects' endpoint
                val response: List<Subject> = client.get("http://10.0.2.2:3000/subjects").body()
                _subjects.value = response.sortedBy { it.name }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSubject(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = client.post("http://10.0.2.2:3000/subjects") {
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

    fun updateSubject(id: Int, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = client.put("http://10.0.2.2:3000/subjects/$id") {
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
                val response = client.delete("http://10.0.2.2:3000/subjects/$id")
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