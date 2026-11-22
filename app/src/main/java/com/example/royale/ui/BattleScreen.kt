package com.example.royale.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.royale.ArenaMetrics
import com.example.royale.BattleSnapshot
import com.example.royale.CardCategory
import com.example.royale.EmoteDef
import com.example.royale.RoyaleCardCatalog
import com.example.royale.RoyaleSettings
import com.example.royale.TeamSide
import com.example.royale.TowerKind
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BattleScreen(
    snapshot: BattleSnapshot,
    isPaused: Boolean,
    settings: RoyaleSettings,
    equippedEmotes: List<EmoteDef>,
    onPlayCard: (handIndex: Int, arenaX: Float, arenaY: Float) -> Boolean,
    onCanDeployAt: (cardId: String, arenaX: Float, arenaY: Float) -> Boolean,
    onActivateChampion: () -> Unit,
    onSendEmote: (EmoteDef) -> Unit,
    onTogglePause: () -> Unit,
    onToggleMute: () -> Unit,
    onForfeit: () -> Unit,
    onPlayAgain: () -> Unit,
    onExitBattle: () -> Unit
) {
    var selectedHandIdx by remember { mutableIntStateOf(-1) }
    var showForfeitConfirm by remember { mutableStateOf(false) }
    var showEmoteDrawer by remember { mutableStateOf(false) }

    var fieldRootPos by remember { mutableStateOf(Offset.Zero) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    var dragArenaPreview by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    BackHandler {
        if (snapshot.isFinished) {
            onExitBattle()
        } else {
            showForfeitConfirm = true
        }
    }

    fun screenRootToArena(rootPos: Offset): Pair<Float, Float>? {
        if (fieldSize.width <= 0 || fieldSize.height <= 0) return null
        val localX = rootPos.x - fieldRootPos.x
        val localY = rootPos.y - fieldRootPos.y
        if (localX < 0f || localX > fieldSize.width || localY < 0f || localY > fieldSize.height) return null
        val ax = (localX / fieldSize.width.toFloat()) * ArenaMetrics.WIDTH
        val ay = (localY / fieldSize.height.toFloat()) * ArenaMetrics.HEIGHT
        return ax to ay
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070D))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP BATTLE HUD
            BattleTopHud(
                snapshot = snapshot,
                isPaused = isPaused,
                sfxEnabled = settings.sfxEnabled,
                onTogglePause = onTogglePause,
                onToggleMute = onToggleMute,
                onRequestForfeit = { showForfeitConfirm = true }
            )

            // 2. LIVE ARENA CANVAS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, Color(snapshot.arena.accentColorHex).copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .onGloballyPositioned { coords ->
                        fieldRootPos = coords.positionInRoot()
                        fieldSize = coords.size
                    }
                    .pointerInput(selectedHandIdx, snapshot.handCardIds) {
                        detectTapGestures { offset ->
                            if (selectedHandIdx in snapshot.handCardIds.indices && fieldSize.width > 0 && fieldSize.height > 0) {
                                val ax = (offset.x / fieldSize.width.toFloat()) * ArenaMetrics.WIDTH
                                val ay = (offset.y / fieldSize.height.toFloat()) * ArenaMetrics.HEIGHT
                                if (onPlayCard(selectedHandIdx, ax, ay)) {
                                    selectedHandIdx = -1
                                }
                            }
                        }
                    }
            ) {
                val activeCardId = snapshot.handCardIds.getOrNull(selectedHandIdx)?.let {
                    if (it == "espelho") snapshot.lastPlayedCardId else it
                }
                BattlefieldCanvas(
                    snapshot = snapshot,
                    selectedCardId = activeCardId,
                    dragPreview = dragArenaPreview,
                    canDeploy = { cid, x, y -> onCanDeployAt(cid, x, y) }
                )

                // Emote Popup overlay
                snapshot.emotePopup?.let { pop ->
                    Surface(
                        modifier = Modifier
                            .align(if (pop.team == TeamSide.PLAYER) Alignment.BottomEnd else Alignment.TopEnd)
                            .padding(12.dp),
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(pop.icon, fontSize = 20.sp)
                            Text(
                                text = pop.phrase,
                                color = Color(0xFF0F172A),
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Crown counter on side of arena
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("👑 ${snapshot.enemyCrowns}", color = Color(0xFFF43F5E), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.25f), modifier = Modifier.width(24.dp))
                        Text("👑 ${snapshot.playerCrowns}", color = Color(0xFF38BDF8), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }

                // Paused overlay
                if (isPaused && !snapshot.isFinished) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF38BDF8))
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("BATALHA PAUSADA", color = Color.White, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Button(onClick = onTogglePause) {
                                    Text("CONTINUAR")
                                }
                            }
                        }
                    }
                }
            }

            // 3. BOTTOM CONTROLS: ELIXIR + HAND + NEXT + CHAMPION + EMOTE
            BattleBottomHud(
                snapshot = snapshot,
                selectedHandIdx = selectedHandIdx,
                showEmoteDrawer = showEmoteDrawer,
                equippedEmotes = equippedEmotes,
                onSelectCard = { idx ->
                    selectedHandIdx = if (selectedHandIdx == idx) -1 else idx
                },
                onDragCardStart = { idx ->
                    selectedHandIdx = idx
                },
                onDragCardMove = { rootOffset ->
                    dragArenaPreview = screenRootToArena(rootOffset)
                },
                onDragCardEnd = { idx, rootOffset ->
                    val coords = screenRootToArena(rootOffset)
                    if (coords != null && onPlayCard(idx, coords.first, coords.second)) {
                        selectedHandIdx = -1
                    }
                    dragArenaPreview = null
                },
                onActivateChampion = onActivateChampion,
                onToggleEmoteDrawer = { showEmoteDrawer = !showEmoteDrawer },
                onPickEmote = { emote ->
                    onSendEmote(emote)
                    showEmoteDrawer = false
                }
            )
        }

        // 4. MATCH RESULT OVERLAY
        if (snapshot.isFinished && snapshot.result != null) {
            BattleResultOverlay(
                snapshot = snapshot,
                onPlayAgain = onPlayAgain,
                onBackHome = onExitBattle
            )
        }
    }

    if (showForfeitConfirm) {
        AlertDialog(
            onDismissRequest = { showForfeitConfirm = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Abandonar Batalha?", color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Se você desistir agora, a partida será encerrada como derrota para o adversário.",
                    color = Color(0xFFCBD5E1),
                    fontFamily = SpaceGroteskFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForfeitConfirm = false
                        onForfeit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("DESISTIR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForfeitConfirm = false }) {
                    Text("CONTINUAR LUTA", color = Color(0xFF38BDF8))
                }
            }
        )
    }
}

@Composable
private fun BattleTopHud(
    snapshot: BattleSnapshot,
    isPaused: Boolean,
    sfxEnabled: Boolean,
    onTogglePause: () -> Unit,
    onToggleMute: () -> Unit,
    onRequestForfeit: () -> Unit
) {
    val totalSec = max(0, snapshot.timeRemainingSec.roundToInt())
    val minPart = totalSec / 60
    val secPart = totalSec % 60
    val timeText = "%d:%02d".format(minPart, secPart)

    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Opponent Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9F1239))
                        .border(1.5.dp, Color(0xFFF43F5E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nv${snapshot.enemyLevel}", color = Color.White, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
                Column {
                    Text(
                        text = snapshot.enemyName,
                        color = Color.White,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = snapshot.mode.title,
                        color = Color(0xFF94A3B8),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }

            // Timer & Multiplier
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = if (snapshot.isOvertime) Color(0xFF7F1D1D) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (snapshot.isOvertime) Color(0xFFF43F5E) else Color(0xFF334155))
                ) {
                    Text(
                        text = if (snapshot.isOvertime) "PRORR. $timeText" else timeText,
                        color = if (snapshot.timeRemainingSec <= 15f) Color(0xFFF43F5E) else Color.White,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (snapshot.elixirMultiplier > 1.0f) {
                    Text(
                        text = "⚡ ELIXIR ${snapshot.elixirMultiplier.toInt()}x",
                        color = Color(0xFFE879F9),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }
            }

            // Controls (Mute, Pause, Forfeit)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onToggleMute, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = if (sfxEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Áudio",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onTogglePause, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = "Pausar",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRequestForfeit,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("battle_forfeit_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = "Desistir",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BattlefieldCanvas(
    snapshot: BattleSnapshot,
    selectedCardId: String?,
    dragPreview: Pair<Float, Float>?,
    canDeploy: (String, Float, Float) -> Boolean
) {
    val arena = snapshot.arena
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val sx = w / ArenaMetrics.WIDTH
        val sy = h / ArenaMetrics.HEIGHT

        fun toScreen(ax: Float, ay: Float): Offset = Offset(ax * sx, ay * sy)

        // 1. Arena checkerboard tiles
        val cols = 18
        val rows = 32
        val tileW = w / cols
        val tileH = h / rows
        drawRect(Color(arena.groundColorHex), Offset.Zero, size)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if ((r + c) % 2 == 0) {
                    drawRect(
                        color = Color(arena.altTileColorHex).copy(alpha = 0.55f),
                        topLeft = Offset(c * tileW, r * tileH),
                        size = Size(tileW + 0.5f, tileH + 0.5f)
                    )
                }
            }
        }

        // Subtle lane path markers
        drawRect(
            color = Color.White.copy(alpha = 0.05f),
            topLeft = Offset((ArenaMetrics.LEFT_BRIDGE_X - 1.1f) * sx, 4.5f * sy),
            size = Size(2.2f * sx, 23f * sy)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.05f),
            topLeft = Offset((ArenaMetrics.RIGHT_BRIDGE_X - 1.1f) * sx, 4.5f * sy),
            size = Size(2.2f * sx, 23f * sy)
        )

        // 2. River
        drawRect(
            color = Color(arena.riverColorHex),
            topLeft = Offset(0f, (ArenaMetrics.RIVER_Y - ArenaMetrics.RIVER_HALF_DEPTH) * sy),
            size = Size(w, ArenaMetrics.RIVER_HALF_DEPTH * 2f * sy)
        )
        // Bridges
        val bridgeHalfW = ArenaMetrics.BRIDGE_HALF_WIDTH * sx
        val bridgeH = ArenaMetrics.RIVER_HALF_DEPTH * 2.4f * sy
        val bridgeTop = (ArenaMetrics.RIVER_Y - ArenaMetrics.RIVER_HALF_DEPTH * 1.2f) * sy
        for (bx in listOf(ArenaMetrics.LEFT_BRIDGE_X, ArenaMetrics.RIGHT_BRIDGE_X)) {
            drawRoundRect(
                color = Color(arena.bridgeColorHex),
                topLeft = Offset(bx * sx - bridgeHalfW, bridgeTop),
                size = Size(bridgeHalfW * 2f, bridgeH),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color(0xFF0F172A).copy(alpha = 0.45f),
                topLeft = Offset(bx * sx - bridgeHalfW, bridgeTop),
                size = Size(bridgeHalfW * 2f, bridgeH),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2.5f)
            )
        }

        // 3. Placement zone highlight when a card is selected
        if (selectedCardId != null) {
            val spec = RoyaleCardCatalog.get(selectedCardId)
            if (spec != null) {
                val anywhere = spec.canDeployAnywhere || spec.category == CardCategory.SPELL
                if (!anywhere) {
                    // Valid lower half highlight
                    drawRect(
                        color = Color(0xFF22C55E).copy(alpha = 0.14f),
                        topLeft = Offset(0f, (ArenaMetrics.RIVER_Y + 0.9f) * sy),
                        size = Size(w, h - (ArenaMetrics.RIVER_Y + 0.9f) * sy)
                    )
                    // Enemy restriction zone
                    drawRect(
                        color = Color(0xFFEF4444).copy(alpha = 0.14f),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, (ArenaMetrics.RIVER_Y + 0.9f) * sy)
                    )
                } else {
                    drawRect(
                        color = Color(0xFF38BDF8).copy(alpha = 0.10f),
                        topLeft = Offset.Zero,
                        size = size
                    )
                }
            }
        }

        // 4. Ongoing Spell Effects
        snapshot.spellEffects.forEach { fx ->
            val p = toScreen(fx.x, fx.y)
            val radPx = fx.radius * sx
            drawCircle(
                color = Color(fx.colorHex).copy(alpha = if (fx.isBurstVisual) 0.45f else 0.25f),
                radius = radPx,
                center = p
            )
            drawCircle(
                color = Color(fx.colorHex).copy(alpha = 0.8f),
                radius = radPx,
                center = p,
                style = Stroke(width = 3f)
            )
        }

        // 5. Towers
        snapshot.towers.forEach { t ->
            val center = toScreen(t.x, t.y)
            val radPx = t.radius * sx
            if (!t.isAlive) {
                // Rubble crater
                drawCircle(color = Color(0xFF1E293B).copy(alpha = 0.85f), radius = radPx * 0.9f, center = center)
                drawCircle(color = Color(0xFF475569), radius = radPx * 0.9f, center = center, style = Stroke(width = 2f))
                return@forEach
            }
            val baseColor = when {
                t.hitFlashTimer > 0f -> Color.White
                t.freezeTimer > 0f -> Color(0xFF67E8F9)
                t.team == TeamSide.PLAYER -> Color(0xFF2563EB)
                else -> Color(0xFFDC2626)
            }
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(center.x - radPx, center.y - radPx),
                size = Size(radPx * 2f, radPx * 2f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = baseColor,
                topLeft = Offset(center.x - radPx * 0.86f, center.y - radPx * 0.86f),
                size = Size(radPx * 1.72f, radPx * 1.72f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            if (t.kind == TowerKind.KING) {
                val crownColor = if (t.isActive) Color(0xFFFACC15) else Color(0xFF94A3B8)
                drawCircle(color = crownColor, radius = radPx * 0.36f, center = center)
            } else {
                drawCircle(color = Color.White.copy(alpha = 0.85f), radius = radPx * 0.25f, center = center)
            }

            // Tower HP bar
            val barW = radPx * 2.2f
            val barH = 6f
            val hpFrac = (t.hp.toFloat() / t.maxHp.toFloat()).coerceIn(0f, 1f)
            val barY = if (t.team == TeamSide.PLAYER) center.y + radPx + 4f else center.y - radPx - 10f
            drawRect(Color.Black.copy(alpha = 0.8f), Offset(center.x - barW / 2f, barY), Size(barW, barH))
            drawRect(
                if (t.team == TeamSide.PLAYER) Color(0xFF38BDF8) else Color(0xFFF43F5E),
                Offset(center.x - barW / 2f, barY),
                Size(barW * hpFrac, barH)
            )
        }

        // 6. Units & Buildings
        snapshot.entities.forEach { u ->
            if (!u.isAlive) return@forEach
            val c = toScreen(u.x, u.y)
            val r = (u.radius * sx).coerceAtLeast(8f)

            // Flying shadow
            if (u.isFlying) {
                drawOval(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(c.x - r * 0.7f, c.y + r * 0.85f),
                    size = Size(r * 1.4f, r * 0.6f)
                )
            }

            val unitAlpha = when {
                u.isInvisible -> 0.35f
                u.isClone -> 0.65f
                u.isHiddenUnderground -> 0.30f
                else -> 1.0f
            }

            // Team ring
            val teamRing = if (u.team == TeamSide.PLAYER) Color(0xFF38BDF8) else Color(0xFFF43F5E)
            drawCircle(
                color = teamRing.copy(alpha = unitAlpha),
                radius = r * 1.15f,
                center = c,
                style = Stroke(width = if (u.isEvolved) 3.5f else 2.2f)
            )

            if (u.isEvolved) {
                drawCircle(color = Color(0xFFD946EF).copy(alpha = 0.35f), radius = r * 1.35f, center = c)
            }
            if (u.rageTimer > 0f) {
                drawCircle(color = Color(0xFFE879F9).copy(alpha = 0.35f), radius = r * 1.3f, center = c)
            }

            val bodyColor = when {
                u.hitFlashTimer > 0f -> Color.White
                u.freezeTimer > 0f -> Color(0xFF67E8F9)
                else -> Color(u.primaryColorHex)
            }

            if (u.category == CardCategory.BUILDING) {
                drawRoundRect(
                    color = bodyColor.copy(alpha = unitAlpha),
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(r * 2f, r * 2f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            } else {
                drawCircle(color = bodyColor.copy(alpha = unitAlpha), radius = r, center = c)
            }

            // Charge indicator
            if (u.isCharging) {
                drawCircle(color = Color(0xFFFACC15), radius = r * 1.32f, center = c, style = Stroke(width = 2.5f))
            }

            // HP + Shield bar
            val barW = max(r * 2.1f, 22f)
            val barH = 4.5f
            val hpFrac = (u.hp.toFloat() / max(1, u.maxHp).toFloat()).coerceIn(0f, 1f)
            drawRect(Color.Black.copy(alpha = 0.8f), Offset(c.x - barW / 2f, c.y - r - 8f), Size(barW, barH))
            drawRect(
                if (u.team == TeamSide.PLAYER) Color(0xFF38BDF8) else Color(0xFFF43F5E),
                Offset(c.x - barW / 2f, c.y - r - 8f),
                Size(barW * hpFrac, barH)
            )
            if (u.shieldHp > 0 && u.maxShieldHp > 0) {
                val shFrac = (u.shieldHp.toFloat() / u.maxShieldHp.toFloat()).coerceIn(0f, 1f)
                drawRect(Color(0xFFE2E8F0), Offset(c.x - barW / 2f, c.y - r - 12f), Size(barW * shFrac, 2.5f))
            }
        }

        // 7. Projectiles
        snapshot.projectiles.forEach { p ->
            val pos = toScreen(p.x, p.y)
            drawCircle(color = Color(p.colorHex), radius = 5f, center = pos)
            drawCircle(color = Color.White, radius = 2.5f, center = pos)
        }

        // 8. Drag placement reticle
        if (selectedCardId != null && dragPreview != null) {
            val (ax, ay) = dragPreview
            val valid = canDeploy(selectedCardId, ax, ay)
            val pos = toScreen(ax, ay)
            val spec = RoyaleCardCatalog.get(selectedCardId)
            val radPx = ((spec?.areaSplashRadius?.takeIf { it > 0f } ?: 1.4f) * sx)
            val color = if (valid) Color(0xFF4ADE80) else Color(0xFFF43F5E)
            drawCircle(color = color.copy(alpha = 0.25f), radius = radPx, center = pos)
            drawCircle(color = color, radius = radPx, center = pos, style = Stroke(width = 3f))
        }
    }
}

@Composable
private fun BattleBottomHud(
    snapshot: BattleSnapshot,
    selectedHandIdx: Int,
    showEmoteDrawer: Boolean,
    equippedEmotes: List<EmoteDef>,
    onSelectCard: (Int) -> Unit,
    onDragCardStart: (Int) -> Unit,
    onDragCardMove: (Offset) -> Unit,
    onDragCardEnd: (Int, Offset) -> Unit,
    onActivateChampion: () -> Unit,
    onToggleEmoteDrawer: () -> Unit,
    onPickEmote: (EmoteDef) -> Unit
) {
    Surface(
        color = Color(0xFF0B1120),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showEmoteDrawer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    equippedEmotes.forEach { em ->
                        Surface(
                            onClick = { onPickEmote(em) },
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(em.rarity.colorHex)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(em.iconTag, fontSize = 18.sp)
                                Text(em.phrase, color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Champion active skill + Emote trigger row
            val champ = snapshot.activeChampion
            val champSpec = champ?.let { RoyaleCardCatalog.get(it.cardId) }
            val champAbility = champSpec?.ability

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Emote button
                Surface(
                    onClick = onToggleEmoteDrawer,
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.EmojiEmotions, contentDescription = "Emotes", tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                        Text("Emote", color = Color.White, fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                    }
                }

                // Elixir Bar
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ELIXIR",
                            color = Color(0xFFE879F9),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${snapshot.playerElixir.toInt()} / 10",
                            color = Color.White,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (snapshot.playerElixir / 10f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = Color(0xFFD946EF),
                        trackColor = Color(0xFF1E1B4B)
                    )
                }

                // Champion ability button if deployed
                if (champ != null && champAbility != null) {
                    val ready = champ.championCooldown <= 0f && snapshot.playerElixir >= champAbility.activeElixirCost
                    Surface(
                        onClick = { if (ready) onActivateChampion() },
                        color = if (ready) Color(0xFFFACC15) else Color(0xFF334155),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("champion_ability_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = champAbility.name, tint = if (ready) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (champ.championCooldown > 0f) "${champ.championCooldown.roundToInt()}s" else "${champAbility.activeElixirCost}⚡",
                                color = if (ready) Color.Black else Color.White,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // 4 Hand Cards + Next Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Next card preview
                val nextSpec = RoyaleCardCatalog.get(snapshot.nextCardId)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(48.dp)
                ) {
                    Text("PRÓX.", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 8.sp)
                    if (nextSpec != null) {
                        CardTile(
                            spec = nextSpec,
                            level = 11,
                            compact = true,
                            modifier = Modifier
                                .size(width = 46.dp, height = 58.dp)
                                .alpha(0.8f)
                        )
                    }
                }

                // 4 Hand Slots (tap OR drag onto arena)
                snapshot.handCardIds.forEachIndexed { idx, cid ->
                    val spec = RoyaleCardCatalog.get(cid)
                    if (spec != null) {
                        val effectiveCost = if (cid == "espelho") {
                            if (snapshot.lastPlayedCardId != null) (snapshot.lastPlayedCardCost + 1).coerceAtMost(10) else 99
                        } else spec.elixirCost
                        val affordable = snapshot.playerElixir >= effectiveCost
                        val selected = selectedHandIdx == idx
                        val evoReady = cid in snapshot.evolvedReadyCardIds

                        var slotRootPos by remember { mutableStateOf(Offset.Zero) }
                        var dragCurrentRoot by remember { mutableStateOf(Offset.Zero) }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(86.dp)
                                .onGloballyPositioned { slotRootPos = it.positionInRoot() }
                                .pointerInput(idx, cid) {
                                    detectDragGestures(
                                        onDragStart = { startOffset ->
                                            onDragCardStart(idx)
                                            dragCurrentRoot = slotRootPos + startOffset
                                            onDragCardMove(dragCurrentRoot)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            dragCurrentRoot = slotRootPos + change.position
                                            onDragCardMove(dragCurrentRoot)
                                        },
                                        onDragEnd = {
                                            onDragCardEnd(idx, dragCurrentRoot)
                                        },
                                        onDragCancel = {
                                            onDragCardEnd(idx, Offset.Zero)
                                        }
                                    )
                                }
                                .clickable { onSelectCard(idx) }
                                .border(
                                    width = if (selected) 2.5.dp else 0.dp,
                                    color = if (selected) Color(0xFF4ADE80) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .testTag("battle_hand_card_$idx")
                        ) {
                            CardTile(
                                spec = spec.copy(elixirCost = if (effectiveCost < 20) effectiveCost else spec.elixirCost),
                                level = 11,
                                isEvoSlotActive = evoReady,
                                compact = true,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(if (affordable) 1f else 0.48f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleResultOverlay(
    snapshot: BattleSnapshot,
    onPlayAgain: () -> Unit,
    onBackHome: () -> Unit
) {
    val res = snapshot.result ?: return
    val bannerColor = when {
        res.won -> Color(0xFF10B981)
        res.draw -> Color(0xFFF59E0B)
        else -> Color(0xFFF43F5E)
    }
    val headline = when {
        res.won -> "VITÓRIA!"
        res.draw -> "EMPATE!"
        else -> "DERROTA"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, bannerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = headline,
                    color = bannerColor,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
                Text(
                    text = "${snapshot.playerName} ${res.playerCrowns} 👑  vs  👑 ${res.enemyCrowns} ${res.opponentName}",
                    color = Color.White,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                // Rewards row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val trophiesSign = if (res.trophiesDelta >= 0) "+${res.trophiesDelta}" else "${res.trophiesDelta}"
                    ResultStatPill("🏆 Troféus", trophiesSign, Color(0xFFFACC15), Modifier.weight(1f))
                    ResultStatPill("🪙 Ouro", "+${res.goldEarned}", Color(0xFFEAB308), Modifier.weight(1f))
                    ResultStatPill("⭐ XP", "+${res.xpEarned}", Color(0xFF38BDF8), Modifier.weight(1f))
                }

                if (res.chestAwarded != null) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(res.chestAwarded.colorHex)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ChestIllustration(res.chestAwarded, Modifier.size(38.dp))
                            Column {
                                Text("BAÚ CONQUISTADO!", color = Color(0xFF4ADE80), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text(res.chestAwarded.title, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Per-card match stats
                if (res.cardStats.isNotEmpty()) {
                    Text("DESEMPENHO DAS CARTAS", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(res.cardStats) { cs ->
                            val name = RoyaleCardCatalog.get(cs.cardId)?.name ?: cs.cardId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$name (${cs.timesPlayed}x)", color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 12.sp)
                                Text("Dano: ${cs.damageDealt} • Abates: ${cs.kills}", color = Color(0xFF38BDF8), fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackHome,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("VOLTAR", color = Color.White)
                    }
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15), contentColor = Color.Black),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("JOGAR DE NOVO", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatPill(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 9.sp)
            Text(value, color = accent, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}
