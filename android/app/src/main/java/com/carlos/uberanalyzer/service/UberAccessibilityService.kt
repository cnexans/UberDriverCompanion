package com.carlos.uberanalyzer.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
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
    private var pendingRetries = 0
    private var lastScreenshotTime = 0L
    private var lastNormalTextCount = 15 // Assume normal until proven otherwise
    private var lastTripSaveTime = 0L
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    companion object {
        private const val TAG = "UberAnalyzer"
        private const val DEBOUNCE_MS = 250L
        private const val RETRY_DELAY_MS = 150L
        private const val MAX_RETRIES = 3
        private const val SCREENSHOT_COOLDOWN_MS = 100L
        var isServiceRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        Log.d(TAG, "AccessibilityService connected")
    }

    private var lastScanTime = 0L
    private val eventTexts = mutableListOf<String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: ""
        if (pkg == "com.ubercab.driver" || pkg == "com.ubercab") {
            // Get text from event
            event.text?.forEach { cs ->
                cs?.toString()?.takeIf { it.isNotBlank() }?.let { eventTexts.add(it) }
            }
            event.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { eventTexts.add(it) }

            // Traverse the source node subtree
            val source = event.source
            if (source != null) {
                source.refresh()
                val sourceTexts = mutableListOf<String>()
                extractTexts(source, sourceTexts)
                eventTexts.addAll(sourceTexts)
                source.recycle()
            }
        }

        val now = System.currentTimeMillis()
        if (now - lastScanTime < DEBOUNCE_MS) return
        lastScanTime = now
        pendingRetries = 0

        scanAndProcess("E")
    }

    private fun scanAndProcess(source: String) {
        val texts = mutableListOf<String>()
        val debugInfo = StringBuilder()
        var isUberActive = false
        var isUberForeground = false
        var rootTextCount = 0

        // Try rootInActiveWindow first
        try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val pkg = rootNode.packageName?.toString() ?: ""
                if (pkg == "com.ubercab.driver" || pkg == "com.ubercab") {
                    isUberActive = true
                    isUberForeground = true
                    rootNode.refresh()
                    extractTexts(rootNode, texts)
                    rootTextCount = texts.size
                    debugInfo.append("ROOT[$pkg,${texts.size}] ")
                }
                rootNode.recycle()
            }
        } catch (e: Exception) {
            debugInfo.append("ROOT_ERR ")
        }

        // Scan all windows for additional/better content
        try {
            for (window in windows) {
                val root = window.root ?: continue
                val pkg = root.packageName?.toString() ?: "null"
                val windowTexts = mutableListOf<String>()
                root.refresh()
                extractTexts(root, windowTexts)
                root.recycle()
                debugInfo.append("W[$pkg,${window.type},${windowTexts.size}] ")

                if (pkg == "com.ubercab.driver" || pkg == "com.ubercab") {
                    isUberActive = true
                    if (windowTexts.size > texts.size) {
                        texts.clear()
                        texts.addAll(windowTexts)
                        debugInfo.append("USED ")
                    }
                } else if (windowTexts.any { it.contains("min (") && it.contains("km)") }) {
                    texts.addAll(windowTexts)
                    debugInfo.append("TRIP! ")
                }
            }
        } catch (e: Exception) {
            debugInfo.append("WIN_ERR ")
        }

        // Merge event-collected texts (may have trip popup content)
        if (eventTexts.isNotEmpty()) {
            val newFromEvents = eventTexts.filter { et -> texts.none { it == et } }
            if (newFromEvents.isNotEmpty()) {
                texts.addAll(newFromEvents)
                debugInfo.append("EVT[+${newFromEvents.size}] ")
            }
            eventTexts.clear()
        }

        // Track normal text count to detect sudden drops
        if (isUberForeground && rootTextCount >= 10) {
            lastNormalTextCount = rootTextCount
        }

        // Detect trip popup: Uber is foreground, had normal text count before, now dropped
        val isTripPopup = isUberForeground && rootTextCount <= 5 && lastNormalTextCount >= 10

        // When trip popup detected, take screenshot for OCR
        if (isTripPopup) {
            debugInfo.append("POPUP! ")
            val now = System.currentTimeMillis()
            if (now - lastScreenshotTime > SCREENSHOT_COOLDOWN_MS) {
                lastScreenshotTime = now
                debugInfo.append("SCR! ")
                takeScreenshotForOCR(texts.toList())
            }
        }

        // Search for trip content via findByText
        if (isUberActive) {
            try {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    val tripTexts = mutableListOf<String>()

                    // Search for trip terms + price patterns
                    val searchTerms = listOf(
                        "Viaje:", "Aceptar", "incluido", "Moto", "Exclusivo",
                        "Artículo", "verificad", "min (", "CABA",
                        "$ 1", "$ 2", "$ 3", "$ 4", "$ 5", "$ 6", "$ 7", "$ 8", "$ 9",
                        "4,9", "4,8", "4,7", "4,6", "4,5"
                    )
                    for (term in searchTerms) {
                        val found = rootNode.findAccessibilityNodeInfosByText(term)
                        for (node in found) {
                            node.refresh()
                            val nodeText = node.text?.toString()?.takeIf { it.isNotBlank() }
                            if (nodeText != null && !tripTexts.contains(nodeText)) {
                                tripTexts.add(nodeText)
                            }
                            val subtreeTexts = mutableListOf<String>()
                            extractTexts(node, subtreeTexts)
                            subtreeTexts.forEach { st ->
                                if (!tripTexts.contains(st)) tripTexts.add(st)
                            }
                            node.recycle()
                        }
                    }

                    if (tripTexts.isNotEmpty()) {
                        val newTexts = tripTexts.filter { tt -> texts.none { it == tt } }
                        if (newTexts.isNotEmpty()) {
                            texts.addAll(newTexts)
                            debugInfo.append("FIND[+${newTexts.size}:${newTexts.joinToString("|").take(200)}] ")
                        }
                    }
                    rootNode.recycle()
                }
            } catch (e: Exception) {
                debugInfo.append("FIND_ERR[${e.message?.take(50)}] ")
            }
        }

        // Log - only log when we have content or on retries
        try {
            val file = java.io.File(filesDir, "uber_texts.log")
            val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
            if (texts.isNotEmpty()) {
                file.appendText("$ts [$source] $debugInfo\n")
                file.appendText("$ts TXT[${texts.size}]: $texts\n")
            }
            if (file.length() > 100_000) {
                val content = file.readText()
                file.writeText(content.takeLast(50_000))
            }
        } catch (_: Exception) {}

        // If Uber is active but got 0 texts, retry after a delay
        if (texts.isEmpty() && isUberActive && pendingRetries < MAX_RETRIES) {
            pendingRetries++
            handler.postDelayed({ scanAndProcess("RETRY$pendingRetries") }, RETRY_DELAY_MS)
            return
        }

        if (texts.isEmpty()) return

        // Filter out our own overlay texts before parsing
        val overlayFilters = listOf("UBER ANALYZER", "UBER", "$/km:", "$/h:", "Dist cobrada:", "Ratio:", "Recogida:", "Esperando viaje")
        val cleanTexts = texts.filter { text -> overlayFilters.none { text.contains(it) } }

        val hasPickupOrTrip = cleanTexts.any { it.contains("min (") && it.contains("km)") }
        if (hasPickupOrTrip) {
            Log.d(TAG, "TRIP_CANDIDATE ($source) [${cleanTexts.size}]: $cleanTexts")
        } else {
            return // No trip data in accessibility tree, rely on OCR
        }

        val trip = TripParser.parse(cleanTexts) ?: return

        Log.d(TAG, "TRIP_PARSED: ${trip.type} $${trip.price} $/km=${trip.pesosPorKm}")

        if (trip.tripKey == lastTripKey) return
        lastTripKey = trip.tripKey

        showTripOnOverlay(trip)

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

    private fun showTripOnOverlay(trip: TripData) {
        if (!OverlayService.isRunning() && android.provider.Settings.canDrawOverlays(this)) {
            startService(Intent(this, OverlayService::class.java))
            handler.postDelayed({ OverlayService.updateTrip(trip) }, 300)
        } else {
            OverlayService.updateTrip(trip)
        }
    }

    private fun takeScreenshotForOCR(existingTexts: List<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.e(TAG, "takeScreenshot requires API 30+, current: ${Build.VERSION.SDK_INT}")
            return
        }

        Log.d(TAG, "Taking screenshot for OCR...")
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        Log.d(TAG, "Screenshot success!")
                        try {
                            val bitmap = Bitmap.wrapHardwareBuffer(
                                result.hardwareBuffer, result.colorSpace
                            )
                            if (bitmap != null) {
                                // Convert to software bitmap for ML Kit
                                val swBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                bitmap.recycle()
                                result.hardwareBuffer.close()

                                if (swBitmap != null) {
                                    val image = InputImage.fromBitmap(swBitmap, 0)
                                    textRecognizer.process(image)
                                        .addOnSuccessListener { visionText ->
                                            val ocrTexts = mutableListOf<String>()
                                            for (block in visionText.textBlocks) {
                                                for (line in block.lines) {
                                                    ocrTexts.add(line.text)
                                                }
                                            }
                                            swBitmap.recycle()

                                            // Filter out our own overlay text
                                            val overlayPatterns = listOf(
                                                "UBER ANALYZER", "$/km:", "$/min:", "$/h:", "Dist cobrada:", "Ratio:",
                                                "Recogida:", "Esperando viaje"
                                            )
                                            val filteredTexts = ocrTexts.filter { text ->
                                                overlayPatterns.none { text.contains(it) }
                                            }

                                            // Log OCR results
                                            try {
                                                val file = java.io.File(filesDir, "uber_texts.log")
                                                val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
                                                file.appendText("$ts OCR[${filteredTexts.size}]: $filteredTexts\n")
                                            } catch (_: Exception) {}

                                            Log.d(TAG, "OCR[${filteredTexts.size}]: $filteredTexts")

                                            // Combine filtered OCR texts with existing texts and parse
                                            val allTexts = existingTexts.toMutableList()
                                            filteredTexts.forEach { ot ->
                                                if (allTexts.none { it == ot }) allTexts.add(ot)
                                            }

                                            val trip = TripParser.parse(allTexts) ?: return@addOnSuccessListener
                                            Log.d(TAG, "OCR_TRIP: ${trip.type} $${trip.price} $/km=${trip.pesosPorKm}")

                                            if (trip.tripKey == lastTripKey) return@addOnSuccessListener
                                            lastTripKey = trip.tripKey

                                            showTripOnOverlay(trip)

                                            // Time-based dedup: don't save to DB within 30s
                                            val saveNow = System.currentTimeMillis()
                                            if (saveNow - lastTripSaveTime < 30_000) return@addOnSuccessListener
                                            lastTripSaveTime = saveNow

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
                                                Log.d(TAG, "OCR Trip saved to DB")
                                            }

                                            sendBroadcast(Intent("com.carlos.uberanalyzer.TRIP_UPDATE"))
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "OCR failed: ${e.message}")
                                        }
                                }
                            } else {
                                result.hardwareBuffer.close()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Screenshot processing error: ${e.message}")
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e(TAG, "Screenshot FAILED: errorCode=$errorCode")
                        try {
                            val file = java.io.File(filesDir, "uber_texts.log")
                            val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
                            file.appendText("$ts SCREENSHOT_FAILED errorCode=$errorCode\n")
                        } catch (_: Exception) {}
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "takeScreenshot error: ${e.message}")
        }
    }

    private fun dumpHierarchy(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth)
        val cls = node.className?.toString()?.substringAfterLast('.') ?: "?"
        val text = node.text?.toString()?.take(80) ?: ""
        val desc = node.contentDescription?.toString()?.take(80) ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        sb.append("$indent$cls")
        if (viewId.isNotEmpty()) sb.append(" id=$viewId")
        if (text.isNotEmpty()) sb.append(" txt=\"$text\"")
        if (desc.isNotEmpty()) sb.append(" desc=\"$desc\"")
        sb.append(" [${bounds.left},${bounds.top}-${bounds.right},${bounds.bottom}]")
        sb.append(" vis=${node.isVisibleToUser} en=${node.isEnabled}")
        sb.append("\n")
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                dumpHierarchy(child, sb, depth + 1)
                child.recycle()
            } else {
                sb.append("$indent  NULL_CHILD[$i]\n")
            }
        }
    }

    private fun extractTexts(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        val text = node.text?.toString()?.takeIf { it.isNotBlank() }
        val desc = node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
        if (text != null) {
            texts.add(text)
        } else if (desc != null) {
            texts.add(desc)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractTexts(child, texts)
            child.recycle()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        isServiceRunning = false
        super.onDestroy()
    }
}
