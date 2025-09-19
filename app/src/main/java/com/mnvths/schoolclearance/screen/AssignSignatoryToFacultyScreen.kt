package com.mnvths.schoolclearance.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.AssignmentViewModel
import kotlin.collections.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSignatoryToFacultyScreen(
    navController: NavController,
    facultyId: Int,
    facultyName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val allSignatories by viewModel.signatories
    val assignedSignatories by viewModel.assignedSignatories
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    // Fetch both lists on load
    LaunchedEffect(Unit) {
        viewModel.fetchSignatories()
        viewModel.fetchAssignedSignatoriesForFaculty(facultyId)
    }

    // Filter the list of all signatories to remove those already assigned
    val unassignedSignatories = allSignatories.filter { allSignatory ->
        !assignedSignatories.any { it.signatoryId == allSignatory.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Signatory to $facultyName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                    items(unassignedSignatories) { signatory ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = signatory.signatoryName, style = MaterialTheme.typography.titleLarge)
                                Button(onClick = {
                                    viewModel.assignSignatoryToFaculty(
                                        facultyId = facultyId,
                                        signatoryId = signatory.id,
                                        onSuccess = {
                                            Toast.makeText(context, "${signatory.signatoryName} assigned to $facultyName!", Toast.LENGTH_SHORT).show()
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