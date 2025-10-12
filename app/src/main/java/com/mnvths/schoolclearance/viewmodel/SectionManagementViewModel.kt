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
import timber.log.Timber // --- LOGGING ADDED ---

class SectionManagementViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    private val _classSections = MutableStateFlow<List<ClassSection>>(emptyList())
    val classSections: StateFlow<List<ClassSection>> = _classSections.asStateFlow()

    private val _gradeLevels = MutableStateFlow<List<String>>(emptyList())
    val gradeLevels: StateFlow<List<String>> = _gradeLevels.asStateFlow()

    init {
        // --- LOGGING ADDED ---
        Timber.i("SectionManagementViewModel initialized.")
        fetchClassSections()
        fetchAllGradeLevels()
    }

    fun fetchClassSections() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching class sections.")
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.get("/sections/class-sections")
                if (response.status.isSuccess()) {
                    _classSections.value = response.body()
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully fetched ${_classSections.value.size} class sections.")
                } else {
                    error.value = "Failed to load sections: ${response.status}"
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch class sections. Status: %s", response.status)
                }
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during fetch class sections network call.")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun fetchAllGradeLevels() {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Fetching all grade levels.")
            try {
                // ✅ UPDATED
                _gradeLevels.value = client.get("/sections/grade-levels").body()
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Failed to fetch grade levels.")
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
            // --- LOGGING ADDED ---
            Timber.i("Attempting to add section '%s' to grade '%s'.", sectionName, gradeLevel)
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.post("/sections") {
                    contentType(ContentType.Application.Json)
                    setBody(AddSectionRequest(gradeLevel = gradeLevel, sectionName = sectionName))
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully added section '%s'.", sectionName)
                    onSuccess()
                    fetchClassSections()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to add section. Server error: %s", errorMessage.ifBlank { response.status.toString() })
                    onError(errorMessage.ifBlank { "Failed to add section." })
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during add section network call.")
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
            // --- LOGGING ADDED ---
            Timber.i("Attempting to update section ID: %d", sectionId)
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.put("/sections/$sectionId") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateSectionRequest(gradeLevel = gradeLevel, sectionName = sectionName))
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully updated section ID: %d", sectionId)
                    onSuccess()
                    fetchClassSections()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to update section ID %d. Server error: %s", sectionId, errorMessage.ifBlank { response.status.toString() })
                    onError(errorMessage.ifBlank { "Failed to update section." })
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during update section network call for ID: %d", sectionId)
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
            // --- LOGGING ADDED ---
            Timber.i("Attempting to delete section ID: %d", sectionId)
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.delete("/sections/$sectionId")
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully deleted section ID: %d", sectionId)
                    onSuccess()
                    fetchClassSections()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to delete section ID %d. Server error: %s", sectionId, errorMessage.ifBlank { response.status.toString() })
                    onError(errorMessage.ifBlank { "Failed to delete section." })
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during delete section network call for ID: %d", sectionId)
                onError("Network Error: ${e.message}")
            }
        }
    }
}