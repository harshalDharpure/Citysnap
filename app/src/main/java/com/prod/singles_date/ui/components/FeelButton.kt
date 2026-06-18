package com.prod.singles_date.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.prod.singles_date.ui.theme.FeelActive

@Composable
fun FeelButton(
    hasFeeled: Boolean,
    feelCount: Int,
    onFeelThis: () -> Unit,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false,
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

    val scale by animateFloatAsState(
        targetValue = when {
            pulsing -> 1.28f
            hasFeeled -> 1.06f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "feelScale",
    )

    val iconButton = @Composable {
        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                pulseKey++
                onFeelThis()
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = if (hasFeeled) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Feel this",
                tint = if (hasFeeled) FeelActive else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(24.dp)
                    .scale(scale),
            )
        }
    }

    if (iconOnly) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            iconButton()
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            iconButton()
            if (feelCount > 0) {
                Text(
                    text = feelCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasFeeled) FeelActive else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .offset(x = (-6).dp)
                        .padding(end = 4.dp),
                )
            }
        }
    }
}
