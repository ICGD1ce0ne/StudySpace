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

    companion object {
        private const val KEY_SESSIONS = "focus_sessions"
        private const val KEY_DAILY_STATS = "daily_stats"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST_SESSION_DATE = "last_session_date"
        private const val KEY_STREAK_START_DATE = "streak_start_date"
        private const val KEY_LONGEST_STREAK = "longest_streak"
    }

    // Сохранить сессию фокуса
    fun saveSession(session: FocusSession) {
        val sessions = getSessions().toMutableList()
        sessions.add(session)
        saveSessions(sessions)

        // Обновляем ежедневную статистику
        updateDailyStats(session)

        // Обновляем стрик
        if (session.isCompleted) {
            updateStreak(session.startTime)
        }
    }

    // Получить все сессии
    fun getSessions(): List<FocusSession> {
        val json = sharedPreferences.getString(KEY_SESSIONS, null)
        return if (json != null) {
            val type = object : TypeToken<List<FocusSession>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Получить статистику за текущий месяц
    fun getCurrentMonthStats(): MonthStats {
        val currentMonth = monthFormat.format(Date())
        return getMonthStats(currentMonth)
    }

    // Получить статистику за конкретный месяц
    fun getMonthStats(monthYear: String): MonthStats {
        val sessions = getSessionsForMonth(monthYear)
        val dailyStats = getDailyStatsForMonth(monthYear)
        val taskManager = TaskManager(context)

        // Общее время фокуса
        val totalFocusTime = sessions.sumOf { it.duration }

        // Количество завершенных сессий
        val completedSessions = sessions.count { it.isCompleted }

        // Самая длинная сессия
        val longestSession = sessions.maxOfOrNull { it.duration } ?: 0L

        // Максимальное время фокуса в сутки
        val maxDailyFocusTime = dailyStats.maxOfOrNull { it.totalFocusTime } ?: 0L

        // Текущий стрик
        val currentStreak = getCurrentStreak()

        // Получаем задачи за месяц
        val allTasks = taskManager.getTasks()
        val completedTasks = allTasks.count { it.isCompleted }
        val taskCompletionRate = if (allTasks.isNotEmpty()) {
            completedTasks.toFloat() / allTasks.size * 100
        } else {
            0f
        }

        // Просроченные задачи
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

    // Получить сессии за месяц
    private fun getSessionsForMonth(monthYear: String): List<FocusSession> {
        return getSessions().filter { session ->
            val date = Date(session.startTime)
            monthFormat.format(date) == monthYear
        }
    }

    // Получить ежедневную статистику за месяц
    private fun getDailyStatsForMonth(monthYear: String): List<DailyStats> {
        val json = sharedPreferences.getString(KEY_DAILY_STATS, null)
        val allDailyStats: List<DailyStats> = if (json != null) {
            val type = object : TypeToken<List<DailyStats>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }

        return allDailyStats.filter { dailyStat ->
            try {
                val date = dateFormat.parse(dailyStat.date)
                date != null && monthFormat.format(date) == monthYear
            } catch (e: Exception) {
                false
            }
        }
    }

    // Получить календарь для месяца (как в Duolingo)
    fun getCalendarForMonth(year: Int, month: Int): List<CalendarDay> {
        val calendarDays = mutableListOf<CalendarDay>()
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)

        // Получаем первый день месяца и количество дней
        val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Получаем сегодняшнюю дату для сравнения
        val today = Calendar.getInstance()
        val todayFormatted = dateFormat.format(today.time)

        // Получаем статистику за дни
        val monthYearStr = String.format(Locale.getDefault(), "%02d.%d", month + 1, year)
        val dailyStats = getDailyStatsForMonth(monthYearStr)
        val dailyStatsMap = dailyStats.associateBy { it.date }

        // Добавляем пустые дни для начала недели (если месяц начинается не с понедельника)
        val startOffset = (firstDayOfMonth - Calendar.MONDAY + 7) % 7

        // Добавляем дни
        for (day in 1..daysInMonth) {
            val dateStr = String.format(Locale.getDefault(), "%02d.%02d.%d", day, month + 1, year)

            val dailyStat = dailyStatsMap[dateStr]
            val hasFocusSession = dailyStat != null && dailyStat.totalFocusTime > 0

            calendarDays.add(CalendarDay(
                date = dateStr,
                hasFocusSession = hasFocusSession,
                totalFocusTime = dailyStat?.totalFocusTime ?: 0,
                completedSessions = dailyStat?.completedSessions ?: 0,
                isToday = dateStr == todayFormatted
            ))
        }

        return calendarDays
    }

    // Получить текущий месяц и год для календаря
    fun getCurrentMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
    }

    // Получить название месяца
    fun getMonthName(month: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, month)
        val date = calendar.time
        val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return monthNameFormat.format(date)
    }

    // Получить список дней недели
    fun getWeekDays(): List<String> {
        return listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    }

    // Обновить ежедневную статистику
    private fun updateDailyStats(session: FocusSession) {
        val sessionDate = dateFormat.format(Date(session.startTime))
        val dailyStats = getDailyStats().toMutableList()

        val existingStat = dailyStats.find { it.date == sessionDate }
        if (existingStat != null) {
            val updatedStat = existingStat.copy(
                totalFocusTime = existingStat.totalFocusTime + session.duration,
                completedSessions = existingStat.completedSessions + (if (session.isCompleted) 1 else 0)
            )
            dailyStats.remove(existingStat)
            dailyStats.add(updatedStat)
        } else {
            dailyStats.add(DailyStats(
                date = sessionDate,
                totalFocusTime = session.duration,
                completedSessions = if (session.isCompleted) 1 else 0,
                completedTasks = 0 // Будет обновляться отдельно
            ))
        }

        saveDailyStats(dailyStats)
    }

    // Обновить количество выполненных задач за день
    fun updateCompletedTasks(date: String, count: Int) {
        val dailyStats = getDailyStats().toMutableList()
        val existingStat = dailyStats.find { it.date == date }

        if (existingStat != null) {
            val updatedStat = existingStat.copy(completedTasks = count)
            dailyStats.remove(existingStat)
            dailyStats.add(updatedStat)
            saveDailyStats(dailyStats)
        }
    }

    // Получить текущий стрик
    fun getCurrentStreak(): Int {
        return sharedPreferences.getInt(KEY_STREAK, 0)
    }

    // Получить самый длинный стрик
    fun getLongestStreak(): Int {
        return sharedPreferences.getInt(KEY_LONGEST_STREAK, 0)
    }

    // Получить историю стриков за последние 30 дней
    fun getStreakHistory(): List<CalendarDay> {
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        val history = mutableListOf<CalendarDay>()

        // Получаем статистику за последние 30 дней
        val dailyStatsList: List<DailyStats> = getDailyStats()
        val filteredStats: List<DailyStats> = dailyStatsList.filter { dailyStat ->
            try {
                val statDate = dateFormat.parse(dailyStat.date)
                val currentDate = dateFormat.parse(today)
                if (statDate != null && currentDate != null) {
                    val diff = currentDate.time - statDate.time
                    val daysDiff = diff / (1000 * 60 * 60 * 24)
                    daysDiff.toInt() in 0..30
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

        val dailyStatsMap: Map<String, DailyStats> = filteredStats.associateBy { it.date }

        // Создаем дни для последних 30 дней
        for (i in 30 downTo 0) {
            val dateCalendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dateStr = dateFormat.format(dateCalendar.time)
            val dailyStat = dailyStatsMap[dateStr]

            history.add(CalendarDay(
                date = dateStr,
                hasFocusSession = dailyStat != null && dailyStat.totalFocusTime > 0,
                totalFocusTime = dailyStat?.totalFocusTime ?: 0,
                completedSessions = dailyStat?.completedSessions ?: 0,
                isToday = i == 0
            ))
        }

        return history
    }

    // Обновить стрик
    private fun updateStreak(sessionTime: Long) {
        val sessionDate = dateFormat.format(Date(sessionTime))
        val lastSessionDate = sharedPreferences.getString(KEY_LAST_SESSION_DATE, null)
        val streakStartDate = sharedPreferences.getString(KEY_STREAK_START_DATE, null)

        if (lastSessionDate == null) {
            // Первая сессия
            sharedPreferences.edit()
                .putInt(KEY_STREAK, 1)
                .putString(KEY_LAST_SESSION_DATE, sessionDate)
                .putString(KEY_STREAK_START_DATE, sessionDate)
                .apply()
        } else {
            try {
                val lastDate = dateFormat.parse(lastSessionDate)
                val currentDate = dateFormat.parse(sessionDate)

                if (lastDate != null && currentDate != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = lastDate
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val nextDay = dateFormat.format(calendar.time)

                    if (sessionDate == nextDay) {
                        // Последовательные дни
                        val newStreak = getCurrentStreak() + 1
                        sharedPreferences.edit()
                            .putInt(KEY_STREAK, newStreak)
                            .putString(KEY_LAST_SESSION_DATE, sessionDate)
                            .apply()

                        // Обновляем самый длинный стрик
                        val longestStreak = getLongestStreak()
                        if (newStreak > longestStreak) {
                            sharedPreferences.edit()
                                .putInt(KEY_LONGEST_STREAK, newStreak)
                                .apply()
                        }
                    } else if (sessionDate != lastSessionDate) {
                        // Сброс стрика
                        sharedPreferences.edit()
                            .putInt(KEY_STREAK, 1)
                            .putString(KEY_LAST_SESSION_DATE, sessionDate)
                            .putString(KEY_STREAK_START_DATE, sessionDate)
                            .apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Получить все ежедневные статистики
    private fun getDailyStats(): List<DailyStats> {
        val json = sharedPreferences.getString(KEY_DAILY_STATS, null)
        return if (json != null) {
            val type = object : TypeToken<List<DailyStats>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Сохранить ежедневные статистики
    private fun saveDailyStats(stats: List<DailyStats>) {
        val json = gson.toJson(stats)
        sharedPreferences.edit().putString(KEY_DAILY_STATS, json).apply()
    }

    // Сохранить сессии
    private fun saveSessions(sessions: List<FocusSession>) {
        val json = gson.toJson(sessions)
        sharedPreferences.edit().putString(KEY_SESSIONS, json).apply()
    }

    // Очистить все статистики (для тестирования)
    fun clearStats() {
        sharedPreferences.edit()
            .remove(KEY_SESSIONS)
            .remove(KEY_DAILY_STATS)
            .remove(KEY_STREAK)
            .remove(KEY_LAST_SESSION_DATE)
            .remove(KEY_STREAK_START_DATE)
            .remove(KEY_LONGEST_STREAK)
            .apply()
    }
}