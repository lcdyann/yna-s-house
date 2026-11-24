package com.example.ynashouse.yna

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.ynashouse.state.Care
import com.example.ynashouse.state.YnaHouseState
import com.example.ynashouse.watch.Reaction

/*
 * YNAVIEW.KT
 * Lukis Yna: gambar ikut ynaPose(), bergoyang naik turun sikit (bernafas),
 * tap pada Yna = pat dia.
 */
@Composable
fun YnaView(
    state: YnaHouseState,
    now: Long,
    modifier: Modifier = Modifier,
    showReaction: Reaction? = null
) {
    // goyang lembut supaya Yna nampak hidup
    val breathe = rememberInfiniteTransition(label = "breathe")
    val squash by breathe.animateFloat(
        initialValue = 1f,
        targetValue = 0.97f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "squash"
    )

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { state.doCare(Care.PAT) })
        }
    ) {
        Image(
            painter = painterResource(ynaPose(state, now, showReaction)),
            contentDescription = "Yna",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // kecut dari bawah (kaki kekal di lantai)
                    scaleY = squash
                    scaleX = 2f - squash
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
        )
    }
}
