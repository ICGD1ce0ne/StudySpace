package com.example.studyspace.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.example.studyspace.R
import com.example.studyspace.analytic.AnalyticWindowActivity
import com.example.studyspace.focus.FocusWindowActivity
import com.example.studyspace.task.TaskWindowActivity
import com.example.studyspace.task.models.Task
import com.example.studyspace.task.models.TaskManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // Нижняя навигация
    private lateinit var buttonTaskList: ImageView
    private lateinit var buttonGoal: ImageView
    private lateinit var buttonAnalytic: ImageView

    // Основные элементы
    private lateinit var layoutForNextButton: FrameLayout
    private lateinit var scrollContainer: LinearLayout
    private lateinit var labelFocusTimer: TextView
    private lateinit var labelNameOfTask: TextView

    // Менеджер задач
    private lateinit var taskManager: TaskManager

    // Выбранная задача для фокуса
    private var selectedTask: Task? = null
    private var selectedCheckBox: CheckBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        initViews()
        initManagers()
        initButtons()
        loadFocusTasksWithGroups()
        updateTimerDisplay()
    }

    override fun onResume() {
        super.onResume()
        loadFocusTasksWithGroups()
        updateTimerDisplay()
    }

    private fun initViews() {
        // Нижняя навигация
        buttonTaskList = findViewById(R.id.buttonTaskList)
        buttonGoal = findViewById(R.id.buttonGoal)
        buttonAnalytic = findViewById(R.id.buttonAnalytic)

        // Основные элементы
        layoutForNextButton = findViewById(R.id.layoutForNextButton)
        labelFocusTimer = findViewById(R.id.labelFocusTimer)
        labelNameOfTask = findViewById(R.id.labelNameOfTask)

        // Контейнер для задач
        val scrollView = findViewById<ScrollView>(R.id.scrollView3)
        scrollContainer = scrollView.getChildAt(0) as LinearLayout
    }

    private fun initManagers() {
        taskManager = TaskManager(this)
    }

    private fun initButtons() {
        // Переход к списку задач
        buttonTaskList.setOnClickListener {
            startActivity(Intent(this, TaskWindowActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }


        // Переход к аналитике
        buttonAnalytic.setOnClickListener {
            startActivity(Intent(this, AnalyticWindowActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        // Старт сессии фокуса
        layoutForNextButton.setOnClickListener {
            if (selectedTask != null) {
                startFocusSession()
            } else {
                Toast.makeText(this, "Выберите задачу для фокуса", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
        ContextCompat.startActivity(this, intent, options.toBundle())
    }

    // Загрузка и группировка активных задач
    private fun loadFocusTasksWithGroups() {
        scrollContainer.removeAllViews()
        selectedTask = null
        selectedCheckBox = null

        val activeTasks = taskManager.getActiveTasks()

        if (activeTasks.isEmpty()) {
            showEmptyState()
        } else {
            showTasksWithGroups(activeTasks)
        }
    }

    private fun showEmptyState() {
        val emptyText = TextView(this).apply {
            text = "Нет активных задач\nСоздайте задачи в разделе 'Задачи'"
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            textSize = 16f
            typeface = resources.getFont(R.font.montserrat)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(50)
            }
        }
        scrollContainer.addView(emptyText)
    }

    private fun showTasksWithGroups(tasks: List<Task>) {
        // Заголовок раздела
        addSectionTitle("Выберите задачу для фокуса")

        // Группировка задач
        val groupedTasks = groupTasksByDisplayDate(tasks)

        // Определение порядка отображения групп
        val groupOrder = listOf(
            "Просрочено",
            "Сегодня",
            "Завтра",
            "Послезавтра",
            "На этой неделе",
            "Будущие задачи",
            "Без даты"
        )

        // Отображение групп в порядке приоритета
        for (groupName in groupOrder) {
            val groupTasks = groupedTasks[groupName]
            if (groupTasks != null && groupTasks.isNotEmpty()) {
                addGroupHeader(groupName)
                groupTasks.forEach { task ->
                    addTaskView(task)
                }
            }
        }
    }

    private fun addSectionTitle(title: String) {
        val titleTextView = TextView(this).apply {
            text = title
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            textSize = 18f
            typeface = resources.getFont(R.font.montserrat_semibold)
            gravity = android.view.Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
                marginStart = dpToPx(45)
                marginEnd = dpToPx(45)
            }
        }
        scrollContainer.addView(titleTextView)
    }

    private fun addGroupHeader(header: String) {
        val groupTitle = TextView(this).apply {
            text = header
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            textSize = 16f
            typeface = resources.getFont(R.font.montserrat_medium)
            gravity = android.view.Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(15)
                marginStart = dpToPx(45)
                marginEnd = dpToPx(45)
            }
        }
        scrollContainer.addView(groupTitle)
    }

    private fun addTaskView(task: Task) {
        val taskView = createFocusTaskView(task)
        scrollContainer.addView(taskView)
    }

    // Группировка задач по отображаемым категориям
    private fun groupTasksByDisplayDate(tasks: List<Task>): Map<String, List<Task>> {
        val groups = mutableMapOf<String, MutableList<Task>>()

        tasks.forEach { task ->
            val groupName = determineTaskGroup(task)
            groups.getOrPut(groupName) { mutableListOf() }.add(task)
        }

        return groups
    }

    private fun determineTaskGroup(task: Task): String {
        return when {
            task.deadline.isEmpty() -> "Без даты"
            task.deadline == Task.getTodayDate() -> "Сегодня"
            task.deadline == Task.getTomorrowDate() -> "Завтра"
            task.deadline == Task.getAfterTomorrowDate() -> "Послезавтра"
            else -> calculateDateBasedGroup(task.deadline)
        }
    }

    private fun calculateDateBasedGroup(deadline: String): String {
        return try {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val taskDate = dateFormat.parse(deadline)
            val today = Date()

            if (taskDate != null) {
                val diffInMillis = taskDate.time - today.time
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

                return when {
                    diffInDays < 0 -> "Просрочено"
                    diffInDays <= 7 -> "На этой неделе"
                    else -> "Будущие задачи"
                }
            }
            "Будущие задачи"
        } catch (e: Exception) {
            "Будущие задачи" // При ошибке парсинга относим к будущим задачам
        }
    }

    // Создание view для задачи
    private fun createFocusTaskView(task: Task): View {
        val inflater = LayoutInflater.from(this)
        val taskView = inflater.inflate(R.layout.fragment_task_focus_window, scrollContainer, false)

        val taskNameLabel = taskView.findViewById<TextView>(R.id.labelForTaskName)
        val checkBox = taskView.findViewById<CheckBox>(R.id.chooseTaskFocus)
        val taskLayout = taskView.findViewById<FrameLayout>(R.id.layoutForTask)

        // Установка названия задачи с временем
        taskNameLabel.text = formatTaskDisplayText(task)

        // Обработка клика по карточке задачи
        taskLayout.setOnClickListener {
            checkBox.isChecked = !checkBox.isChecked
        }

        // Обработка выбора чекбокса
        checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            handleCheckboxChange(task, checkBox, isChecked)
        }

        return taskView
    }

    private fun formatTaskDisplayText(task: Task): String {
        return if (task.time.isNotEmpty()) {
            val displayTime = formatTimeForDisplay(task.time)
            "${task.title} ($displayTime)"
        } else {
            task.title
        }
    }

    // Форматирование времени для отображения (всегда в ЧЧ:ММ:СС)
    private fun formatTimeForDisplay(timeString: String): String {
        return when {
            timeString.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$")) -> {
                // Уже в правильном формате ЧЧ:ММ:СС
                timeString
            }
            timeString.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) -> {
                // Формат ЧЧ:ММ -> добавляем секунды
                "$timeString:00"
            }
            timeString.matches(Regex("^[0-9]{1,3}:[0-5][0-9]$")) -> {
                // Формат ММ:СС -> преобразуем в ЧЧ:ММ:СС
                convertMinutesSecondsToHours(timeString)
            }
            else -> "00:25:00" // Значение по умолчанию
        }
    }

    private fun convertMinutesSecondsToHours(timeString: String): String {
        val parts = timeString.split(":")
        val minutes = parts[0].toIntOrNull() ?: 0
        val seconds = parts[1].toIntOrNull() ?: 0
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return String.format("%02d:%02d:%02d", hours, remainingMinutes, seconds)
    }

    private fun handleCheckboxChange(task: Task, checkBox: CheckBox, isChecked: Boolean) {
        if (isChecked) {
            uncheckOtherCheckboxes(checkBox)
            selectedTask = task
            selectedCheckBox = checkBox
            updateTimerDisplay()
        } else if (selectedTask == task) {
            selectedTask = null
            selectedCheckBox = null
            updateTimerDisplay()
        }
    }

    private fun uncheckOtherCheckboxes(currentCheckBox: CheckBox) {
        for (i in 0 until scrollContainer.childCount) {
            val child = scrollContainer.getChildAt(i)
            val checkBox = child.findViewById<CheckBox?>(R.id.chooseTaskFocus)
            if (checkBox != null && checkBox != currentCheckBox) {
                checkBox.isChecked = false
            }
        }
    }

    // Обновление отображения таймера и кнопки
    private fun updateTimerDisplay() {
        if (selectedTask != null) {
            showSelectedTaskInfo()
        } else {
            showNoTaskSelectedInfo()
        }
    }

    private fun showSelectedTaskInfo() {
        labelNameOfTask.text = selectedTask?.title ?: "Название задачи"
        labelFocusTimer.text = getFormattedTimeForTimer()

        val textNextButton = findViewById<TextView>(R.id.textNextButton)
        textNextButton.text = "Старт фокуса"
    }

    private fun showNoTaskSelectedInfo() {
        labelNameOfTask.text = "Нет текущих сессий"
        labelFocusTimer.text = "--:--:--"

        val textNextButton = findViewById<TextView>(R.id.textNextButton)
        textNextButton.text = "Старт"
    }

    private fun getFormattedTimeForTimer(): String {
        return if (selectedTask?.time?.isNotEmpty() == true) {
            formatTimeForDisplay(selectedTask?.time ?: "00:25:00")
        } else {
            "00:25:00" // Значение по умолчанию
        }
    }

    // Старт сессии фокуса
    private fun startFocusSession() {
        selectedTask?.let { task ->
            val time = formatTimeForFocusWindow(task.time)
            val intent = Intent(this, FocusWindowActivity::class.java).apply {
                putExtra(FocusWindowActivity.EXTRA_TASK_ID, task.id)
                putExtra(FocusWindowActivity.EXTRA_TASK_TITLE, task.title)
                putExtra(FocusWindowActivity.EXTRA_TASK_TIME, time)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        } ?: run {
            Toast.makeText(this, "Выберите задачу для фокуса", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTimeForFocusWindow(timeString: String): String {
        // Обеспечиваем, что время передается в формате ЧЧ:ММ:СС
        val formattedTime = formatTimeForDisplay(timeString)

        // Дополнительная проверка формата
        return if (formattedTime.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$"))) {
            formattedTime
        } else {
            "00:25:00" // Фолбэк значение
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}