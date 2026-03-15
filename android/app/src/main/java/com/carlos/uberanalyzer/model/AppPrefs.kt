package com.carlos.uberanalyzer.model

import android.content.Context

enum class DriverApp(val displayName: String) {
    UBER("Uber"),
    DIDI("Didi");

    companion object {
        fun fromName(name: String): DriverApp {
            return entries.firstOrNull { it.name == name } ?: UBER
        }
    }
}

object AppPrefs {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_SELECTED_APP = "selected_app"

    fun getSelectedApp(ctx: Context): DriverApp {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_SELECTED_APP, DriverApp.UBER.name) ?: DriverApp.UBER.name
        return DriverApp.fromName(name)
    }

    fun setSelectedApp(ctx: Context, app: DriverApp) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_APP, app.name)
            .apply()
    }
}
