package com.example.myapplication.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Favourite
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel

@Composable
fun FavouritesScreen(modifier: Modifier = Modifier, viewModel: LoginViewModel = viewModel(), onBack: () -> Unit = {}) {
    val favourites by viewModel.favourites.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "My Favourites",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(favourites) { favourite ->
                FavouriteItem(
                    favourite = favourite,
                    viewModel = viewModel,
                    onClick = {
                         viewModel.selectUser(favourite.favouritedUser)
                    }
                )
            }
        }
    }
}

@Composable
fun FavouriteItem(
    favourite: Favourite,
    viewModel: LoginViewModel,
    onClick: () -> Unit
) {
    val loginState by viewModel.loginState.collectAsState()
    val currentUser = (loginState as? LoginState.Success)?.user

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "User: ${favourite.favouritedUser.name}", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Email: ${favourite.favouritedUser.email}", style = MaterialTheme.typography.bodyMedium)
                favourite.favouritedUser.age?.let { Text(text = "Age: $it", style = MaterialTheme.typography.bodyMedium) }
                favourite.favouritedUser.gender?.let { Text(text = "Gender: $it", style = MaterialTheme.typography.bodyMedium) }
                favourite.favouritedUser.religion?.let { Text(text = "Religion: $it", style = MaterialTheme.typography.bodyMedium) }
                favourite.favouritedUser.cityTown?.let { Text(text = "City/Town: $it", style = MaterialTheme.typography.bodyMedium) }
                favourite.favouritedUser.district?.let { Text(text = "District: $it", style = MaterialTheme.typography.bodyMedium) }
                favourite.favouritedUser.state?.let { Text(text = "State: $it", style = MaterialTheme.typography.bodyMedium) }
            }
            IconButton(onClick = {
                currentUser?.let { viewModel.removeFavourite(it.id, favourite.favouritedUser.id) }
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove from favourites")
            }
        }
    }
}
