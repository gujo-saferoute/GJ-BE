package com.example.safe_route_project.settings

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat

object AppThemeManager {

    private const val PREF_NAME = "safe_route_settings"
    private const val KEY_DARK_MODE = "key_dark_mode"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun applySavedTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkModeEnabled(context)) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DARK_MODE, false)
    }

    fun bind(activity: AppCompatActivity, switch: SwitchCompat) {
        switch.isChecked = isDarkModeEnabled(activity)

        switch.setOnCheckedChangeListener { _, isChecked ->
            setDarkModeEnabled(activity, isChecked)
        }
    }

    private fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        if (isDarkModeEnabled(context) == enabled) return

        prefs(context)
            .edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()

        AppCompatDelegate.setDefaultNightMode(
            if (enabled) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }
}