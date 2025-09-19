package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.FacultyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFacultyScreen(
    navController: NavController,
    facultyId: Int,
    facultyName: String,
    firstName: String,
    lastName: String,
    middleName: String?,
    username: String, // Correctly receive username here
    viewModel: FacultyViewModel = viewModel()
) {
    val context = LocalContext.current
    var newUsername by remember { mutableStateOf(username) } // Initialize with passed username
    var newPassword by remember { mutableStateOf("") } // Password is not passed, start with blank
    var newFirstName by remember { mutableStateOf(firstName) }
    var newLastName by remember { mutableStateOf(lastName) }
    var newMiddleName by remember { mutableStateOf(middleName ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit: $facultyName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = newUsername,
                onValueChange = { newUsername = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password (Leave blank to keep old)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            OutlinedTextField(
                value = newFirstName,
                onValueChange = { newFirstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newMiddleName,
                onValueChange = { newMiddleName = it },
                label = { Text("Middle Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newLastName,
                onValueChange = { newLastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.editFaculty(
                        id = facultyId,
                        username = newUsername,
                        password = newPassword,
                        firstName = newFirstName,
                        lastName = newLastName,
                        middleName = newMiddleName.ifBlank { null },
                        onSuccess = {
                            Toast.makeText(context, "Faculty updated successfully!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}