package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class HabitOption(
    val id: String,
    val text: String
)

data class HabitQuestion(
    val id: Int,
    val shortLabel: String,
    val text: String,
    val options: List<HabitOption>
)

enum class ExamStatus {
    MOGGER,
    MOGGED
}

data class HistoryEntry(
    val id: String,
    val epochMillis: Long,
    val score: Int,
    val status: ExamStatus,
    val answers: Map<Int, String>
)

data class EvolutionSummary(
    val totalExams: Int = 0,
    val latestScore: Int? = null,
    val previousScore: Int? = null,
    val firstScore: Int? = null,
    val recentDelta: Int? = null,
    val overallDelta: Int? = null,
    val averageScore: Int? = null,
    val bestScore: Int? = null,
    val worstScore: Int? = null
)

data class FormattedDateInfo(
    val shortDate: String,
    val fullDate: String,
    val time: String,
    val weekday: String
)

object HabitCatalog {
    val questions: List<HabitQuestion> = listOf(
        HabitQuestion(
            id = 1,
            shortLabel = "ORAÇÃO",
            text = "Oração?",
            options = listOf(HabitOption("A", "Sim"), HabitOption("B", "Não"))
        ),
        HabitQuestion(
            id = 2,
            shortLabel = "LEITURA BÍBLICA",
            text = "Leitura (Bíblia/livro)?",
            options = listOf(HabitOption("A", "Sim"), HabitOption("B", "Não"))
        ),
        HabitQuestion(
            id = 3,
            shortLabel = "TREINO INTENSO",
            text = "Treino?",
            options = listOf(
                HabitOption("A", "Sim"),
                HabitOption("B", "Não"),
                HabitOption("C", "Hoje não era dia")
            )
        ),
        HabitQuestion(
            id = 4,
            shortLabel = "DIETA ALINHADA",
            text = "Dieta?",
            options = listOf(
                HabitOption("A", "Sim"),
                HabitOption("B", "Não"),
                HabitOption("C", "Hoje foi refeição livre")
            )
        ),
        HabitQuestion(
            id = 5,
            shortLabel = "HIDRATAÇÃO OK",
            text = "Bebi água suficiente?",
            options = listOf(HabitOption("A", "Sim"), HabitOption("B", "Não"))
        ),
        HabitQuestion(
            id = 6,
            shortLabel = "FICHA DE ESTUDO",
            text = "Estudei?",
            options = listOf(
                HabitOption("A", "Sim"),
                HabitOption("B", "Não"),
                HabitOption("C", "Hoje não tinha atividade")
            )
        ),
        HabitQuestion(
            id = 7,
            shortLabel = "CRONOGRAMA SONO",
            text = "Dormi no horário planejado?",
            options = listOf(HabitOption("A", "Sim"), HabitOption("B", "Não"))
        ),
        HabitQuestion(
            id = 8,
            shortLabel = "CONTROLAR REDES",
            text = "Evitei perder tempo em excesso nas redes sociais?",
            options = listOf(HabitOption("A", "Sim"), HabitOption("B", "Não"))
        ),
        HabitQuestion(
            id = 9,
            shortLabel = "COMPROMISSOS",
            text = "Cumpri minhas responsabilidades?",
            options = listOf(HabitOption("A", "Sim"), HabitOption("B", "Não"))
        ),
        HabitQuestion(
            id = 10,
            shortLabel = "EVITAR MAUS HÁBITOS",
            text = "Evitei hábitos que quero abandonar?",
            options = listOf(
                HabitOption("A", "Sim"),
                HabitOption("B", "Não"),
                HabitOption("C", "Não fui exposto à situação")
            )
        ),
        HabitQuestion(
            id = 11,
            shortLabel = "ALONGAMENTO",
            text = "Alongou-se?",
            options = listOf(HabitOption("A", "Sim"), HabitOption("B", "Não"))
        ),
        HabitQuestion(
            id = 12,
            shortLabel = "EVITAR RECLAMAR",
            text = "Evitei reclamar desnecessariamente?",
            options = listOf(HabitOption("A", "Sim"), HabitOption("B", "Não"))
        ),
        HabitQuestion(
            id = 13,
            shortLabel = "TREINO FÍSICO DIÁRIO",
            text = "Cumpriu o seu treino físico diário?",
            options = listOf(HabitOption("A", "Sim"), HabitOption("B", "Não"))
        )
    )

    fun calculateScoreFromAnswers(
        answers: Map<Int, String>,
        questionsLength: Int = questions.size
    ): Int {
        if (questionsLength <= 0) return 0
        var totalScore = 0.0
        for (i in 1..questionsLength) {
            when (answers[i]) {
                "A" -> totalScore += 1.0
                "B" -> totalScore += 0.0
                "C" -> totalScore += if (i == 3 || i == 6) 1.0 else 0.5
            }
        }
        return ((totalScore / questionsLength) * 100.0).roundToInt()
    }

    fun computeEvolutionSummary(entries: List<HistoryEntry>): EvolutionSummary {
        if (entries.isEmpty()) return EvolutionSummary()
        val latest = entries.last()
        val previous = if (entries.size >= 2) entries[entries.size - 2] else null
        val first = entries.first()
        val avg = (entries.sumOf { it.score }.toDouble() / entries.size).roundToInt()
        return EvolutionSummary(
            totalExams = entries.size,
            latestScore = latest.score,
            previousScore = previous?.score,
            firstScore = first.score,
            recentDelta = previous?.let { latest.score - it.score },
            overallDelta = if (entries.size >= 2) latest.score - first.score else null,
            averageScore = avg,
            bestScore = entries.maxOf { it.score },
            worstScore = entries.minOf { it.score }
        )
    }

    fun formatEntryDate(epochMillis: Long): FormattedDateInfo {
        val locale = Locale("pt", "BR")
        val date = Date(epochMillis)
        val shortDate = SimpleDateFormat("dd/MM", locale).format(date)
        val fullDate = SimpleDateFormat("dd.MM.yyyy", locale).format(date)
        val time = SimpleDateFormat("HH:mm", locale).format(date)
        val weekday = SimpleDateFormat("EEE", locale)
            .format(date)
            .replace(".", "")
            .uppercase(locale)
        return FormattedDateInfo(
            shortDate = shortDate,
            fullDate = fullDate,
            time = time,
            weekday = weekday
        )
    }
}
