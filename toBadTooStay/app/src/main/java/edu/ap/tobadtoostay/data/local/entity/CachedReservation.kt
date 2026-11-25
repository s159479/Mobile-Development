package edu.ap.tobadtoostay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local copy of a Firestore reservation, so the orders screen also works offline (K10).
 * Times are epoch millis, status is one of [edu.ap.tobadtoostay.data.model.ReservationStatus].
 * customerId makes sure a second account on the same phone never sees these orders.
 */
@Entity(tableName = "cached_reservations")
data class CachedReservation(
    @PrimaryKey val id: String,
    val customerId: String,
    val bagTitle: String,
    val storeName: String,
    val quantity: Int,
    val pickupCode: String,
    val pickupStart: Long,
    val pickupEnd: Long,
    val status: String,
    val createdAt: Long
)
