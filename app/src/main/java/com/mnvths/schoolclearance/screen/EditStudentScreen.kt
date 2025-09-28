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
import com.mnvths.schoolclearance.data.*
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(
    navController: NavController,
    studentId: String,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val context = LocalContext.current

    // Data from ViewModel
    val studentDetails by viewModel.studentDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val gradeLevels by viewModel.gradeLevels.collectAsState()
    val allSections by viewModel.classSections.collectAsState()
    val shsTracks by viewModel.shsTracks.collectAsState()
    val shsStrands by viewModel.shsStrands.collectAsState()
    val specializations by viewModel.specializations.collectAsState()

    // UI State for text fields
    var studentIdText by remember { mutableStateOf("") }
    var firstNameText by remember { mutableStateOf("") }
    var middleNameText by remember { mutableStateOf("") }
    var lastNameText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // UI State for dropdown selections
    var selectedGrade by remember { mutableStateOf<GradeLevelItem?>(null) }
    var selectedTrack by remember { mutableStateOf<ShsTrack?>(null) }
    var selectedStrand by remember { mutableStateOf<ShsStrand?>(null) }
    var selectedSpecialization by remember { mutableStateOf<Specialization?>(null) }
    var selectedSectionId by remember { mutableStateOf<Int?>(null) }

    // A flag to prevent re-populating fields on configuration change
    var fieldsPopulated by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Fetch initial data for the student
    LaunchedEffect(studentId) {
        viewModel.fetchStudentDetailsForEdit(studentId)
    }

    // This effect now safely populates the UI state only ONCE when the data first arrives.
    LaunchedEffect(studentDetails) {
        studentDetails?.let { details ->
            if (!fieldsPopulated) {
                studentIdText = details.studentId
                firstNameText = details.firstName
                middleNameText = details.middleName ?: ""
                lastNameText = details.lastName
                selectedSectionId = details.sectionId

                val grade = gradeLevels.find { it.name == details.gradeLevel }
                selectedGrade = grade

                val strand = shsStrands.find { it.id == details.strandId }
                selectedStrand = strand
                selectedTrack = shsTracks.find { it.id == strand?.trackId }

                // Trigger fetch for specializations based on the loaded grade/strand
                if (grade?.name in listOf("Grade 8", "Grade 9", "Grade 10")) {
                    viewModel.fetchSpecializations(gradeLevelId = grade?.id)
                } else if (grade?.name in listOf("Grade 11", "Grade 12")) {
                    viewModel.fetchSpecializations(strandId = strand?.id)
                }

                fieldsPopulated = true
            }
        }
    }

    // Effect to select the specialization once the list is loaded
    LaunchedEffect(specializations, studentDetails) {
        if (specializations.isNotEmpty() && studentDetails?.specializationId != null) {
            selectedSpecialization = specializations.find { it.id == studentDetails?.specializationId }
        }
    }

    // Cleanup on exit
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearStudentDetails()
        }
    }

    // Derived state to control UI visibility
    val gradeName = selectedGrade?.name
    // ✅ FIXED: The condition now includes Grade 9 and Grade 10
    val isJhsWithSpecialization = gradeName in listOf("Grade 8", "Grade 9", "Grade 10")
    val isSeniorHigh = gradeName in listOf("Grade 11", "Grade 12")

    val filteredStrands = remember(selectedTrack, shsStrands) {
        shsStrands.filter { it.trackId == selectedTrack?.id }
    }
    val sectionsForSelectedGrade = allSections.filter { it.gradeLevel == gradeName }

    // Effects to fetch specializations when user *changes* a selection
    LaunchedEffect(selectedGrade) {
        if (fieldsPopulated && studentDetails?.gradeLevel != selectedGrade?.name) {
            selectedTrack = null
            selectedStrand = null
            selectedSpecialization = null
            selectedSectionId = null
            if (isJhsWithSpecialization) {
                viewModel.fetchSpecializations(gradeLevelId = selectedGrade?.id)
            } else {
                viewModel.clearSpecializations()
            }
        }
    }
    LaunchedEffect(selectedStrand) {
        if (fieldsPopulated && studentDetails?.strandId != selectedStrand?.id) {
            selectedSpecialization = null
            if (selectedStrand != null) {
                viewModel.fetchSpecializations(strandId = selectedStrand?.id)
            }
        }
    }

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
        if (studentDetails == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isLoading) { // Only show spinner on initial load
                    CircularProgressIndicator()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
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

                    var gradeDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = gradeDropdownExpanded, onExpandedChange = { gradeDropdownExpanded = !gradeDropdownExpanded }) {
                        OutlinedTextField(value = selectedGrade?.name ?: "None", onValueChange = {}, readOnly = true, label = { Text("Grade Level") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeDropdownExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                        ExposedDropdownMenu(expanded = gradeDropdownExpanded, onDismissRequest = { gradeDropdownExpanded = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { selectedGrade = null; gradeDropdownExpanded = false })
                            gradeLevels.forEach { grade ->
                                DropdownMenuItem(text = { Text(grade.name) }, onClick = { selectedGrade = grade; gradeDropdownExpanded = false })
                            }
                        }
                    }

                    if (isJhsWithSpecialization) {
                        var specializationDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = specializationDropdownExpanded, onExpandedChange = { specializationDropdownExpanded = !specializationDropdownExpanded }) {
                            OutlinedTextField(value = selectedSpecialization?.name ?: "Select TVE Specialization", onValueChange = {}, readOnly = true, label = { Text("TVE Specialization") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specializationDropdownExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                            ExposedDropdownMenu(expanded = specializationDropdownExpanded, onDismissRequest = { specializationDropdownExpanded = false }) {
                                specializations.forEach { spec ->
                                    DropdownMenuItem(text = { Text(spec.name) }, onClick = { selectedSpecialization = spec; specializationDropdownExpanded = false })
                                }
                            }
                        }
                    }

                    if (isSeniorHigh) {
                        var trackDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = trackDropdownExpanded, onExpandedChange = { trackDropdownExpanded = !trackDropdownExpanded }) {
                            OutlinedTextField(value = selectedTrack?.trackName ?: "Select Track", onValueChange = {}, readOnly = true, label = { Text("Track") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackDropdownExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                            ExposedDropdownMenu(expanded = trackDropdownExpanded, onDismissRequest = { trackDropdownExpanded = false }) {
                                shsTracks.forEach { track ->
                                    DropdownMenuItem(
                                        text = { Text(track.trackName) },
                                        onClick = {
                                            selectedTrack = track
                                            selectedStrand = null
                                            trackDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        if (selectedTrack != null) {
                            var strandDropdownExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = strandDropdownExpanded, onExpandedChange = { strandDropdownExpanded = !strandDropdownExpanded }) {
                                OutlinedTextField(value = selectedStrand?.strandName ?: "Select Strand", onValueChange = {}, readOnly = true, label = { Text("Strand") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strandDropdownExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), enabled = filteredStrands.isNotEmpty())
                                ExposedDropdownMenu(expanded = strandDropdownExpanded, onDismissRequest = { strandDropdownExpanded = false }) {
                                    filteredStrands.forEach { strand ->
                                        DropdownMenuItem(text = { Text(strand.strandName) }, onClick = { selectedStrand = strand; strandDropdownExpanded = false })
                                    }
                                }
                            }
                        }
                        if (selectedStrand != null) {
                            var specializationDropdownExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = specializationDropdownExpanded, onExpandedChange = { specializationDropdownExpanded = !specializationDropdownExpanded }) {
                                OutlinedTextField(value = selectedSpecialization?.name ?: "Select Specialization/Major", onValueChange = {}, readOnly = true, label = { Text("Specialization / Major") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specializationDropdownExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), enabled = specializations.isNotEmpty())
                                ExposedDropdownMenu(expanded = specializationDropdownExpanded, onDismissRequest = { specializationDropdownExpanded = false }) {
                                    specializations.forEach { spec ->
                                        DropdownMenuItem(text = { Text(spec.name) }, onClick = { selectedSpecialization = spec; specializationDropdownExpanded = false })
                                    }
                                }
                            }
                        }
                    }

                    var sectionDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = sectionDropdownExpanded, onExpandedChange = { sectionDropdownExpanded = !sectionDropdownExpanded }) {
                        OutlinedTextField(
                            value = sectionsForSelectedGrade.find { it.sectionId == selectedSectionId }?.sectionName ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            enabled = selectedGrade != null,
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
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
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
                                    sectionId = selectedSectionId,
                                    strandId = selectedStrand?.id,
                                    specializationId = selectedSpecialization?.id
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