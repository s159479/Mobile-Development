package edu.ap.tobadtoostay.ui.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

/** An extra marker on the map, for example one per store. The [id] is given back when it is tapped. */
data class MapMarker(val id: String, val position: GeoPoint, val title: String)

/**
 * OSMDroid map for Compose.
 * The map only moves when [center] changes, a [marker] is drawn on its own position,
 * and [onTap] is called with the coordinates of a single tap.
 * [markers] are extra markers (one per store); [onMarkerClick] is called with the id of the tapped one.
 */
@Composable
fun OsmMap(
    center: GeoPoint,
    modifier: Modifier = Modifier,
    zoom: Double = 15.0,
    marker: GeoPoint? = null,
    markerTitle: String? = null,
    onTap: ((GeoPoint) -> Unit)? = null,
    markers: List<MapMarker> = emptyList(),
    onMarkerClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnTap = rememberUpdatedState(onTap)
    val currentOnMarkerClick = rememberUpdatedState(onMarkerClick)
    val extraMarkers = remember { mutableListOf<Marker>() }

    val mapView = remember {
        configureOsmdroid(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            controller.setCenter(center)
            // Dragging the map should move the map, not scroll the screen around it
            setOnTouchListener { view, _ ->
                view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    val tap = currentOnTap.value ?: return false
                    tap(p)
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            }))
        }
    }
    val pinMarker = remember { Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) } }
    val lastCenter = remember { arrayOf(center) }

    // Follow the lifecycle of the screen, as OSMDroid requires
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            if (center != lastCenter[0]) {
                lastCenter[0] = center
                map.controller.animateTo(center)
            }

            map.overlays.remove(pinMarker)
            if (marker != null) {
                pinMarker.position = marker
                pinMarker.title = markerTitle
                map.overlays.add(pinMarker)
            }
            // Redraw the extra markers (one per store)
            extraMarkers.forEach { map.overlays.remove(it) }
            extraMarkers.clear()
            markers.forEach { item ->
                val extra = Marker(map)
                extra.position = item.position
                extra.title = item.title
                extra.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                extra.setOnMarkerClickListener { clicked, _ ->
                    clicked.showInfoWindow()
                    currentOnMarkerClick.value?.invoke(item.id)
                    true
                }
                map.overlays.add(extra)
                extraMarkers.add(extra)
            }

            map.invalidate()
        }
    )
}

/** User-Agent and tile cache in the app's own cache folder (no storage permission needed). */
private fun configureOsmdroid(context: Context) {
    val config = Configuration.getInstance()
    config.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    config.userAgentValue = context.packageName
    val basePath = File(context.cacheDir, "osmdroid")
    config.osmdroidBasePath = basePath
    config.osmdroidTileCache = File(basePath, "tiles")
}
