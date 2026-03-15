package com.carlos.uberanalyzer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.carlos.uberanalyzer.billing.BillingManager
import com.carlos.uberanalyzer.model.AppPrefs
import com.carlos.uberanalyzer.model.DriverApp
import com.carlos.uberanalyzer.model.ThresholdPrefs
import com.carlos.uberanalyzer.service.OverlayService
import com.carlos.uberanalyzer.service.UberAccessibilityService
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var overlayVisible = false
    private lateinit var billingManager: BillingManager
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ads & billing only when SHOW_ADS is enabled (production builds)
        adView = findViewById(R.id.adView)
        billingManager = BillingManager(this)

        if (BuildConfig.SHOW_ADS) {
            billingManager.startConnection()

            lifecycleScope.launch {
                billingManager.isAdFree.collect { adFree ->
                    updateAdVisibility(adFree)
                }
            }

            findViewById<MaterialButton>(R.id.btnRemoveAds).setOnClickListener {
                billingManager.launchPurchase(this)
            }

            if (!billingManager.isAdFree.value) {
                adView?.loadAd(AdRequest.Builder().build())
                adView?.visibility = View.VISIBLE
                findViewById<MaterialButton>(R.id.btnRemoveAds).visibility = View.VISIBLE
            }
        } else {
            // Testing builds: no ads, no billing
            adView?.visibility = View.GONE
            findViewById<MaterialButton>(R.id.btnRemoveAds).visibility = View.GONE
        }

        // App selector
        val spinnerApp = findViewById<Spinner>(R.id.spinnerApp)
        val apps = DriverApp.entries.map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, apps)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerApp.adapter = adapter
        spinnerApp.setSelection(AppPrefs.getSelectedApp(this).ordinal)
        spinnerApp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                AppPrefs.setSelectedApp(this@MainActivity, DriverApp.entries[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

    private fun updateAdVisibility(adFree: Boolean) {
        if (adFree) {
            adView?.visibility = View.GONE
            adView?.destroy()
            findViewById<MaterialButton>(R.id.btnRemoveAds).visibility = View.GONE
        } else {
            adView?.visibility = View.VISIBLE
            findViewById<MaterialButton>(R.id.btnRemoveAds).visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        if (BuildConfig.SHOW_ADS) adView?.resume()

        overlayVisible = OverlayService.isRunning()
        findViewById<MaterialButton>(R.id.btnToggleOverlay).text =
            if (overlayVisible) "Ocultar Overlay" else "Mostrar Overlay"
    }

    override fun onPause() {
        if (BuildConfig.SHOW_ADS) adView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        if (BuildConfig.SHOW_ADS) {
            adView?.destroy()
            billingManager.destroy()
        }
        super.onDestroy()
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
