package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
private val SuccessGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    navController: NavController,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val context = LocalContext.current

    // Form State
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

    // Selections
    var selectedGrade by remember { mutableStateOf<GradeLevelItem?>(null) }
    var selectedTrack by remember { mutableStateOf<ShsTrack?>(null) }
    var selectedStrand by remember { mutableStateOf<ShsStrand?>(null) }
    var selectedSpecialization by remember { mutableStateOf<Specialization?>(null) }
    var selectedSection by remember { mutableStateOf<ClassSection?>(null) }

    // Logic for Dependent Fields
    val gradeName = selectedGrade?.name
    val isJhsWithSpecialization = gradeName in listOf("Grade 8", "Grade 9", "Grade 10")
    val isSeniorHigh = gradeName in listOf("Grade 11", "Grade 12")

    val filteredStrands = remember(selectedTrack, shsStrands) {
        shsStrands.filter { it.trackId == selectedTrack?.id }
    }
    val filteredSections = remember(gradeName, allSections) {
        allSections.filter { it.gradeLevel == gradeName }
    }

    // Password Validation Logic
    val hasLength = password.length >= 8
    val hasDigit = password.any { it.isDigit() }
    val hasUpper = password.any { it.isUpperCase() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    val isPasswordValid = hasLength && hasDigit && hasUpper && hasSpecial

    // Reset logic when grade changes
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Registration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = SchoolBlue,
                    navigationIconContentColor = Color.Gray
                )
            )
        },
        bottomBar = {
            // Action Buttons pinned to bottom
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (!isPasswordValid) {
                                Toast.makeText(context, "Please meet all password requirements.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (studentId.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank() && selectedGrade != null) {
                                viewModel.addStudent(
                                    studentId = studentId,
                                    firstName = firstName,
                                    middleName = middleName.takeIf { it.isNotBlank() },
                                    lastName = lastName,
                                    password = password,
                                    gradeLevelId = selectedGrade!!.id,
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
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        enabled = isPasswordValid && studentId.isNotBlank() && firstName.isNotBlank() && lastName.isNotBlank() && selectedGrade != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SchoolBlue)
                    ) {
                        Text("Save Student")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Section 1: Personal Information ---
            FormSectionHeader(title = "Personal Information", icon = Icons.Outlined.Person)

            SchoolTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = "Student ID (LRN)",
                keyboardType = KeyboardType.Number
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    SchoolTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name", capitalization = KeyboardCapitalization.Words)
                }
                Box(modifier = Modifier.weight(1f)) {
                    SchoolTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name", capitalization = KeyboardCapitalization.Words)
                }
            }
            SchoolTextField(value = middleName, onValueChange = { middleName = it }, label = "Middle Name (Optional)", capitalization = KeyboardCapitalization.Words)

            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            // --- Section 2: Academic Details ---
            FormSectionHeader(title = "Academic Details", icon = Icons.Outlined.School)

            SchoolDropdown(
                label = "Grade Level",
                value = selectedGrade?.name,
                options = gradeLevels,
                onSelect = { selectedGrade = it },
                itemLabel = { it.name }
            )

            // Dynamic SHS Fields
            AnimatedVisibility(visible = isSeniorHigh) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SchoolDropdown(
                        label = "Track",
                        value = selectedTrack?.trackName,
                        options = shsTracks,
                        onSelect = {
                            selectedTrack = it
                            selectedStrand = null
                        },
                        itemLabel = { it.trackName }
                    )

                    if (selectedTrack != null) {
                        SchoolDropdown(
                            label = "Strand",
                            value = selectedStrand?.strandName,
                            options = filteredStrands,
                            onSelect = { selectedStrand = it },
                            itemLabel = { it.strandName }
                        )
                    }
                }
            }

            // Specialization Logic
            if (isJhsWithSpecialization || (isSeniorHigh && selectedStrand != null)) {
                SchoolDropdown(
                    label = if(isSeniorHigh) "Major / Specialization" else "TVE Specialization",
                    value = selectedSpecialization?.name,
                    options = specializations,
                    onSelect = { selectedSpecialization = it },
                    itemLabel = { it.name },
                    enabled = specializations.isNotEmpty()
                )
            }

            // Section Logic
            SchoolDropdown(
                label = "Assigned Section",
                value = selectedSection?.sectionName,
                options = filteredSections,
                onSelect = { selectedSection = it },
                itemLabel = { it.sectionName },
                enabled = selectedGrade != null
            )

            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            // --- Section 3: Security ---
            FormSectionHeader(title = "Account Security", icon = Icons.Outlined.Lock)

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Create Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SchoolBlue,
                    focusedLabelColor = SchoolBlue
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password"
                        )
                    }
                }
            )

            // Password Requirements Checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Password Requirements:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                PasswordRequirementRow(label = "At least 8 characters", isValid = hasLength)
                PasswordRequirementRow(label = "At least one number", isValid = hasDigit)
                PasswordRequirementRow(label = "At least one uppercase letter", isValid = hasUpper)
                PasswordRequirementRow(label = "At least one special character", isValid = hasSpecial)
            }

            // Extra spacing for scrolling past FAB/BottomBar
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

// --- Helper Components for Clean Code ---

@Composable
fun FormSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = SchoolBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = SchoolBlue, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PasswordRequirementRow(label: String, isValid: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isValid) SuccessGreen else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isValid) Color.Black else Color.Gray
        )
    }
}

@Composable
fun SchoolTextField(
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
fun <T> SchoolDropdown(
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
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
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
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}