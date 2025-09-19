package com.mnvths.schoolclearance.screen

// ✅ START: Add all of these required imports at the top of your file
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // <-- THIS IS THE CRITICAL IMPORT
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.AssignmentViewModel
// ✅ END: Required imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSubjectToSignatoryScreen(
    navController: NavController,
    signatoryId: Int,
    signatoryName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val allSubjects by viewModel.subjects
    val assignedSubjects by viewModel.assignedSubjects
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(Unit) {
        // NOTE: Make sure these functions exist and are named correctly in your AssignmentViewModel
        viewModel.fetchSubjects()
        viewModel.fetchAssignedSubjectsForSignatory(signatoryId)
    }

    val unassignedSubjects = allSubjects.filter { allSubject ->
        !assignedSubjects.any { it.subjectId == allSubject.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Subject to $signatoryName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                else -> LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(unassignedSubjects) { subject -> // This now works correctly
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = subject.name, style = MaterialTheme.typography.titleLarge)
                                Button(onClick = {
                                    // NOTE: Make sure this function is named correctly in your AssignmentViewModel
                                    viewModel.assignSubjectToSignatory(
                                        signatoryId = signatoryId,
                                        subjectId = subject.id,
                                        onSuccess = {
                                            Toast.makeText(context, "${subject.name} assigned to $signatoryName!", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        },
                                        onError = { errorMsg ->
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}