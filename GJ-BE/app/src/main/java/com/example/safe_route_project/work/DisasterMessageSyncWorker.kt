package com.example.safe_route_project.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.safe_route_project.app.ServiceLocator
import com.example.safe_route_project.data.disaster.store.DisasterAlertStore
import com.example.safe_route_project.notification.DisasterNotificationHelper

class DisasterMessageSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = DisasterAlertStore(applicationContext)
        val repository = ServiceLocator.disasterMessageRepository(applicationContext)

        return runCatching {
            val latest = repository.getLatestAlert() ?: return Result.success()
            val oldFingerprint = store.getLastFingerprint()

            if (oldFingerprint == null) {
                store.save(latest)
                return Result.success()
            }

            if (oldFingerprint != latest.fingerprint) {
                store.save(latest)
                DisasterNotificationHelper(applicationContext).show(latest)
            }

            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}