package edu.ap.tobadtoostay.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import edu.ap.tobadtoostay.data.location.LocationAccess
import edu.ap.tobadtoostay.data.location.locationAccess
import edu.ap.tobadtoostay.data.model.MapPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Current location of the device, with the LocationManager of Android (no Google Play services needed).
 * The caller must ask the location permission first (see ui/location/LocationPermission.kt).
 */
class LocationRepository(context: Context) {

    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /** True if the user allowed at least the approximate location. */
    fun hasPermission(): Boolean = appContext.locationAccess() != LocationAccess.DENIED

    /** False if location is turned off in the device location setting. */
    fun isLocationEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(locationManager)

    /**
     * A recent known location is returned right away. Otherwise we ask a fresh one, and if none
     * arrives within [TIMEOUT_MS] (indoors, emulator...) we fall back to a known location that is
     * not too old. Null when there is no usable location.
     *
     * @throws SecurityException when the location permission is not granted
     */
    @SuppressLint("MissingPermission") // checked by hasPermission()
    suspend fun getCurrentLocation(): MapPoint? {
        if (!hasPermission()) throw SecurityException("Location permission not granted")

        lastKnownLocation(maxAgeMs = RECENT_MS)?.let { return it.toMapPoint() }

        val fresh = bestProvider()?.let { provider ->
            withTimeoutOrNull(TIMEOUT_MS) { requestSingleLocation(provider) }
        }
        return (fresh ?: lastKnownLocation(maxAgeMs = MAX_AGE_MS))?.toMapPoint()
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocation(provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                signal,
                ContextCompat.getMainExecutor(appContext)
            ) { location -> continuation.resume(location) }
        }

    /**
     * Fused (Android 12+) combines GPS, wifi and cell towers. Before Android 12 GPS goes first
     * when the precise permission is granted (the emulator only sends GPS locations),
     * otherwise the network, which works with the approximate permission.
     */
    private fun bestProvider(): String? {
        val enabled = locationManager.getProviders(true)
        val precise = appContext.locationAccess() == LocationAccess.PRECISE

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                LocationManager.FUSED_PROVIDER in enabled -> LocationManager.FUSED_PROVIDER
            precise && LocationManager.GPS_PROVIDER in enabled -> LocationManager.GPS_PROVIDER
            LocationManager.NETWORK_PROVIDER in enabled -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    /** The most recent location any provider still remembers, if it is not older than [maxAgeMs]. */
    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(maxAgeMs: Long): Location? =
        locationManager.getProviders(true)
            .mapNotNull { provider ->
                try {
                    locationManager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null // e.g. GPS with only the approximate permission
                }
            }
            .filter { it.ageMs() <= maxAgeMs }
            .maxByOrNull { it.elapsedRealtimeNanos }

    /** Age based on the time since boot, the clock time of a location can be wrong. */
    private fun Location.ageMs(): Long =
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000

    private fun Location.toMapPoint() = MapPoint(latitude, longitude)

    companion object {
        private const val TIMEOUT_MS = 10_000L

        /** Young enough to use without asking a new location. */
        private const val RECENT_MS = 2 * 60_000L

        /** Older locations are not used: the user may be somewhere else by now. */
        private const val MAX_AGE_MS = 30 * 60_000L
    }
}
