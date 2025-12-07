package com.example.studyspace.focus

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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

    private lateinit var labelNameOfTask: TextView
    private lateinit var labelFocusTimer: TextView
    private lateinit var layoutForStopButton: FrameLayout
    private lateinit var taskManager: TaskManager

    private var selectedTask: Task? = null
    private var countDownTimer: CountDownTimer? = null
    private var remainingTimeMillis: Long = 0
    private var totalTimeMillis: Long = 0
    private var isTimerRunning = false

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_TIME = "extra_task_time"
        const val PREFS_FOCUS = "focus_prefs"
        const val KEY_REMAINING_TIME = "remaining_time"
        const val KEY_TASK_ID = "task_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_window)

        labelNameOfTask = findViewById(R.id.labelNameOfTask)
        labelFocusTimer = findViewById(R.id.labelFocusTimer)
        layoutForStopButton = findViewById(R.id.layoutForStopButton)

        taskManager = TaskManager(this)

        // Получаем данные из Intent
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE)
        val taskTime = intent.getStringExtra(EXTRA_TASK_TIME)

        // Находим задачу в базе
        if (taskId != null) {
            selectedTask = taskManager.getTaskById(taskId)
        }

        initViews(taskTitle ?: "Задача")
        setupTimer(taskTime ?: "25:00")
        setupListeners()
    }

    private fun initViews(taskTitle: String) {
        labelNameOfTask.text = taskTitle
    }

    private fun setupTimer(timeString: String) {
        // Преобразуем строку времени в миллисекунды
        val timeParts = timeString.split(":")
        val minutes = timeParts.getOrNull(0)?.toIntOrNull() ?: 25
        val seconds = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        totalTimeMillis = (minutes * 60 * 1000 + seconds * 1000).toLong()

        // Проверяем сохраненное состояние
        val prefs = getSharedPreferences(PREFS_FOCUS, MODE_PRIVATE)
        val savedTaskId = prefs.getString(KEY_TASK_ID, null)
        val savedRemainingTime = prefs.getLong(KEY_REMAINING_TIME, -1)

        if (savedTaskId == selectedTask?.id && savedRemainingTime > 0) {
            remainingTimeMillis = savedRemainingTime
            updateTimerDisplay(remainingTimeMillis)
            startTimer()
        } else {
            remainingTimeMillis = totalTimeMillis
            updateTimerDisplay(remainingTimeMillis)
        }
    }

    private fun setupListeners() {
        layoutForStopButton.setOnClickListener {
            showCancelDialog()
        }
    }

    private fun startTimer() {
        if (isTimerRunning) return

        isTimerRunning = true
        countDownTimer = object : CountDownTimer(remainingTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                updateTimerDisplay(millisUntilFinished)

                // Сохраняем текущее состояние
                saveTimerState()
            }

            override fun onFinish() {
                remainingTimeMillis = 0
                updateTimerDisplay(0)
                isTimerRunning = false

                // Очищаем сохраненное состояние
                clearTimerState()

                // 1. ВИБРАЦИЯ ПО ЗАВЕРШЕНИЮ
                vibrateOnCompletion()

                // 2. ОТМЕТКА О ЗАВЕРШЕНИИ ЗАДАЧИ
                markTaskAsCompleted()

                // 3. ПОКАЗЫВАЕМ ДИАЛОГ ЗАВЕРШЕНИЯ ИЗ XML
                showFocusCompletedDialog()
            }
        }.start()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        saveTimerState()
    }

    private fun resumeTimer() {
        if (remainingTimeMillis > 0) {
            startTimer()
        }
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        clearTimerState()
    }

    private fun updateTimerDisplay(millis: Long) {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        labelFocusTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun saveTimerState() {
        val prefs = getSharedPreferences(PREFS_FOCUS, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putLong(KEY_REMAINING_TIME, remainingTimeMillis)
        editor.putString(KEY_TASK_ID, selectedTask?.id)
        editor.apply()
    }

    private fun clearTimerState() {
        val prefs = getSharedPreferences(PREFS_FOCUS, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(KEY_REMAINING_TIME)
        editor.remove(KEY_TASK_ID)
        editor.apply()
    }

    // НОВЫЙ МЕТОД: ВИБРАЦИЯ ПРИ ЗАВЕРШЕНИИ
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

    // НОВЫЙ МЕТОД: ОТМЕТКА ЗАДАЧИ КАК ВЫПОЛНЕННОЙ
    private fun markTaskAsCompleted() {
        selectedTask?.let { task ->
            // Отмечаем задачу как выполненную
            val updatedTask = task.copy(
                isCompleted = true,
                time = "" // Очищаем время, так как задача выполнена
            )
            taskManager.updateTask(updatedTask)

            // Можно добавить логирование времени завершения (опционально)
            logCompletionTime(task.id)
        }
    }

    // ОПЦИОНАЛЬНО: Запись времени завершения
    private fun logCompletionTime(taskId: String) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val completionTime = sdf.format(Date())

        // Сохраняем в SharedPreferences или базе данных
        val prefs = getSharedPreferences("task_completions", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(taskId, completionTime)
        editor.apply()
    }

    private fun showCancelDialog() {
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

            // Обновляем время в задаче (сохраняем оставшееся)
            updateTaskTime()

            // Возвращаемся на MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    // НОВЫЙ МЕТОД: ДИАЛОГ ЗАВЕРШЕНИЯ ФОКУСА (используем ваш XML)
    private fun showFocusCompletedDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_focus_completed, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Нельзя закрыть нажатием вне диалога
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
            // Преобразуем оставшееся время обратно в формат HH:mm
            val minutes = (remainingTimeMillis / 1000) / 60
            val seconds = (remainingTimeMillis / 1000) % 60

            val newTime = String.format("%02d:%02d", minutes, seconds)

            // Создаем обновленную задачу (не помечаем как выполненную, только обновляем время)
            val updatedTask = task.copy(time = newTime)
            taskManager.updateTask(updatedTask)
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
        // Автоматически запускаем таймер при открытии активности
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
    }
}