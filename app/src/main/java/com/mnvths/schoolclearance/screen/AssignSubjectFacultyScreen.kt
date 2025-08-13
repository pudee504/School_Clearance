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
import com.mnvths.schoolclearance.AssignmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSubjectToFacultyScreen(
    navController: NavController,
    facultyId: Int,
    facultyName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val subjects by viewModel.subjects
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    // Inputs for year level and section
    var yearLevel by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchSubjects()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Subject to $facultyName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // Input fields for year level and section
                OutlinedTextField(
                    value = yearLevel,
                    onValueChange = { yearLevel = it },
                    label = { Text("Year Level") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subjects) { subject ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subject.subjectName,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Button(onClick = {
                                    if (yearLevel.isBlank() || section.isBlank()) {
                                        Toast.makeText(context, "Please enter year level and section.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val yearLevelInt = yearLevel.toIntOrNull()
                                    if (yearLevelInt == null) {
                                        Toast.makeText(context, "Year level must be a number.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    viewModel.assignSubjectToFaculty(
                                        facultyId = facultyId,
                                        subjectId = subject.id,
                                        yearLevel = yearLevelInt,
                                        section = section.trim(),
                                        onSuccess = {
                                            Toast.makeText(context, "${subject.subjectName} assigned to $facultyName!", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        },
                                        onError = { errorMsg ->
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }) {
                                    Text("Assign")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}