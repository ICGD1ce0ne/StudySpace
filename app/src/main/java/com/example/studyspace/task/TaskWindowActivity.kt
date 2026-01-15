package com.example.studyspace.task

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.example.studyspace.R
import com.example.studyspace.analytic.AnalyticWindowActivity
import com.example.studyspace.main.MainActivity
import com.example.studyspace.task.models.Task
import com.example.studyspace.task.models.TaskManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

        initViews()
        taskManager = TaskManager(this)

        initButtons()
        loadTasks()
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun initViews() {
        buttonTaskList = findViewById(R.id.buttonTaskList)
        buttonGoal = findViewById(R.id.buttonGoal)
        buttonAnalytic = findViewById(R.id.buttonAnalytic)
        layoutForAddTaskButton = findViewById(R.id.layoutForAddTaskButton)

        val scrollView = findViewById<ScrollView>(R.id.scrollView2)
        scrollContainer = scrollView.getChildAt(0) as LinearLayout
    }

    private fun initButtons() {
        buttonTaskList.setOnClickListener {
            Toast.makeText(this, "Вы уже в задачах", Toast.LENGTH_SHORT).show()
        }

        buttonGoal.setOnClickListener {
            navigateToMainMenu()
        }

        buttonAnalytic.setOnClickListener {
            navigateToAnalytics()
        }

        layoutForAddTaskButton.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun navigateToMainMenu() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun navigateToAnalytics() {
        val intent = Intent(this, AnalyticWindowActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun loadTasks() {
        scrollContainer.removeAllViews()

        val groupedTasks = taskManager.getTasksGroupedByDate()

        if (groupedTasks.isEmpty()) {
            showEmptyState()
        } else {
            displayGroupedTasks(groupedTasks)
        }
    }

    private fun showEmptyState() {
        val emptyText = TextView(this).apply {
            text = "Задач пока нет\nДобавьте первую задачу!"
            setTextColor(getColor(R.color.white))
            textSize = 16f
            typeface = resources.getFont(R.font.montserrat)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(0)
            }
        }
        scrollContainer.addView(emptyText)
    }

    private fun displayGroupedTasks(groupedTasks: Map<String, List<Task>>) {
        for ((date, tasks) in groupedTasks) {
            addGroupHeader(date, tasks)
            tasks.forEach { task ->
                val taskView = createTaskView(task)
                scrollContainer.addView(taskView)
            }
        }
    }

    private fun addGroupHeader(date: String, tasks: List<Task>) {
        val displayName = taskManager.getDisplayNameForDate(date)
        val countText = taskManager.getTaskCountText(tasks.size)

        val groupTitle = TextView(this).apply {
            text = "$displayName - $countText"
            setTextColor(getColor(R.color.white))
            textSize = 18f
            typeface = resources.getFont(R.font.montserrat_medium)
            gravity = Gravity.START
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
    }

    private fun createTaskView(task: Task): View {
        val inflater = LayoutInflater.from(this)
        val taskView = inflater.inflate(R.layout.fragment_task_task_window, scrollContainer, false)

        val taskNameLabel = taskView.findViewById<TextView>(R.id.labelForTaskName)
        val editButton = taskView.findViewById<ImageView>(R.id.imageForRedactTask)
        val taskLayout = taskView.findViewById<FrameLayout>(R.id.layoutForTask)

        taskNameLabel.text = task.title

        taskLayout.setOnClickListener {
            toggleTaskCompletion(task)
        }

        editButton.setOnClickListener {
            showEditTaskDialog(task)
        }

        if (task.isCompleted) {
            taskNameLabel.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            taskNameLabel.alpha = 0.6f
        }

        return taskView
    }

    private fun toggleTaskCompletion(task: Task) {
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        taskManager.updateTask(updatedTask)
        loadTasks()
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
                val time = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    selectedHour,
                    selectedMinute
                )
                editText.setText(time)
            },
            hour, minute, true
        )
        timePicker.show()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

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
            return super.onCreateDialog(savedInstanceState).apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                window?.setDimAmount(0.5f)
            }
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

            setupEditTextListeners(editTextDeadlineTask, editTextDeadlineTimeTask)
            setupDateQuickButtons(editTextDeadlineTask)
            setupAddButton(editTextTaskName, editTextDeadlineTask, editTextDeadlineTimeTask, layoutForNextButton)
        }

        private fun setupEditTextListeners(
            deadlineEditText: EditText,
            timeEditText: EditText
        ) {
            deadlineEditText.apply {
                isFocusable = false
                isClickable = true
                setOnClickListener {
                    (requireActivity() as TaskWindowActivity)
                        .showDatePickerDialog(this)
                }
            }

            timeEditText.apply {
                isFocusable = false
                isClickable = true
                setOnClickListener {
                    (requireActivity() as TaskWindowActivity)
                        .showTimePickerDialog(this)
                }
            }
        }

        private fun setupDateQuickButtons(deadlineEditText: EditText) {
            val todayButton = view?.findViewById<FrameLayout>(R.id.layoutForTodayButton)
            val tomorrowButton = view?.findViewById<FrameLayout>(R.id.layoutForTomorrowButton)
            val afterTomorrowButton = view?.findViewById<FrameLayout>(R.id.layoutForAfterTomorrowButton)
            val otherButton = view?.findViewById<FrameLayout>(R.id.layoutForOtherButton)

            todayButton?.setOnClickListener {
                deadlineEditText.setText(Task.getTodayDate())
            }

            tomorrowButton?.setOnClickListener {
                deadlineEditText.setText(Task.getTomorrowDate())
            }

            afterTomorrowButton?.setOnClickListener {
                deadlineEditText.setText(Task.getAfterTomorrowDate())
            }

            otherButton?.setOnClickListener {
                (requireActivity() as TaskWindowActivity)
                    .showDatePickerDialog(deadlineEditText)
            }
        }

        private fun setupAddButton(
            taskNameEditText: EditText,
            deadlineEditText: EditText,
            timeEditText: EditText,
            addButton: FrameLayout
        ) {
            addButton.setOnClickListener {
                val taskName = taskNameEditText.text.toString()
                val deadline = deadlineEditText.text.toString()
                val time = timeEditText.text.toString()

                // ПРОВЕРКА НА ПУСТЫЕ ПОЛЯ (время теперь обязательно)
                if (!validateInput(taskName, deadline, time)) {
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
        }

        private fun validateInput(taskName: String, deadline: String, time: String): Boolean {
            // Проверка названия задачи
            if (taskName.isEmpty()) {
                Toast.makeText(context, "Введите название задачи", Toast.LENGTH_SHORT).show()
                return false
            }

            // Проверка даты
            if (deadline.isEmpty()) {
                Toast.makeText(context, "Выберите дату", Toast.LENGTH_SHORT).show()
                return false
            }

            // Проверка формата даты (должна быть dd.MM.yyyy)
            val dateRegex = Regex("""^\d{2}\.\d{2}\.\d{4}$""")
            if (!deadline.matches(dateRegex)) {
                Toast.makeText(context, "Неверный формат даты. Используйте формат дд.мм.гггг", Toast.LENGTH_SHORT).show()
                return false
            }

            // ПРОВЕРКА ВРЕМЕНИ (теперь обязательно)
            if (time.isEmpty()) {
                Toast.makeText(context, "Выберите время", Toast.LENGTH_SHORT).show()
                return false
            }

            // Проверка формата времени (HH:mm)
            val timeRegex = Regex("""^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$""")
            if (!time.matches(timeRegex)) {
                Toast.makeText(context, "Неверный формат времени. Используйте формат ЧЧ:мм", Toast.LENGTH_SHORT).show()
                return false
            }

            return true
        }
    }

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
            return super.onCreateDialog(savedInstanceState).apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                window?.setDimAmount(0.5f)
            }
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

            setupEditTexts(editTextTaskName, editTextDeadlineTask, editTextDeadlineTimeTask)
            setupButtons(btnSave, btnDelete, btnCancel, editTextTaskName, editTextDeadlineTask, editTextDeadlineTimeTask)
        }

        private fun setupEditTexts(
            taskNameEditText: EditText,
            deadlineEditText: EditText,
            timeEditText: EditText
        ) {
            taskNameEditText.setText(task.title)
            deadlineEditText.setText(task.deadline)
            timeEditText.setText(task.time)

            deadlineEditText.apply {
                isFocusable = false
                isClickable = true
                setOnClickListener {
                    (requireActivity() as TaskWindowActivity)
                        .showDatePickerDialog(this)
                }
            }

            timeEditText.apply {
                isFocusable = false
                isClickable = true
                setOnClickListener {
                    (requireActivity() as TaskWindowActivity)
                        .showTimePickerDialog(this)
                }
            }
        }

        private fun setupButtons(
            btnSave: FrameLayout,
            btnDelete: FrameLayout,
            btnCancel: FrameLayout,
            taskNameEditText: EditText,
            deadlineEditText: EditText,
            timeEditText: EditText
        ) {
            btnSave.setOnClickListener {
                val updatedTaskName = taskNameEditText.text.toString()
                val updatedDeadline = deadlineEditText.text.toString()
                val updatedTime = timeEditText.text.toString()

                // ПРОВЕРКА НА ПУСТЫЕ ПОЛЯ ПРИ РЕДАКТИРОВАНИИ (время теперь обязательно)
                if (!validateInput(updatedTaskName, updatedDeadline, updatedTime)) {
                    return@setOnClickListener
                }

                val updatedTask = task.copy(
                    title = updatedTaskName,
                    deadline = updatedDeadline,
                    time = updatedTime
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

        private fun validateInput(taskName: String, deadline: String, time: String): Boolean {
            // Проверка названия задачи
            if (taskName.isEmpty()) {
                Toast.makeText(context, "Введите название задачи", Toast.LENGTH_SHORT).show()
                return false
            }

            // Проверка даты
            if (deadline.isEmpty()) {
                Toast.makeText(context, "Выберите дату", Toast.LENGTH_SHORT).show()
                return false
            }

            // Проверка формата даты (должна быть dd.MM.yyyy)
            val dateRegex = Regex("""^\d{2}\.\d{2}\.\d{4}$""")
            if (!deadline.matches(dateRegex)) {
                Toast.makeText(context, "Неверный формат даты. Используйте формат дд.мм.гггг", Toast.LENGTH_SHORT).show()
                return false
            }

            // ПРОВЕРКА ВРЕМЕНИ (теперь обязательно)
            if (time.isEmpty()) {
                Toast.makeText(context, "Выберите время", Toast.LENGTH_SHORT).show()
                return false
            }

            // Проверка формата времени (HH:mm)
            val timeRegex = Regex("""^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$""")
            if (!time.matches(timeRegex)) {
                Toast.makeText(context, "Неверный формат времени. Используйте формат ЧЧ:мм", Toast.LENGTH_SHORT).show()
                return false
            }

            return true
        }
    }
}