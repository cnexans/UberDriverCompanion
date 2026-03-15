package com.carlos.uberanalyzer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.carlos.uberanalyzer.parser.DidiTripParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented test for Didi screenshots.
 * Loads screenshots from androidTest/assets/didi_screenshots/,
 * runs OCR, and validates parsing with DidiTripParser.
 */
@RunWith(AndroidJUnit4::class)
class DidiScreenshotOcrTest {

    companion object {
        private const val TAG = "DidiScreenshotOcrTest"
        private const val SCREENSHOTS_DIR = "didi_screenshots"
        private const val ROI_TOP_RATIO = 0.45f
        private const val ROI_SCALE_FACTOR = 0.6f
    }

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    @Test
    fun processAllDidiScreenshots() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val assets = context.assets

        val files = try {
            assets.list(SCREENSHOTS_DIR)?.filter {
                it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg")
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "No didi_screenshots directory found. Skipping.")
            return
        }

        if (files.isEmpty()) {
            Log.w(TAG, "No screenshots in $SCREENSHOTS_DIR/. Add images and re-run.")
            return
        }

        val expected = loadExpected(assets)

        Log.d(TAG, "=== Processing ${files.size} Didi screenshots ===")

        val results = mutableListOf<TestResult>()

        for (filename in files.sorted()) {
            Log.d(TAG, "--- $filename ---")
            val bitmap = assets.open("$SCREENSHOTS_DIR/$filename").use {
                BitmapFactory.decodeStream(it)
            }
            assertNotNull("Failed to decode $filename", bitmap)

            val ocrTexts = runOcr(bitmap!!)
            val filteredTexts = filterOverlayTexts(ocrTexts)
            val trip = DidiTripParser.parse(filteredTexts)

            Log.d(TAG, "OCR raw [${ocrTexts.size}]: $ocrTexts")
            Log.d(TAG, "OCR filtered [${filteredTexts.size}]: $filteredTexts")

            if (trip != null) {
                Log.d(TAG, "PARSED: type=${trip.type} price=${trip.price} " +
                    "pickup=${trip.pickupMinutes}m/${trip.pickupKm}km " +
                    "trip=${trip.tripMinutes}m/${trip.tripKm}km " +
                    "$/km=${String.format("%.1f", trip.pesosPorKm)} " +
                    "bonus=${trip.bonus} rating=${trip.passengerRating}")
            } else {
                Log.d(TAG, "PARSED: null (no trip detected)")
            }

            val exp = expected?.get(filename)
            results.add(TestResult(filename, ocrTexts, filteredTexts, trip, exp))

            bitmap.recycle()
        }

        // Summary
        Log.d(TAG, "")
        Log.d(TAG, "=== SUMMARY ===")
        var passed = 0
        var failed = 0
        var noExpected = 0

        for (r in results) {
            if (r.expected == null) {
                noExpected++
                val status = if (r.trip != null) "DETECTED" else "NO_TRIP"
                Log.d(TAG, "  [?] ${r.filename}: $status (no expected values)")
            } else {
                val errors = r.validate()
                if (errors.isEmpty()) {
                    passed++
                    Log.d(TAG, "  [OK] ${r.filename}")
                } else {
                    failed++
                    Log.e(TAG, "  [FAIL] ${r.filename}:")
                    errors.forEach { Log.e(TAG, "    - $it") }
                }
            }
        }

        Log.d(TAG, "")
        Log.d(TAG, "Results: $passed passed, $failed failed, $noExpected without expected values")
        Log.d(TAG, "Total: ${results.size} screenshots processed")

        if (failed > 0) {
            fail("$failed screenshot(s) failed validation. Check logcat for details.")
        }
    }

    private fun runOcr(bitmap: Bitmap): List<String> {
        val roiBitmap = cropROI(bitmap)
        val optimized = optimizeBitmapForOCR(roiBitmap)
        roiBitmap.recycle()

        val latch = CountDownLatch(1)
        val ocrTexts = mutableListOf<String>()
        var ocrError: Exception? = null

        val image = InputImage.fromBitmap(optimized, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        ocrTexts.add(line.text)
                    }
                }
                latch.countDown()
            }
            .addOnFailureListener { e ->
                ocrError = e
                latch.countDown()
            }

        assertTrue("OCR timed out", latch.await(30, TimeUnit.SECONDS))
        optimized.recycle()

        if (ocrError != null) {
            fail("OCR failed: ${ocrError!!.message}")
        }
        return ocrTexts
    }

    private fun filterOverlayTexts(texts: List<String>): List<String> {
        val overlayPatterns = listOf(
            "UBER ANALYZER", "$/km:", "$/min:", "$/h:", "Dist cobrada:", "Ratio:",
            "Recogida:", "Esperando viaje", "— UBER —", "— DIDI —"
        )
        return texts.filter { text ->
            overlayPatterns.none { text.contains(it) } && text != "UBER" && text != "DIDI"
        }
    }

    private fun cropROI(bitmap: Bitmap): Bitmap {
        val startY = (bitmap.height * ROI_TOP_RATIO).toInt()
        val height = bitmap.height - startY
        return Bitmap.createBitmap(bitmap, 0, startY, bitmap.width, height)
    }

    private fun optimizeBitmapForOCR(bitmap: Bitmap): Bitmap {
        val scaledWidth = (bitmap.width * ROI_SCALE_FACTOR).toInt()
        val scaledHeight = (bitmap.height * ROI_SCALE_FACTOR).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        val grayscaleBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()

        val contrast = 1.5f
        val offset = (-(128f * contrast) + 128f)
        val matrix = ColorMatrix(floatArrayOf(
            0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, offset,
            0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, offset,
            0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)
        scaledBitmap.recycle()

        return grayscaleBitmap
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadExpected(assets: android.content.res.AssetManager): Map<String, Map<String, Any>>? {
        return try {
            val json = assets.open("$SCREENSHOTS_DIR/expected.json").bufferedReader().readText()
            val map = org.json.JSONObject(json)
            val result = mutableMapOf<String, Map<String, Any>>()
            for (key in map.keys()) {
                val obj = map.getJSONObject(key)
                val values = mutableMapOf<String, Any>()
                for (k in obj.keys()) {
                    values[k] = obj.get(k)
                }
                result[key] = values
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    data class TestResult(
        val filename: String,
        val ocrTexts: List<String>,
        val filteredTexts: List<String>,
        val trip: com.carlos.uberanalyzer.model.TripData?,
        val expected: Map<String, Any>?
    ) {
        fun validate(): List<String> {
            if (expected == null) return emptyList()
            val errors = mutableListOf<String>()

            if (trip == null) {
                errors.add("Expected trip but got null")
                return errors
            }

            expected["type"]?.let {
                if (trip.type != it) errors.add("type: expected=$it got=${trip.type}")
            }
            expected["price"]?.let {
                val exp = (it as Number).toDouble()
                if (kotlin.math.abs(trip.price - exp) > 1.0) {
                    errors.add("price: expected=$exp got=${trip.price}")
                }
            }
            expected["pickupMinutes"]?.let {
                val exp = (it as Number).toInt()
                if (trip.pickupMinutes != exp) errors.add("pickupMinutes: expected=$exp got=${trip.pickupMinutes}")
            }
            expected["pickupKm"]?.let {
                val exp = (it as Number).toDouble()
                if (kotlin.math.abs(trip.pickupKm - exp) > 0.2) {
                    errors.add("pickupKm: expected=$exp got=${trip.pickupKm}")
                }
            }
            expected["tripMinutes"]?.let {
                val exp = (it as Number).toInt()
                if (trip.tripMinutes != exp) errors.add("tripMinutes: expected=$exp got=${trip.tripMinutes}")
            }
            expected["tripKm"]?.let {
                val exp = (it as Number).toDouble()
                if (kotlin.math.abs(trip.tripKm - exp) > 0.2) {
                    errors.add("tripKm: expected=$exp got=${trip.tripKm}")
                }
            }
            expected["bonus"]?.let {
                val exp = (it as Number).toDouble()
                val got = trip.bonus ?: 0.0
                if (kotlin.math.abs(got - exp) > 1.0) {
                    errors.add("bonus: expected=$exp got=$got")
                }
            }

            return errors
        }
    }
}
