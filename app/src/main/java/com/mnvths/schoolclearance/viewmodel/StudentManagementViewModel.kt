package com.mnvths.schoolclearance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.*
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
import kotlinx.serialization.Serializable
import timber.log.Timber // --- LOGGING ADDED ---

@Serializable
data class StudentInSection(
    val id: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String
)

@Serializable
data class AssignStudentsRequest(
    val sectionId: Int,
    val studentIds: List<String>
)


class StudentManagementViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    private val _students = MutableStateFlow<List<StudentListItem>>(emptyList())
    val students: StateFlow<List<StudentListItem>> = _students.asStateFlow()

    private val _studentDetails = MutableStateFlow<StudentDetailsForEdit?>(null)
    val studentDetails: StateFlow<StudentDetailsForEdit?> = _studentDetails.asStateFlow()

    private val _adminStudentProfile = MutableStateFlow<AdminStudentProfile?>(null)
    val adminStudentProfile: StateFlow<AdminStudentProfile?> = _adminStudentProfile.asStateFlow()

    private val _gradeLevels = MutableStateFlow<List<GradeLevelItem>>(emptyList())
    val gradeLevels: StateFlow<List<GradeLevelItem>> = _gradeLevels.asStateFlow()

    private val _classSections = MutableStateFlow<List<ClassSection>>(emptyList())
    val classSections: StateFlow<List<ClassSection>> = _classSections.asStateFlow()

    private val _shsTracks = MutableStateFlow<List<ShsTrack>>(emptyList())
    val shsTracks: StateFlow<List<ShsTrack>> = _shsTracks.asStateFlow()

    private val _shsStrands = MutableStateFlow<List<ShsStrand>>(emptyList())
    val shsStrands: StateFlow<List<ShsStrand>> = _shsStrands.asStateFlow()

    private val _specializations = MutableStateFlow<List<Specialization>>(emptyList())
    val specializations: StateFlow<List<Specialization>> = _specializations.asStateFlow()
    // ✅ NEW: Add a separate state for this screen to avoid conflicts
    private val _unassignedStudents = MutableStateFlow<List<StudentListItem>>(emptyList())
    val unassignedStudents: StateFlow<List<StudentListItem>> = _unassignedStudents.asStateFlow()

    private val _studentsInSection = MutableStateFlow<List<StudentInSection>>(emptyList())
    val studentsInSection: StateFlow<List<StudentInSection>> = _studentsInSection.asStateFlow()

    init {
        // --- LOGGING ADDED ---
        Timber.i("StudentManagementViewModel initialized.")
        fetchAllStudents()
        fetchGradeLevels()
        fetchClassSections()
        fetchShsTracks()
        fetchShsStrands()
    }
    fun fetchUnassignedStudents() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching unassigned students.")
            try {
                // This calls the new, efficient endpoint
                _unassignedStudents.value = client.get("/students/unassigned").body()
                // --- LOGGING ADDED ---
                Timber.i("Successfully fetched ${_unassignedStudents.value.size} unassigned students.")
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Failed to fetch unassigned students.")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun unassignStudentFromSection(
        studentId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading.value = true
            // --- LOGGING ADDED ---
            Timber.i("Attempting to unassign student ID: %s from section.", studentId)
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.put("/students/unassign/$studentId")
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully unassigned student ID: %s", studentId)
                    onSuccess()
                    fetchAllStudents()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to unassign student ID %s. Server error: %s", studentId, errorMessage.ifBlank { response.status.toString() })
                    onError(errorMessage.ifBlank { "Failed to unassign student." })
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during unassign student network call for ID: %s", studentId)
                onError("Network error: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun assignStudentsToSection(
        sectionId: Int,
        studentIds: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (studentIds.isEmpty()) {
                onError("No students selected.")
                return@launch
            }
            isLoading.value = true
            // --- LOGGING ADDED ---
            Timber.i("Attempting to assign %d students to section ID: %d", studentIds.size, sectionId)
            try {
                val requestBody = AssignStudentsRequest(sectionId, studentIds)
                // ✅ UPDATED
                val response: HttpResponse = client.put("/students/assign-section") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully assigned students to section ID: %d", sectionId)
                    onSuccess()
                    fetchAllStudents()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to assign students to section ID %d. Server error: %s", sectionId, errorMessage.ifBlank { response.status.toString() })
                    onError(errorMessage.ifBlank { "Failed to assign students." })
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during assign students network call for section ID: %d", sectionId)
                onError("Network error: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun fetchStudentsBySection(sectionId: Int) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching students for section ID: %d", sectionId)
            try {
                // ✅ UPDATED
                _studentsInSection.value = client.get("/students/section/$sectionId").body()
                // --- LOGGING ADDED ---
                Timber.i("Successfully fetched %d students for section ID: %d", _studentsInSection.value.size, sectionId)
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Failed to fetch students for section ID: %d", sectionId)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearStudentsInSection() {
        // --- LOGGING ADDED ---
        Timber.d("Clearing students in section from state.")
        _studentsInSection.value = emptyList()
    }

    fun fetchGradeLevels() = viewModelScope.launch {
        // --- LOGGING ADDED ---
        Timber.i("Fetching grade levels.")
        try {
            // ✅ UPDATED
            _gradeLevels.value = client.get("/curriculum/grade-levels").body()
        } catch (e: Exception) {
            error.value = "Network Error: ${e.message}"
            // --- LOGGING ADDED ---
            Timber.e(e, "Failed to fetch grade levels.")
        }
    }

    fun fetchClassSections() = viewModelScope.launch {
        // --- LOGGING ADDED ---
        Timber.i("Fetching class sections.")
        try {
            // ✅ UPDATED
            _classSections.value = client.get("/curriculum/sections").body()
        } catch (e: Exception) {
            error.value = "Network Error: ${e.message}"
            // --- LOGGING ADDED ---
            Timber.e(e, "Failed to fetch class sections.")
        }
    }

    fun fetchShsTracks() = viewModelScope.launch {
        // --- LOGGING ADDED ---
        Timber.i("Fetching SHS tracks.")
        try {
            // ✅ UPDATED
            _shsTracks.value = client.get("/curriculum/shs-tracks").body()
        } catch (e: Exception) {
            error.value = "Network Error: ${e.message}"
            // --- LOGGING ADDED ---
            Timber.e(e, "Failed to fetch SHS tracks.")
        }
    }

    fun fetchShsStrands() = viewModelScope.launch {
        // --- LOGGING ADDED ---
        Timber.i("Fetching SHS strands.")
        try {
            // ✅ UPDATED
            _shsStrands.value = client.get("/curriculum/shs-strands").body()
        } catch (e: Exception) {
            error.value = "Network Error: ${e.message}"
            // --- LOGGING ADDED ---
            Timber.e(e, "Failed to fetch SHS strands.")
        }
    }

    fun fetchSpecializations(gradeLevelId: Int? = null, strandId: Int? = null) = viewModelScope.launch {
        isLoading.value = true
        // --- LOGGING ADDED ---
        Timber.i("Fetching specializations for gradeLevelId: %s, strandId: %s", gradeLevelId, strandId)
        try {
            // ✅ UPDATED
            var url = "/curriculum/specializations"
            val params = mutableListOf<String>()
            if (gradeLevelId != null) params.add("gradeLevelId=$gradeLevelId")
            if (strandId != null) params.add("strandId=$strandId")
            if (params.isNotEmpty()) url += "?${params.joinToString("&")}"

            _specializations.value = client.get(url).body()
        } catch (e: Exception) {
            error.value = "Network Error: ${e.message}"
            // --- LOGGING ADDED ---
            Timber.e(e, "Failed to fetch specializations.")
        } finally {
            isLoading.value = false
        }
    }

    fun clearSpecializations() {
        // --- LOGGING ADDED ---
        Timber.d("Clearing specializations from state.")
        _specializations.value = emptyList()
    }

    fun fetchAllStudents() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching all students list.")
            try {
                // ✅ UPDATED
                _students.value = client.get("/students").body()
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Failed to fetch all students list.")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun addStudent(
        studentId: String,
        firstName: String,
        middleName: String?,
        lastName: String,
        password: String,
        gradeLevelId: Int,
        sectionId: Int?,
        strandId: Int?,
        specializationId: Int?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to add student with ID: %s", studentId)
            try {
                val requestBody = CreateStudentRequest(
                    studentId = studentId, firstName = firstName, middleName = middleName,
                    lastName = lastName, password = password, gradeLevelId = gradeLevelId,
                    sectionId = sectionId, strandId = strandId,
                    specializationId = specializationId
                )
                // ✅ UPDATED
                val response: HttpResponse = client.post("/students") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully added student with ID: %s", studentId)
                    onSuccess()
                    fetchAllStudents()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to add student with ID %s. Server error: %s", studentId, errorMessage.ifBlank { response.status.toString() })
                    onError(errorMessage.ifBlank { "Failed to add student." })
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during add student network call for ID: %s", studentId)
                onError("Network error: ${e.message}")
            }
        }
    }


    fun deleteStudent(studentId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to delete student with ID: %s", studentId)
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.delete("/students/$studentId")
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully deleted student with ID: %s", studentId)
                    onSuccess()
                    fetchAllStudents()
                } else {
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to delete student with ID %s. Server response: %s", studentId, response.status)
                    onError(response.bodyAsText())
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during delete student network call for ID: %s", studentId)
                onError("Network error: ${e.message}")
            }
        }
    }

    fun fetchStudentDetailsForEdit(studentId: String) {
        viewModelScope.launch {
            isLoading.value = true
            _studentDetails.value = null
            error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching student details for edit for ID: %s", studentId)
            try {
                // ✅ UPDATED
                _studentDetails.value = client.get("/students/$studentId").body()
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Failed to fetch student details for edit for ID: %s", studentId)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearStudentDetails() {
        // --- LOGGING ADDED ---
        Timber.d("Clearing student details for edit from state.")
        _studentDetails.value = null
    }

    fun fetchAdminStudentProfile(studentId: String) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching admin student profile for ID: %s", studentId)
            try {
                // ✅ UPDATED
                _adminStudentProfile.value = client.get("/students/admin-profile/$studentId").body()
            } catch (e: Exception) {
                error.value = "Network Error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Failed to fetch admin student profile for ID: %s", studentId)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearAdminStudentProfile() {
        // --- LOGGING ADDED ---
        Timber.d("Clearing admin student profile from state.")
        _adminStudentProfile.value = null
    }

    fun updateStudent(
        originalStudentId: String, updatedDetails: UpdateStudentRequest,
        onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to update student with original ID: %s", originalStudentId)
            try {
                // ✅ UPDATED
                val response: HttpResponse = client.put("/students/$originalStudentId") {
                    contentType(ContentType.Application.Json)
                    setBody(updatedDetails)
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully updated student with original ID: %s", originalStudentId)
                    onSuccess()
                    fetchAllStudents()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to update student with original ID %s. Server error: %s", originalStudentId, errorMessage.ifBlank { response.status.toString() })
                    onError(errorMessage.ifBlank { "Failed to update student." })
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during update student network call for ID: %s", originalStudentId)
                onError("Network error: ${e.message}")
            }
        }
    }

    fun updateClearanceStatus(
        isCleared: Boolean,
        item: ClearanceStatusItem,
        profile: AdminStudentProfile,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to update clearance status for student %s, requirement %s, to %s", profile.id, item.requirementName, isCleared)
            try {
                val requestBody = UpdateClearanceStatusRequest(
                    userId = profile.userId,
                    requirementId = item.requirementId,
                    schoolYear = profile.activeTerm.schoolYear,
                    term = profile.activeTerm.termNumber,
                    isCleared = isCleared
                )
                // ✅ UPDATED
                val response: HttpResponse = client.put("/students/clearance/status") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully updated clearance status for student %s", profile.id)
                    fetchAdminStudentProfile(profile.id)
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    val errorMessage = errorBody.substringAfter("error\":\"").substringBefore("\"")
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to update clearance status for student %s. Server error: %s", profile.id, errorMessage.ifBlank { response.status.toString() })
                    onError(errorMessage.ifBlank { "Failed to update status." })
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during update clearance status network call for student: %s", profile.id)
                onError("Network error: ${e.message}")
            }
        }
    }
}