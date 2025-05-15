package com.example.optiwatt_ai

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions // MODIFICATO: import corretto per Latin OCR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

object DeepSeekAnalyzer {

    private const val API_KEY = "sk-c7777518861542acb392d6d3619997ac"
    private const val ENDPOINT = "https://api.deepseek.com/v1/chat/completions"

    fun analizzaUltimaBolletta(context: Context) {
        val dao = AppDatabase.getInstance(context).bollettaDao()

        CoroutineScope(Dispatchers.IO).launch {
            val path = dao.getUltimoPathImmagine() ?: return@launch
            val bolletta = dao.prendiUltima() ?: return@launch
            val file = File(path)

            if (!file.exists()) {
                showToast(context, "File immagine non trovato")
                return@launch
            }

            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val image = InputImage.fromBitmap(bitmap, 0)

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) // MODIFICATO

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val testo = visionText.text
                        val estratti = estraiDatiDaOCR(testo)
                        val aggiornata = bolletta.copy(
                            consumoTotale = estratti.consumo,
                            costoTotale = estratti.costo,
                            emissioniCO2 = estratti.co2,
                            nomeFornitore = estratti.fornitore,
                            data = estratti.data
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            dao.aggiorna(aggiornata)
                            inviaTestoADeepSeek(context, testo, aggiornata)
                        }
                    }
                    .addOnFailureListener { e ->
                        showToast(context, "OCR fallito: ${e.message}")
                    }

            } catch (e: Exception) {
                showToast(context, "Errore durante OCR: ${e.message}")
            }
        }
    }

    private fun estraiDatiDaOCR(testo: String): DatiEstratti {
        val consumoRegex = Regex("(?i)consumo.*?(\\d+[,.]?\\d*)\\s*kWh")
        val costoRegex = Regex("(?i)costo.*?(\\d+[,.]?\\d*)\\s*€")
        val co2Regex = Regex("(?i)emissioni.*?(\\d+[,.]?\\d*)\\s*kg")
        val fornitoreRegex = Regex("(?i)fornitore[:\\n\\s]+([a-zA-Z0-9 ]+)")
        val dataRegex = Regex("(?i)data[:\\n\\s]+(\\d{2}/\\d{2}/\\d{4})")

        val consumo = consumoRegex.find(testo)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val costo = costoRegex.find(testo)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val co2 = co2Regex.find(testo)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val fornitore = fornitoreRegex.find(testo)?.groupValues?.get(1)?.trim() ?: "Sconosciuto"
        val data = dataRegex.find(testo)?.groupValues?.get(1) ?: "N/D"

        return DatiEstratti(consumo, costo, co2, fornitore, data)
    }

    private fun inviaTestoADeepSeek(context: Context, testo: String, bolletta: Bolletta) {
        val client = OkHttpClient()
        val dao = AppDatabase.getInstance(context).bollettaDao()

        CoroutineScope(Dispatchers.IO).launch {
            val penultima = dao.prendiPenultima(bolletta.id)

            val testoConfronto = penultima?.let {
                "\nBolletta precedente:\n" +
                        "Consumo: ${it.consumoTotale} kWh, Costo: €${it.costoTotale}, CO2: ${it.emissioniCO2} kg\n"
            } ?: ""

            val prompt = """
                Questa è una bolletta elettrica:
                $testo

                $testoConfronto
                Fornisci:
                - 3 consigli per risparmiare energia
                - Una previsione del consumo e del costo della prossima bolletta
            """.trimIndent()

            val json = """
                {
                  "model": "deepseek-chat",
                  "messages": [{
                    "role": "user",
                    "content": "$prompt"
                  }],
                  "temperature": 0.7
                }
            """.trimIndent()

            val body = json.toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showToast(context, "Errore rete: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val risposta = response.body?.string()
                    if (risposta == null) {
                        showToast(context, "Risposta vuota da DeepSeek")
                        return
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        val righe = risposta.lines().map { it.trim() }
                        val consigli = righe.filter { it.startsWith("1.") || it.startsWith("2.") || it.startsWith("3.") }

                        val previsioneConsumo = Regex("(?i)consumo.*?(\\d+[,.]?\\d*)\\s*kWh")
                            .find(risposta)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                        val previsioneCosto = Regex("(?i)costo.*?(\\d+[,.]?\\d*)\\s*€")
                            .find(risposta)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

                        val aggiornata = bolletta.copy(
                            tip1 = consigli.getOrNull(0) ?: "",
                            tip2 = consigli.getOrNull(1) ?: "",
                            tip3 = consigli.getOrNull(2) ?: "",
                            previsioneConsumo = previsioneConsumo,
                            previsioneCosto = previsioneCosto
                        )

                        dao.aggiorna(aggiornata)
                    }
                }
            })
        }
    }

    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    data class DatiEstratti(
        val consumo: Double,
        val costo: Double,
        val co2: Double,
        val fornitore: String,
        val data: String
    )
}
