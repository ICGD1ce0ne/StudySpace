package com.example.studyspace.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.studyspace.R

class GreetingActivity : AppCompatActivity() {

    // Элементы UI
    private lateinit var viewPager: ViewPager2
    private lateinit var nextButton: FrameLayout
    private lateinit var statusIcon: ImageView
    private lateinit var textNextButton: TextView

    // Настройки приложения
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting)

        initViews()
        initSharedPreferences()
        checkUserStatus()
    }

    // Инициализация View элементов
    private fun initViews() {
        viewPager = findViewById(R.id.viewPagerForInfoAboutGreeting)
        nextButton = findViewById(R.id.layoutForNextButton)
        statusIcon = findViewById(R.id.statusIconOneOutOfThree)
        textNextButton = findViewById(R.id.textNextButton)
    }

    // Инициализация SharedPreferences для хранения данных пользователя
    private fun initSharedPreferences() {
        sharedPreferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
    }

    // Проверка статуса пользователя (зарегистрирован или нет)
    private fun checkUserStatus() {
        val userName = sharedPreferences.getString("user_name", null)

        if (userName != null) {
            // Пользователь зарегистрирован - переходим к приветствию
            navigateToGreetRegisteredUser(userName)
        } else {
            // Новый пользователь - показываем приветственные экраны
            setupGreetingFlow()
        }
    }

    // Навигация к экрану приветствия зарегистрированного пользователя
    private fun navigateToGreetRegisteredUser(userName: String) {
        val intent = Intent(this, GreetRegisteredUser::class.java).apply {
            putExtra("user_name", userName)
        }
        startActivity(intent)
        finish()
    }

    // Настройка приветственного потока (три экрана)
    private fun setupGreetingFlow() {
        setupViewPager()
        setupNextButton()
        updateStatusIcon(0) // Начинаем с первой страницы
    }

    // Настройка ViewPager с тремя фрагментами приветствия
    private fun setupViewPager() {
        viewPager.adapter = GreetingPagerAdapter(this)

        // Слушатель изменения страницы для обновления UI
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateStatusIcon(position)
                updateButtonText(position)
            }
        })
    }

    // Настройка кнопки "Далее/Начать"
    private fun setupNextButton() {
        nextButton.setOnClickListener {
            handleNextButtonClick()
        }
    }

    // Обработка клика по кнопке "Далее/Начать"
    private fun handleNextButtonClick() {
        val currentItem = viewPager.currentItem

        if (currentItem < TOTAL_GREETING_PAGES - 1) {
            // Переход на следующую страницу приветствия
            viewPager.currentItem = currentItem + 1
        } else {
            // Последняя страница - переход к регистрации
            navigateToSignUp()
        }
    }

    // Навигация к экрану регистрации
    private fun navigateToSignUp() {
        val intent = Intent(this, SignUpActivity::class.java)
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.fade_in,
            R.anim.fade_out
        )
        ContextCompat.startActivity(this, intent, options.toBundle())
        finish()
    }

    // Обновление индикатора текущей страницы (1/3, 2/3, 3/3)
    private fun updateStatusIcon(position: Int) {
        val iconRes = when (position) {
            0 -> R.drawable.icon_one_out_of_three
            1 -> R.drawable.icon_two_out_of_three
            2 -> R.drawable.icon_three_out_of_three
            else -> R.drawable.icon_one_out_of_three // Fallback
        }
        statusIcon.setImageResource(iconRes)
    }

    // Обновление текста кнопки в зависимости от страницы
    private fun updateButtonText(position: Int) {
        textNextButton.text = when (position) {
            LAST_PAGE_INDEX -> "Начать" // На последней странице
            else -> "Далее"
        }
    }

    companion object {
        private const val TOTAL_GREETING_PAGES = 3
        private const val LAST_PAGE_INDEX = TOTAL_GREETING_PAGES - 1
    }

    // Фрагмент для первой страницы приветствия
    class GreetingFragment1 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_greeting_one_out_of_three, container, false)
        }
    }

    // Фрагмент для второй страницы приветствия
    class GreetingFragment2 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_greeting_two_out_of_three, container, false)
        }
    }

    // Фрагмент для третьей страницы приветствия
    class GreetingFragment3 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_greeting_three_out_of_three, container, false)
        }
    }

    // Адаптер для ViewPager2 с тремя фрагментами приветствия
    inner class GreetingPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = TOTAL_GREETING_PAGES

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GreetingFragment1()
                1 -> GreetingFragment2()
                2 -> GreetingFragment3()
                else -> GreetingFragment1() // Fallback
            }
        }
    }
}