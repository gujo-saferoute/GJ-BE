package com.example.safe_route_project.app

import android.content.Context
import com.example.safe_route_project.data.disaster.repository.DisasterMessageRepository
import com.example.safe_route_project.data.shelter.ShelterRepository

object ServiceLocator {

    @Volatile
    private var disasterMessageRepository: DisasterMessageRepository? = null

    @Volatile
    private var shelterRepository: ShelterRepository? = null

    fun disasterMessageRepository(context: Context): DisasterMessageRepository {
        return disasterMessageRepository ?: synchronized(this) {
            disasterMessageRepository ?: DisasterMessageRepository.create(context.applicationContext).also {
                disasterMessageRepository = it
            }
        }
    }

    fun shelterRepository(): ShelterRepository {
        return shelterRepository ?: synchronized(this) {
            shelterRepository ?: ShelterRepository().also {
                shelterRepository = it
            }
        }
    }
}