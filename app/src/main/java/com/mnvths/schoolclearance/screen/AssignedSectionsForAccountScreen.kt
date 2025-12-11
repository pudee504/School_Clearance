package com.mnvths.schoolclearance.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.data.ClassSection
import com.mnvths.schoolclearance.viewmodel.SignatoryViewModel

// Brand Colors
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)
private val BackgroundGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignedSectionsForAccountScreen(
    navController: NavController,
    signatoryId: Int,
    accountId: Int,
    accountName: String,
    destinationRoute: String,
    showFab: Boolean = true,
    viewModel: SignatoryViewModel = viewModel()
) {
    val sections by viewModel.sectionsForAccount
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(signatoryId, accountId) {
        viewModel.fetchSectionsForAccount(signatoryId, accountId)
    }

    // Grouping Logic: Keep the list organized by Grade
    val groupedSections = remember(sections) {
        sections.groupBy { it.gradeLevel }
            .toSortedMap(compareBy { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(accountName, fontWeight = FontWeight.Bold)
                        Text(
                            "Assigned Classes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
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
            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate("assignSectionsToAccount/${signatoryId}/${accountId}/${accountName}")
                    },
                    containerColor = SchoolBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Add Class") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SchoolBlue)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: $error",
                        color = SchoolRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (sections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.FolderOpen, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No classes assigned yet.", color = Color.Gray)
                        if (showFab) {
                            Text("Tap 'Add Class' to assign sections.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedSections.forEach { (gradeLevel, classSections) ->
                        // Grade Header
                        item {
                            Text(
                                text = gradeLevel.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )
                        }

                        // Sections Cards
                        items(classSections, key = { it.sectionId }) { section ->
                            AccountSectionListItem(
                                section = section,
                                onClick = {
                                    val route = "clearanceScreenAccount/${section.sectionId}/${accountId}/${section.sectionName}/${accountName}/${section.gradeLevel}"
                                    navController.navigate(route)
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

// Renamed to ensure no conflict with previous screen helper
@Composable
fun AccountSectionListItem(section: ClassSection, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SchoolBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AccountBalance, null, tint = SchoolBlue)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.sectionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage Clearance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Icon(Icons.Filled.ChevronRight, null, tint = Color.Gray)
        }
    }
}