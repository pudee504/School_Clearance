package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mnvths.schoolclearance.R
import com.mnvths.schoolclearance.data.ClearanceItem
import com.mnvths.schoolclearance.data.LoggedInUser
import com.mnvths.schoolclearance.data.StudentProfile
import com.mnvths.schoolclearance.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val SuccessGreen = Color(0xFF2E7D32)
private val BackgroundGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    student: StudentProfile,
    onSignOut: () -> Unit,
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
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
                CenterAlignedTopAppBar(
                    title = { Text("My Clearance", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = SchoolBlue,
                        navigationIconContentColor = SchoolBlue
                    )
                )
            },
            containerColor = BackgroundGray
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.padding(paddingValues)
            ) {
                StudentClearanceContent(
                    student = student
                )
            }
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

    val totalItems = student.clearanceStatus.size
    val clearedItems = student.clearanceStatus.count { it.isCleared }
    val progress = if (totalItems > 0) clearedItems.toFloat() / totalItems else 0f

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StudentViewProfileCard(student = student, schoolYear = schoolYear, term = term)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Clearance Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = if(progress == 1f) SuccessGreen else SchoolBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = if(progress == 1f) SuccessGreen else SchoolBlue,
                        trackColor = Color(0xFFE0E0E0),
                    )
                }
            }
        }

        if (subjects.isNotEmpty()) {
            item { StudentSectionHeader("Academic Subjects") }
            // ✅ CRASH FIX: Removed the 'key' parameter.
            // Duplicate keys (same subject name) were causing the crash on scroll.
            items(subjects) { item ->
                StudentClearanceCard(item = item)
            }
        }

        if (accounts.isNotEmpty()) {
            item { StudentSectionHeader("Administrative Requirements") }
            // ✅ CRASH FIX: Removed the 'key' parameter here too.
            items(accounts) { item ->
                StudentClearanceCard(item = item)
            }
        }

        if (progress == 1f) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Verified, null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Congratulations!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "You are fully cleared for this term.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SuccessGreen
                        )
                    }
                }
            }
        } else {
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StudentViewProfileCard(student: StudentProfile, schoolYear: String, term: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SchoolBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                //
                Text(
                    text = "${student.name.split(" ").firstOrNull()?.take(1) ?: ""}${student.name.split(" ").lastOrNull()?.take(1) ?: ""}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SchoolBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(student.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text("ID: ${student.id}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StudentProfileStat("Grade", student.gradeLevel ?: "N/A")
                StudentProfileStat("Section", student.section ?: "N/A")
                StudentProfileStat("Year", schoolYear)
            }
        }
    }
}

@Composable
private fun StudentProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun StudentSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun StudentClearanceCard(item: ClearanceItem) {
    val borderColor by animateColorAsState(if (item.isCleared) SuccessGreen else Color.Transparent)

    // ✅ FIXED: Logic to handle null or "null" strings
    val statusText = if (item.isCleared) {
        val sigName = item.signatoryName
        if (!sigName.isNullOrBlank() && sigName != "null") "Cleared by $sigName" else "Cleared"
    } else {
        val sigName = item.signatoryName
        if (!sigName.isNullOrBlank() && sigName != "null") "Pending signature from $sigName" else "Pending Assignment"
    }

    val statusColor = if (item.isCleared) SuccessGreen else if(statusText == "Pending Assignment") SchoolRed else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if(item.isCleared) 1.dp else 0.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (item.isCleared) SuccessGreen.copy(alpha = 0.1f) else SchoolRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isCleared) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (item.isCleared) SuccessGreen else SchoolRed
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subjectName ?: "Unknown Requirement",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
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
    ModalDrawerSheet(drawerContainerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SchoolBlue)
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.malasila),
                    contentDescription = "School Logo",
                    modifier = Modifier.size(70.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome Back,",
                style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
            )
            Text(
                text = student.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Lock, null, tint = SchoolBlue) },
            label = { Text("Change Password") },
            selected = false,
            onClick = onChangePasswordClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, null, tint = SchoolRed) },
            label = { Text("Logout", color = SchoolRed) },
            selected = false,
            onClick = onSignOutClick,
            modifier = Modifier.padding(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
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
    var isCurrentVisible by remember { mutableStateOf(false) }
    var isNewVisible by remember { mutableStateOf(false) }
    var isConfirmVisible by remember { mutableStateOf(false) }

    val isMatch = newPassword.isNotEmpty() && newPassword == confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", fontWeight = FontWeight.Bold, color = SchoolBlue) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordInput(label = "Current Password", value = currentPassword, onValueChange = { currentPassword = it }, isVisible = isCurrentVisible, onToggle = { isCurrentVisible = !isCurrentVisible })
                PasswordInput(label = "New Password", value = newPassword, onValueChange = { newPassword = it }, isVisible = isNewVisible, onToggle = { isNewVisible = !isNewVisible })
                PasswordInput(label = "Confirm Password", value = confirmPassword, onValueChange = { confirmPassword = it }, isVisible = isConfirmVisible, onToggle = { isConfirmVisible = !isConfirmVisible }, isError = confirmPassword.isNotEmpty() && !isMatch)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPassword, newPassword) },
                enabled = currentPassword.isNotEmpty() && isMatch,
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBlue)
            ) { Text("Update Password") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun PasswordInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggle: () -> Unit,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    )
}