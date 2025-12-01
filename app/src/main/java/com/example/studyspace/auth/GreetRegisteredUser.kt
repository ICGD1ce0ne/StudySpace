package com.example.studyspace.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.studyspace.R
import com.example.studyspace.main.MainActivity

class GreetRegisteredUser  : AppCompatActivity() {

    private lateinit var labelName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting_registered_user)

        labelName = findViewById(R.id.labelName)

        val userName = intent.getStringExtra("user_name")

        if (userName != null && userName.isNotEmpty()) {
            labelName.text = userName
        } else {
            labelName.text = ""
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000)
    }
}