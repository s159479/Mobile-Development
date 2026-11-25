package edu.ap.tobadtoostay.data.model

import edu.ap.tobadtoostay.data.local.entity.CachedReservation
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdersTest {

    private val now = 1_000_000L

    private fun reservation(
        id: String,
        status: String = ReservationStatus.RESERVED,
        pickupStart: Long = now + 1_000,
        pickupEnd: Long = now + 2_000,
        createdAt: Long = 0L
    ) = CachedReservation(
        id = id,
        customerId = "customer",
        bagTitle = "Bag $id",
        storeName = "Store",
        quantity = 1,
        pickupCode = "ABC234",
        pickupStart = pickupStart,
        pickupEnd = pickupEnd,
        status = status,
        createdAt = createdAt
    )

    @Test
    fun reservedBeforeEndIsActive() {
        assertEquals(OrderStatus.RESERVED, Orders.statusOf(reservation("a"), now))
    }

    @Test
    fun reservedAfterEndIsExpired() {
        val late = reservation("a", pickupStart = now - 2_000, pickupEnd = now - 1)
        assertEquals(OrderStatus.EXPIRED, Orders.statusOf(late, now))
    }

    @Test
    fun reservedExactlyAtEndIsExpired() {
        val endsNow = reservation("a", pickupStart = now - 2_000, pickupEnd = now)
        assertEquals(OrderStatus.EXPIRED, Orders.statusOf(endsNow, now))
    }

    @Test
    fun reservedWithoutEndStaysActive() {
        val noEnd = reservation("a", pickupStart = 0, pickupEnd = 0)
        assertEquals(OrderStatus.RESERVED, Orders.statusOf(noEnd, now))
    }

    @Test
    fun collectedAndCancelledKeepTheirStatus() {
        assertEquals(OrderStatus.COLLECTED, Orders.statusOf(reservation("a", ReservationStatus.COLLECTED), now))
        assertEquals(OrderStatus.CANCELLED, Orders.statusOf(reservation("a", ReservationStatus.CANCELLED), now))
    }

    @Test
    fun splitPutsActiveFirstByPickupAndPastNewestFirst() {
        val split = Orders.split(
            listOf(
                reservation("later", pickupStart = now + 5_000, pickupEnd = now + 6_000),
                reservation("old", ReservationStatus.COLLECTED, createdAt = 1),
                reservation("sooner", pickupStart = now + 1_000),
                reservation("new", ReservationStatus.CANCELLED, createdAt = 5),
                reservation("expired", pickupEnd = now - 1, createdAt = 3)
            ),
            now
        )

        assertEquals(listOf("sooner", "later"), split.active.map { it.reservation.id })
        assertEquals(listOf("new", "expired", "old"), split.past.map { it.reservation.id })
    }
}
