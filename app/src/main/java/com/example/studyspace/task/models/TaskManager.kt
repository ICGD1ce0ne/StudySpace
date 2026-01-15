package com.example.studyspace.task.models

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("tasks_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun saveTasks(tasks: List<Task>) {
        val json = gson.toJson(tasks)
        sharedPreferences.edit().putString("tasks_list", json).apply()
    }

    fun getTasks(): List<Task> {
        val json = sharedPreferences.getString("tasks_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Task>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun getActiveTasks(): List<Task> {
        return getTasks().filter { !it.isCompleted }
    }

    fun getTasksForFocus(): List<Task> {
        val today = Task.getTodayDate()
        return getActiveTasks().filter {
            it.deadline.isEmpty() || it.deadline == today
        }
    }

    fun addTask(task: Task) {
        val tasks = getTasks().toMutableList()
        tasks.add(task)
        saveTasks(tasks)
    }

    fun deleteTask(taskId: String) {
        val tasks = getTasks().toMutableList()
        tasks.removeAll { it.id == taskId }
        saveTasks(tasks)
    }

    fun updateTask(updatedTask: Task) {
        val tasks = getTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            tasks[index] = updatedTask
            saveTasks(tasks)
        }
    }

    fun updateTaskTime(taskId: String, time: String) {
        getTaskById(taskId)?.let { task ->
            val updatedTask = task.copy(time = time)
            updateTask(updatedTask)
        }
    }

    fun getTaskById(taskId: String): Task? {
        return getTasks().find { it.id == taskId }
    }

    fun getTasksGroupedByDate(): Map<String, List<Task>> {
        val tasks = getTasks()

        val sortedTasks = tasks.sortedBy { task ->
            try {
                dateFormat.parse(task.deadline)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }

        return sortedTasks.groupBy { it.deadline }
    }

    fun getDisplayNameForDate(date: String): String = when (date) {
        Task.getTodayDate() -> "Сегодня"
        Task.getTomorrowDate() -> "Завтра"
        Task.getAfterTomorrowDate() -> "Послезавтра"
        else -> date
    }

    fun getTaskCountText(count: Int): String = when {
        count % 10 == 1 && count % 100 != 11 -> "$count задача"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "$count задачи"
        else -> "$count задач"
    }

    fun getTodayTasks(): List<Task> {
        val today = Task.getTodayDate()
        return getTasks().filter { it.deadline == today }
    }

    fun getTomorrowTasks(): List<Task> {
        val tomorrow = Task.getTomorrowDate()
        return getTasks().filter { it.deadline == tomorrow }
    }

    fun getOverdueTasks(): List<Task> {
        val todayFormatted = Task.getTodayDate()

        return getActiveTasks().filter { task ->
            try {
                val taskDate = dateFormat.parse(task.deadline)
                taskDate?.before(Calendar.getInstance().time) == true &&
                        task.deadline != todayFormatted
            } catch (e: Exception) {
                false
            }
        }
    }

    fun clearCompletedTasks() {
        val activeTasks = getActiveTasks()
        saveTasks(activeTasks)
    }

    fun getTaskStats(): Map<String, Int> {
        val tasks = getTasks()
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val active = total - completed
        val today = getTodayTasks().size
        val tomorrow = getTomorrowTasks().size
        val overdue = getOverdueTasks().size

        return mapOf(
            "total" to total,
            "completed" to completed,
            "active" to active,
            "today" to today,
            "tomorrow" to tomorrow,
            "overdue" to overdue
        )
    }
}