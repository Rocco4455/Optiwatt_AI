package com.example.optiwatt_ai

import androidx.room.*

@Dao
interface BollettaDao {
    @Insert
    suspend fun inserisci(bolletta: Bolletta)

    @Update
    suspend fun aggiorna(bolletta: Bolletta)

    @Query("SELECT * FROM Bolletta ORDER BY id DESC")
    suspend fun prendiTutte(): List<Bolletta>

    @Query("SELECT * FROM Bolletta ORDER BY id DESC LIMIT 1")
    suspend fun prendiUltima(): Bolletta?

    @Query("SELECT * FROM Bolletta WHERE id < :id ORDER BY id DESC LIMIT 1")
    suspend fun prendiPenultima(id: Int): Bolletta?

    @Query("SELECT uriImmagine FROM Bolletta ORDER BY id DESC LIMIT 1")
    suspend fun getUltimoPathImmagine(): String?

    @Query("SELECT consumoTotale, costoTotale, emissioniCO2 FROM Bolletta ORDER BY id DESC LIMIT 1")
    suspend fun prendiUltimiDatiEssenziali(): DatiBolletta
}
