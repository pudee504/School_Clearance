package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.FullyClearedStudent
import com.mnvths.schoolclearance.data.GradeLevelItem
import com.mnvths.schoolclearance.data.ReportsResponse
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch

class ReportsViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _reportData = mutableStateOf<ReportsResponse?>(null)
    val reportData: State<ReportsResponse?> = _reportData

    private val _gradeLevels = mutableStateOf<List<GradeLevelItem>>(emptyList())
    val gradeLevels: State<List<GradeLevelItem>> = _gradeLevels

    private val _sections = mutableStateOf<List<ClassSection>>(emptyList())
    val sections: State<List<ClassSection>> = _sections

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        fetchGradeLevels()
    }

    fun fetchFullyClearedStudents(gradeLevelId: Int? = null, sectionId: Int? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: ReportsResponse = client.get("/reports/fully-cleared") {
                    parameter("gradeLevelId", gradeLevelId)
                    parameter("sectionId", sectionId)
                }.body()
                _reportData.value = response
            } catch (e: Exception) {
                _error.value = "Error fetching report: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchGradeLevels() {
        viewModelScope.launch {
            try {
                val response: List<GradeLevelItem> = client.get("/grade-levels").body()
                _gradeLevels.value = response
            } catch (e: Exception) {
                _error.value = "Error fetching grade levels: ${e.message}"
            }
        }
    }

    fun fetchSectionsForGradeLevel(gradeLevelId: Int) {
        viewModelScope.launch {
            try {
                val response: List<com.mnvths.schoolclearance.data.ClassSection> = client.get("/sections/grade-level/$gradeLevelId").body()
                _sections.value = response
            } catch (e: Exception) {
                _error.value = "Error fetching sections: ${e.message}"
                _sections.value = emptyList()
            }
        }
    }
}
