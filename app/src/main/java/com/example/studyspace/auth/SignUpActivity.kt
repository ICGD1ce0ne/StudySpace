package com.example.studyspace.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.studyspace.R
import com.example.studyspace.main.MainActivity

class SignUpActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        viewPager = findViewById(R.id.viewPagerForRegister)
        viewPager.adapter = SignUpPagerAdapter(this)
        viewPager.isUserInputEnabled = false // Отключаем свайпы
    }

    fun goToNextFragment() {
        val nextItem = viewPager.currentItem + 1
        if (nextItem < (viewPager.adapter?.itemCount ?: 0)) {
            viewPager.currentItem = nextItem
        }
    }

    fun setUserName(name: String) {
        this.userName = name
    }

    fun getUserName(): String {
        return userName
    }

    fun completeSignUp() {
        // Ждем 2 секунды на экране приветствия, затем переходим на MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Закрываем SignUpActivity чтобы нельзя было вернуться назад
        }, 2000)
    }

    inner class SignUpPagerAdapter(fa: AppCompatActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> SignUpOneFragment()
                1 -> SignUpTwoFragment()
                2 -> SignUpThreeFragment()
                3 -> SignUpFourFragment().apply {
                    // Передаем имя в четвертый фрагмент
                    arguments = Bundle().apply {
                        putString("user_name", getUserName())
                    }
                }
                else -> SignUpOneFragment()
            }
        }
    }
}

class SignUpOneFragment : Fragment() {
    private lateinit var editTextName: EditText
    private lateinit var layoutForNextButton: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up_one_out_of_four, container, false)

        editTextName = view.findViewById(R.id.editTextName)
        layoutForNextButton = view.findViewById(R.id.layoutForNextButton)

        layoutForNextButton.setOnClickListener {
            if (validateName()) {
                // Сохраняем имя в Activity перед переходом
                (activity as? SignUpActivity)?.setUserName(editTextName.text.toString().trim())
                (activity as? SignUpActivity)?.goToNextFragment()
            }
        }

        return view
    }

    private fun validateName(): Boolean {
        val name = editTextName.text.toString().trim()
        return if (name.length >= 2) {
            true
        } else {
            editTextName.error = "Введите имя (минимум 2 символа)"
            false
        }
    }
}

class SignUpTwoFragment : Fragment() {
    private lateinit var editTextAge: EditText
    private lateinit var layoutForNextButton: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up_two_out_of_four, container, false)

        editTextAge = view.findViewById(R.id.editTextAge)
        layoutForNextButton = view.findViewById(R.id.layoutForNextButton)

        layoutForNextButton.setOnClickListener {
            if (validateAge()) {
                (activity as? SignUpActivity)?.goToNextFragment()
            }
        }

        return view
    }

    private fun validateAge(): Boolean {
        val ageText = editTextAge.text.toString().trim()
        return if (ageText.isNotEmpty()) {
            val age = ageText.toIntOrNull()
            if (age != null && age in 6..100) {
                true
            } else {
                editTextAge.error = "Введите корректный возраст (6-100)"
                false
            }
        } else {
            editTextAge.error = "Введите ваш возраст"
            false
        }
    }
}

class SignUpThreeFragment : Fragment() {
    private lateinit var editTextClass: EditText
    private lateinit var layoutForNextButton: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up_three_out_of_four, container, false)

        editTextClass = view.findViewById(R.id.editTextClass)
        layoutForNextButton = view.findViewById(R.id.layoutForNextButton)

        layoutForNextButton.setOnClickListener {
            if (validateClass()) {
                (activity as? SignUpActivity)?.goToNextFragment()
            }
        }

        return view
    }

    private fun validateClass(): Boolean {
        val classText = editTextClass.text.toString().trim()
        return if (classText.isNotEmpty()) {
            val classNumber = classText.toIntOrNull()
            if (classNumber != null && classNumber in 1..11) {
                true
            } else {
                editTextClass.error = "Введите корректный класс (1-11)"
                false
            }
        } else {
            editTextClass.error = "Введите ваш класс"
            false
        }
    }
}

class SignUpFourFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up_four_out_of_four, container, false)

        // Получаем имя из аргументов
        val userName = arguments?.getString("user_name") ?: ""
        val labelName = view.findViewById<TextView>(R.id.labelName)

        // Устанавливаем имя пользователя
        if (userName.isNotEmpty()) {
            labelName.text = userName
        }

        // Автоматически запускаем переход на MainActivity через 2 секунды
        Handler(Looper.getMainLooper()).postDelayed({
            (activity as? SignUpActivity)?.completeSignUp()
        }, 2000)

        return view
    }
}