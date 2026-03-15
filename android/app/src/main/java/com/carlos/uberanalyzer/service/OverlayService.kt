package com.carlos.uberanalyzer.service

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.carlos.uberanalyzer.R
import com.carlos.uberanalyzer.model.AppPrefs
import com.carlos.uberanalyzer.model.ThresholdPrefs
import com.carlos.uberanalyzer.model.TripData

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isExpanded = true

    companion object {
        private var instance: OverlayService? = null
        var onSyncRequested: (() -> Unit)? = null

        fun updateTrip(trip: TripData) {
            instance?.showTrip(trip)
        }

        fun isRunning(): Boolean = instance != null

        fun showWaiting() {
            instance?.showWaitingState()
        }

        fun hide() {
            instance?.let { svc ->
                svc.layoutParams?.let { params ->
                    params.alpha = 0f
                    try { svc.windowManager?.updateViewLayout(svc.overlayView, params) } catch (_: Exception) {}
                }
            }
        }

        fun show() {
            instance?.let { svc ->
                svc.layoutParams?.let { params ->
                    params.alpha = 1f
                    try { svc.windowManager?.updateViewLayout(svc.overlayView, params) } catch (_: Exception) {}
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Settings.canDrawOverlays(this)) {
            createOverlay()
        }
    }

    override fun onDestroy() {
        instance = null
        overlayView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun createOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 200
        }
        layoutParams = params

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null)

        // Sync button
        overlayView?.findViewById<TextView>(R.id.btnSync)?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> true
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    onSyncRequested?.invoke()
                    true
                }
                else -> true
            }
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var hasMoved = false

        overlayView?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > 100) hasMoved = true
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    try { windowManager?.updateViewLayout(v, params) } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) {
                        isExpanded = !isExpanded
                        overlayView?.findViewById<LinearLayout>(R.id.contentLayout)?.visibility =
                            if (isExpanded) View.VISIBLE else View.GONE
                    }
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(overlayView, params)
        updateTitle()
    }

    private fun updateTitle() {
        val appName = AppPrefs.getSelectedApp(this).displayName.uppercase()
        overlayView?.findViewById<TextView>(R.id.tvTitle)?.text = "\u25AC $appName \u25AC"
    }

    private fun showWaitingState() {
        overlayView?.let { view ->
            view.findViewById<TextView>(R.id.tvPrice)?.text = "Esperando viaje..."
            view.findViewById<TextView>(R.id.tvPrice)?.setTextColor(Color.parseColor("#888888"))
            view.findViewById<TextView>(R.id.tvType)?.text = ""
            view.findViewById<TextView>(R.id.tvPorKm)?.apply {
                text = ""
                visibility = View.GONE
            }
            view.findViewById<TextView>(R.id.tvPorHora)?.apply {
                text = ""
                visibility = View.GONE
            }
            view.findViewById<TextView>(R.id.tvRatio)?.apply {
                text = ""
                visibility = View.GONE
            }
            view.findViewById<TextView>(R.id.tvPickup)?.apply {
                text = ""
                visibility = View.GONE
            }
            view.findViewById<TextView>(R.id.tvTrip)?.apply {
                text = ""
                visibility = View.GONE
            }
        }
    }

    private fun showTrip(trip: TripData) {
        if (overlayView == null && Settings.canDrawOverlays(this)) {
            createOverlay()
        }
        overlayView?.let { view ->
            updateTitle()
            // Restore visibility of all fields
            listOf(R.id.tvPorKm, R.id.tvPorHora, R.id.tvRatio, R.id.tvPickup, R.id.tvTrip).forEach { id ->
                view.findViewById<TextView>(id)?.visibility = View.VISIBLE
            }

            view.findViewById<TextView>(R.id.tvPrice)?.apply {
                text = "$ ${formatNumber(trip.price)}"
                setTextColor(Color.WHITE)
            }

            val kmGreen = ThresholdPrefs.getKmGreen(this)
            val kmYellow = ThresholdPrefs.getKmYellow(this)
            val tvPorKm = view.findViewById<TextView>(R.id.tvPorKm)
            tvPorKm?.text = "$/km: ${formatNumber(trip.pesosPorKm)}"
            tvPorKm?.setTextColor(colorForValue(trip.pesosPorKm, kmGreen.toDouble(), kmYellow.toDouble()))

            val horaGreen = ThresholdPrefs.getHoraGreen(this)
            val horaYellow = ThresholdPrefs.getHoraYellow(this)
            val pesosPorHora = trip.pesosPorMin * 60
            val tvPorHora = view.findViewById<TextView>(R.id.tvPorHora)
            tvPorHora?.text = "$/h: ${formatNumber(pesosPorHora)}"
            tvPorHora?.setTextColor(colorForValue(pesosPorHora, horaGreen.toDouble(), horaYellow.toDouble()))

            val pctGreen = ThresholdPrefs.getPctGreen(this)
            val pctYellow = ThresholdPrefs.getPctYellow(this)
            val tvRatio = view.findViewById<TextView>(R.id.tvRatio)
            tvRatio?.text = "Dist cobrada: ${String.format("%.0f", trip.pctDistancia)}%"
            tvRatio?.setTextColor(colorForValue(trip.pctDistancia, pctGreen.toDouble(), pctYellow.toDouble()))

            view.findViewById<TextView>(R.id.tvType)?.text = "${trip.type} ${trip.subtype ?: ""}"
            view.findViewById<TextView>(R.id.tvPickup)?.text = "Recogida: ${trip.pickupMinutes}min (${trip.pickupKm}km)"
            view.findViewById<TextView>(R.id.tvTrip)?.text = "Viaje: ${trip.tripMinutes}min (${trip.tripKm}km)"

            if (!isExpanded) {
                isExpanded = true
                view.findViewById<LinearLayout>(R.id.contentLayout)?.visibility = View.VISIBLE
            }
        }
    }

    private fun colorForValue(value: Double, green: Double, yellow: Double): Int {
        return when {
            value >= green -> Color.GREEN
            value >= yellow -> Color.YELLOW
            else -> Color.RED
        }
    }

    private fun formatNumber(value: Double): String {
        return String.format("%,.0f", value)
    }
}
