package com.mnvths.schoolclearance.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.FacultyViewModel

@Composable
fun FacultyListScreen(
    viewModel: FacultyViewModel = viewModel(),
    navController: NavController
) {
    val facultyList by viewModel.facultyList
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(Unit) {
        viewModel.fetchFacultyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: $error\nPlease check your server and network connection.",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(facultyList) { faculty ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Clicking the name goes to the new details screen
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val middleName = faculty.middleName ?: "null"
                                        navController.navigate("facultyDetails/${faculty.id}/${faculty.name}/${faculty.firstName}/${faculty.lastName}/${middleName}")
                                    }
                            ) {
                                Text(text = "ID: ${faculty.id}", style = MaterialTheme.typography.bodySmall)
                                Text(text = faculty.name, style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Edit button remains
                            IconButton(onClick = {
                                val middleName = faculty.middleName ?: "null"
                                navController.navigate("editFaculty/${faculty.id}/${faculty.name}/${faculty.firstName}/${faculty.lastName}/${middleName}")
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Faculty")
                            }
                        }
                    }
                }
            }
        }
    }
}
