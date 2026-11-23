package com.example.royale.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.royale.RoyaleCardCatalog
import com.example.royale.data.RoyaleProfileState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun RoyaleRootEntry(
    onExitRoyaleToSuite: () -> Unit,
    royaleViewModel: RoyaleViewModel = viewModel()
) {
    val profile by royaleViewModel.profile.collectAsStateWithLifecycle()
    val activeTab by royaleViewModel.activeTab.collectAsStateWithLifecycle()
    val activeModal by royaleViewModel.activeModal.collectAsStateWithLifecycle()
    val selectedCardId by royaleViewModel.selectedCardDetailId.collectAsStateWithLifecycle()
    val selectedMatch by royaleViewModel.selectedMatchDetail.collectAsStateWithLifecycle()
    val chestReward by royaleViewModel.chestRewardModal.collectAsStateWithLifecycle()
    val matchmaking by royaleViewModel.matchmaking.collectAsStateWithLifecycle()
    val battleSnapshot by royaleViewModel.battleSnapshot.collectAsStateWithLifecycle()
    val isBattlePaused by royaleViewModel.isBattlePaused.collectAsStateWithLifecycle()
    val nowEpochMs by royaleViewModel.nowEpochMs.collectAsStateWithLifecycle()
    val toastMsg by royaleViewModel.toastMessage.collectAsStateWithLifecycle()
    val clanDirectory by royaleViewModel.clanDirectory.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            snackbarHostState.showSnackbar(it)
            royaleViewModel.clearToast()
        }
    }

    // Active Real-Time Battle Screen
    val currentBattle = battleSnapshot
    if (currentBattle != null) {
        val equippedEmoteDefs = profile.equippedEmotes.mapNotNull { id ->
            RoyaleCardCatalog.emotes.firstOrNull { it.id == id }
        }
        BattleScreen(
            snapshot = currentBattle,
            isPaused = isBattlePaused,
            settings = profile.settings,
            equippedEmotes = equippedEmoteDefs,
            onPlayCard = { idx, ax, ay -> royaleViewModel.playCardInBattle(idx, ax, ay) },
            onCanDeployAt = { cid, ax, ay -> royaleViewModel.canDeployCardAt(cid, ax, ay) },
            onActivateChampion = { royaleViewModel.activateChampionAbility() },
            onSendEmote = { royaleViewModel.sendBattleEmote(it) },
            onTogglePause = { royaleViewModel.toggleBattlePause() },
            onToggleMute = { royaleViewModel.updateSettings(profile.settings.copy(sfxEnabled = !profile.settings.sfxEnabled)) },
            onForfeit = { royaleViewModel.forfeitBattle() },
            onPlayAgain = { royaleViewModel.playAgainBattle() },
            onExitBattle = { royaleViewModel.exitBattleScreen() }
        )
        return
    }

    BackHandler {
        when {
            chestReward != null -> royaleViewModel.closeChestRewardModal()
            selectedMatch != null -> royaleViewModel.closeMatchDetail()
            matchmaking != null -> royaleViewModel.cancelMatchmaking()
            selectedCardId != null -> royaleViewModel.closeCardDetail()
            activeModal != RoyaleModal.NONE -> royaleViewModel.closeModal()
            activeTab != RoyaleNavTab.BATTLE -> royaleViewModel.selectTab(RoyaleNavTab.BATTLE)
            else -> onExitRoyaleToSuite()
        }
    }

    val upgradableCardsCount = profile.upgradableCount()
    val unclaimedRewards = profile.unclaimedRewardsCount()
    val readyChests = profile.chestSlots.count { it.isReady(nowEpochMs) }
    val pendingFriends = profile.friends.count { it.pendingIncoming }

    Scaffold(
        containerColor = Color(0xFF060911),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RoyaleTopResourceHeader(
                profile = profile,
                onBackToSuite = onExitRoyaleToSuite,
                onOpenProfile = { royaleViewModel.selectTab(RoyaleNavTab.PROFILE) },
                onOpenShopResources = { royaleViewModel.selectTab(RoyaleNavTab.SHOP) },
                onOpenSettings = { royaleViewModel.openModal(RoyaleModal.SETTINGS) }
            )
        },
        bottomBar = {
            RoyaleFiveTabBottomBar(
                activeTab = activeTab,
                cardsBadge = upgradableCardsCount,
                battleBadge = unclaimedRewards + readyChests,
                clanBadge = profile.clanUnreadCount + pendingFriends,
                onSelectTab = { royaleViewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                RoyaleNavTab.SHOP -> RoyaleShopTab(
                    profile = profile,
                    catalog = royaleViewModel.shopCatalog,
                    onBuyOffer = { royaleViewModel.buyShopOffer(it) }
                )
                RoyaleNavTab.CARDS -> RoyaleCardsDeckTab(
                    profile = profile,
                    onSelectDeck = { royaleViewModel.selectDeckIndex(it) },
                    onRenameDeck = { royaleViewModel.renameActiveDeck(it) },
                    onReplaceCardInSlot = { slot, cid -> royaleViewModel.replaceCardInActiveDeck(slot, cid) },
                    onClearDeck = { royaleViewModel.clearActiveDeck() },
                    onCopyDeckTo = { royaleViewModel.copyActiveDeckTo(it) },
                    onShuffleDeck = { royaleViewModel.shuffleRandomActiveDeck() },
                    onOpenCardDetail = { royaleViewModel.openCardDetail(it) }
                )
                RoyaleNavTab.BATTLE -> RoyaleHomeTab(
                    profile = profile,
                    nowEpochMs = nowEpochMs,
                    onStartBattle = { royaleViewModel.startBattleFromHome() },
                    onOpenModeSelector = { royaleViewModel.openModal(RoyaleModal.MODE_SELECTOR) },
                    onOpenTrophyRoad = { royaleViewModel.openModal(RoyaleModal.TROPHY_ROAD) },
                    onOpenArenas = { royaleViewModel.openModal(RoyaleModal.ARENAS) },
                    onOpenPass = { royaleViewModel.openModal(RoyaleModal.PASS_ROYALE) },
                    onOpenCardDetail = { royaleViewModel.openCardDetail(it) },
                    onOpenCardsTab = { royaleViewModel.selectTab(RoyaleNavTab.CARDS) },
                    onStartUnlockChest = { royaleViewModel.startUnlockChest(it) },
                    onOpenChest = { slot, instant -> royaleViewModel.openChest(slot, instant) }
                )
                RoyaleNavTab.CLAN -> RoyaleClanFriendsTab(
                    profile = profile,
                    clanDirectory = clanDirectory,
                    onSendChat = { royaleViewModel.sendClanChatMessage(it) },
                    onRequestDonation = { royaleViewModel.requestClanCardDonation(it) },
                    onDonateToMsg = { royaleViewModel.donateToClanRequest(it) },
                    onCreateClan = { n, e, d, m -> royaleViewModel.createClan(n, e, d, m) },
                    onJoinClan = { royaleViewModel.joinClan(it) },
                    onLeaveClan = { royaleViewModel.leaveClan() },
                    onAddFriend = { royaleViewModel.addFriendByName(it) },
                    onAcceptFriend = { royaleViewModel.acceptFriendRequest(it) },
                    onRemoveFriend = { royaleViewModel.removeFriend(it) },
                    onChallengeFriend = { royaleViewModel.challengeFriend(it) }
                )
                RoyaleNavTab.PROFILE -> RoyaleProfileStatsTab(
                    profile = profile,
                    onOpenModal = { royaleViewModel.openModal(it) },
                    onBackToSuiteHome = onExitRoyaleToSuite
                )
            }

            // First launch Tutorial
            if (!profile.tutorialCompleted) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TutorialOverlay(onComplete = { royaleViewModel.completeTutorial() })
                }
            }

            // Matchmaking Overlay
            matchmaking?.let { mm ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MatchmakingOverlay(state = mm, onCancel = { royaleViewModel.cancelMatchmaking() })
                }
            }
        }
    }

    // Active Card Detail Modal
    selectedCardId?.let { cid ->
        CardDetailModal(
            cardId = cid,
            profile = profile,
            onDismiss = { royaleViewModel.closeCardDetail() },
            onUpgradeCard = { id, coin -> royaleViewModel.upgradeCard(id, coin) },
            onApplyWildcard = { id, cnt -> royaleViewModel.applyWildcardToCard(id, cnt) },
            onApplyBook = { id, bookBooks -> royaleViewModel.applyBookToCard(id, bookBooks) },
            onApplyEvoShard = { id -> royaleViewModel.applyEvolutionShardToCard(id) },
            onAddToDeck = { id -> royaleViewModel.addOrSwapCardIntoActiveDeck(id) },
            onToggleEvoSlot = { id -> royaleViewModel.toggleEvolutionSlot(id) }
        )
    }

    // Modals
    when (activeModal) {
        RoyaleModal.NONE -> Unit
        RoyaleModal.TROPHY_ROAD -> TrophyRoadModal(
            profile = profile,
            onClaim = { royaleViewModel.claimTrophyMilestone(it) },
            onDismiss = { royaleViewModel.closeModal() }
        )
        RoyaleModal.ARENAS -> ArenasListModal(
            profile = profile,
            onDismiss = { royaleViewModel.closeModal() }
        )
        RoyaleModal.MODE_SELECTOR -> ModeSelectorModal(
            selectedMode = profile.selectedBattleMode,
            selectedDifficulty = profile.selectedAiDifficulty,
            onSelectMode = { royaleViewModel.setBattleMode(it) },
            onSelectDifficulty = { royaleViewModel.setAiDifficulty(it) },
            onDismiss = { royaleViewModel.closeModal() }
        )
        RoyaleModal.PASS_ROYALE -> PassProgressionModal(
            profile = profile,
            onUnlockPremium = { royaleViewModel.unlockPassPremium() },
            onClaimTier = { t, p -> royaleViewModel.claimPassTier(t, p) },
            onDismiss = { royaleViewModel.closeModal() }
        )
        RoyaleModal.ACHIEVEMENTS -> AchievementsModal(
            profile = profile,
            onClaim = { royaleViewModel.claimAchievement(it) },
            onDismiss = { royaleViewModel.closeModal() }
        )
        RoyaleModal.HISTORY -> HistoryAndGraphsModal(
            profile = profile,
            onOpenMatchDetail = { royaleViewModel.openMatchDetail(it) },
            onDismiss = { royaleViewModel.closeModal() }
        )
        RoyaleModal.COSMETICS -> CosmeticsAndTowerTroopsModal(
            profile = profile,
            onSelectTowerTroop = { royaleViewModel.selectTowerTroop(it) },
            onSelectSkin = { s, k -> royaleViewModel.selectSkin(s, k) },
            onToggleEmote = { royaleViewModel.toggleEquipEmote(it) },
            onDismiss = { royaleViewModel.closeModal() }
        )
        RoyaleModal.MAGIC_ITEMS -> MagicItemsInventoryModal(
            profile = profile,
            onDismiss = { royaleViewModel.closeModal() }
        )
        RoyaleModal.SETTINGS -> SettingsModal(
            settings = profile.settings,
            onUpdateSettings = { royaleViewModel.updateSettings(it) },
            onResetProgress = { royaleViewModel.resetProgress() },
            onDismiss = { royaleViewModel.closeModal() }
        )
        RoyaleModal.PROFILE_EDIT -> ProfileEditModal(
            profile = profile,
            onSave = { n, a, b, f -> royaleViewModel.updatePlayerIdentity(n, a, b, f) },
            onDismiss = { royaleViewModel.closeModal() }
        )
    }

    selectedMatch?.let { rec ->
        MatchDetailDialog(record = rec, onDismiss = { royaleViewModel.closeMatchDetail() })
    }

    chestReward?.let { rew ->
        ChestRewardDialog(reward = rew, onDismiss = { royaleViewModel.closeChestRewardModal() })
    }
}

@Composable
private fun RoyaleTopResourceHeader(
    profile: RoyaleProfileState,
    onBackToSuite: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenShopResources: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Suite back + Avatar + Name + XP Level
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBackToSuite,
                        modifier = Modifier.size(34.dp).testTag("royale_back_to_suite_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar ao App", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { onOpenProfile() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(profile.bannerColorHex))
                                .border(1.5.dp, Color(profile.frameColorHex), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(profile.avatarIcon, fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = profile.playerName,
                                color = Color.White,
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Nv.${profile.level}", color = Color(0xFF38BDF8), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                LinearProgressIndicator(
                                    progress = { (profile.xp.toFloat() / profile.xpToNextLevel.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier.width(42.dp).height(5.dp).clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF38BDF8),
                                    trackColor = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }

                // Right: Gold, Gems (+ button), Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ResourcePill("🪙", profile.gold.toString(), Color(0xFFFACC15), onOpenShopResources)
                    ResourcePill("💎", profile.gems.toString(), Color(0xFF22D3EE), onOpenShopResources)
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp).testTag("header_settings_button")) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurações", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF1E293B))
        }
    }
}

@Composable
private fun ResourcePill(emoji: String, amount: String, accent: Color, onAddClick: () -> Unit) {
    Surface(
        onClick = onAddClick,
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(emoji, fontSize = 11.sp)
            Text(amount, color = Color.White, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Icon(Icons.Filled.Add, contentDescription = "Adicionar", tint = Color(0xFF4ADE80), modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun RoyaleFiveTabBottomBar(
    activeTab: RoyaleNavTab,
    cardsBadge: Int,
    battleBadge: Int,
    clanBadge: Int,
    onSelectTab: (RoyaleNavTab) -> Unit
) {
    Surface(
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavSlotItem(
                tab = RoyaleNavTab.SHOP,
                icon = Icons.Filled.Storefront,
                selected = activeTab == RoyaleNavTab.SHOP,
                badgeCount = 0,
                isCenter = false,
                onClick = { onSelectTab(RoyaleNavTab.SHOP) },
                modifier = Modifier.weight(1f).testTag("royale_nav_shop")
            )
            NavSlotItem(
                tab = RoyaleNavTab.CARDS,
                icon = Icons.Filled.Style,
                selected = activeTab == RoyaleNavTab.CARDS,
                badgeCount = cardsBadge,
                isCenter = false,
                onClick = { onSelectTab(RoyaleNavTab.CARDS) },
                modifier = Modifier.weight(1f).testTag("royale_nav_cards")
            )
            NavSlotItem(
                tab = RoyaleNavTab.BATTLE,
                icon = Icons.Filled.SportsEsports,
                selected = activeTab == RoyaleNavTab.BATTLE,
                badgeCount = battleBadge,
                isCenter = true,
                onClick = { onSelectTab(RoyaleNavTab.BATTLE) },
                modifier = Modifier.weight(1.22f).testTag("royale_nav_battle")
            )
            NavSlotItem(
                tab = RoyaleNavTab.CLAN,
                icon = Icons.Filled.Groups,
                selected = activeTab == RoyaleNavTab.CLAN,
                badgeCount = clanBadge,
                isCenter = false,
                onClick = { onSelectTab(RoyaleNavTab.CLAN) },
                modifier = Modifier.weight(1f).testTag("royale_nav_clan")
            )
            NavSlotItem(
                tab = RoyaleNavTab.PROFILE,
                icon = Icons.Filled.Person,
                selected = activeTab == RoyaleNavTab.PROFILE,
                badgeCount = 0,
                isCenter = false,
                onClick = { onSelectTab(RoyaleNavTab.PROFILE) },
                modifier = Modifier.weight(1f).testTag("royale_nav_profile")
            )
        }
    }
}

@Composable
private fun NavSlotItem(
    tab: RoyaleNavTab,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int,
    isCenter: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        selected && isCenter -> Color(0xFFFACC15)
        selected -> Color(0xFF2563EB)
        isCenter -> Color(0xFF1E293B)
        else -> Color.Transparent
    }
    val tint = when {
        selected && isCenter -> Color.Black
        selected -> Color.White
        isCenter -> Color(0xFFFACC15)
        else -> Color(0xFF94A3B8)
    }

    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(if (isCenter) 14.dp else 10.dp),
        border = if (isCenter) BorderStroke(1.5.dp, Color(0xFFFACC15)) else null,
        modifier = modifier.height(if (isCenter) 56.dp else 50.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(containerColor = Color(0xFFEF4444), contentColor = Color.White) {
                            Text(badgeCount.toString(), fontSize = 9.sp)
                        }
                    }
                }
            ) {
                Icon(icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(if (isCenter) 24.dp else 20.dp))
            }
            Text(
                text = tab.label,
                color = tint,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = if (selected || isCenter) FontWeight.Black else FontWeight.Bold,
                fontSize = if (isCenter) 10.sp else 9.sp
            )
        }
    }
}
