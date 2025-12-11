package com.mnvths.schoolclearance.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.FullyClearedStudent
import com.mnvths.schoolclearance.viewmodel.ReportsViewModel

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val SuccessGreen = Color(0xFF2E7D32)
private val BackgroundGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = viewModel()
) {
    val reportData by viewModel.reportData
    val gradeLevels by viewModel.gradeLevels
    val sections by viewModel.sections
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    var selectedGradeLevel by remember { mutableStateOf<Int?>(null) }
    var selectedSection by remember { mutableStateOf<Int?>(null) }
    var gradeLevelExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchFullyClearedStudents()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Clearance Report", fontWeight = FontWeight.Bold) },
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
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // --- Metric Header ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL CLEARED STUDENTS",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${reportData?.totalCount ?: 0}",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        //
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Based on current filters",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // --- Filters ---
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "FILTER RESULTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Grade Filter
                    ReportDropdown(
                        label = "Grade Level",
                        value = gradeLevels.find { it.id == selectedGradeLevel }?.name ?: "All Grades",
                        expanded = gradeLevelExpanded,
                        onExpandedChange = { gradeLevelExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Grade Levels") },
                            onClick = {
                                selectedGradeLevel = null
                                selectedSection = null
                                viewModel.fetchFullyClearedStudents()
                                gradeLevelExpanded = false
                            }
                        )
                        gradeLevels.forEach { gradeLevel ->
                            DropdownMenuItem(
                                text = { Text(gradeLevel.name) },
                                onClick = {
                                    selectedGradeLevel = gradeLevel.id
                                    selectedSection = null
                                    viewModel.fetchSectionsForGradeLevel(gradeLevel.id)
                                    viewModel.fetchFullyClearedStudents(gradeLevelId = gradeLevel.id)
                                    gradeLevelExpanded = false
                                }
                            )
                        }
                    }

                    // Section Filter
                    ReportDropdown(
                        label = "Section",
                        value = sections.find { it.sectionId == selectedSection }?.sectionName ?: "All Sections",
                        expanded = sectionExpanded,
                        onExpandedChange = { sectionExpanded = it },
                        modifier = Modifier.weight(1f),
                        enabled = selectedGradeLevel != null
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Sections") },
                            onClick = {
                                selectedSection = null
                                viewModel.fetchFullyClearedStudents(gradeLevelId = selectedGradeLevel)
                                sectionExpanded = false
                            }
                        )
                        sections.forEach { section ->
                            DropdownMenuItem(
                                text = { Text(section.sectionName) },
                                onClick = {
                                    selectedSection = section.sectionId
                                    viewModel.fetchFullyClearedStudents(gradeLevelId = selectedGradeLevel, sectionId = section.sectionId)
                                    sectionExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- List ---
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SchoolBlue)
                    }
                    error != null -> {
                        Text(text = "Error: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                    }
                    reportData?.students?.isEmpty() == true -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.SearchOff, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No fully cleared students found.", color = Color.Gray)
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(reportData?.students ?: emptyList()) { student ->
                                StudentReportCard(student = student)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Helper Components ---

@Composable
fun StudentReportCard(student: FullyClearedStudent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SchoolBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${student.firstName.take(1)}${student.lastName.take(1)}",
                    color = SchoolBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${student.lastName}, ${student.firstName} ${student.middleName?.firstOrNull()?.plus(".") ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${student.studentId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Success Badge
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Cleared",
                tint = SuccessGreen
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if(enabled) onExpandedChange(!expanded) },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SchoolBlue,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = SchoolBlue
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(Color.White),
            content = content
        )
    }
}