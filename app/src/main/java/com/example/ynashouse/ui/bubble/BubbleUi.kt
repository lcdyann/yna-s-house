package com.example.ynashouse.ui.bubble

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.sin
import com.example.ynashouse.ui.theme.BubblePink
import com.example.ynashouse.ui.theme.BubbleYellow
import com.example.ynashouse.ui.theme.Glass
import com.example.ynashouse.ui.theme.Ink
import com.example.ynashouse.ui.theme.Pink
import com.example.ynashouse.ui.theme.deepOf

/*
 * BUBBLEUI.KT
 * Gaya "cozy game HUD": pil bulat, gradient lembut, bayang berwarna, butang melantun bila ditekan.
 * Semua butang & kotak dalam app guna fungsi di sini supaya rupa sama.
 *
 *  BubbleButton  - butang pil penuh (tulisan)
 *  BubbleCard    - kotak putih kaca
 *  RoundButton   - butang bulat ikon/emoji + label kecil (dok bawah)
 *  LiquidMeter   - meter kotak bercecair macam Pou
 *  HudPill       - pil HUD kecil (nama bilik, meter)
 *  MiniMeter     - meter kecil dalam HUD
 *  MeterBar      - meter penuh (dalam MeterSheet)
 *  PageDots      - titik bilik (ganti peta)
 *  SpeechBubble  - bubble cakap Yna
 */

private val PillShape = RoundedCornerShape(22.dp)

/* ---------- BUTANG PIL ----------
   Gradient terang → deep, kilat putih di atas, mengecil & melantun bila ditekan. */
@Composable
fun BubbleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = BubbleYellow,
    enabled: Boolean = true,
    icon: Ikon? = null           // ikon comel di sebelah kiri tulisan (pilihan)
) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val squish by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "squish"
    )
    val deep = deepOf(color)
    val alpha = if (enabled) 1f else 0.45f

    Box(
        modifier = modifier
            .scale(squish)
            .shadow(if (enabled) 6.dp else 0.dp, PillShape, ambientColor = deep, spotColor = deep)
            .clip(PillShape)
            .background(Brush.verticalGradient(listOf(color.copy(alpha = alpha), deep.copy(alpha = alpha))))
            .border(2.dp, Color.White.copy(alpha = 0.7f), PillShape)
            .clickable(interactionSource = press, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        // kilat putih di separuh atas butang
        Box(
            Modifier
                .matchParentSize()
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.45f),
                        0.45f to Color.Transparent
                    )
                )
        )
        if (icon == null) {
            Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                YnaIcon(icon, size = 24.dp)
                Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}

/* ---------- KOTAK KACA (tak boleh tekan) ---------- */
@Composable
fun BubbleCard(
    modifier: Modifier = Modifier,
    color: Color = Glass,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(8.dp, PillShape, ambientColor = Pink.copy(alpha = 0.4f), spotColor = Pink.copy(alpha = 0.4f))
            .clip(PillShape)
            .background(color)
            .border(2.dp, Color.White, PillShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        content = content
    )
}

/* ---------- BUTANG BULAT (dok bawah) ----------
   highlight = true: butang goyang & jadi pink (contoh: Yna perlukan benda ni sekarang).
   Tiga cara guna: dengan ikon (Ikon.HEART), dengan emoji, atau isi sendiri (content). */
@Composable
fun RoundButton(
    icon: Ikon,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = BubbleYellow,
    highlight: Boolean = false
) = RoundButtonBase(label, onClick, modifier, color, highlight) { YnaIcon(icon, size = 32.dp) }

@Composable
fun RoundButton(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = BubbleYellow,
    highlight: Boolean = false
) = RoundButtonBase(label, onClick, modifier, color, highlight) {
    Text(emoji, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun RoundButtonBase(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = BubbleYellow,
    highlight: Boolean = false,
    content: @Composable () -> Unit
) {
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val squish by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "squish"
    )
    val wiggleAnim = rememberInfiniteTransition(label = "wiggle")
    val wiggle by wiggleAnim.animateFloat(
        initialValue = -9f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(260), RepeatMode.Reverse),
        label = "wiggleAngle"
    )
    val fill = if (highlight) BubblePink else color
    val deep = deepOf(fill)

    Column(
        modifier = modifier.widthIn(min = 62.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(squish)
                .rotate(if (highlight) wiggle else 0f)
                .shadow(7.dp, CircleShape, ambientColor = deep, spotColor = deep)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(fill, deep)))
                .border(2.5.dp, Color.White, CircleShape)
                .clickable(interactionSource = press, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // kilat
            Box(
                Modifier
                    .matchParentSize()
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.55f),
                            0.5f to Color.Transparent
                        )
                    )
            )
            content()
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlight) Pink else Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ---------- METER CECAIR (macam Pou) ----------
   Kotak kecil, cecair di dalam = nilai meter, beralun. Bawah 20 = kotak berdenyut merah. */
@Composable
fun LiquidMeter(icon: Ikon, value: Float, color: Color, modifier: Modifier = Modifier, boxSize: Dp = 44.dp) {
    val level by animateFloatAsState((value / 100f).coerceIn(0f, 1f), animationSpec = tween(700), label = "level")
    val anim = rememberInfiniteTransition(label = "wave")
    val phase by anim.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "phase"
    )
    val pulse by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulse"
    )
    val low = value < 20f
    val deep = deepOf(color)
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .size(boxSize)
            .shadow(4.dp, shape, ambientColor = deep, spotColor = deep)
            .clip(shape)
            .background(Color.White)
            .border(2.5.dp, if (low) Color(0xFFF2657A).copy(alpha = 0.5f + 0.5f * pulse) else Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val top = size.height * (1f - level)
            val amp = if (level in 0.02f..0.98f) size.height * 0.05f else 0f
            fun wave(shift: Float): Path = Path().apply {
                moveTo(0f, size.height)
                var x = 0f
                while (x <= size.width + 4f) {
                    lineTo(x, top + sin(phase + shift + x / size.width * 2f * PI.toFloat()) * amp)
                    x += 4f
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(wave(1.6f), color.copy(alpha = 0.4f))                                // ombak belakang
            drawPath(wave(0f), Brush.verticalGradient(listOf(color, deep), startY = top))  // ombak depan
            // kilat kaca
            drawRoundRect(
                Color.White.copy(alpha = 0.5f),
                topLeft = Offset(size.width * 0.14f, size.height * 0.1f),
                size = Size(size.width * 0.12f, size.height * 0.45f),
                cornerRadius = CornerRadius(size.width * 0.06f)
            )
        }
        YnaIcon(icon, size = boxSize * 0.55f)
    }
}

/* ---------- PIL HUD ---------- */
@Composable
fun HudPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .shadow(6.dp, CircleShape, ambientColor = Pink.copy(alpha = 0.35f), spotColor = Pink.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(Glass)
            .border(2.dp, Color.White, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

/* ---------- METER KECIL (dalam HUD) ---------- */
@Composable
fun MiniMeter(emoji: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(value / 100f, label = "mini")
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(emoji, style = MaterialTheme.typography.labelMedium)
        Box(
            Modifier
                .width(30.dp)
                .height(9.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.22f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown.coerceIn(0.04f, 1f))
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/* ---------- METER PENUH (happiness / tummy / rest) ---------- */
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, style = MaterialTheme.typography.bodyMedium)
        Box(
            Modifier
                .weight(1f)
                .height(14.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.22f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown.coerceIn(0.03f, 1f))
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)))
            )
        }
        Text("${value.toInt()}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(28.dp))
    }
}

/* ---------- TITIK BILIK (ganti peta) ----------
   Titik semasa jadi kapsul panjang. Bilik Yna = titik pink. Tekan titik = pergi ke bilik tu. */
@Composable
fun PageDots(
    count: Int,
    current: Int,
    special: Int,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    HudPill(modifier = modifier) {
        for (i in 0 until count) {
            val w by animateDpAsState(if (i == current) 22.dp else 9.dp, label = "dot")
            val c = when {
                i == special -> Pink
                i == current -> Ink
                else -> Ink.copy(alpha = 0.25f)
            }
            Box(
                Modifier
                    .width(w)
                    .height(9.dp)
                    .clip(CircleShape)
                    .background(c)
                    .clickable { onTap(i) }
            )
        }
    }
}

/* ---------- BUBBLE CAKAP Yna ---------- */
@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .widthIn(max = 220.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Pink, spotColor = Pink)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(2.dp, BubblePink, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}
