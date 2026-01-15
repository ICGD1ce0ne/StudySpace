package com.example.studyspace.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.example.studyspace.R
import com.example.studyspace.main.MainActivity

class GreetRegisteredUser : AppCompatActivity() {

    // UI элементы
    private lateinit var labelName: TextView

    // Константы
    companion object {
        private const val NAVIGATION_DELAY_MS = 2000L // Задержка перехода 2 секунды
        private const val EXTRA_USER_NAME = "user_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting_registered_user)

        initViews()
        displayUserName()
        scheduleNavigationToMain()
    }

    // Инициализация UI элементов
    private fun initViews() {
        labelName = findViewById(R.id.labelName)
    }

    // Отображение имени пользователя
    private fun displayUserName() {
        val userName = intent.getStringExtra(EXTRA_USER_NAME)

        labelName.text = if (!userName.isNullOrEmpty()) {
            userName
        } else {
            "" // Если имя не передано, оставляем пустым
        }
    }

    // Планирование перехода на главный экран с задержкой
    private fun scheduleNavigationToMain() {
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToMainActivity()
        }, NAVIGATION_DELAY_MS)
    }

    // Навигация на главный экран с анимацией
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.fade_in,
            R.anim.fade_out
        )
        ContextCompat.startActivity(this, intent, options.toBundle())
        finish()
    }
}