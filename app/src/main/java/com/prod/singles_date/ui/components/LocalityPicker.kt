package com.prod.singles_date.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prod.singles_date.model.AppLocality

@Composable
fun LocalityPicker(
    cityId: String,
    selectedLocality: String,
    onLocalitySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localities = AppLocality.localitiesForCity(cityId)
    if (localities.isEmpty()) {
        Text(
            text = "Locality selection coming soon for this city.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        localities.forEach { localityId ->
            OptionChip(
                label = AppLocality.displayName(localityId),
                selected = selectedLocality == localityId,
                onClick = { onLocalitySelected(localityId) },
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    fullWidth: Boolean = false,
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        Color.Transparent
    }
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val shape = if (fullWidth) RoundedCornerShape(16.dp) else RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (fullWidth) 20.dp else 16.dp,
                vertical = if (fullWidth) 16.dp else 10.dp,
            ),
        contentAlignment = if (fullWidth) Alignment.CenterStart else Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (fullWidth) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = if (selected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
