package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.viewmodel.SubjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubjectScreen( // ✅ RENAMED function
    navController: NavController,
    subjectId: Int?, // ✅ RENAMED parameter
    initialName: String?,
    viewModel: SubjectViewModel = viewModel() // ✅ Use the correct ViewModel
) {
    val context = LocalContext.current
    val isEditing = subjectId != null // ✅ Use renamed parameter
    var name by remember { mutableStateOf(initialName ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Subject" else "Add Subject") }, // ✅ Updated title
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Subject Name (e.g., Mathematics)") }, // ✅ Updated label
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isEditing) {
                        viewModel.updateSubject( // ✅ Call correct ViewModel function
                            id = subjectId!!, // ✅ Use renamed parameter
                            name = name,
                            onSuccess = {
                                Toast.makeText(context, "Updated successfully.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show() }
                        )
                    } else {
                        viewModel.addSubject( // ✅ Call correct ViewModel function
                            name = name,
                            onSuccess = {
                                Toast.makeText(context, "Added successfully.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show() }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}