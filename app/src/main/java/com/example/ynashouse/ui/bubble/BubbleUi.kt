package com.example.ynashouse.ui.bubble

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ynashouse.ui.theme.BubbleYellow
import com.example.ynashouse.ui.theme.Cream
import com.example.ynashouse.ui.theme.Ink

/*
 * BUBBLEUI.KT
 * Gaya "cute bubble square": petak bucu bulat, garis gelap tebal, bayang di bawah.
 * Semua butang & kotak dalam app guna fungsi di sini supaya rupa sama.
 */

private val BubbleShape = RoundedCornerShape(18.dp)
private val ShadowDepth = 5.dp

/* ---------- BUTANG ----------
   Bila ditekan, butang turun ke bayang (rasa macam butang betul). */
@Composable
fun BubbleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = BubbleYellow,
    enabled: Boolean = true
) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed) ShadowDepth - 1.dp else 0.dp, label = "sink")

    BubbleBase(modifier = modifier, sink = sink) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(BubbleShape)
                .background(if (enabled) color else color.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = press,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

/* ---------- KOTAK (tak boleh tekan) ---------- */
@Composable
fun BubbleCard(
    modifier: Modifier = Modifier,
    color: Color = Cream,
    content: @Composable BoxScope.() -> Unit
) {
    BubbleBase(modifier = modifier, sink = 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(BubbleShape)
                .background(color)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            content = content
        )
    }
}

/* Lapisan asas: bayang gelap di bawah + badan bergaris */
@Composable
private fun BubbleBase(
    modifier: Modifier,
    sink: Dp,
    body: @Composable () -> Unit
) {
    Box(modifier = modifier.padding(bottom = ShadowDepth)) {
        // bayang
        Box(
            Modifier
                .matchParentSize()
                .offset(y = ShadowDepth)
                .background(Ink, BubbleShape)
        )
        // badan
        Box(
            Modifier
                .offset(y = sink)
                .border(3.dp, Ink, BubbleShape)
        ) {
            body()
        }
    }
}

/* ---------- METER (happiness / tummy / rest) ---------- */
@Composable
fun MeterBar(
    emoji: String,
    value: Float,          // 0 - 100
    color: Color,
    modifier: Modifier = Modifier
) {
    val shown by animateFloatAsState(value / 100f, label = "meter")
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, style = MaterialTheme.typography.bodyMedium)
        Box(
            Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White)
                .border(2.dp, Ink, RoundedCornerShape(7.dp))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(7.dp))
                    .background(color)
            )
        }
    }
}

/* ---------- BUBBLE CAKAP Yna ---------- */
@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier) {
    BubbleCard(modifier = modifier.width(220.dp), color = Color.White) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
