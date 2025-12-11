package com.mnvths.schoolclearance.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnvths.schoolclearance.R
import com.mnvths.schoolclearance.viewmodel.AuthViewModel

// Define School Colors locally
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)

@Composable
fun LoginScreen(authViewModel: AuthViewModel, onLogin: (String, String) -> Unit) {
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Local state for validation
    var isIdError by remember { mutableStateOf(false) }
    var isPasswordError by remember { mutableStateOf(false) }

    val loginError by authViewModel.loginError
    val focusManager = LocalFocusManager.current

    fun validateAndLogin() {
        isIdError = loginId.isBlank()
        isPasswordError = password.isBlank()

        if (!isIdError && !isPasswordError) {
            focusManager.clearFocus()
            onLogin(loginId.trim(), password.trim())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // --- Branding Section ---
        Image(
            painter = painterResource(id = R.drawable.malasila),
            contentDescription = "Malasila Logo",
            modifier = Modifier.size(140.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Changed to generic "Welcome Back"
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SchoolBlue
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Made School Name Bigger and Bolder
        Text(
            text = "Malasila National Vocational & Technological High School",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold, // Bolder
                fontSize = 18.sp // Slightly larger
            ),
            color = Color.DarkGray, // Slightly darker for better readability
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // --- Input Section ---

        // Updated Label to support LRN (Student) and Username (Admin/Signatory)
        OutlinedTextField(
            value = loginId,
            onValueChange = {
                loginId = it
                isIdError = false
            },
            label = { Text("Student ID / Username") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = SchoolBlue
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isIdError,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SchoolBlue,
                focusedLabelColor = SchoolBlue,
                errorBorderColor = SchoolRed,
                errorLabelColor = SchoolRed
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                autoCorrect = false
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
            )
        )
        if (isIdError) {
            Text(
                text = "ID or Username is required", // Updated error message
                color = SchoolRed,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                isPasswordError = false
            },
            label = { Text("Password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = SchoolBlue
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isPasswordError,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SchoolBlue,
                focusedLabelColor = SchoolBlue,
                errorBorderColor = SchoolRed,
                errorLabelColor = SchoolRed
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { validateAndLogin() }
            ),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )
        if (isPasswordError) {
            Text(
                text = "Password is required",
                color = SchoolRed,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Action Section ---
        Button(
            onClick = { validateAndLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SchoolBlue,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "LOGIN",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                letterSpacing = 1.sp
            )
        }

        loginError?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SchoolRed.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    color = SchoolRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}