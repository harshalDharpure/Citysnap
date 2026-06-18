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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prod.singles_date.model.AppCity

@Composable
fun CityPicker(
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val sorted = AppCity.ALL.sortedBy { if (it == AppCity.BANGALORE) 0 else 1 }
        sorted.forEach { cityId ->
            val unlocked = AppCity.isExpansionUnlocked(cityId)
            val label = when {
                cityId == AppCity.BANGALORE -> "${AppCity.displayName(cityId)} ★ Live"
                unlocked -> AppCity.displayName(cityId)
                else -> "${AppCity.displayName(cityId)} — Coming soon"
            }
            CityOptionChip(
                label = label,
                selected = selectedCity == cityId,
                enabled = unlocked,
                onClick = { if (unlocked) onCitySelected(cityId) },
            )
        }
    }
}

@Composable
private fun CityOptionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else -> Color.Transparent
    }
    val border = when {
        selected -> MaterialTheme.colorScheme.primary
        !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                selected -> MaterialTheme.colorScheme.onBackground
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
