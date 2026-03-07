package com.carlos.uberanalyzer.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val subtype: String?,
    val price: Double,
    val bonus: Double?,
    val pickupMinutes: Int,
    val pickupKm: Double,
    val pickupAddress: String?,
    val tripMinutes: Int,
    val tripKm: Double,
    val destination: String?,
    val passengerRating: String?,
    val identity: String?,
    val pesosPorKm: Double,
    val pesosPorMin: Double,
    val pctDistancia: Double,
    val timestamp: Long
)
