package com.example.royale

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Standardized spatial and temporal units (Prompt Sections 33.2 & 35):
 * - 1.0 map unit = standardized tile unit
 * - Arena dimensions: 18.0 x 32.0 map units
 * - Speed: map_units_per_second
 * - Time: seconds
 * - DPS = damagePerHit / hitSpeedSec
 */
object ArenaMetrics {
    const val WIDTH = 18.0f
    const val HEIGHT = 32.0f
    const val RIVER_Y = 16.0f
    const val RIVER_HALF_DEPTH = 0.9f
    const val LEFT_BRIDGE_X = 3.5f
    const val RIGHT_BRIDGE_X = 14.5f
    const val BRIDGE_HALF_WIDTH = 1.4f

    const val PLAYER_KING_X = 9.0f
    const val PLAYER_KING_Y = 29.0f
    const val PLAYER_LEFT_X = 3.5f
    const val PLAYER_RIGHT_X = 14.5f
    const val PLAYER_PRINCESS_Y = 25.5f

    const val ENEMY_KING_X = 9.0f
    const val ENEMY_KING_Y = 3.0f
    const val ENEMY_LEFT_X = 3.5f
    const val ENEMY_RIGHT_X = 14.5f
    const val ENEMY_PRINCESS_Y = 6.5f
}

enum class CardCategory(val labelPt: String) {
    TROOP("Tropa"),
    BUILDING("Construção"),
    SPELL("Feitiço")
}

enum class CardRarity(
    val labelPt: String,
    val startingLevel: Int,
    val maxLevel: Int,
    val colorHex: Long
) {
    COMMON("Comum", 1, 15, 0xFF94A3B8),
    RARE("Rara", 3, 15, 0xFFF97316),
    EPIC("Épica", 6, 15, 0xFFA855F7),
    LEGENDARY("Lendária", 9, 15, 0xFF22D3EE),
    CHAMPION("Campeã", 11, 15, 0xFFFACC15)
}

enum class TargetMask(val labelPt: String) {
    GROUND("Terrestre"),
    AIR_AND_GROUND("Terrestre e Aéreo"),
    BUILDINGS_ONLY("Construções"),
    NONE("Não se aplica")
}

enum class AbilityType {
    NONE,
    CHARGE,
    SPLASH_SPIN,
    FREEZE_SLOW_SPAWN,
    ZAP_SPAWN_SPLIT,
    DASH_INVULNERABLE,
    STEALTH_AMBUSH,
    HOOK_PULL,
    DEATH_RAGE,
    DEATH_SPLIT_BOMB,
    SUMMON_UNITS,
    INFERNO_RAMP,
    CHAIN_STUN,
    ELIXIR_PUMP,
    BOOMERANG_AXE,
    SHOTGUN_PELLET,
    CHAMPION_SUMMON,
    CHAMPION_CHAIN_DASH,
    CHAMPION_CLOAK_HASTE
}

data class AbilitySpec(
    val name: String,
    val description: String,
    val type: AbilityType,
    val activeElixirCost: Int = 0,
    val cooldownSec: Float = 0f,
    val durationSec: Float = 0f,
    val effectRadius: Float = 0f,
    val bonusValue: Float = 0f
)

data class EvolutionSpec(
    val name: String,
    val description: String,
    val cyclesRequired: Int = 2,
    val hpBonusMultiplier: Float = 1.15f,
    val damageBonusMultiplier: Float = 1.10f
)

data class CardSpec(
    val id: String,
    val name: String,
    val category: CardCategory,
    val subCategory: String,
    val rarity: CardRarity,
    val elixirCost: Int,
    val unlockArenaIndex: Int,
    val description: String,
    val spawnCount: Int = 1,
    // Level 11 standard benchmark attributes
    val hpAt11: Int = 0,
    val shieldHpAt11: Int = 0,
    val damageAt11: Int = 0,
    val spawnImpactDamageAt11: Int = 0,
    val deathDamageAt11: Int = 0,
    val chargeDamageAt11: Int = 0,
    val crownTowerDamageRatio: Float = 1.0f,
    val areaSplashRadius: Float = 0f,
    val hitSpeedSec: Float = 1.2f,
    val firstHitDelaySec: Float = 0.4f,
    val projectileSpeed: Float = 0f,
    val chainTargets: Int = 1,
    val infernoMaxMultiplier: Float = 1.0f,
    val movementSpeed: Float = 0f,
    val chargeSpeed: Float = 0f,
    val attackRange: Float = 0f,
    val sightRange: Float = 5.5f,
    val chaseBreakRange: Float = 8.5f,
    val deployTimeSec: Float = 1.0f,
    val lifetimeSec: Float = 0f,
    val isFlying: Boolean = false,
    val canJumpRiver: Boolean = false,
    val canDeployAnywhere: Boolean = false,
    val knockbackResistance: Float = 0.0f,
    val targetMask: TargetMask = TargetMask.GROUND,
    val spawnUnitId: String? = null,
    val spawnIntervalSec: Float = 0f,
    val spellDurationSec: Float = 0f,
    val spellKnockback: Float = 0f,
    val spellSlowPct: Float = 0f,
    val spellFreezeSec: Float = 0f,
    val spellStunSec: Float = 0f,
    val spellRageBoostPct: Float = 0f,
    val spellPullStrength: Float = 0f,
    val ability: AbilitySpec? = null,
    val evolution: EvolutionSpec? = null,
    val primaryColorHex: Long = 0xFF38BDF8,
    val secondaryColorHex: Long = 0xFF1E293B
) {
    fun scaleMultiplier(level: Int): Double = 1.10.pow((level - 11).toDouble())

    fun hpAtLevel(level: Int): Int {
        if (hpAt11 <= 0) return 0
        return (hpAt11 * scaleMultiplier(level)).roundToInt().coerceAtLeast(1)
    }

    fun shieldHpAtLevel(level: Int): Int {
        if (shieldHpAt11 <= 0) return 0
        return (shieldHpAt11 * scaleMultiplier(level)).roundToInt().coerceAtLeast(1)
    }

    fun damageAtLevel(level: Int): Int {
        if (damageAt11 <= 0) return 0
        return (damageAt11 * scaleMultiplier(level)).roundToInt().coerceAtLeast(1)
    }

    fun crownTowerDamageAtLevel(level: Int): Int {
        val raw = damageAtLevel(level)
        return (raw * crownTowerDamageRatio).roundToInt()
    }

    fun chargeDamageAtLevel(level: Int): Int {
        if (chargeDamageAt11 <= 0) return 0
        return (chargeDamageAt11 * scaleMultiplier(level)).roundToInt()
    }

    fun deathDamageAtLevel(level: Int): Int {
        if (deathDamageAt11 <= 0) return 0
        return (deathDamageAt11 * scaleMultiplier(level)).roundToInt()
    }

    fun spawnDamageAtLevel(level: Int): Int {
        if (spawnImpactDamageAt11 <= 0) return 0
        return (spawnImpactDamageAt11 * scaleMultiplier(level)).roundToInt()
    }

    fun dpsAtLevel(level: Int): Double {
        if (damageAt11 <= 0 || hitSpeedSec <= 0f) return 0.0
        return damageAtLevel(level).toDouble() / hitSpeedSec.toDouble()
    }

    fun totalGroupDpsAtLevel(level: Int): Double = dpsAtLevel(level) * spawnCount

    fun upgradeCardRequirement(currentLevel: Int): Int {
        if (currentLevel >= rarity.maxLevel) return 0
        val offset = (currentLevel - rarity.startingLevel).coerceAtLeast(0)
        return when (rarity) {
            CardRarity.COMMON -> listOf(2, 4, 10, 20, 50, 100, 200, 400, 800, 1000, 1500, 3000, 5000, 7500).getOrElse(offset) { 7500 }
            CardRarity.RARE -> listOf(2, 4, 10, 20, 50, 100, 200, 400, 500, 750, 1000, 1250).getOrElse(offset) { 1250 }
            CardRarity.EPIC -> listOf(2, 4, 10, 20, 40, 50, 100, 150, 200).getOrElse(offset) { 200 }
            CardRarity.LEGENDARY -> listOf(2, 4, 6, 10, 15, 20).getOrElse(offset) { 20 }
            CardRarity.CHAMPION -> listOf(2, 8, 20, 30).getOrElse(offset) { 30 }
        }
    }

    fun upgradeGoldCost(currentLevel: Int): Int {
        if (currentLevel >= rarity.maxLevel) return 0
        return when (currentLevel) {
            1 -> 5
            2 -> 20
            3 -> 50
            4 -> 150
            5 -> 400
            6 -> 1000
            7 -> 2000
            8 -> 4000
            9 -> 8000
            10 -> 15000
            11 -> 35000
            12 -> 50000
            13 -> 75000
            14 -> 100000
            else -> 100000
        }
    }
}

enum class TowerTroopId(
    val title: String,
    val description: String,
    val hpMult: Float,
    val damageMult: Float,
    val hitSpeedSec: Float,
    val range: Float,
    val projectileSpeed: Float
) {
    PRINCESS("Princesa da Torre", "Ataque equilibrado e preciso de longo alcance.", 1.0f, 1.0f, 0.8f, 7.5f, 14f),
    CANNONEER("Canhoneiro", "Dispara bolas de canhão lentas com altíssimo dano.", 0.90f, 2.35f, 2.2f, 7.5f, 11f),
    DAGGER_DUCHESS("Duquesa das Adagas", "Arremessa rajadas rápidas de adagas contra alvos iniciais.", 1.05f, 0.65f, 0.42f, 7.5f, 16f)
}

data class ArenaConfig(
    val index: Int,
    val name: String,
    val minTrophies: Int,
    val groundColorHex: Long,
    val altTileColorHex: Long,
    val riverColorHex: Long,
    val bridgeColorHex: Long,
    val accentColorHex: Long,
    val ambientDesc: String
)

enum class ChestType(
    val title: String,
    val unlockSeconds: Int,
    val gemSkipCost: Int,
    val minGold: Int,
    val maxGold: Int,
    val cardStacks: Int,
    val guaranteedMinRarity: CardRarity,
    val colorHex: Long
) {
    WOOD("Baú de Madeira", 15, 2, 35, 65, 2, CardRarity.COMMON, 0xFF92400E),
    SILVER("Baú de Prata", 180, 12, 90, 150, 3, CardRarity.COMMON, 0xFF94A3B8),
    GOLD("Baú de Ouro", 480, 28, 220, 380, 4, CardRarity.RARE, 0xFFFACC15),
    GIANT("Baú Gigante", 900, 45, 500, 850, 4, CardRarity.RARE, 0xFFEA580C),
    MAGICAL("Baú Mágico", 1200, 60, 650, 1100, 5, CardRarity.EPIC, 0xFFA855F7),
    EPIC("Baú Épico", 1200, 60, 400, 700, 4, CardRarity.EPIC, 0xFF7E22CE),
    LEGENDARY("Baú Lendário", 1800, 90, 1200, 2000, 4, CardRarity.LEGENDARY, 0xFF06B6D4)
}

data class ChestSlot(
    val slotIndex: Int,
    val type: ChestType,
    val unlockStartEpochMs: Long? = null
) {
    fun remainingSeconds(nowMs: Long): Int {
        val start = unlockStartEpochMs ?: return type.unlockSeconds
        val elapsed = ((nowMs - start) / 1000L).toInt().coerceAtLeast(0)
        return (type.unlockSeconds - elapsed).coerceAtLeast(0)
    }

    fun isReady(nowMs: Long): Boolean = unlockStartEpochMs != null && remainingSeconds(nowMs) <= 0
}

enum class BattleMode(
    val title: String,
    val subtitle: String,
    val elixirMultiplier: Float,
    val durationSec: Int,
    val isSuddenDeath: Boolean,
    val hasAiAlly: Boolean,
    val trophiesEnabled: Boolean
) {
    LADDER_1V1("Batalha 1v1", "Modo ranqueado clássico por troféus e baús.", 1.0f, 180, false, false, true),
    DUO_2V2("Batalha 2v2", "Combate em dupla com aliado IA contra dois adversários.", 1.0f, 180, false, true, false),
    TRAINING("Modo Treinamento", "Pratique decks e combos contra IA instrutora.", 1.0f, 180, false, false, false),
    FRIENDLY("Batalha Amistosa", "Duelo equilibrado com cartas padronizadas no nível 11.", 1.0f, 180, false, false, false),
    DOUBLE_ELIXIR("Elixir Dobrado (2x)", "Regeneração de elixir em dobro desde o início.", 2.0f, 180, false, false, false),
    TRIPLE_ELIXIR("Elixir Triplo (3x)", "Ritmo frenético com geração 3x de elixir.", 3.0f, 150, false, false, false),
    SUDDEN_DEATH("Morte Súbita", "A primeira torre destruída vence instantaneamente.", 2.0f, 120, true, false, false),
    PERMANENT_TOURNAMENT("Torneio Permanente", "Regras de torneio nível 11 com premiação de ouro.", 1.0f, 180, false, false, false)
}

enum class AiDifficulty(val labelPt: String, val reactionSec: Float, val elixirEfficiency: Float) {
    EASY("Fácil", 2.8f, 0.72f),
    NORMAL("Normal", 1.8f, 0.90f),
    HARD("Difícil", 1.1f, 1.0f),
    VERY_HARD("Muito Difícil", 0.65f, 1.08f)
}

enum class MagicItemType(val title: String, val description: String, val colorHex: Long) {
    WILD_COMMON("Curinga Comum", "Adiciona cópias a qualquer carta Comum.", 0xFF94A3B8),
    WILD_RARE("Curinga Raro", "Adiciona cópias a qualquer carta Rara.", 0xFFF97316),
    WILD_EPIC("Curinga Épico", "Adiciona cópias a qualquer carta Épica.", 0xFFA855F7),
    WILD_LEGENDARY("Curinga Lendário", "Adiciona cópias a qualquer carta Lendária.", 0xFF22D3EE),
    WILD_CHAMPION("Curinga de Campeão", "Adiciona cópias a qualquer carta Campeã.", 0xFFFACC15),
    BOOK_OF_CARDS("Livro de Cartas", "Completa todas as cópias necessárias para o próximo nível.", 0xFF3B82F6),
    BOOK_OF_BOOKS("Livro dos Livros", "Completa as cópias de qualquer carta, inclusive Campeãs.", 0xFFF59E0B),
    MAGIC_COIN("Moeda Mágica", "Melhora uma carta sem gastar moedas de ouro.", 0xFFEAB308),
    EVOLUTION_SHARD("Fragmento de Evolução", "Desbloqueia a forma evoluída de uma carta (6 fragmentos).", 0xFFD946EF)
}

data class CardOwnership(
    val cardId: String,
    val level: Int,
    val copies: Int,
    val evolutionShards: Int = 0,
    val evolutionUnlocked: Boolean = false
)

data class DeckPreset(
    val id: Int,
    val name: String,
    val cardIds: List<String>,
    val evoSlotIds: List<String> = emptyList()
)

data class TrophyMilestone(
    val trophies: Int,
    val title: String,
    val rewardGold: Int = 0,
    val rewardGems: Int = 0,
    val rewardCardId: String? = null,
    val rewardCardCopies: Int = 0,
    val rewardChest: ChestType? = null
)

data class PassTier(
    val tier: Int,
    val crownsRequired: Int,
    val freeRewardLabel: String,
    val freeGold: Int = 0,
    val freeGems: Int = 0,
    val freeChest: ChestType? = null,
    val premiumRewardLabel: String,
    val premiumGold: Int = 0,
    val premiumGems: Int = 0,
    val premiumWildEpic: Int = 0,
    val premiumEvoShard: Int = 0
)

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val rewardGold: Int,
    val rewardGems: Int
)

data class EmoteDef(
    val id: String,
    val name: String,
    val category: String,
    val rarity: CardRarity,
    val iconTag: String,
    val phrase: String
)

data class ClanMember(
    val id: String,
    val name: String,
    val role: String,
    val trophies: Int,
    val level: Int,
    val donations: Int
)

data class ClanChatMsg(
    val id: String,
    val sender: String,
    val text: String,
    val timestampMs: Long,
    val isDonationRequest: Boolean = false,
    val requestedCardId: String? = null,
    val donatedCount: Int = 0,
    val maxDonation: Int = 4
)

data class FriendEntry(
    val id: String,
    val name: String,
    val trophies: Int,
    val level: Int,
    val online: Boolean,
    val statusText: String,
    val pendingIncoming: Boolean = false
)

data class MatchCardStat(
    val cardId: String,
    val timesPlayed: Int,
    val damageDealt: Int,
    val towerDamage: Int,
    val kills: Int,
    val elixirSpent: Int
)

data class MatchRecord(
    val id: String,
    val timestampMs: Long,
    val modeTitle: String,
    val opponentName: String,
    val playerCrowns: Int,
    val enemyCrowns: Int,
    val won: Boolean,
    val trophiesDelta: Int,
    val durationSec: Int,
    val deckCardIds: List<String>,
    val cardStats: List<MatchCardStat>
)

data class RoyaleSettings(
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val volume: Float = 0.8f,
    val vibrationEnabled: Boolean = true,
    val highGraphics: Boolean = true,
    val batterySaver: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val language: String = "Português (BR)"
)
