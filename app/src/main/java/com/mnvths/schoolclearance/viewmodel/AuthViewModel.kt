package com.mnvths.schoolclearance.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.ChangePasswordRequest
import com.mnvths.schoolclearance.data.LoggedInUser
import com.mnvths.schoolclearance.data.LogoutRequest
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

// ✅ MODIFIED: Inherit from AndroidViewModel to get application context
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val client = KtorClient.httpClient

    private val _loggedInUser = mutableStateOf<LoggedInUser?>(null)
    val loggedInUser: State<LoggedInUser?> = _loggedInUser

    private val _isUserLoggedIn = mutableStateOf(false)
    val isUserLoggedIn: State<Boolean> = _isUserLoggedIn

    private val _loginError = mutableStateOf<String?>(null)
    val loginError: State<String?> = _loginError

    // ✅ NEW: JSON parser for storing user data
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ✅ NEW: SharedPreferences for session persistence
    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // ✅ NEW: This block runs when the ViewModel is first created
    init {
        checkUserLoggedIn()
    }

    private fun checkUserLoggedIn() {
        // Read from storage
        val userJson = sharedPreferences.getString("logged_in_user", null)
        if (userJson != null) {
            try {
                // If user data exists, parse it and update the state
                val jsonObject = json.decodeFromString<JsonObject>(userJson)
                val role = jsonObject["role"]?.jsonPrimitive?.content
                if (role == "student") {
                    val student = json.decodeFromString<StudentProfile>(userJson)
                    _loggedInUser.value = LoggedInUser.StudentUser(student)
                } else {
                    val otherUser = json.decodeFromString<OtherUser>(userJson)
                    _loggedInUser.value = LoggedInUser.FacultyAdminUser(otherUser)
                }
                _isUserLoggedIn.value = true
                Timber.i("Restored session for user with role: %s", role)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse stored user data.")
                // If parsing fails, clear the bad data
                logout()
            }
        } else {
            _isUserLoggedIn.value = false
        }
    }


    fun login(loginId: String, password: String) {
        viewModelScope.launch {
            _loginError.value = null
            Timber.i("Attempting to log in user: %s", loginId)
            try {
                val response: HttpResponse = client.post("/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("loginId" to loginId, "password" to password))
                }

                if (response.status.isSuccess()) {
                    val responseText = response.bodyAsText()
                    val jsonObject = json.decodeFromString<JsonObject>(responseText)
                    val role = jsonObject["role"]?.jsonPrimitive?.content

                    Timber.i("Login successful for user '%s' with role: %s", loginId, role)

                    // ✅ NEW: Save user data to SharedPreferences on successful login
                    with(sharedPreferences.edit()) {
                        putString("logged_in_user", responseText)
                        apply()
                    }

                    // This part remains the same
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
                    Timber.w("Login failed for user '%s'. Server responded with status: %s", loginId, response.status)
                    _loginError.value = "Login failed: Invalid credentials."
                    _isUserLoggedIn.value = false
                }
            } catch (e: Exception) {
                Timber.e(e, "Login network request failed for user '%s'", loginId)
                _loginError.value = "Unable to connect to the server. Please check your network."
                _isUserLoggedIn.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val userToLogOut = loggedInUser.value
                val userId = when (userToLogOut) {
                    is LoggedInUser.StudentUser -> userToLogOut.student.userId
                    is LoggedInUser.FacultyAdminUser -> userToLogOut.user.id
                    else -> null
                }
                val username = when (userToLogOut) {
                    is LoggedInUser.StudentUser -> userToLogOut.student.id
                    is LoggedInUser.FacultyAdminUser -> userToLogOut.user.username
                    else -> null
                }

                if (userId != null) {
                    Timber.i("Notifying server of logout for user ID: %s", userId)

                    // ✅ 1. Create an instance of your new data class
                    val logoutPayload = LogoutRequest(userId = userId, username = username)

                    client.post("/auth/logout") {
                        contentType(ContentType.Application.Json)
                        // ✅ 2. Set the body to your new type-safe object
                        setBody(logoutPayload)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to notify server of logout. Logging out locally anyway.")
            } finally {
                // ✅ NEW: Clear the user data from SharedPreferences on logout
                with(sharedPreferences.edit()) {
                    remove("logged_in_user")
                    apply()
                }

                // Reset the in-memory state
                Timber.i("User logged out locally and session cleared.")
                _loggedInUser.value = null
                _isUserLoggedIn.value = false
            }
        }
    }

    // changePassword function remains unchanged
    fun changePassword(
        userId: Int,
        oldPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            Timber.i("Attempting to change password for userId: %d", userId)
            try {
                val response: HttpResponse = client.put("/users/change-password") {
                    contentType(ContentType.Application.Json)
                    setBody(ChangePasswordRequest(userId, oldPassword, newPassword))
                }
                if (response.status.isSuccess()) {
                    Timber.i("Password changed successfully for userId: %d", userId)
                    onSuccess()
                } else {
                    val errorBody = response.body<JsonObject>()
                    val errorMessage = errorBody["error"]?.jsonPrimitive?.content ?: "An unknown error occurred."
                    Timber.w("Failed to change password for userId %d. Server error: %s", userId, errorMessage)
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Change password network request failed for userId: %d", userId)
                onError("Could not connect to the server.")
            }
        }
    }
}