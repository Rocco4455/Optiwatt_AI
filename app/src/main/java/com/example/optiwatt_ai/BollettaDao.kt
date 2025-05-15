package com.example.optiwatt_ai

import androidx.room.*

@Dao
interface BollettaDao {
    @Insert
    suspend fun inserisci(bolletta: Bolletta): Long

    @Update
    suspend fun aggiorna(bolletta: Bolletta): Int

    @Query("SELECT * FROM Bolletta ORDER BY id DESC")
    suspend fun prendiTutte(): List<Bolletta>

    @Query("SELECT * FROM Bolletta ORDER BY id DESC LIMIT 1")
    suspend fun prendiUltima(): Bolletta?

    @Query("SELECT * FROM Bolletta WHERE id < :id ORDER BY id DESC LIMIT 1")
    suspend fun prendiPenultima(id: Int): Bolletta?

    @Query("SELECT uriImmagine FROM Bolletta ORDER BY id DESC LIMIT 1")
    suspend fun getUltimoPathImmagine(): String?
}
