package com.example.royale.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.royale.ArenaConfig
import com.example.royale.CardCategory
import com.example.royale.CardRarity
import com.example.royale.CardSpec
import com.example.royale.ChestType
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun CardTile(
    spec: CardSpec,
    level: Int,
    copies: Int? = null,
    isLocked: Boolean = false,
    isEvolvedUnlocked: Boolean = false,
    isEvoSlotActive: Boolean = false,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val rarityColor = Color(spec.rarity.colorHex)
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(spec.primaryColorHex).copy(alpha = if (isLocked) 0.22f else 0.45f),
            Color(spec.secondaryColorHex).copy(alpha = if (isLocked) 0.55f else 0.95f)
        )
    )
    val borderColor = if (isEvoSlotActive) Color(0xFFD946EF) else rarityColor

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(if (isEvoSlotActive) 2.dp else 1.5.dp, borderColor.copy(alpha = if (isLocked) 0.45f else 1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(if (compact) 4.dp else 6.dp)
        ) {
            // Custom vector card illustration
            CardIllustration(
                spec = spec,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (compact) 10.dp else 14.dp, bottom = if (compact) 16.dp else 22.dp)
            )

            // Elixir droplet top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(if (compact) 18.dp else 22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC026D3))
                    .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = spec.elixirCost.toString(),
                    color = Color.White,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = if (compact) 10.sp else 11.sp
                )
            }

            // Evolution diamond top-right if card has evolution
            if (spec.evolution != null && (isEvolvedUnlocked || isEvoSlotActive)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (compact) 14.dp else 18.dp)
                        .clip(CircleShape)
                        .background(if (isEvoSlotActive) Color(0xFFD946EF) else Color(0xFF7E22CE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "◆",
                        color = Color.White,
                        fontSize = if (compact) 8.sp else 10.sp
                    )
                }
            }

            // Bottom label & level
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = spec.name,
                    color = if (isLocked) Color.White.copy(alpha = 0.55f) else Color.White,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 9.sp else 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (!compact) {
                    val req = spec.upgradeCardRequirement(level)
                    val subtitle = when {
                        isLocked -> "Nv.Min ${spec.rarity.startingLevel}"
                        copies != null && level < spec.rarity.maxLevel -> "Nv.$level • $copies/$req"
                        else -> "Nv.$level MÁX"
                    }
                    Text(
                        text = subtitle,
                        color = if (copies != null && req > 0 && copies >= req) Color(0xFF4ADE80) else rarityColor,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Nv.$level",
                        color = rarityColor,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CardIllustration(spec: CardSpec, modifier: Modifier = Modifier) {
    val primary = Color(spec.primaryColorHex)
    val secondary = Color(spec.secondaryColorHex)
    val accent = Color(spec.rarity.colorHex)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.36f

        // Background aura glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 1.45f
            ),
            radius = r * 1.45f,
            center = Offset(cx, cy)
        )

        when (spec.category) {
            CardCategory.TROOP -> {
                if (spec.isFlying) {
                    // Wings silhouette
                    val wingPath = Path().apply {
                        moveTo(cx, cy)
                        lineTo(cx - r * 1.25f, cy - r * 0.65f)
                        lineTo(cx - r * 0.85f, cy + r * 0.25f)
                        close()
                        moveTo(cx, cy)
                        lineTo(cx + r * 1.25f, cy - r * 0.65f)
                        lineTo(cx + r * 0.85f, cy + r * 0.25f)
                        close()
                    }
                    drawPath(wingPath, color = primary.copy(alpha = 0.88f))
                }

                // Body/Armor core
                drawCircle(color = secondary, radius = r * 0.78f, center = Offset(cx, cy + r * 0.1f))
                drawCircle(color = primary, radius = r * 0.55f, center = Offset(cx, cy - r * 0.15f))
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = r * 0.56f,
                    center = Offset(cx, cy - r * 0.15f),
                    style = Stroke(width = 2.5f)
                )

                // Visor / Eyes / Crown detail
                if (spec.rarity == CardRarity.CHAMPION) {
                    val crown = Path().apply {
                        moveTo(cx - r * 0.45f, cy - r * 0.55f)
                        lineTo(cx - r * 0.35f, cy - r * 1.0f)
                        lineTo(cx - r * 0.1f, cy - r * 0.72f)
                        lineTo(cx, cy - r * 1.08f)
                        lineTo(cx + r * 0.1f, cy - r * 0.72f)
                        lineTo(cx + r * 0.35f, cy - r * 1.0f)
                        lineTo(cx + r * 0.45f, cy - r * 0.55f)
                        close()
                    }
                    drawPath(crown, color = Color(0xFFFACC15))
                } else {
                    drawLine(
                        color = Color(0xFF0F172A),
                        start = Offset(cx - r * 0.28f, cy - r * 0.15f),
                        end = Offset(cx + r * 0.28f, cy - r * 0.15f),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }

                // Weapon emblem based on attack range / splash
                if (spec.attackRange > 2.5f) {
                    // Bow / Staff arc
                    drawArc(
                        color = accent,
                        startAngle = -55f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(cx + r * 0.15f, cy - r * 0.7f),
                        size = Size(r * 0.9f, r * 1.3f),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                } else {
                    // Sword / Axe
                    drawLine(
                        color = Color(0xFFF8FAFC),
                        start = Offset(cx + r * 0.45f, cy + r * 0.5f),
                        end = Offset(cx + r * 0.95f, cy - r * 0.7f),
                        strokeWidth = 4.5f,
                        cap = StrokeCap.Round
                    )
                }

                if (spec.spawnCount > 1) {
                    drawCircle(color = primary.copy(alpha = 0.75f), radius = r * 0.26f, center = Offset(cx - r * 0.85f, cy + r * 0.5f))
                    drawCircle(color = primary.copy(alpha = 0.75f), radius = r * 0.26f, center = Offset(cx + r * 0.85f, cy + r * 0.5f))
                }
            }
            CardCategory.BUILDING -> {
                // Fortified stone/metal base + turret barrel
                drawRoundRect(
                    color = secondary,
                    topLeft = Offset(cx - r * 0.85f, cy - r * 0.35f),
                    size = Size(r * 1.7f, r * 1.25f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(cx - r * 0.65f, cy - r * 0.75f),
                    size = Size(r * 1.3f, r * 0.8f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawLine(
                    color = accent,
                    start = Offset(cx, cy - r * 0.45f),
                    end = Offset(cx + r * 0.8f, cy - r * 0.95f),
                    strokeWidth = 7f,
                    cap = StrokeCap.Round
                )
            }
            CardCategory.SPELL -> {
                // Concentric arcane shockwave & core rune
                drawCircle(
                    color = primary.copy(alpha = 0.35f),
                    radius = r * 1.05f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f)
                )
                drawCircle(color = primary, radius = r * 0.62f, center = Offset(cx, cy))
                val diamond = Path().apply {
                    moveTo(cx, cy - r * 0.9f)
                    lineTo(cx + r * 0.6f, cy)
                    lineTo(cx, cy + r * 0.9f)
                    lineTo(cx - r * 0.6f, cy)
                    close()
                }
                drawPath(diamond, color = Color.White.copy(alpha = 0.9f), style = Stroke(width = 3f))
            }
        }
    }
}

@Composable
fun ChestIllustration(type: ChestType, modifier: Modifier = Modifier) {
    val bodyColor = Color(type.colorHex)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Lid
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.92f),
            topLeft = Offset(w * 0.14f, h * 0.16f),
            size = Size(w * 0.72f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )
        // Base
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.72f),
            topLeft = Offset(w * 0.12f, h * 0.46f),
            size = Size(w * 0.76f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        // Gold bands
        drawRect(
            color = Color(0xFFFACC15),
            topLeft = Offset(w * 0.24f, h * 0.18f),
            size = Size(w * 0.09f, h * 0.66f)
        )
        drawRect(
            color = Color(0xFFFACC15),
            topLeft = Offset(w * 0.67f, h * 0.18f),
            size = Size(w * 0.09f, h * 0.66f)
        )
        // Lock clasp
        drawRoundRect(
            color = Color(0xFFFDE047),
            topLeft = Offset(w * 0.43f, h * 0.40f),
            size = Size(w * 0.14f, h * 0.18f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

@Composable
fun ArenaPreviewBanner(
    arena: ArenaConfig,
    trophies: Int,
    towerSkinName: String,
    kingSkinName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(arena.groundColorHex),
        border = BorderStroke(1.5.dp, Color(arena.accentColorHex))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Checkerboard turf perspective hint
                val cols = 8
                val rows = 5
                val cw = w / cols
                val ch = h / rows
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if ((r + c) % 2 == 0) {
                            drawRect(
                                color = Color(arena.altTileColorHex).copy(alpha = 0.65f),
                                topLeft = Offset(c * cw, r * ch),
                                size = Size(cw, ch)
                            )
                        }
                    }
                }

                // River & Bridges
                drawRect(
                    color = Color(arena.riverColorHex).copy(alpha = 0.8f),
                    topLeft = Offset(0f, h * 0.43f),
                    size = Size(w, h * 0.12f)
                )
                drawRoundRect(
                    color = Color(arena.bridgeColorHex),
                    topLeft = Offset(w * 0.17f, h * 0.40f),
                    size = Size(w * 0.14f, h * 0.18f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(arena.bridgeColorHex),
                    topLeft = Offset(w * 0.69f, h * 0.40f),
                    size = Size(w * 0.14f, h * 0.18f),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Player Princess Towers & King Tower silhouette at bottom
                val towerBase = Color(0xFF2563EB)
                val gold = Color(0xFFFACC15)
                // Left princess
                drawRoundRect(towerBase, Offset(w * 0.19f, h * 0.65f), Size(w * 0.10f, h * 0.20f), CornerRadius(6f, 6f))
                drawCircle(gold, radius = w * 0.025f, center = Offset(w * 0.24f, h * 0.63f))
                // Right princess
                drawRoundRect(towerBase, Offset(w * 0.71f, h * 0.65f), Size(w * 0.10f, h * 0.20f), CornerRadius(6f, 6f))
                drawCircle(gold, radius = w * 0.025f, center = Offset(w * 0.76f, h * 0.63f))
                // King Tower
                drawRoundRect(Color(0xFF1D4ED8), Offset(w * 0.42f, h * 0.60f), Size(w * 0.16f, h * 0.28f), CornerRadius(8f, 8f))
                val crown = Path().apply {
                    moveTo(w * 0.45f, h * 0.60f)
                    lineTo(w * 0.46f, h * 0.50f)
                    lineTo(w * 0.50f, h * 0.56f)
                    lineTo(w * 0.54f, h * 0.50f)
                    lineTo(w * 0.55f, h * 0.60f)
                    close()
                }
                drawPath(crown, color = gold)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "ARENA ${arena.index + 1} • ${arena.name.uppercase()}",
                        color = Color(arena.accentColorHex),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = arena.ambientDesc,
                    color = Color.White.copy(alpha = 0.9f),
                    fontFamily = SpaceGroteskFamily,
                    fontSize = 11.sp
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "🏆 $trophies",
                    color = Color(0xFFFACC15),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "$kingSkinName • $towerSkinName",
                    color = Color(0xFFE2E8F0),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
