package com.example.ynashouse.house

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ynashouse.ui.theme.Cream
import com.example.ynashouse.ui.theme.Ink
import com.example.ynashouse.ui.theme.Pink
import kotlin.math.cos
import kotlin.math.sin

/*
 * MINIMAP.KT
 * Peta kecil rumah nonagon (9 bucu = 9 bilik).
 * - Titik gelap besar = bilik yang sedang dilihat
 * - Titik pink        = bilik Yna sekarang
 */
@Composable
fun MiniMap(viewing: HouseRoom, ynaRoom: HouseRoom, modifier: Modifier = Modifier) {
    Canvas(modifier.size(64.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val ringR = size.minDimension * 0.38f
        val count = HouseRoom.entries.size

        // kedudukan setiap bilik di bucu nonagon (bilik pertama di atas)
        val points = List(count) { i ->
            val angle = Math.toRadians(-90.0 + i * 360.0 / count)
            Offset(center.x + ringR * cos(angle).toFloat(), center.y + ringR * sin(angle).toFloat())
        }

        // bentuk nonagon
        val shape = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(shape, Cream)
        drawPath(shape, Ink, style = Stroke(width = 2.dp.toPx()))

        // titik bilik
        HouseRoom.entries.forEachIndexed { i, room ->
            val r = if (room == viewing) 5.dp.toPx() else 3.dp.toPx()
            val c = when {
                room == ynaRoom -> Pink
                room == viewing -> Ink
                else -> Ink.copy(alpha = 0.35f)
            }
            drawCircle(c, radius = r, center = points[i])
            if (room == viewing && room == ynaRoom) {
                drawCircle(Ink, radius = r, center = points[i], style = Stroke(width = 1.5.dp.toPx()))
            }
        }
    }
}
