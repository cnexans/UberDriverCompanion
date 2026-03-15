package com.carlos.uberanalyzer.parser

import com.carlos.uberanalyzer.model.TripData

/**
 * Parser for Didi Driver app (Colombian market).
 *
 * Didi trip request format:
 *   Efectivo
 *   $12.774
 *   $1.263 incluidos  😀 3
 *   4,96 · 296 arrendamientos
 *   Tarjeta bancaria verificada
 *   5min (1,2km)                    ← pickup
 *   Conjunto Residencial ..., Comuna 17
 *   20min (10,1km)                  ← trip
 *   Calle 54 # 26H-19, Comuna 12
 *
 * Key differences from Uber:
 *   - Colombian number format: dots for thousands, comma for decimal in km
 *   - Bonus: "$X incluidos" (not "+$ X incluido")
 *   - No "A" prefix on pickup, no "Viaje:" prefix on trip
 *   - Rating: "4,96 · 296 arrendamientos"
 *   - Identity: "Tarjeta bancaria verificada"
 *   - Stops: "X parada(s)"
 */
object DidiTripParser {

    // Price: "$12.774" or "$ 12.774"
    private val PRICE_REGEX = Regex("""^\$\s?([0-9.]+)$""")

    // Bonus: "$1.263 incluidos" or "$1.263 incluidos"
    private val BONUS_REGEX = Regex("""\$\s?([0-9.]+)\s?incluidos?""")

    // Time+distance in km: "5min (1,2km)" or "20min (10,1km)" or "5 min (1,2 km)"
    private val TIME_DIST_KM_REGEX = Regex("""(\d+)\s?min\s?\(([0-9]+[,.]?[0-9]*)\s?km\)""")

    // Time+distance in meters: "3min (760m)" or "2min (199m)"
    private val TIME_DIST_M_REGEX = Regex("""(\d+)\s?min\s?\((\d+)\s?m\)""")

    // Rating: "4,96 · 296 arrendamientos" or "4.96 · 296 arrendamientos"
    private val RATING_REGEX = Regex("""([0-9][.,][0-9]{2})\s?[·.]\s?(\d+)\s?arrendamientos?""")

    // Identity / verification
    private val IDENTITY_REGEX = Regex("""(?i)(Tarjeta bancaria verificada|verificad)""")

    // Stops
    private val STOPS_REGEX = Regex("""(\d+)\s?parada""")

    /**
     * Represents a parsed time+distance entry.
     */
    private data class TimeDist(val index: Int, val minutes: Int, val km: Double)

    fun parse(texts: List<String>): TripData? {
        val normalized = texts.map { normalizeOcrText(it) }

        // Find all time+distance matches (pickup is first, trip is second)
        // Didi shows short distances in meters (e.g., "760m") and longer in km (e.g., "10,1km")
        val timeDistLines = mutableListOf<TimeDist>()
        for ((index, text) in normalized.withIndex()) {
            val kmMatch = TIME_DIST_KM_REGEX.find(text)
            if (kmMatch != null) {
                val minutes = kmMatch.groupValues[1].toIntOrNull() ?: 0
                val km = kmMatch.groupValues[2].parseColombianKm() ?: 0.0
                timeDistLines.add(TimeDist(index, minutes, km))
                continue
            }
            val mMatch = TIME_DIST_M_REGEX.find(text)
            if (mMatch != null) {
                val minutes = mMatch.groupValues[1].toIntOrNull() ?: 0
                val meters = mMatch.groupValues[2].toDoubleOrNull() ?: 0.0
                timeDistLines.add(TimeDist(index, minutes, meters / 1000.0))
                continue
            }
        }

        if (timeDistLines.isEmpty()) return null

        // Price
        val priceLines = normalized.filter {
            PRICE_REGEX.matches(it) && !BONUS_REGEX.containsMatchIn(it)
        }
        // Pick the largest price (the trip price is typically the biggest number)
        val price = priceLines.mapNotNull { extractPrice(it) }.maxOrNull()

        if (price == null || price <= 0.0) return null

        // Bonus
        val bonusLine = normalized.firstOrNull { BONUS_REGEX.containsMatchIn(it) }
        val bonusMatch = bonusLine?.let { BONUS_REGEX.find(it) }
        val bonus = bonusMatch?.groupValues?.get(1)?.parseColombianNumber()

        // Rating
        val ratingLine = normalized.firstOrNull { RATING_REGEX.containsMatchIn(it) }
        val rating = ratingLine?.let { RATING_REGEX.find(it)?.value }

        // Identity
        val identity = normalized.firstOrNull { IDENTITY_REGEX.containsMatchIn(it) }

        // Stops
        val stopsLine = normalized.firstOrNull { STOPS_REGEX.containsMatchIn(it) }
        val stops = stopsLine?.let { STOPS_REGEX.find(it)?.groupValues?.get(1)?.toIntOrNull() }

        // Pickup (first time+distance line)
        val pickup = timeDistLines.getOrNull(0)
        val pickupMinutes = pickup?.minutes ?: 0
        val pickupKm = pickup?.km ?: 0.0
        val pickupIndex = pickup?.index ?: -1

        // Pickup address (line after pickup)
        val pickupAddress = if (pickupIndex >= 0 && pickupIndex + 1 < normalized.size) {
            val next = normalized[pickupIndex + 1]
            if (!hasTimeDist(next) && !STOPS_REGEX.containsMatchIn(next)) next else null
        } else null

        // Trip (second time+distance line)
        val trip = timeDistLines.getOrNull(1)
        val tripMinutes = trip?.minutes ?: 0
        val tripKm = trip?.km ?: 0.0
        val tripIndex = trip?.index ?: -1

        // Destination (line after trip)
        val destination = if (tripIndex >= 0 && tripIndex + 1 < normalized.size) {
            val next = normalized[tripIndex + 1]
            if (!hasTimeDist(next)) next else null
        } else null

        return TripData(
            type = "Didi",
            subtype = if (stops != null && stops > 0) "$stops parada(s)" else null,
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
     * Detect if OCR texts look like a Didi screen.
     */
    fun isDidi(texts: List<String>): Boolean {
        return texts.any { text ->
            text.contains("arrendamiento", ignoreCase = true) ||
            text.contains("incluidos", ignoreCase = true) ||
            text.contains("parada", ignoreCase = true) ||
            text.contains("Tarjeta bancaria", ignoreCase = true)
        }
    }

    private fun hasTimeDist(text: String): Boolean {
        return TIME_DIST_KM_REGEX.containsMatchIn(text) || TIME_DIST_M_REGEX.containsMatchIn(text)
    }

    fun normalizeOcrText(text: String): String {
        var s = text

        // Fix "$X.XXX" without space: "$12.774" → "$ 12.774" (for price regex)
        s = s.replace(Regex("""^\$(\d)""")) { match ->
            "$ ${match.groupValues[1]}"
        }

        // Fix OCR reading S as 5 in "min" context: "Smin" → "5min"
        s = s.replace(Regex("""(?<![a-zA-Z])S(\s?min)""")) { match ->
            "5${match.groupValues[1]}"
        }

        // Fix OCR reading l as 1 in "min" context
        s = s.replace(Regex("""([l1I]+)\s?min""")) { match ->
            val digits = match.groupValues[1].replace('l', '1').replace('I', '1')
            "${digits}min"
        }

        // Fix OCR prefix artifacts: "O " at the beginning (common OCR noise from icons)
        s = s.replace(Regex("""^O\s+"""), "")

        return s
    }

    private fun extractPrice(text: String): Double? {
        PRICE_REGEX.find(text)?.let {
            return it.groupValues[1].parseColombianNumber()
        }
        return null
    }

    /**
     * Colombian number format: dots as thousands separator.
     * "12.774" → 12774.0
     * "1.263" → 1263.0
     */
    private fun String.parseColombianNumber(): Double? {
        return this.replace(".", "").toDoubleOrNull()
    }

    /**
     * Colombian km format: comma as decimal separator.
     * "1,2" → 1.2
     * "10,1" → 10.1
     * "5" → 5.0
     */
    private fun String.parseColombianKm(): Double? {
        return this.replace(",", ".").toDoubleOrNull()
    }
}
