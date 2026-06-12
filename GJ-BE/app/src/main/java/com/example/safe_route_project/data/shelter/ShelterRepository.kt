package com.example.safe_route_project.data.shelter

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.skt.tmap.TMapPoint

class ShelterRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val shelterCollection = firestore.collection("shelters")

    fun fetchShelters(
        onSuccess: (List<ShelterPin>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        shelterCollection
            .get()
            .addOnSuccessListener { result ->
                onSuccess(result.toShelterPins())
            }
            .addOnFailureListener { exception -> onFailure(exception) }
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

            ShelterPin(
                markerId = markerId,
                name = name,
                address = address,
                description = description,
                point = TMapPoint(latitude, longitude),
                barrierFree = barrierFree,
                evalInfo = evalInfo,
                disasterTypes = doc.disasterTypes()
            )
        }
    }

    private fun DocumentSnapshot.disasterTypes(): List<String> {
        return get("disasterTypes").toStringList()
            .ifEmpty { listOfNotNull(getString("disasterType")) }
    }

    private fun Any?.toStringList(): List<String> {
        return (this as? List<*>)
            ?.mapNotNull { value -> value as? String }
            ?: emptyList()
    }
}
