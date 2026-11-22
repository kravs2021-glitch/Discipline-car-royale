package com.example.ui.exam

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EvolutionSummary
import com.example.data.ExamStatus
import com.example.data.HabitCatalog
import com.example.data.HistoryEntry
import com.example.ui.theme.AmberExempt
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonMuted
import com.example.ui.theme.CarbonSubtext
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonText
import com.example.ui.theme.EmeraldMogger
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoseMogged

private enum class StatusFilter(val label: String) {
    ALL("Todos"),
    MOGGER("MOGGER"),
    MOGGED("MOGGED")
}

private data class EnrichedHistoryItem(
    val entry: HistoryEntry,
    val examNumber: Int,
    val delta: Int?
)

@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    summary: EvolutionSummary,
    onDeleteEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    onStartExam: () -> Unit,
    onBackToExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToExam() }

    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val enrichedDescending = remember(entries) {
        entries.mapIndexed { idx, entry ->
            val prev = if (idx > 0) entries[idx - 1] else null
            EnrichedHistoryItem(
                entry = entry,
                examNumber = idx + 1,
                delta = prev?.let { entry.score - it.score }
            )
        }.reversed()
    }

    val filteredList = remember(enrichedDescending, statusFilter) {
        when (statusFilter) {
            StatusFilter.ALL -> enrichedDescending
            StatusFilter.MOGGER -> enrichedDescending.filter { it.entry.status == ExamStatus.MOGGER }
            StatusFilter.MOGGED -> enrichedDescending.filter { it.entry.status == ExamStatus.MOGGED }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.size(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HISTÓRICO E EVOLUÇÃO",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CarbonText
                        )
                        Text(
                            text = "Compare seus exames e acompanhe períodos de evolução ou desevolução.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CarbonSubtext
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onStartExam,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CarbonText,
                            contentColor = CarbonBlack
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("history_new_exam_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NOVO",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // 4-KPI Grid (2x2)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CarbonSurface)
                        .border(1.dp, CarbonBorder)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        KpiTile(
                            label = "ÚLTIMO EXAME",
                            value = summary.latestScore?.let { "$it%" } ?: "—",
                            valueColor = when {
                                summary.latestScore == null -> CarbonMuted
                                summary.latestScore >= 60 -> EmeraldMogger
                                else -> RoseMogged
                            },
                            subtext = summary.latestScore?.let { if (it >= 60) "MOGGER" else "MOGGED" } ?: "Sem registros",
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.width(1.dp).background(CarbonBorder))
                        KpiTile(
                            label = "VS. ANTERIOR",
                            value = summary.recentDelta?.let { if (it > 0) "+$it%" else "$it%" } ?: "—",
                            valueColor = when {
                                summary.recentDelta == null -> CarbonMuted
                                summary.recentDelta > 0 -> EmeraldMogger
                                summary.recentDelta < 0 -> RoseMogged
                                else -> CarbonText
                            },
                            subtext = when {
                                summary.recentDelta == null -> "Requer 2+ exames"
                                summary.recentDelta > 0 -> "Evolução recente"
                                summary.recentDelta < 0 -> "Desevolução recente"
                                else -> "Sem variação"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = CarbonBorder)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        KpiTile(
                            label = "SALDO DESDE O 1º",
                            value = summary.overallDelta?.let { if (it > 0) "+$it%" else "$it%" } ?: "—",
                            valueColor = when {
                                summary.overallDelta == null -> CarbonMuted
                                summary.overallDelta > 0 -> EmeraldMogger
                                summary.overallDelta < 0 -> RoseMogged
                                else -> CarbonText
                            },
                            subtext = summary.firstScore?.let { "Início: $it%" } ?: "Requer 2+ exames",
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.width(1.dp).background(CarbonBorder))
                        KpiTile(
                            label = "MÉDIA GERAL",
                            value = summary.averageScore?.let { "$it%" } ?: "—",
                            valueColor = if (summary.averageScore != null) CarbonText else CarbonMuted,
                            subtext = if (summary.totalExams > 0) "${summary.totalExams} exames · Máx ${summary.bestScore}%" else "0 exames",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Chart
            item {
                EvolutionChart(
                    entries = entries,
                    onStartExam = onStartExam
                )
            }

            // History Section Header + Filters
            item {
                Surface(
                    color = CarbonSurface,
                    border = BorderStroke(1.dp, CarbonBorder),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REGISTRO DE EXAMES (${entries.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = CarbonText
                            )
                            if (entries.isNotEmpty() && !confirmClear) {
                                OutlinedButton(
                                    onClick = { confirmClear = true },
                                    shape = RoundedCornerShape(2.dp),
                                    border = BorderStroke(1.dp, CarbonBorder),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text(
                                        text = "LIMPAR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CarbonSubtext
                                    )
                                }
                            }
                        }

                        if (confirmClear) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onClearHistory()
                                        confirmClear = false
                                        expandedId = null
                                    },
                                    shape = RoundedCornerShape(2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoseMogged,
                                        contentColor = CarbonText
                                    ),
                                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                                ) {
                                    Text("CONFIRMAR LIMPEZA", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { confirmClear = false },
                                    shape = RoundedCornerShape(2.dp),
                                    border = BorderStroke(1.dp, CarbonBorder),
                                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                                ) {
                                    Text("CANCELAR", style = MaterialTheme.typography.labelSmall, color = CarbonText)
                                }
                            }
                        }

                        // Status Filters
                        Row(
                            modifier = Modifier
                                .background(CarbonBlack)
                                .border(1.dp, CarbonBorder)
                                .padding(2.dp)
                        ) {
                            StatusFilter.entries.forEach { tab ->
                                val active = statusFilter == tab
                                Box(
                                    modifier = Modifier
                                        .clickable { statusFilter = tab }
                                        .background(if (active) CarbonText else Color.Transparent)
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) CarbonBlack else CarbonSubtext
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonSurface)
                            .border(1.dp, CarbonBorder)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (entries.isEmpty()) {
                                "Nenhum exame salvo no histórico ainda."
                            } else {
                                "Nenhum exame com classificação ${statusFilter.label} encontrado."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = CarbonMuted
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.entry.id }) { item ->
                    val entry = item.entry
                    val dateInfo = HabitCatalog.formatEntryDate(entry.epochMillis)
                    val isExpanded = expandedId == entry.id
                    val isGood = entry.status == ExamStatus.MOGGER
                    val accent = if (isGood) EmeraldMogger else RoseMogged

                    Surface(
                        color = CarbonBlack,
                        border = BorderStroke(1.dp, CarbonBorder),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedId = if (isExpanded) null else entry.id }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "#${item.examNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CarbonMuted
                                    )
                                    Column {
                                        Text(
                                            text = "${dateInfo.fullDate} · ${dateInfo.weekday}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = CarbonText
                                        )
                                        Text(
                                            text = "Registrado às ${dateInfo.time}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CarbonMuted
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${entry.score}%",
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = accent
                                            )
                                            Icon(
                                                imageVector = if (isGood) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = entry.status.name,
                                                tint = accent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        DeltaBadge(delta = item.delta, fallbackText = "Base inicial")
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Detalhes",
                                        tint = CarbonSubtext
                                    )

                                    IconButton(
                                        onClick = { onDeleteEntry(entry.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Excluir exame #${item.examNumber}",
                                            tint = CarbonMuted
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CarbonSurface)
                                        .border(1.dp, CarbonBorder)
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HabitCatalog.questions.forEachIndexed { qIdx, q ->
                                        val code = entry.answers[q.id]
                                        val opt = q.options.firstOrNull { it.id == code }
                                        val isYes = code == "A"
                                        val isNo = code == "B"
                                        val color = when {
                                            isYes -> EmeraldMogger
                                            isNo -> RoseMogged
                                            q.id == 3 || q.id == 6 -> EmeraldMogger
                                            else -> AmberExempt
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${(qIdx + 1).toString().padStart(2, '0')}. ${q.text}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = CarbonSubtext,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp)
                                            )
                                            Text(
                                                text = when {
                                                    isYes -> "Sim"
                                                    isNo -> "Não"
                                                    opt != null -> "Isento (${opt.text})"
                                                    else -> "N/A"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = color
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    valueColor: Color,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CarbonMuted
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = valueColor
        )
        Text(
            text = subtext,
            style = MaterialTheme.typography.bodySmall,
            color = CarbonSubtext,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
