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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

// 1. Data Model: A class that exactly matches the JSON response from your server.
data class Student(
    val id: Int,
    val name: String,
    val yearLevel: Int,
    val section: String,
    val clearanceStatus: List<ClearanceItem>
)

// A data class for each department/item to be cleared.
data class ClearanceItem(
    val department: String,
    val isCleared: Boolean
)

// 2. ViewModel for Authentication (now the single source of truth)
class AuthViewModel : ViewModel() {
    private val _isUserLoggedIn = mutableStateOf(false)
    val isUserLoggedIn: State<Boolean> = _isUserLoggedIn
    private val _loggedInStudent = mutableStateOf<Student?>(null)
    val loggedInStudent: State<Student?> = _loggedInStudent

    // OkHttp client for network requests
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    fun signInWithStudentId(
        studentId: String,
        password: String,
        onComplete: (Boolean, Student?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val jsonObject = JSONObject().apply {
                put("studentId", studentId.toInt())
                put("password", password)
            }
            val body = jsonObject.toString().toRequestBody(JSON)

            // IMPORTANT: If using an Android emulator, this IP is correct.
            // For a real device, you need to replace "10.0.2.2" with your computer's local IP address.
            val request = Request.Builder()
                .url("http://10.0.2.2:3000/login")
                .post(body)
                .build()

            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful && response.body != null) {
                    val responseBody = response.body.string()
                    // Use Gson to parse the JSON response from the server into our Student data class.
                    val student = gson.fromJson(responseBody, Student::class.java)

                    withContext(Dispatchers.Main) {
                        _isUserLoggedIn.value = true
                        _loggedInStudent.value = student
                        onComplete(true, student)
                    }
                    Log.d("AuthViewModel", "Custom sign-in successful.")
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(false, null)
                        Log.e("AuthViewModel", "Server login failed: ${response.code} - ${response.message}")
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    onComplete(false, null)
                    Log.e("AuthViewModel", "Network request failed: ${e.message}")
                }
            }
        }
    }

    fun signOut() {
        _isUserLoggedIn.value = false
        _loggedInStudent.value = null
    }
}

// 3. Main Composable function for the entire app.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SchoolClearanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        // This is the key change: always start at a static route.
                        startDestination = "login"
                    ) {
                        composable("login") {
                            // Use LaunchedEffect to check for login status and redirect immediately.
                            // This ensures the user doesn't see the login screen if already authenticated.
                            LaunchedEffect(authViewModel.isUserLoggedIn.value) {
                                if (authViewModel.isUserLoggedIn.value && authViewModel.loggedInStudent.value != null) {
                                    val studentId = authViewModel.loggedInStudent.value!!.id
                                    navController.navigate("studentDetail/$studentId") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            }

                            LoginScreen(
                                authViewModel = authViewModel,
                                onLoginSuccess = { student ->
                                    student?.let {
                                        navController.navigate("studentDetail/${it.id}") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "studentDetail/{studentId}",
                            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val studentId = backStackEntry.arguments?.getString("studentId")?.toIntOrNull()

                            if (studentId != null && authViewModel.loggedInStudent.value?.id == studentId) {
                                StudentDetailScreen(
                                    student = authViewModel.loggedInStudent.value!!,
                                    onSignOut = {
                                        authViewModel.signOut()
                                        navController.navigate("login") {
                                            // The popUpTo should reference the login screen
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.navigate("login") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                                Text("Redirecting to login...")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. Login Screen Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(authViewModel: AuthViewModel, onLoginSuccess: (Student?) -> Unit) {
    var studentId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "School Clearance Login",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        OutlinedTextField(
            value = studentId,
            onValueChange = { studentId = it },
            label = { Text("Student ID") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                authViewModel.signInWithStudentId(studentId, password) { success, student ->
                    if (success) {
                        Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess(student)
                    } else {
                        Toast.makeText(context, "Login Failed. Please check your credentials.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
    }
}

// 5. Composable for the student detail screen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(student: Student, onSignOut: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clearance Details") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = student.name, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Year Level: ${student.yearLevel}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Clearance Status:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            // Display clearance status using a LazyColumn for efficiency
            LazyColumn {
                items(student.clearanceStatus) { item ->
                    ClearanceStatusItem(item)
                }
            }
        }
    }
}

// 6. Composable for a single clearance item's status.
@Composable
fun ClearanceStatusItem(item: ClearanceItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = item.department, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (item.isCleared) "Cleared" else "Not Cleared",
            color = if (item.isCleared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// 7. Preview for the login screen.
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    SchoolClearanceTheme {
        LoginScreen(
            authViewModel = viewModel(),
            onLoginSuccess = {}
        )
    }
}
