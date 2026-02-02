package com.example.studyspace.task.models

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class FocusSession(
    val id: String,
    val startTime: Long,
    val duration: Long, // в миллисекундах
    val isCompleted: Boolean,
    val taskId: String? = null
)

data class DailyStats(
    val date: String, // формат dd.MM.yyyy
    val totalFocusTime: Long, // в миллисекундах
    val completedSessions: Int,
    val completedTasks: Int
)

data class MonthStats(
    val monthYear: String, // формат MM.yyyy
    val totalFocusTime: Long, // в миллисекундах
    val completedSessions: Int,
    val completedTasks: Int,
    val longestSession: Long, // в миллисекундах
    val maxDailyFocusTime: Long, // в миллисекундах
    val currentStreak: Int,
    val taskCompletionRate: Float, // 0-100%
    val overdueTasks: Int
)

data class CalendarDay(
    val date: String, // формат dd.MM.yyyy
    val hasFocusSession: Boolean,
    val totalFocusTime: Long, // в миллисекундах
    val completedSessions: Int,
    val isToday: Boolean = false,
    val isSelected: Boolean = false
)

class StatsManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("stats_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MM.yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    companion object {
        private const val KEY_SESSIONS = "focus_sessions"
        private const val KEY_DAILY_STATS = "daily_stats"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST_FOCUS_DATE = "last_focus_date"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_LAST_ACTIVE_DATE = "last_active_date"
    }

    fun saveSession(session: FocusSession) {
        // Сохраняем сессию
        val sessions = getSessions().toMutableList()
        sessions.add(session)
        saveSessions(sessions)

        // Обновляем дневную статистику
        updateDailyStats(session)

        // Обновляем стрик, если сессия завершена
        if (session.isCompleted && session.duration > 0) {
            updateStreak(session.startTime)
        }
    }

    fun getSessions(): List<FocusSession> {
        val json = sharedPreferences.getString(KEY_SESSIONS, null)
        return if (json != null) {
            val type = object : TypeToken<List<FocusSession>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    // ИСПРАВЛЕННЫЙ метод для получения статистики за месяц
    fun getMonthStats(monthYear: String): MonthStats {
        // Получаем все сессии за указанный месяц
        val monthSessions = getSessionsForMonth(monthYear)

        // Получаем задачи для расчета
        val taskManager = TaskManager(context)
        val allTasks = taskManager.getTasks()
        val completedTasks = allTasks.count { it.isCompleted }
        val taskCompletionRate = if (allTasks.isNotEmpty()) {
            completedTasks.toFloat() / allTasks.size * 100
        } else {
            0f
        }

        // Рассчитываем статистику из сессий
        val dailyStatsMap = mutableMapOf<String, DailyFocusData>()

        // Собираем данные по дням
        for (session in monthSessions) {
            if (!session.isCompleted || session.duration <= 0) continue

            val sessionDate = dateFormat.format(Date(session.startTime))
            val dailyData = dailyStatsMap.getOrPut(sessionDate) { DailyFocusData() }

            dailyData.totalTime += session.duration
            dailyData.sessionCount++
            dailyData.longestSession = max(dailyData.longestSession, session.duration)
        }

        // Рассчитываем итоговую статистику
        var totalFocusTime = 0L
        var completedSessions = 0
        var longestSession = 0L
        var maxDailyFocusTime = 0L

        for ((_, dailyData) in dailyStatsMap) {
            totalFocusTime += dailyData.totalTime
            completedSessions += dailyData.sessionCount
            longestSession = max(longestSession, dailyData.longestSession)
            maxDailyFocusTime = max(maxDailyFocusTime, dailyData.totalTime)
        }

        // Получаем стрики
        val currentStreak = calculateCurrentStreak()
        val overdueTasks = taskManager.getOverdueTasks().size

        return MonthStats(
            monthYear = monthYear,
            totalFocusTime = totalFocusTime,
            completedSessions = completedSessions,
            completedTasks = completedTasks,
            longestSession = longestSession,
            maxDailyFocusTime = maxDailyFocusTime,
            currentStreak = currentStreak,
            taskCompletionRate = taskCompletionRate,
            overdueTasks = overdueTasks
        )
    }

    // Вспомогательный класс для сбора дневной статистики
    private data class DailyFocusData(
        var totalTime: Long = 0L,
        var sessionCount: Int = 0,
        var longestSession: Long = 0L
    )

    // ИСПРАВЛЕННЫЙ метод для проверки наличия фокуса в определенный день
    fun hadFocusOnDay(dateStr: String): Boolean {
        // Проверяем через сессии напрямую
        val sessions = getSessions()
        for (session in sessions) {
            if (!session.isCompleted || session.duration <= 0) continue

            val sessionDate = dateFormat.format(Date(session.startTime))
            if (sessionDate == dateStr) {
                return true
            }
        }
        return false
    }

    // ИСПРАВЛЕННЫЙ метод для получения сессий за месяц
    private fun getSessionsForMonth(monthYear: String): List<FocusSession> {
        return getSessions().filter { session ->
            try {
                if (!session.isCompleted || session.duration <= 0) return@filter false

                val date = Date(session.startTime)
                val sessionMonthYear = monthFormat.format(date)
                sessionMonthYear == monthYear
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getCalendarForMonth(year: Int, month: Int): List<CalendarDay> {
        val calendarDays = mutableListOf<CalendarDay>()
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1)
        }

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayFormatted = dateFormat.format(Date())

        for (day in 1..daysInMonth) {
            val dateStr = String.format(Locale.getDefault(), "%02d.%02d.%d", day, month + 1, year)

            // Получаем статистику за день
            val daySessions = getSessionsForDay(dateStr)
            val hadFocus = daySessions.isNotEmpty()
            val totalFocusTime = daySessions.sumOf { it.duration }
            val completedSessions = daySessions.count { it.isCompleted }

            calendarDays.add(
                CalendarDay(
                    date = dateStr,
                    hasFocusSession = hadFocus,
                    totalFocusTime = totalFocusTime,
                    completedSessions = completedSessions,
                    isToday = dateStr == todayFormatted
                )
            )
        }

        return calendarDays
    }

    // Новый метод: получение сессий за конкретный день
    private fun getSessionsForDay(dateStr: String): List<FocusSession> {
        return getSessions().filter { session ->
            try {
                if (!session.isCompleted || session.duration <= 0) return@filter false

                val sessionDate = dateFormat.format(Date(session.startTime))
                sessionDate == dateStr
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getCurrentMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
    }

    fun getMonthName(month: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    fun getWeekDays(): List<String> = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    // ИСПРАВЛЕННЫЙ метод обновления дневной статистики
    private fun updateDailyStats(session: FocusSession) {
        if (!session.isCompleted || session.duration <= 0) return

        val sessionDate = dateFormat.format(Date(session.startTime))
        val dailyStats = getDailyStats().toMutableList()

        val existingStat = dailyStats.find { it.date == sessionDate }
        if (existingStat != null) {
            val updatedStat = existingStat.copy(
                totalFocusTime = existingStat.totalFocusTime + session.duration,
                completedSessions = existingStat.completedSessions + 1
            )
            dailyStats.remove(existingStat)
            dailyStats.add(updatedStat)
        } else {
            dailyStats.add(
                DailyStats(
                    date = sessionDate,
                    totalFocusTime = session.duration,
                    completedSessions = 1,
                    completedTasks = 0
                )
            )
        }

        saveDailyStats(dailyStats)
    }

    fun updateCompletedTasks(date: String, count: Int) {
        val dailyStats = getDailyStats().toMutableList()
        val existingStat = dailyStats.find { it.date == date }

        existingStat?.let {
            val updatedStat = it.copy(completedTasks = count)
            dailyStats.remove(it)
            dailyStats.add(updatedStat)
            saveDailyStats(dailyStats)
        } ?: run {
            // Создаем новую запись, если нет статистики за этот день
            dailyStats.add(
                DailyStats(
                    date = date,
                    totalFocusTime = 0,
                    completedSessions = 0,
                    completedTasks = count
                )
            )
            saveDailyStats(dailyStats)
        }
    }

    // ИСПРАВЛЕННЫЙ расчет стрика
    private fun calculateCurrentStreak(): Int {
        // Получаем дни с фокусом
        val focusDays = mutableSetOf<String>()

        // Собираем дни из сессий
        getSessions().forEach { session ->
            if (session.isCompleted && session.duration > 0) {
                val sessionDate = dateFormat.format(Date(session.startTime))
                focusDays.add(sessionDate)
            }
        }

        // Также добавляем дни из dailyStats (на всякий случай)
        getDailyStats().forEach { dailyStat ->
            if (dailyStat.completedSessions > 0) {
                focusDays.add(dailyStat.date)
            }
        }

        if (focusDays.isEmpty()) return 0

        // Преобразуем в список и сортируем
        val sortedDays = focusDays.toList().sortedDescending()

        // Проверяем сегодняшний день
        val today = dateFormat.format(Date())
        if (!sortedDays.contains(today)) {
            // Если сегодня нет фокуса, стрик = 0
            return 0
        }

        // Проверяем последовательные дни
        var streak = 1
        val calendar = Calendar.getInstance()

        for (i in 1 until sortedDays.size) {
            try {
                val currentDate = dateFormat.parse(sortedDays[i - 1])
                val prevDate = dateFormat.parse(sortedDays[i])

                if (currentDate != null && prevDate != null) {
                    calendar.time = currentDate
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val expectedPrevDate = dateFormat.format(calendar.time)

                    if (expectedPrevDate == sortedDays[i]) {
                        streak++
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                break
            }
        }

        return streak
    }

    fun getCurrentStreak(): Int = calculateCurrentStreak()

    fun getLongestStreak(): Int = sharedPreferences.getInt(KEY_LONGEST_STREAK, 0)

    private fun updateStreak(sessionTime: Long) {
        val sessionDate = dateFormat.format(Date(sessionTime))
        val lastFocusDate = sharedPreferences.getString(KEY_LAST_FOCUS_DATE, null)

        sharedPreferences.edit().putString(KEY_LAST_ACTIVE_DATE, sessionDate).apply()

        if (lastFocusDate == null) {
            sharedPreferences.edit()
                .putString(KEY_LAST_FOCUS_DATE, sessionDate)
                .putInt(KEY_STREAK, 1)
                .apply()
        } else {
            try {
                val lastDate = dateFormat.parse(lastFocusDate)
                val currentDate = dateFormat.parse(sessionDate)

                if (lastDate != null && currentDate != null) {
                    handleStreakUpdate(sessionDate, lastDate, currentDate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleStreakUpdate(sessionDate: String, lastDate: Date, currentDate: Date) {
        val diff = currentDate.time - lastDate.time
        val daysDiff = diff / (1000 * 60 * 60 * 24)

        when {
            daysDiff == 1L -> incrementStreak(sessionDate)
            daysDiff == 0L -> updateLastFocusDate(sessionDate)
            else -> handleStreakBreak(sessionDate, currentDate)
        }
    }

    private fun incrementStreak(sessionDate: String) {
        val newStreak = sharedPreferences.getInt(KEY_STREAK, 0) + 1
        sharedPreferences.edit()
            .putInt(KEY_STREAK, newStreak)
            .putString(KEY_LAST_FOCUS_DATE, sessionDate)
            .apply()

        val longestStreak = getLongestStreak()
        if (newStreak > longestStreak) {
            sharedPreferences.edit().putInt(KEY_LONGEST_STREAK, newStreak).apply()
        }
    }

    private fun updateLastFocusDate(sessionDate: String) {
        sharedPreferences.edit()
            .putString(KEY_LAST_FOCUS_DATE, sessionDate)
            .apply()
    }

    private fun handleStreakBreak(sessionDate: String, currentDate: Date) {
        val yesterday = Calendar.getInstance().apply {
            time = currentDate
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayStr = dateFormat.format(yesterday.time)

        // Проверяем, был ли фокус вчера через сессии
        val hadFocusYesterday = hadFocusOnDay(yesterdayStr)

        if (!hadFocusYesterday) {
            sharedPreferences.edit()
                .putInt(KEY_STREAK, 1)
                .putString(KEY_LAST_FOCUS_DATE, sessionDate)
                .apply()
        } else {
            incrementStreak(sessionDate)
        }
    }

    private fun getDailyStats(): List<DailyStats> {
        val json = sharedPreferences.getString(KEY_DAILY_STATS, null)
        return if (json != null) {
            val type = object : TypeToken<List<DailyStats>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun saveDailyStats(stats: List<DailyStats>) {
        val json = gson.toJson(stats)
        sharedPreferences.edit().putString(KEY_DAILY_STATS, json).apply()
    }

    private fun saveSessions(sessions: List<FocusSession>) {
        val json = gson.toJson(sessions)
        sharedPreferences.edit().putString(KEY_SESSIONS, json).apply()
    }

    fun clearStats() {
        sharedPreferences.edit()
            .remove(KEY_SESSIONS)
            .remove(KEY_DAILY_STATS)
            .remove(KEY_STREAK)
            .remove(KEY_LAST_FOCUS_DATE)
            .remove(KEY_LONGEST_STREAK)
            .remove(KEY_LAST_ACTIVE_DATE)
            .apply()
    }
}