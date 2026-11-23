package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drift.DriftRaceStatus
import com.example.royale.ui.RoyaleRootEntry
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.drift.DriftRootScreen
import com.example.ui.exam.ExamRootScreen
import com.example.ui.exam.HistoryScreen
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonMuted
import com.example.ui.theme.CarbonSubtext
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonText
import com.example.ui.theme.DisciplinaDriftTheme
import com.example.ui.theme.DriftCrimson
import com.example.ui.theme.DriftGold
import com.example.ui.theme.DriftNitroSky
import com.example.ui.theme.EmeraldMogger
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.BLACK),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.BLACK)
        )
        setContent {
            DisciplinaDriftTheme {
                DisciplinaDriftApp()
            }
        }
    }
}

@Composable
fun DisciplinaDriftApp(
    viewModel: MainViewModel = viewModel()
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val showGameCover by viewModel.showGameCover.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val summary by viewModel.evolutionSummary.collectAsStateWithLifecycle()
    val examUi by viewModel.examUi.collectAsStateWithLifecycle()
    val driftPrefs by viewModel.driftPrefs.collectAsStateWithLifecycle()
    val driftSession by viewModel.driftSession.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeTab == MainTab.ROYALE) {
            RoyaleRootEntry(
                onExitRoyaleToSuite = { viewModel.selectTab(MainTab.EXAM) }
            )
        } else {
            val isFullScreenRace = activeTab == MainTab.DRIFT &&
                (driftSession.status == DriftRaceStatus.COUNTDOWN || driftSession.status == DriftRaceStatus.RACING)

            Scaffold(
                containerColor = CarbonBlack,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    if (!isFullScreenRace) {
                        TopHeaderBar(
                            activeTab = activeTab,
                            onQuickNewExam = { viewModel.startExam() },
                            onLogoClick = { viewModel.resetExamToWelcome() }
                        )
                    }
                },
                bottomBar = {
                    if (!isFullScreenRace) {
                        BottomMainNavBar(
                            activeTab = activeTab,
                            historyCount = history.size,
                            onSelectTab = { viewModel.selectTab(it) }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(CarbonBlack)
                ) {
                    when (activeTab) {
                        MainTab.EXAM -> ExamRootScreen(
                            examUi = examUi,
                            summary = summary,
                            onStartExam = { viewModel.startExam() },
                            onSelectOption = { qId, optId -> viewModel.selectOption(qId, optId) },
                            onPreviousQuestion = { viewModel.previousQuestion() },
                            onCancelExam = { viewModel.resetExamToWelcome() },
                            onOpenHistory = { viewModel.selectTab(MainTab.HISTORY) }
                        )
                        MainTab.HISTORY -> HistoryScreen(
                            entries = history,
                            summary = summary,
                            onDeleteEntry = { viewModel.deleteHistoryEntry(it) },
                            onClearHistory = { viewModel.clearAllHistory() },
                            onStartExam = { viewModel.startExam() },
                            onBackToExam = { viewModel.selectTab(MainTab.EXAM) }
                        )
                        MainTab.DRIFT -> DriftRootScreen(
                            prefs = driftPrefs,
                            session = driftSession,
                            onUpdatePilotName = { viewModel.updatePilotName(it) },
                            onUpdateCarColor = { viewModel.updateCarColor(it) },
                            onUpdateCarStyle = { viewModel.updateCarStyle(it) },
                            onUpdateTrack = { viewModel.updateSelectedTrack(it) },
                            onUpdateBotCount = { viewModel.updateBotCount(it) },
                            onSubmitCheatCode = { viewModel.submitCheatCode(it) },
                            onClearCheatError = { viewModel.clearCheatError() },
                            onStartFreeRoam = { viewModel.startDriftRace(isCompetitive = false) },
                            onStartCompetitive = { viewModel.startDriftRace(isCompetitive = true) },
                            onStartGhostMatch = { viewModel.startGhostMatch() },
                            onFinishRace = { score, frames -> viewModel.finishDriftRace(score, frames) },
                            onLeaveRaceToLobby = { viewModel.leaveDriftToLobby() },
                            onBackToExam = { viewModel.selectTab(MainTab.EXAM) }
                        )
                        MainTab.ROYALE -> Unit
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showGameCover,
            exit = fadeOut(animationSpec = tween(durationMillis = 320))
        ) {
            GameCoverOverlay(
                onDismiss = { viewModel.dismissGameCover() }
            )
        }
    }
}

@Composable
private fun GameCoverOverlay(
    onDismiss: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
        onDismiss()
    }

    val skyGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF156EEB),
            Color(0xFF0E3A8A),
            Color(0xFF071128)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("game_cover_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = Color(0xCC091326),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.5.dp, DriftGold)
            ) {
                Text(
                    text = stringResource(R.string.cover_title_badge),
                    color = DriftGold,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(3.dp, DriftGold),
                shadowElevation = 20.dp,
                color = Color(0xFF156EEB),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 18.dp)
                    .aspectRatio(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.game_cover_art),
                    contentDescription = stringResource(R.string.cover_art_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.cover_loading_label),
                        color = Color.White,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${(progress.value * 100).toInt()}%",
                        color = DriftGold,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                LinearProgressIndicator(
                    progress = { progress.value },
                    color = DriftGold,
                    trackColor = Color(0x55000000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.cover_tap_to_enter),
                    color = DriftNitroSky,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    activeTab: MainTab,
    onQuickNewExam: () -> Unit,
    onLogoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CarbonBlack)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onLogoClick,
                color = CarbonBlack
            ) {
                Text(
                    text = if (activeTab == MainTab.DRIFT) "CAR" else "EXAME DE DISCIPLINA",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = CarbonText
                )
            }

            if (activeTab != MainTab.DRIFT) {
                Surface(
                    onClick = onQuickNewExam,
                    color = CarbonText,
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.testTag("header_new_exam_button")
                ) {
                    Text(
                        text = "NOVO EXAME",
                        style = MaterialTheme.typography.labelSmall,
                        color = CarbonBlack,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            } else {
                Surface(
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, CarbonBorder),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "SOLO 3D",
                        style = MaterialTheme.typography.labelSmall,
                        color = DriftCrimson,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
        HorizontalDivider(
            color = CarbonBorder,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun BottomMainNavBar(
    activeTab: MainTab,
    historyCount: Int,
    onSelectTab: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = CarbonSurface,
        contentColor = CarbonText
    ) {
        NavigationBarItem(
            selected = activeTab == MainTab.EXAM,
            onClick = { onSelectTab(MainTab.EXAM) },
            icon = {
                Icon(
                    imageVector = if (activeTab == MainTab.EXAM) Icons.Filled.FactCheck else Icons.Outlined.FactCheck,
                    contentDescription = "Exame"
                )
            },
            label = { Text("Exame", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CarbonBlack,
                selectedTextColor = CarbonText,
                indicatorColor = CarbonText,
                unselectedIconColor = CarbonMuted,
                unselectedTextColor = CarbonSubtext
            ),
            modifier = Modifier.testTag("nav_exam_tab")
        )

        NavigationBarItem(
            selected = activeTab == MainTab.HISTORY,
            onClick = { onSelectTab(MainTab.HISTORY) },
            icon = {
                BadgedBox(
                    badge = {
                        if (historyCount > 0) {
                            Badge(
                                containerColor = EmeraldMogger,
                                contentColor = CarbonBlack
                            ) {
                                Text(historyCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Histórico e Evolução"
                    )
                }
            },
            label = { Text("Evolução", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CarbonBlack,
                selectedTextColor = CarbonText,
                indicatorColor = EmeraldMogger,
                unselectedIconColor = CarbonMuted,
                unselectedTextColor = CarbonSubtext
            ),
            modifier = Modifier.testTag("nav_history_tab")
        )

        NavigationBarItem(
            selected = activeTab == MainTab.DRIFT,
            onClick = { onSelectTab(MainTab.DRIFT) },
            icon = {
                Icon(
                    imageVector = if (activeTab == MainTab.DRIFT) Icons.Filled.DirectionsCar else Icons.Outlined.DirectionsCar,
                    contentDescription = "Car"
                )
            },
            label = { Text("Car", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CarbonText,
                selectedTextColor = CarbonText,
                indicatorColor = DriftCrimson,
                unselectedIconColor = CarbonMuted,
                unselectedTextColor = CarbonSubtext
            ),
            modifier = Modifier.testTag("nav_drift_tab")
        )

        NavigationBarItem(
            selected = activeTab == MainTab.ROYALE,
            onClick = { onSelectTab(MainTab.ROYALE) },
            icon = {
                Icon(
                    imageVector = if (activeTab == MainTab.ROYALE) Icons.Filled.SportsEsports else Icons.Outlined.SportsEsports,
                    contentDescription = "Arena Royale"
                )
            },
            label = { Text("Royale", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CarbonBlack,
                selectedTextColor = CarbonText,
                indicatorColor = DriftGold,
                unselectedIconColor = CarbonMuted,
                unselectedTextColor = CarbonSubtext
            ),
            modifier = Modifier.testTag("nav_royale_tab")
        )
    }
}
