package com.synthesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.HomeViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvestigationScreen(
    onNavigateBack: () -> Unit,
    onInvestigationCreated: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NEW INVESTIGATION", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(DarkBackground).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("WHAT DO YOU WANT TO INVESTIGATE?", style = MaterialTheme.typography.headlineLarge, color = AccentCyan)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Investigation Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = TextSecondary
                )
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Scientific Question / Query") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = TextSecondary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { })
            )

            Button(
                onClick = {
                    if (title.isNotBlank() && query.isNotBlank()) {
                        val id = UUID.randomUUID().toString()
                        viewModel.createInvestigation(title, query)
                        onInvestigationCreated(id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("START INVESTIGATION", color = Color.Black)
            }
        }
    }
}
