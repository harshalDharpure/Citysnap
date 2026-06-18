package com.prod.singles_date.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.AppLocality
import com.prod.singles_date.model.PostType
import com.prod.singles_date.model.Thought
import com.prod.singles_date.model.ThoughtCategory
import com.prod.singles_date.ui.theme.TrendHot
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
    onAuthorClick: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    isSaved: Boolean = false,
    showComments: Boolean = true,
    showFeelButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val authorName = thought.displayAuthorName()
    val photoUrl = thought.authorPhotoUrl.ifBlank { authorPhotoUrl }
    val hasImages = thought.imageUrls.isNotEmpty()
    val timeShort = igRelativeTime(thought.createdAt)
    val locationLine = buildString {
        if (thought.locality.isNotBlank()) {
            append(AppLocality.displayName(thought.locality))
        } else if (thought.city.isNotBlank()) {
            append(AppCity.displayName(thought.city))
        }
    }
    val trendingLabel = FeedRanking.trendingLabel(thought, rank, userLocality)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 12.dp),
    ) {
        // Header — avatar · username • 3h ···
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                name = authorName,
                photoUrl = photoUrl,
                size = 34.dp,
                showAccentRing = photoUrl.isNotBlank(),
                modifier = Modifier.then(
                    if (onAuthorClick != null) Modifier.clickable(onClick = onAuthorClick) else Modifier,
                ),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(authorName)
                    }
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        append(" • $timeShort")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onAuthorClick != null) Modifier.clickable(onClick = onAuthorClick) else Modifier,
                    ),
            )
            ThoughtCardOverflowMenu(
                onCopy = onCopy,
                onShareImage = onShareImage,
                onWhatsApp = onWhatsApp,
                onEdit = onEdit,
                onDelete = onDelete,
                onReport = onReport,
                onBlock = onBlock,
                onSave = onSave,
                isSaved = isSaved,
            )
        }

        // Full-bleed media
        if (hasImages) {
            ThoughtImageCarousel(
                imageUrls = thought.imageUrls,
                edgeToEdge = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onOpenDetail != null) Modifier.clickable(onClick = onOpenDetail) else Modifier),
            )
        }

        // Text-only — body then actions (no duplicate caption)
        if (!hasImages && thought.text.isNotBlank()) {
            Text(
                text = thought.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .then(if (onOpenDetail != null) Modifier.clickable(onClick = onOpenDetail) else Modifier),
            )
        }

        // Action row — Instagram thin stroke icons + inline counts
        IgPostActionBar(
            hasFeeled = hasFeeled,
            feelCount = thought.feelCount,
            commentCount = thought.commentCount,
            onFeelThis = onFeelThis,
            onComment = {
                if (onOpenDetail != null) onOpenDetail()
                else onComment()
            },
            onShare = onShareImage,
            showFeelButton = showFeelButton,
            showComments = showComments,
            onSave = onSave,
            isSaved = isSaved,
        )

        // Caption below actions (image posts)
        if (hasImages && thought.text.isNotBlank()) {
            IgCaption(
                authorName = authorName,
                text = thought.text,
                onClick = onOpenDetail,
            )
        }

        // Location / meta
        val meta = buildList {
            if (locationLine.isNotBlank()) add(locationLine)
            if (thought.isSponsored) add("Sponsored")
            else if (trendingLabel != null) add(trendingLabel)
            if (thought.postType == PostType.NOTE) add("Local Note")
            if (thought.category.isNotBlank()) {
                add("${ThoughtCategory.emoji(thought.category)} ${ThoughtCategory.displayName(thought.category)}")
            }
        }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = if (trendingLabel != null && !thought.isSponsored) TrendHot else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (trendingLabel != null) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )
        }

        // View comments
        if (showComments && thought.commentCount > 0) {
            Text(
                text = if (thought.commentCount == 1) "View 1 comment" else "View all ${thought.commentCount} comments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        if (onOpenDetail != null) onOpenDetail()
                        else onComment()
                    },
            )
        }
    }
}

@Composable
private fun IgCaption(
    authorName: String,
    text: String,
    onClick: (() -> Unit)?,
) {
    val isLong = text.length > 100
    val caption = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(authorName)
        }
        append(" ")
        append(if (isLong) text.take(97).trimEnd() + "… " else text)
        if (isLong) {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append("more")
            }
        }
    }
    Text(
        text = caption,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
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
    onSave: (() -> Unit)? = null,
    isSaved: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "More options",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text("Copy text") }, onClick = { expanded = false; onCopy() })
        DropdownMenuItem(text = { Text("WhatsApp") }, onClick = { expanded = false; onWhatsApp() })
        DropdownMenuItem(text = { Text("Share card") }, onClick = { expanded = false; onShareImage() })
        if (onSave != null) {
            DropdownMenuItem(
                text = { Text(if (isSaved) "Unsave" else "Save post") },
                onClick = { expanded = false; onSave() },
            )
        }
        if (onEdit != null) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { expanded = false; onEdit() })
        }
        if (onDelete != null) {
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() },
            )
        }
        if (onReport != null) {
            DropdownMenuItem(text = { Text("Report") }, onClick = { expanded = false; onReport() })
        }
        if (onBlock != null) {
            DropdownMenuItem(text = { Text("Block") }, onClick = { expanded = false; onBlock() })
        }
    }
}

/** Instagram-style short time: 3h, 17m, 2d */
private fun igRelativeTime(createdAt: Long): String {
    val diff = System.currentTimeMillis() - createdAt
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${days / 7}w"
    }
}
