package com.example.royale.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.royale.AchievementDef
import com.example.royale.AiDifficulty
import com.example.royale.BattleMode
import com.example.royale.CardOwnership
import com.example.royale.CardRarity
import com.example.royale.ChestSlot
import com.example.royale.ChestType
import com.example.royale.ClanChatMsg
import com.example.royale.ClanMember
import com.example.royale.DeckPreset
import com.example.royale.FriendEntry
import com.example.royale.MagicItemType
import com.example.royale.MatchCardStat
import com.example.royale.MatchRecord
import com.example.royale.RoyaleCardCatalog
import com.example.royale.RoyaleSettings
import com.example.royale.TowerTroopId
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "royale_profile")
data class RoyaleSaveEntity(
    @PrimaryKey val id: String = "local_player",
    val jsonPayload: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Dao
interface RoyaleSaveDao {
    @Query("SELECT * FROM royale_profile WHERE id = 'local_player' LIMIT 1")
    fun observeProfile(): Flow<RoyaleSaveEntity?>

    @Query("SELECT * FROM royale_profile WHERE id = 'local_player' LIMIT 1")
    suspend fun getProfileOnce(): RoyaleSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: RoyaleSaveEntity)
}

data class RoyaleProfileState(
    val playerName: String = "Comandante",
    val avatarIcon: String = "👑",
    val bannerColorHex: Long = 0xFF1D4ED8,
    val frameColorHex: Long = 0xFFFACC15,
    val towerSkinName: String = "Torre Real Clássica",
    val kingSkinName: String = "Rei Dourado",
    val selectedTowerTroop: TowerTroopId = TowerTroopId.PRINCESS,
    val unlockedTowerTroops: Set<TowerTroopId> = setOf(TowerTroopId.PRINCESS),
    val level: Int = 1,
    val xp: Int = 0,
    val trophies: Int = 0,
    val bestTrophies: Int = 0,
    val gold: Int = 1500,
    val gems: Int = 120,
    val wins: Int = 0,
    val losses: Int = 0,
    val winStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val totalTowersDestroyed: Int = 0,
    val duoMatchesPlayed: Int = 0,
    val upgradesPerformed: Int = 0,
    val passCrowns: Int = 0,
    val passPremiumUnlocked: Boolean = false,
    val claimedFreePassTiers: Set<Int> = emptySet(),
    val claimedPremiumPassTiers: Set<Int> = emptySet(),
    val claimedTrophyMilestones: Set<Int> = emptySet(),
    val claimedAchievements: Set<String> = emptySet(),
    val cardCollection: Map<String, CardOwnership> = defaultCollection(),
    val decks: List<DeckPreset> = defaultDecks(),
    val activeDeckIndex: Int = 0,
    val selectedBattleMode: BattleMode = BattleMode.LADDER_1V1,
    val selectedAiDifficulty: AiDifficulty = AiDifficulty.NORMAL,
    val chestSlots: List<ChestSlot> = listOf(
        ChestSlot(0, ChestType.WOOD, unlockStartEpochMs = System.currentTimeMillis() - 20_000L),
        ChestSlot(1, ChestType.SILVER, unlockStartEpochMs = null)
    ),
    val magicItems: Map<MagicItemType, Int> = mapOf(
        MagicItemType.WILD_COMMON to 25,
        MagicItemType.WILD_RARE to 10,
        MagicItemType.WILD_EPIC to 4,
        MagicItemType.WILD_LEGENDARY to 1,
        MagicItemType.WILD_CHAMPION to 0,
        MagicItemType.BOOK_OF_CARDS to 1,
        MagicItemType.BOOK_OF_BOOKS to 0,
        MagicItemType.MAGIC_COIN to 1,
        MagicItemType.EVOLUTION_SHARD to 3
    ),
    val unlockedEmotes: Set<String> = setOf("king_thumbs", "king_laugh", "knight_cheer", "goblin_wink"),
    val equippedEmotes: List<String> = listOf("king_thumbs", "king_laugh", "knight_cheer", "goblin_wink"),
    val unlockedSkins: Set<String> = setOf("Torre Real Clássica", "Rei Dourado"),
    val clanName: String = "Guarda de Ferro",
    val clanTag: String = "#GDF99",
    val clanEmblem: String = "🛡️",
    val clanDescription: String = "Clã ativo focado em evolução diária, doações e batalhas táticas.",
    val clanMinTrophies: Int = 0,
    val inClan: Boolean = true,
    val clanUnreadCount: Int = 1,
    val clanMembers: List<ClanMember> = defaultClanMembers(),
    val clanChat: List<ClanChatMsg> = defaultClanChat(),
    val friends: List<FriendEntry> = defaultFriends(),
    val matchHistory: List<MatchRecord> = emptyList(),
    val settings: RoyaleSettings = RoyaleSettings(),
    val tutorialCompleted: Boolean = false,
    val shopBoughtTodayIds: Set<String> = emptySet()
) {
    val xpToNextLevel: Int get() = 100 + (level - 1) * 80
    val activeDeck: DeckPreset get() = decks.getOrElse(activeDeckIndex) { decks.first() }
    val activeDeckAverageElixir: Float
        get() {
            val cards = activeDeck.cardIds.mapNotNull { RoyaleCardCatalog.get(it) }
            return if (cards.isEmpty()) 0f else cards.sumOf { it.elixirCost }.toFloat() / cards.size
        }

    fun upgradableCount(): Int {
        return cardCollection.values.count { own ->
            val spec = RoyaleCardCatalog.get(own.cardId) ?: return@count false
            own.level < spec.rarity.maxLevel &&
                own.copies >= spec.upgradeCardRequirement(own.level) &&
                gold >= spec.upgradeGoldCost(own.level)
        }
    }

    fun unclaimedRewardsCount(): Int {
        val trophyReady = RoyaleCardCatalog.trophyMilestones.count {
            trophies >= it.trophies && it.trophies !in claimedTrophyMilestones
        }
        val passReady = RoyaleCardCatalog.passTiers.count {
            passCrowns >= it.crownsRequired && (it.tier !in claimedFreePassTiers || (passPremiumUnlocked && it.tier !in claimedPremiumPassTiers))
        }
        val achReady = RoyaleCardCatalog.achievements.count {
            achievementProgress(it) >= it.target && it.id !in claimedAchievements
        }
        return trophyReady + passReady + achReady
    }

    fun achievementProgress(def: AchievementDef): Int = when (def.id) {
        "first_win", "ten_wins", "hundred_wins" -> wins
        "destroy_towers" -> totalTowersDestroyed
        "unlock_35_cards" -> cardCollection.size
        "upgrade_card" -> upgradesPerformed
        "new_arena" -> bestTrophies
        "play_duo" -> duoMatchesPlayed
        else -> 0
    }

    companion object {
        fun defaultCollection(): Map<String, CardOwnership> {
            return RoyaleCardCatalog.initialStarterCardIds.associateWith { id ->
                val spec = RoyaleCardCatalog.get(id)!!
                CardOwnership(
                    cardId = id,
                    level = spec.rarity.startingLevel,
                    copies = if (spec.rarity == CardRarity.COMMON) 4 else 2,
                    evolutionShards = 0,
                    evolutionUnlocked = false
                )
            }
        }

        fun defaultDecks(): List<DeckPreset> {
            val base = RoyaleCardCatalog.defaultDeckIds
            return listOf(
                DeckPreset(0, "Deck Principal", base),
                DeckPreset(1, "Cerco Rápido", listOf("corredor", "valquiria", "mosqueteira", "canhao", "bola_de_fogo", "flechas", "esqueletos", "goblins_lanceiros")),
                DeckPreset(2, "Tanque Pesado", listOf("gigante", "bruxa", "principe", "servos", "mini_pekka", "flechas", "bola_de_fogo", "goblins")),
                DeckPreset(3, "Controle Aéreo", listOf("cavaleiro", "arqueiras", "servos", "mosqueteira", "canhao", "bola_de_fogo", "flechas", "goblins_lanceiros")),
                DeckPreset(4, "Deck Livre", base)
            )
        }

        fun defaultClanMembers(): List<ClanMember> = listOf(
            ClanMember("m1", "Comandante", "Líder", 420, 4, 32),
            ClanMember("m2", "Kaelen_BR", "Colíder", 1450, 7, 118),
            ClanMember("m3", "ValquiriaSul", "Ancião", 1120, 6, 84),
            ClanMember("m4", "MagoGelo99", "Membro", 890, 5, 45),
            ClanMember("m5", "PekkaLover", "Membro", 610, 4, 19)
        )

        fun defaultClanChat(): List<ClanChatMsg> {
            val now = System.currentTimeMillis()
            return listOf(
                ClanChatMsg("c1", "Kaelen_BR", "Bem-vindo à Guarda de Ferro! Vamos subir troféus hoje.", now - 3600_000L),
                ClanChatMsg("c2", "ValquiriaSul", "Alguém pode doar Cavaleiros?", now - 1800_000L, isDonationRequest = true, requestedCardId = "cavaleiro", donatedCount = 1, maxDonation = 4)
            )
        }

        fun defaultFriends(): List<FriendEntry> = listOf(
            FriendEntry("f1", "Kaelen_BR", 1450, 7, true, "Disponível para Amistoso"),
            FriendEntry("f2", "ValquiriaSul", 1120, 6, true, "Na Arena Vale dos Feitiços"),
            FriendEntry("f3", "Arthur_Deck", 760, 5, false, "Visto há 2h"),
            FriendEntry("f4", "Lenda_RJ", 2150, 9, true, "Quer ser seu amigo", pendingIncoming = true)
        )
    }
}

object RoyaleSaveCodec {
    fun encode(state: RoyaleProfileState): String {
        val root = linkedMapOf<String, Any?>(
            "playerName" to state.playerName,
            "avatarIcon" to state.avatarIcon,
            "bannerColorHex" to state.bannerColorHex,
            "frameColorHex" to state.frameColorHex,
            "towerSkinName" to state.towerSkinName,
            "kingSkinName" to state.kingSkinName,
            "selectedTowerTroop" to state.selectedTowerTroop.name,
            "unlockedTowerTroops" to state.unlockedTowerTroops.map { it.name },
            "level" to state.level,
            "xp" to state.xp,
            "trophies" to state.trophies,
            "bestTrophies" to state.bestTrophies,
            "gold" to state.gold,
            "gems" to state.gems,
            "wins" to state.wins,
            "losses" to state.losses,
            "winStreak" to state.winStreak,
            "bestWinStreak" to state.bestWinStreak,
            "totalTowersDestroyed" to state.totalTowersDestroyed,
            "duoMatchesPlayed" to state.duoMatchesPlayed,
            "upgradesPerformed" to state.upgradesPerformed,
            "passCrowns" to state.passCrowns,
            "passPremiumUnlocked" to state.passPremiumUnlocked,
            "claimedFreePassTiers" to state.claimedFreePassTiers.toList(),
            "claimedPremiumPassTiers" to state.claimedPremiumPassTiers.toList(),
            "claimedTrophyMilestones" to state.claimedTrophyMilestones.toList(),
            "claimedAchievements" to state.claimedAchievements.toList(),
            "activeDeckIndex" to state.activeDeckIndex,
            "selectedBattleMode" to state.selectedBattleMode.name,
            "selectedAiDifficulty" to state.selectedAiDifficulty.name,
            "tutorialCompleted" to state.tutorialCompleted,
            "shopBoughtTodayIds" to state.shopBoughtTodayIds.toList(),
            "cardCollection" to state.cardCollection.values.map { c ->
                mapOf(
                    "cardId" to c.cardId,
                    "level" to c.level,
                    "copies" to c.copies,
                    "evolutionShards" to c.evolutionShards,
                    "evolutionUnlocked" to c.evolutionUnlocked
                )
            },
            "decks" to state.decks.map { d ->
                mapOf(
                    "id" to d.id,
                    "name" to d.name,
                    "cardIds" to d.cardIds,
                    "evoSlotIds" to d.evoSlotIds
                )
            },
            "chestSlots" to state.chestSlots.map { ch ->
                mapOf(
                    "slotIndex" to ch.slotIndex,
                    "type" to ch.type.name,
                    "unlockStartEpochMs" to ch.unlockStartEpochMs
                )
            },
            "magicItems" to state.magicItems.entries.associate { it.key.name to it.value },
            "unlockedEmotes" to state.unlockedEmotes.toList(),
            "equippedEmotes" to state.equippedEmotes,
            "unlockedSkins" to state.unlockedSkins.toList(),
            "clanName" to state.clanName,
            "clanTag" to state.clanTag,
            "clanEmblem" to state.clanEmblem,
            "clanDescription" to state.clanDescription,
            "clanMinTrophies" to state.clanMinTrophies,
            "inClan" to state.inClan,
            "clanUnreadCount" to state.clanUnreadCount,
            "clanMembers" to state.clanMembers.map { m ->
                mapOf(
                    "id" to m.id,
                    "name" to m.name,
                    "role" to m.role,
                    "trophies" to m.trophies,
                    "level" to m.level,
                    "donations" to m.donations
                )
            },
            "clanChat" to state.clanChat.takeLast(40).map { m ->
                mapOf(
                    "id" to m.id,
                    "sender" to m.sender,
                    "text" to m.text,
                    "timestampMs" to m.timestampMs,
                    "isDonationRequest" to m.isDonationRequest,
                    "requestedCardId" to m.requestedCardId,
                    "donatedCount" to m.donatedCount,
                    "maxDonation" to m.maxDonation
                )
            },
            "friends" to state.friends.map { f ->
                mapOf(
                    "id" to f.id,
                    "name" to f.name,
                    "trophies" to f.trophies,
                    "level" to f.level,
                    "online" to f.online,
                    "statusText" to f.statusText,
                    "pendingIncoming" to f.pendingIncoming
                )
            },
            "matchHistory" to state.matchHistory.takeLast(30).map { r ->
                mapOf(
                    "id" to r.id,
                    "timestampMs" to r.timestampMs,
                    "modeTitle" to r.modeTitle,
                    "opponentName" to r.opponentName,
                    "playerCrowns" to r.playerCrowns,
                    "enemyCrowns" to r.enemyCrowns,
                    "won" to r.won,
                    "trophiesDelta" to r.trophiesDelta,
                    "durationSec" to r.durationSec,
                    "deckCardIds" to r.deckCardIds,
                    "cardStats" to r.cardStats.map { cs ->
                        mapOf(
                            "cardId" to cs.cardId,
                            "timesPlayed" to cs.timesPlayed,
                            "damageDealt" to cs.damageDealt,
                            "towerDamage" to cs.towerDamage,
                            "kills" to cs.kills,
                            "elixirSpent" to cs.elixirSpent
                        )
                    }
                )
            },
            "settings" to mapOf(
                "musicEnabled" to state.settings.musicEnabled,
                "sfxEnabled" to state.settings.sfxEnabled,
                "volume" to state.settings.volume.toDouble(),
                "vibrationEnabled" to state.settings.vibrationEnabled,
                "highGraphics" to state.settings.highGraphics,
                "batterySaver" to state.settings.batterySaver,
                "notificationsEnabled" to state.settings.notificationsEnabled,
                "language" to state.settings.language
            )
        )
        return RoyaleJson.encodeValue(root)
    }

    @Suppress("UNCHECKED_CAST")
    fun decode(json: String): RoyaleProfileState {
        return try {
            val root = RoyaleJson.parseObject(json)
            val def = RoyaleProfileState()

            val collection = mutableMapOf<String, CardOwnership>()
            (root["cardCollection"] as? List<Any?>)?.forEach { item ->
                val o = item as? Map<String, Any?> ?: return@forEach
                val cid = o.str("cardId") ?: return@forEach
                collection[cid] = CardOwnership(
                    cardId = cid,
                    level = o.int("level", 1),
                    copies = o.int("copies", 0),
                    evolutionShards = o.int("evolutionShards", 0),
                    evolutionUnlocked = o.bool("evolutionUnlocked", false)
                )
            }

            val decks = (root["decks"] as? List<Any?>)?.mapNotNull { item ->
                val o = item as? Map<String, Any?> ?: return@mapNotNull null
                DeckPreset(
                    id = o.int("id", 0),
                    name = o.str("name") ?: "Deck",
                    cardIds = o.strList("cardIds"),
                    evoSlotIds = o.strList("evoSlotIds")
                )
            } ?: emptyList()

            val chests = (root["chestSlots"] as? List<Any?>)?.mapNotNull { item ->
                val o = item as? Map<String, Any?> ?: return@mapNotNull null
                val type = ChestType.entries.firstOrNull { it.name == o.str("type") } ?: ChestType.SILVER
                ChestSlot(
                    slotIndex = o.int("slotIndex", 0),
                    type = type,
                    unlockStartEpochMs = (o["unlockStartEpochMs"] as? Number)?.toLong()
                )
            } ?: emptyList()

            val magicMap = def.magicItems.toMutableMap()
            (root["magicItems"] as? Map<String, Any?>)?.let { obj ->
                MagicItemType.entries.forEach { t ->
                    (obj[t.name] as? Number)?.toInt()?.let { magicMap[t] = it }
                }
            }

            val membersList = (root["clanMembers"] as? List<Any?>)?.mapNotNull { item ->
                val o = item as? Map<String, Any?> ?: return@mapNotNull null
                ClanMember(
                    id = o.str("id") ?: "",
                    name = o.str("name") ?: "",
                    role = o.str("role") ?: "Membro",
                    trophies = o.int("trophies", 0),
                    level = o.int("level", 1),
                    donations = o.int("donations", 0)
                )
            } ?: emptyList()

            val chatList = (root["clanChat"] as? List<Any?>)?.mapNotNull { item ->
                val o = item as? Map<String, Any?> ?: return@mapNotNull null
                ClanChatMsg(
                    id = o.str("id") ?: "",
                    sender = o.str("sender") ?: "",
                    text = o.str("text") ?: "",
                    timestampMs = o.long("timestampMs", 0L),
                    isDonationRequest = o.bool("isDonationRequest", false),
                    requestedCardId = o.str("requestedCardId"),
                    donatedCount = o.int("donatedCount", 0),
                    maxDonation = o.int("maxDonation", 4)
                )
            } ?: emptyList()

            val friendsList = (root["friends"] as? List<Any?>)?.mapNotNull { item ->
                val o = item as? Map<String, Any?> ?: return@mapNotNull null
                FriendEntry(
                    id = o.str("id") ?: "",
                    name = o.str("name") ?: "",
                    trophies = o.int("trophies", 0),
                    level = o.int("level", 1),
                    online = o.bool("online", false),
                    statusText = o.str("statusText") ?: "",
                    pendingIncoming = o.bool("pendingIncoming", false)
                )
            } ?: emptyList()

            val historyList = (root["matchHistory"] as? List<Any?>)?.mapNotNull { item ->
                val o = item as? Map<String, Any?> ?: return@mapNotNull null
                val cardStats = (o["cardStats"] as? List<Any?>)?.mapNotNull { cItem ->
                    val cs = cItem as? Map<String, Any?> ?: return@mapNotNull null
                    MatchCardStat(
                        cardId = cs.str("cardId") ?: "",
                        timesPlayed = cs.int("timesPlayed", 0),
                        damageDealt = cs.int("damageDealt", 0),
                        towerDamage = cs.int("towerDamage", 0),
                        kills = cs.int("kills", 0),
                        elixirSpent = cs.int("elixirSpent", 0)
                    )
                } ?: emptyList()
                MatchRecord(
                    id = o.str("id") ?: "",
                    timestampMs = o.long("timestampMs", 0L),
                    modeTitle = o.str("modeTitle") ?: "",
                    opponentName = o.str("opponentName") ?: "",
                    playerCrowns = o.int("playerCrowns", 0),
                    enemyCrowns = o.int("enemyCrowns", 0),
                    won = o.bool("won", false),
                    trophiesDelta = o.int("trophiesDelta", 0),
                    durationSec = o.int("durationSec", 0),
                    deckCardIds = o.strList("deckCardIds"),
                    cardStats = cardStats
                )
            } ?: emptyList()

            val sObj = root["settings"] as? Map<String, Any?>
            val settings = if (sObj != null) {
                RoyaleSettings(
                    musicEnabled = sObj.bool("musicEnabled", true),
                    sfxEnabled = sObj.bool("sfxEnabled", true),
                    volume = ((sObj["volume"] as? Number)?.toFloat() ?: 0.8f),
                    vibrationEnabled = sObj.bool("vibrationEnabled", true),
                    highGraphics = sObj.bool("highGraphics", true),
                    batterySaver = sObj.bool("batterySaver", false),
                    notificationsEnabled = sObj.bool("notificationsEnabled", true),
                    language = sObj.str("language") ?: "Português (BR)"
                )
            } else def.settings

            def.copy(
                playerName = root.str("playerName") ?: def.playerName,
                avatarIcon = root.str("avatarIcon") ?: def.avatarIcon,
                bannerColorHex = root.long("bannerColorHex", def.bannerColorHex),
                frameColorHex = root.long("frameColorHex", def.frameColorHex),
                towerSkinName = root.str("towerSkinName") ?: def.towerSkinName,
                kingSkinName = root.str("kingSkinName") ?: def.kingSkinName,
                selectedTowerTroop = TowerTroopId.entries.firstOrNull { it.name == root.str("selectedTowerTroop") } ?: def.selectedTowerTroop,
                unlockedTowerTroops = root.strList("unlockedTowerTroops")
                    .mapNotNull { n -> TowerTroopId.entries.firstOrNull { it.name == n } }
                    .toSet().ifEmpty { def.unlockedTowerTroops },
                level = root.int("level", def.level),
                xp = root.int("xp", def.xp),
                trophies = root.int("trophies", def.trophies),
                bestTrophies = root.int("bestTrophies", def.bestTrophies),
                gold = root.int("gold", def.gold),
                gems = root.int("gems", def.gems),
                wins = root.int("wins", def.wins),
                losses = root.int("losses", def.losses),
                winStreak = root.int("winStreak", def.winStreak),
                bestWinStreak = root.int("bestWinStreak", def.bestWinStreak),
                totalTowersDestroyed = root.int("totalTowersDestroyed", def.totalTowersDestroyed),
                duoMatchesPlayed = root.int("duoMatchesPlayed", def.duoMatchesPlayed),
                upgradesPerformed = root.int("upgradesPerformed", def.upgradesPerformed),
                passCrowns = root.int("passCrowns", def.passCrowns),
                passPremiumUnlocked = root.bool("passPremiumUnlocked", def.passPremiumUnlocked),
                claimedFreePassTiers = root.intSet("claimedFreePassTiers"),
                claimedPremiumPassTiers = root.intSet("claimedPremiumPassTiers"),
                claimedTrophyMilestones = root.intSet("claimedTrophyMilestones"),
                claimedAchievements = root.strList("claimedAchievements").toSet(),
                cardCollection = if (collection.isNotEmpty()) collection else def.cardCollection,
                decks = if (decks.isNotEmpty()) decks else def.decks,
                activeDeckIndex = root.int("activeDeckIndex", 0),
                selectedBattleMode = BattleMode.entries.firstOrNull { it.name == root.str("selectedBattleMode") } ?: def.selectedBattleMode,
                selectedAiDifficulty = AiDifficulty.entries.firstOrNull { it.name == root.str("selectedAiDifficulty") } ?: def.selectedAiDifficulty,
                chestSlots = chests,
                magicItems = magicMap,
                unlockedEmotes = root.strList("unlockedEmotes").toSet().ifEmpty { def.unlockedEmotes },
                equippedEmotes = root.strList("equippedEmotes").ifEmpty { def.equippedEmotes },
                unlockedSkins = root.strList("unlockedSkins").toSet().ifEmpty { def.unlockedSkins },
                clanName = root.str("clanName") ?: def.clanName,
                clanTag = root.str("clanTag") ?: def.clanTag,
                clanEmblem = root.str("clanEmblem") ?: def.clanEmblem,
                clanDescription = root.str("clanDescription") ?: def.clanDescription,
                clanMinTrophies = root.int("clanMinTrophies", def.clanMinTrophies),
                inClan = root.bool("inClan", def.inClan),
                clanUnreadCount = root.int("clanUnreadCount", 0),
                clanMembers = if (membersList.isNotEmpty()) membersList else def.clanMembers,
                clanChat = if (chatList.isNotEmpty()) chatList else def.clanChat,
                friends = if (friendsList.isNotEmpty()) friendsList else def.friends,
                matchHistory = historyList,
                settings = settings,
                tutorialCompleted = root.bool("tutorialCompleted", false),
                shopBoughtTodayIds = root.strList("shopBoughtTodayIds").toSet()
            )
        } catch (_: Throwable) {
            RoyaleProfileState()
        }
    }

    private fun Map<String, Any?>.str(key: String): String? = this[key] as? String
    private fun Map<String, Any?>.int(key: String, fallback: Int): Int = (this[key] as? Number)?.toInt() ?: fallback
    private fun Map<String, Any?>.long(key: String, fallback: Long): Long = (this[key] as? Number)?.toLong() ?: fallback
    private fun Map<String, Any?>.bool(key: String, fallback: Boolean): Boolean = (this[key] as? Boolean) ?: fallback
    private fun Map<String, Any?>.strList(key: String): List<String> =
        (this[key] as? List<Any?>)?.mapNotNull { it as? String } ?: emptyList()
    private fun Map<String, Any?>.intSet(key: String): Set<Int> =
        (this[key] as? List<Any?>)?.mapNotNull { (it as? Number)?.toInt() }?.toSet() ?: emptySet()
}
