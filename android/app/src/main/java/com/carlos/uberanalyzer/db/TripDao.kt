package com.carlos.uberanalyzer.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity)

    @Query("SELECT * FROM trips ORDER BY timestamp DESC")
    suspend fun getAll(): List<TripEntity>

    @Query("DELETE FROM trips")
    suspend fun deleteAll()

    @Query("SELECT * FROM trips WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    suspend fun getToday(startOfDay: Long): List<TripEntity>

    @Query("SELECT COUNT(*) as count, AVG(pesosPorKm) as avgPorKm, AVG(pesosPorMin) as avgPorMin, SUM(price) as totalEarnings FROM trips WHERE timestamp >= :startOfDay")
    suspend fun getStatsToday(startOfDay: Long): DayStats
}

data class DayStats(
    val count: Int,
    val avgPorKm: Double?,
    val avgPorMin: Double?,
    val totalEarnings: Double?
)
