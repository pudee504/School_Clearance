package com.mnvths.schoolclearance.screen

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong // ✅ ADD THIS IMPORT
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mnvths.schoolclearance.AdminNavGraph
import com.mnvths.schoolclearance.R
import com.mnvths.schoolclearance.data.AppSettings
import com.mnvths.schoolclearance.data.OtherUser
import com.mnvths.schoolclearance.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

private data class AdminScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    user: OtherUser,
    onSignOut: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val adminNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val appSettings by settingsViewModel.settings.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val screens = listOf(
        AdminScreen(route = "students_graph", title = "Students", icon = Icons.Filled.People),
        AdminScreen(route = "sections_graph", title = "Sections", icon = Icons.Filled.Groups),
        AdminScreen(route = "signatories_graph", title = "Signatories", icon = Icons.Filled.SupervisorAccount),
        AdminScreen(route = "subjects_graph", title = "Subjects", icon = Icons.Filled.Book),
        // ✅ ADD THIS NEW LINE FOR THE ACCOUNTS TAB
        AdminScreen(route = "accounts_graph", title = "Accounts", icon = Icons.Filled.ReceiptLong)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                user = user,
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    showSettingsDialog = true
                },
                onSignOutClick = {
                    scope.launch { drawerState.close() }
                    onSignOut()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by adminNavController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                adminNavController.navigate(screen.route) {
                                    popUpTo(adminNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                AdminNavGraph(navController = adminNavController)
            }
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

@Composable
fun AdminDrawerContent(
    user: OtherUser,
    onSettingsClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.malasila),
                contentDescription = "School Logo",
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 16.dp)
            )
            Text(
                text = "Welcome,",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = user.name ?: "Administrator",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        Divider()
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Set Academic Term") },
            label = { Text("Set Academic Term") },
            selected = false,
            onClick = onSettingsClick
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") },
            label = { Text("Logout") },
            selected = false,
            onClick = onSignOutClick
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("School Year:", style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { schoolYear-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                        Text("$schoolYear-${schoolYear + 1}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { schoolYear++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                    }
                }
                Divider()
                var jhsExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = jhsExpanded, onExpandedChange = { jhsExpanded = it }) {
                    OutlinedTextField(
                        value = "Quarter $jhsQuarter",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("For JHS (G7-10)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jhsExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
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
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}