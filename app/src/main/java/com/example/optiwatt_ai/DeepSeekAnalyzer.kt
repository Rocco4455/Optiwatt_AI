package com.example.optiwatt_ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.*
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
                val testoOCR = eseguiTesseractOCR(context, bitmap)
                analizzaCampiSeparati(context, testoOCR, bolletta)
            } catch (e: Exception) {
                showToast(context, "Errore OCR: ${e.message}")
            }
        }
    }

    private fun eseguiTesseractOCR(context: Context, bitmap: Bitmap): String {
        val tess = TessBaseAPI()
        val tessDataPath = "${context.filesDir}/tesseract/"
        val lang = "ita"

        // Copia traineddata da assets se non esiste
        val trainedDataFile = File("$tessDataPath/tessdata/ita.traineddata")
        if (!trainedDataFile.exists()) {
            trainedDataFile.parentFile?.mkdirs()
            context.assets.open("tessdata/ita.traineddata").use { input ->
                trainedDataFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        tess.init(tessDataPath, lang)
        tess.setImage(bitmap)
        val testo = tess.utF8Text
        tess.end()
        return testo ?: ""
    }

    private fun analizzaCampiSeparati(context: Context, testo: String, bolletta: Bolletta) {
        val dao = AppDatabase.getInstance(context).bollettaDao()
        val client = OkHttpClient()

        val campi = listOf(
            "consumoTotale" to "Estrai solo il valore del consumo totale in kWh. Esempio: 125.4",
            "costoTotale" to "Estrai solo il costo totale in euro. Esempio: 87.90",
            "emissioniCO2" to "Estrai solo il valore delle emissioni di CO2 in kg. Esempio: 23.5",
            "nomeFornitore" to "Estrai solo il nome del fornitore. Esempio: Enel",
            "data" to "Estrai solo la data della bolletta nel formato GG/MM/AAAA. Esempio: 10/05/2025",
            "previsioneConsumo" to "Prevedi solo il prossimo consumo in kWh. Esempio: 150.0",
            "previsioneCosto" to "Prevedi solo il prossimo costo in euro. Esempio: 91.0",
            "tip1" to "Fornisci un consiglio per risparmiare energia.",
            "tip2" to "Fornisci un secondo consiglio per risparmiare energia.",
            "tip3" to "Fornisci un terzo consiglio per risparmiare energia."
        )

        val risultati = mutableMapOf<String, String>()

        CoroutineScope(Dispatchers.IO).launch {
            for ((campo, domanda) in campi) {
                val risposta = inviaPrompt(client, testo, domanda)
                risultati[campo] = risposta ?: "N/D"
                delay(1500)
            }

            val aggiornata = bolletta.copy(
                consumoTotale = risultati["consumoTotale"] ?: "N/D",
                costoTotale = risultati["costoTotale"] ?: "N/D",
                emissioniCO2 = risultati["emissioniCO2"] ?: "N/D",
                nomeFornitore = risultati["nomeFornitore"] ?: "Sconosciuto",
                data = risultati["data"] ?: "N/D",
                previsioneConsumo = risultati["previsioneConsumo"] ?: "N/D",
                previsioneCosto = risultati["previsioneCosto"] ?: "N/D",
                tip1 = risultati["tip1"] ?: "",
                tip2 = risultati["tip2"] ?: "",
                tip3 = risultati["tip3"] ?: ""
            )

            dao.aggiorna(aggiornata)
            showToast(context, "✅ Bolletta aggiornata con Tesseract + IA")
        }
    }

    private suspend fun inviaPrompt(client: OkHttpClient, testo: String, domanda: String): String? {
        return withContext(Dispatchers.IO) {
            val prompt = """
                Questo è il testo OCR di una bolletta:

                $testo

                $domanda
                Rispondi con un solo valore nella prima riga. Nessuna spiegazione.
            """.trimIndent()

            val json = """
                {
                  "model": "deepseek-chat",
                  "messages": [{"role": "user", "content": "$prompt"}],
                  "temperature": 0.3
                }
            """.trimIndent()

            val body = json.toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            try {
                val response = client.newCall(request).execute()
                val risposta = response.body?.string()?.trim()
                println("🧠 Prompt IA:\n$prompt\n📥 Risposta: $risposta")
                if (response.isSuccessful && !risposta.isNullOrBlank()) {
                    risposta.lines().firstOrNull()?.trim()
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
