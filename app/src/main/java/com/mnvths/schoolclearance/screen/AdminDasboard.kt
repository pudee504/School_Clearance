package com.mnvths.schoolclearance.screen

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mnvths.schoolclearance.AdminNavGraph
import com.mnvths.schoolclearance.data.AppSettings
import com.mnvths.schoolclearance.data.OtherUser
import com.mnvths.schoolclearance.viewmodel.SettingsViewModel
import java.util.Calendar

private data class AdminTab(val route: String, val title: String)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    user: OtherUser,
    onSignOut: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val adminNavController = rememberNavController()

    val appSettings by settingsViewModel.settings.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        AdminTab(route = "students_graph", title = "Students"),
        AdminTab(route = "sections_graph", title = "Sections"),
        AdminTab(route = "faculty_graph", title = "Faculty"),
        AdminTab(route = "signatories_graph", title = "Signatories")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val navBackStackEntry by adminNavController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val selectedTabIndex = tabs.indexOfFirst { tab ->
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
            }.coerceAtLeast(0)

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == selectedTabIndex,
                        onClick = {
                            adminNavController.navigate(tab.route) {
                                popUpTo(adminNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        text = { Text(tab.title) }
                    )
                }
            }
            AdminNavGraph(navController = adminNavController)
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = appSettings,
            onDismiss = { showSettingsDialog = false },
            onSave = { newSettings ->
                settingsViewModel.updateSettings(
                    newSettings = newSettings,
                    onSuccess = { showSettingsDialog = false },
                    onError = { /* Handle error if needed */ }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit
) {
    var schoolYear by remember { mutableStateOf(currentSettings.activeSchoolYear.split("-").first().toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)) }
    var jhsQuarter by remember { mutableStateOf(currentSettings.activeQuarterJhs) }
    var shsSemester by remember { mutableStateOf(currentSettings.activeSemesterShs) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Active Term") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("School Year:", modifier = Modifier.weight(1f))
                    IconButton(onClick = { schoolYear-- }) { Icon(Icons.Default.KeyboardArrowLeft, null) }
                    Text("$schoolYear-${schoolYear + 1}")
                    IconButton(onClick = { schoolYear++ }) { Icon(Icons.Default.KeyboardArrowRight, null) }
                }

                var jhsExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = jhsExpanded, onExpandedChange = { jhsExpanded = it }) {
                    OutlinedTextField(
                        value = "Quarter $jhsQuarter",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("For JHS (G7-10)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jhsExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = jhsExpanded, onDismissRequest = { jhsExpanded = false }) {
                        (1..4).forEach { q ->
                            DropdownMenuItem(text = { Text("Quarter $q") }, onClick = {
                                jhsQuarter = q.toString()
                                jhsExpanded = false
                            })
                        }
                    }
                }

                var shsExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = shsExpanded, onExpandedChange = { shsExpanded = it }) {
                    OutlinedTextField(
                        value = if (shsSemester == "1") "1st Semester" else "2nd Semester",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("For SHS (G11-12)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shsExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = shsExpanded, onDismissRequest = { shsExpanded = false }) {
                        DropdownMenuItem(text = { Text("1st Semester") }, onClick = {
                            shsSemester = "1"
                            shsExpanded = false
                        })
                        DropdownMenuItem(text = { Text("2nd Semester") }, onClick = {
                            shsSemester = "2"
                            shsExpanded = false
                        })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newSettings = AppSettings(
                    activeSchoolYear = "$schoolYear-${schoolYear + 1}",
                    activeQuarterJhs = jhsQuarter,
                    activeSemesterShs = shsSemester
                )
                onSave(newSettings)
                Toast.makeText(context, "Settings Saved!", Toast.LENGTH_SHORT).show()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}