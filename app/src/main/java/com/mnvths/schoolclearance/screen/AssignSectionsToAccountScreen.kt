package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.FolderOpen
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
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.AssignmentViewModel

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSectionsToAccountScreen(
    navController: NavController,
    signatoryId: Int,
    accountId: Int,
    accountName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val availableSections by viewModel.availableSections
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    // Selection State
    val selectedSections = remember { mutableStateListOf<ClassSection>() }

    // Initial Load
    LaunchedEffect(signatoryId, accountId) {
        viewModel.loadAvailableSectionsForAccount(signatoryId, accountId)
    }

    // Grouping Logic: Organize by Grade Level
    val groupedSections = remember(availableSections) {
        availableSections.groupBy { it.gradeLevel }
            .toSortedMap(compareBy { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Select Sections", fontWeight = FontWeight.Bold)
                        Text(
                            text = "for $accountName",
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
                visible = selectedSections.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val selectedIds = selectedSections.map { it.sectionId }
                        viewModel.assignSectionsToAccount(
                            signatoryId = signatoryId,
                            accountId = accountId,
                            sectionIds = selectedIds,
                            onSuccess = {
                                Toast.makeText(context, "${selectedSections.size} sections assigned!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    containerColor = SchoolBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Check, null) },
                    text = { Text("Assign (${selectedSections.size})") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SchoolBlue)
                }
            } else if (error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Error: $error",
                        color = SchoolRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (availableSections.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.FolderOpen, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("All sections are already assigned.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedSections.forEach { (gradeLevel, sections) ->
                        // Grade Level Header
                        item {
                            Text(
                                text = gradeLevel.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )
                        }

                        // Section List
                        items(sections, key = { it.sectionId }) { section ->
                            val isSelected = selectedSections.contains(section)
                            SelectableAccountSectionCard(
                                section = section,
                                isSelected = isSelected,
                                onToggle = {
                                    if (isSelected) selectedSections.remove(section)
                                    else selectedSections.add(section)
                                }
                            )
                        }
                    }

                    // Space for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// Custom Card for Account Selection context
@Composable
fun SelectableAccountSectionCard(
    section: ClassSection,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val backgroundColor by animateColorAsState(if (isSelected) SchoolBlue.copy(alpha = 0.05f) else Color.White)
    val borderColor by animateColorAsState(if (isSelected) SchoolBlue else Color.Transparent)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isSelected) 2.dp else 0.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon distinguishing Account assignment
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = if (isSelected) SchoolBlue else Color.Gray
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.sectionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) SchoolBlue else Color.Black
                )
                Text(
                    text = section.gradeLevel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SchoolBlue,
                    uncheckedColor = Color.LightGray
                )
            )
        }
    }
}