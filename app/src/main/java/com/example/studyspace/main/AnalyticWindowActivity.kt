package com.example.studyspace.main

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studyspace.R

class AnalyticWindowActivity : AppCompatActivity() {

    private lateinit var buttonTaskList: ImageView
    private lateinit var buttonGoal: ImageView
    private lateinit var buttonAnalytic: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytic_window)

        buttonTaskList = findViewById(R.id.buttonTaskList)
        buttonGoal = findViewById(R.id.buttonGoal)
        buttonAnalytic = findViewById(R.id.buttonAnalytic)

        initButtons()
    }

    private fun initButtons() {
        buttonTaskList.setOnClickListener {
            val goToMainMenu = Intent(this, TaskWindowActivity::class.java)
            startActivity(goToMainMenu)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        buttonGoal.setOnClickListener {
            val goToMainMenu = Intent(this, MainActivity::class.java)
            startActivity(goToMainMenu)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        buttonAnalytic.setOnClickListener {
            Toast.makeText(this, "Вы уже в аналитике", Toast.LENGTH_SHORT).show()
        }
    }
}