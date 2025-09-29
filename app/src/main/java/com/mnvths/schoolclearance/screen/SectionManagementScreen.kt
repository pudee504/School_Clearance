package com.mnvths.schoolclearance.screen

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.SectionManagementViewModel
import androidx.navigation.compose.currentBackStackEntryAsState

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionManagementScreen(
    rootNavController: NavController,
    viewModel: SectionManagementViewModel = viewModel()
) {
    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        viewModel.fetchClassSections()
    }

    val sections by viewModel.classSections.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var expandedGradeLevel by remember { mutableStateOf<String?>(null) }

    val groupedSections = remember(sections) {
        sections.groupBy { it.gradeLevel }
            .toSortedMap(compareBy { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 })
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { rootNavController.navigate("addSection") },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Section")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Sections", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Text(text = "Tap section to view students", style = MaterialTheme.typography.bodySmall)
            }
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
                    groupedSections.forEach { (gradeLevel, sectionsInGroup) ->
                        val isExpanded = expandedGradeLevel == gradeLevel

                        item {
                            Card(
                                onClick = {
                                    expandedGradeLevel = if (isExpanded) null else gradeLevel
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = gradeLevel,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        modifier = Modifier.rotate(rotationAngle)
                                    )
                                }
                            }
                        }

                        item {
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    sectionsInGroup.sortedBy { it.sectionName }.forEach { section ->
                                        // This Card is now the clickable item to see students
                                        Card(
                                            onClick = {
                                                rootNavController.navigate("sectionStudents/${section.sectionId}/${section.sectionName}/${section.gradeLevel}")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = section.sectionName,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = "View students"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}