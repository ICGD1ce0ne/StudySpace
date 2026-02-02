package com.example.studyspace.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.studyspace.R
import com.example.studyspace.main.MainActivity

class SignUpActivity : AppCompatActivity() {

    // UI элементы
    private lateinit var viewPager: ViewPager2

    // Хранилище данных пользователя
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        initViews()
        initSharedPreferences()
        setupViewPager()
    }

    // Инициализация UI элементов
    private fun initViews() {
        viewPager = findViewById(R.id.viewPagerForRegister)
    }

    // Инициализация SharedPreferences для хранения данных
    private fun initSharedPreferences() {
        sharedPreferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
    }

    // Настройка ViewPager с фрагментами регистрации
    private fun setupViewPager() {
        viewPager.adapter = SignUpPagerAdapter(this)
        viewPager.isUserInputEnabled = false // Отключаем свайпы для пошагового процесса
    }

    // Переход к следующему фрагменту регистрации
    fun goToNextFragment() {
        val nextItem = viewPager.currentItem + 1
        if (nextItem < (viewPager.adapter?.itemCount ?: 0)) {
            viewPager.currentItem = nextItem
        }
    }

    // Сохранение имени пользователя
    fun saveUserName(name: String) {
        sharedPreferences.edit {
            putString("user_name", name)
        }
    }

    // Сохранение возраста пользователя
    fun saveUserAge(age: String) {
        sharedPreferences.edit {
            putString("user_age", age)
        }
    }

    // Получение сохраненного имени пользователя
    fun getUserName(): String {
        return sharedPreferences.getString("user_name", "") ?: ""
    }

    // Завершение регистрации и переход на главный экран
    fun completeSignUp() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this,
                R.anim.fade_in,
                R.anim.fade_out
            )
            ContextCompat.startActivity(this, intent, options.toBundle())
            finish()
        }, 2000) // Задержка 2 секунды для показа финального экрана
    }

    // Адаптер для ViewPager с фрагментами регистрации
    inner class SignUpPagerAdapter(fa: AppCompatActivity) : FragmentStateAdapter(fa) {

        // Всего 4 шага регистрации (хотя фрагмента с номером 3 нет в коде)
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> SignUpOneFragment()  // Ввод имени
                1 -> SignUpTwoFragment()  // Ввод возраста
                2 -> SignUpFourFragment() // Финальный экран
                else -> SignUpOneFragment() // Fallback
            }
        }
    }
}

// Фрагмент 1: Ввод имени пользователя
class SignUpOneFragment : Fragment() {

    private lateinit var editTextName: EditText
    private lateinit var layoutForNextButton: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up_one_out_of_four, container, false)

        initViews(view)
        setupNextButtonListener()

        return view
    }

    // Инициализация UI элементов фрагмента
    private fun initViews(view: View) {
        editTextName = view.findViewById(R.id.editTextName)
        layoutForNextButton = view.findViewById(R.id.layoutForNextButton)
    }

    // Настройка обработчика клика на кнопку "Далее"
    private fun setupNextButtonListener() {
        layoutForNextButton.setOnClickListener {
            if (validateName()) {
                saveNameAndNavigate()
            }
        }
    }

    // Валидация введенного имени
    private fun validateName(): Boolean {
        val name = editTextName.text.toString().trim()

        return when {
            name.isEmpty() -> {
                showValidationError("Введите имя")
                false
            }
            name.length < 2 -> {
                showValidationError("Имя должно содержать минимум 2 символа")
                false
            }
            else -> true
        }
    }

    // Сохранение имени и переход к следующему шагу
    private fun saveNameAndNavigate() {
        val userName = editTextName.text.toString().trim()
        (activity as? SignUpActivity)?.let { signUpActivity ->
            signUpActivity.saveUserName(userName)
            signUpActivity.goToNextFragment()
        }
    }

    // Показ ошибки валидации
    private fun showValidationError(message: String) {
        editTextName.error = message
        // Дополнительно можно показать Toast для лучшей заметности
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

// Фрагмент 2: Ввод возраста пользователя
class SignUpTwoFragment : Fragment() {

    private lateinit var editTextAge: EditText
    private lateinit var layoutForNextButton: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up_two_out_of_four, container, false)

        initViews(view)
        setupNextButtonListener()

        return view
    }

    // Инициализация UI элементов фрагмента
    private fun initViews(view: View) {
        editTextAge = view.findViewById(R.id.editTextAge)
        layoutForNextButton = view.findViewById(R.id.layoutForNextButton)
    }

    // Настройка обработчика клика на кнопку "Далее"
    private fun setupNextButtonListener() {
        layoutForNextButton.setOnClickListener {
            if (validateAge()) {
                saveAgeAndNavigate()
            }
        }
    }

    // Валидация введенного возраста
    private fun validateAge(): Boolean {
        val ageText = editTextAge.text.toString().trim()

        return when {
            ageText.isEmpty() -> {
                showValidationError("Введите ваш возраст")
                false
            }
            else -> {
                val age = ageText.toIntOrNull()
                if (age == null) {
                    showValidationError("Возраст должен быть числом")
                    false
                } else if (age !in 6..100) {
                    showValidationError("Введите корректный возраст (6-100)")
                    false
                } else {
                    true
                }
            }
        }
    }

    // Сохранение возраста и переход к следующему шагу
    private fun saveAgeAndNavigate() {
        val userAge = editTextAge.text.toString().trim()
        (activity as? SignUpActivity)?.let { signUpActivity ->
            signUpActivity.saveUserAge(userAge)
            signUpActivity.goToNextFragment()
        }
    }

    // Показ ошибки валидации
    private fun showValidationError(message: String) {
        editTextAge.error = message
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

// Фрагмент 4: Финальный экран регистрации
class SignUpFourFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up_four_out_of_four, container, false)

        displayUserName(view)
        scheduleNavigationToMain()

        return view
    }

    // Отображение имени пользователя на финальном экране
    private fun displayUserName(view: View) {
        val userName = (activity as? SignUpActivity)?.getUserName() ?: ""
        val labelName = view.findViewById<TextView>(R.id.labelName)

        if (userName.isNotEmpty()) {
            labelName.text = userName
        }
    }

    // Запланированный переход на главный экран через 2 секунды
    private fun scheduleNavigationToMain() {
        Handler(Looper.getMainLooper()).postDelayed({
            (activity as? SignUpActivity)?.completeSignUp()
        }, 4000) // 2 секунды задержки для показа финального экрана
    }
}