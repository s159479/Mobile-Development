package edu.ap.tobadtoostay.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.ap.tobadtoostay.data.local.dao.FavoriteStoreDao
import edu.ap.tobadtoostay.data.local.entity.FavoriteStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the real Room SQL of the favorites, in a database that only lives in memory. Needs an emulator. */
@RunWith(AndroidJUnit4::class)
class FavoriteStoreDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: FavoriteStoreDao

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.favoriteStoreDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun favorite(customerId: String, storeId: String, addedAt: Long = 0L) =
        FavoriteStore(customerId, storeId, name = "Store $storeId", address = "Straat 1", addedAt = addedAt)

    @Test
    fun favoritesAreSeparatedPerCustomer() = runBlocking {
        // the same store for two customers gives two rows
        dao.upsert(favorite("anna", "store1"))
        dao.upsert(favorite("bert", "store1"))

        dao.delete("bert", "store1")

        assertEquals(listOf("store1"), dao.observeForCustomer("anna").first().map { it.storeId })
        assertTrue(dao.observeForCustomer("bert").first().isEmpty())
    }

    @Test
    fun upsertOfSameCustomerAndStoreReplacesTheRow() = runBlocking {
        dao.upsert(favorite("anna", "store1"))
        dao.upsert(favorite("anna", "store1").copy(name = "New name"))

        val stores = dao.observeForCustomer("anna").first()
        assertEquals(1, stores.size)
        assertEquals("New name", stores.single().name)
    }

    @Test
    fun lastAddedComesFirst() = runBlocking {
        dao.upsert(favorite("anna", "old", addedAt = 1L))
        dao.upsert(favorite("anna", "new", addedAt = 2L))

        assertEquals(listOf("new", "old"), dao.observeForCustomer("anna").first().map { it.storeId })
    }
}
