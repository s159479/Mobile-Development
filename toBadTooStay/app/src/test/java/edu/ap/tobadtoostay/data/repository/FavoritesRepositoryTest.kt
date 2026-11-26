package edu.ap.tobadtoostay.data.repository

import edu.ap.tobadtoostay.data.local.dao.FavoriteStoreDao
import edu.ap.tobadtoostay.data.local.entity.FavoriteStore
import edu.ap.tobadtoostay.data.model.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the logic of the repository with a fake DAO in memory.
 * The real SQL (filter on customerId, composite key) is tested in FavoriteStoreDaoTest (androidTest).
 */
class FavoritesRepositoryTest {

    private class FakeFavoriteStoreDao : FavoriteStoreDao {
        val rows = MutableStateFlow<Map<Pair<String, String>, FavoriteStore>>(emptyMap())

        override fun observeForCustomer(customerId: String): Flow<List<FavoriteStore>> =
            rows.map { all ->
                all.values.filter { it.customerId == customerId }.sortedByDescending { it.addedAt }
            }

        override suspend fun upsert(store: FavoriteStore) {
            rows.value = rows.value + ((store.customerId to store.storeId) to store)
        }

        override suspend fun delete(customerId: String, storeId: String) {
            rows.value = rows.value - (customerId to storeId)
        }
    }

    private val repository = FavoritesRepository(FakeFavoriteStoreDao())

    private val bakery = Store(
        id = "store1",
        name = "Bakkerij Jan",
        street = "Kerkstraat 1",
        postalCode = "2000",
        city = "Antwerpen"
    )

    @Test
    fun setFavorite_true_savesCustomerNameAndAddress() = runBlocking {
        repository.setFavorite("anna", bakery, favorite = true)

        val saved = repository.observeAll("anna").first().single()
        assertEquals("anna", saved.customerId)
        assertEquals("store1", saved.storeId)
        assertEquals("Bakkerij Jan", saved.name)
        assertEquals("Kerkstraat 1, 2000 Antwerpen", saved.address)
    }

    @Test
    fun setFavorite_false_removesStore() = runBlocking {
        repository.setFavorite("anna", bakery, favorite = true)
        repository.setFavorite("anna", bakery, favorite = false)

        assertTrue(repository.observeAll("anna").first().isEmpty())
    }

    @Test
    fun setFavorite_sameStateTwice_keepsOneFavorite() = runBlocking {
        // a fast double tap sends the same state twice
        repository.setFavorite("anna", bakery, favorite = true)
        repository.setFavorite("anna", bakery, favorite = true)

        assertEquals(1, repository.observeAll("anna").first().size)
    }

    @Test
    fun restore_putsRemovedFavoriteBackWithItsOldTime() = runBlocking {
        repository.setFavorite("anna", bakery, favorite = true)
        val saved = repository.observeAll("anna").first().single()

        repository.remove(saved)
        repository.restore(saved)

        assertEquals(saved, repository.observeAll("anna").first().single())
    }
}
