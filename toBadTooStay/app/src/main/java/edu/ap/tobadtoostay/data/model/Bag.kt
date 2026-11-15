package edu.ap.tobadtoostay.data.model

/**
 * A surprise bag in Firestore: bags/{bagId}.
 * [storeId] is the uid of the merchant (= id of the store), see firestore.rules.
 * Times are epoch milliseconds (saved as Timestamp in Firestore).
 */
data class Bag(
    val id: String = "",
    val storeId: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val quantityTotal: Int = 0,
    val quantityLeft: Int = 0,
    val pickupStart: Long? = null,
    val pickupEnd: Long? = null
) {
    /** Can still be reserved: something left and the pickup time is not over yet. */
    fun isAvailable(now: Long): Boolean =
        quantityLeft > 0 && (pickupEnd == null || pickupEnd > now)
}

/** A bag with its store and the distance from the customer, for the list and the map. */
data class NearbyBag(
    val bag: Bag,
    val store: Store,
    val distanceMeters: Double
)
