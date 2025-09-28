package com.mnvths.schoolclearance.screen

// STEP 1: ADD ALL OF THESE IMPORTS
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun AddStudentScreen(
    navController: NavController,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    var studentId by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Data from ViewModel
    val gradeLevels by viewModel.gradeLevels.collectAsState()
    val allSections by viewModel.classSections.collectAsState()
    val shsTracks by viewModel.shsTracks.collectAsState()
    val shsStrands by viewModel.shsStrands.collectAsState()
    val specializations by viewModel.specializations.collectAsState()

    // State for selections
    var selectedGrade by remember { mutableStateOf<GradeLevelItem?>(null) }
    var selectedTrack by remember { mutableStateOf<ShsTrack?>(null) }
    var selectedStrand by remember { mutableStateOf<ShsStrand?>(null) }
    var selectedSpecialization by remember { mutableStateOf<Specialization?>(null) }
    var selectedSection by remember { mutableStateOf<ClassSection?>(null) }

    // State for dropdown expansion
    var gradeLevelExpanded by remember { mutableStateOf(false) }
    var trackExpanded by remember { mutableStateOf(false) }
    var strandExpanded by remember { mutableStateOf(false) }
    var specializationExpanded by remember { mutableStateOf(false) }
    // STEP 2: TYPO FIX HERE
    var sectionExpanded by remember { mutableStateOf(false) }

    // Derived state to control UI visibility
    val gradeName = selectedGrade?.name
    val isJhsWithSpecialization = gradeName in listOf("Grade 8", "Grade 9", "Grade 10")
    val isSeniorHigh = gradeName in listOf("Grade 11", "Grade 12")

    val filteredStrands = remember(selectedTrack, shsStrands) {
        shsStrands.filter { it.trackId == selectedTrack?.id }
    }
    val filteredSections = remember(gradeName, allSections) {
        allSections.filter { it.gradeLevel == gradeName }
    }

    // Fetch specializations when grade or strand changes
    LaunchedEffect(selectedGrade) {
        selectedTrack = null
        selectedStrand = null
        selectedSpecialization = null
        selectedSection = null

        if (isJhsWithSpecialization) {
            viewModel.fetchSpecializations(gradeLevelId = selectedGrade?.id)
        } else {
            viewModel.clearSpecializations()
        }
    }

    LaunchedEffect(selectedStrand) {
        selectedSpecialization = null
        if (selectedStrand != null) {
            viewModel.fetchSpecializations(strandId = selectedStrand?.id)
        } else if (isSeniorHigh) {
            viewModel.clearSpecializations()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(text = "Create New Student", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = studentId, onValueChange = { studentId = it }, label = { Text("Student ID (LRN)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = middleName, onValueChange = { middleName = it }, label = { Text("Middle Name (Optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())

            // Grade Level Dropdown
            ExposedDropdownMenuBox(expanded = gradeLevelExpanded, onExpandedChange = { gradeLevelExpanded = !gradeLevelExpanded }) {
                OutlinedTextField(
                    value = selectedGrade?.name ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Grade Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeLevelExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = gradeLevelExpanded, onDismissRequest = { gradeLevelExpanded = false }) {
                    DropdownMenuItem(text = { Text("None") }, onClick = { selectedGrade = null; gradeLevelExpanded = false })
                    gradeLevels.forEach { grade ->
                        DropdownMenuItem(text = { Text(grade.name) }, onClick = { selectedGrade = grade; gradeLevelExpanded = false })
                    }
                }
            }

            // JHS Specialization Dropdown
            if (isJhsWithSpecialization) {
                ExposedDropdownMenuBox(expanded = specializationExpanded, onExpandedChange = { specializationExpanded = !specializationExpanded }) {
                    OutlinedTextField(
                        value = selectedSpecialization?.name ?: "Select TVE Specialization",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("TVE Specialization") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specializationExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = specializationExpanded, onDismissRequest = { specializationExpanded = false }) {
                        specializations.forEach { spec ->
                            DropdownMenuItem(text = { Text(spec.name) }, onClick = { selectedSpecialization = spec; specializationExpanded = false })
                        }
                    }
                }
            }

            // SHS Dropdowns
            if (isSeniorHigh) {
                ExposedDropdownMenuBox(expanded = trackExpanded, onExpandedChange = { trackExpanded = !trackExpanded }) {
                    OutlinedTextField(
                        value = selectedTrack?.trackName ?: "Select Track",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Track") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trackExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = trackExpanded, onDismissRequest = { trackExpanded = false }) {
                        shsTracks.forEach { track ->
                            DropdownMenuItem(
                                text = { Text(track.trackName) },
                                onClick = {
                                    selectedTrack = track
                                    selectedStrand = null // Reset strand when track changes
                                    trackExpanded = false
                                }
                            )
                        }
                    }
                }
                if (selectedTrack != null) {
                    ExposedDropdownMenuBox(expanded = strandExpanded, onExpandedChange = { strandExpanded = !strandExpanded }) {
                        OutlinedTextField(
                            value = selectedStrand?.strandName ?: "Select Strand",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Strand") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strandExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            enabled = filteredStrands.isNotEmpty()
                        )
                        ExposedDropdownMenu(expanded = strandExpanded, onDismissRequest = { strandExpanded = false }) {
                            filteredStrands.forEach { strand ->
                                DropdownMenuItem(text = { Text(strand.strandName) }, onClick = { selectedStrand = strand; strandExpanded = false })
                            }
                        }
                    }
                }
                if (selectedStrand != null) {
                    ExposedDropdownMenuBox(expanded = specializationExpanded, onExpandedChange = { specializationExpanded = !specializationExpanded }) {
                        OutlinedTextField(
                            value = selectedSpecialization?.name ?: "Select Specialization/Major",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Specialization / Major") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specializationExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            enabled = specializations.isNotEmpty()
                        )
                        ExposedDropdownMenu(expanded = specializationExpanded, onDismissRequest = { specializationExpanded = false }) {
                            specializations.forEach { spec ->
                                DropdownMenuItem(text = { Text(spec.name) }, onClick = { selectedSpecialization = spec; specializationExpanded = false })
                            }
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = sectionExpanded, onExpandedChange = { sectionExpanded = !sectionExpanded }) {
                OutlinedTextField(
                    value = selectedSection?.sectionName ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Section") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    enabled = selectedGrade != null
                )
                ExposedDropdownMenu(expanded = sectionExpanded, onDismissRequest = { sectionExpanded = false }) {
                    DropdownMenuItem(text = { Text("None") }, onClick = { selectedSection = null; sectionExpanded = false })
                    filteredSections.forEach { section ->
                        DropdownMenuItem(text = { Text(section.sectionName) }, onClick = { selectedSection = section; sectionExpanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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
                    val passwordError = when {
                        password.length < 8 -> "Password must be at least 8 characters long."
                        !password.any { it.isDigit() } -> "Password must contain at least one number."
                        !password.any { it.isUpperCase() } -> "Password must contain at least one uppercase letter."
                        password.all { it.isLetterOrDigit() } -> "Password must contain at least one special character."
                        else -> null
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
                            sectionId = selectedSection?.sectionId,
                            strandId = selectedStrand?.id,
                            specializationId = selectedSpecialization?.id,
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
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}