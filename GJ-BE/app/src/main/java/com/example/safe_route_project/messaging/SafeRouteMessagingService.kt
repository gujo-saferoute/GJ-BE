package com.example.safe_route_project.messaging

import com.example.safe_route_project.MainActivity
import com.example.safe_route_project.notification.DisasterNotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SafeRouteMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "재난문자"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        val openTab = message.data["open_tab"] ?: MainActivity.TAB_MAP
        val notificationId = message.data["fingerprint"]?.hashCode()
            ?: System.currentTimeMillis().toInt()

        DisasterNotificationHelper(applicationContext).show(
            title = title,
            message = body,
            openTab = openTab,
            notificationId = notificationId
        )
    }
}