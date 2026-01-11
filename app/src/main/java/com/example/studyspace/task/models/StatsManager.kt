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
    }

    // Сохранить сессию фокуса
    fun saveSession(session: FocusSession) {
        val sessions = getSessions().toMutableList()
        sessions.add(session)
        saveSessions(sessions)

        // Обновляем ежедневную статистику
        updateDailyStats(session)

        // Обновляем стрик только если сессия завершена
        if (session.isCompleted && session.duration > 0) {
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

        // Текущий стрик (расчитываем заново для точности)
        val currentStreak = calculateCurrentStreak()

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

    // Получить календарь для месяца
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
                completedTasks = 0
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

    // Рассчитать текущий стрик (дни подряд, когда были завершенные сессии фокуса)
    private fun calculateCurrentStreak(): Int {
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)

        // Получаем все дни со статистикой
        val dailyStats = getDailyStats().sortedByDescending { it.date }
        if (dailyStats.isEmpty()) return 0

        var streak = 0
        var currentDate = today

        // Проверяем сегодняшний день
        val todayStat = dailyStats.find { it.date == today }
        if (todayStat == null || todayStat.completedSessions == 0) {
            return 0 // Сегодня не было фокуса - стрик 0
        }

        streak = 1 // Сегодня был фокус

        // Проверяем предыдущие дни
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        var previousDate = dateFormat.format(calendar.time)

        while (true) {
            val prevStat = dailyStats.find { it.date == previousDate }
            if (prevStat != null && prevStat.completedSessions > 0) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                previousDate = dateFormat.format(calendar.time)
            } else {
                break
            }
        }

        return streak
    }

    // Получить текущий стрик (из сохраненных данных)
    fun getCurrentStreak(): Int {
        // Всегда пересчитываем для точности
        return calculateCurrentStreak()
    }

    // Получить самый длинный стрик
    fun getLongestStreak(): Int {
        return sharedPreferences.getInt(KEY_LONGEST_STREAK, 0)
    }

    // Обновить стрик
    private fun updateStreak(sessionTime: Long) {
        val sessionDate = dateFormat.format(Date(sessionTime))
        val lastFocusDate = sharedPreferences.getString(KEY_LAST_FOCUS_DATE, null)
        val lastActiveDate = sharedPreferences.getString(KEY_LAST_ACTIVE_DATE, null)

        // Сохраняем дату последней активности
        sharedPreferences.edit().putString(KEY_LAST_ACTIVE_DATE, sessionDate).apply()

        if (lastFocusDate == null) {
            // Первый фокус
            sharedPreferences.edit()
                .putString(KEY_LAST_FOCUS_DATE, sessionDate)
                .putInt(KEY_STREAK, 1)
                .apply()
        } else {
            try {
                val lastDate = dateFormat.parse(lastFocusDate)
                val currentDate = dateFormat.parse(sessionDate)

                if (lastDate != null && currentDate != null) {
                    // Разница в днях между последним фокусом и текущим
                    val diff = currentDate.time - lastDate.time
                    val daysDiff = diff / (1000 * 60 * 60 * 24)

                    when {
                        daysDiff == 1L -> {
                            // Последовательные дни с фокусом
                            val newStreak = sharedPreferences.getInt(KEY_STREAK, 0) + 1
                            sharedPreferences.edit()
                                .putInt(KEY_STREAK, newStreak)
                                .putString(KEY_LAST_FOCUS_DATE, sessionDate)
                                .apply()

                            // Обновляем самый длинный стрик
                            val longestStreak = getLongestStreak()
                            if (newStreak > longestStreak) {
                                sharedPreferences.edit()
                                    .putInt(KEY_LONGEST_STREAK, newStreak)
                                    .apply()
                            }
                        }
                        daysDiff == 0L -> {
                            // Та же дата - не увеличиваем стрик, но обновляем дату
                            sharedPreferences.edit()
                                .putString(KEY_LAST_FOCUS_DATE, sessionDate)
                                .apply()
                        }
                        else -> {
                            // Пропуск дня или больше - проверяем был ли вчера фокус
                            val yesterday = Calendar.getInstance().apply {
                                time = currentDate
                                add(Calendar.DAY_OF_YEAR, -1)
                            }
                            val yesterdayStr = dateFormat.format(yesterday.time)

                            // Проверяем, была ли сессия вчера
                            val hadFocusYesterday = getDailyStats().any {
                                it.date == yesterdayStr && it.completedSessions > 0
                            }

                            if (!hadFocusYesterday) {
                                // Вчера не было фокуса - сбрасываем стрик
                                sharedPreferences.edit()
                                    .putInt(KEY_STREAK, 1)
                                    .putString(KEY_LAST_FOCUS_DATE, sessionDate)
                                    .apply()
                            } else {
                                // Сбой в записи - сохраняем
                                val newStreak = sharedPreferences.getInt(KEY_STREAK, 0) + 1
                                sharedPreferences.edit()
                                    .putInt(KEY_STREAK, newStreak)
                                    .putString(KEY_LAST_FOCUS_DATE, sessionDate)
                                    .apply()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Проверить, был ли фокус в конкретный день
    fun hadFocusOnDay(dateStr: String): Boolean {
        return getDailyStats().any { it.date == dateStr && it.completedSessions > 0 }
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

    // Очистить все статистики
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