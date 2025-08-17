package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSectionScreen(
    navController: NavController,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedGradeLevel by remember { mutableStateOf<String?>(null) }
    var sectionName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Section") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dropdown Menu for Grade Level
            Box(
                modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart)
            ) {
                OutlinedTextField(
                    // Format the displayed value here
                    value = selectedGradeLevel?.let { "Grade $it" } ?: "Select Grade Level",
                    onValueChange = {}, // The value is changed by the dropdown, not direct input
                    label = { Text("Grade Level") },
                    readOnly = true, // Prevents keyboard from appearing
                    modifier = Modifier
                        .fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val gradeLevels = (7..12).toList()
                    gradeLevels.forEach { grade ->
                        DropdownMenuItem(
                            text = { Text("Grade $grade") },
                            onClick = {
                                selectedGradeLevel = grade.toString()
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    // Check if both fields have a valid value
                    if (selectedGradeLevel != null && sectionName.isNotBlank()) {
                        viewModel.addSection(
                            gradeLevel = selectedGradeLevel!!, // Safe to use !! because of the check
                            sectionName = sectionName,
                            onSuccess = {
                                Toast.makeText(context, "Section added successfully!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { errorMessage ->
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Please select a grade level and fill in the section name.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Section")
            }
        }
    }
}