package com.example.safe_route_project.app

import android.app.Application
import com.example.safe_route_project.settings.AppThemeManager
import com.example.safe_route_project.work.DisasterWorkScheduler
import com.google.firebase.messaging.FirebaseMessaging

class SafeRouteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppThemeManager.applySavedTheme(this)
        DisasterWorkScheduler.schedule(this)

        // FCM 토픽 구독 - 재난 알림 수신
        FirebaseMessaging.getInstance().subscribeToTopic("disaster_alerts")
    }
}