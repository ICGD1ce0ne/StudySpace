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
import androidx.core.content.ContextCompat
import com.example.studyspace.R
import com.example.studyspace.managers.TaskManager
import com.example.studyspace.models.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var buttonTaskList: ImageView
    private lateinit var buttonGoal: ImageView
    private lateinit var buttonAnalytic: ImageView
    private lateinit var layoutForNextButton: FrameLayout
    private lateinit var scrollContainer: LinearLayout
    private lateinit var labelFocusTimer: TextView
    private lateinit var labelNameOfTask: TextView
    private lateinit var taskManager: TaskManager

    private var selectedTask: Task? = null
    private var selectedCheckBox: CheckBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        buttonTaskList = findViewById(R.id.buttonTaskList)
        buttonGoal = findViewById(R.id.buttonGoal)
        buttonAnalytic = findViewById(R.id.buttonAnalytic)
        layoutForNextButton = findViewById(R.id.layoutForNextButton)
        labelFocusTimer = findViewById(R.id.labelFocusTimer)
        labelNameOfTask = findViewById(R.id.labelNameOfTask)

        val scrollView = findViewById<ScrollView>(R.id.scrollView3)
        scrollContainer = scrollView.getChildAt(0) as LinearLayout

        taskManager = TaskManager(this)

        initButtons()
        loadFocusTasksWithGroups()
        updateTimerDisplay()
    }

    override fun onResume() {
        super.onResume()
        loadFocusTasksWithGroups()
        updateTimerDisplay()
    }

    private fun initButtons() {
        buttonTaskList.setOnClickListener {
            val goToTaskWindow = Intent(this, TaskWindowActivity::class.java)
            startActivity(goToTaskWindow)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        buttonGoal.setOnClickListener {
            Toast.makeText(this, "Вы уже в главном меню", Toast.LENGTH_SHORT).show()
        }

        buttonAnalytic.setOnClickListener {
            val goToAnalytic = Intent(this, AnalyticWindowActivity::class.java)
            startActivity(goToAnalytic)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Кнопка старта фокуса
        layoutForNextButton.setOnClickListener {
            if (selectedTask != null) {
                startFocusSession()
            } else {
                Toast.makeText(this, "Выберите задачу для фокуса", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFocusTasksWithGroups() {
        scrollContainer.removeAllViews()
        selectedTask = null
        selectedCheckBox = null

        // Получаем активные задачи
        val activeTasks = taskManager.getActiveTasks()

        if (activeTasks.isEmpty()) {
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
        } else {
            // Добавляем заголовок
            val titleTextView = TextView(this).apply {
                text = "Выберите задачу для фокуса"
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

            // Группируем задачи по датам
            val groupedTasks = groupTasksByDisplayDate(activeTasks)

            // Отображаем группы
            for ((groupName, tasks) in groupedTasks) {
                // Заголовок группы
                if (tasks.isNotEmpty()) {
                    val groupTitle = TextView(this).apply {
                        text = groupName
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

                    // Задачи в группе
                    for (task in tasks) {
                        val taskView = createFocusTaskView(task)
                        scrollContainer.addView(taskView)
                    }
                }
            }
        }
    }

    private fun groupTasksByDisplayDate(tasks: List<Task>): Map<String, List<Task>> {
        val groups = mutableMapOf<String, MutableList<Task>>()

        // Предопределенные группы в порядке отображения
        val groupOrder = listOf(
            "Сегодня",
            "Завтра",
            "Послезавтра",
            "Без даты"
        )

        // Создаем пустые группы
        for (group in groupOrder) {
            groups[group] = mutableListOf()
        }

        // Распределяем задачи по группам
        for (task in tasks) {
            val groupName = getTaskGroupName(task)
            groups[groupName]?.add(task)
        }

        // Удаляем пустые группы
        return groups.filter { it.value.isNotEmpty() }
    }

    private fun getTaskGroupName(task: Task): String {
        return when {
            task.deadline.isEmpty() -> "Без даты"
            task.deadline == Task.getTodayDate() -> "Сегодня"
            task.deadline == Task.getTomorrowDate() -> "Завтра"
            task.deadline == Task.getAfterTomorrowDate() -> "Послезавтра"
            else -> {
                // Для остальных дат определяем на сколько дней вперед
                try {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val taskDate = dateFormat.parse(task.deadline)
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
                } catch (e: Exception) {
                    // Ошибка парсинга даты
                }
                task.deadline // Если не удалось определить, показываем просто дату
            }
        }
    }

    private fun createFocusTaskView(task: Task): View {
        val inflater = LayoutInflater.from(this)
        val taskView = inflater.inflate(R.layout.fragment_task_focus_window, scrollContainer, false)

        val taskNameLabel = taskView.findViewById<TextView>(R.id.labelForTaskName)
        val checkBox = taskView.findViewById<CheckBox>(R.id.chooseTaskFocus)
        val taskLayout = taskView.findViewById<FrameLayout>(R.id.layoutForTask)

        // Устанавливаем название задачи
        taskNameLabel.text = task.title

        // Если есть время, добавляем его к названию
        val taskText = if (task.time.isNotEmpty()) {
            "${task.title} (${task.time})"
        } else {
            task.title
        }
        taskNameLabel.text = taskText

        // Обработчик клика по карточке задачи
        taskLayout.setOnClickListener {
            checkBox.isChecked = !checkBox.isChecked
        }

        // Обработчик изменения состояния чекбокса
        checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // Если выбран этот чекбокс, снимаем выделение с других
                uncheckOtherCheckboxes(checkBox)
                selectedTask = task
                selectedCheckBox = checkBox
                updateTimerDisplay()
            } else {
                if (selectedTask == task) {
                    selectedTask = null
                    selectedCheckBox = null
                    updateTimerDisplay()
                }
            }
        }

        return taskView
    }

    private fun uncheckOtherCheckboxes(currentCheckBox: CheckBox) {
        // Проходим по всем чекбоксам в контейнере
        for (i in 0 until scrollContainer.childCount) {
            val child = scrollContainer.getChildAt(i)
            val checkBox = child.findViewById<CheckBox?>(R.id.chooseTaskFocus)
            if (checkBox != null && checkBox != currentCheckBox) {
                checkBox.isChecked = false
            }
        }
    }

    private fun updateTimerDisplay() {
        if (selectedTask != null) {
            labelNameOfTask.text = selectedTask?.title ?: "Название задачи"

            // Форматируем время для таймера
            val timeText = if (selectedTask?.time?.isNotEmpty() == true) {
                selectedTask?.time ?: "25:00"
            } else {
                "25:00" // Значение по умолчанию если время не задано
            }
            labelFocusTimer.text = timeText

            // Обновляем текст кнопки
            val textNextButton = findViewById<TextView>(R.id.textNextButton)
            textNextButton.text = "Старт фокуса"
        } else {
            labelNameOfTask.text = "Нет текущих сессий"
            labelFocusTimer.text = "--:--"

            // Обновляем текст кнопки
            val textNextButton = findViewById<TextView>(R.id.textNextButton)
            textNextButton.text = "Старт"
        }
    }

    private fun startFocusSession() {
        if (selectedTask != null) {
            val taskName = selectedTask?.title ?: "Задача"
            val time = selectedTask?.time ?: "25:00"

            // Здесь можно запустить таймер фокуса
            Toast.makeText(this, "Запуск фокуса на задачу: $taskName\nВремя: $time", Toast.LENGTH_LONG).show()

            // TODO: Реализовать логику таймера фокуса
            // Например, запустить Service или использовать CountDownTimer
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}