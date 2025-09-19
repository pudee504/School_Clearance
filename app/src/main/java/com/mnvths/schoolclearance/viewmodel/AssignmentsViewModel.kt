package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AssignClassesRequest
import com.mnvths.schoolclearance.data.AssignedSignatory
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.Signatory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.mnvths.schoolclearance.network.KtorClient

class AssignmentViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _signatories = mutableStateOf<List<Signatory>>(emptyList())
    val signatories: State<List<Signatory>> = _signatories

    private val _sections = mutableStateOf<List<ClassSection>>(emptyList())
    val sections: State<List<ClassSection>> = _sections

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    fun fetchSignatories() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/signatory")
                if (response.status.isSuccess()) {
                    _signatories.value = response.body()
                } else {
                    _error.value = "Failed to load signatories."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // NEW FUNCTION
    private val _assignedSignatories = mutableStateOf<List<AssignedSignatory>>(emptyList())
    val assignedSignatories: State<List<AssignedSignatory>> = _assignedSignatories

    fun fetchAssignedSignatoriesForFaculty(facultyId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response: HttpResponse = client.get("http://10.0.2.2:3000/faculty-signatory/$facultyId")
                if (response.status.isSuccess()) {
                    _assignedSignatories.value = response.body()
                } else {
                    _error.value = "Failed to load assigned signatories for this faculty."
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
                val response: HttpResponse = client.get("http://10.0.2.2:3000/class-sections")
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

    fun assignSignatoryToFaculty(
        facultyId: Int,
        signatoryId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:3000/assign-signatory") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "facultyId" to facultyId,
                            "signatoryId" to signatoryId
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    onError("Failed to select signatory: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun fetchAssignedSections(
        facultyId: Int,
        signatoryId: Int,
        onResult: (List<ClassSection>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse =
                    client.get("http://10.0.2.2:3000/faculty-signatory-sections/$facultyId/$signatoryId")
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

    fun assignClassesToFaculty(
        facultyId: Int,
        signatoryId: Int,
        sectionIds: List<Int>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:3000/assign-classes") {
                    contentType(ContentType.Application.Json)
                    setBody(AssignClassesRequest(facultyId, signatoryId, sectionIds))
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