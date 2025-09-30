package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.Account
import com.mnvths.schoolclearance.data.AssignItemRequest
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

    private val _subjects = mutableStateOf<List<Subject>>(emptyList())
    val subjects: State<List<Subject>> = _subjects

    private val _accounts = mutableStateOf<List<Account>>(emptyList())
    val accounts: State<List<Account>> = _accounts

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    // ✅ This property is needed by the screen
    private val _sections = mutableStateOf<List<ClassSection>>(emptyList())
    val sections: State<List<ClassSection>> = _sections

    // ✅ NEW: This function is now present
    fun fetchAllClassSections() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Assuming you have a /sections endpoint that returns all sections
                val response: HttpResponse = client.get("/sections")
                if (response.status.isSuccess()) {
                    _sections.value = response.body()
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

    // ✅ NEW: This function is now present
    fun fetchAssignedSections(
        signatoryId: Int,
        subjectId: Int,
        onResult: (List<ClassSection>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.get("/assignments/sections/$signatoryId/$subjectId")
                if (response.status.isSuccess()) {
                    onResult(response.body())
                } else {
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }

    // ✅ NEW: This function is now present
    fun assignClassesToSubject(
        signatoryId: Int,
        subjectId: Int,
        sectionIds: List<Int>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("/assignments/assign-classes") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf(
                        "signatoryId" to signatoryId,
                        "subjectId" to subjectId,
                        "sectionIds" to sectionIds
                    ))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                } else {
                    onError("Failed to assign classes: ${response.bodyAsText()}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun fetchSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
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

    fun fetchAccounts() {
        viewModelScope.launch {
            // Re-using isLoading can be tricky; for simplicity, we manage it here too.
            _isLoading.value = true
            _error.value = null
            try {
                _accounts.value = client.get("/accounts").body()
            } catch (e: Exception) {
                _error.value = "Network error fetching accounts: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignItemToSignatory(
        signatoryId: Int,
        itemId: Int,
        itemType: String, // "Subject" or "Account"
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ Create an instance of the new data class
                val requestBody = AssignItemRequest(
                    signatoryId = signatoryId,
                    itemId = itemId,
                    itemType = itemType
                )

                val response: HttpResponse = client.post("/assignments/faculty-signatory") {
                    contentType(ContentType.Application.Json)
                    // ✅ Set the body to the new data class instance
                    setBody(requestBody)
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    onError("Failed to assign: $errorBody")
                }
            } catch (e: Exception) {
                // The error you saw comes from this catch block
                onError("Error assigning item: ${e.message}")
            }
        }
    }
}