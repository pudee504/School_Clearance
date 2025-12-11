package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.*
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

// Theme Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)

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

    // Lists
    val gradeLevels by viewModel.gradeLevels.collectAsState()
    val allSections by viewModel.classSections.collectAsState()
    val shsTracks by viewModel.shsTracks.collectAsState()
    val shsStrands by viewModel.shsStrands.collectAsState()
    val specializations by viewModel.specializations.collectAsState()

    // UI State
    var studentIdText by remember { mutableStateOf("") }
    var firstNameText by remember { mutableStateOf("") }
    var middleNameText by remember { mutableStateOf("") }
    var lastNameText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Dropdown Selections
    var selectedGrade by remember { mutableStateOf<GradeLevelItem?>(null) }
    var selectedTrack by remember { mutableStateOf<ShsTrack?>(null) }
    var selectedStrand by remember { mutableStateOf<ShsStrand?>(null) }
    var selectedSpecialization by remember { mutableStateOf<Specialization?>(null) }
    var selectedSectionId by remember { mutableStateOf<Int?>(null) }

    // Logic Flags
    var fieldsPopulated by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // --- Data Fetching Logic ---

    LaunchedEffect(studentId) {
        viewModel.fetchStudentDetailsForEdit(studentId)
    }

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

                if (grade?.name in listOf("Grade 8", "Grade 9", "Grade 10")) {
                    viewModel.fetchSpecializations(gradeLevelId = grade?.id)
                } else if (grade?.name in listOf("Grade 11", "Grade 12")) {
                    viewModel.fetchSpecializations(strandId = strand?.id)
                }
                fieldsPopulated = true
            }
        }
    }

    LaunchedEffect(specializations, studentDetails) {
        if (specializations.isNotEmpty() && studentDetails?.specializationId != null && fieldsPopulated) {
            // Only set if we haven't manually changed it yet
            if(selectedSpecialization == null) {
                selectedSpecialization = specializations.find { it.id == studentDetails?.specializationId }
            }
        }
    }

    // Dependent Logic
    val gradeName = selectedGrade?.name
    val isJhsWithSpecialization = gradeName in listOf("Grade 8", "Grade 9", "Grade 10")
    val isSeniorHigh = gradeName in listOf("Grade 11", "Grade 12")

    val filteredStrands = remember(selectedTrack, shsStrands) { shsStrands.filter { it.trackId == selectedTrack?.id } }
    val sectionsForSelectedGrade = remember(gradeName, allSections) { allSections.filter { it.gradeLevel == gradeName } }

    // Reset Logic on Changes
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
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = SchoolBlue,
                    navigationIconContentColor = SchoolBlue
                )
            )
        },
        bottomBar = {
            if (studentDetails != null) {
                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Cancel") }

                        Button(
                            onClick = {
                                if (studentIdText.isNotBlank() && firstNameText.isNotBlank() && lastNameText.isNotBlank() && selectedGrade != null) {
                                    isSaving = true
                                    val updatedStudent = UpdateStudentRequest(
                                        studentId = studentIdText,
                                        firstName = firstNameText,
                                        middleName = middleNameText.takeIf { it.isNotBlank() },
                                        lastName = lastNameText,
                                        password = passwordText.takeIf { it.isNotBlank() },
                                        gradeLevelId = selectedGrade!!.id,
                                        sectionId = selectedSectionId,
                                        strandId = selectedStrand?.id,
                                        specializationId = selectedSpecialization?.id
                                    )
                                    viewModel.updateStudent(
                                        originalStudentId = studentId,
                                        updatedDetails = updatedStudent,
                                        onSuccess = {
                                            Toast.makeText(context, "Updated Successfully", Toast.LENGTH_SHORT).show()
                                            isSaving = false
                                            navController.popBackStack()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                            isSaving = false
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Missing required fields.", Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SchoolBlue)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Save Changes")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading && studentDetails == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SchoolBlue)
            }
        } else if (studentDetails != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // --- Avatar Header ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SchoolBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${firstNameText.take(1)}${lastNameText.take(1)}".uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "$firstNameText $lastNameText", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "LRN: $studentIdText", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                // --- Personal Info ---
                EditStudentSectionHeader("Personal Information", Icons.Outlined.Person)

                EditStudentTextField(value = studentIdText, onValueChange = { studentIdText = it }, label = "Student ID (LRN)", keyboardType = KeyboardType.Number)
                EditStudentTextField(value = firstNameText, onValueChange = { firstNameText = it }, label = "First Name", capitalization = KeyboardCapitalization.Words)
                EditStudentTextField(value = middleNameText, onValueChange = { middleNameText = it }, label = "Middle Name (Optional)", capitalization = KeyboardCapitalization.Words)
                EditStudentTextField(value = lastNameText, onValueChange = { lastNameText = it }, label = "Last Name", capitalization = KeyboardCapitalization.Words)

                // --- Academic Info ---
                EditStudentSectionHeader("Academic Details", Icons.Outlined.School)

                EditStudentDropdown(
                    label = "Grade Level",
                    value = selectedGrade?.name,
                    options = gradeLevels,
                    onSelect = { selectedGrade = it },
                    itemLabel = { it.name }
                )

                AnimatedVisibility(visible = isSeniorHigh, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        EditStudentDropdown(label = "Track", value = selectedTrack?.trackName, options = shsTracks, onSelect = { selectedTrack = it; selectedStrand = null }, itemLabel = { it.trackName })
                        if (selectedTrack != null) {
                            EditStudentDropdown(label = "Strand", value = selectedStrand?.strandName, options = filteredStrands, onSelect = { selectedStrand = it }, itemLabel = { it.strandName })
                        }
                    }
                }

                AnimatedVisibility(visible = isJhsWithSpecialization || (isSeniorHigh && selectedStrand != null)) {
                    EditStudentDropdown(
                        label = if(isSeniorHigh) "Major / Specialization" else "TVE Specialization",
                        value = selectedSpecialization?.name,
                        options = specializations,
                        onSelect = { selectedSpecialization = it },
                        itemLabel = { it.name },
                        enabled = specializations.isNotEmpty()
                    )
                }

                EditStudentDropdown(
                    label = "Section",
                    value = sectionsForSelectedGrade.find { it.sectionId == selectedSectionId }?.sectionName,
                    options = sectionsForSelectedGrade,
                    onSelect = { selectedSectionId = it.sectionId },
                    itemLabel = { it.sectionName },
                    enabled = selectedGrade != null
                )

                // --- Security ---
                EditStudentSectionHeader("Security", Icons.Outlined.Lock)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.labelLarge,
                            color = SchoolBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Leave this blank unless you want to change the student's password.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SchoolBlue,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

// Renamed helper functions to avoid conflict with AddStudentScreen
@Composable
private fun EditStudentSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = SchoolBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = SchoolBlue, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditStudentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SchoolBlue,
            focusedLabelColor = SchoolBlue
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = ImeAction.Next
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EditStudentDropdown(
    label: String,
    value: String?,
    options: List<T>,
    onSelect: (T) -> Unit,
    itemLabel: (T) -> String,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if(enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value ?: "Select $label",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SchoolBlue,
                focusedLabelColor = SchoolBlue,
                disabledContainerColor = Color(0xFFFAFAFA),
                disabledBorderColor = Color.LightGray.copy(alpha = 0.3f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(itemLabel(option)) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}