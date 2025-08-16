package com.mnvths.schoolclearance.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.AssignmentViewModel
import com.mnvths.schoolclearance.ClassSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignClassesToSubjectScreen(
    navController: NavController,
    facultyId: Int,
    subjectId: Int,
    subjectName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val sections by viewModel.sections
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    // Sections already assigned in the database
    var assignedSections by remember { mutableStateOf<List<ClassSection>>(emptyList()) }
    var isFetchingAssigned by remember { mutableStateOf(true) }

    // Sections selected by the user (only new selections)
    val selectedSectionIds = remember { mutableStateListOf<Int>() }


    // Fetch data once on load
    LaunchedEffect(Unit) {
        viewModel.fetchAllClassSections()
        viewModel.fetchAssignedSections(facultyId, subjectId) { existing ->
            assignedSections = existing
            isFetchingAssigned = false
            Log.d("AssignClasses", "Initial Assigned Sections: $existing")
        }
    }

    // Debug state changes
    LaunchedEffect(assignedSections, sections) {
        Log.d("AssignClasses", "Assigned Sections: $assignedSections")
        Log.d("AssignClasses", "All Sections: $sections")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign $subjectName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when {
                isLoading || isFetchingAssigned -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                error != null -> Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )

                else -> {
                    // Only show sections not yet assigned
                    val unassignedSections = sections.filter { section ->
                        assignedSections.none { it.sectionId == section.sectionId }
                    }
                    Log.d("AssignClasses", "Unassigned Sections: $unassignedSections")

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(unassignedSections) { section ->
                            val isChecked = selectedSectionIds.contains(section.sectionId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .toggleable(
                                        value = isChecked,
                                        onValueChange = { selected ->
                                            if (selected) selectedSectionIds.add(section.sectionId)
                                            else selectedSectionIds.remove(section.sectionId)
                                        },
                                        role = Role.Checkbox
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${section.gradeLevel} - ${section.sectionName}",
                                    modifier = Modifier.weight(1f)
                                )
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = null
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (selectedSectionIds.isEmpty()) {
                                Toast.makeText(context, "No new sections selected", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // In AssignClassesToSubjectScreen.kt, inside the Button's onClick lambda

                            viewModel.assignClassesToFaculty(
                                facultyId = facultyId,
                                subjectId = subjectId,
                                sectionIds = selectedSectionIds.toList(),
                                onSuccess = {
                                    Toast.makeText(context, "Classes assigned successfully!", Toast.LENGTH_SHORT).show()
                                    // Refetch assigned sections to ensure UI is in sync with server
                                    viewModel.fetchAssignedSections(facultyId, subjectId) { updatedSections ->
                                        // Update the assignedSections state here
                                        assignedSections = updatedSections
                                        // Clear the selected sections list
                                        selectedSectionIds.clear()
                                        Log.d("AssignClasses", "Updated Assigned Sections: $updatedSections")
                                        Log.d("AssignClasses", "Selected Section IDs Cleared: $selectedSectionIds")
                                        // Finally, navigate back
                                        navController.popBackStack()
                                    }
                                },
                                onError = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            )

                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("Save Assignments")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}