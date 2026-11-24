package com.example.ynashouse.watch

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ynashouse.posters.PosterImage
import com.example.ynashouse.posters.PosterStore
import com.example.ynashouse.ui.bubble.BubbleButton
import com.example.ynashouse.ui.bubble.BubbleCard
import com.example.ynashouse.ui.bubble.Ikon
import com.example.ynashouse.ui.bubble.YnaIcon
import com.example.ynashouse.ui.theme.BubbleGreen
import com.example.ynashouse.ui.theme.BubbleLilac
import com.example.ynashouse.ui.theme.BubblePink
import com.example.ynashouse.ui.theme.Cream
import com.example.ynashouse.ui.theme.Pink
import kotlinx.coroutines.launch

/*
 * WATCHLISTSHEET.KT
 * Kotak senarai tontonan (buka dari bilik Theater).
 *
 *  [Now watching]        ← poster + cerita + reaksi Yna
 *  [Recent watches]         butang: 🔎 cari poster · 📷 pilih poster dari galeri · ✓ dah habis · ✕ buang
 *  [All-time favourites]
 *  ── tambah cerita ──      (poster dicari sendiri lepas tekan Add)
 */
@Composable
fun WatchListSheet(store: WatchListStore, canEdit: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(ShowKind.ANIME) }
    var reaction by remember { mutableStateOf(Reaction.LAUGH) }
    var status by remember { mutableStateOf(ShowStatus.NOW) }
    val searching = remember { mutableStateListOf<String>() }      // tajuk yang sedang dicari poster
    var pickingFor by remember { mutableStateOf<String?>(null) }   // tajuk yang sedang pilih gambar

    /* cari poster dalam internet untuk satu cerita */
    fun findPoster(show: Show) {
        if (show.title in searching) return
        searching += show.title
        scope.launch {
            val path = PosterStore.find(context, show.title, show.kind)
            searching -= show.title
            if (path != null) {
                store.setPoster(show.title, path)
            } else {
                Toast.makeText(context, "No poster found 🥺 Try 📷 to pick one from your gallery", Toast.LENGTH_LONG).show()
            }
        }
    }

    // pilih gambar dari galeri
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val forTitle = pickingFor
        pickingFor = null
        if (uri != null && forTitle != null) {
            scope.launch {
                val path = PosterStore.savePicked(context, uri, forTitle)
                if (path != null) store.setPoster(forTitle, path)
            }
        }
    }

    Dialog(onDismissRequest = onClose) {
        BubbleCard(modifier = Modifier.fillMaxWidth(), color = Cream) {
            Column(
                modifier = Modifier
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    YnaIcon(Ikon.TV, size = 28.dp)
                    Text("Yna's watch list", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Box(Modifier.clip(CircleShape).clickable(onClick = onClose).padding(4.dp)) {
                        YnaIcon(Ikon.CLOSE, size = 22.dp)
                    }
                }

                // ---------- senarai ikut status ----------
                ShowStatus.entries.forEach { section ->
                    val inSection = store.shows.filter { it.status == section }
                    if (inSection.isNotEmpty()) {
                        Text(section.label, style = MaterialTheme.typography.labelSmall)
                        inSection.forEach { show ->
                            ShowRow(
                                show = show,
                                canEdit = canEdit,
                                searching = show.title in searching,
                                onFind = { findPoster(show) },
                                onPick = {
                                    pickingFor = show.title
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
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
                            PickChip("${r.emoji} ${r.label}", selected = reaction == r) { reaction = r }
                        }
                    }
                    ChipRow {
                        ShowStatus.entries.forEach { s ->
                            PickChip(s.label.substringBefore(" "), selected = status == s) { status = s }
                        }
                    }
                    BubbleButton(
                        text = "+ Add (poster found automatically)",
                        color = BubbleGreen,
                        enabled = title.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val show = Show(title.trim(), kind, reaction, status)
                            store.add(show)
                            findPoster(show)
                            title = ""
                        }
                    )
                }
            }
        }
    }
}

/* satu baris cerita: poster kecil + tajuk + butang */
@Composable
private fun ShowRow(
    show: Show,
    canEdit: Boolean,
    searching: Boolean,
    onFind: () -> Unit,
    onPick: () -> Unit,
    onDone: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(2.dp, BubbleLilac, RoundedCornerShape(18.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // poster (atau kotak kosong comel)
        Box(
            Modifier
                .size(width = 44.dp, height = 62.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(listOf(BubbleLilac, BubblePink))),
            contentAlignment = Alignment.Center
        ) {
            if (show.poster != null) {
                PosterImage(show.poster, Modifier.matchParentSize())
            } else {
                Text(show.kind.emoji)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(show.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                if (searching) "finding poster…" else "${show.reaction.emoji} ${show.reaction.label}",
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (canEdit) {
            SmallIconButton(Ikon.SEARCH, onFind)
            SmallIconButton(Ikon.CAMERA, onPick)
            if (show.status == ShowStatus.NOW) {
                Text("✓", style = MaterialTheme.typography.titleMedium, color = Pink, modifier = Modifier.clickable(onClick = onDone).padding(4.dp))
            }
            SmallIconButton(Ikon.CLOSE, onRemove)
        }
    }
}

@Composable
private fun SmallIconButton(icon: Ikon, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Cream)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        YnaIcon(icon, size = 20.dp)
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
            .clip(RoundedCornerShape(50))
            .background(if (selected) BubblePink else Color.White)
            .border(2.dp, if (selected) Pink else BubbleLilac, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
