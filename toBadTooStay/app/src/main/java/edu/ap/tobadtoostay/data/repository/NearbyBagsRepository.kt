package edu.ap.tobadtoostay.data.repository

import edu.ap.tobadtoostay.data.location.GeoMath
import edu.ap.tobadtoostay.data.model.Bag
import edu.ap.tobadtoostay.data.model.MapPoint
import edu.ap.tobadtoostay.data.model.NearbyBag
import edu.ap.tobadtoostay.data.model.Store

/** Bags that can still be reserved in stores near the customer, the closest first. */
class NearbyBagsRepository(
    private val storeRepository: StoreRepository = StoreRepository(),
    private val bagRepository: BagRepository = BagRepository()
) {

    suspend fun getNearbyBags(
        center: MapPoint,
        radiusMeters: Double = DEFAULT_RADIUS_METERS
    ): List<NearbyBag> {
        val stores = storeRepository.getStoresNear(center, radiusMeters)
        val bags = bagRepository.getBagsForStores(stores.map { it.id })
        return combine(center, stores, bags, now = System.currentTimeMillis())
    }

    companion object {
        const val DEFAULT_RADIUS_METERS = 5_000.0

        /**
         * Links every available bag to its store and sorts on distance (then on pickup end).
         * Separate from Firestore so it can be unit tested.
         */
        fun combine(center: MapPoint, stores: List<Store>, bags: List<Bag>, now: Long): List<NearbyBag> {
            val storesById = stores.associateBy { it.id }
            return bags
                .filter { it.isAvailable(now) }
                .mapNotNull { bag ->
                    val store = storesById[bag.storeId] ?: return@mapNotNull null
                    NearbyBag(
                        bag = bag,
                        store = store,
                        distanceMeters = GeoMath.distanceMeters(center, MapPoint(store.latitude, store.longitude))
                    )
                }
                .sortedWith(compareBy({ it.distanceMeters }, { it.bag.pickupEnd ?: Long.MAX_VALUE }))
        }
    }
}
