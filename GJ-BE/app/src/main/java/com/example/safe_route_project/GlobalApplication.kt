package com.example.safe_route_project

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoMapSdk.init(this, "34307666cf66aebb77c6e2a1df53b743")
    }
}