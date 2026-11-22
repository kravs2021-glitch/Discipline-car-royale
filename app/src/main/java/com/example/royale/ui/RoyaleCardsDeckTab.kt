package com.example.royale.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.royale.CardCategory
import com.example.royale.CardRarity
import com.example.royale.CardSpec
import com.example.royale.MagicItemType
import com.example.royale.RoyaleCardCatalog
import com.example.royale.data.RoyaleProfileState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily
import kotlin.math.roundToInt

enum class CardSortMode(val label: String) {
    ELIXIR("Elixir"),
    RARITY("Raridade"),
    LEVEL("Nível"),
    NAME("Nome")
}

@Composable
fun RoyaleCardsDeckTab(
    profile: RoyaleProfileState,
    onSelectDeck: (Int) -> Unit,
    onRenameDeck: (String) -> Unit,
    onReplaceCardInSlot: (Int, String) -> Unit,
    onClearDeck: () -> Unit,
    onCopyDeckTo: (Int) -> Unit,
    onShuffleDeck: () -> Unit,
    onOpenCardDetail: (String) -> Unit
) {
    var selectedSlotForSwap by remember { mutableIntStateOf(-1) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterRarity by remember { mutableStateOf<CardRarity?>(null) }
    var filterCategory by remember { mutableStateOf<CardCategory?>(null) }
    var onlyUpgradable by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(CardSortMode.ELIXIR) }

    val filteredCards = remember(
        searchQuery,
        filterRarity,
        filterCategory,
        onlyUpgradable,
        sortMode,
        profile.cardCollection,
        profile.gold
    ) {
        RoyaleCardCatalog.allCards.filter { spec ->
            val matchesQuery = searchQuery.isBlank() || spec.name.contains(searchQuery, ignoreCase = true)
            val matchesRarity = filterRarity == null || spec.rarity == filterRarity
            val matchesCategory = filterCategory == null || spec.category == filterCategory
            val own = profile.cardCollection[spec.id]
            val matchesUpgrade = !onlyUpgradable || (own != null && own.level < spec.rarity.maxLevel && own.copies >= spec.upgradeCardRequirement(own.level) && profile.gold >= spec.upgradeGoldCost(own.level))
            matchesQuery && matchesRarity && matchesCategory && matchesUpgrade
        }.sortedWith(
            when (sortMode) {
                CardSortMode.ELIXIR -> compareBy<CardSpec> { it.elixirCost }.thenBy { it.name }
                CardSortMode.RARITY -> compareByDescending<CardSpec> { it.rarity.ordinal }.thenBy { it.elixirCost }
                CardSortMode.LEVEL -> compareByDescending<CardSpec> { profile.cardCollection[it.id]?.level ?: 0 }.thenBy { it.elixirCost }
                CardSortMode.NAME -> compareBy<CardSpec> { it.name }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Deck Selector (5 Decks)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    profile.decks.forEachIndexed { idx, deck ->
                        val active = idx == profile.activeDeckIndex
                        Surface(
                            onClick = {
                                onSelectDeck(idx)
                                selectedSlotForSwap = -1
                            },
                            color = if (active) Color(0xFF2563EB) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) Color(0xFF60A5FA) else Color(0xFF334155)),
                            modifier = Modifier.weight(1f).testTag("deck_tab_$idx")
                        ) {
                            Text(
                                text = "D${idx + 1}",
                                color = Color.White,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Active Deck Card
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(profile.activeDeck.name, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Renomear deck", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = "⚡ %.1f Elixir".format(profile.activeDeckAverageElixir),
                                color = Color(0xFFE879F9),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }

                        if (selectedSlotForSwap >= 0) {
                            Surface(color = Color(0xFF1E3A8A), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tocando abaixo substitui o slot ${selectedSlotForSwap + 1}.", color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 11.sp)
                                    Text("Cancelar", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.clickable { selectedSlotForSwap = -1 })
                                }
                            }
                        }

                        val ids = profile.activeDeck.cardIds
                        for (rowIdx in 0 until 2) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (colIdx in 0 until 4) {
                                    val slotIdx = rowIdx * 4 + colIdx
                                    val cid = ids.getOrNull(slotIdx)
                                    val spec = cid?.let { RoyaleCardCatalog.get(it) }
                                    val isSwapping = selectedSlotForSwap == slotIdx

                                    if (spec != null) {
                                        val own = profile.cardCollection[cid]
                                        CardTile(
                                            spec = spec,
                                            level = own?.level ?: spec.rarity.startingLevel,
                                            copies = own?.copies,
                                            isEvolvedUnlocked = own?.evolutionUnlocked == true,
                                            isEvoSlotActive = cid in profile.activeDeck.evoSlotIds,
                                            compact = false,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(104.dp)
                                                .clickable {
                                                    if (selectedSlotForSwap == slotIdx) {
                                                        onOpenCardDetail(cid)
                                                    } else {
                                                        selectedSlotForSwap = slotIdx
                                                    }
                                                }
                                        )
                                    } else {
                                        Surface(
                                            onClick = { selectedSlotForSwap = slotIdx },
                                            color = if (isSwapping) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, if (isSwapping) Color(0xFF4ADE80) else Color(0xFF334155)),
                                            modifier = Modifier.weight(1f).height(104.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("Vazio\n+${slotIdx + 1}", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Deck Utility Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = onShuffleDeck,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Aleatório", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { showCopyDialog = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copiar", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = onClearDeck,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Limpar", color = Color(0xFFF43F5E), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Search, Filters & Sort
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "COLEÇÃO COMPLETA (${profile.cardCollection.size}/${RoyaleCardCatalog.allCards.size} DESBLOQUEADAS)",
                    color = Color(0xFF94A3B8),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar carta por nome...", color = Color(0xFF64748B)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category & Upgradable chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterCategory == null,
                        onClick = { filterCategory = null },
                        label = { Text("Todas") }
                    )
                    CardCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { filterCategory = if (filterCategory == cat) null else cat },
                            label = { Text(cat.labelPt) }
                        )
                    }
                    FilterChip(
                        selected = onlyUpgradable,
                        onClick = { onlyUpgradable = !onlyUpgradable },
                        label = { Text("Melhoráveis (${profile.upgradableCount()})") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF065F46), selectedLabelColor = Color.White)
                    )
                }

                // Rarity & Sort chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CardRarity.entries.forEach { rar ->
                        FilterChip(
                            selected = filterRarity == rar,
                            onClick = { filterRarity = if (filterRarity == rar) null else rar },
                            label = { Text(rar.labelPt, color = if (filterRarity == rar) Color.Black else Color(rar.colorHex)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(rar.colorHex))
                        )
                    }
                    CardSortMode.entries.forEach { s ->
                        FilterChip(
                            selected = sortMode == s,
                            onClick = { sortMode = s },
                            label = { Text("Ord: ${s.label}") }
                        )
                    }
                }
            }
        }

        // 3. 4-Column Collection Grid
        val rows = filteredCards.chunked(4)
        items(rows.size) { rowIndex ->
            val rowCards = rows[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowCards.forEach { spec ->
                    val own = profile.cardCollection[spec.id]
                    val isLocked = own == null
                    CardTile(
                        spec = spec,
                        level = own?.level ?: spec.rarity.startingLevel,
                        copies = own?.copies,
                        isLocked = isLocked,
                        isEvolvedUnlocked = own?.evolutionUnlocked == true,
                        isEvoSlotActive = spec.id in profile.activeDeck.evoSlotIds,
                        modifier = Modifier
                            .weight(1f)
                            .height(108.dp)
                            .clickable {
                                if (selectedSlotForSwap >= 0 && !isLocked) {
                                    onReplaceCardInSlot(selectedSlotForSwap, spec.id)
                                    selectedSlotForSwap = -1
                                } else {
                                    onOpenCardDetail(spec.id)
                                }
                            }
                    )
                }
                repeat(4 - rowCards.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    if (showRenameDialog) {
        var nameInput by remember { mutableStateOf(profile.activeDeck.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Renomear Deck", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameDeck(nameInput)
                    showRenameDialog = false
                }) { Text("SALVAR") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Copiar '${profile.activeDeck.name}' para:", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    profile.decks.forEachIndexed { idx, d ->
                        if (idx != profile.activeDeckIndex) {
                            OutlinedButton(
                                onClick = {
                                    onCopyDeckTo(idx)
                                    showCopyDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Deck ${idx + 1} (${d.name})", color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCopyDialog = false }) { Text("FECHAR") }
            }
        )
    }
}

@Composable
fun CardDetailModal(
    cardId: String,
    profile: RoyaleProfileState,
    onDismiss: () -> Unit,
    onUpgradeCard: (String, Boolean) -> Unit,
    onApplyWildcard: (String, Int) -> Unit,
    onApplyBook: (String, Boolean) -> Unit,
    onApplyEvoShard: (String) -> Unit,
    onAddToDeck: (String) -> Unit,
    onToggleEvoSlot: (String) -> Unit
) {
    val spec = RoyaleCardCatalog.get(cardId) ?: return
    val own = profile.cardCollection[cardId]
    val level = own?.level ?: spec.rarity.startingLevel
    val copies = own?.copies ?: 0
    val reqCopies = spec.upgradeCardRequirement(level)
    val goldCost = spec.upgradeGoldCost(level)
    val canUpgrade = own != null && level < spec.rarity.maxLevel && copies >= reqCopies && profile.gold >= goldCost
    val hasMagicCoin = (profile.magicItems[MagicItemType.MAGIC_COIN] ?: 0) > 0
    val canCoinUpgrade = own != null && level < spec.rarity.maxLevel && copies >= reqCopies && hasMagicCoin

    val wildType = when (spec.rarity) {
        CardRarity.COMMON -> MagicItemType.WILD_COMMON
        CardRarity.RARE -> MagicItemType.WILD_RARE
        CardRarity.EPIC -> MagicItemType.WILD_EPIC
        CardRarity.LEGENDARY -> MagicItemType.WILD_LEGENDARY
        CardRarity.CHAMPION -> MagicItemType.WILD_CHAMPION
    }
    val wildCount = profile.magicItems[wildType] ?: 0
    val bookCount = profile.magicItems[MagicItemType.BOOK_OF_CARDS] ?: 0
    val bookOfBooksCount = profile.magicItems[MagicItemType.BOOK_OF_BOOKS] ?: 0
    val evoShardCount = profile.magicItems[MagicItemType.EVOLUTION_SHARD] ?: 0
    val unlockArena = RoyaleCardCatalog.arenas.getOrNull(spec.unlockArenaIndex)?.name ?: "Estádio"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF0B1120),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, Color(spec.rarity.colorHex)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 18.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(spec.name, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text(
                                text = "${spec.rarity.labelPt} • ${spec.category.labelPt} (${spec.subCategory})",
                                color = Color(spec.rarity.colorHex),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("card_detail_close_button")) {
                            Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }
                }

                // Artwork + Summary + Progress
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CardTile(
                            spec = spec,
                            level = level,
                            copies = if (own != null) copies else null,
                            isLocked = own == null,
                            isEvolvedUnlocked = own?.evolutionUnlocked == true,
                            isEvoSlotActive = cardId in profile.activeDeck.evoSlotIds,
                            modifier = Modifier.size(width = 96.dp, height = 128.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(spec.description, color = Color(0xFFCBD5E1), fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
                            Text("Desbloqueia em: $unlockArena", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                            if (own != null && level < spec.rarity.maxLevel) {
                                val pct = if (reqCopies > 0) (copies.toFloat() / reqCopies.toFloat()).coerceIn(0f, 1f) else 1f
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = if (copies >= reqCopies) Color(0xFF4ADE80) else Color(0xFF38BDF8),
                                    trackColor = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Cópias: $copies / $reqCopies • Nível $level → ${level + 1}",
                                    color = Color.White,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.sp
                                )
                            } else if (own == null) {
                                Surface(color = Color(0xFF7F1D1D), shape = RoundedCornerShape(6.dp)) {
                                    Text("Carta ainda não desbloqueada", color = Color.White, fontFamily = JetBrainsMonoFamily, fontSize = 10.sp, modifier = Modifier.padding(6.dp))
                                }
                            }
                        }
                    }
                }

                // Complete Technical Attributes Grid (Sections 33-36)
                item {
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ATRIBUTOS TÉCNICOS (NÍVEL $level)", color = Color(0xFF38BDF8), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            if (spec.hpAt11 > 0) AttrRow("Pontos de Vida (HP)", "${spec.hpAtLevel(level)}")
                            if (spec.shieldHpAt11 > 0) AttrRow("Escudo", "${spec.shieldHpAtLevel(level)}")
                            if (spec.damageAt11 > 0) AttrRow("Dano por Ataque", "${spec.damageAtLevel(level)}")
                            if (spec.dpsAtLevel(level) > 0) AttrRow("Dano por Segundo (DPS)", "${spec.dpsAtLevel(level).roundToInt()}")
                            if (spec.spawnCount > 1) AttrRow("Quantidade Invocada", "${spec.spawnCount}x")
                            if (spec.chargeDamageAt11 > 0) AttrRow("Dano de Investida", "${spec.chargeDamageAtLevel(level)}")
                            if (spec.deathDamageAt11 > 0) AttrRow("Dano ao Morrer", "${spec.deathDamageAtLevel(level)}")
                            if (spec.damageAt11 > 0 && spec.crownTowerDamageRatio < 1f) {
                                AttrRow("Dano na Torre da Coroa", "${spec.crownTowerDamageAtLevel(level)} (${(spec.crownTowerDamageRatio * 100).toInt()}%)")
                            }
                            if (spec.hitSpeedSec > 0f && spec.damageAt11 > 0) AttrRow("Velocidade de Ataque", "${spec.hitSpeedSec}s")
                            if (spec.movementSpeed > 0f) AttrRow("Velocidade de Movimento", "${spec.movementSpeed} unid/s")
                            if (spec.attackRange > 0f) AttrRow("Alcance de Ataque", "${spec.attackRange} unid.")
                            if (spec.areaSplashRadius > 0f) AttrRow("Raio de Área (Splash)", "${spec.areaSplashRadius} unid.")
                            AttrRow("Alvos", spec.targetMask.labelPt)
                            AttrRow("Tempo de Mobilização", "${spec.deployTimeSec}s")
                            if (spec.lifetimeSec > 0f) AttrRow("Duração da Construção", "${spec.lifetimeSec.toInt()}s")
                            if (spec.spellDurationSec > 0f) AttrRow("Duração do Feitiço", "${spec.spellDurationSec}s")
                            AttrRow("Custo de Elixir", "${spec.elixirCost}")
                        }
                    }
                }

                // Special Ability & Evolution
                if (spec.ability != null) {
                    item {
                        Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⚡ HABILIDADE: ${spec.ability.name}", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text(spec.ability.description, color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (spec.evolution != null) {
                    item {
                        val evoShards = own?.evolutionShards ?: 0
                        val evoUnlocked = own?.evolutionUnlocked == true
                        Surface(
                            color = Color(0xFF3B0764),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFD946EF))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("◆ EVOLUÇÃO: ${spec.evolution.name}", color = Color(0xFFF5D0FE), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text("${spec.evolution.description} (Ciclos: ${spec.evolution.cyclesRequired})", color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
                                if (own != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (evoUnlocked) "Evolução Desbloqueada!" else "Fragmentos: $evoShards / 6",
                                            color = Color(0xFF4ADE80),
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 11.sp
                                        )
                                        if (!evoUnlocked && evoShardCount > 0) {
                                            Button(
                                                onClick = { onApplyEvoShard(cardId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD946EF)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("+1 Fragmento ($evoShardCount disp.)", fontSize = 10.sp)
                                            }
                                        } else if (evoUnlocked && cardId in profile.activeDeck.cardIds) {
                                            val active = cardId in profile.activeDeck.evoSlotIds
                                            Button(onClick = { onToggleEvoSlot(cardId) }) {
                                                Text(if (active) "Remover Slot Evo" else "Ativar no Slot Evo", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Actions: Upgrade, Wildcards, Books, Add to Deck
                if (own != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onUpgradeCard(cardId, false) },
                                    enabled = canUpgrade,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (level >= spec.rarity.maxLevel) "NÍVEL MÁX" else "MELHORAR (🪙$goldCost)", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }

                                if (canCoinUpgrade) {
                                    Button(
                                        onClick = { onUpgradeCard(cardId, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15), contentColor = Color.Black),
                                        modifier = Modifier.weight(0.8f).height(48.dp)
                                    ) {
                                        Text("Moeda Mágica", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            // Magic Items row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (wildCount > 0) {
                                    OutlinedButton(onClick = { onApplyWildcard(cardId, 1) }, modifier = Modifier.weight(1f)) {
                                        Text("+1 Curinga ($wildCount)", color = Color(0xFF38BDF8), fontSize = 10.sp)
                                    }
                                }
                                if (bookCount > 0 && spec.rarity != CardRarity.CHAMPION && copies < reqCopies) {
                                    OutlinedButton(onClick = { onApplyBook(cardId, false) }, modifier = Modifier.weight(1f)) {
                                        Text("Livro ($bookCount)", color = Color(0xFF60A5FA), fontSize = 10.sp)
                                    }
                                }
                                if (bookOfBooksCount > 0 && copies < reqCopies) {
                                    OutlinedButton(onClick = { onApplyBook(cardId, true) }, modifier = Modifier.weight(1f)) {
                                        Text("Livro Livros ($bookOfBooksCount)", color = Color(0xFFFACC15), fontSize = 10.sp)
                                    }
                                }
                            }

                            Button(
                                onClick = { onAddToDeck(cardId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (cardId in profile.activeDeck.cardIds) "JÁ ESTÁ NO DECK ATIVO" else "USAR NO DECK ATIVO", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttrRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF94A3B8), fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
        Text(value, color = Color.White, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
    HorizontalDivider(color = Color(0xFF1E293B))
}
