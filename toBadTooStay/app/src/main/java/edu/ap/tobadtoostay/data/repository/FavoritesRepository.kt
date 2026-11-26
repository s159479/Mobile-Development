package edu.ap.tobadtoostay.data.repository

import edu.ap.tobadtoostay.data.local.dao.FavoriteStoreDao
import edu.ap.tobadtoostay.data.local.entity.FavoriteStore
import edu.ap.tobadtoostay.data.model.Store
import kotlinx.coroutines.flow.Flow

/**
 * Favorite stores of the customer (K12). Only saved on this phone, in Room.
 * Every row has the uid of the customer, so the favorites stay after logout
 * and another account on the same phone does not see them.
 */
class FavoritesRepository(private val dao: FavoriteStoreDao) {

    /** All favorites of the customer, the last added first. */
    fun observeAll(customerId: String): Flow<List<FavoriteStore>> = dao.observeForCustomer(customerId)

    /** Marks the store as favorite, or removes it when [favorite] is false. */
    suspend fun setFavorite(customerId: String, store: Store, favorite: Boolean) {
        if (favorite) {
            dao.upsert(
                FavoriteStore(
                    customerId = customerId,
                    storeId = store.id,
                    name = store.name,
                    address = store.address
                )
            )
        } else {
            dao.delete(customerId, store.id)
        }
    }

    suspend fun remove(favorite: FavoriteStore) {
        dao.delete(favorite.customerId, favorite.storeId)
    }

    /** Puts a removed favorite back, with its old [FavoriteStore.addedAt] so it keeps its place. */
    suspend fun restore(favorite: FavoriteStore) {
        dao.upsert(favorite)
    }
}
