// In ClearanceScreen.kt
package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.ClearanceViewModel
import com.mnvths.schoolclearance.viewmodel.StudentClearanceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearanceScreen(
    navController: NavController,
    sectionId: Int,
    subjectId: Int,
    gradeLevel: String,
    sectionName: String,
    subjectName: String,
    viewModel: ClearanceViewModel = viewModel()
) {
    val students by viewModel.students
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val context = LocalContext.current

    // State for the search bar
    var searchText by remember { mutableStateOf("") }

    // This derived state will filter and sort the list automatically
    val processedStudents = remember(students, searchText) {
        students
            .filter { student ->
                val fullName = "${student.lastName}, ${student.firstName}"
                fullName.contains(searchText, ignoreCase = true) || student.studentId.contains(searchText, ignoreCase = true)
            }
            .sortedWith(
                // This sorts by "isCleared" status first (false comes before true),
                // then alphabetically by last name.
                compareBy<StudentClearanceStatus> { it.isCleared }
                    .thenBy { it.lastName }
                    .thenBy { it.firstName }
            )
    }

    LaunchedEffect(Unit) {
        viewModel.fetchStudentClearanceStatus(sectionId, subjectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // ✅ This is the updated title text
                        Text(text = "$subjectName Clearance")
                        Text(
                            // ✅ This is the updated subtitle text
                            text = "Section: $gradeLevel - $sectionName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))


            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by Name or ID") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // "Clear All" Button
            Button(
                onClick = {
                    viewModel.clearAllNotClearedStudents(
                        subjectId = subjectId,
                        sectionId = sectionId,
                        onSuccess = {
                            Toast.makeText(context, "All students cleared.", Toast.LENGTH_SHORT).show()
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                // Only enable the button if there are students who are not cleared
                enabled = students.any { !it.isCleared }
            ) {
                Text("Clear All Not Cleared")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) { /* ... Loading indicator remains the same ... */ }
            else if (error != null) { /* ... Error text remains the same ... */ }
            else if (students.isEmpty()) { /* ... "No students found" text remains the same ... */ }
            else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Use the new processedStudents list
                    items(processedStudents) { student ->
                        StudentClearanceItem(
                            student = student,
                            onStatusChange = { newStatus ->
                                viewModel.updateStudentClearance(
                                    userId = student.userId,
                                    subjectId = subjectId,
                                    sectionId = sectionId,
                                    isCleared = newStatus,
                                    onSuccess = {
                                        val statusText = if (newStatus) "cleared" else "marked as not cleared"
                                        Toast.makeText(context, "${student.lastName} $statusText.", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentClearanceItem(
    student: StudentClearanceStatus,
    onStatusChange: (Boolean) -> Unit // This lambda now passes the new status
) {
    val formattedName = "${student.lastName}, ${student.firstName}" +
            (student.middleName?.takeIf { it.isNotBlank() }?.let { " ${it.first()}." } ?: "")

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = formattedName, fontWeight = FontWeight.Bold)
                Text(text = "ID: ${student.studentId}", style = MaterialTheme.typography.bodySmall)
            }
            // The button now changes based on the student's current status
            if (student.isCleared) {
                // If cleared, show an "Undo" button
                OutlinedButton(onClick = { onStatusChange(false) }) {
                    Text("Undo")
                }
            } else {
                // If not cleared, show the "Clear" button
                Button(onClick = { onStatusChange(true) }) {
                    Text("Clear")
                }
            }
        }
    }
}