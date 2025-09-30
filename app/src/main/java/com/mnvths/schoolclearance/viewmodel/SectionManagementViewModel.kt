package com.mnvths.schoolclearance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AddSectionRequest
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.UpdateSectionRequest
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.body
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SectionManagementViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    private val _classSections = MutableStateFlow<List<ClassSection>>(emptyList())
    val classSections: StateFlow<List<ClassSection>> = _classSections.asStateFlow()

    private val _gradeLevels = MutableStateFlow<List<String>>(emptyList())
    val gradeLevels: StateFlow<List<String>> = _gradeLevels.asStateFlow()

    init {
        fetchClassSections()
        fetchAllGradeLevels()
    }

    fun fetchClassSections() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.get("/sections/class-sections")
                if (response.status.isSuccess()) {
                    _classSections.value = response.body()
                } else {
                    error.value = "Failed to load sections: ${response.status}"
                }
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun fetchAllGradeLevels() {
        viewModelScope.launch {
            try {
                // ✅ UPDATED
                _gradeLevels.value = client.get("/sections/grade-levels").body()
            } catch (e: Exception) {
                println("Failed to fetch grade levels: ${e.message}")
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
                // ✅ UPDATED
                val response: HttpResponse = client.post("/sections") {
                    contentType(ContentType.Application.Json)
                    setBody(AddSectionRequest(gradeLevel = gradeLevel, sectionName = sectionName))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchClassSections()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    onError(errorMessage.ifBlank { "Failed to add section." })
                }
            } catch (e: Exception) {
                onError("Network Error: ${e.message}")
            }
        }
    }

    fun updateSection(
        sectionId: Int,
        gradeLevel: String,
        sectionName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.put("/sections/$sectionId") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateSectionRequest(gradeLevel = gradeLevel, sectionName = sectionName))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchClassSections()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    onError(errorMessage.ifBlank { "Failed to update section." })
                }
            } catch (e: Exception) {
                onError("Network Error: ${e.message}")
            }
        }
    }

    fun deleteSection(
        sectionId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.delete("/sections/$sectionId")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchClassSections()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    onError(errorMessage.ifBlank { "Failed to delete section." })
                }
            } catch (e: Exception) {
                onError("Network Error: ${e.message}")
            }
        }
    }
}