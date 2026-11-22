package com.example.royale

import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class TeamSide {
    PLAYER,
    ENEMY
}

enum class TowerKind {
    KING,
    PRINCESS_LEFT,
    PRINCESS_RIGHT
}

data class BattleTower(
    val id: String,
    val team: TeamSide,
    val kind: TowerKind,
    val x: Float,
    val y: Float,
    val maxHp: Int,
    var hp: Int,
    val damage: Int,
    val hitSpeedSec: Float,
    val range: Float,
    val projectileSpeed: Float,
    var isActive: Boolean,
    var attackCooldown: Float = 0f,
    var freezeTimer: Float = 0f,
    var rageTimer: Float = 0f,
    var stunTimer: Float = 0f,
    var hitFlashTimer: Float = 0f,
    val radius: Float = if (kind == TowerKind.KING) 1.55f else 1.25f
) {
    val isAlive: Boolean get() = hp > 0
}

data class BattleEntity(
    val id: String,
    val cardId: String,
    val name: String,
    val team: TeamSide,
    val level: Int,
    val category: CardCategory,
    var x: Float,
    var y: Float,
    val radius: Float,
    val maxHp: Int,
    var hp: Int,
    val maxShieldHp: Int,
    var shieldHp: Int,
    val baseDamage: Int,
    val chargeDamage: Int,
    val deathDamage: Int,
    val areaSplashRadius: Float,
    val hitSpeedSec: Float,
    val projectileSpeed: Float,
    val chainTargets: Int,
    val infernoMaxMult: Float,
    val movementSpeed: Float,
    val chargeSpeed: Float,
    val attackRange: Float,
    val sightRange: Float,
    val chaseBreakRange: Float,
    val isFlying: Boolean,
    val canJumpRiver: Boolean,
    val targetMask: TargetMask,
    val knockbackResistance: Float,
    val abilityType: AbilityType,
    val spawnUnitId: String?,
    val spawnIntervalSec: Float,
    val isEvolved: Boolean = false,
    val isClone: Boolean = false,
    val primaryColorHex: Long = 0xFF38BDF8,
    // Runtime state
    var deployTimer: Float = 0.8f,
    var lifetimeRemaining: Float = 0f,
    var attackCooldown: Float = 0f,
    var spawnTimer: Float = 2.0f,
    var distanceMovedSinceAttack: Float = 0f,
    var isCharging: Boolean = false,
    var isInvisible: Boolean = false,
    var stealthResetTimer: Float = 0f,
    var stunTimer: Float = 0f,
    var freezeTimer: Float = 0f,
    var slowTimer: Float = 0f,
    var slowPct: Float = 0f,
    var rageTimer: Float = 0f,
    var ragePct: Float = 0f,
    var dashTimer: Float = 0f,
    var dashTargetX: Float = x,
    var dashTargetY: Float = y,
    var lockedTargetId: String? = null,
    var infernoLockTime: Float = 0f,
    var championCooldown: Float = 0f,
    var championActiveTimer: Float = 0f,
    var facingAngleRad: Float = if (team == TeamSide.PLAYER) -PI.toFloat() / 2f else PI.toFloat() / 2f,
    var attackAnimTimer: Float = 0f,
    var hitFlashTimer: Float = 0f,
    var isHiddenUnderground: Boolean = false
) {
    val isAlive: Boolean get() = hp > 0
}

data class BattleProjectile(
    val id: String,
    val team: TeamSide,
    val sourceCardId: String,
    var x: Float,
    var y: Float,
    val targetId: String,
    var targetLastX: Float,
    var targetLastY: Float,
    val speed: Float,
    val damage: Int,
    val splashRadius: Float,
    val stunSec: Float = 0f,
    val slowSec: Float = 0f,
    val slowPct: Float = 0f,
    val colorHex: Long = 0xFFFACC15
)

data class BattleSpellEffect(
    val id: String,
    val cardId: String,
    val team: TeamSide,
    val level: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    var durationRemaining: Float,
    val totalDuration: Float,
    val tickInterval: Float = 0.5f,
    var tickAccumulator: Float = 0f,
    val damagePerTick: Int = 0,
    val towerDamagePerTick: Int = 0,
    val slowPct: Float = 0f,
    val ragePct: Float = 0f,
    val pullStrength: Float = 0f,
    val spawnUnitId: String? = null,
    val colorHex: Long = 0xFF38BDF8,
    val isBurstVisual: Boolean = false
)

data class FloatingBattleText(
    val id: String,
    val x: Float,
    val y: Float,
    val text: String,
    val colorHex: Long,
    var lifeSec: Float = 0.9f
)

data class BattleEmotePopup(
    val id: String,
    val team: TeamSide,
    val icon: String,
    val phrase: String,
    var lifeSec: Float = 2.4f
)

data class BattleResultSummary(
    val playerCrowns: Int,
    val enemyCrowns: Int,
    val won: Boolean,
    val draw: Boolean,
    val trophiesDelta: Int,
    val goldEarned: Int,
    val xpEarned: Int,
    val chestAwarded: ChestType?,
    val durationSec: Int,
    val opponentName: String,
    val cardStats: List<MatchCardStat>
)

data class MutableCardStat(
    val cardId: String,
    var timesPlayed: Int = 0,
    var damageDealt: Int = 0,
    var towerDamage: Int = 0,
    var kills: Int = 0,
    var elixirSpent: Int = 0
)

data class BattleSnapshot(
    val arena: ArenaConfig,
    val mode: BattleMode,
    val playerName: String,
    val playerLevel: Int,
    val enemyName: String,
    val enemyLevel: Int,
    val timeRemainingSec: Float,
    val isOvertime: Boolean,
    val elixirMultiplier: Float,
    val playerElixir: Float,
    val enemyElixir: Float,
    val handCardIds: List<String>,
    val nextCardId: String,
    val evolvedReadyCardIds: Set<String>,
    val cardCycleCounts: Map<String, Int>,
    val lastPlayedCardId: String?,
    val lastPlayedCardCost: Int,
    val towers: List<BattleTower>,
    val entities: List<BattleEntity>,
    val projectiles: List<BattleProjectile>,
    val spellEffects: List<BattleSpellEffect>,
    val floatingTexts: List<FloatingBattleText>,
    val emotePopup: BattleEmotePopup?,
    val activeChampion: BattleEntity?,
    val playerCrowns: Int,
    val enemyCrowns: Int,
    val isFinished: Boolean,
    val result: BattleResultSummary?
)

class RoyaleBattleEngine(
    val arena: ArenaConfig,
    val mode: BattleMode,
    val aiDifficulty: AiDifficulty,
    val playerName: String,
    val playerLevel: Int,
    val playerDeckIds: List<String>,
    val playerCardLevels: Map<String, Int>,
    val playerEvoSlotIds: Set<String>,
    val towerTroop: TowerTroopId,
    val playerTrophies: Int,
    val enemyName: String = listOf("Kaelen_AI", "ValquiriaBot", "Sir_Royale", "MagoSupremo", "ArenaMaster").random(),
    val enemyLevel: Int = playerLevel.coerceIn(1, 15),
    val onSfx: ((RoyaleAudioEngine.SfxType) -> Unit)? = null
) {
    private val effectiveCardLevelOverride: Int? =
        if (mode == BattleMode.FRIENDLY || mode == BattleMode.PERMANENT_TOURNAMENT) 11 else null

    private var elapsedSec = 0f
    private var timeRemainingSec = mode.durationSec.toFloat()
    private var isOvertime = false
    private var isFinished = false
    private var resultSummary: BattleResultSummary? = null

    private var playerElixir = 5.0f
    private var enemyElixir = 5.0f
    private var allyAiElixir = 4.0f

    private val deckQueue = ArrayDeque<String>()
    private val handCards = mutableListOf<String>()
    private var nextCardId: String = ""
    private val cardCycleCounts = mutableMapOf<String, Int>()
    private var lastPlayedCardId: String? = null
    private var lastPlayedCardCost: Int = 0

    private val enemyDeckIds = listOf(
        "cavaleiro", "arqueiras", "gigante", "mosqueteira",
        "valquiria", "corredor", "bola_de_fogo", "flechas"
    ).shuffled()
    private var enemyDeckCursor = 0
    private var enemyAiTimer = 1.6f
    private var allyAiTimer = 3.2f

    private val towers = mutableListOf<BattleTower>()
    private val entities = mutableListOf<BattleEntity>()
    private val projectiles = mutableListOf<BattleProjectile>()
    private val spellEffects = mutableListOf<BattleSpellEffect>()
    private val floatingTexts = mutableListOf<FloatingBattleText>()
    private var currentEmote: BattleEmotePopup? = null
    private val statsMap = mutableMapOf<String, MutableCardStat>()

    init {
        val shuffled = playerDeckIds.shuffled()
        handCards.addAll(shuffled.take(4))
        nextCardId = shuffled.getOrElse(4) { shuffled.first() }
        shuffled.drop(5).forEach { deckQueue.addLast(it) }
        playerDeckIds.forEach { statsMap[it] = MutableCardStat(it) }

        initTowers()
    }

    private fun initTowers() {
        val pLvl = effectiveCardLevelOverride ?: (playerLevel + 8).coerceIn(1, 15)
        val eLvl = effectiveCardLevelOverride ?: (enemyLevel + 8).coerceIn(1, 15)

        val pScale = Math.pow(1.08, (pLvl - 11).toDouble()).toFloat()
        val eScale = Math.pow(1.08, (eLvl - 11).toDouble()).toFloat()

        val princessHp = (3052 * towerTroop.hpMult * pScale).roundToInt()
        val princessDmg = (109 * towerTroop.damageMult * pScale).roundToInt()
        val kingHp = (4824 * pScale).roundToInt()
        val kingDmg = (109 * pScale).roundToInt()

        val ePrincessHp = (3052 * eScale).roundToInt()
        val ePrincessDmg = (109 * eScale).roundToInt()
        val eKingHp = (4824 * eScale).roundToInt()
        val eKingDmg = (109 * eScale).roundToInt()

        towers.addAll(
            listOf(
                BattleTower("p_king", TeamSide.PLAYER, TowerKind.KING, ArenaMetrics.PLAYER_KING_X, ArenaMetrics.PLAYER_KING_Y, kingHp, kingHp, kingDmg, 1.0f, 7.0f, 14f, isActive = false),
                BattleTower("p_left", TeamSide.PLAYER, TowerKind.PRINCESS_LEFT, ArenaMetrics.PLAYER_LEFT_X, ArenaMetrics.PLAYER_PRINCESS_Y, princessHp, princessHp, princessDmg, towerTroop.hitSpeedSec, towerTroop.range, towerTroop.projectileSpeed, isActive = true),
                BattleTower("p_right", TeamSide.PLAYER, TowerKind.PRINCESS_RIGHT, ArenaMetrics.PLAYER_RIGHT_X, ArenaMetrics.PLAYER_PRINCESS_Y, princessHp, princessHp, princessDmg, towerTroop.hitSpeedSec, towerTroop.range, towerTroop.projectileSpeed, isActive = true),
                BattleTower("e_king", TeamSide.ENEMY, TowerKind.KING, ArenaMetrics.ENEMY_KING_X, ArenaMetrics.ENEMY_KING_Y, eKingHp, eKingHp, eKingDmg, 1.0f, 7.0f, 14f, isActive = false),
                BattleTower("e_left", TeamSide.ENEMY, TowerKind.PRINCESS_LEFT, ArenaMetrics.ENEMY_LEFT_X, ArenaMetrics.ENEMY_PRINCESS_Y, ePrincessHp, ePrincessHp, ePrincessDmg, 0.8f, 7.5f, 14f, isActive = true),
                BattleTower("e_right", TeamSide.ENEMY, TowerKind.PRINCESS_RIGHT, ArenaMetrics.ENEMY_RIGHT_X, ArenaMetrics.ENEMY_PRINCESS_Y, ePrincessHp, ePrincessHp, ePrincessDmg, 0.8f, 7.5f, 14f, isActive = true)
            )
        )
    }

    fun currentElixirMultiplier(): Float {
        if (mode == BattleMode.TRIPLE_ELIXIR) return 3.0f
        if (mode == BattleMode.DOUBLE_ELIXIR || mode == BattleMode.SUDDEN_DEATH) return 2.0f
        return if (isOvertime || timeRemainingSec <= 60f) mode.elixirMultiplier * 2.0f else mode.elixirMultiplier
    }

    fun canDeployCardAt(cardId: String, x: Float, y: Float, team: TeamSide = TeamSide.PLAYER): Boolean {
        val spec = RoyaleCardCatalog.get(cardId) ?: return false
        if (x < 0.6f || x > ArenaMetrics.WIDTH - 0.6f || y < 1.2f || y > ArenaMetrics.HEIGHT - 1.2f) {
            return false
        }
        if (spec.canDeployAnywhere || spec.category == CardCategory.SPELL) return true

        return if (team == TeamSide.PLAYER) {
            val leftEnemyDown = towers.firstOrNull { it.id == "e_left" }?.isAlive == false
            val rightEnemyDown = towers.firstOrNull { it.id == "e_right" }?.isAlive == false
            val minY = when {
                x < ArenaMetrics.WIDTH / 2f && leftEnemyDown -> 10.5f
                x >= ArenaMetrics.WIDTH / 2f && rightEnemyDown -> 10.5f
                else -> ArenaMetrics.RIVER_Y + 0.9f
            }
            y >= minY
        } else {
            val leftPlayerDown = towers.firstOrNull { it.id == "p_left" }?.isAlive == false
            val rightPlayerDown = towers.firstOrNull { it.id == "p_right" }?.isAlive == false
            val maxY = when {
                x < ArenaMetrics.WIDTH / 2f && leftPlayerDown -> 21.5f
                x >= ArenaMetrics.WIDTH / 2f && rightPlayerDown -> 21.5f
                else -> ArenaMetrics.RIVER_Y - 0.9f
            }
            y <= maxY
        }
    }

    fun effectiveCardCost(cardId: String): Int {
        val spec = RoyaleCardCatalog.get(cardId) ?: return 99
        return if (cardId == "espelho") {
            if (lastPlayedCardId != null) (lastPlayedCardCost + 1).coerceAtMost(10) else 99
        } else {
            spec.elixirCost
        }
    }

    fun playCard(handIndex: Int, x: Float, y: Float): Boolean {
        if (isFinished) return false
        val cardId = handCards.getOrNull(handIndex) ?: return false
        val actualCardId = if (cardId == "espelho") lastPlayedCardId ?: return false else cardId
        val cost = effectiveCardCost(cardId)
        if (playerElixir < cost) return false
        if (!canDeployCardAt(actualCardId, x, y, TeamSide.PLAYER)) return false

        val spec = RoyaleCardCatalog.get(actualCardId) ?: return false
        val baseLevel = effectiveCardLevelOverride ?: (playerCardLevels[actualCardId] ?: spec.rarity.startingLevel)
        val level = if (cardId == "espelho") (baseLevel + 1).coerceAtMost(15) else baseLevel

        val evoSpec = spec.evolution
        val cycleCount = cardCycleCounts[actualCardId] ?: 0
        val isEvoReady = evoSpec != null && actualCardId in playerEvoSlotIds && cycleCount >= evoSpec.cyclesRequired

        playerElixir -= cost
        deployCardInternal(actualCardId, TeamSide.PLAYER, level, x, y, isEvoReady)

        // Update cycle & stats
        if (evoSpec != null && actualCardId in playerEvoSlotIds) {
            cardCycleCounts[actualCardId] = if (isEvoReady) 0 else cycleCount + 1
        }
        val stat = statsMap.getOrPut(cardId) { MutableCardStat(cardId) }
        stat.timesPlayed += 1
        stat.elixirSpent += cost

        if (cardId != "espelho") {
            lastPlayedCardId = cardId
            lastPlayedCardCost = cost
        }

        // Cycle hand
        handCards[handIndex] = nextCardId
        deckQueue.addLast(cardId)
        nextCardId = deckQueue.removeFirst()

        onSfx?.invoke(if (spec.category == CardCategory.SPELL) RoyaleAudioEngine.SfxType.SPELL else RoyaleAudioEngine.SfxType.DEPLOY)
        return true
    }

    fun activateChampionAbility(): Boolean {
        if (isFinished) return false
        val champ = entities.firstOrNull {
            it.team == TeamSide.PLAYER && it.isAlive && it.championCooldown <= 0f &&
                (it.abilityType == AbilityType.CHAMPION_SUMMON ||
                    it.abilityType == AbilityType.CHAMPION_CHAIN_DASH ||
                    it.abilityType == AbilityType.CHAMPION_CLOAK_HASTE)
        } ?: return false

        val spec = RoyaleCardCatalog.get(champ.cardId) ?: return false
        val ability = spec.ability ?: return false
        if (playerElixir < ability.activeElixirCost) return false

        playerElixir -= ability.activeElixirCost
        champ.championCooldown = ability.cooldownSec
        onSfx?.invoke(RoyaleAudioEngine.SfxType.SPELL)

        when (champ.abilityType) {
            AbilityType.CHAMPION_SUMMON -> {
                spawnUnits("esqueletos", TeamSide.PLAYER, champ.level, champ.x, champ.y, countOverride = 6)
                addFloatingText(champ.x, champ.y - 1f, "ALMAS!", 0xFFFACC15)
            }
            AbilityType.CHAMPION_CHAIN_DASH -> {
                champ.championActiveTimer = ability.durationSec
                champ.isCharging = true
                addFloatingText(champ.x, champ.y - 1f, "INVESTIDA!", 0xFFFDE047)
            }
            AbilityType.CHAMPION_CLOAK_HASTE -> {
                champ.isInvisible = true
                champ.championActiveTimer = ability.durationSec
                champ.rageTimer = ability.durationSec
                champ.ragePct = 1.5f
                addFloatingText(champ.x, champ.y - 1f, "MANTO REAL!", 0xFFD946EF)
            }
            else -> Unit
        }
        return true
    }

    fun triggerPlayerEmote(emote: EmoteDef) {
        currentEmote = BattleEmotePopup(
            id = UUID.randomUUID().toString(),
            team = TeamSide.PLAYER,
            icon = emote.iconTag,
            phrase = emote.phrase
        )
    }

    private fun deployCardInternal(
        cardId: String,
        team: TeamSide,
        level: Int,
        x: Float,
        y: Float,
        isEvolved: Boolean
    ) {
        val spec = RoyaleCardCatalog.get(cardId) ?: return
        if (spec.category == CardCategory.SPELL) {
            castSpell(spec, team, level, x, y, isEvolved)
            return
        }

        // Spawn entry impact (Mago de Gelo, Mago Elétrico)
        val spawnDmg = spec.spawnDamageAtLevel(level)
        if (spawnDmg > 0) {
            val radius = spec.ability?.effectRadius ?: 2.5f
            applyAreaDamage(
                sourceCardId = cardId,
                team = team,
                x = x,
                y = y,
                radius = radius,
                damage = spawnDmg,
                towerDamage = (spawnDmg * 0.3f).roundToInt(),
                hitAir = true,
                stunSec = if (cardId == "mago_eletrico") 0.5f else 0f,
                slowSec = if (cardId == "mago_de_gelo") 2.5f else 0f,
                slowPct = if (cardId == "mago_de_gelo") 0.35f else 0f
            )
            spellEffects.add(
                BattleSpellEffect(
                    id = UUID.randomUUID().toString(),
                    cardId = cardId,
                    team = team,
                    level = level,
                    x = x,
                    y = y,
                    radius = radius,
                    durationRemaining = 0.5f,
                    totalDuration = 0.5f,
                    colorHex = spec.primaryColorHex,
                    isBurstVisual = true
                )
            )
        }

        spawnUnits(cardId, team, level, x, y, isEvolved = isEvolved)
    }

    private fun spawnUnits(
        cardId: String,
        team: TeamSide,
        level: Int,
        centerX: Float,
        centerY: Float,
        countOverride: Int? = null,
        isEvolved: Boolean = false,
        isClone: Boolean = false
    ) {
        val spec = RoyaleCardCatalog.get(cardId) ?: return
        val count = countOverride ?: spec.spawnCount
        val evoMultHp = if (isEvolved && spec.evolution != null) spec.evolution.hpBonusMultiplier else 1.0f
        val evoMultDmg = if (isEvolved && spec.evolution != null) spec.evolution.damageBonusMultiplier else 1.0f

        for (i in 0 until count) {
            val angle = if (count == 1) 0f else (i.toFloat() / count) * 2f * PI.toFloat()
            val offsetDist = if (count == 1) 0f else 0.75f
            val sx = (centerX + cos(angle) * offsetDist).coerceIn(0.8f, ArenaMetrics.WIDTH - 0.8f)
            val sy = (centerY + sin(angle) * offsetDist).coerceIn(1.5f, ArenaMetrics.HEIGHT - 1.5f)
            val rawHp = if (isClone) 1 else (spec.hpAtLevel(level) * evoMultHp).roundToInt()
            val rawShield = if (isClone) 0 else spec.shieldHpAtLevel(level)

            entities.add(
                BattleEntity(
                    id = UUID.randomUUID().toString(),
                    cardId = spec.id,
                    name = spec.name,
                    team = team,
                    level = level,
                    category = spec.category,
                    x = sx,
                    y = sy,
                    radius = when {
                        spec.category == CardCategory.BUILDING -> 1.05f
                        spec.hpAt11 >= 2500 -> 0.82f
                        spec.spawnCount >= 3 -> 0.42f
                        else -> 0.58f
                    },
                    maxHp = rawHp,
                    hp = rawHp,
                    maxShieldHp = rawShield,
                    shieldHp = rawShield,
                    baseDamage = (spec.damageAtLevel(level) * evoMultDmg).roundToInt(),
                    chargeDamage = (spec.chargeDamageAtLevel(level) * evoMultDmg).roundToInt(),
                    deathDamage = spec.deathDamageAtLevel(level),
                    areaSplashRadius = spec.areaSplashRadius,
                    hitSpeedSec = spec.hitSpeedSec,
                    projectileSpeed = spec.projectileSpeed,
                    chainTargets = spec.chainTargets,
                    infernoMaxMult = spec.infernoMaxMultiplier,
                    movementSpeed = spec.movementSpeed,
                    chargeSpeed = spec.chargeSpeed,
                    attackRange = spec.attackRange,
                    sightRange = spec.sightRange,
                    chaseBreakRange = spec.chaseBreakRange,
                    isFlying = spec.isFlying,
                    canJumpRiver = spec.canJumpRiver,
                    targetMask = spec.targetMask,
                    knockbackResistance = spec.knockbackResistance,
                    abilityType = spec.ability?.type ?: AbilityType.NONE,
                    spawnUnitId = spec.spawnUnitId,
                    spawnIntervalSec = spec.spawnIntervalSec,
                    isEvolved = isEvolved,
                    isClone = isClone,
                    primaryColorHex = spec.primaryColorHex,
                    deployTimer = if (isClone) 0.25f else spec.deployTimeSec,
                    lifetimeRemaining = spec.lifetimeSec,
                    spawnTimer = if (spec.spawnIntervalSec > 0f) spec.spawnIntervalSec * 0.6f else 0f,
                    isInvisible = spec.ability?.type == AbilityType.STEALTH_AMBUSH
                )
            )
        }
    }

    private fun castSpell(
        spec: CardSpec,
        team: TeamSide,
        level: Int,
        x: Float,
        y: Float,
        isEvolved: Boolean
    ) {
        when (spec.id) {
            "barril_de_goblins" -> {
                spawnUnits("goblins", team, level, x, y, countOverride = 3)
                if (isEvolved) {
                    val decoyX = (ArenaMetrics.WIDTH - x).coerceIn(1.5f, ArenaMetrics.WIDTH - 1.5f)
                    spawnUnits("goblins", team, level, decoyX, y, countOverride = 2, isClone = true)
                }
                spellEffects.add(
                    BattleSpellEffect(UUID.randomUUID().toString(), spec.id, team, level, x, y, 1.5f, 0.5f, 0.5f, colorHex = spec.primaryColorHex, isBurstVisual = true)
                )
            }
            "cemiterio" -> {
                spellEffects.add(
                    BattleSpellEffect(
                        id = UUID.randomUUID().toString(),
                        cardId = spec.id,
                        team = team,
                        level = level,
                        x = x,
                        y = y,
                        radius = spec.areaSplashRadius,
                        durationRemaining = spec.spellDurationSec,
                        totalDuration = spec.spellDurationSec,
                        tickInterval = 0.65f,
                        spawnUnitId = "esqueletos",
                        colorHex = spec.primaryColorHex
                    )
                )
            }
            "clone" -> {
                val allies = entities.filter {
                    it.team == team && it.isAlive && it.category == CardCategory.TROOP && !it.isClone &&
                        hypot(it.x - x, it.y - y) <= spec.areaSplashRadius
                }.toList()
                allies.forEach { ally ->
                    spawnUnits(ally.cardId, team, ally.level, (ally.x + 0.7f).coerceAtMost(ArenaMetrics.WIDTH - 1f), ally.y, countOverride = 1, isClone = true)
                }
                spellEffects.add(
                    BattleSpellEffect(UUID.randomUUID().toString(), spec.id, team, level, x, y, spec.areaSplashRadius, 0.6f, 0.6f, colorHex = spec.primaryColorHex, isBurstVisual = true)
                )
            }
            "relampago" -> {
                val dmg = spec.damageAtLevel(level)
                val towerDmg = spec.crownTowerDamageAtLevel(level)
                val targets = mutableListOf<Pair<Int, () -> Unit>>()

                entities.filter { it.team != team && it.isAlive && hypot(it.x - x, it.y - y) <= spec.areaSplashRadius }
                    .forEach { target ->
                        targets.add(target.hp to {
                            dealDamageToEntity(target, dmg, team, spec.id)
                            target.stunTimer = max(target.stunTimer, spec.spellStunSec)
                            target.isCharging = false
                            target.infernoLockTime = 0f
                        })
                    }
                towers.filter { it.team != team && it.isAlive && hypot(it.x - x, it.y - y) <= spec.areaSplashRadius + it.radius }
                    .forEach { t ->
                        targets.add(t.hp to {
                            dealDamageToTower(t, towerDmg, team, spec.id)
                            t.stunTimer = max(t.stunTimer, spec.spellStunSec)
                        })
                    }
                targets.sortedByDescending { it.first }.take(3).forEach { it.second.invoke() }
                spellEffects.add(
                    BattleSpellEffect(UUID.randomUUID().toString(), spec.id, team, level, x, y, spec.areaSplashRadius, 0.6f, 0.6f, colorHex = spec.primaryColorHex, isBurstVisual = true)
                )
            }
            "veneno", "tornado", "terremoto", "furia" -> {
                val ticks = max(1, (spec.spellDurationSec / 0.5f).roundToInt())
                val dmgTick = if (spec.damageAt11 > 0) (spec.damageAtLevel(level) / ticks).coerceAtLeast(1) else 0
                val tDmgTick = if (spec.damageAt11 > 0) (spec.crownTowerDamageAtLevel(level) / ticks).coerceAtLeast(1) else 0
                if (spec.id == "furia") {
                    applyAreaDamage(spec.id, team, x, y, spec.areaSplashRadius, spec.damageAtLevel(level), spec.crownTowerDamageAtLevel(level), hitAir = true)
                }
                spellEffects.add(
                    BattleSpellEffect(
                        id = UUID.randomUUID().toString(),
                        cardId = spec.id,
                        team = team,
                        level = level,
                        x = x,
                        y = y,
                        radius = spec.areaSplashRadius,
                        durationRemaining = spec.spellDurationSec,
                        totalDuration = spec.spellDurationSec,
                        tickInterval = 0.5f,
                        damagePerTick = if (spec.id == "furia") 0 else dmgTick,
                        towerDamagePerTick = if (spec.id == "furia") 0 else tDmgTick,
                        slowPct = spec.spellSlowPct,
                        ragePct = spec.spellRageBoostPct,
                        pullStrength = spec.spellPullStrength,
                        colorHex = spec.primaryColorHex
                    )
                )
            }
            else -> {
                // Instant area spells: Flechas, Bola de Fogo, Zap, Congelamento, Bola de Neve, O Tronco
                val waves = if (spec.id == "zap" && isEvolved) 3 else 1
                val dmg = spec.damageAtLevel(level) * waves
                val towerDmg = spec.crownTowerDamageAtLevel(level) * waves
                applyAreaDamage(
                    sourceCardId = spec.id,
                    team = team,
                    x = x,
                    y = y,
                    radius = spec.areaSplashRadius,
                    damage = dmg,
                    towerDamage = towerDmg,
                    hitAir = spec.targetMask != TargetMask.GROUND,
                    stunSec = spec.spellStunSec,
                    freezeSec = spec.spellFreezeSec,
                    slowSec = if (spec.spellSlowPct > 0f) 2.5f else 0f,
                    slowPct = spec.spellSlowPct,
                    knockbackDist = spec.spellKnockback
                )
                spellEffects.add(
                    BattleSpellEffect(UUID.randomUUID().toString(), spec.id, team, level, x, y, spec.areaSplashRadius, 0.55f, 0.55f, colorHex = spec.primaryColorHex, isBurstVisual = true)
                )
            }
        }
    }

    fun step(dtRaw: Float) {
        if (isFinished) return
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        elapsedSec += dt
        timeRemainingSec = max(0f, timeRemainingSec - dt)

        // Check Sudden Death or Time Expiration
        if (timeRemainingSec <= 0f) {
            val pCrowns = countCrowns(TeamSide.PLAYER)
            val eCrowns = countCrowns(TeamSide.ENEMY)
            if (pCrowns != eCrowns || isOvertime || mode.isSuddenDeath) {
                finishMatch()
                return
            } else {
                isOvertime = true
                timeRemainingSec = 60f
            }
        }

        // Regenerate Elixir
        val mult = currentElixirMultiplier()
        val regenPerSec = (1.0f / 2.8f) * mult
        playerElixir = min(10f, playerElixir + regenPerSec * dt)
        enemyElixir = min(10f, enemyElixir + regenPerSec * dt * aiDifficulty.elixirEfficiency)
        if (mode.hasAiAlly) {
            allyAiElixir = min(10f, allyAiElixir + regenPerSec * dt * 0.85f)
        }

        // AI Decisions
        updateEnemyAi(dt)
        if (mode.hasAiAlly) updateAllyAi(dt)

        // Update Spells
        updateSpells(dt)

        // Update Entities
        updateEntities(dt)

        // Resolve Unit Separation Physics
        resolveUnitCollisions()

        // Update Towers
        updateTowers(dt)

        // Update Projectiles
        updateProjectiles(dt)

        // Cleanup dead units & triggers
        handleDeathsAndCleanups()

        // Update Floating texts & Emote popup
        floatingTexts.forEach { it.lifeSec -= dt }
        floatingTexts.removeAll { it.lifeSec <= 0f }
        currentEmote?.let {
            it.lifeSec -= dt
            if (it.lifeSec <= 0f) currentEmote = null
        }

        // Victory checks
        val pKingAlive = towers.firstOrNull { it.id == "p_king" }?.isAlive == true
        val eKingAlive = towers.firstOrNull { it.id == "e_king" }?.isAlive == true
        if (!pKingAlive || !eKingAlive) {
            finishMatch()
            return
        }
        if ((mode.isSuddenDeath || isOvertime) && (countCrowns(TeamSide.PLAYER) != countCrowns(TeamSide.ENEMY))) {
            finishMatch()
        }
    }

    private fun updateEnemyAi(dt: Float) {
        enemyAiTimer -= dt
        if (enemyAiTimer > 0f) return
        enemyAiTimer = aiDifficulty.reactionSec

        val nextCard = enemyDeckIds[enemyDeckCursor % enemyDeckIds.size]
        val spec = RoyaleCardCatalog.get(nextCard) ?: return
        if (enemyElixir < spec.elixirCost) return

        // Threat evaluation near enemy side
        val threat = entities.firstOrNull { it.team == TeamSide.PLAYER && it.isAlive && it.y < 18f }
        val deployX: Float
        val deployY: Float

        if (spec.category == CardCategory.SPELL && threat != null) {
            deployX = threat.x
            deployY = threat.y
        } else if (threat != null) {
            deployX = threat.x.coerceIn(2.2f, ArenaMetrics.WIDTH - 2.2f)
            deployY = (threat.y - 3.0f).coerceIn(4.5f, ArenaMetrics.RIVER_Y - 1.2f)
        } else {
            val lane = if (Random.nextBoolean()) ArenaMetrics.LEFT_BRIDGE_X else ArenaMetrics.RIGHT_BRIDGE_X
            deployX = lane + (Random.nextFloat() - 0.5f) * 1.4f
            deployY = if (spec.hpAt11 >= 2500) 5.5f else 11.5f
        }

        if (canDeployCardAt(nextCard, deployX, deployY, TeamSide.ENEMY)) {
            enemyElixir -= spec.elixirCost
            val lvl = effectiveCardLevelOverride ?: enemyLevel.coerceIn(1, 15)
            deployCardInternal(nextCard, TeamSide.ENEMY, lvl, deployX, deployY, false)
            enemyDeckCursor++
        }
    }

    private fun updateAllyAi(dt: Float) {
        allyAiTimer -= dt
        if (allyAiTimer > 0f) return
        allyAiTimer = 2.6f
        val cardId = listOf("cavaleiro", "arqueiras", "valquiria", "servos", "mosqueteira").random()
        val spec = RoyaleCardCatalog.get(cardId) ?: return
        if (allyAiElixir < spec.elixirCost) return
        val lane = if (Random.nextBoolean()) ArenaMetrics.LEFT_BRIDGE_X else ArenaMetrics.RIGHT_BRIDGE_X
        val x = lane + (Random.nextFloat() - 0.5f) * 1.2f
        val y = 21.5f
        if (canDeployCardAt(cardId, x, y, TeamSide.PLAYER)) {
            allyAiElixir -= spec.elixirCost
            deployCardInternal(cardId, TeamSide.PLAYER, effectiveCardLevelOverride ?: playerLevel, x, y, false)
        }
    }

    private fun updateSpells(dt: Float) {
        val it = spellEffects.iterator()
        while (it.hasNext()) {
            val fx = it.next()
            fx.durationRemaining -= dt
            if (fx.isBurstVisual) {
                if (fx.durationRemaining <= 0f) it.remove()
                continue
            }

            fx.tickAccumulator += dt
            if (fx.tickAccumulator >= fx.tickInterval) {
                fx.tickAccumulator -= fx.tickInterval

                if (fx.spawnUnitId != null) {
                    val rx = (fx.x + (Random.nextFloat() - 0.5f) * fx.radius).coerceIn(1.2f, ArenaMetrics.WIDTH - 1.2f)
                    val ry = (fx.y + (Random.nextFloat() - 0.5f) * fx.radius).coerceIn(1.5f, ArenaMetrics.HEIGHT - 1.5f)
                    spawnUnits(fx.spawnUnitId, fx.team, fx.level, rx, ry, countOverride = 1)
                }

                if (fx.damagePerTick > 0 || fx.slowPct > 0f || fx.pullStrength > 0f) {
                    entities.filter { it.team != fx.team && it.isAlive && hypot(it.x - fx.x, it.y - fx.y) <= fx.radius }
                        .forEach { target ->
                            val mult = if (fx.cardId == "terremoto" && target.category == CardCategory.BUILDING) 3 else 1
                            if (fx.damagePerTick > 0) dealDamageToEntity(target, fx.damagePerTick * mult, fx.team, fx.cardId)
                            if (fx.slowPct > 0f) {
                                target.slowTimer = 0.8f
                                target.slowPct = max(target.slowPct, fx.slowPct)
                            }
                            if (fx.pullStrength > 0f) {
                                val dx = fx.x - target.x
                                val dy = fx.y - target.y
                                val dist = hypot(dx, dy).coerceAtLeast(0.1f)
                                val pull = fx.pullStrength * 0.25f * (1f - target.knockbackResistance)
                                target.x += (dx / dist) * pull
                                target.y += (dy / dist) * pull
                            }
                        }
                    if (fx.towerDamagePerTick > 0) {
                        towers.filter { it.team != fx.team && it.isAlive && hypot(it.x - fx.x, it.y - fx.y) <= fx.radius + it.radius }
                            .forEach { t -> dealDamageToTower(t, fx.towerDamagePerTick, fx.team, fx.cardId) }
                    }
                }

                if (fx.ragePct > 0f) {
                    entities.filter { it.team == fx.team && it.isAlive && hypot(it.x - fx.x, it.y - fx.y) <= fx.radius }
                        .forEach { ally ->
                            ally.rageTimer = 0.7f
                            ally.ragePct = max(ally.ragePct, fx.ragePct)
                        }
                    towers.filter { it.team == fx.team && it.isAlive && hypot(it.x - fx.x, it.y - fx.y) <= fx.radius + it.radius }
                        .forEach { t -> t.rageTimer = 0.7f }
                }
            }

            if (fx.durationRemaining <= 0f) it.remove()
        }
    }

    private fun updateEntities(dt: Float) {
        val currentSnapshot = entities.toList()
        for (u in currentSnapshot) {
            if (!u.isAlive) continue

            if (u.hitFlashTimer > 0f) u.hitFlashTimer = max(0f, u.hitFlashTimer - dt)
            if (u.attackAnimTimer > 0f) u.attackAnimTimer = max(0f, u.attackAnimTimer - dt)
            if (u.championCooldown > 0f) u.championCooldown = max(0f, u.championCooldown - dt)

            if (u.championActiveTimer > 0f) {
                u.championActiveTimer = max(0f, u.championActiveTimer - dt)
                if (u.championActiveTimer <= 0f && u.cardId == "rainha_arqueira") {
                    u.isInvisible = false
                }
            }

            if (u.deployTimer > 0f) {
                u.deployTimer = max(0f, u.deployTimer - dt)
                continue
            }

            // Building lifetime decay
            if (u.category == CardCategory.BUILDING && u.lifetimeRemaining > 0f) {
                u.lifetimeRemaining -= dt
                if (u.lifetimeRemaining <= 0f) {
                    u.hp = 0
                    continue
                }
            }

            // Status timers
            if (u.freezeTimer > 0f) {
                u.freezeTimer = max(0f, u.freezeTimer - dt)
                u.infernoLockTime = 0f
                continue
            }
            if (u.stunTimer > 0f) {
                u.stunTimer = max(0f, u.stunTimer - dt)
                u.infernoLockTime = 0f
                continue
            }
            if (u.slowTimer > 0f) {
                u.slowTimer = max(0f, u.slowTimer - dt)
                if (u.slowTimer <= 0f) u.slowPct = 0f
            }
            if (u.rageTimer > 0f) {
                u.rageTimer = max(0f, u.rageTimer - dt)
                if (u.rageTimer <= 0f) u.ragePct = 0f
            }

            val speedMult = (1f + u.ragePct) * (1f - u.slowPct)
            if (u.attackCooldown > 0f) {
                u.attackCooldown = max(0f, u.attackCooldown - dt * speedMult)
            }

            // Periodic spawning / Elixir pump
            if (u.spawnIntervalSec > 0f) {
                u.spawnTimer -= dt * (1f + u.ragePct)
                if (u.spawnTimer <= 0f) {
                    u.spawnTimer = u.spawnIntervalSec
                    if (u.abilityType == AbilityType.ELIXIR_PUMP) {
                        if (u.team == TeamSide.PLAYER) {
                            playerElixir = min(10f, playerElixir + 1f)
                            addFloatingText(u.x, u.y - 0.8f, "+1 ELIXIR", 0xFFD946EF)
                        } else {
                            enemyElixir = min(10f, enemyElixir + 1f)
                        }
                    } else if (u.spawnUnitId != null) {
                        val forwardOffset = if (u.team == TeamSide.PLAYER) -0.9f else 0.9f
                        spawnUnits(u.spawnUnitId, u.team, u.level, u.x, u.y + forwardOffset, countOverride = if (u.cardId == "bruxa") 3 else 1)
                    }
                }
            }

            if (u.targetMask == TargetMask.NONE) continue

            // Find best target
            val target = findTargetFor(u)
            if (u.cardId == "tesla") {
                u.isHiddenUnderground = (target == null)
            }

            if (target != null) {
                val dist = hypot(target.x - u.x, target.y - u.y) - target.radius
                u.facingAngleRad = kotlin.math.atan2(target.y - u.y, target.x - u.x)

                // Bandit dash trigger
                if (u.abilityType == AbilityType.DASH_INVULNERABLE && !u.isCharging && dist in 3.0f..6.0f) {
                    u.isCharging = true
                    u.dashTimer = 0.55f
                }

                if (dist <= u.attackRange) {
                    // Attack!
                    if (u.isInvisible) u.isInvisible = false
                    u.stealthResetTimer = 1.6f
                    if (u.attackCooldown <= 0f) {
                        performUnitAttack(u, target)
                        u.attackCooldown = u.hitSpeedSec
                    }
                } else if (u.movementSpeed > 0f) {
                    // Move toward target
                    if (u.abilityType == AbilityType.STEALTH_AMBUSH && !u.isInvisible) {
                        u.stealthResetTimer -= dt
                        if (u.stealthResetTimer <= 0f) u.isInvisible = true
                    }
                    moveUnitToward(u, target.x, target.y, dt, speedMult)
                    u.infernoLockTime = 0f
                }
            } else if (u.movementSpeed > 0f) {
                // March toward nearest enemy tower
                u.infernoLockTime = 0f
                if (u.abilityType == AbilityType.STEALTH_AMBUSH && !u.isInvisible) {
                    u.stealthResetTimer -= dt
                    if (u.stealthResetTimer <= 0f) u.isInvisible = true
                }
                val marchTarget = findNearestEnemyTower(u)
                if (marchTarget != null) {
                    val dest = computeBridgeWaypoint(u, marchTarget.x, marchTarget.y)
                    u.facingAngleRad = kotlin.math.atan2(dest.second - u.y, dest.first - u.x)
                    moveUnitToward(u, dest.first, dest.second, dt, speedMult)
                }
            }
        }
    }

    private data class CombatTarget(
        val id: String,
        val x: Float,
        val y: Float,
        val radius: Float,
        val entity: BattleEntity? = null,
        val tower: BattleTower? = null
    )

    private fun findTargetFor(u: BattleEntity): CombatTarget? {
        var best: CombatTarget? = null
        var bestDist = Float.MAX_VALUE

        if (u.targetMask != TargetMask.BUILDINGS_ONLY) {
            for (other in entities) {
                if (!other.isAlive || other.team == u.team) continue
                if (other.isInvisible || other.isHiddenUnderground) continue
                if (other.isFlying && u.targetMask == TargetMask.GROUND) continue
                val d = hypot(other.x - u.x, other.y - u.y) - other.radius
                if (d <= u.sightRange && d < bestDist) {
                    bestDist = d
                    best = CombatTarget(other.id, other.x, other.y, other.radius, entity = other)
                }
            }
        }

        // Check enemy buildings
        for (other in entities) {
            if (!other.isAlive || other.team == u.team || other.category != CardCategory.BUILDING || other.isHiddenUnderground) continue
            val d = hypot(other.x - u.x, other.y - u.y) - other.radius
            val scanRange = if (u.targetMask == TargetMask.BUILDINGS_ONLY) 9.0f else u.sightRange
            if (d <= scanRange && d < bestDist) {
                bestDist = d
                best = CombatTarget(other.id, other.x, other.y, other.radius, entity = other)
            }
        }

        // Check enemy towers
        for (t in towers) {
            if (!t.isAlive || t.team == u.team) continue
            val d = hypot(t.x - u.x, t.y - u.y) - t.radius
            val maxT = if (u.targetMask == TargetMask.BUILDINGS_ONLY) Float.MAX_VALUE else u.sightRange
            if (d <= maxT && d < bestDist) {
                bestDist = d
                best = CombatTarget(t.id, t.x, t.y, t.radius, tower = t)
            }
        }
        return best
    }

    private fun findNearestEnemyTower(u: BattleEntity): BattleTower? {
        return towers.filter { it.team != u.team && it.isAlive }
            .minByOrNull { hypot(it.x - u.x, it.y - u.y) }
    }

    private fun computeBridgeWaypoint(u: BattleEntity, targetX: Float, targetY: Float): Pair<Float, Float> {
        if (u.isFlying || u.canJumpRiver) return targetX to targetY
        val crossingDown = u.y < ArenaMetrics.RIVER_Y - 0.5f && targetY > ArenaMetrics.RIVER_Y + 0.5f
        val crossingUp = u.y > ArenaMetrics.RIVER_Y + 0.5f && targetY < ArenaMetrics.RIVER_Y - 0.5f
        if (!crossingDown && !crossingUp) return targetX to targetY

        val bridgeX = if (abs(u.x - ArenaMetrics.LEFT_BRIDGE_X) <= abs(u.x - ArenaMetrics.RIGHT_BRIDGE_X)) {
            ArenaMetrics.LEFT_BRIDGE_X
        } else {
            ArenaMetrics.RIGHT_BRIDGE_X
        }
        val onBridgeX = abs(u.x - bridgeX) <= ArenaMetrics.BRIDGE_HALF_WIDTH * 0.7f
        return if (onBridgeX) targetX to targetY else bridgeX to ArenaMetrics.RIVER_Y
    }

    private fun moveUnitToward(u: BattleEntity, tx: Float, ty: Float, dt: Float, speedMult: Float) {
        val dx = tx - u.x
        val dy = ty - u.y
        val dist = hypot(dx, dy)
        if (dist < 0.05f) return

        // Check Charge build-up
        if (u.chargeSpeed > u.movementSpeed && (u.abilityType == AbilityType.CHARGE || u.abilityType == AbilityType.CHAMPION_CHAIN_DASH)) {
            u.distanceMovedSinceAttack += u.movementSpeed * dt
            if (u.distanceMovedSinceAttack >= 2.6f) {
                u.isCharging = true
            }
        }

        val activeSpeed = (if (u.isCharging && u.chargeSpeed > 0f) u.chargeSpeed else u.movementSpeed) * speedMult
        val step = min(dist, activeSpeed * dt)
        u.x = (u.x + (dx / dist) * step).coerceIn(0.6f, ArenaMetrics.WIDTH - 0.6f)
        u.y = (u.y + (dy / dist) * step).coerceIn(1.2f, ArenaMetrics.HEIGHT - 1.2f)
    }

    private fun performUnitAttack(u: BattleEntity, target: CombatTarget) {
        u.attackAnimTimer = 0.25f

        // Fisherman Hook
        if (u.abilityType == AbilityType.HOOK_PULL && target.entity != null && hypot(target.x - u.x, target.y - u.y) > 2.2f) {
            val dx = u.x - target.x
            val dy = u.y - target.y
            val dist = hypot(dx, dy).coerceAtLeast(0.1f)
            target.entity.x += (dx / dist) * 2.5f
            target.entity.y += (dy / dist) * 2.5f
            target.entity.slowTimer = 1.5f
            target.entity.slowPct = 0.35f
        }

        // Inferno Beam Ramping
        val dmg = if (u.abilityType == AbilityType.INFERNO_RAMP) {
            if (u.lockedTargetId == target.id) {
                u.infernoLockTime += u.hitSpeedSec
            } else {
                u.lockedTargetId = target.id
                u.infernoLockTime = 0f
            }
            val ramp = (1f + (u.infernoLockTime / 3.5f) * (u.infernoMaxMult - 1f)).coerceAtMost(u.infernoMaxMult)
            (u.baseDamage * ramp).roundToInt()
        } else if (u.isCharging && u.chargeDamage > 0) {
            u.isCharging = false
            u.distanceMovedSinceAttack = 0f
            u.dashTimer = 0f
            u.chargeDamage
        } else {
            u.baseDamage
        }

        // Evolved Skeletons multiply on hit
        if (u.isEvolved && u.cardId == "esqueletos" && entities.count { it.team == u.team && it.cardId == "esqueletos" } < 8) {
            spawnUnits("esqueletos", u.team, u.level, u.x + 0.4f, u.y, countOverride = 1, isEvolved = true)
        }

        if (u.projectileSpeed > 0f) {
            // Spawn ranged projectile
            projectiles.add(
                BattleProjectile(
                    id = UUID.randomUUID().toString(),
                    team = u.team,
                    sourceCardId = u.cardId,
                    x = u.x,
                    y = u.y,
                    targetId = target.id,
                    targetLastX = target.x,
                    targetLastY = target.y,
                    speed = u.projectileSpeed,
                    damage = dmg,
                    splashRadius = u.areaSplashRadius,
                    stunSec = if (u.abilityType == AbilityType.ZAP_SPAWN_SPLIT || u.abilityType == AbilityType.CHAIN_STUN) 0.5f else 0f,
                    slowSec = if (u.abilityType == AbilityType.FREEZE_SLOW_SPAWN) 2.5f else 0f,
                    slowPct = if (u.abilityType == AbilityType.FREEZE_SLOW_SPAWN) 0.35f else 0f,
                    colorHex = u.primaryColorHex
                )
            )
            // Chain lightning extra targets (Electro Wizard / Electro Dragon)
            if (u.chainTargets > 1) {
                val extra = entities.filter {
                    it.isAlive && it.team != u.team && it.id != target.id && hypot(it.x - target.x, it.y - target.y) <= 3.5f
                }.take(u.chainTargets - 1)
                extra.forEach { ex ->
                    dealDamageToEntity(ex, dmg, u.team, u.cardId)
                    ex.stunTimer = max(ex.stunTimer, 0.5f)
                }
            }
        } else {
            // Melee hit (single or splash)
            onSfx?.invoke(RoyaleAudioEngine.SfxType.HIT)
            if (u.areaSplashRadius > 0f) {
                applyAreaDamage(u.cardId, u.team, target.x, target.y, u.areaSplashRadius, dmg, dmg, hitAir = u.targetMask != TargetMask.GROUND)
            } else {
                target.entity?.let { dealDamageToEntity(it, dmg, u.team, u.cardId) }
                target.tower?.let { dealDamageToTower(it, dmg, u.team, u.cardId) }
            }
        }
    }

    private fun updateTowers(dt: Float) {
        for (t in towers) {
            if (!t.isAlive) continue
            if (t.hitFlashTimer > 0f) t.hitFlashTimer = max(0f, t.hitFlashTimer - dt)
            if (!t.isActive) continue
            if (t.freezeTimer > 0f) {
                t.freezeTimer = max(0f, t.freezeTimer - dt)
                continue
            }
            if (t.stunTimer > 0f) {
                t.stunTimer = max(0f, t.stunTimer - dt)
                continue
            }
            val speedMult = if (t.rageTimer > 0f) {
                t.rageTimer = max(0f, t.rageTimer - dt)
                1.35f
            } else 1.0f

            if (t.attackCooldown > 0f) {
                t.attackCooldown = max(0f, t.attackCooldown - dt * speedMult)
                continue
            }

            val target = entities.filter {
                it.team != t.team && it.isAlive && !it.isInvisible && !it.isHiddenUnderground &&
                    hypot(it.x - t.x, it.y - t.y) <= t.range + it.radius
            }.minByOrNull { hypot(it.x - t.x, it.y - t.y) }

            if (target != null) {
                t.attackCooldown = t.hitSpeedSec
                projectiles.add(
                    BattleProjectile(
                        id = UUID.randomUUID().toString(),
                        team = t.team,
                        sourceCardId = t.id,
                        x = t.x,
                        y = t.y,
                        targetId = target.id,
                        targetLastX = target.x,
                        targetLastY = target.y,
                        speed = t.projectileSpeed,
                        damage = t.damage,
                        splashRadius = 0f,
                        colorHex = if (t.team == TeamSide.PLAYER) 0xFF38BDF8 else 0xFFF43F5E
                    )
                )
            }
        }
    }

    private fun updateProjectiles(dt: Float) {
        val it = projectiles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            val targetEntity = entities.firstOrNull { it.id == p.targetId && it.isAlive }
            val targetTower = towers.firstOrNull { it.id == p.targetId && it.isAlive }
            val tx = targetEntity?.x ?: targetTower?.x ?: p.targetLastX
            val ty = targetEntity?.y ?: targetTower?.y ?: p.targetLastY
            p.targetLastX = tx
            p.targetLastY = ty

            val dx = tx - p.x
            val dy = ty - p.y
            val dist = hypot(dx, dy)
            val step = p.speed * dt
            if (dist <= step + 0.25f) {
                if (p.splashRadius > 0f) {
                    applyAreaDamage(
                        sourceCardId = p.sourceCardId,
                        team = p.team,
                        x = tx,
                        y = ty,
                        radius = p.splashRadius,
                        damage = p.damage,
                        towerDamage = p.damage,
                        hitAir = true,
                        stunSec = p.stunSec,
                        slowSec = p.slowSec,
                        slowPct = p.slowPct
                    )
                } else {
                    targetEntity?.let {
                        dealDamageToEntity(it, p.damage, p.team, p.sourceCardId)
                        if (p.stunSec > 0f) it.stunTimer = max(it.stunTimer, p.stunSec)
                        if (p.slowSec > 0f) {
                            it.slowTimer = max(it.slowTimer, p.slowSec)
                            it.slowPct = max(it.slowPct, p.slowPct)
                        }
                    }
                    targetTower?.let { dealDamageToTower(it, p.damage, p.team, p.sourceCardId) }
                }
                it.remove()
            } else {
                p.x += (dx / dist) * step
                p.y += (dy / dist) * step
            }
        }
    }

    private fun resolveUnitCollisions() {
        val n = entities.size
        for (i in 0 until n) {
            val a = entities[i]
            if (!a.isAlive || a.category == CardCategory.BUILDING) continue
            for (j in i + 1 until n) {
                val b = entities[j]
                if (!b.isAlive || b.category == CardCategory.BUILDING) continue
                if (a.isFlying != b.isFlying) continue
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = hypot(dx, dy)
                val minDist = (a.radius + b.radius) * 0.85f
                if (dist in 0.001f..minDist) {
                    val push = (minDist - dist) * 0.45f
                    val nx = dx / dist
                    val ny = dy / dist
                    a.x = (a.x - nx * push).coerceIn(0.6f, ArenaMetrics.WIDTH - 0.6f)
                    a.y = (a.y - ny * push).coerceIn(1.2f, ArenaMetrics.HEIGHT - 1.2f)
                    b.x = (b.x + nx * push).coerceIn(0.6f, ArenaMetrics.WIDTH - 0.6f)
                    b.y = (b.y + ny * push).coerceIn(1.2f, ArenaMetrics.HEIGHT - 1.2f)
                }
            }
        }
    }

    private fun applyAreaDamage(
        sourceCardId: String,
        team: TeamSide,
        x: Float,
        y: Float,
        radius: Float,
        damage: Int,
        towerDamage: Int,
        hitAir: Boolean,
        stunSec: Float = 0f,
        freezeSec: Float = 0f,
        slowSec: Float = 0f,
        slowPct: Float = 0f,
        knockbackDist: Float = 0f
    ) {
        entities.filter {
            it.team != team && it.isAlive && (hitAir || !it.isFlying) && hypot(it.x - x, it.y - y) <= radius + it.radius
        }.forEach { target ->
            dealDamageToEntity(target, damage, team, sourceCardId)
            if (stunSec > 0f) {
                target.stunTimer = max(target.stunTimer, stunSec)
                target.isCharging = false
                target.infernoLockTime = 0f
            }
            if (freezeSec > 0f) target.freezeTimer = max(target.freezeTimer, freezeSec)
            if (slowSec > 0f) {
                target.slowTimer = max(target.slowTimer, slowSec)
                target.slowPct = max(target.slowPct, slowPct)
            }
            if (knockbackDist > 0f && target.knockbackResistance < 1f) {
                val dx = target.x - x
                val dy = target.y - y
                val d = hypot(dx, dy).coerceAtLeast(0.1f)
                val push = knockbackDist * (1f - target.knockbackResistance)
                target.x = (target.x + (dx / d) * push).coerceIn(0.6f, ArenaMetrics.WIDTH - 0.6f)
                target.y = (target.y + (dy / d) * push).coerceIn(1.2f, ArenaMetrics.HEIGHT - 1.2f)
            }
        }

        towers.filter {
            it.team != team && it.isAlive && hypot(it.x - x, it.y - y) <= radius + it.radius
        }.forEach { t ->
            dealDamageToTower(t, towerDamage, team, sourceCardId)
            if (stunSec > 0f) t.stunTimer = max(t.stunTimer, stunSec)
            if (freezeSec > 0f) t.freezeTimer = max(t.freezeTimer, freezeSec)
        }
    }

    private fun dealDamageToEntity(target: BattleEntity, rawDmg: Int, attackerTeam: TeamSide, sourceCardId: String) {
        if (!target.isAlive || target.dashTimer > 0f) return
        val dmg = if (target.isEvolved && target.cardId == "cavaleiro") (rawDmg * 0.6f).roundToInt() else rawDmg

        if (target.shieldHp > 0) {
            val absorbed = min(target.shieldHp, dmg)
            target.shieldHp -= absorbed
        } else {
            target.hp = max(0, target.hp - dmg)
        }
        target.hitFlashTimer = 0.14f

        if (attackerTeam == TeamSide.PLAYER) {
            statsMap[sourceCardId]?.let {
                it.damageDealt += dmg
                if (target.hp <= 0) it.kills += 1
            }
        }
    }

    private fun dealDamageToTower(tower: BattleTower, rawDmg: Int, attackerTeam: TeamSide, sourceCardId: String) {
        if (!tower.isAlive) return
        val prevHp = tower.hp
        tower.hp = max(0, tower.hp - rawDmg)
        tower.hitFlashTimer = 0.16f
        if (tower.kind == TowerKind.KING && !tower.isActive) {
            tower.isActive = true
        }
        if (attackerTeam == TeamSide.PLAYER) {
            statsMap[sourceCardId]?.let {
                it.damageDealt += rawDmg
                it.towerDamage += rawDmg
            }
        }
        if (prevHp > 0 && tower.hp <= 0) {
            onSfx?.invoke(RoyaleAudioEngine.SfxType.TOWER_DOWN)
            addFloatingText(tower.x, tower.y, "COROA!", 0xFFFACC15)
            // Activate team's King Tower when either Princess Tower falls
            towers.firstOrNull { it.team == tower.team && it.kind == TowerKind.KING }?.isActive = true
        }
    }

    private fun handleDeathsAndCleanups() {
        val dead = entities.filter { !it.isAlive }
        for (u in dead) {
            if (u.deathDamage > 0) {
                applyAreaDamage(u.cardId, u.team, u.x, u.y, 2.2f, u.deathDamage, (u.deathDamage * 0.5f).roundToInt(), hitAir = true)
            }
            if (u.abilityType == AbilityType.DEATH_RAGE) {
                spellEffects.add(
                    BattleSpellEffect(UUID.randomUUID().toString(), "furia", u.team, u.level, u.x, u.y, 3.0f, 5.5f, 5.5f, ragePct = 0.35f, colorHex = 0xFFD946EF)
                )
            }
            if (u.abilityType == AbilityType.DEATH_SPLIT_BOMB && !u.isClone) {
                spawnUnits("cavaleiro", u.team, (u.level - 2).coerceAtLeast(1), u.x - 0.6f, u.y, countOverride = 2)
            }
        }
        entities.removeAll { !it.isAlive }
    }

    private fun addFloatingText(x: Float, y: Float, text: String, colorHex: Long) {
        floatingTexts.add(FloatingBattleText(UUID.randomUUID().toString(), x, y, text, colorHex))
    }

    fun countCrowns(team: TeamSide): Int {
        val enemyTeam = if (team == TeamSide.PLAYER) TeamSide.ENEMY else TeamSide.PLAYER
        val enemyKingDead = towers.firstOrNull { it.team == enemyTeam && it.kind == TowerKind.KING }?.isAlive == false
        if (enemyKingDead) return 3
        return towers.count { it.team == enemyTeam && !it.isAlive && it.kind != TowerKind.KING }
    }

    private fun finishMatch() {
        if (isFinished) return
        isFinished = true
        val pCrowns = countCrowns(TeamSide.PLAYER)
        val eCrowns = countCrowns(TeamSide.ENEMY)
        val won = pCrowns > eCrowns
        val draw = pCrowns == eCrowns

        onSfx?.invoke(if (won) RoyaleAudioEngine.SfxType.VICTORY else RoyaleAudioEngine.SfxType.DEFEAT)

        val trophiesDelta = if (!mode.trophiesEnabled) {
            0
        } else if (won) {
            28 + pCrowns * 2
        } else if (draw) {
            0
        } else {
            -(22 - pCrowns * 2).coerceAtLeast(8).coerceAtMost(playerTrophies)
        }

        val goldEarned = if (won) 45 + pCrowns * 12 else if (draw) 20 else 10
        val xpEarned = if (won) 25 + pCrowns * 5 else 12
        val chest = if (won && mode != BattleMode.TRAINING) {
            val roll = Random.nextInt(100)
            when {
                roll < 50 -> ChestType.SILVER
                roll < 80 -> ChestType.GOLD
                roll < 93 -> ChestType.GIANT
                roll < 98 -> ChestType.MAGICAL
                else -> ChestType.LEGENDARY
            }
        } else null

        val cardStatsList = statsMap.values
            .filter { it.timesPlayed > 0 || it.damageDealt > 0 }
            .map { MatchCardStat(it.cardId, it.timesPlayed, it.damageDealt, it.towerDamage, it.kills, it.elixirSpent) }

        resultSummary = BattleResultSummary(
            playerCrowns = pCrowns,
            enemyCrowns = eCrowns,
            won = won,
            draw = draw,
            trophiesDelta = trophiesDelta,
            goldEarned = goldEarned,
            xpEarned = xpEarned,
            chestAwarded = chest,
            durationSec = elapsedSec.roundToInt(),
            opponentName = enemyName,
            cardStats = cardStatsList
        )
    }

    fun snapshot(): BattleSnapshot {
        val activeChamp = entities.firstOrNull {
            it.team == TeamSide.PLAYER && it.isAlive &&
                (it.abilityType == AbilityType.CHAMPION_SUMMON ||
                    it.abilityType == AbilityType.CHAMPION_CHAIN_DASH ||
                    it.abilityType == AbilityType.CHAMPION_CLOAK_HASTE)
        }
        val evoReadySet = handCards.filter { cid ->
            val spec = RoyaleCardCatalog.get(cid)
            spec?.evolution != null && cid in playerEvoSlotIds && (cardCycleCounts[cid] ?: 0) >= spec.evolution.cyclesRequired
        }.toSet()

        return BattleSnapshot(
            arena = arena,
            mode = mode,
            playerName = playerName,
            playerLevel = playerLevel,
            enemyName = enemyName,
            enemyLevel = enemyLevel,
            timeRemainingSec = timeRemainingSec,
            isOvertime = isOvertime,
            elixirMultiplier = currentElixirMultiplier(),
            playerElixir = playerElixir,
            enemyElixir = enemyElixir,
            handCardIds = handCards.toList(),
            nextCardId = nextCardId,
            evolvedReadyCardIds = evoReadySet,
            cardCycleCounts = cardCycleCounts.toMap(),
            lastPlayedCardId = lastPlayedCardId,
            lastPlayedCardCost = lastPlayedCardCost,
            towers = towers.map { it.copy() },
            entities = entities.map { it.copy() },
            projectiles = projectiles.map { it.copy() },
            spellEffects = spellEffects.map { it.copy() },
            floatingTexts = floatingTexts.map { it.copy() },
            emotePopup = currentEmote?.copy(),
            activeChampion = activeChamp?.copy(),
            playerCrowns = countCrowns(TeamSide.PLAYER),
            enemyCrowns = countCrowns(TeamSide.ENEMY),
            isFinished = isFinished,
            result = resultSummary
        )
    }
}
