package edu.ap.tobadtoostay.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import edu.ap.tobadtoostay.data.model.NearbyBag
import kotlinx.coroutines.tasks.await
import java.util.Date

class ReservationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // a transaction, so two customers can't take the last bag at the same time
    suspend fun reserve(nearbyBag: NearbyBag, quantity: Int): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
        val bag = nearbyBag.bag
        val bagRef = db.collection("bags").document(bag.id)
        val reservationRef = db.collection("reservations").document()
        val pickupCode = makePickupCode()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(bagRef)
            val quantityLeft = snapshot.getLong("quantityLeft")?.toInt() ?: 0
            val pickupEnd = snapshot.getTimestamp("pickupEnd")?.toDate()?.time

            if (pickupEnd != null && pickupEnd <= System.currentTimeMillis()) {
                throw FirebaseFirestoreException("Pickup over", FirebaseFirestoreException.Code.OUT_OF_RANGE)
            }
            if (quantity < 1 || quantity > quantityLeft) {
                throw FirebaseFirestoreException("Sold out", FirebaseFirestoreException.Code.FAILED_PRECONDITION)
            }

            transaction.update(bagRef, "quantityLeft", quantityLeft - quantity)
            transaction.set(
                reservationRef,
                hashMapOf(
                    "bagId" to bag.id,
                    "storeId" to bag.storeId,
                    "customerId" to uid,
                    "quantity" to quantity,
                    "pickupCode" to pickupCode,
                    "status" to "reserved",
                    "createdAt" to Timestamp.now(),
                    "bagTitle" to bag.title,
                    "storeName" to nearbyBag.store.name,
                    "pickupStart" to bag.pickupStart?.let { Timestamp(Date(it)) },
                    "pickupEnd" to bag.pickupEnd?.let { Timestamp(Date(it)) }
                )
            )
        }.await()

        return pickupCode
    }

    private fun makePickupCode(): String {
        // no 0/O and 1/I, they look the same
        val characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..CODE_LENGTH).map { characters.random() }.joinToString("")
    }

    private companion object {
        const val CODE_LENGTH = 6
    }
}
