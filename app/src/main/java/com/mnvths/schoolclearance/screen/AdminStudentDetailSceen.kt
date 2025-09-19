package com.mnvths.schoolclearance.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.ClearanceStatusItem
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudentDetailScreen(
    navController: NavController,
    studentId: String,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val profile by viewModel.studentProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    // ✅ CHANGE 1: "Listen" for the error state from the ViewModel.
    val error by viewModel.error.collectAsState()

    LaunchedEffect(studentId) {
        viewModel.fetchStudentProfile(studentId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearStudentProfile()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.name ?: "Student Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // ✅ CHANGE 2: Updated logic to show the specific error message.
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(
                    text = error!!, // Display the actual error from the ViewModel
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else if (profile != null) {
                val p = profile!!
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Student Details", style = MaterialTheme.typography.titleLarge)
                    Text("LRN/ID: ${p.id}", style = MaterialTheme.typography.bodyLarge)
                    Text("Grade & Section: ${p.gradeLevel} ${p.section}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Clearance for ${p.activeTerm.schoolYear} (${p.activeTerm.termName} ${p.activeTerm.termNumber})",
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (p.clearanceStatus.isEmpty()) {
                        Text("No signatories assigned or student is unassigned.")
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(p.clearanceStatus) { item ->
                                ClearanceRow(item = item)
                            }
                        }
                    }

                    Button(
                        onClick = { navController.navigate("editStudent/${p.id}") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Student Details")
                    }
                }
            } else {
                // This is now just a fallback for an unknown state
                Text("Could not load student profile.")
            }
        }
    }
}

@Composable
fun ClearanceRow(item: ClearanceStatusItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isCleared) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
            contentDescription = if (item.isCleared) "Cleared" else "Not Cleared",
            tint = if (item.isCleared) Color(0xFF008000) else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(16.dp))
        Text(item.signatoryName, style = MaterialTheme.typography.bodyLarge)
    }
}