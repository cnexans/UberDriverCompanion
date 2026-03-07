package com.carlos.uberanalyzer.model

data class TripData(
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
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalKm: Double get() = pickupKm + tripKm
    val totalMinutes: Int get() = pickupMinutes + tripMinutes

    val pesosPorKm: Double
        get() = if (totalKm > 0) price / totalKm else 0.0

    val pesosPorMin: Double
        get() = if (totalMinutes > 0) price / totalMinutes else 0.0

    val pctDistancia: Double
        get() = if (totalKm > 0) (tripKm / totalKm) * 100.0 else 0.0

    val tripKey: String
        get() = "$type|$price|$pickupMinutes|$pickupKm|$tripMinutes|$tripKm"
}
