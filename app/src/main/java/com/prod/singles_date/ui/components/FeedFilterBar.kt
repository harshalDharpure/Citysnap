package com.prod.singles_date.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.AppLocality

@Composable
fun FeedFilterBar(
    cityId: String,
    selectedLocality: String,
    snapsOnly: Boolean,
    onLocalitySelected: (String) -> Unit,
    onSnapsOnlySelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedLocality.isBlank(),
            onClick = { onLocalitySelected("") },
            label = { Text("All ${AppCity.displayName(cityId)}") },
            colors = filterChipColors(),
        )
        AppLocality.localitiesForCity(cityId).forEach { localityId ->
            FilterChip(
                selected = selectedLocality == localityId,
                onClick = { onLocalitySelected(localityId) },
                label = { Text(AppLocality.displayName(localityId)) },
                colors = filterChipColors(),
            )
        }
        FilterChip(
            selected = snapsOnly,
            onClick = { onSnapsOnlySelected(!snapsOnly) },
            label = { Text("📸 Snaps") },
            colors = filterChipColors(),
        )
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    selectedLabelColor = MaterialTheme.colorScheme.primary,
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
