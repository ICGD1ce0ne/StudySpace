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
        private const val KEY_LAST_FOCUS_DATE = "last_focus_date"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_LAST_ACTIVE_DATE = "last_active_date"
        private const val KEY_MONTHLY_STATS = "monthly_stats" // Добавлено для разделения по месяцам
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

    // МЕСЯЧНАЯ СТАТИСТИКА - исправленный метод
    fun getCurrentMonthStats(): MonthStats {
        val calendar = Calendar.getInstance()
        val monthYear = String.format(Locale.getDefault(), "%02d.%d",
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR))

        return getMonthStats(monthYear)
    }

    fun getMonthStats(monthYear: String): MonthStats {
        // Получаем сессии только для указанного месяца
        val sessions = getSessionsForMonth(monthYear)
        val dailyStats = getDailyStatsForMonth(monthYear)
        val taskManager = TaskManager(context)

        // Расчитываем статистику ТОЛЬКО для этого месяца
        val totalFocusTime = sessions.sumOf { it.duration }
        val completedSessions = sessions.count { it.isCompleted }
        val longestSession = sessions.maxOfOrNull { it.duration } ?: 0L

        // Максимальное время фокуса за день в этом месяце
        val maxDailyFocusTime = dailyStats.maxOfOrNull { it.totalFocusTime } ?: 0L

        // Стрик рассчитываем на основе всех дней, а не только месяца
        val currentStreak = calculateCurrentStreak()

        // Статистика задач - можно ограничить только задачами с дедлайном в этом месяце
        val allTasks = taskManager.getTasks()
        val completedTasks = allTasks.count { it.isCompleted }
        val taskCompletionRate = if (allTasks.isNotEmpty()) {
            completedTasks.toFloat() / allTasks.size * 100
        } else {
            0f
        }

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

    // ФИКС: Метод для проверки наличия фокуса в определенный день
    fun hadFocusOnDay(dateStr: String): Boolean {
        // Проверяем через dailyStats, так как они уже агрегированы
        val dailyStats = getDailyStats()
        val dailyStat = dailyStats.find { it.date == dateStr }
        return dailyStat != null && dailyStat.completedSessions > 0
    }

    private fun getSessionsForMonth(monthYear: String): List<FocusSession> {
        return getSessions().filter { session ->
            try {
                val date = Date(session.startTime)
                val sessionMonthYear = monthFormat.format(date)
                sessionMonthYear == monthYear
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun getDailyStatsForMonth(monthYear: String): List<DailyStats> {
        val allDailyStats = getDailyStats()
        return allDailyStats.filter { dailyStat ->
            try {
                val date = dateFormat.parse(dailyStat.date)
                date != null && monthFormat.format(date) == monthYear
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

        // Используем метод hadFocusOnDay для проверки наличия фокуса
        for (day in 1..daysInMonth) {
            val dateStr = String.format(Locale.getDefault(), "%02d.%02d.%d", day, month + 1, year)
            val hadFocus = hadFocusOnDay(dateStr) // Исправлено: используем метод hadFocusOnDay

            // Получаем статистику за день для отображения времени
            val dailyStat = getDailyStats().find { it.date == dateStr }

            calendarDays.add(
                CalendarDay(
                    date = dateStr,
                    hasFocusSession = hadFocus,
                    totalFocusTime = dailyStat?.totalFocusTime ?: 0,
                    completedSessions = dailyStat?.completedSessions ?: 0,
                    isToday = dateStr == todayFormatted
                )
            )
        }

        return calendarDays
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

    private fun updateDailyStats(session: FocusSession) {
        val sessionDate = dateFormat.format(Date(session.startTime))
        val dailyStats = getDailyStats().toMutableList()

        val existingStat = dailyStats.find { it.date == sessionDate }
        if (existingStat != null) {
            val updatedStat = existingStat.copy(
                totalFocusTime = existingStat.totalFocusTime + session.duration,
                completedSessions = existingStat.completedSessions +
                        if (session.isCompleted) 1 else 0
            )
            dailyStats.remove(existingStat)
            dailyStats.add(updatedStat)
        } else {
            dailyStats.add(
                DailyStats(
                    date = sessionDate,
                    totalFocusTime = session.duration,
                    completedSessions = if (session.isCompleted) 1 else 0,
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
        }
    }

    // ФИКС: Улучшенный расчет стрика
    private fun calculateCurrentStreak(): Int {
        val dailyStats = getDailyStats()
        if (dailyStats.isEmpty()) return 0

        // Сортируем по дате (новые первыми)
        val sortedStats = dailyStats.sortedByDescending { it.date }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        var streak = 0
        var currentDate = calendar.time

        // Проверяем сегодняшний день
        val todayStr = dateFormat.format(currentDate)
        val todayStat = sortedStats.find { it.date == todayStr }
        if (todayStat?.completedSessions ?: 0 > 0) {
            streak = 1
        } else {
            return 0 // Если сегодня нет фокуса, стрик = 0
        }

        // Проверяем предыдущие дни
        while (true) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val prevDateStr = dateFormat.format(calendar.time)
            val prevStat = sortedStats.find { it.date == prevDateStr }

            if (prevStat?.completedSessions ?: 0 > 0) {
                streak++
            } else {
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
        val hadFocusYesterday = getDailyStats().any {
            it.date == yesterdayStr && it.completedSessions > 0
        }

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