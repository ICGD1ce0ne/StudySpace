package com.example.studyspace.managers

import android.content.Context
import android.content.SharedPreferences
import com.example.studyspace.models.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class TaskManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("tasks_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // Сохранить все задачи
    fun saveTasks(tasks: List<Task>) {
        val json = gson.toJson(tasks)
        sharedPreferences.edit().putString("tasks_list", json).apply()
    }

    // Получить все задачи
    fun getTasks(): List<Task> {
        val json = sharedPreferences.getString("tasks_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Task>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Добавить новую задачу
    fun addTask(task: Task) {
        val tasks = getTasks().toMutableList()
        tasks.add(task)
        saveTasks(tasks)
    }

    // Удалить задачу по ID
    fun deleteTask(taskId: String) {
        val tasks = getTasks().toMutableList()
        tasks.removeAll { it.id == taskId }
        saveTasks(tasks)
    }

    // Обновить задачу
    fun updateTask(updatedTask: Task) {
        val tasks = getTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            tasks[index] = updatedTask
            saveTasks(tasks)
        }
    }

    // Получить задачи, сгруппированные по дате
    fun getTasksGroupedByDate(): Map<String, List<Task>> {
        val tasks = getTasks()

        // Сортируем задачи по дате дедлайна
        val sortedTasks = tasks.sortedBy {
            try {
                dateFormat.parse(it.deadline)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }

        // Группируем по дате
        return sortedTasks.groupBy { it.deadline }
    }

    // Получить отображаемое имя для группы
    fun getDisplayNameForDate(date: String): String {
        return when (date) {
            Task.getTodayDate() -> "Сегодня"
            Task.getTomorrowDate() -> "Завтра"
            Task.getAfterTomorrowDate() -> "Послезавтра"
            else -> date
        }
    }

    // Получить правильное окончание для количества задач
    fun getTaskCountText(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "$count задача"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "$count задачи"
            else -> "$count задач"
        }
    }
}