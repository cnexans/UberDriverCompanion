package com.carlos.uberanalyzer.model

import android.content.Context
import android.content.SharedPreferences

object ThresholdPrefs {
    private const val PREFS_NAME = "thresholds"

    private const val KEY_KM_GREEN = "km_green"
    private const val KEY_KM_YELLOW = "km_yellow"
    private const val KEY_HORA_GREEN = "hora_green"
    private const val KEY_HORA_YELLOW = "hora_yellow"
    private const val KEY_PCT_GREEN = "pct_green"
    private const val KEY_PCT_YELLOW = "pct_yellow"

    // Defaults
    const val DEF_KM_GREEN = 300
    const val DEF_KM_YELLOW = 200
    const val DEF_HORA_GREEN = 6000
    const val DEF_HORA_YELLOW = 4000
    const val DEF_PCT_GREEN = 60
    const val DEF_PCT_YELLOW = 40

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getKmGreen(ctx: Context) = prefs(ctx).getInt(KEY_KM_GREEN, DEF_KM_GREEN)
    fun getKmYellow(ctx: Context) = prefs(ctx).getInt(KEY_KM_YELLOW, DEF_KM_YELLOW)
    fun getHoraGreen(ctx: Context) = prefs(ctx).getInt(KEY_HORA_GREEN, DEF_HORA_GREEN)
    fun getHoraYellow(ctx: Context) = prefs(ctx).getInt(KEY_HORA_YELLOW, DEF_HORA_YELLOW)
    fun getPctGreen(ctx: Context) = prefs(ctx).getInt(KEY_PCT_GREEN, DEF_PCT_GREEN)
    fun getPctYellow(ctx: Context) = prefs(ctx).getInt(KEY_PCT_YELLOW, DEF_PCT_YELLOW)

    fun setKmGreen(ctx: Context, v: Int) = prefs(ctx).edit().putInt(KEY_KM_GREEN, v).apply()
    fun setKmYellow(ctx: Context, v: Int) = prefs(ctx).edit().putInt(KEY_KM_YELLOW, v).apply()
    fun setHoraGreen(ctx: Context, v: Int) = prefs(ctx).edit().putInt(KEY_HORA_GREEN, v).apply()
    fun setHoraYellow(ctx: Context, v: Int) = prefs(ctx).edit().putInt(KEY_HORA_YELLOW, v).apply()
    fun setPctGreen(ctx: Context, v: Int) = prefs(ctx).edit().putInt(KEY_PCT_GREEN, v).apply()
    fun setPctYellow(ctx: Context, v: Int) = prefs(ctx).edit().putInt(KEY_PCT_YELLOW, v).apply()
}
