package com.mnvths.schoolclearance.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.AssignedAccount
import com.mnvths.schoolclearance.data.AssignedSubject
import com.mnvths.schoolclearance.data.ChangePasswordRequest
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.Signatory
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber // --- LOGGING ADDED ---

class SignatoryViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _signatoryList = mutableStateOf<List<Signatory>>(emptyList())
    val signatoryList: State<List<Signatory>> = _signatoryList

    private val _assignedSubjects = mutableStateOf<List<AssignedSubject>>(emptyList())
    val assignedSubjects: State<List<AssignedSubject>> = _assignedSubjects

    private val _isLoading = mutableStateOf<Boolean>(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _assignedSections = mutableStateOf<Map<Int, List<ClassSection>>>(emptyMap())
    val assignedSections: State<Map<Int, List<ClassSection>>> = _assignedSections

    // ✅ Add state for assigned accounts
    private val _assignedAccounts = mutableStateOf<List<AssignedAccount>>(emptyList())
    val assignedAccounts: State<List<AssignedAccount>> = _assignedAccounts

    // ✅ State for the list of sections for a specific account
    private val _sectionsForAccount = mutableStateOf<List<ClassSection>>(emptyList())
    val sectionsForAccount: State<List<ClassSection>> = _sectionsForAccount

    // ✅ State for the list of sections for a specific subject
    private val _sectionsForSubject = mutableStateOf<List<ClassSection>>(emptyList())
    val sectionsForSubject: State<List<ClassSection>> = _sectionsForSubject


    // ✅ Function to fetch sections for a specific account assignment
    fun fetchSectionsForAccount(signatoryId: Int, accountId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _sectionsForAccount.value = emptyList() // Clear previous results
            // --- LOGGING ADDED ---
            Timber.i("Fetching sections for account. Signatory ID: %d, Account ID: %d", signatoryId, accountId)
            try {
                val response: HttpResponse = client.get("/assignments/sections-for-account/$signatoryId/$accountId")
                if (response.status.isSuccess()) {
                    _sectionsForAccount.value = response.body()
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully fetched %d sections for account.", _sectionsForAccount.value.size)
                } else {
                    _error.value = "Failed to load assigned sections for account."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch sections for account. Status: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during fetch sections for account network call.")
            } finally {
                _isLoading.value = false
            }
        }
    }
    // ✅ Function to fetch sections for a specific subject assignment
    fun fetchSectionsForSubject(signatoryId: Int, subjectId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _sectionsForSubject.value = emptyList() // Clear previous results
            // --- LOGGING ADDED ---
            Timber.i("Fetching sections for subject. Signatory ID: %d, Subject ID: %d", signatoryId, subjectId)
            try {
                // This new endpoint matches the one created on the server
                val response: HttpResponse = client.get("/assignments/sections/$signatoryId/$subjectId")
                if (response.status.isSuccess()) {
                    _sectionsForSubject.value = response.body()
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully fetched %d sections for subject.", _sectionsForSubject.value.size)
                } else {
                    _error.value = "Failed to load assigned sections."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch sections for subject. Status: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during fetch sections for subject network call.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ NEW FUNCTION to unassign an account
    fun unassignAccount(
        signatoryId: Int,
        accountId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to unassign account ID %d from signatory ID %d", accountId, signatoryId)
            try {
                val response: HttpResponse = client.delete("/assignments/accounts/$signatoryId/$accountId")
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully unassigned account.")
                    onSuccess()
                    fetchAssignedAccounts(signatoryId) // Refresh the list
                } else {
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to unassign account. Server response: %s", response.status)
                    onError(response.bodyAsText())
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during unassign account network call.")
                onError(e.message ?: "Unknown error")
            }
        }
    }

    // ✅ NEW FUNCTION to fetch assigned accounts
    fun fetchAssignedAccounts(signatoryId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching assigned accounts for signatory ID: %d", signatoryId)
            try {
                val response: HttpResponse = client.get("/assignments/accounts/$signatoryId")
                if (response.status.isSuccess()) {
                    _assignedAccounts.value = response.body()
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully fetched %d assigned accounts.", _assignedAccounts.value.size)
                } else {
                    _error.value = "Failed to load assigned accounts."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch assigned accounts. Status: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during fetch assigned accounts network call.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ NEW FUNCTION to unassign a subject
    fun unassignSubject(
        signatoryId: Int,
        subjectId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to unassign subject ID %d from signatory ID %d", subjectId, signatoryId)
            try {
                val response: HttpResponse = client.delete("/assignments/faculty-signatory/$signatoryId/$subjectId")

                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully unassigned subject.")
                    onSuccess()
                    // Refresh the list automatically after successful deletion
                    fetchAssignedSubjects(signatoryId)
                } else {
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to unassign subject. Server response: %s", response.status)
                    onError(response.bodyAsText())
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during unassign subject network call.")
                onError(e.message ?: "Unknown error")
            }
        }
    }
    fun fetchSignatoryList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching signatory list.")
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.get("/signatories")
                if (response.status.isSuccess()) {
                    val signatoryData: List<Signatory> = response.body()
                    _signatoryList.value = signatoryData
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully fetched %d signatories.", _signatoryList.value.size)
                } else {
                    val errorBody = response.bodyAsText()
                    _error.value = "Server error: ${response.status.description}. Details: $errorBody"
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch signatory list. Server response: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during fetch signatory list network call.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAssignedSection(
        signatoryId: Int,
        subjectId: Int,
        sectionId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to delete assigned section %d for subject %d, signatory %d", sectionId, subjectId, signatoryId)
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.delete("/faculty/$signatoryId/signatory/$subjectId/section/$sectionId")

                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully deleted assigned section.")
                    onSuccess()
                    fetchAssignedSections(signatoryId, subjectId)
                } else {
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to delete assigned section. Server response: %s", response.status)
                    onError("Failed to delete assignment: ${response.bodyAsText()}")
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during delete assigned section network call.")
                onError("Network error: ${e.message}")
            }
        }
    }

    fun fetchAssignedSubjects(signatoryId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching assigned subjects for signatory ID: %d", signatoryId)
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.get("/assignments/faculty-signatory/$signatoryId")
                if (response.status.isSuccess()) {
                    val subjects: List<AssignedSubject> = response.body()
                    _assignedSubjects.value = subjects
                    _assignedSections.value = emptyMap()
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully fetched %d assigned subjects.", _assignedSubjects.value.size)
                } else {
                    _error.value = "Failed to load assigned subjects."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch assigned subjects. Server response: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during fetch assigned subjects network call.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAssignedSections(signatoryId: Int, subjectId: Int) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Fetching assigned sections for subject ID %d, signatory ID %d", subjectId, signatoryId)
            try {
                _isLoading.value = true
                // ✅ UPDATED
                val response: HttpResponse = client.get("/faculty-signatory-sections/$signatoryId/$subjectId")
                if (response.status.isSuccess()) {
                    val sections: List<ClassSection> = response.body()
                    _assignedSections.value = _assignedSections.value + (subjectId to sections)
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully fetched %d assigned sections.", sections.size)
                } else {
                    // Handle error case
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch assigned sections. Server response: %s", response.status)
                }
            } catch (e: Exception) {
                // Handle network error
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during fetch assigned sections network call.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAssignedSubject(signatoryId: Int, subjectId: Int) {
        viewModelScope.launch {
            try {
                // --- LOGGING ADDED (from original code) ---
                Log.d("SignatoryViewModel", "Attempting to delete subject $subjectId for signatory $signatoryId")
                // --- LOGGING ADDED ---
                Timber.i("Attempting to delete assigned subject %d for signatory %d", subjectId, signatoryId)
                // ✅ UPDATED
                val response: HttpResponse = client.delete("/faculty-signatory/$signatoryId/$subjectId")
                if (response.status.isSuccess()) {
                    fetchAssignedSubjects(signatoryId)
                } else {
                    _error.value = "Failed to delete subject assignment."
                    // --- LOGGING ADDED (from original code) ---
                    Log.e("SignatoryViewModel", "Deletion failed: ${response.status.description}")
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to delete assigned subject. Server response: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED (from original code) ---
                Log.e("SignatoryViewModel", "Deletion network error: ${e.stackTraceToString()}")
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during delete assigned subject network call.")
            }
        }
    }

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
            // --- LOGGING ADDED ---
            Timber.i("Attempting to add signatory with username: %s", username)
            try {
                // ✅ UPDATED
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
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully added signatory '%s'.", username)
                    onSuccess()
                    fetchSignatoryList()
                } else {
                    val errorBody = response.bodyAsText()
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to add signatory. Server response: %s", response.status)
                    onError("Failed to add signatory: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during add signatory network call.")
                onError("Network error: ${e.message}")
            }
        }
    }

    fun deleteSignatory(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to delete signatory with ID: %d", id)
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.delete("/signatories/$id")
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully deleted signatory with ID: %d", id)
                    onSuccess()
                    fetchSignatoryList()
                } else {
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to delete signatory. Server response: %s", response.status)
                    onError("Failed to delete signatory: ${response.status.description}")
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during delete signatory network call.")
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
            // --- LOGGING ADDED ---
            Timber.i("Attempting to edit signatory with ID: %d", id)
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

                // ✅ UPDATED
                val response: HttpResponse = client.put("/signatories/${id}") {
                    contentType(ContentType.Application.Json)
                    setBody(bodyMap)
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully edited signatory with ID: %d", id)
                    onSuccess()
                    fetchSignatoryList()
                } else {
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to edit signatory. Server response: %s", response.status)
                    onError("Failed to update signatory: ${response.status.description}")
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during edit signatory network call.")
                onError("Network error: ${e.message}")
            }
        }
    }
}