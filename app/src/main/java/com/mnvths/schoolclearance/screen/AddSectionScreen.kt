package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.SectionManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSectionScreen(
    navController: NavController,
    viewModel: SectionManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    // ✅ FIX: The state now holds the full grade level name, e.g., "Grade 7"
    var selectedGradeLevel by remember { mutableStateOf<String?>(null) }
    var sectionName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // --- Top Bar ---
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Add New Section",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        // --- Content ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    // ✅ FIX: Directly use the state variable's value
                    value = selectedGradeLevel ?: "Select Grade Level",
                    onValueChange = {},
                    label = { Text("Grade Level") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    (7..12).forEach { grade ->
                        val gradeName = "Grade $grade"
                        DropdownMenuItem(
                            text = { Text(gradeName) },
                            onClick = {
                                // ✅ FIX: Save the full name to the state
                                selectedGradeLevel = gradeName
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
        }

        // --- Bottom Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    if (selectedGradeLevel != null && sectionName.isNotBlank()) {
                        viewModel.addSection(
                            // ✅ FIX: Pass the full grade name to the ViewModel
                            gradeLevel = selectedGradeLevel!!,
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
                        Toast.makeText(context, "Please select a grade level and enter a section name.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}