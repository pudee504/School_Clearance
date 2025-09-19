package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.UpdateStudentRequest
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(
    navController: NavController,
    studentId: String,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val context = LocalContext.current

    val profile by viewModel.studentProfile.collectAsState()
    val allSections by viewModel.sections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var studentIdText by remember { mutableStateOf("") }
    var firstNameText by remember { mutableStateOf("") }
    var middleNameText by remember { mutableStateOf("") }
    var lastNameText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedGradeLevel by remember { mutableStateOf<String?>(null) }
    var selectedSectionId by remember { mutableStateOf<Int?>(null) }
    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var sectionDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(studentId) {
        viewModel.fetchStudentProfile(studentId)
        viewModel.fetchSections()
    }

    LaunchedEffect(profile) {
        profile?.let {
            studentIdText = it.id
            firstNameText = it.firstName
            middleNameText = it.middleName ?: ""
            lastNameText = it.lastName
            if (it.gradeLevel != "Unassigned") {
                selectedGradeLevel = it.gradeLevel
            } else {
                selectedGradeLevel = null
            }
            selectedSectionId = it.sectionId
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearStudentProfile()
        }
    }

    val gradeLevels = allSections.map { it.gradeLevel }.distinct().sorted()
    val sectionsForSelectedGrade = allSections.filter { it.gradeLevel == selectedGradeLevel }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Student") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && profile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (profile != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = studentIdText, onValueChange = { studentIdText = it }, label = { Text("Student ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = firstNameText, onValueChange = { firstNameText = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = middleNameText, onValueChange = { middleNameText = it }, label = { Text("Middle Name (Optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lastNameText, onValueChange = { lastNameText = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    label = { Text("New Password (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(image, "Toggle password visibility") }
                    }
                )

                ExposedDropdownMenuBox(expanded = gradeDropdownExpanded, onExpandedChange = { gradeDropdownExpanded = !gradeDropdownExpanded }) {
                    OutlinedTextField(
                        value = selectedGradeLevel ?: "Select Grade",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grade Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = gradeDropdownExpanded, onDismissRequest = { gradeDropdownExpanded = false }) {
                        gradeLevels.forEach { grade ->
                            DropdownMenuItem(
                                text = { Text(grade) },
                                onClick = {
                                    selectedGradeLevel = grade
                                    selectedSectionId = null
                                    gradeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = sectionDropdownExpanded, onExpandedChange = { sectionDropdownExpanded = !sectionDropdownExpanded }) {
                    OutlinedTextField(
                        value = sectionsForSelectedGrade.find { it.sectionId == selectedSectionId }?.sectionName ?: "Select Section",
                        onValueChange = {},
                        readOnly = true,
                        enabled = selectedGradeLevel != null,
                        label = { Text("Section") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = sectionDropdownExpanded, onDismissRequest = { sectionDropdownExpanded = false }) {
                        sectionsForSelectedGrade.forEach { section ->
                            DropdownMenuItem(
                                text = { Text(section.sectionName) },
                                onClick = {
                                    selectedSectionId = section.sectionId
                                    sectionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (studentIdText.isNotBlank() && firstNameText.isNotBlank() && lastNameText.isNotBlank() && selectedSectionId != null) {
                            isSaving = true
                            val updatedStudent = UpdateStudentRequest(
                                studentId = studentIdText,
                                firstName = firstNameText,
                                middleName = middleNameText.takeIf { it.isNotBlank() },
                                lastName = lastNameText,
                                password = passwordText.takeIf { it.isNotBlank() },
                                sectionId = selectedSectionId
                            )
                            viewModel.updateStudent(
                                originalStudentId = studentId,
                                updatedDetails = updatedStudent,
                                onSuccess = {
                                    Toast.makeText(context, "Student updated successfully", Toast.LENGTH_SHORT).show()
                                    isSaving = false
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                    isSaving = false
                                }
                            )
                        } else {
                            Toast.makeText(context, "Please ensure Student ID, names, and a section are provided.", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}