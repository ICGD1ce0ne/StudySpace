package com.example.studyspace.main

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.example.studyspace.R
import com.example.studyspace.managers.TaskManager
import com.example.studyspace.models.Task
import java.text.SimpleDateFormat
import java.util.*

class TaskWindowActivity : AppCompatActivity() {

    private lateinit var buttonTaskList: ImageView
    private lateinit var buttonGoal: ImageView
    private lateinit var buttonAnalytic: ImageView
    private lateinit var layoutForAddTaskButton: FrameLayout
    private lateinit var scrollContainer: LinearLayout
    private lateinit var taskManager: TaskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_window)

        buttonTaskList = findViewById(R.id.buttonTaskList)
        buttonGoal = findViewById(R.id.buttonGoal)
        buttonAnalytic = findViewById(R.id.buttonAnalytic)
        layoutForAddTaskButton = findViewById(R.id.layoutForAddTaskButton)

        val scrollView = findViewById<ScrollView>(R.id.scrollView2)
        scrollContainer = scrollView.getChildAt(0) as LinearLayout

        taskManager = TaskManager(this)

        initButtons()
        loadTasks()
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun initButtons() {
        buttonTaskList.setOnClickListener {
            Toast.makeText(this, "Вы уже в задачах", Toast.LENGTH_SHORT).show()
        }

        buttonGoal.setOnClickListener {
            val goToMainMenu = Intent(this, MainActivity::class.java)
            startActivity(goToMainMenu)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        buttonAnalytic.setOnClickListener {
            val goToAnalytic = Intent(this, AnalyticWindowActivity::class.java)
            startActivity(goToAnalytic)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        layoutForAddTaskButton.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun loadTasks() {
        scrollContainer.removeAllViews()

        val groupedTasks = taskManager.getTasksGroupedByDate()

        if (groupedTasks.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "Задач пока нет\nДобавьте первую задачу!"
                setTextColor(getColor(R.color.white))
                textSize = 16f
                typeface = resources.getFont(R.font.montserrat)
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(0)
                }
            }
            scrollContainer.addView(emptyText)
        } else {
            for ((date, tasks) in groupedTasks) {
                val groupTitle = TextView(this).apply {
                    val displayName = taskManager.getDisplayNameForDate(date)
                    val countText = taskManager.getTaskCountText(tasks.size)
                    text = "$displayName - $countText"
                    setTextColor(getColor(R.color.white))
                    textSize = 18f
                    typeface = resources.getFont(R.font.montserrat_medium)
                    gravity = android.view.Gravity.START
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dpToPx(20)
                        marginStart = dpToPx(45)
                        marginEnd = dpToPx(45)
                    }
                }
                scrollContainer.addView(groupTitle)

                for (task in tasks) {
                    val taskView = createTaskView(task)
                    scrollContainer.addView(taskView)
                }
            }
        }
    }

    private fun createTaskView(task: Task): View {
        val inflater = LayoutInflater.from(this)
        val taskView = inflater.inflate(R.layout.fragment_task_task_window, scrollContainer, false)

        val taskNameLabel = taskView.findViewById<TextView>(R.id.labelForTaskName)
        val editButton = taskView.findViewById<ImageView>(R.id.imageForRedactTask)
        val taskLayout = taskView.findViewById<FrameLayout>(R.id.layoutForTask)

        taskNameLabel.text = task.title

        taskLayout.setOnClickListener {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            taskManager.updateTask(updatedTask)
            loadTasks()
        }

        editButton.setOnClickListener {
            showEditTaskDialog(task)
        }

        if (task.isCompleted) {
            taskNameLabel.paintFlags = android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            taskNameLabel.alpha = 0.6f
        }

        return taskView
    }

    private fun showEditTaskDialog(task: Task) {
        val dialog = EditTaskDialogFragment(task) { updatedTask, shouldDelete ->
            if (shouldDelete) {
                taskManager.deleteTask(task.id)
            } else {
                taskManager.updateTask(updatedTask)
            }
            loadTasks()
        }
        dialog.show(supportFragmentManager, "EditTaskDialog")
    }

    private fun showAddTaskDialog() {
        val dialog = AddTaskDialogFragment { task ->
            taskManager.addTask(task)
            loadTasks()
        }
        dialog.show(supportFragmentManager, "AddTaskDialog")
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // Вспомогательные функции для DatePicker и TimePicker
    fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                editText.setText(dateFormat.format(selectedDate.time))
            },
            year, month, day
        )
        datePicker.show()
    }

    fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                editText.setText(time)
            },
            hour, minute, true // 24-часовой формат
        )
        timePicker.show()
    }

    // DialogFragment для добавления задачи
    class AddTaskDialogFragment(
        private val onTaskAdded: (Task) -> Unit
    ) : DialogFragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.dialog_add_task, container, false)
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = super.onCreateDialog(savedInstanceState)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setDimAmount(0.5f)
            return dialog
        }

        override fun onStart() {
            super.onStart()
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val editTextTaskName = view.findViewById<EditText>(R.id.editTextTaskName)
            val editTextDeadlineTask = view.findViewById<EditText>(R.id.editTextDeadlineTask)
            val editTextDeadlineTimeTask = view.findViewById<EditText>(R.id.editTextDeadlineTimeTask)
            val layoutForTodayButton = view.findViewById<FrameLayout>(R.id.layoutForTodayButton)
            val layoutForTomorrowButton = view.findViewById<FrameLayout>(R.id.layoutForTomorrowButton)
            val layoutForAfterTomorrowButton = view.findViewById<FrameLayout>(R.id.layoutForAfterTomorrowButton)
            val layoutForOtherButton = view.findViewById<FrameLayout>(R.id.layoutForOtherButton)
            val layoutForNextButton = view.findViewById<FrameLayout>(R.id.layoutForNextButton)

            // Делаем поля даты и времени не редактируемыми вручную
            editTextDeadlineTask.isFocusable = false
            editTextDeadlineTask.isClickable = true

            editTextDeadlineTimeTask.isFocusable = false
            editTextDeadlineTimeTask.isClickable = true

            // При клике на поле даты открываем DatePicker
            editTextDeadlineTask.setOnClickListener {
                val activity = requireActivity() as TaskWindowActivity
                activity.showDatePickerDialog(editTextDeadlineTask)
            }

            // При клике на поле времени открываем TimePicker
            editTextDeadlineTimeTask.setOnClickListener {
                val activity = requireActivity() as TaskWindowActivity
                activity.showTimePickerDialog(editTextDeadlineTimeTask)
            }

            view.setOnClickListener { dismiss() }

            // Кнопки быстрого выбора даты
            layoutForTodayButton.setOnClickListener {
                editTextDeadlineTask.setText(Task.getTodayDate())
            }

            layoutForTomorrowButton.setOnClickListener {
                editTextDeadlineTask.setText(Task.getTomorrowDate())
            }

            layoutForAfterTomorrowButton.setOnClickListener {
                editTextDeadlineTask.setText(Task.getAfterTomorrowDate())
            }

            layoutForOtherButton.setOnClickListener {
                // Теперь тоже открываем DatePicker
                val activity = requireActivity() as TaskWindowActivity
                activity.showDatePickerDialog(editTextDeadlineTask)
            }

            // Кнопка добавления задачи
            layoutForNextButton.setOnClickListener {
                val taskName = editTextTaskName.text.toString().trim()
                val deadline = editTextDeadlineTask.text.toString().trim()
                val time = editTextDeadlineTimeTask.text.toString().trim()

                if (taskName.isEmpty()) {
                    Toast.makeText(context, "Введите название задачи", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (deadline.isEmpty()) {
                    Toast.makeText(context, "Выберите дату", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Проверяем формат времени (необязательно)
                if (time.isNotEmpty() && !time.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
                    Toast.makeText(context, "Введите время в формате HH:mm", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val task = Task(
                    title = taskName,
                    deadline = deadline,
                    time = time
                )

                onTaskAdded(task)
                Toast.makeText(context, "Задача добавлена", Toast.LENGTH_SHORT).show()
                dismiss()
            }

            view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.dialog_content)
                ?.setOnClickListener {
                    // Ничего не делаем
                }
        }
    }

    // DialogFragment для редактирования задачи
    class EditTaskDialogFragment(
        private val task: Task,
        private val onTaskUpdated: (Task, Boolean) -> Unit
    ) : DialogFragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.dialog_edit_task, container, false)
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = super.onCreateDialog(savedInstanceState)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setDimAmount(0.5f)
            return dialog
        }

        override fun onStart() {
            super.onStart()
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val editTextTaskName = view.findViewById<EditText>(R.id.editTextTaskName)
            val editTextDeadlineTask = view.findViewById<EditText>(R.id.editTextDeadlineTask)
            val editTextDeadlineTimeTask = view.findViewById<EditText>(R.id.editTextDeadlineTimeTask)
            val btnSave = view.findViewById<FrameLayout>(R.id.btnSave)
            val btnDelete = view.findViewById<FrameLayout>(R.id.btnDelete)
            val btnCancel = view.findViewById<FrameLayout>(R.id.btnCancel)

            // Делаем поля даты и времени не редактируемыми вручную
            editTextDeadlineTask.isFocusable = false
            editTextDeadlineTask.isClickable = true

            editTextDeadlineTimeTask.isFocusable = false
            editTextDeadlineTimeTask.isClickable = true

            // При клике на поле даты открываем DatePicker
            editTextDeadlineTask.setOnClickListener {
                val activity = requireActivity() as TaskWindowActivity
                activity.showDatePickerDialog(editTextDeadlineTask)
            }

            // При клике на поле времени открываем TimePicker
            editTextDeadlineTimeTask.setOnClickListener {
                val activity = requireActivity() as TaskWindowActivity
                activity.showTimePickerDialog(editTextDeadlineTimeTask)
            }

            editTextTaskName.setText(task.title)
            editTextDeadlineTask.setText(task.deadline)
            editTextDeadlineTimeTask.setText(task.time)

            btnSave.setOnClickListener {
                val updatedTask = task.copy(
                    title = editTextTaskName.text.toString().trim(),
                    deadline = editTextDeadlineTask.text.toString().trim(),
                    time = editTextDeadlineTimeTask.text.toString().trim()
                )
                onTaskUpdated(updatedTask, false)
                dismiss()
            }

            btnDelete.setOnClickListener {
                onTaskUpdated(task, true)
                dismiss()
            }

            btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }
}