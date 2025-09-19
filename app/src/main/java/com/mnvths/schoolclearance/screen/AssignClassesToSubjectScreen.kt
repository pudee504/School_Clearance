package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.AssignmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignClassesToSubjectScreen( // ✅ RENAMED function
    navController: NavController,
    signatoryId: Int, // ✅ RENAMED parameter (formerly facultyId)
    subjectId: Int,   // ✅ RENAMED parameter (formerly signatoryId)
    subjectName: String, // ✅ RENAMED parameter (formerly signatoryName)
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val sections by viewModel.sections
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    var assignedSections by remember { mutableStateOf<List<ClassSection>>(emptyList()) }
    var isFetchingAssigned by remember { mutableStateOf(true) }
    val selectedSectionIds = remember { mutableStateListOf<Int>() }

    LaunchedEffect(Unit) {
        viewModel.fetchAllClassSections()
        // ✅ Use correct parameters to fetch sections for this specific subject/signatory pair
        viewModel.fetchAssignedSections(signatoryId, subjectId) { existing ->
            assignedSections = existing
            isFetchingAssigned = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Classes: $subjectName") }, // ✅ Updated title
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    val unassignedSections = sections.filter { section ->
                        assignedSections.none { it.sectionId == section.sectionId }
                    }

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

                            // ✅ Call the updated ViewModel function with correct parameters
                            // NOTE: You will need to rename this function in your AssignmentViewModel
                            viewModel.assignClassesToSubject(
                                signatoryId = signatoryId,
                                subjectId = subjectId,
                                sectionIds = selectedSectionIds.toList(),
                                onSuccess = {
                                    Toast.makeText(context, "Classes assigned successfully!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
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