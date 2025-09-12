package com.mnvths.schoolclearance.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mnvths.schoolclearance.AdminNavGraph
import com.mnvths.schoolclearance.OtherUser

// A data class to cleanly manage the properties for each tab
private data class AdminTab(val route: String, val title: String)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(user: OtherUser, onSignOut: () -> Unit) { // Removed unused NavController
    val adminNavController = rememberNavController()

    // List of tabs. The routes MUST match the graph routes in AdminNavGraph.
    val tabs = listOf(
        AdminTab(route = "students", title = "Students"),
        AdminTab(route = "faculty", title = "Faculty"),
        AdminTab(route = "signatories_graph", title = "Signatories") // Use a unique route for the graph
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
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

            // ✅ FIX: This logic checks the navigation hierarchy to find the correct tab.
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
            // The nested NavHost
            AdminNavGraph(navController = adminNavController)
        }
    }
}