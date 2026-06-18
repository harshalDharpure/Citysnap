package com.prod.singles_date.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.AppLocality
import com.prod.singles_date.model.Thought
import com.prod.singles_date.ui.theme.feedDividerColor
import com.prod.singles_date.util.FeedRanking
import java.util.concurrent.TimeUnit

@Composable
fun ThoughtCard(
    thought: Thought,
    hasFeeled: Boolean,
    userLocality: String,
    rank: Int,
    authorPhotoUrl: String = "",
    onFeelThis: () -> Unit,
    onCopy: () -> Unit,
    onShareImage: () -> Unit,
    onWhatsApp: () -> Unit,
    onComment: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
    onBlock: (() -> Unit)? = null,
    onOpenDetail: (() -> Unit)? = null,
    showComments: Boolean = true,
    showFeelButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val authorName = thought.displayAuthorName()
    val photoUrl = thought.authorPhotoUrl.ifBlank { authorPhotoUrl }
    val localityLine = buildString {
        if (thought.locality.isNotBlank()) {
            append(AppLocality.displayName(thought.locality))
        } else if (thought.city.isNotBlank()) {
            append(AppCity.displayName(thought.city))
        }
        append(" · ")
        append(relativeTime(thought.createdAt))
    }
    val trendingLabel = FeedRanking.trendingLabel(thought, rank, userLocality)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                name = authorName,
                photoUrl = photoUrl,
                size = 40.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = authorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = localityLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ThoughtCardOverflowMenu(
                onCopy = onCopy,
                onShareImage = onShareImage,
                onWhatsApp = onWhatsApp,
                onEdit = onEdit,
                onDelete = onDelete,
                onReport = onReport,
                onBlock = onBlock,
            )
        }

        if (thought.isSponsored || trendingLabel != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                when {
                    thought.isSponsored -> Text(
                        text = "Sponsored · ${thought.sponsorLabel.ifBlank { "Local partner" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    trendingLabel != null -> Text(
                        text = trendingLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onOpenDetail != null) Modifier.clickable(onClick = onOpenDetail)
                    else Modifier,
                ),
        ) {
            if (thought.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                ThoughtImageCarousel(
                    imageUrls = thought.imageUrls,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (thought.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = thought.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showFeelButton) {
                IconButton(onClick = onFeelThis) {
                    Icon(
                        imageVector = if (hasFeeled) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Feel this",
                        tint = if (hasFeeled) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = if (thought.feelCount > 0) thought.feelCount.toString() else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (hasFeeled) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(end = 4.dp),
                )
            }

            if (showComments) {
                IconButton(onClick = onComment) {
                    Text(
                        text = "💬",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (thought.commentCount > 0) {
                    Text(
                        text = thought.commentCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }

            IconButton(onClick = onShareImage) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = feedDividerColor(),
            thickness = 0.5.dp,
        )
    }
}

@Composable
private fun ThoughtCardOverflowMenu(
    onCopy: () -> Unit,
    onShareImage: () -> Unit,
    onWhatsApp: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onReport: (() -> Unit)?,
    onBlock: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = { expanded = true },
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "More options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        DropdownMenuItem(
            text = { Text("Copy text") },
            onClick = {
                expanded = false
                onCopy()
            },
        )
        DropdownMenuItem(
            text = { Text("WhatsApp") },
            onClick = {
                expanded = false
                onWhatsApp()
            },
        )
        if (onEdit != null) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
        }
        if (onDelete != null) {
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
        if (onReport != null) {
            DropdownMenuItem(
                text = { Text("Report") },
                onClick = {
                    expanded = false
                    onReport()
                },
            )
        }
        if (onBlock != null) {
            DropdownMenuItem(
                text = { Text("Block") },
                onClick = {
                    expanded = false
                    onBlock()
                },
            )
        }
    }
}

private fun relativeTime(createdAt: Long): String {
    val diff = System.currentTimeMillis() - createdAt
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${days / 7}w"
    }
}
