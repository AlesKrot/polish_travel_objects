package com.aleskrot.zabytki.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.maplibre.spatialk.geojson.Position

@Composable
fun AddHeritageDialog(
    position: Position,
    onDismiss: () -> Unit,
    onAdd: (name: String, category: String, imageUrl: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add new object") },
        text = {
            AddHeritageContent(position, onAdd = onAdd)
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddHeritageContent(
    position: Position,
    onAdd: (name: String, category: String, imageUrl: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Coordinates: ${position.latitude.toString().take(8)}, ${position.longitude.toString().take(8)}")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = imageUrl,
            onValueChange = { imageUrl = it },
            label = { Text("Image URL") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://...") }
        )
        Text(
            text = "Note: Image must be accessible via link.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onAdd(name, category, imageUrl) },
            enabled = name.isNotBlank() && category.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add")
        }
    }
}
