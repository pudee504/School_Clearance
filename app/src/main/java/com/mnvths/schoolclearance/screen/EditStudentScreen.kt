package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.mnvths.schoolclearance.viewmodel.SectionManagementViewModel
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

private val tracks = listOf("Academic", "TVL (Technical-Vocational-Livelihood)")
private val academicStrands = listOf("STEM", "ABM", "HUMSS", "GAS")
private val tvlStrands = listOf("ICT", "HE (Home Economics)", "IA (Industrial Arts)")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(
    navController: NavController,
    studentId: String,
    studentViewModel: StudentManagementViewModel = viewModel(),
    sectionViewModel: SectionManagementViewModel = viewModel()
) {
    val context = LocalContext.current

    // Observe the new, correctly typed state object from the ViewModel
    val studentDetails by studentViewModel.studentDetails.collectAsState()
    val isLoading by studentViewModel.isLoading.collectAsState()

    val allSections by sectionViewModel.classSections.collectAsState()
    val gradeLevels by sectionViewModel.gradeLevels.collectAsState()

    var studentIdText by remember { mutableStateOf("") }
    var firstNameText by remember { mutableStateOf("") }
    var middleNameText by remember { mutableStateOf("") }
    var lastNameText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var selectedGradeLevel by remember { mutableStateOf<String?>(null) }
    var selectedSectionId by remember { mutableStateOf<Int?>(null) }
    var selectedTrack by remember { mutableStateOf<String?>(null) }
    var selectedStrand by remember { mutableStateOf<String?>(null) }

    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var sectionDropdownExpanded by remember { mutableStateOf(false) }
    var trackExpanded by remember { mutableStateOf(false) }
    var strandExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(studentId) {
        // Call the correct fetch function
        studentViewModel.fetchStudentDetailsForEdit(studentId)
        sectionViewModel.fetchClassSections()
        sectionViewModel.fetchAllGradeLevels()
    }

    LaunchedEffect(studentDetails) {
        // This now correctly populates the UI state when the data loads
        studentDetails?.let { details ->
            studentIdText = details.studentId
            firstNameText = details.firstName
            middleNameText = details.middleName ?: ""
            lastNameText = details.lastName
            selectedGradeLevel = details.gradeLevel
            selectedSectionId = details.sectionId
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Call the correct clear function on exit
            studentViewModel.clearStudentDetails()
        }
    }

    val isSeniorHigh = selectedGradeLevel == "Grade 11" || selectedGradeLevel == "Grade 12"
    val sectionsForSelectedGrade = allSections.filter { it.gradeLevel == selectedGradeLevel }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Student") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        // The condition now checks the new state object
        if (isLoading && studentDetails == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (studentDetails != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(value = studentIdText, onValueChange = { studentIdText = it }, label = { Text("Student ID (LRN)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = firstNameText, onValueChange = { firstNameText = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = middleNameText, onValueChange = { middleNameText = it }, label = { Text("Middle Name (Optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = lastNameText, onValueChange = { lastNameText = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(
                        value = passwordText,
                        onValueChange = { passwordText = it },
                        label = { Text("New Password (Optional)") },
                        placeholder = { Text("Leave blank to keep unchanged") },
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
                            value = selectedGradeLevel ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grade Level") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = gradeDropdownExpanded, onDismissRequest = { gradeDropdownExpanded = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = {
                                selectedGradeLevel = null
                                selectedSectionId = null
                                gradeDropdownExpanded = false
                            })
                            gradeLevels.forEach { grade ->
                                DropdownMenuItem(text = { Text(grade) }, onClick = {
                                    if (selectedGradeLevel != grade) {
                                        selectedSectionId = null
                                    }
                                    selectedGradeLevel = grade
                                    gradeDropdownExpanded = false
                                })
                            }
                        }
                    }

                    if (isSeniorHigh) {
                        ExposedDropdownMenuBox(expanded = trackExpanded, onExpandedChange = { trackExpanded = !trackExpanded }) {
                            OutlinedTextField(value = selectedTrack ?: "Select a Track", onValueChange = {}, readOnly = true, label = { Text("Track") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                            ExposedDropdownMenu(expanded = trackExpanded, onDismissRequest = { trackExpanded = false }) {
                                tracks.forEach { track -> DropdownMenuItem(text = { Text(track) }, onClick = { selectedTrack = track; trackExpanded = false }) }
                            }
                        }
                        if (selectedTrack != null) {
                            val currentStrands = if (selectedTrack == "Academic") academicStrands else tvlStrands
                            ExposedDropdownMenuBox(expanded = strandExpanded, onExpandedChange = { strandExpanded = !strandExpanded }) {
                                OutlinedTextField(value = selectedStrand ?: "Select a Strand", onValueChange = {}, readOnly = true, label = { Text("Strand") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strandExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                                ExposedDropdownMenu(expanded = strandExpanded, onDismissRequest = { strandExpanded = false }) {
                                    currentStrands.forEach { strand -> DropdownMenuItem(text = { Text(strand) }, onClick = { selectedStrand = strand; strandExpanded = false }) }
                                }
                            }
                        }
                    }

                    ExposedDropdownMenuBox(expanded = sectionDropdownExpanded, onExpandedChange = { sectionDropdownExpanded = !sectionDropdownExpanded }) {
                        OutlinedTextField(
                            value = sectionsForSelectedGrade.find { it.sectionId == selectedSectionId }?.sectionName ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            enabled = selectedGradeLevel != null,
                            label = { Text("Section") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = sectionDropdownExpanded, onDismissRequest = { sectionDropdownExpanded = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = {
                                selectedSectionId = null
                                sectionDropdownExpanded = false
                            })
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
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (studentIdText.isNotBlank() && firstNameText.isNotBlank() && lastNameText.isNotBlank()) {
                                isSaving = true
                                val updatedStudent = UpdateStudentRequest(
                                    studentId = studentIdText,
                                    firstName = firstNameText,
                                    middleName = middleNameText.takeIf { it.isNotBlank() },
                                    lastName = lastNameText,
                                    password = passwordText.takeIf { it.isNotBlank() },
                                    sectionId = selectedSectionId
                                )
                                studentViewModel.updateStudent(
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
                                Toast.makeText(context, "Please ensure Student ID and names are provided.", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}