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

suspend fun calcolaMediaConsumoUltime3(dao: BollettaDao): Double {
    val ultimeBolle: List<Bolletta> = dao.prendiTutte().take(3)
    if (ultimeBolle.isEmpty()) return 0.0
    val somma = ultimeBolle.sumOf { it.consumoTotale }
    return somma / ultimeBolle.size
}

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

        val consumoTextView = findViewById<TextView>(R.id.textConsumptionR)
        val costoTextView = findViewById<TextView>(R.id.textCostR)
        val co2TextView = findViewById<TextView>(R.id.textCO2R)
        val consumoChartTextView = findViewById<TextView>(R.id.textInChartLeft)

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(applicationContext).bollettaDao()
            val dati = dao.prendiUltimiDatiEssenziali()
            val mediaConsumo = calcolaMediaConsumoUltime3(dao)

            val percentuale = if (mediaConsumo != 0.0) {
                (dati.consumoTotale / mediaConsumo).coerceIn(0.0, 1.0).toFloat()
            } else {
                0f
            }

            withContext(Dispatchers.Main) {
                consumoTextView.text = "${dati.consumoTotale} kWh"
                costoTextView.text = "${dati.costoTotale} €"
                co2TextView.text = "${dati.emissioniCO2} kg CO₂"
                consumoChartTextView.text = "$mediaConsumo"

                ringChart.percent = percentuale
                ringChart.invalidate()
            }
        }
    }
}
