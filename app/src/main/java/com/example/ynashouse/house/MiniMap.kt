package com.example.ynashouse.house

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ynashouse.ui.theme.BubblePink
import com.example.ynashouse.ui.theme.Glass
import com.example.ynashouse.ui.theme.Ink
import com.example.ynashouse.ui.theme.Pink
import kotlin.math.cos
import kotlin.math.sin

/*
 * MINIMAP.KT
 * Butang peta rumah nonagon (9 bucu = 9 bilik). Tekan = buka Hallway (9 gambar bilik).
 * - Titik gelap besar = bilik yang sedang dilihat
 * - Titik pink        = bilik Yna sekarang
 */
@Composable
fun MiniMap(viewing: HouseRoom, ynaRoom: HouseRoom, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            Modifier
                .size(62.dp)
                .shadow(6.dp, CircleShape, ambientColor = Pink, spotColor = Pink)
                .clip(CircleShape)
                .background(Glass)
                .border(2.5.dp, BubblePink, CircleShape)
                .clickable(onClick = onClick)
                .padding(6.dp)
        ) {
            Canvas(Modifier.size(50.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val ringR = size.minDimension * 0.4f
                val count = HouseRoom.entries.size

                // kedudukan setiap bilik di bucu nonagon (bilik pertama di atas)
                val points = List(count) { i ->
                    val angle = Math.toRadians(-90.0 + i * 360.0 / count)
                    Offset(center.x + ringR * cos(angle).toFloat(), center.y + ringR * sin(angle).toFloat())
                }

                // bentuk nonagon (lantai pink lembut)
                val shape = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(shape, BubblePink.copy(alpha = 0.45f))
                drawPath(shape, Pink.copy(alpha = 0.7f), style = Stroke(width = 2.dp.toPx()))

                // titik bilik
                HouseRoom.entries.forEachIndexed { i, room ->
                    val r = if (room == viewing) 5.dp.toPx() else 3.dp.toPx()
                    val c = when {
                        room == ynaRoom -> Pink
                        room == viewing -> Ink
                        else -> Ink.copy(alpha = 0.3f)
                    }
                    drawCircle(c, radius = r, center = points[i])
                    if (room == viewing && room == ynaRoom) {
                        drawCircle(Ink, radius = r, center = points[i], style = Stroke(width = 1.5.dp.toPx()))
                    }
                }
            }
        }
        Text("Hallway", style = MaterialTheme.typography.labelSmall)
    }
}
