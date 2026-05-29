package com.aleskrot.zabytki.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aleskrot.zabytki.domain.model.HeritageItem

@Composable
actual fun HeritageInfoPopup(
    item: HeritageItem,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm decision") },
            text = { Text("Are you sure to delete this object?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete?.invoke()
                }) { Text("Yes", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        val cardWidth = maxWidth * 0.85f

        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .width(cardWidth)
                .padding(16.dp)
                .clickable(enabled = false) { },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (item.image.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(item.image)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = item.itemLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    softWrap = false, // ВЫПРАЎЛЕННЕ ДЛЯ IOS
                    modifier = Modifier.width(cardWidth - 32.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.categoryLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    softWrap = false, // ВЫПРАЎЛЕННЕ ДЛЯ IOS
                    modifier = Modifier.width(cardWidth - 32.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.width(120.dp)
                ) {
                    Text("Close", softWrap = false)
                }
            }
        }
        
        if (onDelete != null) {
            IconButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
