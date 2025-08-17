package com.mnvths.schoolclearance.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.StudentManagementViewModel
import com.mnvths.schoolclearance.ClassSection
import java.time.Year

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentManagementScreen(
    navController: NavController,
    viewModel: StudentManagementViewModel = viewModel()
) {
    val sections by viewModel.sections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // State for the school year and quarter
    var startYear by remember { mutableStateOf(Year.now().value) }
    var quarter by remember { mutableIntStateOf(1) }

    // State to manage whether the selectors are in editing mode
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // School Year and Quarter selectors
        SchoolYearAndQuarterSelectors(
            startYear = startYear,
            onYearChange = { newYear -> startYear = newYear },
            quarter = quarter,
            onQuarterChange = { newQuarter -> quarter = newQuarter },
            isEditing = isEditing,
            onEditClick = { isEditing = true },
            onSaveChanges = {
                // Here, you would implement the logic to save the new school year and quarter
                // For example, calling a function in the ViewModel
                // viewModel.updateSchoolYearAndQuarter(startYear, quarter)

                // After saving, turn off editing mode
                isEditing = false
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Add Section Button
        Button(
            onClick = { navController.navigate("addSection") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Section")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Section")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Sections:", style = MaterialTheme.typography.titleLarge)
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
                items(sections) { section ->
                    SectionItem(section = section, navController = navController)
                }
            }
        }
    }
}

// Separate composable for the school year and quarter selectors
@Composable
fun SchoolYearAndQuarterSelectors(
    startYear: Int,
    onYearChange: (Int) -> Unit,
    quarter: Int,
    onQuarterChange: (Int) -> Unit,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onSaveChanges: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Display text for the current school year and quarter
                Column {
                    Text(
                        text = "School Year: ${startYear}-${startYear + 1}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Quarter: $quarter",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Edit/Done Button
                IconButton(onClick = {
                    if (isEditing) {
                        // When in editing mode, clicking this button saves the changes
                        onSaveChanges()
                    } else {
                        // When not in editing mode, clicking this button starts the editing process
                        onEditClick()
                    }
                }) {
                    if (isEditing) {
                        // Use a "Done" icon when in editing mode
                        Icon(Icons.Filled.Done, contentDescription = "Save Changes")
                    } else {
                        // Use an "Edit" icon when not in editing mode
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                }
            }

            // Only show the selectors and the Save button when in editing mode
            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))

                // School Year Selector (visible only when editing)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { onYearChange(startYear - 1) }) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Decrement year")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onYearChange(startYear + 1) }) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Increment year")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quarter Selector (visible only when editing)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (q in 1..4) {
                        FilterChip(
                            selected = quarter == q,
                            onClick = { onQuarterChange(q) },
                            label = { Text(q.toString(), textAlign = TextAlign.Center) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionItem(section: ClassSection, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("studentList/${section.sectionId}/${section.gradeLevel}/${section.sectionName}")
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "${section.gradeLevel} - ${section.sectionName}", style = MaterialTheme.typography.titleMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /* TODO: Implement edit section */ }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Section")
                }
                // TODO: Add a delete button
            }
        }
    }
}