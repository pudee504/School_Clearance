package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AssignedItem
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.Signatory
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch

// ❌ The duplicate inner class has been removed
class SignatoryViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _signatoryList = mutableStateOf<List<Signatory>>(emptyList())
    val signatoryList: State<List<Signatory>> = _signatoryList

    private val _assignedItems = mutableStateOf<List<AssignedItem>>(emptyList())
    val assignedItems: State<List<AssignedItem>> = _assignedItems

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    // ✅ ADDED BACK: State to hold the sections for an expanded subject
    private val _assignedSections = mutableStateOf<Map<Int, List<ClassSection>>>(emptyMap())
    val assignedSections: State<Map<Int, List<ClassSection>>> = _assignedSections


    fun fetchAssignedSections(signatoryId: Int, subjectId: Int) {
        viewModelScope.launch {
            try {
                // Assuming this is your endpoint for sections. Update if it's different.
                val response: HttpResponse = client.get("/assignments/sections/$signatoryId/$subjectId")
                if (response.status.isSuccess()) {
                    val sections: List<ClassSection> = response.body()
                    _assignedSections.value = _assignedSections.value + (subjectId to sections)
                } else {
                    _assignedSections.value = _assignedSections.value + (subjectId to emptyList())
                }
            } catch (e: Exception) {
                _assignedSections.value = _assignedSections.value + (subjectId to emptyList())
            }
        }
    }

    fun unassignItem(
        assignmentId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.delete("/assignments/$assignmentId")
                if (response.status.isSuccess()) {
                    // For a fast UI update, remove the item from the local list
                    _assignedItems.value = _assignedItems.value.filterNot { it.assignmentId == assignmentId }
                    onSuccess()
                } else {
                    onError("Failed to unassign: ${response.bodyAsText()}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
    fun fetchSignatoryList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("/signatories")
                if (response.status.isSuccess()) {
                    val signatoryData: List<Signatory> = response.body()
                    _signatoryList.value = signatoryData
                } else {
                    val errorBody = response.bodyAsText()
                    _error.value = "Server error: ${response.status.description}. Details: $errorBody"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAssignedItems(signatoryId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("/assignments/faculty-signatory/$signatoryId")
                if (response.status.isSuccess()) {
                    val items: List<AssignedItem> = response.body()
                    _assignedItems.value = items
                } else {
                    _error.value = "Failed to load assigned items."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // NOTE: The old, broken functions like deleteAssignedSubject and fetchAssignedSections have been removed.
    // They would need to be rewritten to work with the new `AssignedItem` data model if you need them.

    fun addSignatory(
        username: String,
        password: String,
        firstName: String,
        middleName: String?,
        lastName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("/signatories") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "username" to username,
                            "password" to password,
                            "firstName" to firstName,
                            "middleName" to middleName,
                            "lastName" to lastName
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSignatoryList()
                } else {
                    val errorBody = response.bodyAsText()
                    onError("Failed to add signatory: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun deleteSignatory(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.delete("/signatories/$id")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSignatoryList()
                } else {
                    onError("Failed to delete signatory: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun editSignatory(
        id: Int,
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        middleName: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val bodyMap = mutableMapOf<String, String?>(
                    "username" to username,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "middleName" to middleName
                )

                if (password.isNotBlank()) {
                    bodyMap["password"] = password
                }

                val response: HttpResponse = client.put("/signatories/${id}") {
                    contentType(ContentType.Application.Json)
                    setBody(bodyMap)
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchSignatoryList()
                } else {
                    onError("Failed to update signatory: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}