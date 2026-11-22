package com.example.royale.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.royale.RoyaleCardCatalog
import com.example.royale.data.RoyaleProfileState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun RoyaleProfileStatsTab(
    profile: RoyaleProfileState,
    onOpenModal: (RoyaleModal) -> Unit,
    onBackToSuiteHome: () -> Unit
) {
    val totalMatches = profile.wins + profile.losses
    val winRate = if (totalMatches > 0) (profile.wins * 100) / totalMatches else 0
    val arena = RoyaleCardCatalog.arenaForTrophies(profile.trophies)
    val favCardName = profile.activeDeck.cardIds.firstOrNull()?.let { RoyaleCardCatalog.get(it)?.name } ?: "Cavaleiro"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Player Banner & Identity
        item {
            Surface(
                color = Color(profile.bannerColorHex).copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(profile.frameColorHex))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(profile.bannerColorHex))
                                    .border(2.dp, Color(profile.frameColorHex), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(profile.avatarIcon, fontSize = 28.sp)
                            }
                            Column {
                                Text(profile.playerName, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("Nível ${profile.level} • ${arena.name}", color = Color(0xFFCBD5E1), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                                if (profile.inClan) {
                                    Text("${profile.clanEmblem} ${profile.clanName}", color = Color(0xFF38BDF8), fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
                                }
                            }
                        }

                        Surface(
                            onClick = { onOpenModal(RoyaleModal.PROFILE_EDIT) },
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar perfil", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Editar", color = Color.White, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                            }
                        }
                    }

                    val xpFrac = (profile.xp.toFloat() / profile.xpToNextLevel.toFloat()).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("XP: ${profile.xp} / ${profile.xpToNextLevel}", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                        LinearProgressIndicator(
                            progress = { xpFrac },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }

        // 2. Career Stats Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ESTATÍSTICAS DE CARREIRA", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMiniBox("🏆 Troféus", "${profile.trophies} (Máx ${profile.bestTrophies})", Color(0xFFFACC15), Modifier.weight(1f))
                    StatMiniBox("⚔️ Vitórias", "${profile.wins} ($winRate% taxa)", Color(0xFF4ADE80), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMiniBox("🔥 Sequência", "${profile.winStreak} (Máx ${profile.bestWinStreak})", Color(0xFFF97316), Modifier.weight(1f))
                    StatMiniBox("👑 Torres Derrubadas", "${profile.totalTowersDestroyed}", Color(0xFF38BDF8), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMiniBox("🃏 Carta Favorita", favCardName, Color(0xFFC084FC), Modifier.weight(1f))
                    StatMiniBox("🏰 Tropa da Torre", profile.selectedTowerTroop.title, Color(0xFFE2E8F0), Modifier.weight(1f))
                }
            }
        }

        // 3. Action Menu Rows (History, Achievements, Cosmetics, Magic Items, Settings)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GERENCIAMENTO & PROGRESSÃO", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)

                ProfileActionRow(
                    title = "Histórico de Partidas e Gráficos",
                    subtitle = "${profile.matchHistory.size} partidas recentes registradas",
                    icon = Icons.Filled.History,
                    accent = Color(0xFF38BDF8),
                    onClick = { onOpenModal(RoyaleModal.HISTORY) },
                    modifier = Modifier.testTag("profile_history_button")
                )
                ProfileActionRow(
                    title = "Conquistas e Recompensas",
                    subtitle = "${profile.claimedAchievements.size}/${RoyaleCardCatalog.achievements.size} concluídas",
                    icon = Icons.Filled.EmojiEvents,
                    accent = Color(0xFFFACC15),
                    onClick = { onOpenModal(RoyaleModal.ACHIEVEMENTS) },
                    modifier = Modifier.testTag("profile_achievements_button")
                )
                ProfileActionRow(
                    title = "Cosméticos, Emotes e Tropas de Torre",
                    subtitle = "${profile.unlockedEmotes.size} emotes • ${profile.unlockedSkins.size} visuais",
                    icon = Icons.Filled.Brush,
                    accent = Color(0xFFD946EF),
                    onClick = { onOpenModal(RoyaleModal.COSMETICS) },
                    modifier = Modifier.testTag("profile_cosmetics_button")
                )
                ProfileActionRow(
                    title = "Inventário de Itens Mágicos",
                    subtitle = "${profile.magicItems.values.sum()} itens mágicos disponíveis",
                    icon = Icons.Filled.AutoFixHigh,
                    accent = Color(0xFF4ADE80),
                    onClick = { onOpenModal(RoyaleModal.MAGIC_ITEMS) },
                    modifier = Modifier.testTag("profile_magic_items_button")
                )
                ProfileActionRow(
                    title = "Configurações do Jogo",
                    subtitle = "Áudio, gráficos, bateria, notificações e idioma",
                    icon = Icons.Filled.Settings,
                    accent = Color(0xFF94A3B8),
                    onClick = { onOpenModal(RoyaleModal.SETTINGS) },
                    modifier = Modifier.testTag("profile_settings_button")
                )

                Surface(
                    onClick = onBackToSuiteHome,
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "VOLTAR PARA EXAME DE DISCIPLINA / DRIFT 3D",
                        color = Color(0xFFE2E8F0),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMiniBox(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
            Text(value, color = accent, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ProfileActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = Color(0xFF94A3B8), fontFamily = SpaceGroteskFamily, fontSize = 11.sp)
            }
            Text("›", color = Color(0xFF64748B), fontSize = 20.sp)
        }
    }
}
