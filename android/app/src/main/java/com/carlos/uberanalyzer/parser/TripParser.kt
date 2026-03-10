package com.carlos.uberanalyzer.parser

import com.carlos.uberanalyzer.model.TripData

object TripParser {

    private val PRICE_PESO_REGEX = Regex("""^\$\s?([0-9.,]+)$""")
    private val PRICE_ARS_REGEX = Regex("""^ARS\s?([0-9.,]+)""")
    private val BONUS_REGEX = Regex("""\+\$\s?([0-9.,]+)\s?incluido""")
    private val PICKUP_REGEX = Regex("""A\s?(\d+)\s?min\s?\(([0-9.]+)\s?km\)""")
    private val TRIP_REGEX = Regex("""(?:Via)?je:\s?(\d+)\s?min\s?\(([0-9.]+)\s?km\)""")
    private val RATING_REGEX = Regex("""([0-9][.,][0-9]{2})\s?\((\d+)\)""")
    private val IDENTITY_REGEX = Regex("""(?i)(DNI|Identidad|verificad|Pasaporte|Licencia)""")

    fun parse(texts: List<String>): TripData? {
        val normalized = texts.map { normalizeOcrText(it) }

        val hasPickup = normalized.any { PICKUP_REGEX.containsMatchIn(it) }
        val hasTrip = normalized.any { TRIP_REGEX.containsMatchIn(it) }
        if (!hasPickup && !hasTrip) return null

        val aceptarIndex = normalized.indexOfLast {
            it.contains("Aceptar") || it.contains("Viaje disponible")
        }

        // Price: try $ format first, then ARS format
        val priceLines = normalized.filter {
            (PRICE_PESO_REGEX.matches(it) || PRICE_ARS_REGEX.containsMatchIn(it)) &&
            it != "$ 0,00" && it != "$ 0" && it != "\$0" &&
            !it.contains("adicionales") && !it.contains("Incentivo")
        }
        val priceLine = if (aceptarIndex >= 0 && priceLines.size > 1) {
            priceLines.minByOrNull { kotlin.math.abs(normalized.indexOf(it) - aceptarIndex) }
        } else {
            priceLines.firstOrNull()
        }
        val price = priceLine?.let { extractPrice(it) }

        // Type
        val knownTypes = listOf("Moto", "Auto", "Artículo", "UberX", "Encargo", "Flash", "Eco", "Exclusivo")
        val typeFromTexts = normalized.firstOrNull { text -> knownTypes.any { text == it } }
            ?: normalized.firstOrNull { text -> knownTypes.any { text.contains(it) } }?.let { text ->
                knownTypes.firstOrNull { text.contains(it) }
            }
        val mainType = typeFromTexts ?: "Viaje"

        // Subtype: find a different known type in any text
        var subtype: String? = null
        for (text in normalized) {
            for (kt in knownTypes) {
                if (kt != mainType && (text == kt || text.contains(kt))) {
                    subtype = kt
                    break
                }
            }
            if (subtype != null) break
        }

        // Bonus
        val bonusLine = normalized.firstOrNull { BONUS_REGEX.containsMatchIn(it) }
        val bonusMatch = bonusLine?.let { BONUS_REGEX.find(it) }
        val bonus = bonusMatch?.groupValues?.get(1)?.parseArgentineNumber()

        // Identity
        val identity = normalized.firstOrNull { IDENTITY_REGEX.containsMatchIn(it) }

        // Rating (supports both 4,95 and 4.95)
        val ratingLine = normalized.firstOrNull { RATING_REGEX.containsMatchIn(it) }
        val rating = ratingLine?.let { RATING_REGEX.find(it)?.value }

        // Pickup
        val pickupLines = normalized.filter { PICKUP_REGEX.containsMatchIn(it) }
        val pickupLine = if (aceptarIndex >= 0 && pickupLines.size > 1) {
            pickupLines.minByOrNull { kotlin.math.abs(normalized.indexOf(it) - aceptarIndex) }
        } else {
            pickupLines.firstOrNull()
        }
        val pickupMatch = pickupLine?.let { PICKUP_REGEX.find(it) }
        var pickupMinutes = pickupMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val pickupKm = pickupMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0

        // Speed heuristic: if 1 min + km implies > 60 km/h, OCR likely read "11" as "l" → "1"
        if (pickupMinutes == 1 && pickupKm > 1.0) {
            val speedKmH = pickupKm / (1.0 / 60.0) // km/h if 1 min
            if (speedKmH > 60.0) {
                pickupMinutes = 11
            }
        }

        // Pickup address
        val pickupAddress = if (pickupLine != null && pickupLine.contains(" - ")) {
            pickupLine.substringAfter(" - ").trim()
        } else {
            val pickupIndex = normalized.indexOf(pickupLine)
            if (pickupIndex >= 0 && pickupIndex + 1 < normalized.size) normalized[pickupIndex + 1] else null
        }

        // Trip info
        val tripLines = normalized.filter { TRIP_REGEX.containsMatchIn(it) }
        val tripLine = if (aceptarIndex >= 0 && tripLines.size > 1) {
            tripLines.minByOrNull { kotlin.math.abs(normalized.indexOf(it) - aceptarIndex) }
        } else {
            tripLines.firstOrNull()
        }
        val tripMatch = tripLine?.let { TRIP_REGEX.find(it) }
        val tripMinutes = tripMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val tripKm = tripMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0

        // Destination
        val tripIndex = normalized.indexOf(tripLine)
        val destination = if (tripIndex >= 0 && tripIndex + 1 < normalized.size) {
            normalized[tripIndex + 1]
        } else null

        if (price == null || price <= 0.0) return null

        return TripData(
            type = mainType,
            subtype = subtype,
            price = price,
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

    /**
     * Normalize common OCR misreads before parsing.
     * - Fix l/I confused with 1 in numeric contexts
     * - Fix missing decimal points (e.g., "13 km" should be "1.3 km")
     * - Normalize spacing
     */
    fun normalizeOcrText(text: String): String {
        var s = text

        // Fix "A l min" → "A 1 min", "A ll min" → "A 11 min" (OCR reads 1 as l)
        s = s.replace(Regex("""A\s?([l1I]+)\s?min""")) { match ->
            val digits = match.groupValues[1].replace('l', '1').replace('I', '1')
            "A $digits min"
        }

        // Fix "Viaje:" prefix with l/I confusion
        s = s.replace(Regex("""Viaje:\s?([l1I]+)\s?min""")) { match ->
            val digits = match.groupValues[1].replace('l', '1').replace('I', '1')
            "Viaje: $digits min"
        }

        // Fix l→1 inside parentheses for km values: "(1.l km)" → "(1.1 km)", "(3.l km)" → "(3.1 km)"
        s = s.replace(Regex("""\(([0-9.]+[l])(\s?km\))""")) { match ->
            val fixed = match.groupValues[1].replace('l', '1')
            "(${fixed}${match.groupValues[2]}"
        }

        // Fix missing decimal in km values for small numbers read without dot
        // e.g., "A5 min (13 km)" where 13 should be 1.3
        // Only apply when the number is 2 digits and context suggests it should have a decimal
        s = s.replace(Regex("""\((\d{2})\s?km\)""")) { match ->
            val num = match.groupValues[1]
            // Insert decimal after first digit: "13" → "1.3", "45" → "4.5"
            "(${num[0]}.${num[1]} km)"
        }

        // Fix "$X.XXX" without space: "$1.920" → "$ 1.920"
        s = s.replace(Regex("""^\$(\d)""")) { match ->
            "$ ${match.groupValues[1]}"
        }

        // Fix "A4 min" → "A 4 min" (missing space after A)
        s = s.replace(Regex("""^A(\d+)\s?min""")) { match ->
            "A ${match.groupValues[1]} min"
        }

        return s
    }

    private fun extractPrice(text: String): Double? {
        // Try $ format
        PRICE_PESO_REGEX.find(text)?.let {
            return it.groupValues[1].parseArgentineNumber()
        }
        // Try ARS format: "ARS1,476" → 1476.0 (uses comma as thousands separator)
        PRICE_ARS_REGEX.find(text)?.let {
            val raw = it.groupValues[1]
            // ARS format uses comma as thousands separator (not decimal)
            return raw.replace(",", "").toDoubleOrNull()
        }
        return null
    }

    private fun String.parseArgentineNumber(): Double? {
        return this.replace(".", "").replace(",", ".").toDoubleOrNull()
    }
}
