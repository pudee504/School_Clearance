package com.mnvths.schoolclearance.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.SectionManagementViewModel

// Theme Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

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

    // Control which grade is expanded
    var expandedGradeLevel by remember { mutableStateOf<String?>(null) }

    // Logic to group sections
    val groupedSections = remember(sections) {
        sections.groupBy { it.gradeLevel }
            .toSortedMap(compareBy { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Section Management", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = SchoolBlue
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { rootNavController.navigate("addSection") },
                containerColor = SchoolBlue,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Section") }
            )
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Header Info
            Card(
                colors = CardDefaults.cardColors(containerColor = SchoolBlue.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SchoolBlue.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Class, null, tint = SchoolBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Academic Structure", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SchoolBlue)
                        Text("Tap a Grade Level to view or manage sections.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Area
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SchoolBlue)
                }
            } else if (error != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = SchoolRed, modifier = Modifier.size(48.dp))
                    Text(text = "Error loading sections", color = Color.Gray)
                }
            } else if (sections.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.FolderOpen, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "No sections created yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    groupedSections.forEach { (gradeLevel, sectionsInGroup) ->
                        val isExpanded = expandedGradeLevel == gradeLevel

                        item {
                            GradeLevelHeader(
                                gradeLevel = gradeLevel,
                                sectionCount = sectionsInGroup.size,
                                isExpanded = isExpanded,
                                onClick = { expandedGradeLevel = if (isExpanded) null else gradeLevel }
                            )
                        }

                        item {
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (sectionsInGroup.isEmpty()) {
                                        Text(
                                            "No sections in this grade.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    } else {
                                        sectionsInGroup.sortedBy { it.sectionName }.forEach { section ->
                                            SectionItemCard(
                                                section = section,
                                                onClick = {
                                                    rootNavController.navigate("sectionStudents/${section.sectionId}/${section.sectionName}/${section.gradeLevel}")
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp)) // Breathing room after list
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Helper Composables ---

@Composable
fun GradeLevelHeader(
    gradeLevel: String,
    sectionCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(if (isExpanded) SchoolBlue else Color.White)
    val contentColor by animateColorAsState(if (isExpanded) Color.White else Color.Black)
    val rotationAngle by animateFloatAsState(if (isExpanded) 180f else 0f)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isExpanded) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                contentDescription = null,
                tint = if (isExpanded) Color.White else SchoolBlue
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gradeLevel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = "$sectionCount Section${if(sectionCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand",
                modifier = Modifier.rotate(rotationAngle),
                tint = contentColor
            )
        }
    }
}

@Composable
fun SectionItemCard(
    section: ClassSection,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Little dot connector visual
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SchoolBlue.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = section.sectionName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "View Students",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.Gray
            )
        }
    }
}