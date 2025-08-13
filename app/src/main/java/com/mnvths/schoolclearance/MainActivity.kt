package com.mnvths.schoolclearance

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mnvths.schoolclearance.ui.theme.SchoolClearanceTheme
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

// -----------------------------------------------------------------------------
// Data Classes to Match the NEW Server Response
// -----------------------------------------------------------------------------

// Data class for student clearance status
@Serializable
data class ClearanceItem(
    val subjectName: String,
    val schoolYear: String,
    val quarter: Int,
    val isCleared: Boolean
)

// Data class for a student user's full profile
@Serializable
data class Student(
    val id: String,
    val name: String,
    val role: String,
    val yearLevel: Int,
    val section: String,
    val clearanceStatus: List<ClearanceItem>
)

// Data class for a faculty or admin user's basic info
@Serializable
data class OtherUser(
    val id: Int,
    val name: String,
    val role: String
)

// Sealed class to represent the two possible user types
sealed class LoggedInUser {
    data class StudentUser(val student: Student) : LoggedInUser()
    data class FacultyAdminUser(val user: OtherUser) : LoggedInUser()
}

// -----------------------------------------------------------------------------
// AuthViewModel: Handles Login Logic
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
                        _isUserLoggedIn.value = true
                    } else {
                        val otherUser = json.decodeFromString<OtherUser>(responseText)
                        _loggedInUser.value = LoggedInUser.FacultyAdminUser(otherUser)
                        _isUserLoggedIn.value = true
                    }
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

// -----------------------------------------------------------------------------
// Main Application
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

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()

    // This effect listens for changes in the loggedInUser state and navigates accordingly.
    LaunchedEffect(authViewModel.loggedInUser.value) {
        authViewModel.loggedInUser.value?.let { user ->
            when (user) {
                is LoggedInUser.StudentUser -> navController.navigate("studentDetail") {
                    popUpTo("login") { inclusive = true }
                }
                is LoggedInUser.FacultyAdminUser -> {
                    when (user.user.role) {
                        "faculty" -> navController.navigate("facultyDashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                        "admin" -> navController.navigate("adminDashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                        else -> {
                            navController.navigate("login")
                        }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLogin = { loginId, password ->
                    authViewModel.login(loginId, password)
                }
            )
        }

        composable("studentDetail") {
            val student = (authViewModel.loggedInUser.value as? LoggedInUser.StudentUser)?.student
            if (student != null) {
                StudentDetailScreen(student = student, onSignOut = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                })
            }
        }

        composable("facultyDashboard") {
            val user = (authViewModel.loggedInUser.value as? LoggedInUser.FacultyAdminUser)?.user
            if (user != null) {
                FacultyDashboard(user = user, onSignOut = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                })
            }
        }

        composable("adminDashboard") {
            val user = (authViewModel.loggedInUser.value as? LoggedInUser.FacultyAdminUser)?.user
            if (user != null) {
                AdminDashboard(user = user, onSignOut = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                })
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Composable Screens
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(authViewModel: AuthViewModel, onLogin: (String, String) -> Unit) {
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginError by authViewModel.loginError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = loginId,
            onValueChange = { loginId = it },
            label = { Text("Student ID / Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onLogin(loginId, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        loginError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(student: Student, onSignOut: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Clearance Status") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        val schoolYear = student.clearanceStatus.firstOrNull()?.schoolYear ?: "N/A"
        val quarter = student.clearanceStatus.firstOrNull()?.quarter?.toString() ?: "N/A"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Name: ${student.name}", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Student ID: ${student.id}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Year Level: ${student.yearLevel}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Section: ${student.section}", style = MaterialTheme.typography.bodyLarge)

            Text(text = "School Year: $schoolYear", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Quarter: $quarter", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Clearance Status:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(student.clearanceStatus) { item ->
                    ClearanceStatusItem(
                        subjectName = item.subjectName,
                        isCleared = item.isCleared
                    )
                }
            }
        }
    }
}

@Composable
fun ClearanceStatusItem(subjectName: String, isCleared: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCleared) Color.Green.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = subjectName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isCleared) "Cleared" else "Not Cleared",
                color = if (isCleared) Color.Green else Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun FacultyDashboard(user: OtherUser, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Welcome, Faculty: ${user.name}", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You are logged in with a Faculty account.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(user: OtherUser, onSignOut: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Faculty", "Students", "Subjects")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            when (selectedTabIndex) {
                0 -> FacultyListScreen()
                1 -> StudentListScreen()
                2 -> SubjectListScreen()
            }
        }
    }
}

// Placeholder for the Faculty tab content
@Composable
fun FacultyListScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Faculty Management Screen (coming soon!)", style = MaterialTheme.typography.titleLarge)
    }
}

// Placeholder for the Students tab content
@Composable
fun StudentListScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Student Management Screen (coming soon!)", style = MaterialTheme.typography.titleLarge)
    }
}

// Placeholder for the Subjects tab content
@Composable
fun SubjectListScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Subject Management Screen (coming soon!)", style = MaterialTheme.typography.titleLarge)
    }
}
