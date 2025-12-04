// Task.kt
package com.example.studyspace.models

import java.io.Serializable
import java.text.SimpleDateFormat
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

    fun getDeadlineDisplayName(): String {
        return when (deadline) {
            getTodayDate() -> "Сегодня"
            getTomorrowDate() -> "Завтра"
            getAfterTomorrowDate() -> "Послезавтра"
            else -> deadline
        }
    }

    companion object {
        fun getTodayDate(): String {
            val calendar = java.util.Calendar.getInstance()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return dateFormat.format(calendar.time)
        }

        fun getTomorrowDate(): String {
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return dateFormat.format(calendar.time)
        }

        fun getAfterTomorrowDate(): String {
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 2)
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return dateFormat.format(calendar.time)
        }
    }
}