package com.prod.singles_date.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Thin stroke icons matching Instagram's post action bar. */
object IgIcons {
    private const val STROKE = 1.5f

    val Heart: ImageVector by lazy {
        outlined("IgHeart") {
            moveTo(16.792f, 3.904f)
            arcTo(4.989f, 4.989f, 0f, false, false, 9.336f, 9.7f)
            lineTo(12f, 12.364f)
            lineTo(14.664f, 9.7f)
            arcTo(4.989f, 4.989f, 0f, false, false, 7.208f, 3.904f)
            arcTo(4.989f, 4.989f, 0f, false, false, 2.5f, 9.122f)
            curveTo(2.5f, 12.294f, 5.718f, 15.041f, 12f, 20.657f)
            curveTo(18.282f, 15.041f, 21.5f, 12.294f, 21.5f, 9.122f)
            arcTo(4.989f, 4.989f, 0f, false, false, 16.792f, 3.904f)
            close()
        }
    }

    val HeartFilled: ImageVector by lazy {
        filled("IgHeartFilled") {
            moveTo(12f, 21.638f)
            lineTo(10.873f, 20.595f)
            curveTo(5.268f, 15.671f, 2f, 12.866f, 2f, 9.5f)
            arcTo(5.5f, 5.5f, 0f, false, true, 12f, 5.089f)
            arcTo(5.5f, 5.5f, 0f, false, true, 22f, 9.5f)
            curveTo(22f, 12.866f, 18.732f, 15.671f, 13.127f, 20.595f)
            lineTo(12f, 21.638f)
            close()
        }
    }

    val Comment: ImageVector by lazy {
        outlined("IgComment") {
            moveTo(20.656f, 17.008f)
            lineTo(15.328f, 20.5f)
            arcToRelative(1.2f, 1.2f, 0f, false, true, -1.648f, -0.086f)
            lineTo(10.891f, 16.832f)
            arcTo(6.5f, 6.5f, 0f, true, true, 14.883f, 13.789f)
        }
    }

    val Send: ImageVector by lazy {
        outlined("IgSend") {
            moveTo(22f, 2f)
            lineTo(11f, 13f)
            moveTo(22f, 2f)
            lineTo(15f, 22f)
            lineTo(11f, 13f)
            moveTo(22f, 2f)
            lineTo(2f, 9f)
            lineTo(11f, 13f)
        }
    }

    val Bookmark: ImageVector by lazy {
        outlined("IgBookmark") {
            moveTo(19f, 21f)
            lineTo(12f, 16f)
            lineTo(5f, 21f)
            lineTo(5f, 5f)
            arcTo(2f, 2f, 0f, false, true, 7f, 3f)
            lineTo(17f, 3f)
            arcTo(2f, 2f, 0f, false, true, 19f, 5f)
            close()
        }
    }

    val BookmarkFilled: ImageVector by lazy {
        filled("IgBookmarkFilled") {
            moveTo(19f, 21f)
            lineTo(12f, 16f)
            lineTo(5f, 21f)
            lineTo(5f, 5f)
            arcTo(2f, 2f, 0f, false, true, 7f, 3f)
            lineTo(17f, 3f)
            arcTo(2f, 2f, 0f, false, true, 19f, 5f)
            close()
        }
    }

    private fun outlined(
        name: String,
        pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        )
    }.build()

    private fun filled(
        name: String,
        pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathBuilder = pathBuilder,
        )
    }.build()
}
