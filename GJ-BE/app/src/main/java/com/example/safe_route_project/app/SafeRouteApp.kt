package com.example.safe_route_project.app

import android.app.Application
import com.example.safe_route_project.settings.AppThemeManager
import com.example.safe_route_project.work.DisasterWorkScheduler

class SafeRouteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppThemeManager.applySavedTheme(this)
        DisasterWorkScheduler.schedule(this)
    }
}