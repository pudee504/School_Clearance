package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.SectionManagementViewModel

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val BackgroundGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSectionScreen(
    navController: NavController,
    viewModel: SectionManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedGradeLevel by remember { mutableStateOf<String?>(null) }
    var sectionName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // Validation State
    val isValid = selectedGradeLevel != null && sectionName.isNotBlank()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Section", fontWeight = FontWeight.Bold) },
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
        containerColor = BackgroundGray,
        bottomBar = {
            // Buttons pinned to bottom
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (isValid) {
                                viewModel.addSection(
                                    gradeLevel = selectedGradeLevel!!,
                                    sectionName = sectionName.trim(), // Trim whitespace
                                    onSuccess = {
                                        Toast.makeText(context, "Section added successfully!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    },
                                    onError = { errorMessage ->
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SchoolBlue,
                            disabledContainerColor = Color.LightGray
                        )
                    ) {
                        Text("Save Section")
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Instructions
            Column {
                Text(
                    text = "Class Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = SchoolBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Assign a grade level and a unique name for this section.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Grade Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedGradeLevel ?: "Select Grade Level",
                            onValueChange = {},
                            label = { Text("Grade Level") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            leadingIcon = { Icon(Icons.Outlined.School, null, tint = SchoolBlue) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SchoolBlue,
                                focusedLabelColor = SchoolBlue,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            (7..12).forEach { grade ->
                                val gradeName = "Grade $grade"
                                DropdownMenuItem(
                                    text = { Text(gradeName) },
                                    onClick = {
                                        selectedGradeLevel = gradeName
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Section Name Input
                    OutlinedTextField(
                        value = sectionName,
                        onValueChange = { sectionName = it },
                        label = { Text("Section Name") },
                        placeholder = { Text("e.g. Rizal, Newton, Diamond") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.Class, null, tint = SchoolBlue) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SchoolBlue,
                            focusedLabelColor = SchoolBlue,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        // Smart Keyboard Options
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words, // Auto-capitalize names
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }
        }
    }
}