package com.carlos.uberanalyzer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.carlos.uberanalyzer.db.TripDatabase
import com.carlos.uberanalyzer.service.OverlayService
import com.carlos.uberanalyzer.service.UberAccessibilityService
import com.carlos.uberanalyzer.ui.HistoryAdapter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: HistoryAdapter
    private val db by lazy { TripDatabase.getInstance(this) }

    private val tripReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = HistoryAdapter()
        findViewById<RecyclerView>(R.id.recyclerHistory).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<MaterialButton>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<MaterialButton>(R.id.btnOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshData()
        registerReceiver(tripReceiver, IntentFilter("com.carlos.uberanalyzer.TRIP_UPDATE"),
            RECEIVER_NOT_EXPORTED)

        // Start overlay service if both permissions are granted
        if (Settings.canDrawOverlays(this) && !OverlayService.isRunning()) {
            startService(Intent(this, OverlayService::class.java))
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(tripReceiver)
    }

    private fun updateStatus() {
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val accessibilityOn = UberAccessibilityService.isServiceRunning
        val overlayOn = Settings.canDrawOverlays(this)

        tvStatus.text = when {
            accessibilityOn && overlayOn -> "Servicio: Activo"
            accessibilityOn -> "Falta: Permiso overlay"
            overlayOn -> "Falta: Servicio accesibilidad"
            else -> "Servicio: Inactivo"
        }
        tvStatus.setTextColor(
            if (accessibilityOn && overlayOn) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt()
        )
    }

    private fun refreshData() {
        lifecycleScope.launch {
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val trips = db.tripDao().getToday(startOfDay)
            adapter.submitList(trips)

            val stats = db.tripDao().getStatsToday(startOfDay)
            findViewById<TextView>(R.id.tvStatsCount).text = "${stats.count}"
            findViewById<TextView>(R.id.tvStatsEarnings).text =
                "$ ${String.format("%,.0f", stats.totalEarnings ?: 0.0)}"
            findViewById<TextView>(R.id.tvStatsAvgKm).text =
                "$ ${String.format("%,.0f", stats.avgPorKm ?: 0.0)}/km"
        }
    }
}
