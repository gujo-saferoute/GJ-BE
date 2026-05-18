package com.example.safe_route_project.app

import android.app.Application
import com.example.safe_route_project.settings.AppThemeManager
import com.google.firebase.messaging.FirebaseMessaging

class SafeRouteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppThemeManager.applySavedTheme(this)
        FirebaseMessaging.getInstance().subscribeToTopic("disaster_alerts")
    }
}