package com.carlos.uberanalyzer.fuel

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * Reverse-engineered from https://surtidores.com.ar/precios/
 *
 * Architecture findings:
 * - WordPress site (page ID 40152, template: page-precios.php)
 * - WP REST API exposed at /wp-json/wp/v2/ (posts, pages)
 * - Price data is statically embedded in HTML tables (NOT fetched via API)
 * - No dynamic API endpoint for prices — data is manually updated in page content
 * - Uses Infogram (e.infogram.com) for chart visualization
 * - Prices are YPF C.A.B.A only, 4 fuel types: Super, Premium, Gasoil, Euro
 * - Table structure: year sections → month columns (Enero-Diciembre) → 4 fuel rows
 * - Scraping strategy: fetch page HTML, regex-parse the table rows
 *
 * Fallback: hardcoded prices updated from last successful scrape (March 2026)
 */
object FuelPriceProvider {

    private const val PREFS_NAME = "fuel_prices"
    private const val KEY_SUPER = "price_super"
    private const val KEY_PREMIUM = "price_premium"
    private const val KEY_GASOIL = "price_gasoil"
    private const val KEY_EURO = "price_euro"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val KEY_CONSUMPTION = "consumption_km_per_liter"
    private const val KEY_FUEL_TYPE = "selected_fuel_type"

    private const val SCRAPE_URL = "https://surtidores.com.ar/precios/"

    // Hardcoded fallback prices (March 2026, YPF C.A.B.A)
    // Reverse-engineered from static HTML table at surtidores.com.ar/precios/
    private val FALLBACK_PRICES = mapOf(
        FuelType.SUPER to 1717.0,
        FuelType.PREMIUM to 1881.0,
        FuelType.GASOIL to 1768.0,
        FuelType.EURO to 1966.0
    )

    // Historical data extracted from the site (2025-2026)
    // Useful for cost trend analysis
    val HISTORICAL_PRICES_2025 = mapOf(
        "Enero" to mapOf(FuelType.SUPER to 1128.0, FuelType.PREMIUM to 1394.0, FuelType.GASOIL to 1143.0, FuelType.EURO to 1392.0),
        "Febrero" to mapOf(FuelType.SUPER to 1188.0, FuelType.PREMIUM to 1468.0, FuelType.GASOIL to 1203.0, FuelType.EURO to 1465.0),
        "Marzo" to mapOf(FuelType.SUPER to 1188.0, FuelType.PREMIUM to 1468.0, FuelType.GASOIL to 1203.0, FuelType.EURO to 1465.0),
        "Abril" to mapOf(FuelType.SUPER to 1188.0, FuelType.PREMIUM to 1468.0, FuelType.GASOIL to 1203.0, FuelType.EURO to 1465.0),
        "Mayo" to mapOf(FuelType.SUPER to 1188.0, FuelType.PREMIUM to 1468.0, FuelType.GASOIL to 1203.0, FuelType.EURO to 1465.0),
        "Junio" to mapOf(FuelType.SUPER to 1259.0, FuelType.PREMIUM to 1511.0, FuelType.GASOIL to 1276.0, FuelType.EURO to 1509.0),
        "Julio" to mapOf(FuelType.SUPER to 1259.0, FuelType.PREMIUM to 1511.0, FuelType.GASOIL to 1276.0, FuelType.EURO to 1509.0),
        "Agosto" to mapOf(FuelType.SUPER to 1331.0, FuelType.PREMIUM to 1551.0, FuelType.GASOIL to 1350.0, FuelType.EURO to 1554.0),
        "Septiembre" to mapOf(FuelType.SUPER to 1391.0, FuelType.PREMIUM to 1621.0, FuelType.GASOIL to 1410.0, FuelType.EURO to 1620.0),
        "Octubre" to mapOf(FuelType.SUPER to 1481.0, FuelType.PREMIUM to 1721.0, FuelType.GASOIL to 1501.0, FuelType.EURO to 1720.0),
        "Noviembre" to mapOf(FuelType.SUPER to 1541.0, FuelType.PREMIUM to 1781.0, FuelType.GASOIL to 1553.0, FuelType.EURO to 1768.0),
        "Diciembre" to mapOf(FuelType.SUPER to 1611.0, FuelType.PREMIUM to 1835.0, FuelType.GASOIL to 1603.0, FuelType.EURO to 1802.0)
    )

    val HISTORICAL_PRICES_2026 = mapOf(
        "Enero" to mapOf(FuelType.SUPER to 1566.0, FuelType.PREMIUM to 1780.0, FuelType.GASOIL to 1601.0, FuelType.EURO to 1809.0),
        "Febrero" to mapOf(FuelType.SUPER to 1609.0, FuelType.PREMIUM to 1845.0, FuelType.GASOIL to 1658.0, FuelType.EURO to 1861.0),
        "Marzo" to mapOf(FuelType.SUPER to 1717.0, FuelType.PREMIUM to 1881.0, FuelType.GASOIL to 1768.0, FuelType.EURO to 1966.0)
    )

    // Default consumption: 10 km/l for a typical Uber car in city
    const val DEFAULT_CONSUMPTION = 10.0f

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedFuelType(ctx: Context): FuelType {
        val name = prefs(ctx).getString(KEY_FUEL_TYPE, FuelType.SUPER.name) ?: FuelType.SUPER.name
        return try { FuelType.valueOf(name) } catch (_: Exception) { FuelType.SUPER }
    }

    fun setSelectedFuelType(ctx: Context, type: FuelType) {
        prefs(ctx).edit().putString(KEY_FUEL_TYPE, type.name).apply()
    }

    fun getConsumption(ctx: Context): Float {
        return prefs(ctx).getFloat(KEY_CONSUMPTION, DEFAULT_CONSUMPTION)
    }

    fun setConsumption(ctx: Context, kmPerLiter: Float) {
        prefs(ctx).edit().putFloat(KEY_CONSUMPTION, kmPerLiter).apply()
    }

    fun getCurrentPrice(ctx: Context, type: FuelType? = null): Double {
        val fuelType = type ?: getSelectedFuelType(ctx)
        val key = when (fuelType) {
            FuelType.SUPER -> KEY_SUPER
            FuelType.PREMIUM -> KEY_PREMIUM
            FuelType.GASOIL -> KEY_GASOIL
            FuelType.EURO -> KEY_EURO
        }
        val saved = prefs(ctx).getFloat(key, 0f)
        return if (saved > 0) saved.toDouble() else FALLBACK_PRICES[fuelType] ?: 1717.0
    }

    fun getLastUpdateTimestamp(ctx: Context): Long {
        return prefs(ctx).getLong(KEY_LAST_UPDATE, 0)
    }

    /**
     * Calculate fuel cost for a trip distance.
     * Returns cost in ARS.
     */
    fun calculateFuelCost(ctx: Context, totalKm: Double): Double {
        val pricePerLiter = getCurrentPrice(ctx)
        val kmPerLiter = getConsumption(ctx).toDouble()
        return if (kmPerLiter > 0) (totalKm / kmPerLiter) * pricePerLiter else 0.0
    }

    /**
     * Calculate net earnings (trip price minus fuel cost).
     */
    fun calculateNetEarnings(ctx: Context, tripPrice: Double, totalKm: Double): Double {
        return tripPrice - calculateFuelCost(ctx, totalKm)
    }

    /**
     * Scrape latest prices from surtidores.com.ar/precios/
     *
     * The site uses static HTML tables with this structure:
     * - Year header rows
     * - Month columns (Enero through Diciembre)
     * - 4 fuel type rows per year: Super, Premium, Gasoil, Euro
     *
     * We extract the most recent month's prices.
     */
    suspend fun refreshPrices(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = URL(SCRAPE_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "UberDriverCompanion/1.0")

            val html = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val prices = parsePricesFromHtml(html)
            if (prices != null) {
                prefs(ctx).edit()
                    .putFloat(KEY_SUPER, prices[FuelType.SUPER]?.toFloat() ?: 0f)
                    .putFloat(KEY_PREMIUM, prices[FuelType.PREMIUM]?.toFloat() ?: 0f)
                    .putFloat(KEY_GASOIL, prices[FuelType.GASOIL]?.toFloat() ?: 0f)
                    .putFloat(KEY_EURO, prices[FuelType.EURO]?.toFloat() ?: 0f)
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .apply()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Parse fuel prices from the HTML of surtidores.com.ar/precios/
     *
     * The page has a table where each year has rows like:
     *   Super | 1566 | 1609 | 1717 | ...
     *   Premium | 1780 | 1845 | 1881 | ...
     *   Gasoil | 1601 | 1658 | 1768 | ...
     *   Euro | 1809 | 1861 | 1966 | ...
     *
     * We find the current year section and extract the latest non-empty month.
     */
    internal fun parsePricesFromHtml(html: String): Map<FuelType, Double>? {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()

        // Find the section for the current year
        val yearIndex = html.indexOf(currentYear)
        if (yearIndex < 0) return null

        // Extract a chunk after the year marker that contains the 4 fuel rows
        val chunk = html.substring(yearIndex, minOf(yearIndex + 5000, html.length))

        val result = mutableMapOf<FuelType, Double>()

        for (fuelType in FuelType.values()) {
            val label = fuelType.displayName
            val labelIndex = chunk.indexOf(label, ignoreCase = true)
            if (labelIndex < 0) continue

            // Get the row content after the label
            val rowChunk = chunk.substring(labelIndex, minOf(labelIndex + 1000, chunk.length))

            // Extract all numbers from td elements in this row
            val numberPattern = Regex("""<td[^>]*>\s*(\d{3,5})\s*</td>""")
            val numbers = numberPattern.findAll(rowChunk).map { it.groupValues[1].toDouble() }.toList()

            // Take the last non-zero number (most recent month with data)
            val latestPrice = numbers.lastOrNull { it > 0 }
            if (latestPrice != null) {
                result[fuelType] = latestPrice
            }
        }

        return if (result.size == 4) result else null
    }
}

enum class FuelType(val displayName: String) {
    SUPER("Super"),
    PREMIUM("Premium"),
    GASOIL("Gasoil"),
    EURO("Euro");
}
