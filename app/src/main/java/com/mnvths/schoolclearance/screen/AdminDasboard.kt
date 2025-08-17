// AdminDashboard.kt
package com.mnvths.schoolclearance.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mnvths.schoolclearance.OtherUser
import com.mnvths.schoolclearance.AdminNavGraph // IMPORT THE NEW FILE

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(user: OtherUser, onSignOut: () -> Unit, navController: NavController) {
    // This is the NavController for the NESTED navigation graph.
    val adminNavController = rememberNavController()

    val navBackStackEntry by adminNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
            TabRow(
                selectedTabIndex = when (currentRoute) {
                    "facultyList" -> 0
                    "studentManagement" -> 1
                    "subjects" -> 2
                    else -> 0
                }
            ) {
                Tab(
                    selected = currentRoute == "facultyList",
                    onClick = { adminNavController.navigate("facultyList") },
                    text = { Text("Faculty") }
                )
                Tab(
                    selected = currentRoute == "studentManagement",
                    onClick = { adminNavController.navigate("studentManagement") },
                    text = { Text("Students") }
                )
                Tab(
                    selected = currentRoute == "subjects",
                    onClick = { adminNavController.navigate("signatories") },
                    text = { Text("Signatories") }
                )
            }
            // The nested NavHost
            AdminNavGraph(navController = adminNavController)
        }
    }
}