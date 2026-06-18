package com.prod.singles_date.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prod.singles_date.ui.theme.FeelActive

private val IgIconSize = 22.dp
private val IgActionSpacing = 10.dp

@Composable
fun IgPostActionBar(
    hasFeeled: Boolean,
    feelCount: Int,
    commentCount: Int,
    onFeelThis: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    showFeelButton: Boolean,
    showComments: Boolean,
    onSave: (() -> Unit)? = null,
    isSaved: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var pulseKey by remember { mutableStateOf(0) }
    var pulsing by remember { mutableStateOf(false) }

    LaunchedEffect(pulseKey) {
        if (pulseKey == 0) return@LaunchedEffect
        pulsing = true
        kotlinx.coroutines.delay(180)
        pulsing = false
    }

    val heartScale by animateFloatAsState(
        targetValue = when {
            pulsing -> 1.2f
            hasFeeled -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "igHeartScale",
    )

    val tint = MaterialTheme.colorScheme.onBackground

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IgActionSpacing),
    ) {
        if (showFeelButton) {
            IgActionGroup(
                count = feelCount,
                contentDescription = "Feel this",
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    pulseKey++
                    onFeelThis()
                },
            ) {
                Icon(
                    imageVector = if (hasFeeled) IgIcons.HeartFilled else IgIcons.Heart,
                    contentDescription = null,
                    tint = if (hasFeeled) FeelActive else tint,
                    modifier = Modifier
                        .size(IgIconSize)
                        .scale(heartScale),
                )
            }
        }

        if (showComments) {
            IgActionGroup(
                count = commentCount,
                contentDescription = "Comments",
                onClick = onComment,
            ) {
                Icon(
                    imageVector = IgIcons.Comment,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(IgIconSize),
                )
            }
        }

        IgActionGroup(
            count = 0,
            contentDescription = "Share",
            onClick = onShare,
            showCount = false,
        ) {
            Icon(
                imageVector = IgIcons.Send,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(IgIconSize),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (onSave != null) {
            Row(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onSave,
                    )
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isSaved) IgIcons.BookmarkFilled else IgIcons.Bookmark,
                    contentDescription = if (isSaved) "Unsave" else "Save",
                    tint = tint,
                    modifier = Modifier.size(IgIconSize),
                )
            }
        }
    }
}

@Composable
private fun IgActionGroup(
    count: Int,
    contentDescription: String,
    onClick: () -> Unit,
    showCount: Boolean = true,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon()
        if (showCount && count > 0) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
