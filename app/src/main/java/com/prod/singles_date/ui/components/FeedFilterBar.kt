package com.prod.singles_date.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.AppLocality
import com.prod.singles_date.model.ThoughtCategory
import com.prod.singles_date.util.FeedSortMode

@Composable
fun FeedFilterBar(
    cityId: String,
    selectedLocality: String,
    snapsOnly: Boolean,
    workOnly: Boolean,
    selectedCategory: String,
    sortMode: FeedSortMode,
    onLocalitySelected: (String) -> Unit,
    onSnapsOnlySelected: (Boolean) -> Unit,
    onWorkOnlySelected: (Boolean) -> Unit,
    onCategorySelected: (String) -> Unit,
    onSortModeSelected: (FeedSortMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            SegmentedButton(
                selected = sortMode == FeedSortMode.NEW,
                onClick = { onSortModeSelected(FeedSortMode.NEW) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("New") }
            SegmentedButton(
                selected = sortMode == FeedSortMode.HOT,
                onClick = { onSortModeSelected(FeedSortMode.HOT) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Hot") }
        }

        Row(
            modifier = Modifier
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
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedCategory.isBlank() && !workOnly,
                onClick = {
                    onWorkOnlySelected(false)
                    onCategorySelected("")
                },
                label = { Text("All topics") },
                colors = filterChipColors(),
            )
            FilterChip(
                selected = workOnly,
                onClick = { onWorkOnlySelected(!workOnly) },
                label = { Text("💼 Work & Jobs") },
                colors = filterChipColors(),
            )
            listOf(
                ThoughtCategory.TRAFFIC,
                ThoughtCategory.RENT,
                ThoughtCategory.STARTUP,
                ThoughtCategory.FOOD,
                ThoughtCategory.LIFE,
            ).forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = {
                        onWorkOnlySelected(false)
                        onCategorySelected(if (selectedCategory == cat) "" else cat)
                    },
                    label = {
                        Text("${ThoughtCategory.emoji(cat)} ${ThoughtCategory.displayName(cat)}")
                    },
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
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    selectedLabelColor = MaterialTheme.colorScheme.primary,
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
