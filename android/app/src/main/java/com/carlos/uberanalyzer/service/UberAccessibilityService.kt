package com.carlos.uberanalyzer.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.carlos.uberanalyzer.db.TripDatabase
import com.carlos.uberanalyzer.db.TripEntity
import com.carlos.uberanalyzer.model.TripData
import com.carlos.uberanalyzer.parser.TripParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UberAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastTripKey: String? = null
    private val db by lazy { TripDatabase.getInstance(this) }
    private val handler = Handler(Looper.getMainLooper())
    private var lastTripSaveTime = 0L
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    private var isTripActive = false
    private var tripGoneTime = 0L
    private var isProcessing = false

    companion object {
        private const val TAG = "UberAnalyzer"
        private const val POLL_INTERVAL_MS = 100L
        private const val TRIP_GONE_TIMEOUT_MS = 3000L
        private const val ROI_TOP_RATIO = 0.45f
        private const val ROI_SCALE_FACTOR = 0.6f
        var isServiceRunning = false
            private set
    }

    private var pollingRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        Log.d(TAG, "AccessibilityService connected — continuous screenshot mode")

        OverlayService.onSyncRequested = {
            Log.d(TAG, "Manual sync requested")
            takeScreenshotAndProcess()
        }

        startPolling()
    }

    private fun startPolling() {
        pollingRunnable = object : Runnable {
            override fun run() {
                if (!isProcessing && OverlayService.isRunning()) {
                    takeScreenshotAndProcess()
                }
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        handler.postDelayed(pollingRunnable!!, POLL_INTERVAL_MS)
    }

    private fun takeScreenshotAndProcess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        isProcessing = true

        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        try {
                            val bitmap = Bitmap.wrapHardwareBuffer(
                                result.hardwareBuffer, result.colorSpace
                            )
                            if (bitmap != null) {
                                val swBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                bitmap.recycle()
                                result.hardwareBuffer.close()

                                if (swBitmap != null) {
                                    val roiBitmap = cropROI(swBitmap)
                                    swBitmap.recycle()

                                    val optimizedBitmap = optimizeBitmapForOCR(roiBitmap)
                                    roiBitmap.recycle()

                                    val image = InputImage.fromBitmap(optimizedBitmap, 0)
                                    textRecognizer.process(image)
                                        .addOnSuccessListener { visionText ->
                                            optimizedBitmap.recycle()
                                            isProcessing = false

                                            val ocrTexts = mutableListOf<String>()
                                            for (block in visionText.textBlocks) {
                                                for (line in block.lines) {
                                                    ocrTexts.add(line.text)
                                                }
                                            }

                                            processOcrResults(ocrTexts)
                                        }
                                        .addOnFailureListener { e ->
                                            optimizedBitmap.recycle()
                                            isProcessing = false
                                            Log.e(TAG, "OCR failed: ${e.message}")
                                        }
                                } else {
                                    isProcessing = false
                                }
                            } else {
                                result.hardwareBuffer.close()
                                isProcessing = false
                            }
                        } catch (e: Exception) {
                            isProcessing = false
                            Log.e(TAG, "Screenshot processing error: ${e.message}")
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        isProcessing = false
                    }
                }
            )
        } catch (e: Exception) {
            isProcessing = false
            Log.e(TAG, "takeScreenshot error: ${e.message}")
        }
    }

    private fun processOcrResults(ocrTexts: List<String>) {
        val overlayPatterns = listOf(
            "UBER ANALYZER", "$/km:", "$/min:", "$/h:", "Dist cobrada:", "Ratio:",
            "Recogida:", "Esperando viaje", "— UBER —"
        )
        val filteredTexts = ocrTexts.filter { text ->
            overlayPatterns.none { text.contains(it) } && text != "UBER"
        }

        if (filteredTexts.isEmpty()) {
            handleNoTrip()
            return
        }

        val trip = TripParser.parse(filteredTexts)
        if (trip == null) {
            handleNoTrip()
            return
        }

        Log.d(TAG, "TRIP: ${trip.type} $${trip.price} $/km=${trip.pesosPorKm}")

        // Reset gone timer — trip is visible
        isTripActive = true
        tripGoneTime = 0L

        if (trip.tripKey == lastTripKey) return
        lastTripKey = trip.tripKey

        showTripOnOverlay(trip)
        saveTrip(trip)
    }

    private fun handleNoTrip() {
        if (!isTripActive) return
        val now = System.currentTimeMillis()
        if (tripGoneTime == 0L) {
            tripGoneTime = now
        } else if (now - tripGoneTime > TRIP_GONE_TIMEOUT_MS) {
            isTripActive = false
            lastTripKey = null
            OverlayService.showWaiting()
        }
    }

    private fun showTripOnOverlay(trip: TripData) {
        if (!OverlayService.isRunning() && android.provider.Settings.canDrawOverlays(this)) {
            startService(Intent(this, OverlayService::class.java))
            handler.postDelayed({ OverlayService.updateTrip(trip) }, 300)
        } else {
            OverlayService.updateTrip(trip)
        }
    }

    private fun saveTrip(trip: TripData) {
        val now = System.currentTimeMillis()
        if (now - lastTripSaveTime < 30_000) return
        lastTripSaveTime = now

        scope.launch {
            db.tripDao().insert(
                TripEntity(
                    type = trip.type,
                    subtype = trip.subtype,
                    price = trip.price,
                    bonus = trip.bonus,
                    pickupMinutes = trip.pickupMinutes,
                    pickupKm = trip.pickupKm,
                    pickupAddress = trip.pickupAddress,
                    tripMinutes = trip.tripMinutes,
                    tripKm = trip.tripKm,
                    destination = trip.destination,
                    passengerRating = trip.passengerRating,
                    identity = trip.identity,
                    pesosPorKm = trip.pesosPorKm,
                    pesosPorMin = trip.pesosPorMin,
                    pctDistancia = trip.pctDistancia,
                    timestamp = trip.timestamp
                )
            )
            Log.d(TAG, "Trip saved to DB")
        }

        sendBroadcast(Intent("com.carlos.uberanalyzer.TRIP_UPDATE"))
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used — all processing is screenshot-based polling
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        isServiceRunning = false
        pollingRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}
