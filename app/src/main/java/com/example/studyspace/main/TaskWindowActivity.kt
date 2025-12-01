package com.example.studyspace.main

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studyspace.R

class TaskWindowActivity : AppCompatActivity() {

    private lateinit var buttonTaskList: ImageView
    private lateinit var buttonGoal: ImageView
    private lateinit var buttonAnalytic: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_window)

        buttonTaskList = findViewById(R.id.buttonTaskList)
        buttonGoal = findViewById(R.id.buttonGoal)
        buttonAnalytic = findViewById(R.id.buttonAnalytic)

        initButtons()
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
    }
}