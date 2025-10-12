package com.mnvths.schoolclearance.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.ChangePasswordRequest
import com.mnvths.schoolclearance.data.LoggedInUser
import com.mnvths.schoolclearance.data.OtherUser
import com.mnvths.schoolclearance.data.StudentProfile
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber // --- LOGGING ADDED ---

class AuthViewModel : ViewModel() {
    // This client is now configured with the dynamic base URL from ApiConfig
    private val client = KtorClient.httpClient

    private val _loggedInUser = mutableStateOf<LoggedInUser?>(null)
    val loggedInUser: State<LoggedInUser?> = _loggedInUser

    private val _isUserLoggedIn = mutableStateOf(false)
    val isUserLoggedIn: State<Boolean> = _isUserLoggedIn

    private val _loginError = mutableStateOf<String?>(null)
    val loginError: State<String?> = _loginError

    fun login(loginId: String, password: String) {
        viewModelScope.launch {
            _loginError.value = null // Clear previous errors
            // --- LOGGING ADDED ---
            Timber.i("Attempting to log in user: %s", loginId)
            try {
                // ✅ UPDATED: The URL is now just the endpoint path.
                // The base URL (http://<IP>:<PORT>) is added automatically by KtorClient.
                val response: HttpResponse = client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("loginId" to loginId, "password" to password))
                }

                if (response.status.isSuccess()) {
                    val responseText = response.bodyAsText()
                    val json = Json { ignoreUnknownKeys = true; isLenient = true }
                    val jsonObject = json.decodeFromString<JsonObject>(responseText)
                    val role = jsonObject["role"]?.jsonPrimitive?.content

                    // --- LOGGING ADDED ---
                    Timber.i("Login successful for user '%s' with role: %s", loginId, role)

                    if (role == "student") {
                        val student = json.decodeFromString<StudentProfile>(responseText)
                        _loggedInUser.value = LoggedInUser.StudentUser(student)
                    } else {
                        val otherUser = json.decodeFromString<OtherUser>(responseText)
                        _loggedInUser.value = LoggedInUser.FacultyAdminUser(otherUser)
                    }
                    _isUserLoggedIn.value = true
                    _loginError.value = null
                } else {
                    // --- LOGGING ADDED ---
                    Timber.w("Login failed for user '%s'. Server responded with status: %s", loginId, response.status)
                    _loginError.value = "Login failed: Invalid credentials."
                    _isUserLoggedIn.value = false
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Login network request failed for user '%s'", loginId)
                // Provide a more user-friendly error for network issues
                _loginError.value = "Unable to connect to the server. Please check your network."
                _isUserLoggedIn.value = false
            }
        }
    }

    fun changePassword(
        userId: Int,
        oldPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to change password for userId: %d", userId)
            try {
                val response: HttpResponse = client.put("/api/users/change-password") {
                    contentType(ContentType.Application.Json)
                    setBody(ChangePasswordRequest(userId, oldPassword, newPassword))
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Password changed successfully for userId: %d", userId)
                    onSuccess()
                } else {
                    val errorBody = response.body<JsonObject>()
                    val errorMessage = errorBody["error"]?.jsonPrimitive?.content ?: "An unknown error occurred."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to change password for userId %d. Server error: %s", userId, errorMessage)
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Change password network request failed for userId: %d", userId)
                onError("Could not connect to the server.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // --- ADD THIS API CALL ---
                // We'll try to inform the server about the logout.
                val userToLogOut = loggedInUser.value
                val userId = when(userToLogOut) {
                    is LoggedInUser.StudentUser -> userToLogOut.student.userId
                    is LoggedInUser.FacultyAdminUser -> userToLogOut.user.id
                    else -> null
                }
                val username = when(userToLogOut) {
                    is LoggedInUser.StudentUser -> userToLogOut.student.id
                    is LoggedInUser.FacultyAdminUser -> userToLogOut.user.username
                    else -> null
                }

                if (userId != null) {
                    Timber.i("Notifying server of logout for user ID: %s", userId)
                    client.post("/auth/logout") {
                        contentType(ContentType.Application.Json)
                        setBody(mapOf("userId" to userId, "username" to username))
                    }
                }
            } catch (e: Exception) {
                // Log the error, but don't stop the user from logging out locally.
                Timber.e(e, "Failed to notify server of logout. Logging out locally anyway.")
            } finally {
                // --- THIS IS YOUR ORIGINAL LOGIC ---
                // It now runs after the API call attempts.
                Timber.i("User logged out locally.")
                _loggedInUser.value = null
                _isUserLoggedIn.value = false
            }
        }
    }

}