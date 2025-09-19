package com.mnvths.schoolclearance.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mnvths.schoolclearance.data.StudentProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(student: StudentProfile, onSignOut: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Clearance Status") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        val schoolYear = student.clearanceStatus.firstOrNull()?.schoolYear ?: "N/A"
        val quarter = student.clearanceStatus.firstOrNull()?.quarter?.toString() ?: "N/A"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Name: ${student.name}", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Student ID: ${student.id}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Year Level: ${student.gradeLevel}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Section: ${student.section}", style = MaterialTheme.typography.bodyLarge)

            Text(text = "School Year: $schoolYear", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Quarter: $quarter", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Clearance Status:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(student.clearanceStatus) { item ->
                    // ✅ FIX 2: Provide a default value in case the name is null
                    ClearanceStatusItem(
                        signatoryName = item.signatoryName ?: "Unknown Signatory",
                        isCleared = item.isCleared
                    )
                }
            }
        }
    }
}