package edu.ap.tobadtoostay.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** "customer" or "merchant" from users/{uid}, null when not logged in or the profile is missing. */
    suspend fun getMyRole(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("users").document(uid).get().await().getString("role")
    }

    companion object {
        const val ROLE_CUSTOMER = "customer"
    }
}
