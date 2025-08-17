package com.mnvths.schoolclearance

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mnvths.schoolclearance.screen.AdminDashboard
import com.mnvths.schoolclearance.screen.AssignClassesToSignatoryScreen
import com.mnvths.schoolclearance.screen.AssignSignatoryToFacultyScreen
import com.mnvths.schoolclearance.screen.EditFacultyScreen
import com.mnvths.schoolclearance.screen.FacultyDetailsScreen
import com.mnvths.schoolclearance.screen.LoginScreen
import com.mnvths.schoolclearance.screen.StudentDetailScreen
import com.mnvths.schoolclearance.ui.theme.SchoolClearanceTheme
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

// -----------------------------------------------------------------------------
// Data Classes to Match the NEW Server Response
// -----------------------------------------------------------------------------

@Serializable
data class ClearanceItem(
    val signatoryName: String,
    val schoolYear: String,
    val quarter: Int,
    val isCleared: Boolean
)

@Serializable
data class Student(
    val id: String,
    val name: String,
    val role: String,
    val gradeLevel: String,
    val section: String,
    val clearanceStatus: List<ClearanceItem>
)

@Serializable
data class OtherUser(
    val id: Int,
    val name: String,
    val role: String
)

sealed class LoggedInUser {
    data class StudentUser(val student: Student) : LoggedInUser()
    data class FacultyAdminUser(val user: OtherUser) : LoggedInUser()
}

// NEW data class for a faculty member
@Serializable
data class FacultyMember(
    val id: Int,
    val name: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val username: String
)

// NEW data class for a signatory
@Serializable
data class Signatory(
    val id: Int,
    val signatoryName: String
)

// NEW data class for an assigned signatory
@Serializable
data class AssignedSignatory(
    val signatoryId: Int,
    val signatoryName: String
)

// NEW data class for a class section
@Serializable
data class ClassSection(
    val sectionId: Int,
    val gradeLevel: String,
    val sectionName: String
)

@Serializable
data class AssignClassesRequest(
    val facultyId: Int,
    val signatoryId: Int,
    val sectionIds: List<Int>
)

@Serializable
data class AddSectionRequest(val gradeLevel: String, val sectionName: String)

@Serializable
data class StudentListItem(
    val student_id: String,
    val name: String
)

@Serializable
data class AddStudentRequest(
    val studentId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val sectionId: Int
)



// -----------------------------------------------------------------------------
// ViewModels
// -----------------------------------------------------------------------------

class AuthViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val _loggedInUser = mutableStateOf<LoggedInUser?>(null)
    val loggedInUser: State<LoggedInUser?> = _loggedInUser

    private val _isUserLoggedIn = mutableStateOf(false)
    val isUserLoggedIn: State<Boolean> = _isUserLoggedIn

    private val _loginError = mutableStateOf<String?>(null)
    val loginError: State<String?> = _loginError

    fun login(loginId: String, password: String) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:3000/login") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("loginId" to loginId, "password" to password))
                }

                if (response.status.isSuccess()) {
                    val responseText = response.bodyAsText()
                    val json = Json { ignoreUnknownKeys = true; isLenient = true }
                    val jsonObject = json.decodeFromString<JsonObject>(responseText)
                    val role = jsonObject["role"]?.jsonPrimitive?.content

                    if (role == "student") {
                        val student = json.decodeFromString<Student>(responseText)
                        _loggedInUser.value = LoggedInUser.StudentUser(student)
                    } else {
                        val otherUser = json.decodeFromString<OtherUser>(responseText)
                        _loggedInUser.value = LoggedInUser.FacultyAdminUser(otherUser)
                    }
                    _isUserLoggedIn.value = true
                    _loginError.value = null
                } else {
                    _loginError.value = "Login failed: Invalid credentials."
                    _isUserLoggedIn.value = false
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed: ${e.stackTraceToString()}")
                _loginError.value = "An error occurred: ${e.message}"
                _isUserLoggedIn.value = false
            }
        }
    }

    fun logout() {
        _loggedInUser.value = null
        _isUserLoggedIn.value = false
    }
}

// Updated ViewModel for Faculty to include editing functionality and assigned signatories
class FacultyViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private val _facultyList = mutableStateOf<List<FacultyMember>>(emptyList())
    val facultyList: State<List<FacultyMember>> = _facultyList

    private val _assignedSignatories = mutableStateOf<List<AssignedSignatory>>(emptyList())
    val assignedSignatories: State<List<AssignedSignatory>> = _assignedSignatories

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _assignedSections = mutableStateOf<Map<Int, List<ClassSection>>>(emptyMap())
    val assignedSections: State<Map<Int, List<ClassSection>>> = _assignedSections

    fun fetchFacultyList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/faculty")
                if (response.status.isSuccess()) {
                    val facultyData: List<FacultyMember> = response.body()
                    _facultyList.value = facultyData
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

    fun fetchAssignedSignatories(facultyId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/faculty-signatory/$facultyId")
                if (response.status.isSuccess()) {
                    val signatories: List<AssignedSignatory> = response.body()
                    _assignedSignatories.value = signatories
                    _assignedSections.value = emptyMap()
                } else {
                    _error.value = "Failed to load assigned signatories."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAssignedSections(facultyId: Int, signatoryId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response: HttpResponse = client.get("http://10.0.2.2:3000/faculty-signatory-sections/$facultyId/$signatoryId")
                if (response.status.isSuccess()) {
                    val sections: List<ClassSection> = response.body()
                    _assignedSections.value = _assignedSections.value + (signatoryId to sections)
                } else {
                    // Handle error case
                }
            } catch (e: Exception) {
                // Handle network error
            } finally {
                _isLoading.value = false
            }
        }
    }

    // New function to handle the deletion of a signatory assignment
    fun deleteAssignedSignatory(facultyId: Int, signatoryId: Int) {
        viewModelScope.launch {
            try {
                Log.d("FacultyViewModel", "Attempting to delete signatory $signatoryId for faculty $facultyId")
                val response: HttpResponse = client.delete("http://10.0.2.2:3000/faculty-signatory/$facultyId/$signatoryId")
                if (response.status.isSuccess()) {
                    // Refresh the list of assigned signatories after a successful deletion
                    fetchAssignedSignatories(facultyId)
                } else {
                    _error.value = "Failed to delete signatory assignment."
                    Log.e("FacultyViewModel", "Deletion failed: ${response.status.description}")
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                Log.e("FacultyViewModel", "Deletion network error: ${e.stackTraceToString()}")
            }
        }
    }

    // New function to add a faculty member
    fun addFaculty(
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
                val response: HttpResponse = client.post("http://10.0.2.2:3000/faculty") {
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
                    fetchFacultyList() // Refresh the list after adding a new user
                } else {
                    val errorBody = response.bodyAsText()
                    onError("Failed to add faculty: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    fun deleteFaculty(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.delete("http://10.0.2.2:3000/faculty/$id")
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchFacultyList() // Refresh the list
                } else {
                    onError("Failed to delete faculty: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
    fun editFaculty(
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

                val response: HttpResponse = client.put("http://10.0.2.2:3000/faculty/${id}") {
                    contentType(ContentType.Application.Json)
                    setBody(bodyMap)
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                    fetchFacultyList() // Refresh the list
                } else {
                    onError("Failed to update faculty: ${response.status.description}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}


    class AssignmentViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SchoolClearanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}