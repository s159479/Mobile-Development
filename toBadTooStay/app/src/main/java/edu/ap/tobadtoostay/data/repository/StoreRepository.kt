package edu.ap.tobadtoostay.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import edu.ap.tobadtoostay.data.location.GeoMath
import edu.ap.tobadtoostay.data.model.MapPoint
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
        return doc.toStore()
    }

    /**
     * Stores within [radiusMeters] of [center].
     * Firestore can't search on distance, so we ask the stores in a band of latitudes
     * (GeoPoints are sorted on latitude first) and check the real distance here.
     */
    suspend fun getStoresNear(center: MapPoint, radiusMeters: Double): List<Store> {
        val latitudes = GeoMath.latitudeRange(center, radiusMeters)
        val docs = stores
            .whereGreaterThanOrEqualTo("location", GeoPoint(latitudes.start, -180.0))
            .whereLessThanOrEqualTo("location", GeoPoint(latitudes.endInclusive, 180.0))
            .get()
            .await()

        return docs.documents
            .filter { it.getGeoPoint("location") != null }
            .map { it.toStore() }
            .filter { GeoMath.distanceMeters(center, MapPoint(it.latitude, it.longitude)) <= radiusMeters }
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

    private fun DocumentSnapshot.toStore(): Store {
        val location = getGeoPoint("location")
        return Store(
            id = id,
            ownerId = getString("ownerId").orEmpty(),
            name = getString("name").orEmpty(),
            category = getString("category").orEmpty(),
            latitude = location?.latitude ?: 0.0,
            longitude = location?.longitude ?: 0.0,
            street = getString("street").orEmpty(),
            postalCode = getString("postalCode").orEmpty(),
            city = getString("city").orEmpty()
        )
    }
}
