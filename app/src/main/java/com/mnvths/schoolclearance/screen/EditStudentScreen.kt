package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel
import com.mnvths.schoolclearance.viewmodel.UpdateStudentRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(
    navController: NavController,
    studentId: String,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val context = LocalContext.current

    // Observe state from ViewModel
    val studentDetails by viewModel.studentDetails.collectAsState()
    val allSections by viewModel.sections.collectAsState() // Reusing sections from main screen

    // Local state for form fields
    var studentIdText by remember { mutableStateOf("") }
    var firstNameText by remember { mutableStateOf("") }
    var middleNameText by remember { mutableStateOf("") }
    var lastNameText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // State for dropdowns
    var selectedGradeLevel by remember { mutableStateOf<String?>(null) }
    var selectedSectionId by remember { mutableStateOf<Int?>(null) }
    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var sectionDropdownExpanded by remember { mutableStateOf(false) }

    // Fetch data when the screen is first composed
    LaunchedEffect(Unit) {
        viewModel.fetchStudentDetails(studentId)
        viewModel.fetchSections() // Ensure sections are loaded
    }

    // Populate local state once studentDetails are fetched from the ViewModel
    LaunchedEffect(studentDetails) {
        studentDetails?.let {
            studentIdText = it.studentId
            firstNameText = it.firstName
            middleNameText = it.middleName ?: ""
            lastNameText = it.lastName
            selectedGradeLevel = it.gradeLevel
            selectedSectionId = it.sectionId
        }
    }

    // Clean up when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearStudentDetails()
        }
    }

    // Derived lists for dropdowns
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Text Fields for student info
            OutlinedTextField(value = studentIdText, onValueChange = { studentIdText = it }, label = { Text("Student ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = firstNameText, onValueChange = { firstNameText = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = middleNameText, onValueChange = { middleNameText = it }, label = { Text("Middle Name (Optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = lastNameText, onValueChange = { lastNameText = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())

            // Password Field
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

            // Grade Level Dropdown
            ExposedDropdownMenuBox(expanded = gradeDropdownExpanded, onExpandedChange = { gradeDropdownExpanded = it }) {
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
                                selectedSectionId = null // Reset section when grade changes
                                gradeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Section Dropdown (dependent on Grade Level)
            ExposedDropdownMenuBox(expanded = sectionDropdownExpanded, onExpandedChange = { sectionDropdownExpanded = it }) {
                OutlinedTextField(
                    value = sectionsForSelectedGrade.find { it.sectionId == selectedSectionId }?.sectionName ?: "Select Section",
                    onValueChange = {},
                    readOnly = true,
                    enabled = selectedGradeLevel != null, // Only enable after a grade is selected
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
                        val updatedStudent = UpdateStudentRequest(
                            studentId = studentIdText,
                            firstName = firstNameText,
                            middleName = middleNameText.takeIf { it.isNotBlank() },
                            lastName = lastNameText,
                            password = passwordText.takeIf { it.isNotBlank() },
                            sectionId = selectedSectionId!!
                        )
                        viewModel.updateStudent(
                            originalStudentId = studentId,
                            updatedDetails = updatedStudent,
                            onSuccess = {
                                Toast.makeText(context, "Student updated successfully", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}