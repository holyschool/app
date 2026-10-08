package com.enderplusbayzuiship.edupage2.ui.subjects

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Rasterizes a Compose [ImageVector] into a circular Android [Bitmap] with the
 * given background and icon colors. Used for the live-class notification large
 * icon, where a vector cannot be drawn directly.
 */
fun rasterizeSubjectIcon(
    image: ImageVector,
    sizePx: Int,
    backgroundColorArgb: Int,
    iconColorArgb: Int,
): Bitmap {
    val bitmap = ImageBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val parser = PathParser()

    fun collect(node: VectorNode) {
        when (node) {
            is VectorPath -> parser.addPathNodes(node.pathData)
            is VectorGroup -> node.forEach { collect(it) }
        }
    }
    image.root.forEach { collect(it) }
    val path = parser.toPath(Path())

    val side = sizePx.toFloat()
    val scaleX = side / image.viewportWidth
    val scaleY = side / image.viewportHeight

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(side, side)) {
        drawCircle(
            color = Color(backgroundColorArgb),
            radius = side / 2f,
            center = Offset(side / 2f, side / 2f),
        )
        scale(scaleX, scaleY, pivot = Offset.Zero) {
            drawPath(path, color = Color(iconColorArgb))
        }
    }
    return bitmap.asAndroidBitmap()
}

/**
 * Creates a circular avatar [Bitmap] with the subject's initials. Used as the
 * live-class notification large icon when the subject has a color but no icon.
 */
fun subjectLetterAvatarBitmap(
    text: String,
    sizePx: Int,
    backgroundColorArgb: Int,
    textColorArgb: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val circlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColorArgb
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, circlePaint)

    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = textColorArgb
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = sizePx * 0.42f
    }
    val metrics = textPaint.fontMetrics
    val centerY = sizePx / 2f - (metrics.ascent + metrics.descent) / 2f
    canvas.drawText(subjectInitials(text), sizePx / 2f, centerY, textPaint)
    return bitmap
}
