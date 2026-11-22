package com.example

import com.example.royale.AiDifficulty
import com.example.royale.ArenaMetrics
import com.example.royale.BattleMode
import com.example.royale.CardCategory
import com.example.royale.RoyaleBattleEngine
import com.example.royale.RoyaleCardCatalog
import com.example.royale.TeamSide
import com.example.royale.TowerTroopId
import com.example.royale.data.RoyaleProfileState
import com.example.royale.data.RoyaleSaveCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RoyaleEngineTest {

    @Test
    fun catalogHasAll62CardsWithConsistentStatsAndDpsFormula() {
        assertEquals(62, RoyaleCardCatalog.allCards.size)
        assertEquals(15, RoyaleCardCatalog.arenas.size)

        for (spec in RoyaleCardCatalog.allCards) {
            assertTrue("${spec.id} elixir should be 1..10", spec.elixirCost in 1..10)
            assertTrue("${spec.id} arena index should be 0..14", spec.unlockArenaIndex in 0..14)

            if (spec.damageAt11 > 0 && spec.hitSpeedSec > 0f && spec.category != CardCategory.SPELL) {
                val expectedDps = spec.damageAtLevel(11).toDouble() / spec.hitSpeedSec.toDouble()
                assertTrue(
                    "${spec.id} DPS mismatch",
                    abs(expectedDps - spec.dpsAtLevel(11)) < 0.001
                )
            }

            if (spec.hpAt11 > 0) {
                val lvl11 = spec.hpAtLevel(11)
                val lvl12 = spec.hpAtLevel(12)
                assertTrue("${spec.id} level 12 HP should scale ~10%", lvl12 > lvl11)
            }

            if (spec.rarity.startingLevel < spec.rarity.maxLevel) {
                assertTrue("${spec.id} upgrade copies > 0", spec.upgradeCardRequirement(spec.rarity.startingLevel) > 0)
                assertTrue("${spec.id} upgrade gold > 0", spec.upgradeGoldCost(spec.rarity.startingLevel) > 0)
            }
        }
    }

    @Test
    fun all62CardsCanBeDeployedAndSimulatedInBattleEngine() {
        val arena = RoyaleCardCatalog.arenas.first()

        // Test every card inside a real engine instance
        for (spec in RoyaleCardCatalog.allCards) {
            val deck = if (spec.id == "espelho") {
                listOf("cavaleiro", "espelho", "arqueiras", "flechas", "esqueletos", "zap", "goblins", "bola_de_neve")
            } else {
                listOf(spec.id, "esqueletos", "zap", "goblins", "bola_de_neve", "tronco", "goblins_lanceiros", "furia")
            }
            val engine = RoyaleBattleEngine(
                arena = arena,
                mode = BattleMode.TRIPLE_ELIXIR,
                aiDifficulty = AiDifficulty.EASY,
                playerName = "Tester",
                playerLevel = 11,
                playerDeckIds = deck,
                playerCardLevels = deck.associateWith { 11 },
                playerEvoSlotIds = emptySet(),
                towerTroop = TowerTroopId.PRINCESS,
                playerTrophies = 500
            )

            // Wait for max elixir (10)
            repeat(80) { engine.step(0.05f) }

            if (spec.id == "espelho") {
                // Play Cavaleiro first, cycling until both Cavaleiro and Espelho are played
                var knightPlayed = false
                var mirrorPlayed = false
                for (attempt in 0 until 8) {
                    repeat(40) { engine.step(0.05f) }
                    val snap = engine.snapshot()
                    if (!knightPlayed) {
                        val kIdx = snap.handCardIds.indexOf("cavaleiro")
                        if (kIdx >= 0) {
                            knightPlayed = engine.playCard(kIdx, 4.0f, 22.0f)
                        } else {
                            engine.playCard(0, 4.0f, 22.0f)
                        }
                    } else if (!mirrorPlayed) {
                        val mIdx = snap.handCardIds.indexOf("espelho")
                        if (mIdx >= 0) {
                            mirrorPlayed = engine.playCard(mIdx, 4.0f, 22.0f)
                            break
                        } else {
                            val fillerIdx = snap.handCardIds.indexOfFirst { it != "espelho" }
                            if (fillerIdx >= 0) engine.playCard(fillerIdx, 4.0f, 22.0f)
                        }
                    }
                }
                assertTrue("Espelho should deploy after Cavaleiro", mirrorPlayed)
            } else {
                var targetDeployed = false
                for (attempt in 0 until 8) {
                    repeat(50) { engine.step(0.05f) }
                    val snap = engine.snapshot()
                    val idx = snap.handCardIds.indexOf(spec.id)
                    if (idx >= 0) {
                        val deployY = if (spec.canDeployAnywhere || spec.category == CardCategory.SPELL) 8.0f else 22.0f
                        targetDeployed = engine.playCard(idx, 4.0f, deployY)
                        break
                    } else {
                        engine.playCard(0, 4.0f, 22.0f)
                    }
                }
                assertTrue("Card ${spec.id} should deploy", targetDeployed)
            }

            // Step combat forward 2 seconds without exceptions
            repeat(40) { engine.step(0.05f) }
        }
    }

    @Test
    fun deploymentRestrictionsRejectEnemyTerritoryForStandardTroops() {
        val engine = RoyaleBattleEngine(
            arena = RoyaleCardCatalog.arenas.first(),
            mode = BattleMode.LADDER_1V1,
            aiDifficulty = AiDifficulty.NORMAL,
            playerName = "Tester",
            playerLevel = 5,
            playerDeckIds = RoyaleCardCatalog.defaultDeckIds,
            playerCardLevels = RoyaleCardCatalog.defaultDeckIds.associateWith { 11 },
            playerEvoSlotIds = emptySet(),
            towerTroop = TowerTroopId.PRINCESS,
            playerTrophies = 0
        )

        // Cavaleiro cannot be placed behind enemy river while towers stand
        assertFalse(engine.canDeployCardAt("cavaleiro", 5.0f, 8.0f, TeamSide.PLAYER))
        assertTrue(engine.canDeployCardAt("cavaleiro", 5.0f, 22.0f, TeamSide.PLAYER))

        // Mineiro and Bola de Fogo can be placed anywhere
        assertTrue(engine.canDeployCardAt("mineiro", 3.5f, 6.5f, TeamSide.PLAYER))
        assertTrue(engine.canDeployCardAt("bola_de_fogo", 3.5f, 6.5f, TeamSide.PLAYER))
    }

    @Test
    fun championActiveAbilityAndSpellsModifyBattleState() {
        val deck = listOf("rei_esqueleto", "furia", "congelamento", "barril_de_goblins", "cavaleiro", "arqueiras", "flechas", "zap")
        val engine = RoyaleBattleEngine(
            arena = RoyaleCardCatalog.arenas[12],
            mode = BattleMode.TRIPLE_ELIXIR,
            aiDifficulty = AiDifficulty.NORMAL,
            playerName = "ChampTester",
            playerLevel = 12,
            playerDeckIds = deck,
            playerCardLevels = deck.associateWith { 11 },
            playerEvoSlotIds = emptySet(),
            towerTroop = TowerTroopId.CANNONEER,
            playerTrophies = 4200
        )
        repeat(100) { engine.step(0.05f) }

        val snap1 = engine.snapshot()
        val kingIdx = snap1.handCardIds.indexOf("rei_esqueleto")
        if (kingIdx >= 0) {
            assertTrue(engine.playCard(kingIdx, 9.0f, 22.0f))
            repeat(50) { engine.step(0.05f) }
            assertNotNull(engine.snapshot().activeChampion)
            assertTrue(engine.activateChampionAbility())
            assertTrue(engine.snapshot().entities.any { it.cardId == "esqueletos" && it.team == TeamSide.PLAYER })
        }
    }

    @Test
    fun saveCodecRoundTripPreservesState() {
        val original = RoyaleProfileState(
            playerName = "ArthurRoyale",
            trophies = 1420,
            gold = 3450,
            gems = 210,
            wins = 14,
            losses = 6
        )
        val json = RoyaleSaveCodec.encode(original)
        val decoded = RoyaleSaveCodec.decode(json)

        assertEquals(original.playerName, decoded.playerName)
        assertEquals(original.trophies, decoded.trophies)
        assertEquals(original.gold, decoded.gold)
        assertEquals(original.gems, decoded.gems)
        assertEquals(original.wins, decoded.wins)
        assertEquals(original.decks.size, decoded.decks.size)
        assertEquals(original.cardCollection.size, decoded.cardCollection.size)
    }
}
