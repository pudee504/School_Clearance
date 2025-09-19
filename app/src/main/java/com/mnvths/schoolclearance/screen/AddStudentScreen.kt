package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

// --- Constants for SHS Tracks and Strands ---
private val tracks = listOf("Academic", "TVL (Technical-Vocational-Livelihood)")
private val academicStrands = listOf("STEM", "ABM", "HUMSS", "GAS")
private val tvlStrands = listOf("ICT", "HE (Home Economics)", "IA (Industrial Arts)")


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    navController: NavController,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val context = LocalContext.current

    // --- Basic Student Info State ---
    var studentId by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // --- State for Dropdown Data ---
    // In a real app, these would be fetched from the viewModel
    val gradeLevels by viewModel.gradeLevels.collectAsState()
    val allSections by viewModel.classSections.collectAsState()

    // --- State for Dropdown Selections ---
    var selectedGradeLevel by remember { mutableStateOf<String?>(null) }
    var selectedTrack by remember { mutableStateOf<String?>(null) }
    var selectedStrand by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf<ClassSection?>(null) }

    // --- State for Dropdown Expansion ---
    var gradeLevelExpanded by remember { mutableStateOf(false) }
    var trackExpanded by remember { mutableStateOf(false) }
    var strandExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    // --- Derived State for Conditional UI ---
    val isSeniorHigh = selectedGradeLevel == "11" || selectedGradeLevel == "12"
    val filteredSections = remember(selectedGradeLevel, allSections) {
        if (selectedGradeLevel != null) {
            allSections.filter { it.gradeLevel == selectedGradeLevel }
        } else {
            emptyList()
        }
    }

    // --- Effect to clear dependent selections when grade level changes ---
    LaunchedEffect(selectedGradeLevel) {
        selectedTrack = null
        selectedStrand = null
        selectedSection = null
    }

    // --- Effect to clear strand when track changes ---
    LaunchedEffect(selectedTrack) {
        selectedStrand = null
    }


    // --- UI Layout ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // --- Top Bar with Back Button ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Create New Student",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        // --- Scrollable Content Area ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("Student ID (LRN)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = middleName, onValueChange = { middleName = it }, label = { Text("Middle Name (Optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())

            // --- Grade Level Dropdown ---
            ExposedDropdownMenuBox(
                expanded = gradeLevelExpanded,
                onExpandedChange = { gradeLevelExpanded = !gradeLevelExpanded }
            ) {
                OutlinedTextField(
                    value = selectedGradeLevel ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Grade Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeLevelExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = gradeLevelExpanded,
                    onDismissRequest = { gradeLevelExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("None") }, onClick = { selectedGradeLevel = null; gradeLevelExpanded = false })
                    gradeLevels.forEach { grade ->
                        DropdownMenuItem(text = { Text(grade) }, onClick = { selectedGradeLevel = grade; gradeLevelExpanded = false })
                    }
                }
            }

            // --- Conditional Senior High School (SHS) Dropdowns ---
            if (isSeniorHigh) {
                // --- Track Dropdown ---
                ExposedDropdownMenuBox(
                    expanded = trackExpanded,
                    onExpandedChange = { trackExpanded = !trackExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTrack ?: "Select a Track",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Track") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = trackExpanded,
                        onDismissRequest = { trackExpanded = false }
                    ) {
                        tracks.forEach { track ->
                            DropdownMenuItem(text = { Text(track) }, onClick = { selectedTrack = track; trackExpanded = false })
                        }
                    }
                }

                // --- Strand Dropdown ---
                if (selectedTrack != null) {
                    val currentStrands = if (selectedTrack == "Academic") academicStrands else tvlStrands
                    ExposedDropdownMenuBox(
                        expanded = strandExpanded,
                        onExpandedChange = { strandExpanded = !strandExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedStrand ?: "Select a Strand",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Strand") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strandExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = strandExpanded,
                            onDismissRequest = { strandExpanded = false }
                        ) {
                            currentStrands.forEach { strand ->
                                DropdownMenuItem(text = { Text(strand) }, onClick = { selectedStrand = strand; strandExpanded = false })
                            }
                        }
                    }
                }
            }


            // --- Section Dropdown ---
            ExposedDropdownMenuBox(
                expanded = sectionExpanded,
                onExpandedChange = { sectionExpanded = !sectionExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSection?.sectionName ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Section") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    enabled = selectedGradeLevel != null
                )
                ExposedDropdownMenu(
                    expanded = sectionExpanded,
                    onDismissRequest = { sectionExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("None") }, onClick = { selectedSection = null; sectionExpanded = false })
                    filteredSections.forEach { section ->
                        DropdownMenuItem(text = { Text(section.sectionName) }, onClick = { selectedSection = section; sectionExpanded = false })
                    }
                }
            }

            // --- Password Field ---
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility")
                    }
                }
            )

        }

        // --- Save Button at the bottom ---
        Button(
            onClick = {
                // Password Validation Block
                val passwordError = when {
                    password.length < 8 -> "Password must be at least 8 characters long."
                    !password.any { it.isDigit() } -> "Password must contain at least one number."
                    !password.any { it.isUpperCase() } -> "Password must contain at least one uppercase letter."
                    password.all { it.isLetterOrDigit() } -> "Password must contain at least one special character."
                    else -> null // No error
                }

                if (passwordError != null) {
                    Toast.makeText(context, passwordError, Toast.LENGTH_LONG).show()
                    return@Button
                }

                if (studentId.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank()) {
                    viewModel.addStudent(
                        studentId = studentId,
                        firstName = firstName,
                        middleName = middleName.takeIf { it.isNotBlank() },
                        lastName = lastName,
                        password = password,
                        sectionId = selectedSection?.sectionId, // Pass the nullable section ID
                        onSuccess = {
                            Toast.makeText(context, "Student created successfully!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { errorMessage ->
                            Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Save Student")
        }
    }
}