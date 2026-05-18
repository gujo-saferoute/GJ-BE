package com.example.safe_route_project.data.shelter

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.skt.tmap.TMapPoint

class ShelterRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun fetchShelters(
        onSuccess: (List<ShelterPin>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("shelters")
            .get()
            .addOnSuccessListener { result ->
                onSuccess(result.toShelterPins())
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    private fun QuerySnapshot.toShelterPins(): List<ShelterPin> {
        return documents.mapNotNull { doc ->
            val markerId = doc.getString("markerId") ?: return@mapNotNull null
            val name = doc.getString("name") ?: return@mapNotNull null
            val address = doc.getString("address") ?: ""
            val description = doc.getString("description") ?: ""
            val latitude = doc.getDouble("latitude") ?: return@mapNotNull null
            val longitude = doc.getDouble("longitude") ?: return@mapNotNull null
            val barrierFree = doc.getBoolean("barrierFree") ?: false
            val evalInfo = doc.getString("evalInfo") ?: ""

            val disasterTypeStrings = doc.get("disasterTypes") as? List<*> ?: emptyList<Any>()
            val disasterTypes = disasterTypeStrings
                .mapNotNull { it as? String }
                .mapNotNull { typeName ->
                    DisasterType.entries.firstOrNull { it.name == typeName }
                }
                .toSet()

            ShelterPin(
                markerId = markerId,
                name = name,
                address = address,
                description = description,
                point = TMapPoint(latitude, longitude),
                disasterTypes = disasterTypes,
                barrierFree = barrierFree,
                evalInfo = evalInfo
            )
        }
    }
}