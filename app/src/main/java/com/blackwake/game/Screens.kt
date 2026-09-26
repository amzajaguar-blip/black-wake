package com.blackwake.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenPadding = Modifier
    .fillMaxSize()
    .displayCutoutPadding()
    .padding(horizontal = 24.dp, vertical = 16.dp)

@Composable
fun MainMenu(state: GameState, viewModel: GameViewModel) {
    val progress = state.progress
    ChartBackdrop {
        Row(ScreenPadding, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(0.42f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Label("OPERAZIONE 07.14N", Palette.Amber, 12.sp)
                Text("BLACK WAKE", color = Palette.Text, fontSize = 40.sp, fontFamily = Mono, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(
                    "Elias Vane ha la prova. Black Tide ha le barche.\nTre corsie, una scia, nessun porto sicuro.",
                    color = Palette.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(Modifier.height(18.dp))
                Label("INTEL IN BANCA", Palette.Muted, 10.sp)
                Text("${progress.intelBank}", color = Palette.Cyan, fontSize = 26.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                TacticalButton("OFFICINA", onClick = viewModel::openGarage, modifier = Modifier.widthIn(min = 200.dp))
                Spacer(Modifier.height(8.dp))
                TacticalButton(
                    if (progress.muted) "AUDIO: SPENTO" else "AUDIO: ACCESO",
                    onClick = viewModel::toggleMute,
                    color = if (progress.muted) Palette.Muted else Palette.Cyan,
                    modifier = Modifier.widthIn(min = 200.dp)
                )
            }
            Column(Modifier.weight(0.58f).fillMaxHeight()) {
                Label("CAMPAGNA // ${progress.unlockedChapterCount}/${CHAPTERS.size} DOSSIER APERTI", Palette.Muted, 10.sp, Modifier.padding(bottom = 8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(CHAPTERS) { index, chapter ->
                        ChapterCard(index, chapter, progress, onOpen = { viewModel.openBriefing(index) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterCard(index: Int, chapter: Chapter, progress: Progress, onOpen: () -> Unit) {
    val unlocked = index < progress.unlockedChapterCount
    val best = progress.bestIntel.getOrElse(index) { 0 }
    val cleared = index + 1 < progress.unlockedChapterCount || (index == CHAPTERS.lastIndex && best >= chapter.intelRequired)
    val border = when {
        !unlocked -> Palette.Dim
        cleared -> Palette.Green.copy(alpha = 0.5f)
        else -> Palette.Cyan.copy(alpha = 0.6f)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (unlocked) Palette.Panel else Palette.Deep)
            .border(1.dp, border)
            .clickable(enabled = unlocked, role = Role.Button, onClick = onOpen)
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label("CAP ${chapter.code}", if (unlocked) Palette.Cyan else Palette.Dim, 10.sp)
            Label(
                when {
                    !unlocked -> "BLOCCATO"
                    cleared -> "COMPLETATO"
                    else -> "DISPONIBILE"
                },
                if (cleared) Palette.Green else if (unlocked) Palette.Amber else Palette.Dim, 9.sp
            )
        }
        Text(chapter.title, color = if (unlocked) Palette.Text else Palette.Dim, fontSize = 15.sp, fontFamily = Mono, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        Text(if (unlocked) chapter.subtitle else "Dossier cifrato", color = Palette.Muted.copy(alpha = if (unlocked) 1f else 0.5f), fontSize = 11.sp, maxLines = 2)
        if (unlocked && best > 0) Label("RECORD INTEL $best", Palette.Muted, 9.sp, Modifier.padding(top = 4.dp))
    }
}

@Composable
fun BriefingScreen(state: GameState, viewModel: GameViewModel) {
    val chapter = CHAPTERS[state.chapterIndex]
    val boat = boatSpec(state.progress.boat)
    ChartBackdrop {
        Row(ScreenPadding, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(0.55f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Label("CAPITOLO ${chapter.code} // DOSSIER", Palette.Cyan, 11.sp)
                Text(chapter.title, color = Palette.Text, fontSize = 32.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
                Text(chapter.subtitle, color = Palette.Amber, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                chapter.briefing.forEach {
                    Text(it, color = Palette.Text.copy(alpha = 0.85f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("MAYA: ${chapter.maya}", color = Palette.Cyan.copy(alpha = 0.85f), fontSize = 13.sp, fontStyle = FontStyle.Italic)
            }
            Column(Modifier.weight(0.45f).fillMaxHeight()) {
              Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Panel(Modifier.fillMaxWidth()) {
                    Label("OBIETTIVO", Palette.Amber, 10.sp)
                    Text(
                        "Recupera almeno ${chapter.intelRequired} intel prima che si chiuda la finestra di estrazione.",
                        color = Palette.Text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                    Label(
                        "${chapter.sectors.size} SETTORI · ${chapter.sectors.count { it.fork }} BIVIO · VASCELLO ${boat.title}",
                        Palette.Muted, 9.sp, Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Panel(Modifier.fillMaxWidth()) {
                    Label("COMANDI", Palette.Cyan, 10.sp)
                    ControlLine("TRASCINA SUL MARE", "cambia corsia")
                    ControlLine("LEVA POTENZA", "alta: veloce e visibile · bassa: silenzio")
                    ControlLine("IMMERGI (tieni)", "passa sotto relitti e barche; consuma ossigeno")
                    ControlLine("RAZZO", "acceca gli inseguitori")
                }
              }
              // Pinned: the primary action must never sit below the fold.
              Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  TacticalButton("INIZIA MISSIONE", onClick = viewModel::startRun, filled = true)
                  TacticalButton("INDIETRO", onClick = viewModel::openMenu, color = Palette.Muted)
              }
            }
        }
    }
}

@Composable
private fun ControlLine(key: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Label(key, Palette.Text, 9.sp, Modifier.width(118.dp))
        Text(detail, color = Palette.Muted, fontSize = 11.sp)
    }
}

@Composable
fun GarageScreen(state: GameState, viewModel: GameViewModel) {
    val progress = state.progress
    ChartBackdrop {
        Column(ScreenPadding) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Label("OFFICINA", Palette.Amber, 11.sp)
                    Text("INTEL IN BANCA: ${progress.intelBank}", color = Palette.Cyan, fontSize = 18.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
                }
                TacticalButton("INDIETRO", onClick = viewModel::openMenu, color = Palette.Muted)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(0.5f)) {
                    Label("VASCELLO", Palette.Muted, 10.sp, Modifier.padding(bottom = 6.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(BOATS) { _, boat ->
                            BoatCard(boat, selected = progress.boat == boat.type) { viewModel.selectBoat(boat.type) }
                        }
                    }
                }
                Column(Modifier.weight(0.5f)) {
                    Label("MODULI", Palette.Muted, 10.sp, Modifier.padding(bottom = 6.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MODULES) { module ->
                            ModuleRow(module, progress) { viewModel.buyModule(module.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoatCard(boat: BoatSpec, selected: Boolean, onSelect: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (selected) Palette.Cyan.copy(alpha = 0.16f) else Palette.Panel)
            .border(if (selected) 2.dp else 1.dp, if (selected) Palette.Cyan else Palette.Line)
            .clickable(role = Role.Button, onClick = onSelect)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(Color(boat.hullColor)).border(1.dp, Palette.Cyan.copy(alpha = 0.6f)))
            Label(boat.title, if (selected) Palette.Cyan else Palette.Text, 11.sp, Modifier.padding(start = 6.dp))
        }
        Text(boat.detail, color = Palette.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 2)
        Label(boatStats(boat), Palette.Dim, 9.sp, Modifier.padding(top = 4.dp))
    }
}

private fun boatStats(boat: BoatSpec): String {
    fun pct(value: Float) = ((value - 1f) * 100f).toInt().let { if (it > 0) "+$it%" else if (it < 0) "$it%" else "=" }
    val hull = boat.hullBonus.toInt().let { if (it > 0) "+$it" else if (it < 0) "$it" else "=" }
    return "VEL ${pct(boat.speed)} · SCAFO $hull · FIRMA ${pct(boat.detection)} · CONS ${pct(boat.fuelUse)}"
}

@Composable
private fun ModuleRow(module: ModuleDef, progress: Progress, onBuy: () -> Unit) {
    val level = progress.modules.level(module.id)
    val maxed = level >= MODULE_MAX_LEVEL
    val cost = module.costs.getOrNull(level)
    val affordable = !maxed && cost != null && progress.intelBank >= cost
    Row(
        Modifier.fillMaxWidth().background(Palette.Panel).border(1.dp, Palette.Line).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(module.title, Palette.Text, 11.sp)
                Label(module.tag, Palette.Amber, 9.sp, Modifier.padding(start = 8.dp))
            }
            Text(module.detail, color = Palette.Muted, fontSize = 11.sp)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(MODULE_MAX_LEVEL) { i ->
                    Box(Modifier.width(18.dp).height(5.dp).background(if (i < level) Palette.Cyan else Palette.Deep).border(1.dp, Palette.Line))
                }
            }
        }
        TacticalButton(
            text = if (maxed) "MAX" else "$cost INTEL",
            onClick = onBuy,
            enabled = affordable,
            filled = affordable,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun PauseScreen(state: GameState, viewModel: GameViewModel) {
    val chapter = CHAPTERS[state.chapterIndex]
    val run = state.run
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f))) {
        Row(ScreenPadding, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(0.38f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("SOSPENSIONE", Palette.Amber, 12.sp)
                Text(chapter.title, color = Palette.Text, fontSize = 22.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
                Label("INTEL ${run.intel}/${chapter.intelRequired} · SCAFO ${run.hull.toInt()}", Palette.Muted, 10.sp)
                Spacer(Modifier.height(4.dp))
                TacticalButton("RIPRENDI", onClick = viewModel::resume, filled = true, modifier = Modifier.fillMaxWidth())
                TacticalButton(
                    "MAPPA TATTICA", onClick = { viewModel.togglePausePanel(PausePanel.MAP) },
                    color = if (state.pausePanel == PausePanel.MAP) Palette.Amber else Palette.Cyan, modifier = Modifier.fillMaxWidth()
                )
                TacticalButton(
                    "REGISTRO DI BORDO", onClick = { viewModel.togglePausePanel(PausePanel.LOG) },
                    color = if (state.pausePanel == PausePanel.LOG) Palette.Amber else Palette.Cyan, modifier = Modifier.fillMaxWidth()
                )
                TacticalButton(
                    if (state.progress.muted) "AUDIO: SPENTO" else "AUDIO: ACCESO", onClick = viewModel::toggleMute,
                    color = Palette.Muted, modifier = Modifier.fillMaxWidth()
                )
                TacticalButton("ABBANDONA MISSIONE", onClick = viewModel::abandonRun, color = Palette.Red, modifier = Modifier.fillMaxWidth())
            }
            Box(Modifier.weight(0.62f).fillMaxHeight()) {
                when (state.pausePanel) {
                    PausePanel.MAP -> TacticalMap(chapter, run)
                    PausePanel.LOG -> EventLog(run)
                    PausePanel.NONE -> Panel(Modifier.fillMaxWidth()) {
                        Label("SITUAZIONE", Palette.Cyan, 10.sp)
                        val sector = chapter.sectors[run.sectorIndex.coerceIn(0, chapter.sectors.lastIndex)]
                        Text("${sector.title} — ${sector.tag}", color = Palette.Text, fontSize = 14.sp, fontFamily = Mono, modifier = Modifier.padding(top = 4.dp))
                        Text(sector.radio, color = Palette.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                        Label(
                            "FIRMA ${(run.detection * 100).toInt()}% · CARBURANTE ${run.fuel.toInt()}% · RAZZI ${run.flare.charges}",
                            Palette.Muted, 10.sp, Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalMap(chapter: Chapter, run: RunState) {
    Panel(Modifier.fillMaxSize()) {
        Label("MAPPA TATTICA // ${chapter.title}", Palette.Cyan, 11.sp)
        Column(Modifier.padding(top = 8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chapter.sectors.forEachIndexed { index, sector ->
                val current = index == run.sectorIndex
                val past = index < run.sectorIndex
                val color = when {
                    current -> Palette.Amber
                    past -> Palette.Cyan
                    else -> Palette.Dim
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(26.dp).border(2.dp, color).background(if (current) color.copy(alpha = 0.25f) else Color.Transparent), contentAlignment = Alignment.Center) {
                        Label(if (sector.extract) "E" else if (sector.fork) "?" else "${index + 1}", color, 11.sp)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(sector.title, color = if (current) Palette.Text else if (past) Palette.Muted else Palette.Dim, fontSize = 13.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
                        Label(
                            when {
                                sector.extract -> "OBIETTIVO: ESTRAZIONE"
                                sector.fork -> "OBIETTIVO: BIVIO TATTICO"
                                else -> "NAVIGAZIONE · ${sector.tag}"
                            },
                            color.copy(alpha = 0.8f), 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventLog(run: RunState) {
    Panel(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label("REGISTRO DI BORDO", Palette.Cyan, 11.sp)
            Label("${run.feed.size} VOCI", Palette.Muted, 9.sp)
        }
        LazyColumn(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            items(run.feed) { entry ->
                val minutes = (entry.timestamp / 60f).toInt()
                val seconds = (entry.timestamp % 60f).toInt()
                Row {
                    Label("T+%02d:%02d".format(minutes, seconds), Palette.Dim, 10.sp, Modifier.width(66.dp))
                    Text(entry.text, color = toneColor(entry.tone), fontSize = 11.sp, fontFamily = Mono)
                }
            }
        }
    }
}

@Composable
fun DebriefScreen(state: GameState, viewModel: GameViewModel) {
    val outcome = state.outcome ?: return
    val chapter = CHAPTERS[state.chapterIndex]
    val hasNext = outcome.won && state.chapterIndex + 1 < CHAPTERS.size && state.chapterIndex + 1 < state.progress.unlockedChapterCount
    val accent = if (outcome.won) Palette.Cyan else Palette.Red
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .displayCutoutPadding()
                .padding(16.dp)
                .widthIn(max = 620.dp)
                .background(Palette.PanelSolid)
                .border(1.dp, accent)
                .padding(20.dp)
        ) {
          Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Label("DEBRIEFING // CAP ${chapter.code} ${chapter.title}", Palette.Muted, 10.sp)
            Text(if (outcome.won) "MISSIONE COMPIUTA" else "MISSIONE FALLITA", color = accent, fontSize = 26.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
            Text(outcome.reason, color = Palette.Text, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("INTEL", "${outcome.intel}/${outcome.intelRequired}", if (outcome.intel >= outcome.intelRequired) Palette.Green else Palette.Amber)
                Stat("IN BANCA", "+${outcome.banked}", Palette.Cyan)
                Stat("FIRMA MAX", "${(outcome.peakDetection * 100).toInt()}%", if (outcome.peakDetection >= 0.7f) Palette.Red else Palette.Cyan)
                Stat("TEMPO", "%d:%02d".format((outcome.seconds / 60f).toInt(), (outcome.seconds % 60f).toInt()), Palette.Text)
            }
            if (outcome.won) {
                Spacer(Modifier.height(12.dp))
                chapter.closer.forEach {
                    Text(it, color = Palette.Muted, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                }
                if (outcome.unlockedNext) {
                    Label("NUOVO DOSSIER SBLOCCATO", Palette.Green, 11.sp, Modifier.padding(top = 8.dp))
                }
            } else if (outcome.banked > 0) {
                Text("Il 40% dell'intel raccolta è stato messo al sicuro.", color = Palette.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
          }
          Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              if (hasNext) TacticalButton("PROSSIMO CAPITOLO", onClick = viewModel::openNextChapter, filled = true)
              TacticalButton("RIPROVA", onClick = viewModel::startRun, filled = !hasNext)
              TacticalButton("MENU", onClick = viewModel::openMenu, color = Palette.Muted)
          }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color) {
    Column {
        Label(label, Palette.Muted, 9.sp)
        Text(value, color = color, fontSize = 18.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
    }
}
