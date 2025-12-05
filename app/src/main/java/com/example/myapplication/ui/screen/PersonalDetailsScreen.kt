package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsScreen(
    modifier: Modifier = Modifier,
    user: User,
    onNext: (User) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    var age by remember { mutableStateOf(user.age?.toString() ?: "") }
    var caste by remember { mutableStateOf(user.caste ?: "") }
    var gotr by remember { mutableStateOf(user.gotr ?: "") }
    var category by remember { mutableStateOf(user.category ?: "") }
    var religion by remember { mutableStateOf(user.religion ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = caste,
            onValueChange = { caste = it },
            label = { Text("Caste") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = gotr,
            onValueChange = { gotr = it },
            label = { Text("Gotra") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = religion,
            onValueChange = { religion = it },
            label = { Text("Religion") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val updatedUser = user.copy(
                    age = age.toIntOrNull(),
                    caste = caste.takeIf { it.isNotBlank() },
                    gotr = gotr.takeIf { it.isNotBlank() },
                    category = category.takeIf { it.isNotBlank() },
                    religion = religion.takeIf { it.isNotBlank() }
                )
                viewModel.updateUser(updatedUser)
                onNext(updatedUser)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }
    }
}
