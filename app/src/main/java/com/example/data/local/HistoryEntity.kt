package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.ExamStatus
import com.example.data.HistoryEntry

@Entity(tableName = "exam_history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val epochMillis: Long,
    val score: Int,
    val status: String,
    val answersEncoded: String
) {
    fun toDomain(): HistoryEntry {
        val parsedAnswers = answersEncoded
            .split(";")
            .mapNotNull { token ->
                val parts = token.split("=")
                if (parts.size == 2) {
                    val key = parts[0].toIntOrNull()
                    if (key != null) key to parts[1] else null
                } else null
            }
            .toMap()

        return HistoryEntry(
            id = id,
            epochMillis = epochMillis,
            score = score,
            status = if (status == "MOGGER") ExamStatus.MOGGER else ExamStatus.MOGGED,
            answers = parsedAnswers
        )
    }

    companion object {
        fun fromDomain(entry: HistoryEntry): HistoryEntity {
            val encoded = entry.answers.entries
                .sortedBy { it.key }
                .joinToString(";") { "${it.key}=${it.value}" }
            return HistoryEntity(
                id = entry.id,
                epochMillis = entry.epochMillis,
                score = entry.score,
                status = entry.status.name,
                answersEncoded = encoded
            )
        }
    }
}
