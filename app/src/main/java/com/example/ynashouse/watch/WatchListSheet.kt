package com.example.ynashouse.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ynashouse.ui.bubble.BubbleButton
import com.example.ynashouse.ui.bubble.BubbleCard
import com.example.ynashouse.ui.theme.BubbleGreen
import com.example.ynashouse.ui.theme.BubblePink
import com.example.ynashouse.ui.theme.Cream
import com.example.ynashouse.ui.theme.Ink

/*
 * WATCHLISTSHEET.KT
 * Kotak senarai tontonan (buka dari bilik Theater).
 *
 *  [Now watching]        ← cerita + reaksi Yna, butang ✓ (dah habis) & ✕ (buang)
 *  [Recent watches]
 *  [All-time favourites]
 *  ── tambah cerita ──
 *  [tajuk] [jenis] [reaksi] [Add]
 */
@Composable
fun WatchListSheet(store: WatchListStore, canEdit: Boolean, onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(ShowKind.ANIME) }
    var reaction by remember { mutableStateOf(Reaction.LAUGH) }
    var status by remember { mutableStateOf(ShowStatus.NOW) }

    Dialog(onDismissRequest = onClose) {
        BubbleCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📺 Diyana's watch list", style = MaterialTheme.typography.titleMedium)

                // ---------- senarai ikut status ----------
                ShowStatus.entries.forEach { section ->
                    val inSection = store.shows.filter { it.status == section }
                    if (inSection.isNotEmpty()) {
                        Text(section.label, style = MaterialTheme.typography.labelSmall)
                        inSection.forEach { show ->
                            ShowRow(
                                show = show,
                                canEdit = canEdit,
                                onDone = { store.move(show, ShowStatus.RECENT) },
                                onRemove = { store.remove(show) }
                            )
                        }
                    }
                }

                // ---------- tambah cerita (owner sahaja) ----------
                if (canEdit) {
                    Text("Add a show", style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(60) },
                        placeholder = { Text("Title, e.g. Kaiju No. 8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChipRow {
                        ShowKind.entries.forEach { k ->
                            PickChip("${k.emoji} ${k.label}", selected = kind == k) { kind = k }
                        }
                    }
                    ChipRow {
                        Reaction.entries.forEach { r ->
                            PickChip(r.emoji, selected = reaction == r) { reaction = r }
                        }
                    }
                    ChipRow {
                        ShowStatus.entries.forEach { s ->
                            PickChip(s.label.substringBefore(" "), selected = status == s) { status = s }
                        }
                    }
                    BubbleButton(
                        text = "+ Add",
                        color = BubbleGreen,
                        enabled = title.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            store.add(Show(title.trim(), kind, reaction, status))
                            title = ""
                        }
                    )
                }

                BubbleButton(text = "Close", onClick = onClose, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/* satu baris cerita */
@Composable
private fun ShowRow(show: Show, canEdit: Boolean, onDone: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Cream)
            .border(2.dp, Ink, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(show.kind.emoji)
        Text(show.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(show.reaction.emoji)
        if (canEdit) {
            if (show.status == ShowStatus.NOW) {
                Text("✓", modifier = Modifier.clickable(onClick = onDone).padding(4.dp))
            }
            Text("✕", modifier = Modifier.clickable(onClick = onRemove).padding(4.dp))
        }
    }
}

/* baris cip pilihan (boleh scroll ke tepi kalau tak muat) */
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) { content() }
}

/* cip kecil yang boleh dipilih */
@Composable
private fun PickChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) BubblePink else Cream)
            .border(2.dp, Ink, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
