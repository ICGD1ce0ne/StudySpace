package com.example.studyspace.analytic

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studyspace.R
import com.example.studyspace.main.MainActivity
import com.example.studyspace.task.TaskWindowActivity
import com.example.studyspace.task.models.StatsManager
import com.example.studyspace.task.models.TaskManager
import java.util.*

class AnalyticWindowActivity : AppCompatActivity() {

    private lateinit var buttonTaskList: ImageView
    private lateinit var buttonGoal: ImageView
    private lateinit var buttonAnalytic: ImageView

    // Статистика элементы
    private lateinit var tvTotalFocusTime: TextView
    private lateinit var tvCompletedSessions: TextView
    private lateinit var tvLongestSession: TextView
    private lateinit var tvMaxDailyFocus: TextView
    private lateinit var tvCurrentStreak: TextView
    private lateinit var tvLongestStreak: TextView
    private lateinit var tvTaskCompletionRate: TextView
    private lateinit var tvOverdueTasks: TextView

    // Календарь элементы
    private lateinit var tvMonthTitle: TextView
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView
    private lateinit var recyclerViewCalendar: RecyclerView
    private lateinit var weekDaysContainer: ViewGroup

    private lateinit var statsManager: StatsManager
    private lateinit var taskManager: TaskManager

    private var currentYear: Int = 0
    private var currentMonth: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytic_window)

        buttonTaskList = findViewById(R.id.buttonTaskList)
        buttonGoal = findViewById(R.id.buttonGoal)
        buttonAnalytic = findViewById(R.id.buttonAnalytic)

        // Инициализация элементов статистики
        tvTotalFocusTime = findViewById(R.id.tvTotalFocusTime)
        tvCompletedSessions = findViewById(R.id.tvCompletedSessions)
        tvLongestSession = findViewById(R.id.tvLongestSession)
        tvMaxDailyFocus = findViewById(R.id.tvMaxDailyFocus)
        tvCurrentStreak = findViewById(R.id.tvCurrentStreak)
        tvLongestStreak = findViewById(R.id.tvLongestStreak)
        tvTaskCompletionRate = findViewById(R.id.tvTaskCompletionRate)
        tvOverdueTasks = findViewById(R.id.tvOverdueTasks)

        // Инициализация элементов календаря
        tvMonthTitle = findViewById(R.id.tvMonthTitle)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        recyclerViewCalendar = findViewById(R.id.recyclerViewCalendar)
        weekDaysContainer = findViewById(R.id.weekDaysContainer)

        statsManager = StatsManager(this)
        taskManager = TaskManager(this)

        initButtons()
        initCalendar()
        loadStatistics()
        setupWeekDays()
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
        loadCalendar()
    }

    private fun initButtons() {
        buttonTaskList.setOnClickListener {
            val goToMainMenu = Intent(this, TaskWindowActivity::class.java)
            startActivity(goToMainMenu)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        buttonGoal.setOnClickListener {
            val goToMainMenu = Intent(this, MainActivity::class.java)
            startActivity(goToMainMenu)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        buttonAnalytic.setOnClickListener {
            Toast.makeText(this, "Вы уже в аналитике", Toast.LENGTH_SHORT).show()
        }

        // Кнопки навигации по месяцам
        btnPrevMonth.setOnClickListener {
            currentMonth--
            if (currentMonth < 0) {
                currentMonth = 11
                currentYear--
            }
            loadCalendar()
        }

        btnNextMonth.setOnClickListener {
            currentMonth++
            if (currentMonth > 11) {
                currentMonth = 0
                currentYear++
            }
            loadCalendar()
        }
    }

    private fun initCalendar() {
        // Устанавливаем текущий месяц
        val (year, month) = statsManager.getCurrentMonthYear()
        currentYear = year
        currentMonth = month

        // Настраиваем RecyclerView для календаря
        recyclerViewCalendar.layoutManager = GridLayoutManager(this, 7)
        recyclerViewCalendar.adapter = CalendarAdapter(emptyList())

        loadCalendar()
    }

    private fun setupWeekDays() {
        val weekDays = statsManager.getWeekDays()

        // Очищаем контейнер
        weekDaysContainer.removeAllViews()

        // Добавляем дни недели
        for (day in weekDays) {
            val dayView = LayoutInflater.from(this).inflate(R.layout.week_day_item, weekDaysContainer, false)
            val tvDay = dayView.findViewById<TextView>(R.id.tvWeekDay)
            tvDay.text = day

            // Разные цвета для выходных
            if (day == "Сб" || day == "Вс") {
                tvDay.setTextColor(ContextCompat.getColor(this, R.color.red_light))
            } else {
                tvDay.setTextColor(ContextCompat.getColor(this, R.color.light_gray))
            }

            weekDaysContainer.addView(dayView)
        }
    }

    private fun loadCalendar() {
        // Обновляем заголовок месяца
        tvMonthTitle.text = statsManager.getMonthName(currentMonth)

        // Получаем дни для месяца
        val calendarDays = statsManager.getCalendarForMonth(currentYear, currentMonth)

        // Настраиваем адаптер
        recyclerViewCalendar.adapter = CalendarAdapter(calendarDays).apply {
            setOnDayClickListener { day ->
                showDayDetails(day)
            }
        }
    }

    private fun loadStatistics() {
        val monthStats = statsManager.getCurrentMonthStats()

        // 1. Общее время в фокусе
        val totalHours = monthStats.totalFocusTime / (1000 * 60 * 60)
        val totalMinutes = (monthStats.totalFocusTime % (1000 * 60 * 60)) / (1000 * 60)
        tvTotalFocusTime.text = "${totalHours}ч ${totalMinutes}м"

        // 2. Количество завершенных сессий
        tvCompletedSessions.text = monthStats.completedSessions.toString()

        // 3. Самая длинная сессия
        val longestSessionMinutes = monthStats.longestSession / (1000 * 60)
        tvLongestSession.text = "${longestSessionMinutes} мин"

        // 4. Максимальная продуктивность в сутки
        val maxDailyHours = monthStats.maxDailyFocusTime / (1000 * 60 * 60)
        val maxDailyMinutes = (monthStats.maxDailyFocusTime % (1000 * 60 * 60)) / (1000 * 60)
        tvMaxDailyFocus.text = "${maxDailyHours}ч ${maxDailyMinutes}м"

        // 5. Текущий стрик
        tvCurrentStreak.text = "${monthStats.currentStreak} дней"

        // 6. Самый длинный стрик
        val longestStreak = statsManager.getLongestStreak()
        tvLongestStreak.text = "${longestStreak} дней"

        // 7. Процент выполненных задач
        val completionRate = String.format("%.1f", monthStats.taskCompletionRate)
        tvTaskCompletionRate.text = "$completionRate%"

        // 8. Количество просроченных задач
        tvOverdueTasks.text = monthStats.overdueTasks.toString()

        // Изменяем цвет в зависимости от показателей
        updateVisuals(monthStats)
    }

    private fun showDayDetails(day: com.example.studyspace.task.models.CalendarDay) {
        // Показываем детали дня в тосте
        val hours = day.totalFocusTime / (1000 * 60 * 60)
        val minutes = (day.totalFocusTime % (1000 * 60 * 60)) / (1000 * 60)

        val message = if (day.hasFocusSession) {
            "День: ${day.date}\n" +
                    "Время в фокусе: ${hours}ч ${minutes}м\n" +
                    "Завершенных сессий: ${day.completedSessions}"
        } else {
            "День: ${day.date}\n" +
                    "Нет сессий фокуса"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateVisuals(monthStats: com.example.studyspace.task.models.MonthStats) {
        // Для стрика - зеленый если больше 0
        if (monthStats.currentStreak > 0) {
            tvCurrentStreak.setTextColor(ContextCompat.getColor(this, R.color.green))
        } else {
            tvCurrentStreak.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        // Для процента выполненных задач - градиент от красного к зеленому
        when {
            monthStats.taskCompletionRate >= 80 -> {
                tvTaskCompletionRate.setTextColor(ContextCompat.getColor(this, R.color.green))
            }
            monthStats.taskCompletionRate >= 50 -> {
                tvTaskCompletionRate.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            }
            else -> {
                tvTaskCompletionRate.setTextColor(ContextCompat.getColor(this, R.color.red))
            }
        }

        // Для просроченных задач - красный если есть
        if (monthStats.overdueTasks > 0) {
            tvOverdueTasks.setTextColor(ContextCompat.getColor(this, R.color.red))
        } else {
            tvOverdueTasks.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    // Адаптер для календаря с кружками
    inner class CalendarAdapter(private val days: List<com.example.studyspace.task.models.CalendarDay>) :
        RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

        private var onDayClickListener: ((com.example.studyspace.task.models.CalendarDay) -> Unit)? = null

        fun setOnDayClickListener(listener: (com.example.studyspace.task.models.CalendarDay) -> Unit) {
            this.onDayClickListener = listener
        }

        inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.calendar_day_item_simple, parent, false)
            return CalendarViewHolder(view)
        }

        override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
            val day = days[position]
            val dayNumber = day.date.split(".")[0].toIntOrNull() ?: 0

            // Устанавливаем номер дня
            holder.tvDayNumber.text = dayNumber.toString()

            // Проверяем, был ли фокус в этот день
            val hadFocus = day.hasFocusSession && day.completedSessions > 0

            // Настраиваем отображение дня с красивыми кружками
            when {
                day.isToday -> {
                    if (hadFocus) {
                        // Сегодня + был фокус = оранжевый/золотой кружок
                        holder.tvDayNumber.setTextColor(Color.WHITE)
                        holder.tvDayNumber.setBackgroundResource(R.drawable.today_focus_circle)
                    } else {
                        // Сегодня, но без фокуса = зеленый кружок
                        holder.tvDayNumber.setTextColor(Color.WHITE)
                        holder.tvDayNumber.setBackgroundResource(R.drawable.today_circle)
                    }
                }
                hadFocus -> {
                    // Был фокус (но не сегодня) = желтый кружок
                    holder.tvDayNumber.setTextColor(Color.WHITE)
                    holder.tvDayNumber.setBackgroundResource(R.drawable.focus_circle)
                }
                else -> {
                    // Нет фокуса
                    holder.tvDayNumber.setTextColor(Color.parseColor("#CCCCCC"))
                    holder.tvDayNumber.setBackgroundResource(0) // Без кружка
                }
            }

            // Обработчик клика
            holder.itemView.setOnClickListener {
                onDayClickListener?.invoke(day)
            }
        }

        override fun getItemCount(): Int = days.size
    }
}