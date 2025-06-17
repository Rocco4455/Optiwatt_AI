package com.example.optiwatt_ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

object DeepSeekAnalyzer {

    private const val API_KEY = "sk-82448023cbeb44acb4b262ce5d38c8e8"
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
                val testoPulito = pulisciTestoOCR(testoOCR)
                Log.d("OCR_TESSERACT", "🧼 Testo OCR pulito:\n$testoPulito")

                analizzaDatiBolletta(context, testoPulito, bolletta)
                generaConsigliIA(context)

            } catch (e: Exception) {
                showToast(context, "Errore OCR: ${e.message}")
            }
        }
    }

    private fun eseguiTesseractOCR(context: Context, bitmap: Bitmap): String {
        val tess = TessBaseAPI()
        val tessDataPath = "${context.filesDir}/tesseract/"
        val lang = "ita"

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
        val testo = tess.utF8Text ?: ""
        Log.d("OCR_TESSERACT", "📄 Testo OCR rilevato:\n$testo")
        tess.end()
        return testo
    }

    private fun pulisciTestoOCR(testo: String): String {
        return testo
            .replace(Regex("[^\\p{Print}\\n]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private suspend fun calcolaMediaConsumo(dao: BollettaDao): Double {
        val bollette = dao.prendiTutte().sortedByDescending { it.id }.take(3)
        val valori = bollette.mapNotNull {
            it.consumoTotale.replace(",", ".").replace(Regex("[^\\d.]"), "").toDoubleOrNull()
        }
        return if (valori.isNotEmpty()) valori.average() else 0.0
    }

    private suspend fun calcolaMediaCosto(dao: BollettaDao): Double {
        val bollette = dao.prendiTutte().sortedByDescending { it.id }.take(3)
        val valori = bollette.mapNotNull {
            it.costoTotale.replace(",", ".").replace(Regex("[^\\d.]"), "").toDoubleOrNull()
        }
        return if (valori.isNotEmpty()) valori.average() else 0.0
    }

    fun analizzaDatiBolletta(context: Context, testo: String, bolletta: Bolletta) {
        val dao = AppDatabase.getInstance(context).bollettaDao()
        val client = OkHttpClient()

        CoroutineScope(Dispatchers.IO).launch {
            val mediaConsumo = calcolaMediaConsumo(dao)
            val mediaCosto = calcolaMediaCosto(dao)

            val campi = listOf(
                "consumoTotale" to "Estrai solo il valore del consumo totale in kWh. Esempio: 125.4, di fianco al valore scrivi KWh",
                "costoTotale" to "Estrai solo il costo totale in euro. Esempio: 87.90, affianco scrivi simbolo euro",
                "emissioniCO2" to "Calcola una stima delle emissioni di CO2 in kg, basata su un consumo elettrico kWh. Fornisci solo un numero seguito da kg.",
                "nomeFornitore" to "Estrai solo il nome del fornitore. Esempio: Enel",
                "data" to "Estrai solo la data della bolletta nel formato GG/MM/AAAA. Esempio: 10/05/2025",
                "previsioneConsumo" to "Considera la media dei consumi delle ultime bollette: $mediaConsumo. Fornisci un valore previsto basato su stagionalità e abitudini.",
                "previsioneCosto" to "Considera la media dei costi delle ultime bollette: $mediaCosto. Fornisci un valore previsto basato su tariffe e abitudini."
            )

            val risultati = mutableMapOf<String, String>()

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
                previsioneCosto = risultati["previsioneCosto"] ?: "N/D"
            )

            dao.aggiorna(aggiornata)
            showToast(context, "✅ Dati bolletta aggiornati con IA")
        }
    }

    fun generaConsigliIA(context: Context) {
        val dao = AppDatabase.getInstance(context).bollettaDao()
        val client = OkHttpClient()

        CoroutineScope(Dispatchers.IO).launch {
            val bolletta = dao.prendiUltima() ?: return@launch
            val testo = "Testo OCR bolletta non richiesto, fornisci direttamente consigli."

            val campi = listOf(
                "tip1" to "Fornisci un consiglio pratico per risparmiare energia.",
                "tip2" to "Fornisci un secondo consiglio utile per risparmiare energia, basato su fonti online.",
                "tip3" to "Fornisci un terzo consiglio efficace per ridurre il consumo energetico domestico."
            )

            val risultati = mutableMapOf<String, String>()

            for ((campo, domanda) in campi) {
                val risposta = inviaPrompt(client, testo, domanda)
                risultati[campo] = risposta ?: ""
                delay(1500)
            }

            val aggiornata = bolletta.copy(
                tip1 = risultati["tip1"] ?: "",
                tip2 = risultati["tip2"] ?: "",
                tip3 = risultati["tip3"] ?: ""
            )

            dao.aggiorna(aggiornata)
            showToast(context, "✨ Consigli IA aggiornati")
        }
    }

    private suspend fun inviaPrompt(client: OkHttpClient, testo: String, domanda: String): String? {
        return withContext(Dispatchers.IO) {
            val prompt = """
                Questo è il testo OCR di una bolletta elettrica:

                $testo

                $domanda
                Rispondi con un solo valore nella prima riga. Nessuna spiegazione.
            """.trimIndent()

            val json = """
                {
                  "model": "deepseek-chat",
                  "messages": [{"role": "user", "content": ${JSONObject.quote(prompt)}}],
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
                val raw = response.body?.string()?.trim() ?: ""
                Log.d("DEEPSEEK_AI", "📤 Prompt inviato:\n$prompt")
                Log.d("DEEPSEEK_AI", "📥 Risposta ricevuta:\n$raw")

                if (response.isSuccessful) {
                    val jsonObj = JSONObject(raw)
                    jsonObj.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                        .lines()
                        .firstOrNull()
                        ?.trim()
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
