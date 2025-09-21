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
import com.mnvths.schoolclearance.viewmodel.SectionManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSectionScreen(
    navController: NavController,
    sectionId: Int,
    initialGradeLevel: String,
    initialSectionName: String,
    viewModel: SectionManagementViewModel = viewModel() // <-- Use the new ViewModel
) {
    val context = LocalContext.current
    var selectedGradeLevel by remember { mutableStateOf(initialGradeLevel) }
    var sectionName by remember { mutableStateOf(initialSectionName) }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Section") },
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
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart)
            ) {
                OutlinedTextField(
                    value = "Grade $selectedGradeLevel",
                    onValueChange = {},
                    label = { Text("Grade Level") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
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
                    if (selectedGradeLevel.isNotBlank() && sectionName.isNotBlank()) {
                        viewModel.updateSection(
                            sectionId = sectionId,
                            gradeLevel = selectedGradeLevel,
                            sectionName = sectionName,
                            onSuccess = {
                                Toast.makeText(context, "Section updated successfully!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Fields cannot be empty.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}