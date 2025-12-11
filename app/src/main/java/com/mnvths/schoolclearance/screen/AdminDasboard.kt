package com.mnvths.schoolclearance.screen

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mnvths.schoolclearance.DashboardNavGraph
import com.mnvths.schoolclearance.R
import com.mnvths.schoolclearance.data.AppSettings
import com.mnvths.schoolclearance.data.OtherUser
import com.mnvths.schoolclearance.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

// Define School Colors locally
private val SchoolBlue = Color(0xFF0038A8)
private val SchoolRed = Color(0xFFC62828)

private data class AdminScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val unselectedIcon: ImageVector // Added for better UX state
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    rootNavController: NavHostController,
    user: OtherUser,
    onSignOut: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val innerNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val appSettings by settingsViewModel.settings.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // improved screen definitions with outlined icons for inactive states
    val screens = listOf(
        AdminScreen("students_graph", "Students", Icons.Filled.People, Icons.Outlined.People),
        AdminScreen("sections_graph", "Sections", Icons.Filled.Groups, Icons.Outlined.Groups),
        AdminScreen("signatories_graph", "Signatories", Icons.Filled.SupervisorAccount, Icons.Outlined.SupervisorAccount),
        AdminScreen("subjects_graph", "Subjects", Icons.Filled.Book, Icons.Outlined.Book),
        AdminScreen("accounts_graph", "Accounts", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
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
                },
                onReportsClick = {
                    scope.launch { drawerState.close() }
                    rootNavController.navigate("reports")
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                // Modern CenterAlignedTopAppBar with Brand Color
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Admin Dashboard",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SchoolBlue
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    screens.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.icon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp
                                )
                            },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SchoolBlue,
                                selectedTextColor = SchoolBlue,
                                indicatorColor = SchoolBlue.copy(alpha = 0.1f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            onClick = {
                                innerNavController.navigate(screen.route) {
                                    popUpTo(innerNavController.graph.findStartDestination().id) {
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
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)) // Light gray background for content area
            ) {
                DashboardNavGraph(
                    innerNavController = innerNavController,
                    rootNavController = rootNavController,
                    appSettings = appSettings
                )
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
                    onSuccess = {
                        showSettingsDialog = false
                        Toast.makeText(context, "Settings Saved!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { errorMsg ->
                        Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
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
    onReportsClick: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White
    ) {
        // Custom Header with Brand Color
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SchoolBlue) // Brand Header
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.malasila),
                        contentDescription = "School Logo",
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = user.name.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = "Administrator",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Items
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, null, tint = SchoolBlue) },
            label = { Text("Academic Settings") }, // Renamed for clarity
            selected = false,
            onClick = onSettingsClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.ReceiptLong, null, tint = SchoolBlue) },
            label = { Text("Reports") },
            selected = false,
            onClick = onReportsClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f)) // Push logout to bottom
        Divider(modifier = Modifier.padding(horizontal = 16.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, null, tint = SchoolRed) }, // Red for logout
            label = { Text("Logout", color = SchoolRed) },
            selected = false,
            onClick = onSignOutClick,
            modifier = Modifier.padding(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Academic Term Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SchoolBlue
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // School Year Selector (Card Style)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active School Year", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledIconButton(
                                onClick = { schoolYear-- },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.Black)
                            }

                            Text(
                                "$schoolYear - ${schoolYear + 1}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = SchoolBlue)
                            )

                            FilledIconButton(
                                onClick = { schoolYear++ },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Black)
                            }
                        }
                    }
                }

                Divider()

                // Dropdowns
                var jhsExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = jhsExpanded, onExpandedChange = { jhsExpanded = it }) {
                    OutlinedTextField(
                        value = "Quarter $jhsQuarter",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Junior High (G7-10)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jhsExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SchoolBlue,
                            focusedLabelColor = SchoolBlue
                        )
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
                        label = { Text("Senior High (G11-12)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shsExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SchoolBlue,
                            focusedLabelColor = SchoolBlue
                        )
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
            Button(
                onClick = {
                    val newSettings = AppSettings(
                        activeSchoolYear = "$schoolYear-${schoolYear + 1}",
                        activeQuarterJhs = jhsQuarter,
                        activeSemesterShs = shsSemester
                    )
                    onSave(newSettings)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SchoolBlue)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)) {
                Text("Cancel")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}