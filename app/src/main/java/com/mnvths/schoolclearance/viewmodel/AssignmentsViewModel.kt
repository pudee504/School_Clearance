package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.Account
import com.mnvths.schoolclearance.data.AssignClassesRequest
import com.mnvths.schoolclearance.data.AssignSectionsToAccountRequest
import com.mnvths.schoolclearance.data.AssignSectionsToSubjectRequest
import com.mnvths.schoolclearance.data.AssignedAccount
import com.mnvths.schoolclearance.data.AssignedSubject
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.data.Subject
import com.mnvths.schoolclearance.data.SubjectGroup
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber // --- LOGGING ADDED ---

class AssignmentViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _subjects = mutableStateOf<List<Subject>>(emptyList())
    val subjects: State<List<Subject>> = _subjects

    private val _assignedSubjects = mutableStateOf<List<AssignedSubject>>(emptyList())
    val assignedSubjects: State<List<AssignedSubject>> = _assignedSubjects

    private val _sections = mutableStateOf<List<ClassSection>>(emptyList())
    val sections: State<List<ClassSection>> = _sections

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error


    // ✅ Accounts
    private val _accounts = mutableStateOf<List<Account>>(emptyList())
    val accounts: State<List<Account>> = _accounts
    private val _assignedAccounts = mutableStateOf<List<AssignedAccount>>(emptyList())
    val assignedAccounts: State<List<AssignedAccount>> = _assignedAccounts

    // ✅ NEW: State for available sections
    private val _availableSections = mutableStateOf<List<ClassSection>>(emptyList())
    val availableSections: State<List<ClassSection>> = _availableSections

    // ✅ --- NEW EFFICIENT FETCH FUNCTIONS ---

    // ✅ NEW: Function to load available sections for a specific subject
    fun loadAvailableSections(signatoryId: Int, subjectId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Loading available sections for subject ID: %d", subjectId)
            try {
                val response: HttpResponse = client.get("/assignments/available-sections/$signatoryId/$subjectId")
                if (response.status.isSuccess()) {
                    _availableSections.value = response.body()
                    // --- LOGGING ADDED ---
                    Timber.i("Found %d available sections.", _availableSections.value.size)
                } else {
                    _error.value = "Failed to load available sections."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to load available sections. Status: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error loading available sections.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ NEW: Function to assign selected sections to a subject
    // ✅ FIX: Update the function to use the new data class
    fun assignSectionsToSubject(
        signatoryId: Int,
        subjectId: Int,
        sectionIds: List<Int>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Assigning %d sections to subject ID: %d", sectionIds.size, subjectId)
            try {
                val response: HttpResponse = client.post("/assignments/sections") {
                    contentType(ContentType.Application.Json)
                    // Use the serializable data class instead of a mapOf
                    setBody(AssignSectionsToSubjectRequest(signatoryId, subjectId, sectionIds))
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully assigned sections to subject.")
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to assign sections to subject. Server error: %s", errorBody)
                    onError("Failed to assign sections: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during assign sections to subject network call.")
                onError("Network error: ${e.message}")
            }
        }
    }
    // ✅ NEW: Function to load available sections for a specific account
    fun loadAvailableSectionsForAccount(signatoryId: Int, accountId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Loading available sections for account ID: %d", accountId)
            try {
                // We re-use the `_availableSections` state from the subject flow
                val response: HttpResponse = client.get("/assignments/available-sections-for-account/$signatoryId/$accountId")
                if (response.status.isSuccess()) {
                    _availableSections.value = response.body()
                    // --- LOGGING ADDED ---
                    Timber.i("Found %d available sections for account.", _availableSections.value.size)
                } else {
                    _error.value = "Failed to load available sections."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to load available sections for account. Status: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error loading available sections for account.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ NEW: Function to assign selected sections to an account
    // ✅ FIX: Also update the account assignment function to prevent future errors
    fun assignSectionsToAccount(
        signatoryId: Int,
        accountId: Int,
        sectionIds: List<Int>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Assigning %d sections to account ID: %d", sectionIds.size, accountId)
            try {
                val response: HttpResponse = client.post("/assignments/sections-for-account") {
                    contentType(ContentType.Application.Json)
                    // Use the serializable data class instead of a mapOf
                    setBody(AssignSectionsToAccountRequest(signatoryId, accountId, sectionIds))
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully assigned sections to account.")
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to assign sections to account. Server error: %s", errorBody)
                    onError("Failed to assign sections: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during assign sections to account network call.")
                onError("Network error: ${e.message}")
            }
        }
    }
    fun loadSubjectAssignmentData(signatoryId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Loading subject assignment data for signatory ID: %d", signatoryId)
            try {
                // This will run both launch blocks at the same time
                // and wait for both to complete before continuing.
                coroutineScope {
                    launch { fetchSubjects() }
                    launch { fetchAssignedSubjectsForSignatory(signatoryId) }
                }
            } catch (e: Exception) {
                _error.value = "An unexpected error occurred: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error loading subject assignment data.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAccountAssignmentData(signatoryId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Loading account assignment data for signatory ID: %d", signatoryId)
            try {
                coroutineScope {
                    launch { fetchAccounts() }
                    launch { fetchAssignedAccountsForSignatory(signatoryId) }
                }
            } catch (e: Exception) {
                _error.value = "An unexpected error occurred: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error loading account assignment data.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching all subjects.")
            try {
                // ✅ UPDATED: Endpoint to fetch subjects
                val response: HttpResponse = client.get("/subjects")
                if (response.status.isSuccess()) {
                    _subjects.value = response.body()
                } else {
                    _error.value = "Failed to load subjects."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch subjects. Status: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error fetching subjects.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchAssignedSubjectsForSignatory(signatoryId: Int) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Fetching assigned subjects for signatory ID: %d", signatoryId)
            try {
                _isLoading.value = true
                // ✅ UPDATED to match your existing server route
                val response: HttpResponse = client.get("/assignments/faculty-signatory/$signatoryId")
                if (response.status.isSuccess()) {
                    _assignedSubjects.value = response.body()
                } else {
                    _error.value = "Failed to load assigned subjects for this signatory."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch assigned subjects. Status: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error fetching assigned subjects.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAllClassSections() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching all class sections.")
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.get("/students/class-sections")
                if (response.status.isSuccess()) {
                    val classSections: List<ClassSection> = response.body()
                    _sections.value = classSections
                } else {
                    _error.value = "Failed to load class sections."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch class sections. Status: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error fetching class sections.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignSubjectToSignatory(
        signatoryId: Int,
        subjectId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Assigning subject ID %d to signatory ID %d", subjectId, signatoryId)
            try {
                // ✅ UPDATED: Endpoint for assignments. You will need to create this on your backend.
                val response: HttpResponse = client.post("/assignments/assign-subject") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "signatoryId" to signatoryId,
                            "subjectId" to subjectId
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully assigned subject to signatory.")
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to assign subject to signatory. Server error: %s", errorBody)
                    onError("Failed to assign subject: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during assign subject to signatory network call.")
                onError("Network error: ${e.message}")
            }
        }
    }

    fun fetchAssignedSections(
        signatoryId: Int,
        subjectId: Int,
        onResult: (List<ClassSection>) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Fetching assigned sections for subject ID %d, signatory ID %d", subjectId, signatoryId)
            try {
                // ✅ UPDATED: Endpoint for assignments. You will need to create this on your backend.
                val response: HttpResponse =
                    client.get("/assignments/sections/$signatoryId/$subjectId")
                if (response.status.isSuccess()) {
                    val assigned: List<ClassSection> = response.body()
                    // --- LOGGING ADDED ---
                    Timber.i("Found %d assigned sections.", assigned.size)
                    onResult(assigned)
                } else {
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch assigned sections. Status: %s", response.status)
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error fetching assigned sections.")
                onResult(emptyList())
            }
        }
    }

    fun assignClassesToSubject(
        signatoryId: Int,
        subjectId: Int,
        sectionIds: List<Int>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Assigning %d classes to subject ID %d", sectionIds.size, subjectId)
            try {
                // ✅ UPDATED: Endpoint for assignments. You will need to create this on your backend.
                val response: HttpResponse = client.post("/assignments/assign-classes") {
                    contentType(ContentType.Application.Json)
                    setBody(AssignClassesRequest(signatoryId, subjectId, sectionIds))
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully assigned classes to subject.")
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to assign classes to subject. Server error: %s", errorBody)
                    onError("Failed to assign classes: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error assigning classes to subject.")
                onError("Network error: ${e.message}")
            }
        }
    }

    // ✅ THIS IS THE NEW FUNCTION THAT FIXES THE 'Unresolved reference' ERROR
    fun assignMultipleSubjectsToSignatory(
        signatoryId: Int,
        subjects: List<Subject>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Assigning %d subjects to signatory ID %d", subjects.size, signatoryId)
            var allSuccessful = true
            for (subject in subjects) {
                try {
                    // ✅ ADD the "/assignments" prefix here
                    val response: HttpResponse = client.post("/assignments/faculty-signatory") {
                        contentType(ContentType.Application.Json)
                        setBody(mapOf("signatoryId" to signatoryId, "subjectId" to subject.id))
                    }
                    if (!response.status.isSuccess()) {
                        allSuccessful = false
                        // --- LOGGING ADDED ---
                        Timber.w("Error assigning subject '%s'. Status: %s", subject.name, response.status)
                        onError("Error assigning ${subject.name}.")
                        break // Stop on the first error
                    }
                } catch (e: Exception) {
                    allSuccessful = false
                    // --- LOGGING ADDED ---
                    Timber.e(e, "Network error while assigning subject '%s'.", subject.name)
                    onError("Network error while assigning ${subject.name}.")
                    break // Stop on the first error
                }
            }

            if (allSuccessful) {
                // --- LOGGING ADDED ---
                Timber.i("Successfully assigned all subjects.")
                onSuccess()
                fetchAssignedSubjectsForSignatory(signatoryId)
            }
        }
    }

    // ✅ --- Account Functions ---
    private fun fetchAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching all accounts.")
            try {
                // Assuming you have an /accounts endpoint from accounts.js
                val response: HttpResponse = client.get("/accounts")
                if (response.status.isSuccess()) {
                    _accounts.value = response.body()
                } else {
                    _error.value = "Failed to load accounts."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch accounts. Status: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error fetching accounts.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchAssignedAccountsForSignatory(signatoryId: Int) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Fetching assigned accounts for signatory ID: %d", signatoryId)
            try {
                _isLoading.value = true
                val response: HttpResponse = client.get("/assignments/accounts/$signatoryId")
                if (response.status.isSuccess()) {
                    _assignedAccounts.value = response.body()
                } else {
                    _error.value = "Failed to load assigned accounts for this signatory."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to fetch assigned accounts. Status: %s", response.status)
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error fetching assigned accounts.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignMultipleAccountsToSignatory(
        signatoryId: Int,
        accounts: List<Account>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Assigning %d accounts to signatory ID %d", accounts.size, signatoryId)
            var allSuccessful = true
            for (account in accounts) {
                try {
                    val response: HttpResponse = client.post("/assignments/accounts") {
                        contentType(ContentType.Application.Json)
                        setBody(mapOf("signatoryId" to signatoryId, "accountId" to account.id))
                    }
                    if (!response.status.isSuccess()) {
                        allSuccessful = false
                        // --- LOGGING ADDED ---
                        Timber.w("Error assigning account '%s'. Status: %s", account.name, response.status)
                        onError("Error assigning ${account.name}.")
                        break
                    }
                } catch (e: Exception) {
                    allSuccessful = false
                    // --- LOGGING ADDED ---
                    Timber.e(e, "Network error while assigning account '%s'.", account.name)
                    onError("Network error while assigning ${account.name}.")
                    break
                }
            }

            if (allSuccessful) {
                // --- LOGGING ADDED ---
                Timber.i("Successfully assigned all accounts.")
                onSuccess()
                fetchAssignedAccountsForSignatory(signatoryId)
            }
        }
    }

}