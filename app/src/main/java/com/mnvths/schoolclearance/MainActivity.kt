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
    val yearLevel: Int,
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

// NEW data class for a class section
@Serializable
data class ClassSection(
    val yearLevel: Int,
    val section: String
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

// Updated ViewModel for Faculty to include editing functionality
class FacultyViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val _facultyList = mutableStateOf<List<FacultyMember>>(emptyList())
    val facultyList: State<List<FacultyMember>> = _facultyList

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
            json(Json { ignoreUnknownKeys = true })
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

    // Fetches all sections for a given subject
    fun fetchSectionsForSubject(subjectId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: HttpResponse = client.get("http://10.0.2.2:3000/class-sections/$subjectId")
                if (response.status.isSuccess()) {
                    _sections.value = response.body()
                } else {
                    _error.value = "Failed to load sections."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // NEW function to assign a class to a faculty member
    fun assignClassToFaculty(facultyId: Int, subjectId: Int, yearLevel: Int, section: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("http://10.0.2.2:3000/assign-class") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("facultyId" to facultyId, "subjectId" to subjectId, "yearLevel" to yearLevel, "section" to section))
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

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()

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
                AdminDashboard(
                    user = user,
                    onSignOut = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    navController = navController
                )
            }
        }

        // NEW navigation routes for managing faculty
        composable(
            route = "editFaculty/{facultyId}/{facultyName}/{firstName}/{lastName}/{middleName}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType },
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType },
                navArgument("middleName") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val name = backStackEntry.arguments?.getString("facultyName") ?: ""
            val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
            val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
            val middleName = backStackEntry.arguments?.getString("middleName")
            EditFacultyScreen(
                navController = navController,
                facultyId = id,
                facultyName = name,
                firstName = firstName,
                lastName = lastName,
                middleName = middleName
            )
        }
        composable(
            route = "assignSubject/{facultyId}/{facultyName}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val name = backStackEntry.arguments?.getString("facultyName") ?: ""
            AssignSubjectScreen(navController = navController, facultyId = id, facultyName = name)
        }
        composable(
            route = "assignSection/{facultyId}/{facultyName}/{subjectId}/{subjectName}",
            arguments = listOf(
                navArgument("facultyId") { type = NavType.IntType },
                navArgument("facultyName") { type = NavType.StringType },
                navArgument("subjectId") { type = NavType.IntType },
                navArgument("subjectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val facultyId = backStackEntry.arguments?.getInt("facultyId") ?: return@composable
            val facultyName = backStackEntry.arguments?.getString("facultyName") ?: ""
            val subjectId = backStackEntry.arguments?.getInt("subjectId") ?: return@composable
            val subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
            AssignSectionScreen(
                navController = navController,
                facultyId = facultyId,
                facultyName = facultyName,
                subjectId = subjectId,
                subjectName = subjectName
            )
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "School Clearance", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
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
fun AdminDashboard(user: OtherUser, onSignOut: () -> Unit, navController: NavController) {
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
                0 -> FacultyListScreen(navController = navController)
                1 -> StudentListScreen()
                2 -> SubjectListScreen()
            }
        }
    }
}

// Updated Faculty List Screen with new buttons
@Composable
fun FacultyListScreen(
    viewModel: FacultyViewModel = viewModel(),
    navController: NavController
) {
    val facultyList by viewModel.facultyList
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(Unit) {
        viewModel.fetchFacultyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: $error\nPlease check your server and network connection.",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(facultyList) { faculty ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "ID: ${faculty.id}", style = MaterialTheme.typography.bodySmall)
                                Text(text = faculty.name, style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // NEW: Edit Button
                            IconButton(onClick = {
                                val middleName = faculty.middleName ?: "null"
                                navController.navigate("editFaculty/${faculty.id}/${faculty.name}/${faculty.firstName}/${faculty.lastName}/${middleName}")
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Faculty")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // NEW: Assign Subject Button
                            Button(onClick = {
                                navController.navigate("assignSubject/${faculty.id}/${faculty.name}")
                            }) {
                                Text("Assign Subject")
                            }
                        }
                    }
                }
            }
        }
    }
}

// NEW: Screen to edit a faculty member's information
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFacultyScreen(
    navController: NavController,
    facultyId: Int,
    facultyName: String,
    firstName: String,
    lastName: String,
    middleName: String?,
    viewModel: FacultyViewModel = viewModel()
) {
    val context = LocalContext.current
    var newFirstName by remember { mutableStateOf(firstName) }
    var newLastName by remember { mutableStateOf(lastName) }
    var newMiddleName by remember { mutableStateOf(middleName ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit: $facultyName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = newFirstName,
                onValueChange = { newFirstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newMiddleName,
                onValueChange = { newMiddleName = it },
                label = { Text("Middle Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newLastName,
                onValueChange = { newLastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.editFaculty(
                        id = facultyId,
                        firstName = newFirstName,
                        lastName = newLastName,
                        middleName = newMiddleName.ifBlank { null },
                        onSuccess = {
                            Toast.makeText(context, "Faculty updated successfully!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}

// NEW: Screen to assign a subject to a faculty member
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSubjectScreen(
    navController: NavController,
    facultyId: Int,
    facultyName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val subjects by viewModel.subjects
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(Unit) {
        viewModel.fetchSubjects()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Subject to $facultyName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(text = "Error: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subjects) { subject ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = subject.subjectName, style = MaterialTheme.typography.titleLarge)
                                Button(onClick = {
                                    navController.navigate("assignSection/$facultyId/$facultyName/${subject.id}/${subject.subjectName}")
                                }) {
                                    Text("Assign Class")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// NEW: Screen to assign a class section to a faculty member
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSectionScreen(
    navController: NavController,
    facultyId: Int,
    facultyName: String,
    subjectId: Int,
    subjectName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    var yearLevel by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Section for $subjectName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Assigning to: $facultyName", style = MaterialTheme.typography.titleMedium)
            Text("Subject: $subjectName", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = yearLevel,
                onValueChange = { yearLevel = it },
                label = { Text("Year Level") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = section,
                onValueChange = { section = it },
                label = { Text("Section") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (yearLevel.isNotBlank() && section.isNotBlank()) {
                        viewModel.assignClassToFaculty(
                            facultyId = facultyId,
                            subjectId = subjectId,
                            yearLevel = yearLevel.toInt(),
                            section = section,
                            onSuccess = {
                                Toast.makeText(context, "Class assigned successfully!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack(route = "adminDashboard", inclusive = false)
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Assign Class to Faculty")
            }
        }
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
