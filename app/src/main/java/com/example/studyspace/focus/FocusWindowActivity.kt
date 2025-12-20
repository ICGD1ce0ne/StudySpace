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
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.studyspace.R
import com.example.studyspace.main.MainActivity
import com.example.studyspace.task.models.Task
import com.example.studyspace.task.models.TaskManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FocusWindowActivity : AppCompatActivity() {

    private lateinit var labelFocus: TextView
    private lateinit var labelNameOfTask: TextView
    private lateinit var labelFocusTimer: TextView
    private lateinit var layoutForStopButton: FrameLayout
    private lateinit var textStopButton: TextView
    private lateinit var taskManager: TaskManager

    // MediaPlayer для воспроизведения звуков
    private var mediaPlayer: MediaPlayer? = null

    private var selectedTask: Task? = null
    private var countDownTimer: CountDownTimer? = null
    private var remainingTimeMillis: Long = 0
    private var totalFocusTimeMillis: Long = 0 // Общее время фокуса (например, 60 мин)
    private var currentBlockTimeMillis: Long = 0 // Время текущего блока (работа/перерыв)
    private var isTimerRunning = false
    private var isBreakTime = false // true = сейчас перерыв, false = работа
    private var currentBlock = 0 // Текущий блок (0, 1, 2...)
    private var totalBlocks = 3 // Всего блоков работы
    private var breakDuration = 5 // Длительность перерыва в минутах
    private var workBlockDuration = 0 // Длительность рабочего блока в минутах
    private var userAge = 25
    private var isLastMinute = false

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_TIME = "extra_task_time"
        const val PREFS_FOCUS = "focus_prefs"
        const val KEY_REMAINING_TIME = "remaining_time"
        const val KEY_TASK_ID = "task_id"
        const val KEY_IS_BREAK = "is_break"
        const val KEY_CURRENT_BLOCK = "current_block"
        const val KEY_TOTAL_FOCUS_TIME = "total_focus_time"
        const val KEY_CURRENT_BLOCK_TIME = "current_block_time"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_window)

        labelFocus = findViewById(R.id.labelFocus)
        labelNameOfTask = findViewById(R.id.labelNameOfTask)
        labelFocusTimer = findViewById(R.id.labelFocusTimer)
        layoutForStopButton = findViewById(R.id.layoutForStopButton)
        textStopButton = findViewById(R.id.textStopButton)

        taskManager = TaskManager(this)

        // Получаем возраст пользователя
        val prefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        userAge = prefs.getString("user_age", "25")?.toIntOrNull() ?: 25

        // Получаем данные из Intent
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE)
        val taskTime = intent.getStringExtra(EXTRA_TASK_TIME)

        // Находим задачу в базе
        if (taskId != null) {
            selectedTask = taskManager.getTaskById(taskId)
        }

        initViews(taskTitle ?: "Задача")
        calculateDurations(taskTime ?: "00:25:00")
        loadAndSetupTimer()
        setupListeners()
    }

    private fun initViews(taskTitle: String) {
        labelNameOfTask.text = taskTitle
        labelFocusTimer.setTextColor(Color.WHITE)

        // Получаем время из Intent и отображаем его
        val taskTime = intent.getStringExtra(EXTRA_TASK_TIME)
        if (!taskTime.isNullOrEmpty()) {
            // Отображаем время в формате ЧЧ:ММ:СС
            val formattedTime = formatTimeForDisplay(taskTime)
            labelFocusTimer.text = formattedTime
        }
    }

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
            else -> "00:25:00"
        }
    }

    private fun calculateDurations(totalTimeString: String) {
        // Преобразуем общее время фокуса в минуты
        // Формат: "ЧЧ:ММ:СС" или "ЧЧ:ММ" или "ММ:СС"
        val timeParts = totalTimeString.split(":")

        var totalMinutes = 25 // Значение по умолчанию

        when (timeParts.size) {
            3 -> {
                // Формат "ЧЧ:ММ:СС" (например, 01:30:00 = 1 час 30 минут = 90 минут)
                val hours = timeParts[0].toIntOrNull() ?: 0
                val minutes = timeParts[1].toIntOrNull() ?: 0
                val seconds = timeParts[2].toIntOrNull() ?: 0
                totalMinutes = hours * 60 + minutes
                // Секунды игнорируем при расчете блоков
            }
            2 -> {
                // Формат "ЧЧ:ММ" или "ММ:СС"
                val firstPart = timeParts[0].toIntOrNull() ?: 0
                val secondPart = timeParts[1].toIntOrNull() ?: 0

                // Если первая часть меньше 24, а вторая меньше 60, то это ЧЧ:ММ
                if (firstPart < 24 && secondPart < 60) {
                    // Формат ЧЧ:ММ (например, 01:30 = 1 час 30 минут = 90 минут)
                    totalMinutes = firstPart * 60 + secondPart
                } else {
                    // Формат ММ:СС (например, 90:00 = 90 минут)
                    totalMinutes = firstPart
                }
            }
            else -> {
                totalMinutes = 25 // Значение по умолчанию
            }
        }

        println("DEBUG: Общее время в минутах: $totalMinutes")

        // Определяем разумное количество блоков и длительность перерыва
        when {
            totalMinutes <= 5 -> {
                // Для очень коротких сессий (до 5 минут) - без перерывов
                totalBlocks = 1
                breakDuration = 0
                workBlockDuration = totalMinutes
            }
            totalMinutes <= 15 -> {
                // Для коротких сессий (5-15 минут) - 1 перерыв
                totalBlocks = 2
                breakDuration = 1 // 1 минута перерыва
                val totalBreakTime = breakDuration * (totalBlocks - 1)
                workBlockDuration = if (totalBlocks > 0) (totalMinutes - totalBreakTime) / totalBlocks else totalMinutes
            }
            totalMinutes <= 30 -> {
                // Для средних сессий (15-30 минут) - 1-2 перерыва
                totalBlocks = 2
                breakDuration = 2 // 2 минуты перерыва
                val totalBreakTime = breakDuration * (totalBlocks - 1)
                workBlockDuration = if (totalBlocks > 0) (totalMinutes - totalBreakTime) / totalBlocks else totalMinutes
            }
            totalMinutes <= 60 -> {
                // Для длинных сессий (30-60 минут) - 2-3 перерыва
                totalBlocks = 3
                breakDuration = 3 // 3 минуты перерыва
                val totalBreakTime = breakDuration * (totalBlocks - 1)
                workBlockDuration = if (totalBlocks > 0) (totalMinutes - totalBreakTime) / totalBlocks else totalMinutes
            }
            else -> {
                // Для очень длинных сессий (более 60 минут) - максимум 4 блока
                totalBlocks = 4
                breakDuration = 5 // 5 минут перерыва
                val totalBreakTime = breakDuration * (totalBlocks - 1)
                workBlockDuration = if (totalBlocks > 0) (totalMinutes - totalBreakTime) / totalBlocks else totalMinutes
            }
        }

        // Гарантируем минимальную длительность рабочего блока в 1 минуту
        if (workBlockDuration < 1) {
            workBlockDuration = 1
        }

        // Гарантируем, что общее время работы + перерывов не превышает заданное время
        val totalCalculatedTime = (workBlockDuration * totalBlocks) + (breakDuration * (totalBlocks - 1))
        if (totalCalculatedTime > totalMinutes) {
            // Корректируем: уменьшаем длительность рабочих блоков
            val availableWorkTime = totalMinutes - (breakDuration * (totalBlocks - 1))
            workBlockDuration = if (totalBlocks > 0) availableWorkTime / totalBlocks else totalMinutes
            if (workBlockDuration < 1) workBlockDuration = 1
        }

        println("DEBUG: Блоков: $totalBlocks, Работа: $workBlockDuration мин, Перерыв: $breakDuration мин")

        // Конвертируем в миллисекунды
        totalFocusTimeMillis = totalMinutes * 60 * 1000L
    }

    private fun loadAndSetupTimer() {
        val prefs = getSharedPreferences(PREFS_FOCUS, MODE_PRIVATE)
        val savedTaskId = prefs.getString(KEY_TASK_ID, null)
        val savedRemainingTime = prefs.getLong(KEY_REMAINING_TIME, -1)
        val savedIsBreak = prefs.getBoolean(KEY_IS_BREAK, false)
        val savedCurrentBlock = prefs.getInt(KEY_CURRENT_BLOCK, 0)
        val savedCurrentBlockTime = prefs.getLong(KEY_CURRENT_BLOCK_TIME, -1)

        if (savedTaskId == selectedTask?.id && savedRemainingTime > 0 && savedCurrentBlockTime > 0) {
            // Восстанавливаем сохраненную сессию
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
        } else {
            // Начинаем новый фокус с первого рабочего блока
            startNewSession()
        }
    }

    private fun startNewSession() {
        currentBlock = 0
        isBreakTime = false
        startWorkBlock()
    }

    private fun startWorkBlock() {
        isBreakTime = false
        currentBlockTimeMillis = workBlockDuration * 60 * 1000L
        remainingTimeMillis = currentBlockTimeMillis

        // Проверяем, что время блока не отрицательное
        if (currentBlockTimeMillis <= 0) {
            currentBlockTimeMillis = 60000L // Минимум 1 минута
            remainingTimeMillis = currentBlockTimeMillis
        }

        setupWorkBlock()
        updateTimerDisplay(remainingTimeMillis)
        saveTimerState()
        // Воспроизводим звук начала работы
        playSound(R.raw.alert)
        startTimer()
    }

    private fun startBreakBlock() {
        isBreakTime = true
        currentBlockTimeMillis = breakDuration * 60 * 1000L
        remainingTimeMillis = currentBlockTimeMillis
        setupBreakBlock()
        updateTimerDisplay(remainingTimeMillis)
        saveTimerState()
        // Воспроизводим звук начала перерыва
        playSound(R.raw.alert)
        startTimer()
    }

    private fun setupWorkBlock() {
        labelFocus.text = "Фокус на задаче"
        labelNameOfTask.text = "${selectedTask?.title ?: "Задача"} (Блок ${currentBlock + 1}/$totalBlocks)"
        textStopButton.text = "Стоп"
        labelFocusTimer.setTextColor(Color.WHITE)
        isLastMinute = false
    }

    private fun setupBreakBlock() {
        labelFocus.text = "Короткий перерыв"
        labelNameOfTask.text = "Отдыхайте ${breakDuration} мин"
        textStopButton.text = "Пропустить перерыв"
        labelFocusTimer.setTextColor(Color.parseColor("#4CAF50")) // Зеленый
        isLastMinute = false
    }

    private fun setupListeners() {
        layoutForStopButton.setOnClickListener {
            if (isBreakTime) {
                showSkipBreakDialog()
            } else {
                showCancelFocusDialog()
            }
        }
    }

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

                // Вибрация при завершении блока
                vibrateOnCompletion()

                // Определяем, что делать дальше
                if (isBreakTime) {
                    // Завершился перерыв
                    onBreakCompleted()
                } else {
                    // Завершился рабочий блок
                    onWorkBlockCompleted()
                }
            }
        }.start()
    }

    private fun onWorkBlockCompleted() {
        currentBlock++

        if (currentBlock < totalBlocks) {
            // Есть еще рабочие блоки, показываем перерыв
            showBreakNotification()
        } else {
            // Все блоки завершены - фокус окончен
            onFocusCompleted()
        }
    }

    private fun onBreakCompleted() {
        // Перерыв завершен, начинаем следующий рабочий блок
        // Воспроизводим звук начала работы после перерыва
        playSound(R.raw.alert)
        startWorkBlock()
    }

    private fun onFocusCompleted() {
        // Весь фокус завершен
        // Воспроизводим звук окончания работы
        playSound(R.raw.alert)
        markTaskAsCompleted()
        clearTimerState()
        showFocusCompletedDialog()
    }

    private fun showBreakNotification() {
        // Показываем уведомление о перерыве
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
            // Пропускаем перерыв и сразу начинаем следующий рабочий блок
            // Воспроизводим звук начала работы
            playSound(R.raw.alert)
            startWorkBlock()
        }

        dialog.show()
    }

    private fun showSkipBreakDialog() {
        pauseTimer()

        AlertDialog.Builder(this)
            .setTitle("Пропустить перерыв?")
            .setMessage("Вы уверены, что хотите пропустить перерыв и продолжить работу?")
            .setPositiveButton("Да, продолжить") { _, _ ->
                // Пропускаем перерыв и начинаем рабочий блок
                // Воспроизводим звук начала работы
                playSound(R.raw.alert)
                startWorkBlock()
            }
            .setNegativeButton("Нет, остаться") { _, _ ->
                resumeTimer()
            }
            .setCancelable(false)
            .show()
    }

    private fun showCancelFocusDialog() {
        pauseTimer()

        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_focus, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

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

    // Метод для воспроизведения звука
    private fun playSound(soundResourceId: Int) {
        try {
            // Освобождаем предыдущий MediaPlayer, если он есть
            mediaPlayer?.release()

            // Создаем новый MediaPlayer
            mediaPlayer = MediaPlayer.create(this, soundResourceId)

            // Настраиваем слушатель для освобождения ресурсов после завершения
            mediaPlayer?.setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }

            // Устанавливаем громкость (50% от максимальной)
            mediaPlayer?.setVolume(0.5f, 0.5f)

            // Запускаем воспроизведение
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            // В случае ошибки просто игнорируем
        }
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        saveTimerState()
    }

    private fun resumeTimer() {
        if (remainingTimeMillis > 0 && !isTimerRunning) {
            startTimer()
        }
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        clearTimerState()
    }

    private fun updateTimerDisplay(millis: Long) {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        // Не даем времени быть отрицательным
        val safeHours = if (hours < 0) 0 else hours
        val safeMinutes = if (minutes < 0) 0 else minutes
        val safeSeconds = if (seconds < 0) 0 else seconds

        val displayText = if (safeHours > 0) {
            String.format("%02d:%02d:%02d", safeHours, safeMinutes, safeSeconds)
        } else {
            String.format("%02d:%02d", safeMinutes, safeSeconds)
        }

        labelFocusTimer.text = displayText

        // Меняем цвет в последнюю минуту рабочего блока
        if (!isBreakTime) {
            val newLastMinute = (millis <= 60000 && millis > 0)
            if (newLastMinute != isLastMinute) {
                isLastMinute = newLastMinute
                if (isLastMinute) {
                    labelFocusTimer.setTextColor(Color.RED)
                } else if (millis > 60000) {
                    labelFocusTimer.setTextColor(Color.WHITE)
                }
            }
        }
    }

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

    private fun logCompletionTime(taskId: String) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val completionTime = sdf.format(Date())
        val prefs = getSharedPreferences("task_completions", MODE_PRIVATE)
        prefs.edit().putString(taskId, completionTime).apply()
    }

    private fun showFocusCompletedDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_focus_completed, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val layoutBackToMainButton = dialogView.findViewById<FrameLayout>(R.id.layoutBackToMainButton)

        layoutBackToMainButton.setOnClickListener {
            dialog.dismiss()
            returnToMain()
        }

        dialog.show()
    }

    private fun updateTaskTime() {
        selectedTask?.let { task ->
            // Если фокус завершен полностью, оставляем 00:00:00
            if (currentBlock >= totalBlocks) {
                val updatedTask = task.copy(time = "00:00:00")
                taskManager.updateTask(updatedTask)
                return
            }

            // Если сейчас рабочий блок, который мы прервали
            if (!isBreakTime) {
                // Преобразуем оставшееся время в секунды
                val remainingSeconds = remainingTimeMillis / 1000

                // Плюс все будущие блоки и перерывы
                val futureWorkBlocks = totalBlocks - currentBlock - 1 // оставшиеся после текущего
                val futureWorkTime = futureWorkBlocks * workBlockDuration * 60 // в секундах
                val futureBreaks = futureWorkBlocks // перерывы между будущими блоками
                val futureBreakTime = futureBreaks * breakDuration * 60 // в секундах

                // Общее оставшееся время в секундах
                val totalSeconds = remainingSeconds + futureWorkTime + futureBreakTime

                // Конвертируем в формат ЧЧ:ММ:СС
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                val newTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                val updatedTask = task.copy(time = newTime)
                taskManager.updateTask(updatedTask)
            } else {
                // Если сейчас перерыв, который мы прервали
                // Оставшееся время перерыва + все будущие рабочие блоки и перерывы
                val remainingBreakSeconds = remainingTimeMillis / 1000

                val futureWorkBlocks = totalBlocks - currentBlock // оставшиеся рабочие блоки
                val futureWorkTime = futureWorkBlocks * workBlockDuration * 60 // в секундах
                val futureBreaks = futureWorkBlocks - 1 // перерывы между будущими блоками
                val futureBreakTime = futureBreaks * breakDuration * 60 // в секундах

                // Общее оставшееся время в секундах
                val totalSeconds = remainingBreakSeconds + futureWorkTime + futureBreakTime

                // Конвертируем в формат ЧЧ:ММ:СС
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                val newTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                val updatedTask = task.copy(time = newTime)
                taskManager.updateTask(updatedTask)
            }
        }
    }

    private fun returnToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

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
        // Освобождаем MediaPlayer при уничтожении активности
        mediaPlayer?.release()
        mediaPlayer = null
    }
}