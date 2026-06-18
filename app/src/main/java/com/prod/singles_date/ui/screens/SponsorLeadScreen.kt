package com.prod.singles_date.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorLeadScreen(
    defaultCity: String,
    currentUid: String,
    userEmail: String,
    onSubmit: (businessName: String, email: String, city: String, budget: String, message: String, onDone: (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    var business by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(userEmail) }
    var city by remember { mutableStateOf(defaultCity) }
    var budget by remember { mutableStateOf("₹10,000") }
    var message by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advertise on Citysnap") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                "Reach people in specific localities with sponsored snaps. We'll contact you within 24 hours.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(business, { business = it }, label = { Text("Business name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(city, { city = it }, label = { Text("Target city") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(budget, { budget = it }, label = { Text("Monthly budget") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                message,
                { message = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    busy = true
                    onSubmit(business, email, city, budget, message) { ok ->
                        busy = false
                        sent = ok
                    }
                },
                enabled = !busy && business.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (sent) "Request sent!" else "Submit inquiry")
            }
        }
    }
}
