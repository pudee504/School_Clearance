package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.Subject
import com.mnvths.schoolclearance.viewmodel.AssignmentViewModel

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

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

    val selectedSubjects = remember { mutableStateListOf<Subject>() }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadSubjectAssignmentData(signatoryId)
    }

    // Filter logic: Only show subjects NOT yet assigned
    val unassignedSubjects = remember(allSubjects, assignedSubjects) {
        allSubjects.filter { allSub ->
            assignedSubjects.none { it.subjectId == allSub.id }
        }.sortedBy { it.name }
    }

    // Search filter
    val filteredSubjects = remember(searchQuery, unassignedSubjects) {
        if (searchQuery.isBlank()) {
            unassignedSubjects
        } else {
            unassignedSubjects.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Assign Subjects", fontWeight = FontWeight.Bold)
                        Text(
                            text = "to $signatoryName",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = SchoolBlue,
                    navigationIconContentColor = SchoolBlue
                )
            )
        },
        containerColor = BackgroundGray,
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedSubjects.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.assignMultipleSubjectsToSignatory(
                            signatoryId = signatoryId,
                            subjects = selectedSubjects.toList(),
                            onSuccess = {
                                Toast.makeText(context, "${selectedSubjects.size} subjects assigned!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    containerColor = SchoolBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Check, null) },
                    text = { Text("Assign (${selectedSubjects.size})") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar Area
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search available subjects...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SchoolBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SchoolBlue)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = SchoolRed)
                }
            } else if (filteredSubjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Book, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "All subjects are already assigned." else "No subjects match your search.",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Available Subjects",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }
                    items(filteredSubjects, key = { it.id }) { subject ->
                        val isSelected = selectedSubjects.contains(subject)
                        SelectableSubjectCard(
                            subject = subject,
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) selectedSubjects.remove(subject)
                                else selectedSubjects.add(subject)
                            }
                        )
                    }
                    // Space for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// Helper Component for Selection
@Composable
fun SelectableSubjectCard(
    subject: Subject,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val backgroundColor by animateColorAsState(if (isSelected) SchoolBlue.copy(alpha = 0.05f) else Color.White)
    val borderColor by animateColorAsState(if (isSelected) SchoolBlue else Color.Transparent)

    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isSelected) 2.dp else 0.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) SchoolBlue else Color.Black
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SchoolBlue,
                    uncheckedColor = Color.Gray
                )
            )
        }
    }
}