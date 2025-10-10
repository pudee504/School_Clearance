// file: viewmodel/AuthViewModel.kt
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
                    _loginError.value = "Login failed: Invalid credentials."
                    _isUserLoggedIn.value = false
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed: ${e.stackTraceToString()}")
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
            try {
                val response: HttpResponse = client.put("/api/users/change-password") {
                    contentType(ContentType.Application.Json)
                    setBody(ChangePasswordRequest(userId, oldPassword, newPassword))
                }
                if (response.status.isSuccess()) {
                    onSuccess()
                } else {
                    val errorBody = response.body<JsonObject>()
                    val errorMessage = errorBody["error"]?.jsonPrimitive?.content ?: "An unknown error occurred."
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                onError("Could not connect to the server.")
            }
        }
    }

    fun logout() {
        _loggedInUser.value = null
        _isUserLoggedIn.value = false
    }

}