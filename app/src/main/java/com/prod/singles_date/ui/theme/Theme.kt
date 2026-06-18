package com.prod.singles_date.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val InnerCircleDarkScheme = darkColorScheme(
    primary = VioletBright,
    onPrimary = Color.White,
    secondary = FeelActive,
    onSecondary = Color.White,
    tertiary = VioletDeep,
    onTertiary = Color.White,
    background = NightTop,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceHi,
    onSurfaceVariant = TextMuted,
    outline = OutlineSoft,
    outlineVariant = DividerDark,
)

private val InnerCircleLightScheme = lightColorScheme(
    primary = VioletDeep,
    onPrimary = Color.White,
    secondary = FeelActive,
    onSecondary = Color.White,
    tertiary = Violet,
    onTertiary = Color.White,
    background = DayTop,
    onBackground = DayTextPrimary,
    surface = DaySurface,
    onSurface = DayTextPrimary,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = DayTextMuted,
    outline = DayOutline,
    outlineVariant = DividerLight,
)

/** Flat screen background — neutral canvas, no purple tint. */
@Composable
fun hoghtBackgroundBrush(): Brush {
    val bg = MaterialTheme.colorScheme.background
    return Brush.verticalGradient(colors = listOf(bg, bg))
}

/** Flat card surface — same as [MaterialTheme.colorScheme.surface]. */
@Composable
fun hoghtCardBrush(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    return Brush.verticalGradient(colors = listOf(surface, surface))
}

/** Solid brand accent for primary CTAs (prefer over gradients). */
@Composable
fun hoghtAccentBrush(): Brush {
    val primary = MaterialTheme.colorScheme.primary
    return Brush.horizontalGradient(colors = listOf(primary, primary))
}

/** Hairline divider between feed posts. */
@Composable
fun feedDividerColor(): Color = MaterialTheme.colorScheme.outlineVariant

@Deprecated("Use flat background")
val NightGradient: Brush = Brush.verticalGradient(
    colors = listOf(NightTop, NightMid, NightBottom),
)

@Deprecated("Use hoghtAccentBrush")
val VioletGradient: Brush = Brush.horizontalGradient(
    colors = listOf(VioletDeep, Violet, VioletBright),
)

@Composable
fun CitysnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) InnerCircleDarkScheme else InnerCircleLightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? {
    if (this is Activity) return this
    val base = (this as? ContextWrapper)?.baseContext ?: return null
    return base.findActivity()
}
