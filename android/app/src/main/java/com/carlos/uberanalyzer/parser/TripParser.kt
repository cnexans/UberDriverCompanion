package com.carlos.uberanalyzer.parser

import com.carlos.uberanalyzer.model.TripData

object TripParser {

    private val PRICE_REGEX = Regex("""^\$\s?([0-9.,]+)$""")
    private val BONUS_REGEX = Regex("""\+\$ ([0-9.,]+) incluido""")
    private val PICKUP_REGEX = Regex("""A\s?(\d+) min \(([0-9.]+) km\)""")
    private val TRIP_REGEX = Regex("""Viaje: (\d+) min \(([0-9.]+) km\)""")
    private val RATING_REGEX = Regex("""([0-9],[0-9]{2}) \((\d+)\)""")
    private val IDENTITY_REGEX = Regex("""(?i)(DNI|Identidad|verificad|Pasaporte|Licencia)""")

    fun parse(texts: List<String>): TripData? {
        // Detect trip by presence of pickup AND trip distance info
        val hasPickup = texts.any { PICKUP_REGEX.containsMatchIn(it) }
        val hasTrip = texts.any { TRIP_REGEX.containsMatchIn(it) }
        if (!hasPickup && !hasTrip) return null

        // Find the "Aceptar" button position - the active trip is near it
        val aceptarIndex = texts.indexOfLast { it.contains("Aceptar") }

        // Price: find "$ X.XXX" closest to Aceptar if present, otherwise first valid
        val priceLines = texts.filter {
            PRICE_REGEX.matches(it) && it != "$ 0,00" && it != "$ 0" && it != "\$0" &&
            !it.contains("adicionales") && !it.contains("Incentivo")
        }
        val priceLine = if (aceptarIndex >= 0 && priceLines.size > 1) {
            // Pick price closest to "Aceptar" button
            priceLines.minByOrNull { kotlin.math.abs(texts.indexOf(it) - aceptarIndex) }
        } else {
            priceLines.firstOrNull()
        }
        val priceMatch = priceLine?.let { PRICE_REGEX.find(it) }
        val price = priceMatch?.groupValues?.get(1)?.parseArgentineNumber()

        // Type: look for known trip types in the texts (exact match or contained)
        val knownTypes = listOf("Moto", "Auto", "Artículo", "Exclusivo", "Flash", "Eco")
        val typeFromTexts = texts.firstOrNull { text -> knownTypes.any { text == it } }
            ?: texts.firstOrNull { text -> knownTypes.any { text.contains(it) } }?.let { text ->
                knownTypes.firstOrNull { text.contains(it) }
            }
        val mainType = typeFromTexts ?: "Viaje"

        // Check for subtype - find a second type that differs from the main one
        val subtype = texts.firstOrNull { text ->
            val matchedType = knownTypes.firstOrNull { text == it || text.contains(it) }
            matchedType != null && matchedType != mainType
        }?.let { text -> knownTypes.firstOrNull { text == it || text.contains(it) } }

        // Bonus
        val bonusLine = texts.firstOrNull { BONUS_REGEX.containsMatchIn(it) }
        val bonusMatch = bonusLine?.let { BONUS_REGEX.find(it) }
        val bonus = bonusMatch?.groupValues?.get(1)?.parseArgentineNumber()

        // Identity
        val identity = texts.firstOrNull { IDENTITY_REGEX.containsMatchIn(it) }

        // Rating
        val ratingLine = texts.firstOrNull { RATING_REGEX.containsMatchIn(it) }
        val rating = ratingLine?.let { RATING_REGEX.find(it)?.value }

        // Pickup: "A X min (X.X km)" - find closest to "Aceptar" if multiple
        val pickupLines = texts.filter { PICKUP_REGEX.containsMatchIn(it) }
        val pickupLine = if (aceptarIndex >= 0 && pickupLines.size > 1) {
            pickupLines.minByOrNull { kotlin.math.abs(texts.indexOf(it) - aceptarIndex) }
        } else {
            pickupLines.firstOrNull()
        }
        val pickupMatch = pickupLine?.let { PICKUP_REGEX.find(it) }
        val pickupMinutes = pickupMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val pickupKm = pickupMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0

        // Pickup address: after " - " in the same text, or next text
        val pickupAddress = if (pickupLine != null && pickupLine.contains(" - ")) {
            pickupLine.substringAfter(" - ").trim()
        } else {
            val pickupIndex = texts.indexOf(pickupLine)
            if (pickupIndex >= 0 && pickupIndex + 1 < texts.size) texts[pickupIndex + 1] else null
        }

        // Trip info: "Viaje: X min (X.X km)" - find closest to "Aceptar" if multiple
        val tripLines = texts.filter { TRIP_REGEX.containsMatchIn(it) }
        val tripLine = if (aceptarIndex >= 0 && tripLines.size > 1) {
            tripLines.minByOrNull { kotlin.math.abs(texts.indexOf(it) - aceptarIndex) }
        } else {
            tripLines.firstOrNull()
        }
        val tripMatch = tripLine?.let { TRIP_REGEX.find(it) }
        val tripMinutes = tripMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val tripKm = tripMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0

        // Destination: line after trip info
        val tripIndex = texts.indexOf(tripLine)
        val destination = if (tripIndex >= 0 && tripIndex + 1 < texts.size) {
            texts[tripIndex + 1]
        } else null

        // If we have no price and no trip/pickup info, this is not a valid trip
        if (price == null && !hasPickup && !hasTrip) return null

        return TripData(
            type = mainType,
            subtype = subtype,
            price = price ?: 0.0,
            bonus = bonus,
            pickupMinutes = pickupMinutes,
            pickupKm = pickupKm,
            pickupAddress = pickupAddress,
            tripMinutes = tripMinutes,
            tripKm = tripKm,
            destination = destination,
            passengerRating = rating,
            identity = identity
        )
    }

    private fun String.parseArgentineNumber(): Double? {
        return this.replace(".", "").replace(",", ".").toDoubleOrNull()
    }
}
