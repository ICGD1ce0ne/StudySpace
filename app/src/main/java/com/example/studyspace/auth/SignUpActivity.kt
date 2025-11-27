package com.example.studyspace.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.studyspace.R

class SignUpActivity : AppCompatActivity() {

    private lateinit var viewPager: androidx.viewpager2.widget.ViewPager2
    private lateinit var labelSwitch: android.widget.TextView
    private var isLoginPage = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        viewPager = findViewById(R.id.viewPagerForSignUp)
        labelSwitch = findViewById(R.id.labelSwitch)

        setupViewPager()
        setupSwitchListener()
    }

    private fun setupViewPager() {
        viewPager.adapter = AuthPagerAdapter(this)
        viewPager.isUserInputEnabled = false // Отключаем свайпы
    }

    private fun setupSwitchListener() {
        labelSwitch.setOnClickListener {
            if (isLoginPage) {
                // Переключаем на регистрацию
                viewPager.currentItem = 1
                labelSwitch.text = "Уже есть аккаунт? Войти"
            } else {
                // Переключаем на логин
                viewPager.currentItem = 0
                labelSwitch.text = "Еще нет аккаунта? Зарегистрироваться"
            }
            isLoginPage = !isLoginPage
        }
    }

    // Вложенные классы фрагментов
    class LoginFragment : Fragment() {
        override fun onCreateView(
            inflater: android.view.LayoutInflater,
            container: android.view.ViewGroup?,
            savedInstanceState: Bundle?
        ): android.view.View? {
            return inflater.inflate(R.layout.fragment_login, container, false)
        }
    }

    class RegisterFragment : Fragment() {
        override fun onCreateView(
            inflater: android.view.LayoutInflater,
            container: android.view.ViewGroup?,
            savedInstanceState: Bundle?
        ): android.view.View? {
            return inflater.inflate(R.layout.fragment_register, container, false)
        }
    }

    // Adapter для ViewPager2
    inner class AuthPagerAdapter(activity: AppCompatActivity) :
        androidx.viewpager2.adapter.FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> LoginFragment()
                1 -> RegisterFragment()
                else -> LoginFragment()
            }
        }
    }
}