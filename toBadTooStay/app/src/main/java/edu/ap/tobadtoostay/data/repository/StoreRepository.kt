package edu.ap.tobadtoostay.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import edu.ap.tobadtoostay.data.model.Store
import kotlinx.coroutines.tasks.await

class StoreRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val stores = db.collection("stores")

    /** The uid of the logged-in merchant is also the id of their store (one store per merchant). */
    private fun currentUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")

    /** Returns the store of the logged-in merchant, or null if they have none yet. */
    suspend fun getMyStore(): Store? {
        val uid = currentUid()
        val doc = stores.document(uid).get().await()
        if (!doc.exists()) return null

        val location = doc.getGeoPoint("location")
        return Store(
            id = doc.id,
            ownerId = doc.getString("ownerId").orEmpty(),
            name = doc.getString("name").orEmpty(),
            category = doc.getString("category").orEmpty(),
            latitude = location?.latitude ?: 0.0,
            longitude = location?.longitude ?: 0.0,
            street = doc.getString("street").orEmpty(),
            postalCode = doc.getString("postalCode").orEmpty(),
            city = doc.getString("city").orEmpty()
        )
    }

    /** Creates or updates the store of the logged-in merchant. */
    suspend fun saveMyStore(store: Store) {
        val uid = currentUid()
        val data = hashMapOf(
            "ownerId" to uid,
            "name" to store.name,
            "category" to store.category,
            "location" to GeoPoint(store.latitude, store.longitude),
            "address" to store.address,
            "city" to store.city,
            // extra fields, so the form can be filled in again when editing
            "street" to store.street,
            "postalCode" to store.postalCode
        )
        stores.document(uid).set(data).await()
    }
}
