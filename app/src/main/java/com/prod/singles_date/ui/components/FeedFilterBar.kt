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
import com.prod.singles_date.ui.theme.TrendHot
import com.prod.singles_date.util.FeedSortMode

@Composable
fun FeedFilterBar(
    feedCityFilter: String,
    selectedLocality: String,
    sortMode: FeedSortMode,
    onCitySelected: (String) -> Unit,
    onLocalitySelected: (String) -> Unit,
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
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = TrendHot.copy(alpha = 0.16f),
                    activeContentColor = TrendHot,
                ),
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
                selected = feedCityFilter.isBlank(),
                onClick = { onCitySelected("") },
                label = { Text("All cities") },
                colors = filterChipColors(),
            )
            AppCity.ALL.forEach { cityId ->
                FilterChip(
                    selected = feedCityFilter == cityId,
                    onClick = { onCitySelected(if (feedCityFilter == cityId) "" else cityId) },
                    label = { Text(AppCity.displayName(cityId)) },
                    colors = filterChipColors(),
                )
            }
        }

        if (feedCityFilter.isNotBlank()) {
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
                    label = { Text("All ${AppCity.displayName(feedCityFilter)}") },
                    colors = filterChipColors(),
                )
                AppLocality.localitiesForCity(feedCityFilter).forEach { localityId ->
                    FilterChip(
                        selected = selectedLocality == localityId,
                        onClick = { onLocalitySelected(localityId) },
                        label = { Text(AppLocality.displayName(localityId)) },
                        colors = filterChipColors(),
                    )
                }
            }
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
