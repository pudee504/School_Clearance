package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mnvths.schoolclearance.R
import com.mnvths.schoolclearance.data.ClearanceItem
import com.mnvths.schoolclearance.data.LoggedInUser
import com.mnvths.schoolclearance.data.StudentProfile
import com.mnvths.schoolclearance.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    student: StudentProfile,
    onSignOut: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }

    if (showPasswordDialog) {
        val userId = (authViewModel.loggedInUser.value as? LoggedInUser.StudentUser)?.student?.userId ?: -1

        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { oldPassword, newPassword ->
                if (userId != -1) {
                    authViewModel.changePassword(
                        userId = userId,
                        oldPassword = oldPassword,
                        newPassword = newPassword,
                        onSuccess = {
                            Toast.makeText(context, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                            showPasswordDialog = false
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Error: Could not identify user.", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            StudentDrawerContent(
                student = student,
                onChangePasswordClick = {
                    scope.launch { drawerState.close() }
                    showPasswordDialog = true
                },
                onSignOutClick = {
                    scope.launch { drawerState.close() }
                    onSignOut()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Clearance") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                        }
                    }
                )
            }
        ) { paddingValues ->
            StudentClearanceContent(
                modifier = Modifier.padding(paddingValues),
                student = student
            )
        }
    }
}

@Composable
private fun StudentClearanceContent(modifier: Modifier = Modifier, student: StudentProfile) {
    val schoolYear = student.clearanceStatus.firstOrNull()?.schoolYear ?: "N/A"
    val term = student.clearanceStatus.firstOrNull()?.let { "Quarter ${it.quarter}" } ?: "N/A"

    val subjects = remember(student.clearanceStatus) {
        student.clearanceStatus.filter { it.requirementType == "subject" }
    }
    val accounts = remember(student.clearanceStatus) {
        student.clearanceStatus.filter { it.requirementType == "account" }
    }

    val allCleared = remember(student.clearanceStatus) {
        student.clearanceStatus.isNotEmpty() && student.clearanceStatus.all { it.isCleared }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        StudentInfo(label = "Name", value = student.name)
        StudentInfo(label = "Student ID", value = student.id)
        StudentInfo(label = "Grade Level", value = student.gradeLevel ?: "N/A")
        StudentInfo(label = "Section", value = student.section ?: "N/A")
        StudentInfo(label = "School Year", value = schoolYear)
        StudentInfo(label = "Term", value = term)
        Spacer(modifier = Modifier.height(12.dp))
        Divider()

        if (student.clearanceStatus.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No clearance requirements found.")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (subjects.isNotEmpty()) {
                    item {
                        Text(
                            "Subjects",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(subjects, key = { "subject-" + (it.subjectName ?: it.hashCode()) }) { item ->
                        StudentClearanceRow(item = item)
                        Divider()
                    }
                }

                if (accounts.isNotEmpty()) {
                    item {
                        if (subjects.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp)); Divider()
                        }
                        Text(
                            "Accounts",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(accounts, key = { "account-" + (it.subjectName ?: it.hashCode()) }) { item ->
                        StudentClearanceRow(item = item)
                        Divider()
                    }
                }
            }
        }

        if (allCleared) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All Requirements Cleared",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF008000)
                )
            }
        }
    }
}

@Composable
private fun StudentDrawerContent(
    student: StudentProfile,
    onChangePasswordClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.malasila),
                contentDescription = "School Logo",
                modifier = Modifier.size(100.dp).padding(bottom = 16.dp)
            )
            Text(text = "Welcome,", style = MaterialTheme.typography.titleMedium)
            Text(
                text = student.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        Divider()
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Key, contentDescription = "Change Password") },
            label = { Text("Change Password") },
            selected = false,
            onClick = onChangePasswordClick
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") },
            label = { Text("Logout") },
            selected = false,
            onClick = onSignOutClick
        )
    }
}

@Composable
fun StudentClearanceRow(item: ClearanceItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 16.dp)
        ) {
            Text(
                text = item.subjectName ?: "Invalid Requirement",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = item.signatoryName ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (item.isCleared) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
            contentDescription = if (item.isCleared) "Cleared" else "Not Cleared",
            tint = if (item.isCleared) Color(0xFF008000) else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isCurrentPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    val isError = remember(newPassword, confirmPassword) {
        confirmPassword.isNotEmpty() && newPassword != confirmPassword
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = if (isCurrentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isCurrentPasswordVisible = !isCurrentPasswordVisible }) {
                            Icon(if (isCurrentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    }
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                            Icon(if (isNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    }
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    isError = isError,
                    supportingText = { if (isError) Text("Passwords do not match") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPassword, newPassword) },
                enabled = currentPassword.isNotEmpty() && newPassword.isNotEmpty() && !isError
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}