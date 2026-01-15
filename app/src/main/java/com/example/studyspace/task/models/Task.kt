package com.example.studyspace.task.models

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val deadline: String, // Формат: "dd.MM.yyyy"
    val time: String = "", // Формат: "HH:mm"
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
) : Serializable {

    fun getDeadlineDate(): Date {
        val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return format.parse(deadline) ?: Date()
    }

    fun getDeadlineDisplayName(): String = when (deadline) {
        getTodayDate() -> "Сегодня"
        getTomorrowDate() -> "Завтра"
        getAfterTomorrowDate() -> "Послезавтра"
        else -> deadline
    }

    companion object {
        private const val DATE_FORMAT = "dd.MM.yyyy"

        fun getTodayDate(): String = formatDate(Calendar.getInstance())

        fun getTomorrowDate(): String = formatDate(Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        })

        fun getAfterTomorrowDate(): String = formatDate(Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 2)
        })

        private fun formatDate(calendar: Calendar): String {
            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            return dateFormat.format(calendar.time)
        }
    }
}