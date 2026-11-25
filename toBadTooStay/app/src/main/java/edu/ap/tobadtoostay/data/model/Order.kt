package edu.ap.tobadtoostay.data.model

import edu.ap.tobadtoostay.data.local.entity.CachedReservation

/** Status shown on the orders screen. EXPIRED is a reservation that was never collected in time. */
enum class OrderStatus { RESERVED, COLLECTED, CANCELLED, EXPIRED }

/** A reservation of the customer, as shown on the orders screen (K10). */
data class Order(val reservation: CachedReservation, val status: OrderStatus) {
    val isActive: Boolean get() = status == OrderStatus.RESERVED
}

/** Active orders (still to collect) and past orders. */
data class SplitOrders(val active: List<Order>, val past: List<Order>)

object Orders {

    /**
     * A reservation is expired once its pickup window has ended (pickupEnd <= now).
     * Without a known end (pickupEnd = 0) it stays active, because we can't tell.
     */
    fun statusOf(reservation: CachedReservation, now: Long): OrderStatus = when (reservation.status) {
        ReservationStatus.COLLECTED -> OrderStatus.COLLECTED
        ReservationStatus.CANCELLED -> OrderStatus.CANCELLED
        else -> if (reservation.pickupEnd in 1..now) OrderStatus.EXPIRED else OrderStatus.RESERVED
    }

    /** Active orders: first pickup first. Past orders: newest first. */
    fun split(reservations: List<CachedReservation>, now: Long): SplitOrders {
        val orders = reservations.map { Order(it, statusOf(it, now)) }
        val (active, past) = orders.partition { it.isActive }
        return SplitOrders(
            active = active.sortedBy { it.reservation.pickupStart },
            past = past.sortedByDescending { it.reservation.createdAt }
        )
    }
}
