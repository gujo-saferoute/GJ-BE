package com.example.safe_route_project.app

import android.content.Context
import com.example.safe_route_project.data.disaster.repository.DisasterMessageRepository

object ServiceLocator {
    @Volatile
    private var disasterMessageRepository: DisasterMessageRepository? = null

    fun disasterMessageRepository(context: Context): DisasterMessageRepository {
        return disasterMessageRepository ?: synchronized(this) {
            disasterMessageRepository ?: DisasterMessageRepository.create(context.applicationContext).also {
                disasterMessageRepository = it
            }
        }
    }
}