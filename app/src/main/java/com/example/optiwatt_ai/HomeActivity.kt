// HomeActivity.kt
package com.example.optiwatt_ai

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DatiBolletta(
    val consumoTotale: Double,
    val costoTotale: Double,
    val emissioniCO2: Double
)

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val ringChart = findViewById<RingChartView>(R.id.ringChart)
        ringChart.percent = 0.75f
        ringChart.invalidate()

        val uploadButton = findViewById<ImageView>(R.id.UploadButton)
        uploadButton.setOnClickListener {
            val intent = Intent(this, INPUT_img::class.java)
            startActivity(intent)
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

        // Recupera e aggiorna i valori dalla bolletta più recente
        val consumoTextView = findViewById<TextView>(R.id.textConsumptionR)
        val costoTextView = findViewById<TextView>(R.id.textCostR)
        val co2TextView = findViewById<TextView>(R.id.textCO2R)

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(applicationContext).bollettaDao()
            val dati = dao.prendiUltimiDatiEssenziali()

            withContext(Dispatchers.Main) {
                consumoTextView.text = "${dati.consumoTotale} kWh"
                costoTextView.text = "${dati.costoTotale} €"
                co2TextView.text = "${dati.emissioniCO2} kg CO₂"
            }
        }
    }
}
