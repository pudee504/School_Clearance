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
        // Automatically fetch data when the ViewModel is created
        fetchClassSections()
        fetchAllGradeLevels()
    }

    fun fetchClassSections() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/sections/class-sections")
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
                _gradeLevels.value = client.get("http://10.0.2.2:3000/sections/grade-levels").body()
            } catch (e: Exception) {
                // This is a non-critical error, so we can log it or show a subtle message
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
                val response: HttpResponse = client.post("http://10.0.2.2:3000/sections") {
                    contentType(ContentType.Application.Json)
                    // Backend expects the grade level number and section name
                    setBody(AddSectionRequest(gradeLevel = gradeLevel, sectionName = sectionName))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchClassSections() // Refresh the list after adding
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
                val response: HttpResponse = client.put("http://10.0.2.2:3000/sections/$sectionId") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateSectionRequest(gradeLevel = gradeLevel, sectionName = sectionName))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchClassSections() // Refresh the list after updating
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
                val response: HttpResponse = client.delete("http://10.0.2.2:3000/sections/$sectionId")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchClassSections() // Refresh the list after deleting
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