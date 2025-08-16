package com.mnvths.schoolclearance.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.FacultyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyDetailsScreen(
    navController: NavController,
    facultyId: Int,
    facultyName: String,
    firstName: String,
    lastName: String,
    middleName: String?,
    viewModel: FacultyViewModel = viewModel()
) {
    val assignedSubjects by viewModel.assignedSubjects
    val assignedSections by viewModel.assignedSections
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    var expandedSubjectId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(facultyId) {
        viewModel.fetchAssignedSubjects(facultyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details for $facultyName") },
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
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Name: $facultyName", style = MaterialTheme.typography.headlineSmall)
            Text(text = "ID: $facultyId", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("assignSubjectToFaculty/$facultyId/$facultyName") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Assign New Subject")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Assigned Subjects:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assignedSubjects) { subject ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (expandedSubjectId == subject.subjectId) {
                                                expandedSubjectId = null
                                            } else {
                                                expandedSubjectId = subject.subjectId
                                                viewModel.fetchAssignedSections(facultyId, subject.subjectId)
                                            }
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = subject.subjectName, style = MaterialTheme.typography.titleMedium)
                                    Icon(
                                        imageVector = if (expandedSubjectId == subject.subjectId) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand or collapse"
                                    )
                                }
                                AnimatedVisibility(visible = expandedSubjectId == subject.subjectId) {
                                    val sectionsForSubject = assignedSections[subject.subjectId]
                                    if (sectionsForSubject != null) {
                                        if (sectionsForSubject.isNotEmpty()) {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                sectionsForSubject.forEach { section ->
                                                    Text(text = "${section.gradeLevel} - ${section.sectionName}", modifier = Modifier.padding(start = 16.dp))
                                                }
                                            }
                                        } else {
                                            Text(text = "No classes assigned yet.", modifier = Modifier.padding(top = 8.dp))
                                        }
                                    } else {
                                        CircularProgressIndicator(modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        navController.navigate("assignClassesToSubject/$facultyId/$facultyName/${subject.subjectId}/${subject.subjectName}")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Assign Class")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}