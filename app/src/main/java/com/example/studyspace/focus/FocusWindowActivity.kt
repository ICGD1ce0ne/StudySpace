package com.example.studyspace.focus

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.studyspace.R
import com.example.studyspace.main.MainActivity
import com.example.studyspace.task.models.FocusSession
import com.example.studyspace.task.models.StatsManager
import com.example.studyspace.task.models.Task
import com.example.studyspace.task.models.TaskManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FocusWindowActivity : AppCompatActivity() {

    // UI элементы
    private lateinit var labelFocus: TextView
    private lateinit var labelNameOfTask: TextView
    private lateinit var labelFocusTimer: TextView
    private lateinit var layoutForStopButton: FrameLayout
    private lateinit var textStopButton: TextView

    // Менеджеры данных
    private lateinit var taskManager: TaskManager

    // Воспроизведение звуков
    private var mediaPlayer: MediaPlayer? = null

    // Состояние таймера
    private var selectedTask: Task? = null
    private var countDownTimer: CountDownTimer? = null
    private var remainingTimeMillis: Long = 0
    private var totalFocusTimeMillis: Long = 0 // Общее время фокуса
    private var currentBlockTimeMillis: Long = 0 // Время текущего блока
    private var isTimerRunning = false
    private var isBreakTime = false // true = сейчас перерыв, false = работа
    private var currentBlock = 0 // Текущий блок (0, 1, 2...)
    private var totalBlocks = 3 // Всего блоков работы
    private var breakDuration = 5 // Длительность перерыва в минутах
    private var workBlockDuration = 0 // Длительность рабочего блока в минутах
    private var userAge = 25
    private var isLastMinute = false

    companion object {
        // Ключи для Intent
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_TIME = "extra_task_time"

        // Ключи для SharedPreferences
        const val PREFS_FOCUS = "focus_prefs"
        private const val KEY_REMAINING_TIME = "remaining_time"
        private const val KEY_TASK_ID = "task_id"
        private const val KEY_IS_BREAK = "is_break"
        private const val KEY_CURRENT_BLOCK = "current_block"
        private const val KEY_TOTAL_FOCUS_TIME = "total_focus_time"
        private const val KEY_CURRENT_BLOCK_TIME = "current_block_time"

        // Пользовательские настройки
        private const val USER_PREFS = "user_preferences"
        private const val KEY_USER_AGE = "user_age"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_window)

        initViews()
        initManagers()
        loadUserAge()
        loadTaskData()
        calculateDurations()
        loadAndSetupTimer()
        setupListeners()
    }

    // Инициализация UI элементов
    private fun initViews() {
        labelFocus = findViewById(R.id.labelFocus)
        labelNameOfTask = findViewById(R.id.labelNameOfTask)
        labelFocusTimer = findViewById(R.id.labelFocusTimer)
        layoutForStopButton = findViewById(R.id.layoutForStopButton)
        textStopButton = findViewById(R.id.textStopButton)
    }

    // Инициализация менеджеров данных
    private fun initManagers() {
        taskManager = TaskManager(this)
    }

    // Загрузка возраста пользователя
    private fun loadUserAge() {
        val prefs = getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
        userAge = prefs.getString(KEY_USER_AGE, "25")?.toIntOrNull() ?: 25
    }

    // Загрузка данных задачи из Intent
    private fun loadTaskData() {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE)

        // Находим задачу в базе по ID
        if (taskId != null) {
            selectedTask = taskManager.getTaskById(taskId)
        }

        displayTaskTitle(taskTitle ?: "Задача")
    }

    // Отображение названия задачи
    private fun displayTaskTitle(taskTitle: String) {
        labelNameOfTask.text = taskTitle
        labelFocusTimer.setTextColor(Color.WHITE)

        // Отображение времени из Intent
        val taskTime = intent.getStringExtra(EXTRA_TASK_TIME)
        if (!taskTime.isNullOrEmpty()) {
            labelFocusTimer.text = formatTimeForDisplay(taskTime)
        }
    }

    // Форматирование времени для отображения (всегда в ЧЧ:ММ:СС)
    private fun formatTimeForDisplay(timeString: String): String {
        val timeParts = timeString.split(":")

        return when (timeParts.size) {
            3 -> timeString // Уже в формате ЧЧ:ММ:СС
            2 -> {
                // Формат ЧЧ:ММ или ММ:СС
                val first = timeParts[0].toIntOrNull() ?: 0
                val second = timeParts[1].toIntOrNull() ?: 0

                if (first < 24 && second < 60) {
                    // ЧЧ:ММ -> добавляем секунды
                    "$timeString:00"
                } else {
                    // ММ:СС -> добавляем часы
                    String.format("00:%02d:%02d", first, second)
                }
            }
            else -> "00:25:00" // Значение по умолчанию
        }
    }

    // Расчет длительности рабочих блоков и перерывов
    private fun calculateDurations() {
        val taskTime = intent.getStringExtra(EXTRA_TASK_TIME) ?: "00:25:00"
        val totalMinutes = calculateTotalMinutes(taskTime)

        calculateBlockConfiguration(totalMinutes)
        convertToMilliseconds(totalMinutes)
    }

    // Расчет общего времени в минутах из строки времени
    private fun calculateTotalMinutes(timeString: String): Int {
        val timeParts = timeString.split(":")

        return when (timeParts.size) {
            3 -> {
                // Формат "ЧЧ:ММ:СС"
                val hours = timeParts[0].toIntOrNull() ?: 0
                val minutes = timeParts[1].toIntOrNull() ?: 0
                hours * 60 + minutes // Секунды игнорируем
            }
            2 -> {
                // Формат "ЧЧ:ММ" или "ММ:СС"
                val firstPart = timeParts[0].toIntOrNull() ?: 0
                val secondPart = timeParts[1].toIntOrNull() ?: 0

                if (firstPart < 24 && secondPart < 60) {
                    // Формат ЧЧ:ММ
                    firstPart * 60 + secondPart
                } else {
                    // Формат ММ:СС
                    firstPart
                }
            }
            else -> 25 // Значение по умолчанию
        }
    }

    // Расчет конфигурации блоков работы/перерывов
    private fun calculateBlockConfiguration(totalMinutes: Int) {
        when {
            totalMinutes <= 5 -> {
                // Очень короткие сессии (до 5 минут) - без перерывов
                totalBlocks = 1
                breakDuration = 0
                workBlockDuration = totalMinutes
            }
            totalMinutes <= 15 -> {
                // Короткие сессии (5-15 минут) - 1 перерыв
                totalBlocks = 2
                breakDuration = 1 // 1 минута перерыва
                val totalBreakTime = breakDuration * (totalBlocks - 1)
                workBlockDuration = if (totalBlocks > 0) (totalMinutes - totalBreakTime) / totalBlocks else totalMinutes
            }
            totalMinutes <= 30 -> {
                // Средние сессии (15-30 минут) - 1-2 перерыва
                totalBlocks = 2
                breakDuration = 2 // 2 минуты перерыва
                val totalBreakTime = breakDuration * (totalBlocks - 1)
                workBlockDuration = if (totalBlocks > 0) (totalMinutes - totalBreakTime) / totalBlocks else totalMinutes
            }
            totalMinutes <= 60 -> {
                // Длинные сессии (30-60 минут) - 2-3 перерыва
                totalBlocks = 3
                breakDuration = 3 // 3 минуты перерыва
                val totalBreakTime = breakDuration * (totalBlocks - 1)
                workBlockDuration = if (totalBlocks > 0) (totalMinutes - totalBreakTime) / totalBlocks else totalMinutes
            }
            else -> {
                // Очень длинные сессии (более 60 минут) - максимум 4 блока
                totalBlocks = 4
                breakDuration = 5 // 5 минут перерыва
                val totalBreakTime = breakDuration * (totalBlocks - 1)
                workBlockDuration = if (totalBlocks > 0) (totalMinutes - totalBreakTime) / totalBlocks else totalMinutes
            }
        }

        // Гарантируем минимальную длительность рабочего блока
        if (workBlockDuration < 1) {
            workBlockDuration = 1
        }

        // Корректировка для точного соответствия общему времени
        adjustBlockConfiguration(totalMinutes)
    }

    // Корректировка конфигурации блоков под общее время
    private fun adjustBlockConfiguration(totalMinutes: Int) {
        val totalCalculatedTime = (workBlockDuration * totalBlocks) + (breakDuration * (totalBlocks - 1))

        if (totalCalculatedTime > totalMinutes) {
            // Корректируем: уменьшаем длительность рабочих блоков
            val availableWorkTime = totalMinutes - (breakDuration * (totalBlocks - 1))
            workBlockDuration = if (totalBlocks > 0) availableWorkTime / totalBlocks else totalMinutes
            if (workBlockDuration < 1) workBlockDuration = 1
        }
    }

    // Конвертация времени в миллисекунды
    private fun convertToMilliseconds(totalMinutes: Int) {
        totalFocusTimeMillis = totalMinutes * 60 * 1000L
    }

    // Загрузка и настройка таймера (восстановление состояния)
    private fun loadAndSetupTimer() {
        val prefs = getSharedPreferences(PREFS_FOCUS, MODE_PRIVATE)
        val savedTaskId = prefs.getString(KEY_TASK_ID, null)
        val savedRemainingTime = prefs.getLong(KEY_REMAINING_TIME, -1)
        val savedIsBreak = prefs.getBoolean(KEY_IS_BREAK, false)
        val savedCurrentBlock = prefs.getInt(KEY_CURRENT_BLOCK, 0)
        val savedCurrentBlockTime = prefs.getLong(KEY_CURRENT_BLOCK_TIME, -1)

        if (savedTaskId == selectedTask?.id && savedRemainingTime > 0 && savedCurrentBlockTime > 0) {
            // Восстановление сохраненной сессии
            restoreTimerState(
                savedRemainingTime,
                savedCurrentBlockTime,
                savedIsBreak,
                savedCurrentBlock
            )
        } else {
            // Начало новой сессии фокуса
            startNewSession()
        }
    }

    // Восстановление состояния таймера
    private fun restoreTimerState(
        savedRemainingTime: Long,
        savedCurrentBlockTime: Long,
        savedIsBreak: Boolean,
        savedCurrentBlock: Int
    ) {
        remainingTimeMillis = savedRemainingTime
        currentBlockTimeMillis = savedCurrentBlockTime
        isBreakTime = savedIsBreak
        currentBlock = savedCurrentBlock

        if (isBreakTime) {
            setupBreakBlock()
        } else {
            setupWorkBlock()
        }

        updateTimerDisplay(remainingTimeMillis)

        if (remainingTimeMillis > 0) {
            startTimer()
        }
    }

    // Начало новой сессии фокуса
    private fun startNewSession() {
        currentBlock = 0
        isBreakTime = false
        startWorkBlock()
    }

    // Запуск рабочего блока
    private fun startWorkBlock() {
        isBreakTime = false
        currentBlockTimeMillis = workBlockDuration * 60 * 1000L
        remainingTimeMillis = currentBlockTimeMillis

        // Гарантируем минимальное время блока
        if (currentBlockTimeMillis <= 0) {
            currentBlockTimeMillis = 60000L // Минимум 1 минута
            remainingTimeMillis = currentBlockTimeMillis
        }

        setupWorkBlock()
        updateTimerDisplay(remainingTimeMillis)
        saveTimerState()
        playSound(R.raw.alert) // Звук начала работы
        startTimer()
    }

    // Запуск перерыва
    private fun startBreakBlock() {
        isBreakTime = true
        currentBlockTimeMillis = breakDuration * 60 * 1000L
        remainingTimeMillis = currentBlockTimeMillis
        setupBreakBlock()
        updateTimerDisplay(remainingTimeMillis)
        saveTimerState()
        playSound(R.raw.alert) // Звук начала перерыва
        startTimer()
    }

    // Настройка UI для рабочего блока
    private fun setupWorkBlock() {
        labelFocus.text = "Фокус на задаче"
        labelNameOfTask.text = "${selectedTask?.title ?: "Задача"} (Блок ${currentBlock + 1}/$totalBlocks)"
        textStopButton.text = "Стоп"
        labelFocusTimer.setTextColor(Color.WHITE)
        isLastMinute = false
    }

    // Настройка UI для перерыва
    private fun setupBreakBlock() {
        labelFocus.text = "Короткий перерыв"
        labelNameOfTask.text = "Отдыхайте ${breakDuration} мин"
        textStopButton.text = "Пропустить перерыв"
        labelFocusTimer.setTextColor(Color.parseColor("#4CAF50")) // Зеленый
        isLastMinute = false
    }

    // Настройка обработчиков кликов
    private fun setupListeners() {
        layoutForStopButton.setOnClickListener {
            if (isBreakTime) {
                showSkipBreakDialog()
            } else {
                showCancelFocusDialog()
            }
        }
    }

    // Запуск таймера
    private fun startTimer() {
        if (isTimerRunning || remainingTimeMillis <= 0) return

        isTimerRunning = true

        countDownTimer = object : CountDownTimer(remainingTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                updateTimerDisplay(millisUntilFinished)
                saveTimerState()
            }

            override fun onFinish() {
                remainingTimeMillis = 0
                isTimerRunning = false
                updateTimerDisplay(0)
                vibrateOnCompletion()
                handleBlockCompletion()
            }
        }.start()
    }

    // Обработка завершения блока (рабочего или перерыва)
    private fun handleBlockCompletion() {
        if (isBreakTime) {
            onBreakCompleted()
        } else {
            onWorkBlockCompleted()
        }
    }

    // Завершение рабочего блока
    private fun onWorkBlockCompleted() {
        currentBlock++

        if (currentBlock < totalBlocks) {
            // Еще есть рабочие блоки - показываем перерыв
            showBreakNotification()
        } else {
            // Все блоки завершены
            onFocusCompleted()
        }
    }

    // Завершение перерыва
    private fun onBreakCompleted() {
        playSound(R.raw.alert) // Звук начала работы после перерыва
        startWorkBlock()
    }

    // Завершение всей сессии фокуса
    private fun onFocusCompleted() {
        playSound(R.raw.alert) // Звук окончания работы
        markTaskAsCompleted()
        clearTimerState()

        val sessionDuration = totalFocusTimeMillis - remainingTimeMillis
        saveFocusSession(sessionDuration, true)

        showFocusCompletedDialog()
    }

    // Сохранение сессии фокуса
    private fun saveFocusSession(duration: Long, isCompleted: Boolean) {
        val statsManager = StatsManager(this)

        val session = FocusSession(
            id = UUID.randomUUID().toString(),
            startTime = System.currentTimeMillis() - duration,
            duration = duration,
            isCompleted = isCompleted,
            taskId = selectedTask?.id
        )

        statsManager.saveSession(session)

        // Обновление статистики выполненных задач
        if (isCompleted) {
            updateCompletedTasksStats()
        }
    }

    // Обновление статистики выполненных задач
    private fun updateCompletedTasksStats() {
        val today = Task.getTodayDate()
        val todayTasks = taskManager.getTodayTasks()
        val completedToday = todayTasks.count { it.isCompleted }
        StatsManager(this).updateCompletedTasks(today, completedToday)
    }

    // Показ уведомления о перерыве
    private fun showBreakNotification() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_break_notification, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val labelBreakTitle = dialogView.findViewById<TextView>(R.id.labelBreakTitle)
        val labelBreakMessage = dialogView.findViewById<TextView>(R.id.labelBreakMessage)
        val layoutStartBreakButton = dialogView.findViewById<FrameLayout>(R.id.layoutStartBreakButton)
        val layoutSkipBreakButton = dialogView.findViewById<FrameLayout>(R.id.layoutSkipBreakButton)

        labelBreakTitle.text = "Время перерыва!"
        labelBreakMessage.text = "Вы хорошо поработали ${workBlockDuration} минут. Отдохните $breakDuration минут перед следующим блоком."

        val textStartBreakButton = dialogView.findViewById<TextView>(R.id.textStartBreakButton)
        val textSkipBreakButton = dialogView.findViewById<TextView>(R.id.textSkipBreakButton)

        textStartBreakButton.text = "Начать перерыв"
        textSkipBreakButton.text = "Пропустить перерыв"

        layoutStartBreakButton.setOnClickListener {
            dialog.dismiss()
            startBreakBlock()
        }

        layoutSkipBreakButton.setOnClickListener {
            dialog.dismiss()
            playSound(R.raw.alert) // Звук начала работы
            startWorkBlock()
        }

        dialog.show()
    }

    // Диалог пропуска перерыва
    private fun showSkipBreakDialog() {
        pauseTimer()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_break, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Настройка окна диалога
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            // Установка параметров для центрирования
            val params = attributes
            params?.width = WindowManager.LayoutParams.WRAP_CONTENT
            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
            params?.gravity = Gravity.CENTER
            params?.horizontalMargin = 0.1f // 10% отступы по горизонтали
            attributes = params
        }

        // Находим элементы
        val layoutBackButton = dialogView.findViewById<FrameLayout>(R.id.layoutBackButton)
        val layoutCancelButton = dialogView.findViewById<FrameLayout>(R.id.layoutCancelButton)

        // Устанавливаем слушатели
        layoutBackButton.setOnClickListener {
            dialog.dismiss()
            resumeTimer()
        }

        layoutCancelButton.setOnClickListener {
            dialog.dismiss()
            playSound(R.raw.alert)
            startWorkBlock()
        }

        dialog.show()
    }

    // Диалог отмены фокуса
    private fun showCancelFocusDialog() {
        pauseTimer()

        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_focus, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Настройка окна диалога
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            // Установка параметров для центрирования
            val params = attributes
            params?.width = WindowManager.LayoutParams.WRAP_CONTENT
            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
            params?.gravity = Gravity.CENTER
            params?.horizontalMargin = 0.1f // 10% отступы по горизонтали
            attributes = params
        }

        val layoutBackButton = dialogView.findViewById<FrameLayout>(R.id.layoutBackButton)
        val layoutCancelButton = dialogView.findViewById<FrameLayout>(R.id.layoutCancelButton)

        layoutBackButton.setOnClickListener {
            dialog.dismiss()
            resumeTimer()
        }

        layoutCancelButton.setOnClickListener {
            dialog.dismiss()
            stopTimer()
            updateTaskTime()
            returnToMain()
        }

        dialog.show()
    }

    // Воспроизведение звука
    private fun playSound(soundResourceId: Int) {
        try {
            mediaPlayer?.release() // Освобождение предыдущего плеера
            mediaPlayer = MediaPlayer.create(this, soundResourceId)

            mediaPlayer?.setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }

            mediaPlayer?.setVolume(0.5f, 0.5f) // Установка громкости (50%)
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            // В случае ошибки просто игнорируем
        }
    }

    // Пауза таймера
    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        saveTimerState()
    }

    // Возобновление таймера
    private fun resumeTimer() {
        if (remainingTimeMillis > 0 && !isTimerRunning) {
            startTimer()
        }
    }

    // Остановка таймера
    private fun stopTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        clearTimerState()
    }

    // Обновление отображения таймера
    private fun updateTimerDisplay(millis: Long) {
        val (hours, minutes, seconds) = calculateTimeComponents(millis)
        labelFocusTimer.text = formatTimeForDisplay(hours, minutes, seconds)
        updateTimerColor(millis)
    }

    // Расчет компонентов времени из миллисекунд
    private fun calculateTimeComponents(millis: Long): Triple<Int, Int, Int> {
        val totalSeconds = millis / 1000
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()

        // Гарантируем неотрицательные значения
        val safeHours = if (hours < 0) 0 else hours
        val safeMinutes = if (minutes < 0) 0 else minutes
        val safeSeconds = if (seconds < 0) 0 else seconds

        return Triple(safeHours, safeMinutes, safeSeconds)
    }

    // Форматирование времени для отображения
    private fun formatTimeForDisplay(hours: Int, minutes: Int, seconds: Int): String {
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // Обновление цвета таймера
    private fun updateTimerColor(millis: Long) {
        if (!isBreakTime) {
            val newLastMinute = (millis <= 60000 && millis > 0)

            if (newLastMinute != isLastMinute) {
                isLastMinute = newLastMinute

                labelFocusTimer.setTextColor(
                    if (isLastMinute) {
                        Color.RED
                    } else if (millis > 60000) {
                        Color.WHITE
                    } else {
                        labelFocusTimer.currentTextColor
                    }
                )
            }
        }
    }

    // Сохранение состояния таймера
    private fun saveTimerState() {
        val prefs = getSharedPreferences(PREFS_FOCUS, MODE_PRIVATE)
        prefs.edit().apply {
            putLong(KEY_REMAINING_TIME, remainingTimeMillis)
            putLong(KEY_CURRENT_BLOCK_TIME, currentBlockTimeMillis)
            putLong(KEY_TOTAL_FOCUS_TIME, totalFocusTimeMillis)
            putString(KEY_TASK_ID, selectedTask?.id)
            putBoolean(KEY_IS_BREAK, isBreakTime)
            putInt(KEY_CURRENT_BLOCK, currentBlock)
            apply()
        }
    }

    // Очистка состояния таймера
    private fun clearTimerState() {
        val prefs = getSharedPreferences(PREFS_FOCUS, MODE_PRIVATE)
        prefs.edit().apply {
            remove(KEY_REMAINING_TIME)
            remove(KEY_CURRENT_BLOCK_TIME)
            remove(KEY_TOTAL_FOCUS_TIME)
            remove(KEY_TASK_ID)
            remove(KEY_IS_BREAK)
            remove(KEY_CURRENT_BLOCK)
            apply()
        }
    }

    // Вибрация при завершении блока
    private fun vibrateOnCompletion() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?

        if (vibrator?.hasVibrator() == true) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(1000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Отметка задачи как выполненной
    private fun markTaskAsCompleted() {
        selectedTask?.let { task ->
            val updatedTask = task.copy(
                isCompleted = true,
                time = "00:00:00"
            )
            taskManager.updateTask(updatedTask)
            logCompletionTime(task.id)
        }
    }

    // Логирование времени выполнения задачи
    private fun logCompletionTime(taskId: String) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val completionTime = sdf.format(Date())
        getSharedPreferences("task_completions", MODE_PRIVATE)
            .edit()
            .putString(taskId, completionTime)
            .apply()
    }

    // Диалог завершения фокуса
    private fun showFocusCompletedDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_focus_completed, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.fragment_background_for_task_dialog)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        val layoutBackToMainButton = dialogView.findViewById<FrameLayout>(R.id.layoutBackToMainButton)

        layoutBackToMainButton.setOnClickListener {
            dialog.dismiss()

            val sessionDuration = totalFocusTimeMillis - remainingTimeMillis
            saveFocusSession(sessionDuration, false)

            returnToMain()
        }

        dialog.show()
    }

    // Обновление времени задачи при прерывании фокуса
    private fun updateTaskTime() {
        selectedTask?.let { task ->
            // Если фокус завершен полностью
            if (currentBlock >= totalBlocks) {
                taskManager.updateTask(task.copy(time = "00:00:00"))
                return
            }

            // Расчет оставшегося времени
            val remainingSeconds = calculateRemainingSeconds()
            val newTime = formatRemainingTime(remainingSeconds)

            taskManager.updateTask(task.copy(time = newTime))
        }
    }

    // Расчет оставшихся секунд
    private fun calculateRemainingSeconds(): Long {
        val remainingSeconds = remainingTimeMillis / 1000

        return if (!isBreakTime) {
            // Прерван рабочий блок
            calculateRemainingWorkSeconds(remainingSeconds)
        } else {
            // Прерван перерыв
            calculateRemainingBreakSeconds(remainingSeconds)
        }
    }

    // Расчет оставшихся секунд при прерывании рабочего блока
    private fun calculateRemainingWorkSeconds(remainingSeconds: Long): Long {
        val futureWorkBlocks = totalBlocks - currentBlock - 1
        val futureWorkTime = futureWorkBlocks * workBlockDuration * 60L
        val futureBreaks = futureWorkBlocks
        val futureBreakTime = futureBreaks * breakDuration * 60L

        return remainingSeconds + futureWorkTime + futureBreakTime
    }

    // Расчет оставшихся секунд при прерывании перерыва
    private fun calculateRemainingBreakSeconds(remainingSeconds: Long): Long {
        val futureWorkBlocks = totalBlocks - currentBlock
        val futureWorkTime = futureWorkBlocks * workBlockDuration * 60L
        val futureBreaks = futureWorkBlocks - 1
        val futureBreakTime = futureBreaks * breakDuration * 60L

        return remainingSeconds + futureWorkTime + futureBreakTime
    }

    // Форматирование оставшегося времени
    private fun formatRemainingTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Возврат на главный экран
    private fun returnToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    // Жизненный цикл активности
    override fun onResume() {
        super.onResume()
        if (!isTimerRunning && remainingTimeMillis > 0) {
            startTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        pauseTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        mediaPlayer?.release() // Освобождение ресурсов MediaPlayer
        mediaPlayer = null
    }
}