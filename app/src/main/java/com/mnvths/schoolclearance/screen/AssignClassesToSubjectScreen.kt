package com.mnvths.schoolclearance.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mnvths.schoolclearance.AssignmentViewModel
import com.mnvths.schoolclearance.ClassSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignClassesToSubjectScreen(
    navController: NavController,
    facultyId: Int,
    facultyName: String,
    subjectId: Int,
    subjectName: String,
    viewModel: AssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val sections by viewModel.sections
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    // Track checkbox state
    val selectedSections = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        viewModel.fetchAllClassSections()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Classes to $subjectName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                else -> LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sections) { sectionData ->
                        val key = "${sectionData.gradeLevel}-${sectionData.sectionName}"
                        val checked = selectedSections[key] ?: false

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedSections[key] = isChecked
                                    if (isChecked) {
                                        // Assign class to faculty for subject
                                        viewModel.assignClassToFaculty(
                                            facultyId = facultyId,
                                            subjectId = subjectId,
                                            sectionId = sectionData.sectionId,
                                            onSuccess = {
                                                Toast.makeText(context, "Assigned Grade ${sectionData.gradeLevel}-${sectionData.sectionName}", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { errMsg ->
                                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                                selectedSections[key] = false
                                            }
                                        )


                                    }
                                }
                            )
                            Text(
                                text = "Grade ${sectionData.gradeLevel} - Section ${sectionData.sectionName}", // ✅ renamed
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
