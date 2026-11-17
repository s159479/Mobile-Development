package edu.ap.tobadtoostay.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import edu.ap.tobadtoostay.data.model.Bag
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class BagRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val bags = db.collection("bags")

    /**
     * All bags of the given stores. Firestore allows at most 30 values in whereIn,
     * so the stores are asked in groups of 30 at the same time.
     * Filtering (still available, pickup time) happens in the app, so no extra index is needed.
     */
    suspend fun getBagsForStores(storeIds: Collection<String>): List<Bag> {
        if (storeIds.isEmpty()) return emptyList()

        return coroutineScope {
            storeIds.distinct()
                .chunked(MAX_WHERE_IN)
                .map { ids -> async { bags.whereIn(STORE_ID, ids).get().await() } }
                .awaitAll()
                .flatMap { snapshot -> snapshot.documents.map { it.toBag() } }
        }
    }

    /**
     * Same bags as [getBagsForStores], but as a Flow that sends a new list every time
     * something changes in Firestore (for example when someone reserves the last bag).
     */
    fun observeBagsForStores(storeIds: Collection<String>): Flow<List<Bag>> {
        if (storeIds.isEmpty()) return flowOf(emptyList())

        val flows = storeIds.distinct()
            .chunked(MAX_WHERE_IN)
            .map { ids ->
                bags.whereIn(STORE_ID, ids).snapshots().map { snapshot ->
                    snapshot.documents.map { it.toBag() }
                }
            }
        return combine(flows) { lists -> lists.toList().flatten() }
    }

    private fun DocumentSnapshot.toBag() = Bag(
        id = id,
        storeId = getString(STORE_ID).orEmpty(),
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        price = getDouble("price") ?: 0.0,
        quantityTotal = getLong("quantityTotal")?.toInt() ?: 0,
        quantityLeft = getLong("quantityLeft")?.toInt() ?: 0,
        pickupStart = getTimestamp("pickupStart")?.toDate()?.time,
        pickupEnd = getTimestamp("pickupEnd")?.toDate()?.time
    )

    private companion object {
        const val STORE_ID = "storeId"
        const val MAX_WHERE_IN = 30
    }
}
