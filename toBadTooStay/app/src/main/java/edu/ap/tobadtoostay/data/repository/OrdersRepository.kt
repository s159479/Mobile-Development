package edu.ap.tobadtoostay.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import edu.ap.tobadtoostay.data.local.dao.CachedReservationDao
import edu.ap.tobadtoostay.data.local.entity.CachedReservation
import edu.ap.tobadtoostay.data.model.ReservationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Orders of the customer (K10). Firestore is the source, Room is the copy the screen reads,
 * so the orders are also there without internet.
 * Every row has the uid of the customer, so the orders stay after logout
 * and another account on the same phone does not see them.
 */
class OrdersRepository(
    private val dao: CachedReservationDao,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** The orders saved on this phone. The screen only reads this. */
    fun observeCached(customerId: String): Flow<List<CachedReservation>> =
        dao.observeForCustomer(customerId)

    /**
     * Listens to the reservations of the customer in Firestore and writes every change to Room.
     * Emits true while Firestore only has its offline cache: then Room is not overwritten,
     * because that cache can be incomplete.
     */
    fun syncFromFirestore(customerId: String): Flow<Boolean> {
        // The first answer from the server is always written, because Room may be empty or old
        var previousWasOffline = true

        return db.collection(RESERVATIONS)
            // the security rules only allow reading your own reservations, so this filter is needed
            .whereEqualTo(CUSTOMER_ID, customerId)
            // INCLUDE: also tell us when we go from offline (cache) to online (server)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val offline = snapshot.metadata.isFromCache
                // documentChanges leaves out snapshots where only the metadata changed
                val changed = snapshot.documentChanges.isNotEmpty() || previousWasOffline

                if (!offline && changed) {
                    dao.replaceForCustomer(customerId, snapshot.documents.map { it.toCached() })
                }
                previousWasOffline = offline
                offline
            }
    }

    /**
     * Removes the orders of other accounts whose pickup ended more than [KEEP_DAYS] days ago,
     * so old pickup codes do not stay on a shared phone forever.
     * The orders of [customerId] are left alone: they come back from Firestore anyway.
     */
    suspend fun removeOldOrdersOfOthers(customerId: String, now: Long = System.currentTimeMillis()) {
        dao.deleteOthersEndedBefore(customerId, now - KEEP_DAYS * DAY_MS)
    }

    private fun DocumentSnapshot.toCached() = CachedReservation(
        id = id,
        customerId = getString(CUSTOMER_ID).orEmpty(),
        bagTitle = getString("bagTitle").orEmpty(),
        storeName = getString("storeName").orEmpty(),
        quantity = getLong("quantity")?.toInt() ?: 0,
        pickupCode = getString("pickupCode").orEmpty(),
        pickupStart = getTimestamp("pickupStart")?.toDate()?.time ?: 0L,
        pickupEnd = getTimestamp("pickupEnd")?.toDate()?.time ?: 0L,
        status = getString("status") ?: ReservationStatus.RESERVED,
        createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L
    )

    private companion object {
        const val RESERVATIONS = "reservations"
        const val CUSTOMER_ID = "customerId"
        const val KEEP_DAYS = 30L
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
