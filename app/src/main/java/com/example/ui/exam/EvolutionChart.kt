package com.example.ui.exam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HabitCatalog
import com.example.data.HistoryEntry
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonMuted
import com.example.ui.theme.CarbonSubtext
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonText
import com.example.ui.theme.EmeraldMogger
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoseMogged
import kotlin.math.abs

enum class ChartRange(val label: String) {
    LAST_7("Últimos 7"),
    LAST_15("Últimos 15"),
    ALL("Todos")
}

@Composable
fun EvolutionChart(
    entries: List<HistoryEntry>,
    onStartExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    var range by remember { mutableStateOf(ChartRange.LAST_15) }
    var selectedEntryId by remember { mutableStateOf<String?>(null) }

    val visibleEntries = remember(entries, range) {
        when (range) {
            ChartRange.LAST_7 -> entries.takeLast(7)
            ChartRange.LAST_15 -> entries.takeLast(15)
            ChartRange.ALL -> entries
        }
    }

    Surface(
        color = CarbonSurface,
        border = BorderStroke(1.dp, CarbonBorder),
        shape = RoundedCornerShape(2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "QUADRO DE EVOLUÇÃO E DESEVOLUÇÃO",
                    style = MaterialTheme.typography.titleMedium,
                    color = CarbonText
                )
                Text(
                    text = "Suas notas ao longo dos exames formarão linhas subindo (evolução) e descendo (desevolução) aqui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CarbonSubtext
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CarbonBlack)
                        .border(1.dp, CarbonBorder)
                        .padding(vertical = 28.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Nenhum exame registrado ainda para traçar a curva de desempenho.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CarbonMuted
                    )
                    Button(
                        onClick = onStartExam,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CarbonText,
                            contentColor = CarbonBlack
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("chart_empty_start_button")
                    ) {
                        Text(
                            text = "FAZER PRIMEIRO EXAME",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            return@Surface
        }

        val startOffset = entries.size - visibleEntries.size
        val activeIndexInVisible = remember(visibleEntries, selectedEntryId) {
            val found = visibleEntries.indexOfFirst { it.id == selectedEntryId }
            if (found >= 0) found else visibleEntries.lastIndex
        }
        val activeEntry = visibleEntries[activeIndexInVisible]
        val activeGlobalIndex = startOffset + activeIndexInVisible
        val activePrevEntry = if (activeGlobalIndex > 0) entries[activeGlobalIndex - 1] else null
        val activeDelta = activePrevEntry?.let { activeEntry.score - it.score }
        val activeDate = HabitCatalog.formatEntryDate(activeEntry.epochMillis)
        val textMeasurer = rememberTextMeasurer()

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Range Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "QUADRO DE EVOLUÇÃO E DESEVOLUÇÃO",
                    style = MaterialTheme.typography.titleMedium,
                    color = CarbonText
                )
                Text(
                    text = "Linhas subindo mostram evolução; linhas descendo mostram desevolução.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CarbonSubtext
                )
                Row(
                    modifier = Modifier
                        .background(CarbonBlack)
                        .border(1.dp, CarbonBorder)
                        .padding(2.dp)
                ) {
                    ChartRange.entries.forEach { opt ->
                        val active = range == opt
                        Box(
                            modifier = Modifier
                                .clickable { range = opt }
                                .background(if (active) CarbonText else Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = opt.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) CarbonBlack else CarbonSubtext
                            )
                        }
                    }
                }
            }

            // Active point readout bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonBlack)
                    .border(1.dp, CarbonBorder)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Exame #${activeGlobalIndex + 1} · ${activeDate.fullDate} às ${activeDate.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CarbonSubtext
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isGood = activeEntry.score >= 60
                        Text(
                            text = "${activeEntry.score}%",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (isGood) EmeraldMogger else RoseMogged
                        )
                        Text(
                            text = activeEntry.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGood) EmeraldMogger else RoseMogged
                        )
                    }
                }
                DeltaBadge(delta = activeDelta, fallbackText = "Marco inicial")
            }

            // Canvas Line Chart
            val yTicks = listOf(100, 75, 60, 50, 25, 0)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .pointerInput(visibleEntries) {
                        detectTapGestures { offset ->
                            if (visibleEntries.isEmpty()) return@detectTapGestures
                            val padLeft = 40.dp.toPx()
                            val padRight = 14.dp.toPx()
                            val chartW = (size.width - padLeft - padRight).coerceAtLeast(1f)
                            val n = visibleEntries.size
                            val closestIdx = if (n == 1) 0 else {
                                (0 until n).minByOrNull { idx ->
                                    val px = padLeft + (idx.toFloat() / (n - 1)) * chartW
                                    abs(px - offset.x)
                                } ?: 0
                            }
                            selectedEntryId = visibleEntries[closestIdx].id
                        }
                    }
            ) {
                val padLeft = 40.dp.toPx()
                val padRight = 14.dp.toPx()
                val padTop = 18.dp.toPx()
                val padBottom = 26.dp.toPx()
                val chartW = (size.width - padLeft - padRight).coerceAtLeast(1f)
                val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                fun yForScore(score: Int): Float =
                    padTop + ((100f - score.coerceIn(0, 100)) / 100f) * chartH

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                // Horizontal grid lines + labels
                yTicks.forEach { tick ->
                    val y = yForScore(tick)
                    val isMeta = tick == 60
                    drawLine(
                        color = if (isMeta) Color(0xFF52525B) else Color(0xFF1A1A20),
                        start = Offset(padLeft, y),
                        end = Offset(size.width - padRight, y),
                        strokeWidth = if (isMeta) 1.5.dp.toPx() else 1.dp.toPx(),
                        pathEffect = if (isMeta) dashEffect else null
                    )
                    drawAxisLabel(
                        textMeasurer = textMeasurer,
                        text = "$tick%",
                        x = 2.dp.toPx(),
                        y = y - 6.dp.toPx(),
                        color = if (isMeta) CarbonSubtext else CarbonMuted
                    )
                }

                // Compute plotted coordinates
                val n = visibleEntries.size
                val coords = visibleEntries.mapIndexed { idx, item ->
                    val x = if (n == 1) padLeft + chartW / 2f else padLeft + (idx.toFloat() / (n - 1)) * chartW
                    val y = yForScore(item.score)
                    Offset(x, y)
                }

                // Area gradient fill under curve
                if (coords.size >= 2) {
                    val baseLineY = padTop + chartH
                    val areaPath = Path().apply {
                        moveTo(coords.first().x, baseLineY)
                        coords.forEach { lineTo(it.x, it.y) }
                        lineTo(coords.last().x, baseLineY)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                            startY = padTop,
                            endY = baseLineY
                        )
                    )
                }

                // Evolution / Devolution segments
                for (idx in 1 until coords.size) {
                    val diff = visibleEntries[idx].score - visibleEntries[idx - 1].score
                    val segColor = when {
                        diff > 0 -> EmeraldMogger
                        diff < 0 -> RoseMogged
                        else -> CarbonSubtext
                    }
                    drawLine(
                        color = segColor,
                        start = coords[idx - 1],
                        end = coords[idx],
                        strokeWidth = 2.75.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Active vertical hairline
                val activeCoord = coords[activeIndexInVisible]
                drawLine(
                    color = CarbonBorder,
                    start = Offset(activeCoord.x, padTop),
                    end = Offset(activeCoord.x, padTop + chartH),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                // Nodes & bottom labels
                coords.forEachIndexed { idx, pt ->
                    val entry = visibleEntries[idx]
                    val isActive = idx == activeIndexInVisible
                    val nodeColor = if (entry.score >= 60) EmeraldMogger else RoseMogged

                    if (isActive) {
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.35f),
                            radius = 8.dp.toPx(),
                            center = pt,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = CarbonBlack,
                        radius = if (isActive) 5.dp.toPx() else 3.8.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = nodeColor,
                        radius = if (isActive) 5.dp.toPx() else 3.8.dp.toPx(),
                        center = pt,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val showX = n <= 6 || idx == 0 || idx == n - 1 || isActive
                    if (showX) {
                        val dateStr = HabitCatalog.formatEntryDate(entry.epochMillis).shortDate
                        drawAxisLabel(
                            textMeasurer = textMeasurer,
                            text = "#${startOffset + idx + 1} $dateStr",
                            x = (pt.x - 18.dp.toPx()).coerceIn(padLeft, size.width - 54.dp.toPx()),
                            y = padTop + chartH + 6.dp.toPx(),
                            color = if (isActive) CarbonText else CarbonMuted
                        )
                    }
                }
            }

            if (entries.size == 1) {
                Text(
                    text = "Primeiro registro marcado como ponto inicial. Ao concluir o próximo exame, o gráfico traçará a curva de evolução ou desevolução.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CarbonSubtext
                )
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendChip(color = EmeraldMogger, label = "Subida: Evolução")
                LegendChip(color = RoseMogged, label = "Descida: Desevolução")
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAxisLabel(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color
) {
    val result = textMeasurer.measure(
        text = text,
        style = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = 9.sp,
            color = color
        )
    )
    drawText(
        textLayoutResult = result,
        topLeft = Offset(x, y)
    )
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(2.5.dp)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = CarbonSubtext
        )
    }
}
