package edu.ap.tobadtoostay.data.repository

import edu.ap.tobadtoostay.data.model.Bag
import edu.ap.tobadtoostay.data.model.MapPoint
import edu.ap.tobadtoostay.data.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyBagsRepositoryTest {

    private val now = 1_000_000L
    private val customer = MapPoint(51.2172, 4.4211)

    private val nearStore = Store(id = "near", name = "Bakker", latitude = 51.2180, longitude = 4.4200)
    private val farStore = Store(id = "far", name = "Supermarkt", latitude = 51.2300, longitude = 4.4300)

    private fun bag(id: String, storeId: String, left: Int = 3, pickupEnd: Long? = now + 60_000) =
        Bag(id = id, storeId = storeId, quantityTotal = 5, quantityLeft = left, pickupEnd = pickupEnd)

    @Test
    fun closestStoreComesFirst() {
        val result = NearbyBagsRepository.combine(
            customer,
            stores = listOf(farStore, nearStore),
            bags = listOf(bag("1", "far"), bag("2", "near")),
            now = now
        )

        assertEquals(listOf("2", "1"), result.map { it.bag.id })
        assertTrue(result[0].distanceMeters < result[1].distanceMeters)
        assertEquals("Bakker", result[0].store.name)
    }

    @Test
    fun soldOutBagsAreLeftOut() {
        val result = NearbyBagsRepository.combine(
            customer, listOf(nearStore), listOf(bag("1", "near", left = 0)), now
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun bagsAfterPickupTimeAreLeftOut() {
        val result = NearbyBagsRepository.combine(
            customer, listOf(nearStore), listOf(bag("1", "near", pickupEnd = now - 1)), now
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun bagsWithoutPickupEndAreKept() {
        val result = NearbyBagsRepository.combine(
            customer, listOf(nearStore), listOf(bag("1", "near", pickupEnd = null)), now
        )
        assertEquals(1, result.size)
    }

    @Test
    fun bagsOfUnknownStoresAreLeftOut() {
        val result = NearbyBagsRepository.combine(
            customer, listOf(nearStore), listOf(bag("1", "other")), now
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun sameStoreSortsOnPickupEnd() {
        val result = NearbyBagsRepository.combine(
            customer,
            listOf(nearStore),
            listOf(bag("late", "near", pickupEnd = now + 90_000), bag("soon", "near", pickupEnd = now + 10_000)),
            now
        )
        assertEquals(listOf("soon", "late"), result.map { it.bag.id })
    }
}
