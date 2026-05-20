package com.example.safe_route_project.settings

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

object DisasterTestManager {

    private const val PREF_NAME = "safe_route_settings"
    private const val KEY_DISASTER_TEST = "key_disaster_test"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DISASTER_TEST, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context)
            .edit()
            .putBoolean(KEY_DISASTER_TEST, enabled)
            .apply()
    }

    fun bind(
        activity: AppCompatActivity,
        switch: SwitchCompat,
        onChanged: (Boolean) -> Unit
    ) {
        switch.isChecked = isEnabled(activity)

        switch.setOnCheckedChangeListener { _, isChecked ->
            setEnabled(activity, isChecked)
            onChanged(isChecked)
        }
    }
}