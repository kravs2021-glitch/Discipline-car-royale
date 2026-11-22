package com.example.royale.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.royale.AiDifficulty
import com.example.royale.ArenaMetrics
import com.example.royale.BattleMode
import com.example.royale.BattleResultSummary
import com.example.royale.BattleSnapshot
import com.example.royale.CardOwnership
import com.example.royale.CardRarity
import com.example.royale.ChestSlot
import com.example.royale.ChestType
import com.example.royale.ClanChatMsg
import com.example.royale.ClanMember
import com.example.royale.DeckPreset
import com.example.royale.EmoteDef
import com.example.royale.FriendEntry
import com.example.royale.MagicItemType
import com.example.royale.MatchRecord
import com.example.royale.RoyaleAudioEngine
import com.example.royale.RoyaleBattleEngine
import com.example.royale.RoyaleCardCatalog
import com.example.royale.RoyaleSettings
import com.example.royale.TeamSide
import com.example.royale.TowerTroopId
import com.example.royale.data.RoyaleProfileState
import com.example.royale.data.RoyaleSaveCodec
import com.example.royale.data.RoyaleSaveEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.min
import kotlin.random.Random

enum class RoyaleNavTab(val label: String) {
    SHOP("Loja"),
    CARDS("Cartas"),
    BATTLE("Batalha"),
    CLAN("Clã"),
    PROFILE("Perfil")
}

enum class RoyaleModal {
    NONE,
    TROPHY_ROAD,
    ARENAS,
    MODE_SELECTOR,
    PASS_ROYALE,
    ACHIEVEMENTS,
    HISTORY,
    COSMETICS,
    MAGIC_ITEMS,
    SETTINGS,
    PROFILE_EDIT
}

data class ChestOpeningReward(
    val chestType: ChestType,
    val gold: Int,
    val gems: Int,
    val cards: List<Pair<String, Int>>
)

data class ShopOfferItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val section: String,
    val costGold: Int = 0,
    val costGems: Int = 0,
    val cardId: String? = null,
    val cardCopies: Int = 0,
    val grantGold: Int = 0,
    val grantGems: Int = 0,
    val magicItem: MagicItemType? = null,
    val magicCount: Int = 0,
    val emoteId: String? = null,
    val skinName: String? = null,
    val towerTroop: TowerTroopId? = null,
    val chestType: ChestType? = null,
    val accentHex: Long = 0xFF38BDF8
)

data class ClanDirectoryEntry(
    val name: String,
    val tag: String,
    val emblem: String,
    val description: String,
    val minTrophies: Int,
    val membersCount: Int,
    val totalTrophies: Int
)

data class MatchmakingState(
    val isSearching: Boolean = false,
    val opponentName: String = "",
    val opponentArenaName: String = "",
    val mode: BattleMode = BattleMode.LADDER_1V1,
    val difficulty: AiDifficulty = AiDifficulty.NORMAL,
    val friendlyOpponent: FriendEntry? = null
)

class RoyaleViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).royaleSaveDao()
    val audio = RoyaleAudioEngine()

    private val _profile = MutableStateFlow(RoyaleProfileState())
    val profile: StateFlow<RoyaleProfileState> = _profile.asStateFlow()

    private val _activeTab = MutableStateFlow(RoyaleNavTab.BATTLE)
    val activeTab: StateFlow<RoyaleNavTab> = _activeTab.asStateFlow()

    private val _activeModal = MutableStateFlow(RoyaleModal.NONE)
    val activeModal: StateFlow<RoyaleModal> = _activeModal.asStateFlow()

    private val _selectedCardDetailId = MutableStateFlow<String?>(null)
    val selectedCardDetailId: StateFlow<String?> = _selectedCardDetailId.asStateFlow()

    private val _selectedMatchDetail = MutableStateFlow<MatchRecord?>(null)
    val selectedMatchDetail: StateFlow<MatchRecord?> = _selectedMatchDetail.asStateFlow()

    private val _chestRewardModal = MutableStateFlow<ChestOpeningReward?>(null)
    val chestRewardModal: StateFlow<ChestOpeningReward?> = _chestRewardModal.asStateFlow()

    private val _matchmaking = MutableStateFlow<MatchmakingState?>(null)
    val matchmaking: StateFlow<MatchmakingState?> = _matchmaking.asStateFlow()

    private val _battleSnapshot = MutableStateFlow<BattleSnapshot?>(null)
    val battleSnapshot: StateFlow<BattleSnapshot?> = _battleSnapshot.asStateFlow()

    private val _isBattlePaused = MutableStateFlow(false)
    val isBattlePaused: StateFlow<Boolean> = _isBattlePaused.asStateFlow()

    private val _nowEpochMs = MutableStateFlow(System.currentTimeMillis())
    val nowEpochMs: StateFlow<Long> = _nowEpochMs.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _clanDirectory = MutableStateFlow(defaultClanDirectory())
    val clanDirectory: StateFlow<List<ClanDirectoryEntry>> = _clanDirectory.asStateFlow()

    private var currentEngine: RoyaleBattleEngine? = null
    private var battleLoopJob: Job? = null
    private var matchmakingJob: Job? = null
    private var lastFriendlyChallengeTarget: FriendEntry? = null
    private var hasRecordedCurrentMatchResult = false

    val shopCatalog: List<ShopOfferItem> = buildShopCatalog()

    init {
        viewModelScope.launch {
            val saved = dao.getProfileOnce()
            if (saved != null) {
                val decoded = RoyaleSaveCodec.decode(saved.jsonPayload)
                _profile.value = decoded
                syncAudio(decoded.settings)
            } else {
                persistState(_profile.value)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _nowEpochMs.value = System.currentTimeMillis()
            }
        }
    }

    private fun syncAudio(settings: RoyaleSettings) {
        audio.sfxEnabled = settings.sfxEnabled
        audio.musicEnabled = settings.musicEnabled
        audio.masterVolume = settings.volume
    }

    private fun updateProfile(transform: (RoyaleProfileState) -> RoyaleProfileState) {
        val next = transform(_profile.value)
        _profile.value = next
        syncAudio(next.settings)
        viewModelScope.launch { persistState(next) }
    }

    private suspend fun persistState(state: RoyaleProfileState) {
        dao.upsertProfile(RoyaleSaveEntity(jsonPayload = RoyaleSaveCodec.encode(state)))
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun selectTab(tab: RoyaleNavTab) {
        audio.playSfx(RoyaleAudioEngine.SfxType.CLICK)
        _activeTab.value = tab
        if (tab == RoyaleNavTab.CLAN && _profile.value.clanUnreadCount > 0) {
            updateProfile { it.copy(clanUnreadCount = 0) }
        }
    }

    fun openModal(modal: RoyaleModal) {
        audio.playSfx(RoyaleAudioEngine.SfxType.CLICK)
        _activeModal.value = modal
    }

    fun closeModal() {
        _activeModal.value = RoyaleModal.NONE
    }

    fun openCardDetail(cardId: String) {
        audio.playSfx(RoyaleAudioEngine.SfxType.CLICK)
        _selectedCardDetailId.value = cardId
    }

    fun closeCardDetail() {
        _selectedCardDetailId.value = null
    }

    fun openMatchDetail(record: MatchRecord) {
        _selectedMatchDetail.value = record
    }

    fun closeMatchDetail() {
        _selectedMatchDetail.value = null
    }

    fun closeChestRewardModal() {
        _chestRewardModal.value = null
    }

    fun completeTutorial() {
        updateProfile { it.copy(tutorialCompleted = true) }
    }

    // ==================== BATTLE & MATCHMAKING ====================

    fun startBattleFromHome() {
        val state = _profile.value
        if (state.activeDeck.cardIds.size != 8) {
            showToast("Complete seu deck com 8 cartas antes de batalhar!")
            _activeTab.value = RoyaleNavTab.CARDS
            return
        }
        lastFriendlyChallengeTarget = null
        launchMatchmaking(state.selectedBattleMode, state.selectedAiDifficulty, null)
    }

    fun challengeFriend(friend: FriendEntry) {
        val state = _profile.value
        if (state.activeDeck.cardIds.size != 8) {
            showToast("Complete seu deck com 8 cartas antes de desafiar!")
            return
        }
        lastFriendlyChallengeTarget = friend
        launchMatchmaking(BattleMode.FRIENDLY, AiDifficulty.HARD, friend)
    }

    fun cancelMatchmaking() {
        matchmakingJob?.cancel()
        _matchmaking.value = null
    }

    private fun launchMatchmaking(
        mode: BattleMode,
        difficulty: AiDifficulty,
        friendlyTarget: FriendEntry?
    ) {
        matchmakingJob?.cancel()
        val state = _profile.value
        val arena = RoyaleCardCatalog.arenaForTrophies(state.trophies)
        val oppName = friendlyTarget?.name ?: listOf(
            "Kaelen_IA", "ValquiriaSul_IA", "SirRoyale_IA", "MagoArcano_IA", "ComandanteX_IA"
        ).random()

        _matchmaking.value = MatchmakingState(
            isSearching = true,
            opponentName = oppName,
            opponentArenaName = arena.name,
            mode = mode,
            difficulty = difficulty,
            friendlyOpponent = friendlyTarget
        )

        matchmakingJob = viewModelScope.launch {
            delay(1150L)
            _matchmaking.value = null
            startBattleEngine(mode, difficulty, oppName, friendlyTarget?.level ?: state.level)
        }
    }

    private fun startBattleEngine(
        mode: BattleMode,
        difficulty: AiDifficulty,
        oppName: String,
        oppLevel: Int
    ) {
        battleLoopJob?.cancel()
        hasRecordedCurrentMatchResult = false
        _isBattlePaused.value = false
        val state = _profile.value
        val arena = RoyaleCardCatalog.arenaForTrophies(state.trophies)
        val levelsMap = state.cardCollection.mapValues { it.value.level }

        val engine = RoyaleBattleEngine(
            arena = arena,
            mode = mode,
            aiDifficulty = difficulty,
            playerName = state.playerName,
            playerLevel = state.level,
            playerDeckIds = state.activeDeck.cardIds,
            playerCardLevels = levelsMap,
            playerEvoSlotIds = state.activeDeck.evoSlotIds.toSet(),
            towerTroop = state.selectedTowerTroop,
            playerTrophies = state.trophies,
            enemyName = oppName,
            enemyLevel = oppLevel,
            onSfx = { audio.playSfx(it) }
        )
        currentEngine = engine
        _battleSnapshot.value = engine.snapshot()

        battleLoopJob = viewModelScope.launch {
            var lastTimeNs = System.nanoTime()
            while (isActive) {
                delay(33L)
                val nowNs = System.nanoTime()
                val dt = ((nowNs - lastTimeNs) / 1_000_000_000.0).toFloat().coerceIn(0.01f, 0.05f)
                lastTimeNs = nowNs

                if (!_isBattlePaused.value) {
                    engine.step(dt)
                    val snap = engine.snapshot()
                    _battleSnapshot.value = snap
                    if (snap.isFinished && snap.result != null && !hasRecordedCurrentMatchResult) {
                        hasRecordedCurrentMatchResult = true
                        onBattleFinished(snap)
                        break
                    }
                }
            }
        }
    }

    fun playCardInBattle(handIndex: Int, arenaX: Float, arenaY: Float): Boolean {
        val engine = currentEngine ?: return false
        val ok = engine.playCard(handIndex, arenaX, arenaY)
        if (ok) {
            _battleSnapshot.value = engine.snapshot()
        }
        return ok
    }

    fun canDeployCardAt(cardId: String, arenaX: Float, arenaY: Float): Boolean {
        return currentEngine?.canDeployCardAt(cardId, arenaX, arenaY, TeamSide.PLAYER) ?: false
    }

    fun activateChampionAbility(): Boolean {
        val engine = currentEngine ?: return false
        val ok = engine.activateChampionAbility()
        if (ok) _battleSnapshot.value = engine.snapshot()
        return ok
    }

    fun sendBattleEmote(emote: EmoteDef) {
        currentEngine?.triggerPlayerEmote(emote)
        _battleSnapshot.value = currentEngine?.snapshot()
    }

    fun toggleBattlePause() {
        _isBattlePaused.value = !_isBattlePaused.value
    }

    fun forfeitBattle() {
        val snap = _battleSnapshot.value ?: return
        if (!hasRecordedCurrentMatchResult) {
            hasRecordedCurrentMatchResult = true
            val forfeitSummary = BattleResultSummary(
                playerCrowns = snap.playerCrowns,
                enemyCrowns = 3,
                won = false,
                draw = false,
                trophiesDelta = if (snap.mode.trophiesEnabled) -min(20, _profile.value.trophies) else 0,
                goldEarned = 5,
                xpEarned = 8,
                chestAwarded = null,
                durationSec = (snap.mode.durationSec - snap.timeRemainingSec).toInt().coerceAtLeast(1),
                opponentName = snap.enemyName,
                cardStats = emptyList()
            )
            onBattleFinished(snap.copy(isFinished = true, result = forfeitSummary))
            _battleSnapshot.value = snap.copy(isFinished = true, enemyCrowns = 3, result = forfeitSummary)
        }
        battleLoopJob?.cancel()
    }

    fun exitBattleScreen() {
        battleLoopJob?.cancel()
        currentEngine = null
        _battleSnapshot.value = null
        _isBattlePaused.value = false
    }

    fun playAgainBattle() {
        val snap = _battleSnapshot.value ?: return
        val mode = snap.mode
        val diff = _profile.value.selectedAiDifficulty
        exitBattleScreen()
        launchMatchmaking(mode, diff, lastFriendlyChallengeTarget)
    }

    private fun onBattleFinished(snap: BattleSnapshot) {
        val res = snap.result ?: return
        updateProfile { s ->
            val newTrophies = (s.trophies + res.trophiesDelta).coerceAtLeast(0)
            val newBest = maxOf(s.bestTrophies, newTrophies)
            val newStreak = if (res.won) s.winStreak + 1 else if (res.draw) s.winStreak else 0
            val newBestStreak = maxOf(s.bestWinStreak, newStreak)

            var xp = s.xp + res.xpEarned
            var level = s.level
            while (xp >= (100 + (level - 1) * 80) && level < 15) {
                xp -= (100 + (level - 1) * 80)
                level++
            }

            val updatedChests = if (res.chestAwarded != null && s.chestSlots.size < 4) {
                val occupied = s.chestSlots.map { it.slotIndex }.toSet()
                val freeIndex = (0..3).firstOrNull { it !in occupied }
                if (freeIndex != null) {
                    s.chestSlots + ChestSlot(freeIndex, res.chestAwarded, null)
                } else s.chestSlots
            } else s.chestSlots

            val record = MatchRecord(
                id = UUID.randomUUID().toString(),
                timestampMs = System.currentTimeMillis(),
                modeTitle = snap.mode.title,
                opponentName = res.opponentName,
                playerCrowns = res.playerCrowns,
                enemyCrowns = res.enemyCrowns,
                won = res.won,
                trophiesDelta = res.trophiesDelta,
                durationSec = res.durationSec,
                deckCardIds = s.activeDeck.cardIds,
                cardStats = res.cardStats
            )

            s.copy(
                level = level,
                xp = xp,
                trophies = newTrophies,
                bestTrophies = newBest,
                gold = s.gold + res.goldEarned,
                wins = s.wins + if (res.won) 1 else 0,
                losses = s.losses + if (!res.won && !res.draw) 1 else 0,
                winStreak = newStreak,
                bestWinStreak = newBestStreak,
                totalTowersDestroyed = s.totalTowersDestroyed + res.playerCrowns,
                duoMatchesPlayed = s.duoMatchesPlayed + if (snap.mode == BattleMode.DUO_2V2) 1 else 0,
                passCrowns = s.passCrowns + res.playerCrowns,
                chestSlots = updatedChests,
                matchHistory = (listOf(record) + s.matchHistory).take(30)
            )
        }
    }

    // ==================== CARDS, DECKS, UPGRADES, MAGIC ITEMS ====================

    fun setBattleMode(mode: BattleMode) {
        audio.playSfx(RoyaleAudioEngine.SfxType.CLICK)
        updateProfile { it.copy(selectedBattleMode = mode) }
    }

    fun setAiDifficulty(diff: AiDifficulty) {
        audio.playSfx(RoyaleAudioEngine.SfxType.CLICK)
        updateProfile { it.copy(selectedAiDifficulty = diff) }
    }

    fun selectDeckIndex(index: Int) {
        audio.playSfx(RoyaleAudioEngine.SfxType.CLICK)
        updateProfile { it.copy(activeDeckIndex = index.coerceIn(0, it.decks.lastIndex)) }
    }

    fun renameActiveDeck(newName: String) {
        val clean = newName.trim().take(20)
        if (clean.isEmpty()) return
        updateProfile { s ->
            val updated = s.decks.mapIndexed { idx, d ->
                if (idx == s.activeDeckIndex) d.copy(name = clean) else d
            }
            s.copy(decks = updated)
        }
        showToast("Deck renomeado!")
    }

    fun replaceCardInActiveDeck(slotIndex: Int, newCardId: String) {
        if (newCardId !in _profile.value.cardCollection) {
            showToast("Desbloqueie a carta primeiro em baús ou na loja.")
            return
        }
        updateProfile { s ->
            val currentDeck = s.activeDeck
            val list = currentDeck.cardIds.toMutableList()
            val existingIdx = list.indexOf(newCardId)
            if (existingIdx >= 0 && existingIdx != slotIndex && slotIndex < list.size) {
                val old = list[slotIndex]
                list[slotIndex] = newCardId
                list[existingIdx] = old
            } else if (slotIndex in list.indices) {
                list[slotIndex] = newCardId
            } else if (list.size < 8 && newCardId !in list) {
                list.add(newCardId)
            }
            val cleanEvo = currentDeck.evoSlotIds.filter { it in list }.take(2)
            val updated = s.decks.mapIndexed { idx, d ->
                if (idx == s.activeDeckIndex) d.copy(cardIds = list, evoSlotIds = cleanEvo) else d
            }
            s.copy(decks = updated)
        }
        audio.playSfx(RoyaleAudioEngine.SfxType.DEPLOY)
    }

    fun addOrSwapCardIntoActiveDeck(cardId: String) {
        val s = _profile.value
        if (cardId !in s.cardCollection) {
            showToast("Carta ainda bloqueada.")
            return
        }
        val current = s.activeDeck.cardIds
        if (cardId in current) {
            showToast("Esta carta já está no deck ativo.")
            return
        }
        if (current.size < 8) {
            updateProfile { st ->
                val updated = st.decks.mapIndexed { idx, d ->
                    if (idx == st.activeDeckIndex) d.copy(cardIds = d.cardIds + cardId) else d
                }
                st.copy(decks = updated)
            }
            showToast("Carta adicionada ao deck!")
        } else {
            // Swap out last card in deck
            replaceCardInActiveDeck(7, cardId)
            showToast("Substituída na posição 8 do deck!")
        }
    }

    fun clearActiveDeck() {
        updateProfile { s ->
            val updated = s.decks.mapIndexed { idx, d ->
                if (idx == s.activeDeckIndex) d.copy(cardIds = emptyList(), evoSlotIds = emptyList()) else d
            }
            s.copy(decks = updated)
        }
        showToast("Deck limpo. Selecione 8 cartas para jogar.")
    }

    fun copyActiveDeckTo(targetDeckIndex: Int) {
        updateProfile { s ->
            val src = s.activeDeck
            val updated = s.decks.mapIndexed { idx, d ->
                if (idx == targetDeckIndex) d.copy(cardIds = src.cardIds.toList(), evoSlotIds = src.evoSlotIds.toList()) else d
            }
            s.copy(decks = updated)
        }
        showToast("Deck copiado para o slot ${targetDeckIndex + 1}!")
    }

    fun shuffleRandomActiveDeck() {
        val s = _profile.value
        val owned = s.cardCollection.keys.toList()
        if (owned.size < 8) {
            showToast("Coleção insuficiente para gerar deck aleatório.")
            return
        }
        val picked = owned.shuffled().take(8)
        val evo = picked.filter { s.cardCollection[it]?.evolutionUnlocked == true }.take(2)
        updateProfile { st ->
            val updated = st.decks.mapIndexed { idx, d ->
                if (idx == st.activeDeckIndex) d.copy(cardIds = picked, evoSlotIds = evo) else d
            }
            st.copy(decks = updated)
        }
        audio.playSfx(RoyaleAudioEngine.SfxType.CLICK)
        showToast("Deck aleatório montado!")
    }

    fun toggleEvolutionSlot(cardId: String) {
        val s = _profile.value
        val own = s.cardCollection[cardId]
        if (own == null || !own.evolutionUnlocked) {
            showToast("Desbloqueie a Evolução desta carta com 6 Fragmentos primeiro.")
            return
        }
        updateProfile { st ->
            val deck = st.activeDeck
            if (cardId !in deck.cardIds) return@updateProfile st
            val nextEvo = if (cardId in deck.evoSlotIds) {
                deck.evoSlotIds - cardId
            } else {
                (deck.evoSlotIds + cardId).takeLast(2)
            }
            st.copy(decks = st.decks.mapIndexed { idx, d -> if (idx == st.activeDeckIndex) d.copy(evoSlotIds = nextEvo) else d })
        }
    }

    fun upgradeCard(cardId: String, useMagicCoin: Boolean = false) {
        val s = _profile.value
        val spec = RoyaleCardCatalog.get(cardId) ?: return
        val own = s.cardCollection[cardId] ?: return
        if (own.level >= spec.rarity.maxLevel) {
            showToast("Esta carta já atingiu o nível máximo!")
            return
        }
        val reqCopies = spec.upgradeCardRequirement(own.level)
        val goldCost = if (useMagicCoin) 0 else spec.upgradeGoldCost(own.level)
        if (own.copies < reqCopies) {
            showToast("Cópias insuficientes (${own.copies}/$reqCopies).")
            return
        }
        if (useMagicCoin && (s.magicItems[MagicItemType.MAGIC_COIN] ?: 0) <= 0) {
            showToast("Você não possui Moeda Mágica.")
            return
        }
        if (!useMagicCoin && s.gold < goldCost) {
            showToast("Ouro insuficiente ($goldCost necessário).")
            return
        }

        audio.playSfx(RoyaleAudioEngine.SfxType.CHEST_OPEN)
        updateProfile { st ->
            val newMagic = st.magicItems.toMutableMap()
            if (useMagicCoin) newMagic[MagicItemType.MAGIC_COIN] = (newMagic[MagicItemType.MAGIC_COIN] ?: 1) - 1

            var xp = st.xp + (own.level * 12)
            var pLevel = st.level
            while (xp >= (100 + (pLevel - 1) * 80) && pLevel < 15) {
                xp -= (100 + (pLevel - 1) * 80)
                pLevel++
            }

            val nextOwn = own.copy(level = own.level + 1, copies = own.copies - reqCopies)
            val nextMap = st.cardCollection.toMutableMap().apply { put(cardId, nextOwn) }
            st.copy(
                level = pLevel,
                xp = xp,
                gold = st.gold - goldCost,
                upgradesPerformed = st.upgradesPerformed + 1,
                cardCollection = nextMap,
                magicItems = newMagic
            )
        }
        showToast("${spec.name} evoluída para o nível ${own.level + 1}!")
    }

    fun applyWildcardToCard(cardId: String, count: Int = 1) {
        val s = _profile.value
        val spec = RoyaleCardCatalog.get(cardId) ?: return
        val own = s.cardCollection[cardId] ?: return
        val wildType = when (spec.rarity) {
            CardRarity.COMMON -> MagicItemType.WILD_COMMON
            CardRarity.RARE -> MagicItemType.WILD_RARE
            CardRarity.EPIC -> MagicItemType.WILD_EPIC
            CardRarity.LEGENDARY -> MagicItemType.WILD_LEGENDARY
            CardRarity.CHAMPION -> MagicItemType.WILD_CHAMPION
        }
        val available = s.magicItems[wildType] ?: 0
        val used = minOf(count, available)
        if (used <= 0) {
            showToast("Sem ${wildType.title} disponível.")
            return
        }
        audio.playSfx(RoyaleAudioEngine.SfxType.DEPLOY)
        updateProfile { st ->
            val magic = st.magicItems.toMutableMap().apply { put(wildType, available - used) }
            val nextMap = st.cardCollection.toMutableMap().apply {
                put(cardId, own.copy(copies = own.copies + used))
            }
            st.copy(cardCollection = nextMap, magicItems = magic)
        }
        showToast("+$used cópias adicionadas a ${spec.name}!")
    }

    fun applyBookToCard(cardId: String, isBookOfBooks: Boolean) {
        val s = _profile.value
        val spec = RoyaleCardCatalog.get(cardId) ?: return
        val own = s.cardCollection[cardId] ?: return
        val itemType = if (isBookOfBooks) MagicItemType.BOOK_OF_BOOKS else MagicItemType.BOOK_OF_CARDS
        val avail = s.magicItems[itemType] ?: 0
        if (avail <= 0) {
            showToast("Sem ${itemType.title} no inventário.")
            return
        }
        if (!isBookOfBooks && spec.rarity == CardRarity.CHAMPION) {
            showToast("Use o Livro dos Livros para cartas Campeãs.")
            return
        }
        val req = spec.upgradeCardRequirement(own.level)
        if (own.copies >= req) {
            showToast("Esta carta já possui todas as cópias necessárias!")
            return
        }
        audio.playSfx(RoyaleAudioEngine.SfxType.CHEST_OPEN)
        updateProfile { st ->
            val magic = st.magicItems.toMutableMap().apply { put(itemType, avail - 1) }
            val nextMap = st.cardCollection.toMutableMap().apply { put(cardId, own.copy(copies = req)) }
            st.copy(cardCollection = nextMap, magicItems = magic)
        }
        showToast("${spec.name} completou as cópias para melhoria!")
    }

    fun applyEvolutionShardToCard(cardId: String) {
        val s = _profile.value
        val spec = RoyaleCardCatalog.get(cardId) ?: return
        if (spec.evolution == null) {
            showToast("Esta carta não possui evolução.")
            return
        }
        val own = s.cardCollection[cardId] ?: return
        if (own.evolutionUnlocked) {
            showToast("Evolução já desbloqueada!")
            return
        }
        val avail = s.magicItems[MagicItemType.EVOLUTION_SHARD] ?: 0
        if (avail <= 0) {
            showToast("Você precisa de Fragmentos de Evolução.")
            return
        }
        audio.playSfx(RoyaleAudioEngine.SfxType.SPELL)
        updateProfile { st ->
            val magic = st.magicItems.toMutableMap().apply { put(MagicItemType.EVOLUTION_SHARD, avail - 1) }
            val newShards = own.evolutionShards + 1
            val unlocked = newShards >= 6
            val nextMap = st.cardCollection.toMutableMap().apply {
                put(cardId, own.copy(evolutionShards = newShards, evolutionUnlocked = unlocked))
            }
            st.copy(cardCollection = nextMap, magicItems = magic)
        }
        if (own.evolutionShards + 1 >= 6) {
            showToast("Evolução de ${spec.name} DESBLOQUEADA!")
        } else {
            showToast("Fragmento aplicado (${own.evolutionShards + 1}/6)")
        }
    }

    // ==================== CHESTS ====================

    fun startUnlockChest(slotIndex: Int) {
        val now = System.currentTimeMillis()
        updateProfile { s ->
            val anyRunning = s.chestSlots.any { it.unlockStartEpochMs != null && !it.isReady(now) }
            if (anyRunning) {
                showToast("Já existe um baú sendo desbloqueado no momento.")
                return@updateProfile s
            }
            val updated = s.chestSlots.map { ch ->
                if (ch.slotIndex == slotIndex && ch.unlockStartEpochMs == null) ch.copy(unlockStartEpochMs = now) else ch
            }
            s.copy(chestSlots = updated)
        }
        audio.playSfx(RoyaleAudioEngine.SfxType.CLICK)
    }

    fun openChest(slotIndex: Int, instantWithGems: Boolean) {
        val s = _profile.value
        val slot = s.chestSlots.firstOrNull { it.slotIndex == slotIndex } ?: return
        val now = System.currentTimeMillis()
        if (!instantWithGems && !slot.isReady(now)) {
            showToast("O baú ainda está desbloqueando.")
            return
        }
        if (instantWithGems && s.gems < slot.type.gemSkipCost) {
            showToast("Gemas insuficientes (${slot.type.gemSkipCost} necessárias).")
            return
        }

        val reward = generateChestReward(slot.type, s)
        audio.playSfx(RoyaleAudioEngine.SfxType.CHEST_OPEN)
        updateProfile { st ->
            val nextCollection = st.cardCollection.toMutableMap()
            reward.cards.forEach { (cid, count) ->
                val spec = RoyaleCardCatalog.get(cid) ?: return@forEach
                val existing = nextCollection[cid]
                if (existing != null) {
                    nextCollection[cid] = existing.copy(copies = existing.copies + count)
                } else {
                    nextCollection[cid] = CardOwnership(cid, spec.rarity.startingLevel, count)
                }
            }
            st.copy(
                gold = st.gold + reward.gold,
                gems = st.gems - (if (instantWithGems) slot.type.gemSkipCost else 0) + reward.gems,
                chestSlots = st.chestSlots.filterNot { it.slotIndex == slotIndex },
                cardCollection = nextCollection
            )
        }
        _chestRewardModal.value = reward
    }

    private fun generateChestReward(type: ChestType, state: RoyaleProfileState): ChestOpeningReward {
        val arenaIdx = RoyaleCardCatalog.arenaForTrophies(state.trophies).index
        val eligible = RoyaleCardCatalog.allCards.filter { it.unlockArenaIndex <= arenaIdx + 2 }
        val gold = Random.nextInt(type.minGold, type.maxGold + 1)
        val gems = if (type == ChestType.LEGENDARY || type == ChestType.MAGICAL) Random.nextInt(4, 15) else 0

        val cardRewards = mutableListOf<Pair<String, Int>>()
        repeat(type.cardStacks) { idx ->
            val targetRarity = if (idx == type.cardStacks - 1) type.guaranteedMinRarity else {
                listOf(CardRarity.COMMON, CardRarity.COMMON, CardRarity.RARE, CardRarity.EPIC).random()
            }
            val pool = eligible.filter { it.rarity == targetRarity }.ifEmpty { eligible }
            val picked = pool.random()
            val qty = when (picked.rarity) {
                CardRarity.COMMON -> Random.nextInt(8, 22)
                CardRarity.RARE -> Random.nextInt(3, 8)
                CardRarity.EPIC -> Random.nextInt(1, 4)
                CardRarity.LEGENDARY, CardRarity.CHAMPION -> 1
            }
            cardRewards.add(picked.id to qty)
        }
        return ChestOpeningReward(type, gold, gems, cardRewards)
    }

    // ==================== SHOP, TROPHY ROAD, PASS, ACHIEVEMENTS ====================

    fun buyShopOffer(offer: ShopOfferItem) {
        val s = _profile.value
        if (offer.id in s.shopBoughtTodayIds) {
            showToast("Oferta já resgatada!")
            return
        }
        if (s.gold < offer.costGold) {
            showToast("Ouro insuficiente.")
            return
        }
        if (s.gems < offer.costGems) {
            showToast("Gemas insuficientes.")
            return
        }

        audio.playSfx(RoyaleAudioEngine.SfxType.CHEST_OPEN)
        var chestModalReward: ChestOpeningReward? = null

        updateProfile { st ->
            val coll = st.cardCollection.toMutableMap()
            if (offer.cardId != null && offer.cardCopies > 0) {
                val spec = RoyaleCardCatalog.get(offer.cardId)
                if (spec != null) {
                    val cur = coll[offer.cardId]
                    coll[offer.cardId] = if (cur != null) cur.copy(copies = cur.copies + offer.cardCopies)
                    else CardOwnership(offer.cardId, spec.rarity.startingLevel, offer.cardCopies)
                }
            }

            val magic = st.magicItems.toMutableMap()
            if (offer.magicItem != null && offer.magicCount > 0) {
                magic[offer.magicItem] = (magic[offer.magicItem] ?: 0) + offer.magicCount
            }

            var chestGold = 0
            var chestGems = 0
            if (offer.chestType != null) {
                val generated = generateChestReward(offer.chestType, st)
                chestModalReward = generated
                chestGold = generated.gold
                chestGems = generated.gems
                generated.cards.forEach { (cid, qty) ->
                    val spec = RoyaleCardCatalog.get(cid) ?: return@forEach
                    val cur = coll[cid]
                    coll[cid] = if (cur != null) cur.copy(copies = cur.copies + qty) else CardOwnership(cid, spec.rarity.startingLevel, qty)
                }
            }

            val emotes = if (offer.emoteId != null) st.unlockedEmotes + offer.emoteId else st.unlockedEmotes
            val skins = if (offer.skinName != null) st.unlockedSkins + offer.skinName else st.unlockedSkins
            val troops = if (offer.towerTroop != null) st.unlockedTowerTroops + offer.towerTroop else st.unlockedTowerTroops

            st.copy(
                gold = st.gold - offer.costGold + offer.grantGold + chestGold,
                gems = st.gems - offer.costGems + offer.grantGems + chestGems,
                cardCollection = coll,
                magicItems = magic,
                unlockedEmotes = emotes,
                unlockedSkins = skins,
                unlockedTowerTroops = troops,
                shopBoughtTodayIds = if (offer.section == "Ofertas Diárias" || offer.emoteId != null || offer.skinName != null || offer.towerTroop != null) {
                    st.shopBoughtTodayIds + offer.id
                } else st.shopBoughtTodayIds
            )
        }
        chestModalReward?.let { _chestRewardModal.value = it }
        showToast("${offer.title} adquirido!")
    }

    fun claimTrophyMilestone(trophies: Int) {
        val m = RoyaleCardCatalog.trophyMilestones.firstOrNull { it.trophies == trophies } ?: return
        val s = _profile.value
        if (s.trophies < trophies || trophies in s.claimedTrophyMilestones) return

        audio.playSfx(RoyaleAudioEngine.SfxType.CHEST_OPEN)
        var chestPopup: ChestOpeningReward? = null
        updateProfile { st ->
            val coll = st.cardCollection.toMutableMap()
            if (m.rewardCardId != null && m.rewardCardCopies > 0) {
                val spec = RoyaleCardCatalog.get(m.rewardCardId)
                if (spec != null) {
                    val cur = coll[m.rewardCardId]
                    coll[m.rewardCardId] = if (cur != null) cur.copy(copies = cur.copies + m.rewardCardCopies)
                    else CardOwnership(m.rewardCardId, spec.rarity.startingLevel, m.rewardCardCopies)
                }
            }
            var extraGold = 0
            var extraGems = 0
            if (m.rewardChest != null) {
                val gen = generateChestReward(m.rewardChest, st)
                chestPopup = gen
                extraGold = gen.gold
                extraGems = gen.gems
                gen.cards.forEach { (cid, qty) ->
                    val spec = RoyaleCardCatalog.get(cid) ?: return@forEach
                    val cur = coll[cid]
                    coll[cid] = if (cur != null) cur.copy(copies = cur.copies + qty) else CardOwnership(cid, spec.rarity.startingLevel, qty)
                }
            }
            st.copy(
                gold = st.gold + m.rewardGold + extraGold,
                gems = st.gems + m.rewardGems + extraGems,
                cardCollection = coll,
                claimedTrophyMilestones = st.claimedTrophyMilestones + trophies
            )
        }
        chestPopup?.let { _chestRewardModal.value = it }
        showToast("Recompensa de ${m.title} resgatada!")
    }

    fun unlockPassPremium() {
        val s = _profile.value
        if (s.passPremiumUnlocked) return
        if (s.gems < 120) {
            showToast("São necessárias 120 Gemas para ativar o Passe Dourado.")
            return
        }
        audio.playSfx(RoyaleAudioEngine.SfxType.CHEST_OPEN)
        updateProfile { it.copy(gems = it.gems - 120, passPremiumUnlocked = true) }
        showToast("Passe Dourado desbloqueado!")
    }

    fun claimPassTier(tierNumber: Int, isPremium: Boolean) {
        val tier = RoyaleCardCatalog.passTiers.firstOrNull { it.tier == tierNumber } ?: return
        val s = _profile.value
        if (s.passCrowns < tier.crownsRequired) return
        if (isPremium && !s.passPremiumUnlocked) return
        if (!isPremium && tierNumber in s.claimedFreePassTiers) return
        if (isPremium && tierNumber in s.claimedPremiumPassTiers) return

        audio.playSfx(RoyaleAudioEngine.SfxType.CHEST_OPEN)
        var chestPopup: ChestOpeningReward? = null
        updateProfile { st ->
            val coll = st.cardCollection.toMutableMap()
            val magic = st.magicItems.toMutableMap()
            var addGold = if (isPremium) tier.premiumGold else tier.freeGold
            var addGems = if (isPremium) tier.premiumGems else tier.freeGems

            if (!isPremium && tier.freeChest != null) {
                val gen = generateChestReward(tier.freeChest, st)
                chestPopup = gen
                addGold += gen.gold
                addGems += gen.gems
                gen.cards.forEach { (cid, qty) ->
                    val spec = RoyaleCardCatalog.get(cid) ?: return@forEach
                    val cur = coll[cid]
                    coll[cid] = if (cur != null) cur.copy(copies = cur.copies + qty) else CardOwnership(cid, spec.rarity.startingLevel, qty)
                }
            }
            if (isPremium) {
                if (tier.premiumWildEpic > 0) magic[MagicItemType.WILD_EPIC] = (magic[MagicItemType.WILD_EPIC] ?: 0) + tier.premiumWildEpic
                if (tier.premiumEvoShard > 0) magic[MagicItemType.EVOLUTION_SHARD] = (magic[MagicItemType.EVOLUTION_SHARD] ?: 0) + tier.premiumEvoShard
            }
            st.copy(
                gold = st.gold + addGold,
                gems = st.gems + addGems,
                cardCollection = coll,
                magicItems = magic,
                claimedFreePassTiers = if (!isPremium) st.claimedFreePassTiers + tierNumber else st.claimedFreePassTiers,
                claimedPremiumPassTiers = if (isPremium) st.claimedPremiumPassTiers + tierNumber else st.claimedPremiumPassTiers
            )
        }
        chestPopup?.let { _chestRewardModal.value = it }
        showToast("Patamar $tierNumber do Passe resgatado!")
    }

    fun claimAchievement(id: String) {
        val def = RoyaleCardCatalog.achievements.firstOrNull { it.id == id } ?: return
        val s = _profile.value
        if (s.achievementProgress(def) < def.target || id in s.claimedAchievements) return
        audio.playSfx(RoyaleAudioEngine.SfxType.CHEST_OPEN)
        updateProfile { st ->
            st.copy(
                gold = st.gold + def.rewardGold,
                gems = st.gems + def.rewardGems,
                claimedAchievements = st.claimedAchievements + id
            )
        }
        showToast("Conquista '${def.title}' resgatada!")
    }

    // ==================== CLAN & FRIENDS ====================

    fun sendClanChatMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val s = _profile.value
        if (!s.inClan) return
        audio.playSfx(RoyaleAudioEngine.SfxType.CLICK)
        updateProfile { st ->
            val msg = ClanChatMsg(UUID.randomUUID().toString(), st.playerName, clean, System.currentTimeMillis())
            st.copy(clanChat = (st.clanChat + msg).takeLast(40))
        }
    }

    fun requestClanCardDonation(cardId: String) {
        val spec = RoyaleCardCatalog.get(cardId) ?: return
        val s = _profile.value
        if (!s.inClan) return
        updateProfile { st ->
            val msg = ClanChatMsg(
                id = UUID.randomUUID().toString(),
                sender = st.playerName,
                text = "Pedido de doação: ${spec.name}",
                timestampMs = System.currentTimeMillis(),
                isDonationRequest = true,
                requestedCardId = cardId,
                donatedCount = 0,
                maxDonation = 4
            )
            st.copy(clanChat = (st.clanChat + msg).takeLast(40))
        }
        showToast("Pedido de ${spec.name} enviado ao clã!")
    }

    fun donateToClanRequest(msgId: String) {
        val s = _profile.value
        val msg = s.clanChat.firstOrNull { it.id == msgId } ?: return
        val cid = msg.requestedCardId ?: return
        if (msg.donatedCount >= msg.maxDonation) return
        val own = s.cardCollection[cid]
        if (own == null || own.copies < 1) {
            showToast("Você precisa de pelo menos 1 cópia para doar.")
            return
        }
        audio.playSfx(RoyaleAudioEngine.SfxType.DEPLOY)
        updateProfile { st ->
            val coll = st.cardCollection.toMutableMap().apply {
                put(cid, own.copy(copies = own.copies - 1))
            }
            val updatedChat = st.clanChat.map {
                if (it.id == msgId) it.copy(donatedCount = it.donatedCount + 1) else it
            }
            st.copy(
                gold = st.gold + 15,
                xp = st.xp + 5,
                cardCollection = coll,
                clanChat = updatedChat
            )
        }
        showToast("+15 Ouro e +5 XP pela doação!")
    }

    fun createClan(name: String, emblem: String, description: String, minTrophies: Int) {
        val clean = name.trim().take(22)
        if (clean.isEmpty()) {
            showToast("Digite um nome para o clã.")
            return
        }
        updateProfile { st ->
            st.copy(
                inClan = true,
                clanName = clean,
                clanTag = "#CL${Random.nextInt(100, 999)}",
                clanEmblem = emblem,
                clanDescription = description.ifBlank { "Clã criado por ${st.playerName}." },
                clanMinTrophies = minTrophies,
                clanMembers = listOf(ClanMember("me", st.playerName, "Líder", st.trophies, st.level, 0)),
                clanChat = listOf(ClanChatMsg("init", st.playerName, "Clã $clean fundado! Boas batalhas!", System.currentTimeMillis()))
            )
        }
        showToast("Clã '$clean' criado!")
    }

    fun joinClan(entry: ClanDirectoryEntry) {
        if (_profile.value.trophies < entry.minTrophies) {
            showToast("Requer ${entry.minTrophies} troféus.")
            return
        }
        updateProfile { st ->
            st.copy(
                inClan = true,
                clanName = entry.name,
                clanTag = entry.tag,
                clanEmblem = entry.emblem,
                clanDescription = entry.description,
                clanMinTrophies = entry.minTrophies,
                clanMembers = listOf(ClanMember("me", st.playerName, "Membro", st.trophies, st.level, 0)) + RoyaleProfileState.defaultClanMembers().drop(1),
                clanChat = RoyaleProfileState.defaultClanChat()
            )
        }
        showToast("Você entrou no clã ${entry.name}!")
    }

    fun leaveClan() {
        updateProfile { it.copy(inClan = false) }
        showToast("Você saiu do clã.")
    }

    fun addFriendByName(name: String) {
        val clean = name.trim().take(20)
        if (clean.isEmpty()) return
        updateProfile { st ->
            if (st.friends.any { it.name.equals(clean, ignoreCase = true) }) {
                showToast("Jogador já está na sua lista.")
                return@updateProfile st
            }
            val added = FriendEntry(
                id = UUID.randomUUID().toString(),
                name = clean,
                trophies = Random.nextInt(400, 2600),
                level = Random.nextInt(3, 10),
                online = true,
                statusText = "Disponível para Duelo Amistoso (IA)"
            )
            st.copy(friends = listOf(added) + st.friends)
        }
        showToast("$clean adicionado aos amigos!")
    }

    fun acceptFriendRequest(friendId: String) {
        updateProfile { st ->
            st.copy(friends = st.friends.map {
                if (it.id == friendId) it.copy(pendingIncoming = false, statusText = "Amigo • Disponível para Amistoso") else it
            })
        }
    }

    fun removeFriend(friendId: String) {
        updateProfile { st -> st.copy(friends = st.friends.filterNot { it.id == friendId }) }
    }

    // ==================== PROFILE, COSMETICS, SETTINGS ====================

    fun updatePlayerIdentity(name: String, avatar: String, bannerHex: Long, frameHex: Long) {
        val clean = name.trim().ifEmpty { "Comandante" }.take(18)
        updateProfile { st ->
            st.copy(
                playerName = clean,
                avatarIcon = avatar,
                bannerColorHex = bannerHex,
                frameColorHex = frameHex,
                clanMembers = st.clanMembers.mapIndexed { idx, m -> if (idx == 0) m.copy(name = clean) else m }
            )
        }
        showToast("Perfil atualizado!")
    }

    fun selectTowerTroop(id: TowerTroopId) {
        if (id !in _profile.value.unlockedTowerTroops) {
            showToast("Desbloqueie esta Tropa da Torre na Loja.")
            return
        }
        updateProfile { it.copy(selectedTowerTroop = id) }
        showToast("${id.title} selecionada!")
    }

    fun selectSkin(name: String, isKing: Boolean) {
        if (name !in _profile.value.unlockedSkins) {
            showToast("Visual disponível na Loja.")
            return
        }
        updateProfile {
            if (isKing) it.copy(kingSkinName = name) else it.copy(towerSkinName = name)
        }
        showToast("Visual aplicado!")
    }

    fun toggleEquipEmote(emoteId: String) {
        val s = _profile.value
        if (emoteId !in s.unlockedEmotes) return
        updateProfile { st ->
            val next = if (emoteId in st.equippedEmotes) {
                if (st.equippedEmotes.size > 1) st.equippedEmotes - emoteId else st.equippedEmotes
            } else {
                (st.equippedEmotes + emoteId).takeLast(4)
            }
            st.copy(equippedEmotes = next)
        }
    }

    fun updateSettings(settings: RoyaleSettings) {
        updateProfile { it.copy(settings = settings) }
    }

    fun resetProgress() {
        val fresh = RoyaleProfileState(tutorialCompleted = true)
        _profile.value = fresh
        syncAudio(fresh.settings)
        viewModelScope.launch { persistState(fresh) }
        showToast("Progresso reiniciado.")
    }

    private fun buildShopCatalog(): List<ShopOfferItem> = listOf(
        // Daily Offers
        ShopOfferItem("daily_free_gold", "Presente Diário Real", "Resgate grátis de ouro para iniciar o dia.", "Ofertas Diárias", costGold = 0, grantGold = 250, accentHex = 0xFF22C55E),
        ShopOfferItem("daily_knight", "25x Cavaleiro", "Lote com desconto diário para melhorar sua linha de frente.", "Ofertas Diárias", costGold = 200, cardId = "cavaleiro", cardCopies = 25, accentHex = 0xFF60A5FA),
        ShopOfferItem("daily_hog", "8x Corredor", "Carta Rara ofensiva com foco em torres.", "Ofertas Diárias", costGold = 360, cardId = "corredor", cardCopies = 8, accentHex = 0xFFF97316),
        ShopOfferItem("daily_pekka", "2x P.E.K.K.A", "Colosso Épico destruidor de tanques.", "Ofertas Diárias", costGold = 900, cardId = "pekka", cardCopies = 2, accentHex = 0xFFA855F7),
        // Featured Cards by Rarity
        ShopOfferItem("card_ewiz", "Mago Elétrico (Lendária)", "Desbloqueie ou ganhe +1 cópia de controle duplo.", "Cartas em Destaque", costGold = 2000, cardId = "mago_eletrico", cardCopies = 1, accentHex = 0xFF22D3EE),
        ShopOfferItem("card_miner", "Mineiro (Lendária)", "Unidade versátil de infiltração em qualquer lugar.", "Cartas em Destaque", costGold = 2000, cardId = "mineiro", cardCopies = 1, accentHex = 0xFFF59E0B),
        ShopOfferItem("card_champ_sk", "Rei Esqueleto (Campeão)", "Campeão invocador com habilidade ativa em batalha.", "Cartas em Destaque", costGems = 95, cardId = "rei_esqueleto", cardCopies = 1, accentHex = 0xFFFACC15),
        // Magic Items & Chests
        ShopOfferItem("magic_evo", "2x Fragmentos de Evolução", "Progrida na evolução de cartas especiais.", "Itens Mágicos e Baús", costGems = 60, magicItem = MagicItemType.EVOLUTION_SHARD, magicCount = 2, accentHex = 0xFFD946EF),
        ShopOfferItem("magic_book", "Livro de Cartas", "Completa as cópias de qualquer carta Comum, Rara ou Épica.", "Itens Mágicos e Baús", costGems = 75, magicItem = MagicItemType.BOOK_OF_CARDS, magicCount = 1, accentHex = 0xFF3B82F6),
        ShopOfferItem("chest_legendary_shop", "Baú Lendário", "Abre imediatamente com carta Lendária garantida.", "Itens Mágicos e Baús", costGems = 110, chestType = ChestType.LEGENDARY, accentHex = 0xFF06B6D4),
        // Resources (Gem -> Gold)
        ShopOfferItem("res_gold_bag", "Bolsa de 1.500 Ouro", "Converta gemas acumuladas em ouro para melhorias.", "Recursos", costGems = 20, grantGold = 1500, accentHex = 0xFFEAB308),
        ShopOfferItem("res_gold_vault", "Cofre de 5.000 Ouro", "Reserva grande de ouro para níveis altos.", "Recursos", costGems = 55, grantGold = 5000, accentHex = 0xFFFACC15),
        // Cosmetics & Tower Troops
        ShopOfferItem("cos_cannoneer", "Tropa da Torre: Canhoneiro", "Dispara tiros lentos e devastadores de canhão.", "Cosméticos e Tropas de Torre", costGold = 1200, towerTroop = TowerTroopId.CANNONEER, accentHex = 0xFFF97316),
        ShopOfferItem("cos_duchess", "Tropa da Torre: Duquesa", "Arremessa rajadas velozes de adagas defensivas.", "Cosméticos e Tropas de Torre", costGems = 45, towerTroop = TowerTroopId.DAGGER_DUCHESS, accentHex = 0xFFC084FC),
        ShopOfferItem("cos_emote_pekka", "Emote: P.E.K.K.A Borboleta", "Emote épico exclusivo para usar na arena.", "Cosméticos e Tropas de Torre", costGems = 30, emoteId = "pekka_heart", accentHex = 0xFFA855F7),
        ShopOfferItem("cos_emote_wiz", "Emote: Mago Flamejante", "Reação animada de fogo para batalhas.", "Cosméticos e Tropas de Torre", costGems = 30, emoteId = "wizard_fire", accentHex = 0xFFEF4444),
        ShopOfferItem("cos_skin_obsidian", "Visual: Torre Obsidiana Real", "Acabamento escuro e dourado para suas torres.", "Cosméticos e Tropas de Torre", costGems = 50, skinName = "Torre Obsidiana Real", accentHex = 0xFF94A3B8)
    )

    private fun defaultClanDirectory(): List<ClanDirectoryEntry> = listOf(
        ClanDirectoryEntry("Guarda de Ferro", "#GDF99", "🛡️", "Clã ativo focado em evolução diária, doações e batalhas táticas.", 0, 28, 19400),
        ClanDirectoryEntry("Coroas do Sul", "#CDS12", "👑", "Competitivo amistoso, doações rápidas toda hora.", 300, 42, 46200),
        ClanDirectoryEntry("Legião Elétrica", "#LEL77", "⚡", "Especialistas em decks de controle e ciclo rápido.", 1000, 36, 51800),
        ClanDirectoryEntry("Forja P.E.K.K.A", "#FPK04", "⚔️", "Tanques pesados e estratégia de três coroas.", 600, 19, 22900)
    )
}
