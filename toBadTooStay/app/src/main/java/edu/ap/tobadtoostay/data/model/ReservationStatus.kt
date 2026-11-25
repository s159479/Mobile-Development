package edu.ap.tobadtoostay.data.model

/** The values of the field "status" of a reservation in Firestore (reservations/{id}). */
object ReservationStatus {
    const val RESERVED = "reserved"
    const val COLLECTED = "collected"
    const val CANCELLED = "cancelled"
}
