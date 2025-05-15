package com.example.optiwatt_ai

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Bolletta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uriImmagine: String,
    val consumoTotale: String,
    val costoTotale: String,
    val emissioniCO2: String,
    val previsioneCosto: String,
    val nomeFornitore: String,
    val previsioneConsumo: String,
    val data: String,
    val tip1: String,
    val tip2: String,
    val tip3: String
)

