package com.example.royale.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.royale.ChestSlot
import com.example.royale.RoyaleCardCatalog
import com.example.royale.data.RoyaleProfileState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun RoyaleHomeTab(
    profile: RoyaleProfileState,
    nowEpochMs: Long,
    onStartBattle: () -> Unit,
    onOpenModeSelector: () -> Unit,
    onOpenTrophyRoad: () -> Unit,
    onOpenArenas: () -> Unit,
    onOpenPass: () -> Unit,
    onOpenCardDetail: (String) -> Unit,
    onOpenCardsTab: () -> Unit,
    onStartUnlockChest: (Int) -> Unit,
    onOpenChest: (Int, Boolean) -> Unit
) {
    val arena = RoyaleCardCatalog.arenaForTrophies(profile.trophies)
    val nextMilestone = RoyaleCardCatalog.trophyMilestones.firstOrNull { it.trophies !in profile.claimedTrophyMilestones }
    val trophyTarget = nextMilestone?.trophies ?: 6000
    val trophyProgress = if (trophyTarget > 0) (profile.trophies.toFloat() / trophyTarget.toFloat()).coerceIn(0f, 1f) else 1f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Arena Banner + King/Princess Towers Preview
        item {
            ArenaPreviewBanner(
                arena = arena,
                trophies = profile.trophies,
                towerSkinName = profile.towerSkinName,
                kingSkinName = profile.kingSkinName,
                modifier = Modifier.clickable { onOpenArenas() }
            )
        }

        // 2. Trophy Road & Pass Royale Progress Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onOpenTrophyRoad,
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.weight(1.35f).testTag("home_trophy_road_button")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = "Jornada de Troféus", tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                            Text("JORNADA DE TROFÉUS", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                        Text(
                            text = if (nextMilestone != null) "Próxima: ${nextMilestone.title} (${nextMilestone.trophies}🏆)" else "Todos os marcos concluídos!",
                            color = Color.White,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { trophyProgress },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFACC15),
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }

                Surface(
                    onClick = onOpenPass,
                    color = Color(0xFF1E1B4B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFD946EF)),
                    modifier = Modifier.weight(1f).testTag("home_pass_button")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.MilitaryTech, contentDescription = "Passe", tint = Color(0xFFE879F9), modifier = Modifier.size(16.dp))
                            Text("PASSE REAL", color = Color(0xFFE879F9), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                        Text("👑 ${profile.passCrowns} Coroas", color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = if (profile.passPremiumUnlocked) "Dourado Ativo" else "Gratuito + Dourado",
                            color = Color(0xFFCBD5E1),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // 3. Chest Slots (4 Slots)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("BAÚS DE BATALHA (${profile.chestSlots.size}/4)", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (slotIdx in 0..3) {
                        val slot = profile.chestSlots.firstOrNull { it.slotIndex == slotIdx }
                        ChestSlotCard(
                            slot = slot,
                            nowEpochMs = nowEpochMs,
                            onStartUnlock = { onStartUnlockChest(slotIdx) },
                            onOpenNow = { instant -> onOpenChest(slotIdx, instant) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Primary Battle CTA + Mode Selector + Arenas Button
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartBattle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15), contentColor = Color(0xFF09090B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("home_battle_cta")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "BATALHAR",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onOpenModeSelector,
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.weight(1.6f).testTag("home_mode_selector_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${profile.selectedBattleMode.title} • IA ${profile.selectedAiDifficulty.labelPt}",
                                    color = Color.White,
                                    fontFamily = SpaceGroteskFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = profile.selectedBattleMode.subtitle,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = SpaceGroteskFamily,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Filled.SwapHoriz, contentDescription = "Trocar modo", tint = Color(0xFF38BDF8))
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenArenas,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier.weight(0.85f).height(56.dp).testTag("home_arenas_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Map, contentDescription = "Arenas", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Text("15 Arenas", color = Color.White, fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 5. Active 8-Card Deck Preview + Average Elixir
        item {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DECK ATIVO: ${profile.activeDeck.name}", color = Color.White, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Tropa da Torre: ${profile.selectedTowerTroop.title}", color = Color(0xFF94A3B8), fontFamily = SpaceGroteskFamily, fontSize = 11.sp)
                        }
                        Surface(
                            color = Color(0xFF581C87),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "⚡ %.1f Elixir".format(profile.activeDeckAverageElixir),
                                color = Color(0xFFF5D0FE),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val ids = profile.activeDeck.cardIds
                    if (ids.size < 8) {
                        Surface(
                            onClick = onOpenCardsTab,
                            color = Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Deck incompleto (${ids.size}/8 cartas). Toque para editar no menu Cartas.",
                                color = Color.White,
                                fontFamily = SpaceGroteskFamily,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    for (rowIdx in 0 until 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (colIdx in 0 until 4) {
                                val slotIdx = rowIdx * 4 + colIdx
                                val cid = ids.getOrNull(slotIdx)
                                val spec = cid?.let { RoyaleCardCatalog.get(it) }
                                if (spec != null) {
                                    val own = profile.cardCollection[cid]
                                    CardTile(
                                        spec = spec,
                                        level = own?.level ?: spec.rarity.startingLevel,
                                        isEvoSlotActive = cid in profile.activeDeck.evoSlotIds,
                                        compact = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(82.dp)
                                            .clickable { onOpenCardDetail(cid) }
                                    )
                                } else {
                                    Surface(
                                        onClick = onOpenCardsTab,
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.weight(1f).height(82.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("+", color = Color(0xFF64748B), fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChestSlotCard(
    slot: ChestSlot?,
    nowEpochMs: Long,
    onStartUnlock: () -> Unit,
    onOpenNow: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (slot == null) {
        Surface(
            modifier = modifier.height(92.dp),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Vazio", color = Color(0xFF475569), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
            }
        }
        return
    }

    val remSec = slot.remainingSeconds(nowEpochMs)
    val isReady = slot.isReady(nowEpochMs)
    val isUnlocking = slot.unlockStartEpochMs != null && !isReady

    Surface(
        onClick = {
            when {
                isReady -> onOpenNow(false)
                isUnlocking -> onOpenNow(true)
                else -> onStartUnlock()
            }
        },
        modifier = modifier.height(92.dp),
        color = if (isReady) Color(0xFF064E3B) else Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, if (isReady) Color(0xFF4ADE80) else Color(slot.type.colorHex))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ChestIllustration(slot.type, Modifier.size(34.dp))
            Text(
                text = slot.type.title.replace("Baú de ", "").replace("Baú ", ""),
                color = Color.White,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1
            )
            when {
                isReady -> Text("ABRIR!", color = Color(0xFF4ADE80), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 10.sp)
                isUnlocking -> {
                    val m = remSec / 60
                    val s = remSec % 60
                    Text("${m}m${"%02d".format(s)} • 💎${slot.type.gemSkipCost}", color = Color(0xFF38BDF8), fontFamily = JetBrainsMonoFamily, fontSize = 8.sp)
                }
                else -> Text("Desbloquear", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontSize = 9.sp)
            }
        }
    }
}
