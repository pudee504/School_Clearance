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
import com.mnvths.schoolclearance.screen.AssignClassesToSubjectScreen
import com.mnvths.schoolclearance.screen.AssignSubjectToFacultyScreen
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
    val subjectName: String,
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
    val lastName: String
)

// NEW data class for a subject
@Serializable
data class Subject(
    val id: Int,
    val subjectName: String
)

// NEW data class for an assigned subject
@Serializable
data class AssignedSubject(
    val subjectId: Int,
    val subjectName: String
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
    val subjectId: Int,
    val sectionIds: List<Int>
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

// Updated ViewModel for Faculty to include editing functionality and assigned subjects
class FacultyViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private val _facultyList = mutableStateOf<List<FacultyMember>>(emptyList())
    val facultyList: State<List<FacultyMember>> = _facultyList

    private val _assignedSubjects = mutableStateOf<List<AssignedSubject>>(emptyList())
    val assignedSubjects: State<List<AssignedSubject>> = _assignedSubjects

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

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

    fun fetchAssignedSubjects(facultyId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/faculty-subjects/$facultyId")
                if (response.status.isSuccess()) {
                    val subjects: List<AssignedSubject> = response.body()
                    _assignedSubjects.value = subjects
                } else {
                    _error.value = "Failed to load assigned subjects."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // NEW function to edit a faculty member
    fun editFaculty(id: Int, firstName: String, lastName: String, middleName: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.put("http://10.0.2.2:3000/faculty/${id}") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("firstName" to firstName, "lastName" to lastName, "middleName" to middleName))
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

// NEW ViewModel to handle subjects and class assignment
class AssignmentViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private val _subjects = mutableStateOf<List<Subject>>(emptyList())
    val subjects: State<List<Subject>> = _subjects

    private val _sections = mutableStateOf<List<ClassSection>>(emptyList())
    val sections: State<List<ClassSection>> = _sections

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    // Fetches all subjects
    fun fetchSubjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/subjects")
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

    // NEW: Fetches all unique year and section combinations from the student table
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

    // NEW function to assign a subject to a faculty member
    // NEW function to confirm a subject selection for a faculty member
    fun assignSubjectToFaculty(
        facultyId: Int,
        subjectId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:3000/assign-subject") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "facultyId" to facultyId,
                            "subjectId" to subjectId
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    // ✅ Just confirm selection
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    onError("Failed to select subject: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    // In AssignmentViewModel
    fun fetchAssignedSections(
        facultyId: Int,
        subjectId: Int,
        onResult: (List<ClassSection>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/assigned-sections") {
                    parameter("facultyId", facultyId)
                    parameter("subjectId", subjectId)
                }
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
        subjectId: Int,
        sectionIds: List<Int>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:3000/assign-classes") {
                    contentType(ContentType.Application.Json)
                    setBody(AssignClassesRequest(facultyId, subjectId, sectionIds))
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






    // NEW function to assign a class to a faculty member for a specific subject
    fun assignClassToFaculty(
        facultyId: Int,
        subjectId: Int,
        sectionId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:3000/assign-class") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "facultyId" to facultyId,
                            "subjectId" to subjectId,
                            "sectionId" to sectionId
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                } else {
                    val errorBody = response.bodyAsText()
                    onError("Failed to assign class: ${response.status.description}. Details: $errorBody")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }
}


// -----------------------------------------------------------------------------
// Main Application and Navigation
// -----------------------------------------------------------------------------

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
// -----------------------------------------------------------------------------
// Composable Screens
// -----------------------------------------------------------------------------





