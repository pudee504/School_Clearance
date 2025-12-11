package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack // ✅ Fixed import
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.SubjectViewModel

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

// --- Screen 1: Grade Level Selection ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumHomeScreen(
    navController: NavController,
    viewModel: SubjectViewModel = viewModel()
) {
    val gradeLevels by viewModel.gradeLevels.collectAsState()
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(Unit) {
        viewModel.fetchGradeLevels()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Curriculum", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = SchoolBlue
                )
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "Select Grade Level",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SchoolBlue)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: $error", color = SchoolRed)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(gradeLevels, key = { it.id }) { gradeLevel ->
                        GradeLevelCard(
                            name = gradeLevel.name,
                            onClick = {
                                navController.navigate("curriculumManagement/${gradeLevel.id}/${gradeLevel.name}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GradeLevelCard(name: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SchoolBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.School, null, tint = SchoolBlue)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Icon(Icons.Filled.ChevronRight, null, tint = Color.Gray)
        }
    }
}

// --- Screen 2: Subject Management ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumManagementScreen(
    navController: NavController,
    gradeLevelId: Int,
    gradeLevelName: String,
    viewModel: SubjectViewModel = viewModel()
) {
    val subjects by viewModel.managementSubjects.collectAsState()
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf<Int?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedSemester by remember { mutableStateOf(1) }
    var expandedMenuRequirementId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(gradeLevelId, selectedSemester) {
        val semesterToFetch = if (gradeLevelId > 4) selectedSemester else 1
        viewModel.fetchSubjectsForGradeLevel(gradeLevelId, semesterToFetch)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(gradeLevelName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        // ✅ FIX: Use Default instead of Automirrored
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SchoolBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add Subject") }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Semester Tabs (Only for Senior High / Grade 11-12 logic)
            if (gradeLevelId > 4) {
                TabRow(
                    selectedTabIndex = selectedSemester - 1,
                    containerColor = Color.White,
                    contentColor = SchoolBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSemester - 1]),
                            color = SchoolBlue
                        )
                    }
                ) {
                    listOf("1st Semester", "2nd Semester").forEachIndexed { index, title ->
                        Tab(
                            selected = (selectedSemester - 1) == index,
                            onClick = { selectedSemester = index + 1 },
                            text = { Text(title, fontWeight = if((selectedSemester - 1) == index) FontWeight.Bold else FontWeight.Normal) },
                            selectedContentColor = SchoolBlue,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SchoolBlue)
                } else if (error != null) {
                    Text("Error: $error", modifier = Modifier.align(Alignment.Center), color = SchoolRed)
                } else if (subjects.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Book, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No subjects found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "Active Subjects",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                            )
                        }

                        items(subjects, key = { it.requirementId }) { subject ->
                            val nonRemovableSubjectIds = listOf(87, 88, 89)
                            val isLocked = subject.subjectId in nonRemovableSubjectIds

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if(isLocked) Icons.Outlined.Lock else Icons.Outlined.Class,
                                        contentDescription = null,
                                        tint = if(isLocked) Color.Gray else SchoolBlue
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = subject.subjectName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isLocked) {
                                            Text(
                                                text = "Mandatory Requirement",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    if (!isLocked) {
                                        Box {
                                            IconButton(onClick = { expandedMenuRequirementId = subject.requirementId }) {
                                                Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray)
                                            }
                                            DropdownMenu(
                                                expanded = expandedMenuRequirementId == subject.requirementId,
                                                onDismissRequest = { expandedMenuRequirementId = null },
                                                modifier = Modifier.background(Color.White)
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Remove", color = SchoolRed) },
                                                    onClick = {
                                                        showRemoveDialog = subject.requirementId
                                                        expandedMenuRequirementId = null
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = SchoolRed) }
                                                )
                                            }
                                        }
                                    } else {
                                        // Visual indicator that it's locked
                                        Icon(Icons.Outlined.Lock, "Locked", tint = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    if (showAddDialog) {
        var newSubjectName by remember { mutableStateOf("") }
        var newSubjectCode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Add a new subject to $gradeLevelName.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    OutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        label = { Text("Subject Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    OutlinedTextField(
                        value = newSubjectCode,
                        onValueChange = { newSubjectCode = it },
                        label = { Text("Subject Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubjectName.isNotBlank() && newSubjectCode.isNotBlank()) {
                            isSubmitting = true
                            viewModel.addSubjectToCurriculum(
                                name = newSubjectName.trim(),
                                gradeLevelId = gradeLevelId,
                                semester = selectedSemester,
                                subjectCode = newSubjectCode.trim(),
                                onSuccess = {
                                    Toast.makeText(context, "Subject added!", Toast.LENGTH_SHORT).show()
                                    isSubmitting = false
                                    showAddDialog = false
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    isSubmitting = false
                                }
                            )
                        }
                    },
                    enabled = !isSubmitting && newSubjectName.isNotBlank() && newSubjectCode.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolBlue)
                ) {
                    if(isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White) else Text("Add")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
            containerColor = Color.White
        )
    }

    if (showRemoveDialog != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            icon = { Icon(Icons.Default.Delete, null, tint = SchoolRed) },
            title = { Text("Remove Subject?") },
            text = { Text("This will remove the subject from this curriculum level. Existing grades/clearances for students might be affected.", textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.setRequirementStatus(
                            requirementId = showRemoveDialog!!,
                            status = "inactive",
                            gradeLevelId = gradeLevelId,
                            semester = selectedSemester,
                            onSuccess = {
                                Toast.makeText(context, "Subject removed.", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                showRemoveDialog = null
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                isSubmitting = false
                            }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = SchoolRed)
                ) {
                    if(isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White) else Text("Remove")
                }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = null }) { Text("Cancel") } },
            containerColor = Color.White
        )
    }
}