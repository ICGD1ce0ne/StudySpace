package com.example.studyspace.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.studyspace.R

class GreetingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var nextButton: FrameLayout
    private lateinit var statusIcon: ImageView
    private lateinit var textNextButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting)

        // Инициализация через findViewById
        viewPager = findViewById(R.id.viewPagerForInfoAboutGreeting)
        nextButton = findViewById(R.id.layoutForNextButton)
        statusIcon = findViewById(R.id.statusIconOneOutOfThree)
        textNextButton = findViewById(R.id.textNextButton)

        setupViewPager()
        setupNextButton()
        updateStatusIcon(0)
    }

    private fun setupViewPager() {
        viewPager.adapter = GreetingPagerAdapter(this)

        // Слушатель изменения страницы для обновления иконки статуса
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateStatusIcon(position)
                updateButtonText(position)
            }
        })
    }

    private fun setupNextButton() {
        nextButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < 2) {
                // Переход на следующую страницу приветствия
                viewPager.currentItem = currentItem + 1
            } else {
                // Последняя страница - переход к экрану авторизации
                val intent = Intent(this, SignUpActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun updateStatusIcon(position: Int) {
        val iconRes = when (position) {
            0 -> R.drawable.icon_one_out_of_three
            1 -> R.drawable.icon_two_out_of_three
            2 -> R.drawable.icon_three_out_of_three
            else -> R.drawable.icon_one_out_of_three
        }
        statusIcon.setImageResource(iconRes)
    }

    private fun updateButtonText(position: Int) {
        textNextButton.text = when (position) {
            2 -> "Начать" // На последней странице
            else -> "Далее"
        }
    }

    // Вложенные классы фрагментов
    class GreetingFragment1 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_greeting_one_out_of_three, container, false)
        }
    }

    class GreetingFragment2 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_greeting_two_out_of_three, container, false)
        }
    }

    class GreetingFragment3 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_greeting_three_out_of_three, container, false)
        }
    }

    // Adapter для ViewPager2
    inner class GreetingPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GreetingFragment1()
                1 -> GreetingFragment2()
                2 -> GreetingFragment3()
                else -> GreetingFragment1()
            }
        }
    }
}