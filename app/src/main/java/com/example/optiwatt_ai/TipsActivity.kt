package com.example.optiwatt_ai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TipsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_tips)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val menuButton = findViewById<ImageView>(R.id.menuButton)
        menuButton.setOnClickListener {
            val popup = PopupMenu(this, menuButton)
            popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.go_to_provider -> {
                        startActivity(Intent(this, ProviderActivity::class.java))
                        true
                    }
                    R.id.go_to_home -> {
                        startActivity(Intent(this, HomeActivity::class.java))
                        true
                    }
                    R.id.go_to_tips -> {
                        startActivity(Intent(this, TipsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        val tip1Text = findViewById<TextView>(R.id.tip1Text)
        val tip2Text = findViewById<TextView>(R.id.tip2Text)
        val tip3Text = findViewById<TextView>(R.id.tip3Text)
        val previsioneText = findViewById<TextView>(R.id.previsioneText)
        val loadingSpinner = findViewById<ProgressBar>(R.id.loadingSpinner)
        val loadingText = findViewById<TextView>(R.id.loadingText)

        // Mostra spinner e testo di caricamento
        loadingSpinner.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                DeepSeekAnalyzer.generaConsigliIA(applicationContext)
            }

            val bolletta = AppDatabase.getInstance(this@TipsActivity).bollettaDao().prendiUltima()
            bolletta?.let {
                tip1Text.text = it.tip1
                tip2Text.text = it.tip2
                tip3Text.text = it.tip3
                previsioneText.text = "Prossima bolletta: ${it.previsioneConsumo} kWh, €${it.previsioneCosto}"
            }

            // Nascondi caricamento
            loadingSpinner.visibility = View.GONE
            loadingText.visibility = View.GONE
        }
    }
}
