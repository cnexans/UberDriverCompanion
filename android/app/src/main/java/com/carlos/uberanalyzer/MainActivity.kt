package com.carlos.uberanalyzer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.carlos.uberanalyzer.model.ThresholdPrefs
import com.carlos.uberanalyzer.service.OverlayService
import com.carlos.uberanalyzer.service.UberAccessibilityService
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private var overlayVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Permission buttons
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

        // Overlay toggle
        val btnToggle = findViewById<MaterialButton>(R.id.btnToggleOverlay)
        btnToggle.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) return@setOnClickListener

            if (overlayVisible) {
                stopService(Intent(this, OverlayService::class.java))
                overlayVisible = false
                btnToggle.text = "Mostrar Overlay"
            } else {
                startService(Intent(this, OverlayService::class.java))
                overlayVisible = true
                btnToggle.text = "Ocultar Overlay"
            }
        }

        // Setup threshold rows
        setupThreshold(
            findViewById(R.id.thresholdKm),
            "$/km",
            ThresholdPrefs.getKmGreen(this),
            ThresholdPrefs.getKmYellow(this),
            { ThresholdPrefs.setKmGreen(this, it) },
            { ThresholdPrefs.setKmYellow(this, it) }
        )

        setupThreshold(
            findViewById(R.id.thresholdHora),
            "$/hora",
            ThresholdPrefs.getHoraGreen(this),
            ThresholdPrefs.getHoraYellow(this),
            { ThresholdPrefs.setHoraGreen(this, it) },
            { ThresholdPrefs.setHoraYellow(this, it) }
        )

        setupThreshold(
            findViewById(R.id.thresholdPct),
            "% dist. cobrada",
            ThresholdPrefs.getPctGreen(this),
            ThresholdPrefs.getPctYellow(this),
            { ThresholdPrefs.setPctGreen(this, it) },
            { ThresholdPrefs.setPctYellow(this, it) }
        )
    }

    override fun onResume() {
        super.onResume()
        updateStatus()

        overlayVisible = OverlayService.isRunning()
        findViewById<MaterialButton>(R.id.btnToggleOverlay).text =
            if (overlayVisible) "Ocultar Overlay" else "Mostrar Overlay"
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

    private fun setupThreshold(
        row: View,
        label: String,
        greenVal: Int,
        yellowVal: Int,
        saveGreen: (Int) -> Unit,
        saveYellow: (Int) -> Unit
    ) {
        row.findViewById<TextView>(R.id.tvThresholdLabel).text = label

        val etGreen = row.findViewById<EditText>(R.id.etGreen)
        val etYellow = row.findViewById<EditText>(R.id.etYellow)

        etGreen.setText(greenVal.toString())
        etYellow.setText(yellowVal.toString())

        etGreen.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { saveGreen(it) }
            }
        })

        etYellow.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { saveYellow(it) }
            }
        })
    }
}
