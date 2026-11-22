package com.example.ui.exam

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EvolutionSummary
import com.example.data.ExamStatus
import com.example.data.HabitCatalog
import com.example.data.HabitQuestion
import com.example.data.HistoryEntry
import com.example.ui.ExamStage
import com.example.ui.ExamUiState
import com.example.ui.theme.AmberExempt
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonMuted
import com.example.ui.theme.CarbonSubtext
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonText
import com.example.ui.theme.DriftCrimson
import com.example.ui.theme.EmeraldMogger
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoseMogged
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun ExamRootScreen(
    examUi: ExamUiState,
    summary: EvolutionSummary,
    onStartExam: () -> Unit,
    onSelectOption: (Int, String) -> Unit,
    onPreviousQuestion: () -> Unit,
    onCancelExam: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDrift: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = examUi.stage,
        label = "exam_stage_transition",
        modifier = modifier.fillMaxSize()
    ) { stage ->
        when (stage) {
            ExamStage.WELCOME -> ExamWelcomeView(
                summary = summary,
                onStartExam = onStartExam,
                onOpenHistory = onOpenHistory,
                onOpenDrift = onOpenDrift
            )
            ExamStage.QUESTION -> {
                val question = HabitCatalog.questions.getOrNull(examUi.currentQuestionIndex)
                if (question != null) {
                    ExamQuestionView(
                        question = question,
                        totalQuestions = HabitCatalog.questions.size,
                        selectedOptionId = examUi.answers[question.id],
                        canGoBack = examUi.currentQuestionIndex > 0,
                        onSelectOption = onSelectOption,
                        onPrevious = onPreviousQuestion,
                        onCancel = onCancelExam
                    )
                }
            }
            ExamStage.ANALYZING -> ExamAnalyzingView()
            ExamStage.RESULTS -> {
                val result = examUi.latestResult
                if (result != null) {
                    ExamResultsView(
                        entry = result,
                        delta = examUi.latestDelta,
                        onOpenHistory = onOpenHistory,
                        onRestartWelcome = onCancelExam
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamWelcomeView(
    summary: EvolutionSummary,
    onStartExam: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDrift: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = CarbonSurface,
            border = BorderStroke(1.dp, CarbonBorder),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "EXAME DE DISCIPLINA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = CarbonText,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Avaliação diária de 13 hábitos essenciais com registro de evolução.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CarbonSubtext,
                        textAlign = TextAlign.Center
                    )
                }

                // 13 Parameters Grid
                Surface(
                    color = CarbonBlack,
                    border = BorderStroke(1.dp, CarbonBorder),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "REQUISITOS AVALIADOS",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarbonMuted
                        )
                        val items = HabitCatalog.questions
                        val half = (items.size + 1) / 2
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in 0 until half) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val left = items[row]
                                    ParameterChip(
                                        index = left.id,
                                        label = left.shortLabel,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val rightIdx = row + half
                                    if (rightIdx < items.size) {
                                        val right = items[rightIdx]
                                        ParameterChip(
                                            index = right.id,
                                            label = right.shortLabel,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Evolution Snapshot
                if (summary.totalExams > 0 && summary.latestScore != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonBlack)
                            .border(1.dp, CarbonBorder)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Último:",
                                style = MaterialTheme.typography.bodySmall,
                                color = CarbonSubtext
                            )
                            Text(
                                text = "${summary.latestScore}%",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = if (summary.latestScore >= 60) EmeraldMogger else RoseMogged
                            )
                        }

                        DeltaBadge(delta = summary.recentDelta, fallbackText = "${summary.totalExams} registro")
                    }
                }

                // Primary & Secondary CTAs
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartExam,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CarbonText,
                            contentColor = CarbonBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(48.dp)
                            .testTag("start_exam_button")
                    ) {
                        Text(
                            text = "COMEÇAR O EXAME",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenHistory,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CarbonText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(48.dp)
                            .testTag("open_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VER EVOLUÇÃO E HISTÓRICO (${summary.totalExams})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Drift Masters 3D Promo Card
        Surface(
            onClick = onOpenDrift,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, DriftCrimson.copy(alpha = 0.45f)),
            color = CarbonCard,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .testTag("open_drift_card")
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                DriftCrimson.copy(alpha = 0.16f),
                                CarbonCard
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DriftCrimson.copy(alpha = 0.2f))
                                .border(1.dp, DriftCrimson.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Drift Masters 3D",
                                tint = DriftCrimson
                            )
                        }
                        Column {
                            Text(
                                text = "DRIFT MASTERS 3D",
                                fontFamily = SpaceGroteskFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = CarbonText
                            )
                            Text(
                                text = "5 circuitos, garagem 3D, bots e modo fantasma",
                                style = MaterialTheme.typography.bodySmall,
                                color = CarbonSubtext,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Abrir Drift Masters 3D",
                        tint = DriftCrimson
                    )
                }
            }
        }
    }
}

@Composable
private fun ParameterChip(index: Int, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${index.toString().padStart(2, '0')}.",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = CarbonMuted
        )
        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            color = CarbonText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExamQuestionView(
    question: HabitQuestion,
    totalQuestions: Int,
    selectedOptionId: String?,
    canGoBack: Boolean,
    onSelectOption: (Int, String) -> Unit,
    onPrevious: () -> Unit,
    onCancel: () -> Unit
) {
    BackHandler {
        if (canGoBack) onPrevious() else onCancel()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUESTÃO ${question.id} DE $totalQuestions",
                    style = MaterialTheme.typography.labelSmall,
                    color = CarbonMuted
                )
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cancel_exam_button")
                ) {
                    Text(
                        text = "CANCELAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = CarbonSubtext
                    )
                }
            }

            // Progress pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..totalQuestions) {
                    val width by animateDpAsState(
                        targetValue = when {
                            i == question.id -> 26.dp
                            i < question.id -> 10.dp
                            else -> 6.dp
                        },
                        animationSpec = spring(),
                        label = "progress_pill"
                    )
                    val color = when {
                        i == question.id -> CarbonText
                        i < question.id -> CarbonMuted
                        else -> CarbonBorder
                    }
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(width)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
            }

            // Question text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AVALIAÇÃO",
                    style = MaterialTheme.typography.labelSmall,
                    color = CarbonMuted
                )
                Text(
                    text = question.text,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    color = CarbonText,
                    textAlign = TextAlign.Center
                )
            }

            // Stacked option cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                question.options.forEach { option ->
                    val isSelected = selectedOptionId == option.id
                    Surface(
                        color = if (isSelected) CarbonText else CarbonSurface,
                        border = BorderStroke(1.dp, if (isSelected) CarbonText else CarbonBorder),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onSelectOption(question.id, option.id) }
                            .testTag("opt_${question.id}_${option.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.text.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) CarbonBlack else CarbonText,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (isSelected) "SELECIONADO" else "SELECIONAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) CarbonBlack else CarbonMuted
                            )
                        }
                    }
                }
            }

            // Previous question button
            if (canGoBack) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("previous_question_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = CarbonSubtext,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Voltar à pergunta anterior",
                            style = MaterialTheme.typography.bodySmall,
                            color = CarbonSubtext
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamAnalyzingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = CarbonText,
                trackColor = CarbonBorder,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "CALCULANDO MÉTRICA DE DISCIPLINA...",
                style = MaterialTheme.typography.labelSmall,
                color = CarbonSubtext
            )
        }
    }
}

@Composable
private fun ExamResultsView(
    entry: HistoryEntry,
    delta: Int?,
    onOpenHistory: () -> Unit,
    onRestartWelcome: () -> Unit
) {
    BackHandler { onRestartWelcome() }

    val isGood = entry.status == ExamStatus.MOGGER
    val accent = if (isGood) EmeraldMogger else RoseMogged
    val formatted = HabitCatalog.formatEntryDate(entry.epochMillis)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Header Card
            Surface(
                color = CarbonSurface,
                border = BorderStroke(1.dp, CarbonBorder),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "STATUS DO EXAME · ${formatted.fullDate} • ${formatted.weekday}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarbonSubtext
                        )
                        Text(
                            text = entry.status.name,
                            style = MaterialTheme.typography.displayLarge,
                            color = accent,
                            letterSpacing = 2.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isGood) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isGood) "APROVADO" else "REPROVADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .border(BorderStroke(0.dp, Color.Transparent))
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = "DIÁRIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarbonMuted
                        )
                        Text(
                            text = "${entry.score}%",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = accent
                        )
                    }
                }
            }

            // Delta Banner
            Surface(
                color = CarbonSurface,
                border = BorderStroke(1.dp, CarbonBorder),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comparativo com o exame anterior",
                        style = MaterialTheme.typography.bodySmall,
                        color = CarbonSubtext
                    )
                    DeltaBadge(delta = delta, fallbackText = "Marco inicial salvo")
                }
            }

            // 13-Habit Breakdown
            Surface(
                color = CarbonSurface,
                border = BorderStroke(1.dp, CarbonBorder),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CONTROLE DE STATUS DIÁRIOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CarbonSubtext
                    )
                    HabitCatalog.questions.forEachIndexed { idx, q ->
                        val ansCode = entry.answers[q.id]
                        val chosenOpt = q.options.firstOrNull { it.id == ansCode }
                        val isYes = ansCode == "A"
                        val isNo = ansCode == "B"
                        val badgeColor = when {
                            isYes -> EmeraldMogger
                            isNo -> RoseMogged
                            q.id == 3 || q.id == 6 -> EmeraldMogger
                            else -> AmberExempt
                        }
                        val badgeLabel = when {
                            isYes -> "SIM"
                            isNo -> "NÃO"
                            chosenOpt != null -> "ISENTO (${chosenOpt.text})"
                            else -> "N/A"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(idx + 1).toString().padStart(2, '0')}. ${q.text}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CarbonText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor
                            )
                        }
                        if (idx < HabitCatalog.questions.lastIndex) {
                            HorizontalDivider(color = CarbonBorder.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenHistory,
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CarbonText,
                        contentColor = CarbonBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .height(48.dp)
                        .testTag("results_history_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EVOLUÇÃO",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedButton(
                    onClick = onRestartWelcome,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, CarbonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CarbonText),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .height(48.dp)
                        .testTag("results_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INÍCIO",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun DeltaBadge(delta: Int?, fallbackText: String) {
    when {
        delta == null -> {
            Text(
                text = fallbackText,
                style = MaterialTheme.typography.bodySmall,
                color = CarbonSubtext
            )
        }
        delta > 0 -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = EmeraldMogger,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "+$delta% evolução",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldMogger
                )
            }
        }
        delta < 0 -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = RoseMogged,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "$delta% desevolução",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = RoseMogged
                )
            }
        }
        else -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = CarbonSubtext,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "0% estável",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = CarbonSubtext
                )
            }
        }
    }
}
