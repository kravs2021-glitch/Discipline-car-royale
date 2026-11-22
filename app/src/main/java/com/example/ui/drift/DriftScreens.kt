package com.example.ui.drift

import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.DriftUserPrefs
import com.example.drift.CarStyle
import com.example.drift.DRIFT_CAR_COLORS
import com.example.drift.DriftAudioEngine
import com.example.drift.DriftControlInput
import com.example.drift.DriftHudSnapshot
import com.example.drift.DriftRaceStatus
import com.example.drift.DriftRenderer3D
import com.example.drift.GhostFrame
import com.example.drift.TrackCatalog
import com.example.drift.TrackId
import com.example.ui.DriftSessionState
import com.example.ui.theme.AmberExempt
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonMuted
import com.example.ui.theme.CarbonSubtext
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonText
import com.example.ui.theme.DriftCrimson
import com.example.ui.theme.DriftGold
import com.example.ui.theme.DriftNitroSky
import com.example.ui.theme.EmeraldMogger
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoseMogged
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun DriftRootScreen(
    prefs: DriftUserPrefs,
    session: DriftSessionState,
    onUpdatePilotName: (String) -> Unit,
    onUpdateCarColor: (String) -> Unit,
    onUpdateCarStyle: (CarStyle) -> Unit,
    onUpdateTrack: (TrackId) -> Unit,
    onUpdateBotCount: (Int) -> Unit,
    onSubmitCheatCode: (String) -> Unit,
    onClearCheatError: () -> Unit,
    onStartFreeRoam: () -> Unit,
    onStartCompetitive: () -> Unit,
    onStartGhostMatch: () -> Unit,
    onFinishRace: (Int, List<GhostFrame>) -> Unit,
    onLeaveRaceToLobby: () -> Unit,
    onBackToExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (session.status) {
        DriftRaceStatus.LOBBY -> {
            BackHandler { onBackToExam() }
            DriftLobbyScreen(
                prefs = prefs,
                cheatError = session.cheatError,
                onUpdatePilotName = onUpdatePilotName,
                onUpdateCarColor = onUpdateCarColor,
                onUpdateCarStyle = onUpdateCarStyle,
                onUpdateTrack = onUpdateTrack,
                onUpdateBotCount = onUpdateBotCount,
                onSubmitCheatCode = onSubmitCheatCode,
                onClearCheatError = onClearCheatError,
                onStartFreeRoam = onStartFreeRoam,
                onStartCompetitive = onStartCompetitive,
                onBackToExam = onBackToExam,
                modifier = modifier
            )
        }
        DriftRaceStatus.COUNTDOWN, DriftRaceStatus.RACING -> {
            BackHandler { onLeaveRaceToLobby() }
            DriftRaceScreen(
                prefs = prefs,
                session = session,
                onChangeTrack = onUpdateTrack,
                onFinishRace = onFinishRace,
                onLeaveToLobby = onLeaveRaceToLobby,
                modifier = modifier
            )
        }
        DriftRaceStatus.FINISHED -> {
            BackHandler { onLeaveRaceToLobby() }
            DriftPodiumScreen(
                pilotName = prefs.pilotName,
                session = session,
                onBackToLobby = onLeaveRaceToLobby,
                onStartGhostMatch = onStartGhostMatch,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun DriftLobbyScreen(
    prefs: DriftUserPrefs,
    cheatError: String?,
    onUpdatePilotName: (String) -> Unit,
    onUpdateCarColor: (String) -> Unit,
    onUpdateCarStyle: (CarStyle) -> Unit,
    onUpdateTrack: (TrackId) -> Unit,
    onUpdateBotCount: (Int) -> Unit,
    onSubmitCheatCode: (String) -> Unit,
    onClearCheatError: () -> Unit,
    onStartFreeRoam: () -> Unit,
    onStartCompetitive: () -> Unit,
    onBackToExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTrack = TrackCatalog.get(prefs.selectedTrack)
    val availableTracks = remember(prefs.isCheatUnlocked) {
        if (prefs.isCheatUnlocked) {
            listOf(TrackId.JAPAN, TrackId.USA, TrackId.BRAZIL, TrackId.SCRAPYARD, TrackId.AIRPORT)
        } else {
            listOf(TrackId.JAPAN, TrackId.USA, TrackId.BRAZIL, TrackId.SCRAPYARD)
        }
    }
    val availableStyles = remember(prefs.isCheatUnlocked) {
        if (prefs.isCheatUnlocked) {
            listOf(CarStyle.COUPE, CarStyle.JDM, CarStyle.MUSCLE, CarStyle.SUPER, CarStyle.PLANE)
        } else {
            listOf(CarStyle.COUPE, CarStyle.JDM, CarStyle.MUSCLE, CarStyle.SUPER)
        }
    }

    var cheatInput by remember { mutableStateOf("") }
    var pilotText by remember(prefs.pilotName) { mutableStateOf(prefs.pilotName) }

    val colorInt = remember(prefs.carColor) {
        DRIFT_CAR_COLORS.firstOrNull { it.hex.equals(prefs.carColor, ignoreCase = true) }?.colorInt
            ?: 0xFFE62424.toInt()
    }

    val turntableRenderer = remember { DriftRenderer3D(mode = DriftRenderer3D.Mode.TURNTABLE) }
    LaunchedEffect(prefs.selectedTrack, colorInt, prefs.carStyle) {
        turntableRenderer.trackId = prefs.selectedTrack
        turntableRenderer.carColorInt = colorInt
        turntableRenderer.carStyle = prefs.carStyle
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Drift Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToExam,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, CarbonBorder),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao Exame",
                            tint = CarbonText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXAME", style = MaterialTheme.typography.labelSmall, color = CarbonText)
                    }
                    Column {
                        Text(
                            text = "DRIFT MASTERS 3D",
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = CarbonText
                        )
                        Text(
                            text = "GARAGEM & SOLO SANDBOX · RECORDE ${prefs.bestDriftScore} PTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = DriftGold
                        )
                    }
                }
            }

            // 3D Turntable Showroom Card
            Surface(
                color = CarbonSurface,
                border = BorderStroke(1.dp, CarbonBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = DriftCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "GARAGEM 3D",
                                style = MaterialTheme.typography.titleMedium,
                                color = CarbonText
                            )
                        }
                        Text(
                            text = activeTrack.name.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = CarbonSubtext
                        )
                    }

                    // Embedded OpenGL ES 3D Turntable
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
                    ) {
                        DriftGLSurface(
                            renderer = turntableRenderer,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Pilot Name
                    OutlinedTextField(
                        value = pilotText,
                        onValueChange = {
                            val trimmed = it.take(18)
                            pilotText = trimmed
                            onUpdatePilotName(trimmed)
                        },
                        label = { Text("Nome do Piloto", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DriftCrimson,
                            unfocusedBorderColor = CarbonBorder,
                            focusedTextColor = CarbonText,
                            unfocusedTextColor = CarbonText,
                            focusedLabelColor = DriftCrimson,
                            unfocusedLabelColor = CarbonSubtext
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pilot_name_input")
                    )

                    // Car Styles
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CHASSI / MODELO",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarbonSubtext
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableStyles.take(4).forEach { style ->
                                val selected = prefs.carStyle == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) DriftCrimson else CarbonBlack)
                                        .border(
                                            1.dp,
                                            if (selected) DriftCrimson else CarbonBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onUpdateCarStyle(style) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) CarbonText else CarbonSubtext,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (prefs.isCheatUnlocked) {
                            val selected = prefs.carStyle == CarStyle.PLANE
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) DriftGold else CarbonBlack)
                                    .border(
                                        1.dp,
                                        if (selected) DriftGold else DriftGold.copy(alpha = 0.5f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onUpdateCarStyle(CarStyle.PLANE) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AVIÃO SECRETO (DESBLOQUEADO)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) CarbonBlack else DriftGold
                                )
                            }
                        }
                    }

                    // Paint Swatches
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PINTURA",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarbonSubtext
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DRIFT_CAR_COLORS.forEach { swatch ->
                                val selected = prefs.carColor.equals(swatch.hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(swatch.colorInt))
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) Color.White else CarbonBorder,
                                            shape = CircleShape
                                        )
                                        .clickable { onUpdateCarColor(swatch.hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = swatch.name,
                                            tint = CarbonBlack,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Track Selection & Launch Card
            Surface(
                color = CarbonSurface,
                border = BorderStroke(1.dp, CarbonBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = DriftCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ESCOLHER CIRCUITO",
                                style = MaterialTheme.typography.titleMedium,
                                color = CarbonText
                            )
                        }
                        Text(
                            text = activeTrack.difficulty.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (activeTrack.difficulty) {
                                "Fácil" -> EmeraldMogger
                                "Médio" -> DriftNitroSky
                                else -> RoseMogged
                            }
                        )
                    }

                    availableTracks.forEach { tId ->
                        val t = TrackCatalog.get(tId)
                        val selected = prefs.selectedTrack == tId
                        Surface(
                            onClick = { onUpdateTrack(tId) },
                            color = if (selected) DriftCrimson.copy(alpha = 0.14f) else CarbonBlack,
                            border = BorderStroke(1.dp, if (selected) DriftCrimson else CarbonBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = t.name.uppercase(),
                                        fontFamily = SpaceGroteskFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = CarbonText
                                    )
                                    Text(
                                        text = "${t.country} · ${t.difficulty}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CarbonSubtext
                                    )
                                }
                                Text(
                                    text = t.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CarbonSubtext
                                )
                            }
                        }
                    }

                    // Bot Count
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonBlack, RoundedCornerShape(8.dp))
                            .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Adversários (Bots)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = CarbonText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0, 1, 2, 3).forEach { count ->
                                val selected = prefs.botCount == count
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) DriftCrimson else CarbonSurface)
                                        .border(1.dp, if (selected) DriftCrimson else CarbonBorder, RoundedCornerShape(6.dp))
                                        .clickable { onUpdateBotCount(count) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CarbonText
                                    )
                                }
                            }
                        }
                    }

                    // Secret code unlock ("fun-kyon")
                    if (!prefs.isCheatUnlocked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = cheatInput,
                                onValueChange = {
                                    cheatInput = it
                                    onClearCheatError()
                                },
                                placeholder = { Text("Código secreto (opcional)...", style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = CarbonMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CarbonSubtext,
                                    unfocusedBorderColor = CarbonBorder,
                                    focusedTextColor = CarbonText,
                                    unfocusedTextColor = CarbonText
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    onSubmitCheatCode(cheatInput)
                                    if (cheatInput.trim().lowercase() == "fun-kyon") {
                                        cheatInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, CarbonBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .height(52.dp)
                            ) {
                                Text("ATIVAR", style = MaterialTheme.typography.labelSmall, color = CarbonText)
                            }
                        }
                        if (cheatError != null) {
                            Text(
                                text = cheatError,
                                style = MaterialTheme.typography.labelSmall,
                                color = RoseMogged
                            )
                        }
                    }

                    // Launch Buttons
                    Button(
                        onClick = onStartFreeRoam,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DriftCrimson,
                            contentColor = CarbonText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(50.dp)
                            .testTag("start_drift_free_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TREINO LIVRE / DRIFT SOLO",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onStartCompetitive,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DriftGold.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CarbonText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(48.dp)
                            .testTag("start_drift_comp_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = DriftGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DESAFIO COMPETITIVO (60S)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DriftRaceScreen(
    prefs: DriftUserPrefs,
    session: DriftSessionState,
    onChangeTrack: (TrackId) -> Unit,
    onFinishRace: (Int, List<GhostFrame>) -> Unit,
    onLeaveToLobby: () -> Unit,
    modifier: Modifier = Modifier
) {
    val input = remember { DriftControlInput() }
    val audioEngine = remember { DriftAudioEngine() }
    var muted by remember { mutableStateOf(false) }
    var hud by remember { mutableStateOf(DriftHudSnapshot()) }
    var showTrackMenu by remember { mutableStateOf(false) }

    val colorInt = remember(prefs.carColor) {
        DRIFT_CAR_COLORS.firstOrNull { it.hex.equals(prefs.carColor, ignoreCase = true) }?.colorInt
            ?: 0xFFE62424.toInt()
    }

    val renderer = remember {
        DriftRenderer3D(
            mode = DriftRenderer3D.Mode.RACE,
            input = input,
            audioEngine = audioEngine,
            onHudUpdate = { snapshot -> hud = snapshot }
        )
    }

    LaunchedEffect(prefs.selectedTrack, colorInt, prefs.carStyle, prefs.botCount, session.status, session.isGhostActive, session.ghostReplay) {
        renderer.trackId = prefs.selectedTrack
        renderer.carColorInt = colorInt
        renderer.carStyle = prefs.carStyle
        renderer.botCount = prefs.botCount
        renderer.canDrive = session.status == DriftRaceStatus.RACING
        renderer.isGhostActive = session.isGhostActive
        renderer.ghostReplay = session.ghostReplay
    }

    // Auto-finish when 60s competitive timer hits 0
    LaunchedEffect(session.isCompetitiveMode, session.competitiveSecondsLeft, session.status) {
        if (session.isCompetitiveMode && session.status == DriftRaceStatus.RACING && session.competitiveSecondsLeft <= 0) {
            onFinishRace(renderer.currentTotalScore(), renderer.snapshotRecordedFrames())
        }
    }

    DisposableEffect(Unit) {
        audioEngine.start()
        onDispose {
            input.reset()
            audioEngine.stop()
        }
    }

    val trackCfg = TrackCatalog.get(prefs.selectedTrack)
    val availableTracks = if (prefs.isCheatUnlocked) {
        listOf(TrackId.JAPAN, TrackId.USA, TrackId.BRAZIL, TrackId.SCRAPYARD, TrackId.AIRPORT)
    } else {
        listOf(TrackId.JAPAN, TrackId.USA, TrackId.BRAZIL, TrackId.SCRAPYARD)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
    ) {
        // 3D OpenGL Race Canvas
        DriftGLSurface(
            renderer = renderer,
            modifier = Modifier.fillMaxSize()
        )

        // Top HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Switcher
            Box {
                Surface(
                    onClick = { showTrackMenu = true },
                    color = CarbonBlack.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, CarbonBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = trackCfg.name.uppercase(),
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = CarbonText
                        )
                        Text(
                            text = "▼",
                            fontSize = 9.sp,
                            color = CarbonSubtext
                        )
                    }
                }
                DropdownMenu(
                    expanded = showTrackMenu,
                    onDismissRequest = { showTrackMenu = false }
                ) {
                    availableTracks.forEach { tId ->
                        DropdownMenuItem(
                            text = { Text(TrackCatalog.get(tId).name) },
                            onClick = {
                                showTrackMenu = false
                                onChangeTrack(tId)
                            }
                        )
                    }
                }
            }

            // Right HUD Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (session.isCompetitiveMode) {
                    Surface(
                        color = CarbonBlack.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, DriftGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${session.competitiveSecondsLeft}s",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = DriftGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        muted = !muted
                        audioEngine.setMuted(muted)
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarbonBlack.copy(alpha = 0.85f))
                        .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (muted) "Ativar som" else "Silenciar",
                        tint = CarbonText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = {
                        onFinishRace(renderer.currentTotalScore(), renderer.snapshotRecordedFrames())
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DriftCrimson),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FINALIZAR", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onLeaveToLobby,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CarbonBorder),
                    modifier = Modifier
                        .height(38.dp)
                        .background(CarbonBlack.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                ) {
                    Text("LOBBY", style = MaterialTheme.typography.labelSmall, color = CarbonText)
                }
            }
        }

        // Live Drift Combo Banner
        AnimatedVisibility(
            visible = hud.currentChain > 0,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 66.dp)
        ) {
            Surface(
                color = CarbonBlack.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, DriftCrimson.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (hud.isDrifting) DriftGold else DriftCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "+${hud.currentChain}",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = CarbonText
                    )
                    Text(
                        text = "${hud.multiplier}x",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = DriftGold
                    )
                }
            }
        }

        // Countdown Overlay (3, 2, 1, GO!!!)
        session.countdownNumber?.let { countVal ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CarbonBlack.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (countVal > 0) countVal.toString() else "GO!!!",
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = if (countVal > 0) 96.sp else 82.sp,
                    color = if (countVal > 0) CarbonText else DriftCrimson
                )
            }
        }

        // Bottom Telemetry + Multi-Touch Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Telemetry Strip
            Surface(
                color = CarbonBlack.copy(alpha = 0.86f),
                border = BorderStroke(1.dp, CarbonBorder),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = DriftCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${hud.speedKmh}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = CarbonText
                        )
                        Text("KM/H", style = MaterialTheme.typography.labelSmall, color = CarbonSubtext)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PONTOS DRIFT", style = MaterialTheme.typography.labelSmall, color = CarbonMuted)
                        Text(
                            text = "${hud.totalPoints}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = EmeraldMogger
                        )
                    }

                    Column(
                        modifier = Modifier.width(90.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = DriftNitroSky,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text("NITRO", style = MaterialTheme.typography.labelSmall, color = CarbonSubtext)
                            }
                            Text("${hud.nitroCharge}%", style = MaterialTheme.typography.labelSmall, color = CarbonText)
                        }
                        LinearProgressIndicator(
                            progress = { hud.nitroCharge / 100f },
                            color = DriftNitroSky,
                            trackColor = CarbonBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            // Arcade Multi-Touch Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steering Left & Right
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HoldControlButton(
                        label = "◀",
                        bgColor = CarbonBlack.copy(alpha = 0.85f),
                        borderColor = CarbonBorder,
                        onPressChange = { input.steerLeft = it },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Virar à esquerda",
                            tint = CarbonText,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    HoldControlButton(
                        label = "▶",
                        bgColor = CarbonBlack.copy(alpha = 0.85f),
                        borderColor = CarbonBorder,
                        onPressChange = { input.steerRight = it },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Virar à direita",
                            tint = CarbonText,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Pedals: Brake, Drift, Nitro, Accelerate
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HoldControlButton(
                        label = "FREIO",
                        bgColor = CarbonBlack.copy(alpha = 0.85f),
                        borderColor = CarbonBorder,
                        onPressChange = { input.brake = it },
                        modifier = Modifier.size(width = 56.dp, height = 64.dp)
                    ) {
                        Text("RÉ", style = MaterialTheme.typography.labelSmall, color = CarbonSubtext)
                    }
                    HoldControlButton(
                        label = "DRIFT",
                        bgColor = AmberExempt.copy(alpha = 0.88f),
                        borderColor = DriftGold,
                        onPressChange = { input.handbrake = it },
                        modifier = Modifier.size(width = 64.dp, height = 64.dp)
                    ) {
                        Text("DRIFT", style = MaterialTheme.typography.labelSmall, color = CarbonBlack)
                    }
                    HoldControlButton(
                        label = "NITRO",
                        bgColor = DriftNitroSky.copy(alpha = 0.88f),
                        borderColor = Color.White.copy(alpha = 0.6f),
                        onPressChange = { input.boost = it },
                        modifier = Modifier.size(width = 60.dp, height = 64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Nitro",
                            tint = CarbonBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    HoldControlButton(
                        label = "ACELERAR",
                        bgColor = DriftCrimson.copy(alpha = 0.92f),
                        borderColor = Color.White.copy(alpha = 0.5f),
                        onPressChange = { input.throttle = it },
                        modifier = Modifier.size(width = 76.dp, height = 64.dp)
                    ) {
                        Text("GÁS", style = MaterialTheme.typography.labelLarge, color = CarbonText)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HoldControlButton(
    label: String,
    bgColor: Color,
    borderColor: Color,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) bgColor.copy(alpha = 1f) else bgColor)
            .border(
                width = if (pressed) 2.dp else 1.dp,
                color = if (pressed) Color.White else borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        pressed = true
                        onPressChange(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        pressed = false
                        onPressChange(false)
                    }
                }
                true
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun DriftPodiumScreen(
    pilotName: String,
    session: DriftSessionState,
    onBackToLobby: () -> Unit,
    onStartGhostMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = CarbonSurface,
            border = BorderStroke(1.dp, CarbonBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DriftCrimson.copy(alpha = 0.14f))
                        .border(1.dp, DriftCrimson.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = DriftCrimson,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CORRIDA FINALIZADA!",
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = CarbonText
                    )
                    Text(
                        text = "Mural de Resultados de Drift",
                        style = MaterialTheme.typography.bodySmall,
                        color = CarbonSubtext
                    )
                }

                Surface(
                    color = CarbonBlack,
                    border = BorderStroke(1.dp, CarbonBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "VENCEDOR DO DUELO",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarbonMuted
                        )
                        Text(
                            text = pilotName.ifBlank { "Piloto 1" }.uppercase(),
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 20.sp,
                            color = CarbonText
                        )
                        Text(
                            text = "Pontuação: ${session.lastWinnerScore} pts",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = DriftCrimson
                        )
                    }
                }

                Button(
                    onClick = onBackToLobby,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DriftCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .height(48.dp)
                ) {
                    Text("VOLTAR PARA O LOBBY", style = MaterialTheme.typography.labelLarge)
                }

                if (session.pendingGhostReplay.isNotEmpty()) {
                    Button(
                        onClick = onStartGhostMatch,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DriftNitroSky,
                            contentColor = CarbonBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "DESAFIAR JOGADOR FANTASMA",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DriftGLSurface(
    renderer: DriftRenderer3D,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val glView = remember(renderer) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier
    )
}
