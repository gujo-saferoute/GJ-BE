package com.example.safe_route_project.data.disaster.store

import android.content.Context
import com.example.safe_route_project.data.disaster.model.DisasterAlert

class DisasterAlertStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getLastFingerprint(): String? = preferences.getString(KEY_LAST_FINGERPRINT, null)

    fun save(alert: DisasterAlert) {
        preferences.edit()
            .putString(KEY_LAST_FINGERPRINT, alert.fingerprint)
            .putString(KEY_LAST_TITLE, alert.title)
            .putString(KEY_LAST_MESSAGE, alert.message)
            .putString(KEY_LAST_SOURCE, alert.source)
            .apply()
    }

    fun getCachedAlert(): DisasterAlert? {
        val fingerprint = preferences.getString(KEY_LAST_FINGERPRINT, null) ?: return null
        val title = preferences.getString(KEY_LAST_TITLE, null) ?: return null
        val message = preferences.getString(KEY_LAST_MESSAGE, null) ?: return null
        val source = preferences.getString(KEY_LAST_SOURCE, null) ?: return null

        return DisasterAlert(
            fingerprint = fingerprint,
            title = title,
            message = message,
            source = source,
            region = "",
            createdAt = "",
            regYmd = "",
            sn = null,
        )
    }

    companion object {
        private const val PREF_NAME = "disaster_alert_store"
        private const val KEY_LAST_FINGERPRINT = "key_last_fingerprint"
        private const val KEY_LAST_TITLE = "key_last_title"
        private const val KEY_LAST_MESSAGE = "key_last_message"
        private const val KEY_LAST_SOURCE = "key_last_source"
    }
}