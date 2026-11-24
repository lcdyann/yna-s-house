package com.example.ynashouse.house

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ynashouse.state.YnaHouseState
import com.example.ynashouse.ui.bubble.BubbleButton
import com.example.ynashouse.ui.bubble.BubbleCard
import com.example.ynashouse.ui.theme.BubbleGreen
import com.example.ynashouse.ui.theme.HappyColor
import com.example.ynashouse.ui.theme.Ink
import com.example.ynashouse.ui.theme.Pink
import com.example.ynashouse.ui.theme.Cream
import com.example.ynashouse.ui.theme.RestColor
import com.example.ynashouse.ui.theme.TummyColor

/*
 * METERSHEET.KT
 * Kotak untuk Diyana set meter ikut mood sebenar (hanya owner).
 */
@Composable
fun MeterSheet(state: YnaHouseState, onClose: () -> Unit) {
    var happy by remember { mutableFloatStateOf(state.happiness) }
    var tummy by remember { mutableFloatStateOf(state.tummy) }
    var rest by remember { mutableFloatStateOf(state.rest) }

    Dialog(onDismissRequest = onClose) {
        BubbleCard(modifier = Modifier.fillMaxWidth(), color = Cream) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("How are you really feeling?", style = MaterialTheme.typography.titleMedium)

                MeterSlider("❤️ Happiness", happy, HappyColor) { happy = it }
                MeterSlider("🍟 Tummy", tummy, TummyColor) { tummy = it }
                MeterSlider("😴 Rest", rest, RestColor) { rest = it }

                BubbleButton(
                    text = "Save ♥",
                    color = BubbleGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        state.setMeters(happy, tummy, rest)
                        onClose()
                    }
                )
            }
        }
    }
}

@Composable
fun MeterSlider(label: String, value: Float, color: Color, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(118.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Pink,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.3f)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
