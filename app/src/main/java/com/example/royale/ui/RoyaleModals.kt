package com.example.royale.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.royale.AiDifficulty
import com.example.royale.BattleMode
import com.example.royale.MagicItemType
import com.example.royale.MatchRecord
import com.example.royale.RoyaleCardCatalog
import com.example.royale.RoyaleSettings
import com.example.royale.TowerTroopId
import com.example.royale.data.RoyaleProfileState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
private fun ModalContainer(
    title: String,
    accent: Color = Color(0xFF38BDF8),
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF0B1120),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, accent),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title, color = accent, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }
                HorizontalDivider(color = Color(0xFF1E293B))
                Box(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}

@Composable
fun TrophyRoadModal(
    profile: RoyaleProfileState,
    onClaim: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalContainer("JORNADA DE TROFÉUS", Color(0xFFFACC15), onDismiss) {
        LazyColumn(
            modifier = Modifier.height(500.dp),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RoyaleCardCatalog.trophyMilestones) { m ->
                val reached = profile.trophies >= m.trophies
                val claimed = m.trophies in profile.claimedTrophyMilestones
                Surface(
                    color = if (reached) Color(0xFF1E293B) else Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (reached && !claimed) Color(0xFF4ADE80) else Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("🏆 ${m.trophies} • ${m.title}", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            val rewardParts = buildList {
                                if (m.rewardGold > 0) add("🪙 ${m.rewardGold}")
                                if (m.rewardGems > 0) add("💎 ${m.rewardGems}")
                                if (m.rewardCardId != null) add("${m.rewardCardCopies}x ${RoyaleCardCatalog.get(m.rewardCardId)?.name ?: m.rewardCardId}")
                                if (m.rewardChest != null) add(m.rewardChest.title)
                            }.joinToString(" + ")
                            Text(rewardParts, color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
                        }
                        when {
                            claimed -> Text("RESGATADO", color = Color(0xFF4ADE80), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                            reached -> Button(
                                onClick = { onClaim(m.trophies) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80), contentColor = Color.Black)
                            ) { Text("RESGATAR", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                            else -> Text("Faltam ${m.trophies - profile.trophies}", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArenasListModal(
    profile: RoyaleProfileState,
    onDismiss: () -> Unit
) {
    ModalContainer("15 ARENAS OFICIAIS", Color(0xFF38BDF8), onDismiss) {
        LazyColumn(
            modifier = Modifier.height(500.dp),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RoyaleCardCatalog.arenas) { arena ->
                val unlocked = profile.trophies >= arena.minTrophies
                val cardsInArena = RoyaleCardCatalog.allCards.count { it.unlockArenaIndex == arena.index }
                Surface(
                    color = Color(arena.groundColorHex).copy(alpha = if (unlocked) 0.9f else 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(arena.accentColorHex))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ARENA ${arena.index + 1}: ${arena.name}", color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(arena.ambientDesc, color = Color(0xFFCBD5E1), fontFamily = SpaceGroteskFamily, fontSize = 11.sp)
                            Text("$cardsInArena cartas desbloqueáveis • Mín: 🏆 ${arena.minTrophies}", color = Color(arena.accentColorHex), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                        }
                        Text(
                            text = if (unlocked) "ABERTA" else "BLOQ.",
                            color = if (unlocked) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelectorModal(
    selectedMode: BattleMode,
    selectedDifficulty: AiDifficulty,
    onSelectMode: (BattleMode) -> Unit,
    onSelectDifficulty: (AiDifficulty) -> Unit,
    onDismiss: () -> Unit
) {
    ModalContainer("MODOS DE BATALHA PERMANENTES", Color(0xFFFACC15), onDismiss) {
        LazyColumn(
            modifier = Modifier.height(500.dp),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("DIFICULDADE DA IA ADVERSÁRIA", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AiDifficulty.entries.forEach { d ->
                        val active = d == selectedDifficulty
                        Surface(
                            onClick = { onSelectDifficulty(d) },
                            color = if (active) Color(0xFFFACC15) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = d.labelPt,
                                color = if (active) Color.Black else Color.White,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("ESCOLHA O MODO", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
            }
            items(BattleMode.entries) { mode ->
                val active = mode == selectedMode
                Surface(
                    onClick = {
                        onSelectMode(mode)
                        onDismiss()
                    },
                    color = if (active) Color(0xFF1E3A8A) else Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (active) Color(0xFF38BDF8) else Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(mode.title, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text(mode.subtitle, color = Color(0xFFCBD5E1), fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PassProgressionModal(
    profile: RoyaleProfileState,
    onUnlockPremium: () -> Unit,
    onClaimTier: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalContainer("PASSE DE PROGRESSÃO PERMANENTE", Color(0xFFD946EF), onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👑 Coroas: ${profile.passCrowns}", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 13.sp)
                if (!profile.passPremiumUnlocked) {
                    Button(
                        onClick = onUnlockPremium,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15), contentColor = Color.Black)
                    ) {
                        Text("ATIVAR DOURADO (💎 120)", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    Text("PASSE DOURADO ATIVO", color = Color(0xFF4ADE80), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
            LazyColumn(
                modifier = Modifier.height(450.dp),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RoyaleCardCatalog.passTiers) { tier ->
                    val unlocked = profile.passCrowns >= tier.crownsRequired
                    val freeClaimed = tier.tier in profile.claimedFreePassTiers
                    val premClaimed = tier.tier in profile.claimedPremiumPassTiers
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (unlocked) Color(0xFFD946EF) else Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Patamar ${tier.tier} • 👑 ${tier.crownsRequired} Coroas", color = Color.White, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onClaimTier(tier.tier, false) },
                                    enabled = unlocked && !freeClaimed,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (freeClaimed) "Gratuito ✓" else "Grátis: ${tier.freeRewardLabel}", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { onClaimTier(tier.tier, true) },
                                    enabled = unlocked && profile.passPremiumUnlocked && !premClaimed,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15), contentColor = Color.Black),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (premClaimed) "Dourado ✓" else "Ouro: ${tier.premiumRewardLabel}", fontSize = 10.sp)
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
fun AchievementsModal(
    profile: RoyaleProfileState,
    onClaim: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalContainer("CONQUISTAS E MARCOS", Color(0xFFFACC15), onDismiss) {
        LazyColumn(
            modifier = Modifier.height(480.dp),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RoyaleCardCatalog.achievements) { def ->
                val prog = profile.achievementProgress(def).coerceAtMost(def.target)
                val done = prog >= def.target
                val claimed = def.id in profile.claimedAchievements
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (done && !claimed) Color(0xFF4ADE80) else Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(def.title, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("🪙${def.rewardGold} + 💎${def.rewardGems}", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                        }
                        Text(def.description, color = Color(0xFF94A3B8), fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
                        LinearProgressIndicator(
                            progress = { (prog.toFloat() / def.target.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF4ADE80),
                            trackColor = Color(0xFF1E293B)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("$prog / ${def.target}", color = Color(0xFFCBD5E1), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                            when {
                                claimed -> Text("CONCLUÍDA", color = Color(0xFF4ADE80), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                                done -> Button(onClick = { onClaim(def.id) }) { Text("RESGATAR", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryAndGraphsModal(
    profile: RoyaleProfileState,
    onOpenMatchDetail: (MatchRecord) -> Unit,
    onDismiss: () -> Unit
) {
    ModalContainer("HISTÓRICO E GRÁFICO DE DESEMPENHO", Color(0xFF38BDF8), onDismiss) {
        LazyColumn(
            modifier = Modifier.height(500.dp),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("DESEMPENHO RECENTE (COROAS POR PARTIDA)", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                ) {
                    val data = profile.matchHistory.take(12).reversed()
                    if (data.isEmpty()) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Dispute partidas para gerar seu gráfico de evolução.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    } else {
                        Canvas(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            val w = size.width
                            val h = size.height
                            val stepX = if (data.size > 1) w / (data.size - 1) else w
                            val path = Path()
                            data.forEachIndexed { idx, rec ->
                                val x = idx * stepX
                                val y = h - (rec.playerCrowns.toFloat() / 3f) * h
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                drawCircle(
                                    color = if (rec.won) Color(0xFF4ADE80) else Color(0xFFF43F5E),
                                    radius = 6f,
                                    center = Offset(x, y)
                                )
                            }
                            drawPath(path, color = Color(0xFF38BDF8), style = Stroke(width = 3f))
                        }
                    }
                }
            }

            if (profile.matchHistory.isEmpty()) {
                item {
                    Text("Nenhuma partida registrada ainda.", color = Color(0xFF64748B), fontFamily = SpaceGroteskFamily)
                }
            } else {
                items(profile.matchHistory) { rec ->
                    Surface(
                        onClick = { onOpenMatchDetail(rec) },
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (rec.won) Color(0xFF10B981) else Color(0xFFF43F5E))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${if (rec.won) "VITÓRIA" else "DERROTA"} • ${rec.playerCrowns} 👑 vs 👑 ${rec.enemyCrowns}",
                                    color = if (rec.won) Color(0xFF4ADE80) else Color(0xFFF43F5E),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Text("${rec.modeTitle} vs ${rec.opponentName} (${rec.durationSec}s)", color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
                            }
                            Text(
                                text = if (rec.trophiesDelta >= 0) "+${rec.trophiesDelta} 🏆" else "${rec.trophiesDelta} 🏆",
                                color = Color(0xFFFACC15),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchDetailDialog(
    record: MatchRecord,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = { Text("${record.modeTitle} vs ${record.opponentName}", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Placar: ${record.playerCrowns} 👑 vs 👑 ${record.enemyCrowns} • ${record.durationSec}s", color = Color(0xFF38BDF8))
                Text("Cartas usadas na partida:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                if (record.cardStats.isEmpty()) {
                    Text("Sem estatísticas detalhadas registradas.", color = Color(0xFF64748B), fontSize = 12.sp)
                } else {
                    record.cardStats.forEach { cs ->
                        val name = RoyaleCardCatalog.get(cs.cardId)?.name ?: cs.cardId
                        Text("• $name: ${cs.timesPlayed}x | Dano ${cs.damageDealt} | Torres ${cs.towerDamage} | Abates ${cs.kills}", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("FECHAR") } }
    )
}

@Composable
fun CosmeticsAndTowerTroopsModal(
    profile: RoyaleProfileState,
    onSelectTowerTroop: (TowerTroopId) -> Unit,
    onSelectSkin: (String, Boolean) -> Unit,
    onToggleEmote: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalContainer("COSMÉTICOS, EMOTES E TORRES", Color(0xFFD946EF), onDismiss) {
        LazyColumn(
            modifier = Modifier.height(500.dp),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("TROPA DA TORRE DA PRINCESA", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
            }
            items(TowerTroopId.entries) { troop ->
                val unlocked = troop in profile.unlockedTowerTroops
                val active = troop == profile.selectedTowerTroop
                Surface(
                    onClick = { if (unlocked) onSelectTowerTroop(troop) },
                    color = if (active) Color(0xFF1E3A8A) else Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (active) Color(0xFF38BDF8) else Color(0xFF334155))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(troop.title, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(troop.description, color = Color(0xFF94A3B8), fontFamily = SpaceGroteskFamily, fontSize = 11.sp)
                        }
                        Text(
                            text = when {
                                active -> "EM USO"
                                unlocked -> "SELECIONAR"
                                else -> "NA LOJA"
                            },
                            color = if (active) Color(0xFF4ADE80) else Color(0xFFFACC15),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            item {
                Text("EMOTES EQUIPADOS (${profile.equippedEmotes.size}/4)", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
            }
            items(RoyaleCardCatalog.emotes) { em ->
                val unlocked = em.id in profile.unlockedEmotes
                val equipped = em.id in profile.equippedEmotes
                Surface(
                    onClick = { if (unlocked) onToggleEmote(em.id) },
                    color = if (equipped) Color(0xFF3B0764) else Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(em.rarity.colorHex))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(em.iconTag, fontSize = 22.sp)
                            Column {
                                Text(em.name, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("\"${em.phrase}\"", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = when {
                                equipped -> "EQUIPADO"
                                unlocked -> "EQUIPAR"
                                else -> "BLOQUEADO"
                            },
                            color = if (equipped) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            item {
                Text("VISUAIS DE TORRE E REI DESBLOQUEADOS", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                profile.unlockedSkins.forEach { skin ->
                    val isKing = skin.contains("Rei", ignoreCase = true)
                    val active = skin == profile.towerSkinName || skin == profile.kingSkinName
                    Surface(
                        onClick = { onSelectSkin(skin, isKing) },
                        color = if (active) Color(0xFF064E3B) else Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(skin, color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 13.sp)
                            Text(if (active) "ATIVO" else "APLICAR", color = Color(0xFF4ADE80), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MagicItemsInventoryModal(
    profile: RoyaleProfileState,
    onDismiss: () -> Unit
) {
    ModalContainer("INVENTÁRIO DE ITENS MÁGICOS", Color(0xFF4ADE80), onDismiss) {
        LazyColumn(
            modifier = Modifier.height(460.dp),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Para usar um item mágico, abra qualquer carta na aba Cartas e selecione o botão correspondente.", color = Color(0xFF94A3B8), fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
            }
            items(MagicItemType.entries) { item ->
                val count = profile.magicItems[item] ?: 0
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(item.colorHex))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, color = Color(item.colorHex), fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(item.description, color = Color(0xFFCBD5E1), fontFamily = SpaceGroteskFamily, fontSize = 11.sp)
                        }
                        Text("${count}x", color = Color.White, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsModal(
    settings: RoyaleSettings,
    onUpdateSettings: (RoyaleSettings) -> Unit,
    onResetProgress: () -> Unit,
    onDismiss: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    ModalContainer("CONFIGURAÇÕES DO JOGO", Color(0xFF94A3B8), onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingToggle("Efeitos Sonoros (SFX)", settings.sfxEnabled) { onUpdateSettings(settings.copy(sfxEnabled = it)) }
            SettingToggle("Música / Ambiente", settings.musicEnabled) { onUpdateSettings(settings.copy(musicEnabled = it)) }
            Column {
                Text("Volume Geral: ${(settings.volume * 100).toInt()}%", color = Color.White, fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
                Slider(value = settings.volume, onValueChange = { onUpdateSettings(settings.copy(volume = it)) })
            }
            SettingToggle("Vibração Tátil", settings.vibrationEnabled) { onUpdateSettings(settings.copy(vibrationEnabled = it)) }
            SettingToggle("Gráficos de Alta Qualidade", settings.highGraphics) { onUpdateSettings(settings.copy(highGraphics = it)) }
            SettingToggle("Economia de Bateria", settings.batterySaver) { onUpdateSettings(settings.copy(batterySaver = it)) }
            SettingToggle("Notificações Internas", settings.notificationsEnabled) { onUpdateSettings(settings.copy(notificationsEnabled = it)) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Idioma", color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 14.sp)
                Text(settings.language, color = Color(0xFF38BDF8), fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
            }

            HorizontalDivider(color = Color(0xFF1E293B))
            Button(
                onClick = { showResetConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("REINICIAR PROGRESSO DA CONTA", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Reiniciar Conta?", color = Color.White) },
            text = { Text("Isso restaurará troféus, ouro, cartas e histórico para o início.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetProgress()
                        showResetConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) { Text("CONFIRMAR") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("CANCELAR") } }
        )
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
fun ProfileEditModal(
    profile: RoyaleProfileState,
    onSave: (String, String, Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.playerName) }
    var avatar by remember { mutableStateOf(profile.avatarIcon) }
    var bannerHex by remember { mutableLongStateOf(profile.bannerColorHex) }
    var frameHex by remember { mutableLongStateOf(profile.frameColorHex) }

    ModalContainer("PERSONALIZAR PERFIL", Color(0xFF38BDF8), onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do Jogador") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Avatar", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("👑", "⚔️", "🛡️", "🔥", "⚡", "🐉").forEach { av ->
                    Surface(
                        onClick = { avatar = av },
                        color = if (avatar == av) Color(0xFF2563EB) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(av, fontSize = 22.sp, modifier = Modifier.padding(10.dp))
                    }
                }
            }
            Text("Cor da Bandeira", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0xFF1D4ED8L, 0xFF7E22CEL, 0xFFBE123CL, 0xFF047857L, 0xFFB45309L).forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .clickable { bannerHex = hex }
                    ) {
                        if (bannerHex == hex) Text("✓", color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
            Button(
                onClick = {
                    onSave(name, avatar, bannerHex, frameHex)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("SALVAR PERFIL", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun ChestRewardDialog(
    reward: ChestOpeningReward,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChestIllustration(reward.chestType, Modifier.size(40.dp))
                Text("${reward.chestType.title} Aberto!", color = Color(0xFFFACC15), fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🪙 +${reward.gold} Ouro", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 15.sp)
                if (reward.gems > 0) {
                    Text("💎 +${reward.gems} Gemas", color = Color(0xFF22D3EE), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                HorizontalDivider(color = Color(0xFF1E293B))
                reward.cards.forEach { (cid, qty) ->
                    val spec = RoyaleCardCatalog.get(cid)
                    if (spec != null) {
                        Text("🃏 ${qty}x ${spec.name} (${spec.rarity.labelPt})", color = Color(spec.rarity.colorHex), fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("COLETAR TUDO") }
        }
    )
}

@Composable
fun MatchmakingOverlay(
    state: MatchmakingState,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, Color(0xFFFACC15))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = Color(0xFFFACC15))
                Text("PREPARANDO BATALHA", color = Color.White, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("${state.mode.title} • ${state.opponentArenaName}", color = Color(0xFF38BDF8), fontFamily = SpaceGroteskFamily, fontSize = 13.sp)
                Text(
                    text = "Oponente: ${state.opponentName} (IA • ${state.difficulty.labelPt})",
                    color = Color(0xFFCBD5E1),
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 12.sp
                )
                OutlinedButton(onClick = onCancel) {
                    Text("CANCELAR", color = Color(0xFFF43F5E))
                }
            }
        }
    }
}

@Composable
fun TutorialOverlay(
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val steps = listOf(
        "Bem-vindo à Arena Royale!" to "Comande tropas, construções e feitiços em tempo real para derrubar as torres adversárias e conquistar coroas.",
        "Elixir e Mobilização" to "Seu elixir carrega até 10. Arraste ou toque em uma carta da mão e toque no campo para posicionar suas unidades.",
        "Evoluções, Campeões e Decks" to "Monte até 5 decks de 8 cartas na aba Cartas, melhore seus níveis, use Itens Mágicos e ative habilidades de Campeões em batalha.",
        "Jornada, Baús e Clã" to "Vença batalhas contra a IA para abrir baús cronometrados, avançar pelas 15 arenas e trocar doações no seu Clã!"
    )
    val (title, body) = steps[step]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, Color(0xFF38BDF8))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("GUIA INICIAL (${step + 1}/${steps.size})", color = Color(0xFF38BDF8), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text(title, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(body, color = Color(0xFFCBD5E1), fontFamily = SpaceGroteskFamily, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onComplete) { Text("PULAR", color = Color(0xFF94A3B8)) }
                    Button(
                        onClick = {
                            if (step < steps.lastIndex) step++ else onComplete()
                        }
                    ) {
                        Text(if (step < steps.lastIndex) "PRÓXIMO" else "COMEÇAR!")
                    }
                }
            }
        }
    }
}
