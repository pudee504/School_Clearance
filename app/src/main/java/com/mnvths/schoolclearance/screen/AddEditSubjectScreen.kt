/*package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.GradeLevelItem
import com.mnvths.schoolclearance.viewmodel.SubjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubjectScreen(
    navController: NavController,
    subjectId: Int?,
    initialName: String?,
    viewModel: SubjectViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditing = subjectId != null
    var name by remember { mutableStateOf(initialName ?: "") }

    // State for the grade level dropdown
    val gradeLevels by viewModel.gradeLevels.collectAsState()
    var selectedGrade by remember { mutableStateOf<GradeLevelItem?>(null) }
    var gradeDropdownExpanded by remember { mutableStateOf(false) }

    // Fetch grade levels when the screen is composed
    LaunchedEffect(Unit) {
        viewModel.fetchGradeLevels()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Subject" else "Add Subject") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Subject Name (e.g., Mathematics)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Show Grade Level dropdown only when adding a new subject
            if (!isEditing) {
                ExposedDropdownMenuBox(
                    expanded = gradeDropdownExpanded,
                    onExpandedChange = { gradeDropdownExpanded = !gradeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedGrade?.name ?: "Select Grade Level",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grade Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = gradeDropdownExpanded,
                        onDismissRequest = { gradeDropdownExpanded = false }
                    ) {
                        gradeLevels.forEach { grade ->
                            DropdownMenuItem(
                                text = { Text(grade.name) },
                                onClick = {
                                    selectedGrade = grade
                                    gradeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isEditing) {
                        viewModel.updateSubject(
                            id = subjectId!!,
                            name = name,
                            onSuccess = {
                                Toast.makeText(context, "Updated successfully.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show() }
                        )
                    } else {
                        if (selectedGrade == null) {
                            Toast.makeText(context, "Please select a grade level.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addSubject(
                            name = name,
                            gradeLevelId = selectedGrade!!.id, // Pass the selected grade ID
                            onSuccess = {
                                Toast.makeText(context, "Added successfully.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show() }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
} */