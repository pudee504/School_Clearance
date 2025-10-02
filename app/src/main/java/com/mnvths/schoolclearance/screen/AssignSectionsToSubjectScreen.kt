// com/mnvths/schoolclearance/screen/AssignSectionsToSubjectScreen.kt
package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.AssignmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSectionsToSubjectScreen(
    navController: NavController,
    signatoryId: Int,
    subjectId: Int,
    subjectName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val availableSections by viewModel.availableSections
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val selectedSections = remember { mutableStateListOf<ClassSection>() }

    LaunchedEffect(signatoryId, subjectId) {
        viewModel.loadAvailableSections(signatoryId, subjectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign to $subjectName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedSections.isNotEmpty()) {
                FloatingActionButton(onClick = {
                    val selectedIds = selectedSections.map { it.sectionId }
                    viewModel.assignSectionsToSubject(
                        signatoryId = signatoryId,
                        subjectId = subjectId,
                        sectionIds = selectedIds,
                        onSuccess = {
                            Toast.makeText(context, "Sections assigned!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    )
                }) {
                    Icon(Icons.Filled.Done, contentDescription = "Assign Selected Sections")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Text(
                    "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                availableSections.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("All relevant sections have already been assigned.")
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableSections, key = { it.sectionId }) { section ->
                            val isSelected = selectedSections.contains(section)
                            Card(
                                onClick = {
                                    if (isSelected) selectedSections.remove(section)
                                    else selectedSections.add(section)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${section.gradeLevel} - ${section.sectionName}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(checked = isSelected, onCheckedChange = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}