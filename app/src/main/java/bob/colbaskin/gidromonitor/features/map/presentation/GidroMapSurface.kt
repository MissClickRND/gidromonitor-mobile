package bob.colbaskin.gidromonitor.features.map.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import bob.colbaskin.gidromonitor.features.map.domain.model.BaseMapStyle
import bob.colbaskin.gidromonitor.design_system.theme.GidroTheme
import bob.colbaskin.gidromonitor.features.analysis.domain.model.ObservationLayer
import bob.colbaskin.gidromonitor.features.analysis.domain.model.ObservationLayerType
import bob.colbaskin.gidromonitor.features.analysis.domain.model.GeoPoint
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import bob.colbaskin.gidromonitor.R
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.geometry.LatLngQuad
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.ImageSource

data class RasterOverlay(
    val assetPath: String? = null,
    val filePath: String? = null,
    val north: Double,
    val south: Double,
    val west: Double,
    val east: Double
)

@Composable
fun GidroMapSurface(
    mapStyle: BaseMapStyle,
    observationLayers: List<ObservationLayer> = emptyList(),
    visibleLayerTypes: Set<ObservationLayerType> = emptySet(),
    layerOpacity: Map<ObservationLayerType, Float> = emptyMap(),
    draftPoints: List<GeoPoint> = emptyList(),
    areaOutlinePoints: List<GeoPoint> = emptyList(),
    rasterOverlay: RasterOverlay? = null,
    cameraFocusPoints: List<GeoPoint> = emptyList(),
    cameraFocusPadding: Dp = 48.dp,
    initialZoom: Double = 10.3,
    onMapTap: ((GeoPoint) -> Unit)? = null,
    onMapStyleLoaded: (() -> Unit)? = null,
    onMapReady: ((MapLibreMap) -> Unit)? = null,
    onCameraChanged: ((MapLibreMap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val pointIcon = remember(context) {
        val size = (16 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        ContextCompat.getDrawable(context, R.drawable.ic_map_point)?.apply {
            setBounds(0, 0, size, size)
            draw(Canvas(bitmap))
        }
        IconFactory.getInstance(context).fromBitmap(bitmap)
    }
    val currentStyle by rememberUpdatedState(mapStyle)
    val currentMapTap by rememberUpdatedState(onMapTap)
    val currentMapStyleLoaded by rememberUpdatedState(onMapStyleLoaded)
    val currentMapReady by rememberUpdatedState(onMapReady)
    val currentCameraChanged by rememberUpdatedState(onCameraChanged)
    val mapSelectionColor = GidroTheme.colors.mapSelection.toArgb()
    val rasterBitmap = remember(context, rasterOverlay?.assetPath, rasterOverlay?.filePath) {
        rasterOverlay?.let { overlay ->
            overlay.filePath?.let(BitmapFactory::decodeFile)
                ?: overlay.assetPath?.let { path -> context.assets.open(path).use(BitmapFactory::decodeStream) }
        }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val mapView = remember {
        MapView(context).apply {
            getMapAsync { loadedMap ->
                loadedMap.setMinZoomPreference(2.0)
                loadedMap.setMaxZoomPreference(22.0)
                loadedMap.uiSettings.apply {
                    setLogoEnabled(false)
                    setAttributionEnabled(false)
                }
                loadedMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(50.42, 127.54))
                    .zoom(initialZoom)
                    .build()
                map = loadedMap
                currentMapReady?.invoke(loadedMap)
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(map, mapStyle, rasterOverlay, rasterBitmap) {
        map?.setStyle(currentStyle.styleUrl()) { style ->
            val overlay = rasterOverlay
            if (overlay != null && rasterBitmap != null) {
                val sourceId = "offline-cog-raster-source"
                val layerId = "offline-cog-raster-layer"
                val coordinates = LatLngQuad(
                    LatLng(overlay.north, overlay.west),
                    LatLng(overlay.north, overlay.east),
                    LatLng(overlay.south, overlay.east),
                    LatLng(overlay.south, overlay.west)
                )
                style.addSource(ImageSource(sourceId, coordinates, rasterBitmap))
                style.addLayer(RasterLayer(layerId, sourceId))
            }
            currentMapStyleLoaded?.invoke()
        }
    }

    LaunchedEffect(map, cameraFocusPoints, cameraFocusPadding) {
        if (cameraFocusPoints.size >= 2) {
            val bounds = LatLngBounds.Builder().apply {
                cameraFocusPoints.forEach { include(LatLng(it.latitude, it.longitude)) }
            }.build()
            mapView.post {
                map?.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        bounds,
                        (cameraFocusPadding.value * context.resources.displayMetrics.density).toInt()
                    )
                )
            }
        }
    }

    DisposableEffect(mapView) {
        val listener = MapView.OnDidFinishLoadingStyleListener {
            currentMapStyleLoaded?.invoke()
        }
        mapView.addOnDidFinishLoadingStyleListener(listener)
        onDispose { mapView.removeOnDidFinishLoadingStyleListener(listener) }
    }

    DisposableEffect(mapView, map) {
        val listener = MapView.OnCameraDidChangeListener {
            map?.let { currentCameraChanged?.invoke(it) }
        }
        mapView.addOnCameraDidChangeListener(listener)
        map?.let { currentCameraChanged?.invoke(it) }
        onDispose { mapView.removeOnCameraDidChangeListener(listener) }
    }

    LaunchedEffect(map, observationLayers, visibleLayerTypes, layerOpacity, draftPoints, areaOutlinePoints, mapSelectionColor) {
        map?.apply {
            clear()
            observationLayers
                .filter { it.type in visibleLayerTypes }
                .forEach { layer ->
                    addPolygon(
                        PolygonOptions()
                            .addAll(layer.geometry.map { LatLng(it.latitude, it.longitude) })
                            .fillColor(Color.parseColor(layer.colorHex))
                            .strokeColor(Color.parseColor(layer.colorHex))
                            .alpha(layerOpacity[layer.type] ?: 0.65f)
                    )
                }
            if (areaOutlinePoints.size >= 3) {
                val outline = areaOutlinePoints.map { LatLng(it.latitude, it.longitude) }
                addPolygon(
                    PolygonOptions()
                        .addAll(outline)
                        .fillColor(mapSelectionColor)
                        .strokeColor(Color.parseColor("#FFFFFF"))
                        .alpha(0.28f)
                )
                addPolyline(
                    PolylineOptions()
                        .addAll((outline + outline.first()))
                        .color(mapSelectionColor)
                        .width(4f)
                )
            }
            if (draftPoints.size >= 3) {
                addPolygon(
                    PolygonOptions()
                        .addAll(draftPoints.map { LatLng(it.latitude, it.longitude) })
                        .fillColor(mapSelectionColor)
                        .strokeColor(Color.parseColor("#FFFFFF"))
                        .alpha(0.55f)
                )
            }
            if (draftPoints.isNotEmpty()) {
                draftPoints.forEach { point ->
                    addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)).icon(pointIcon))
                }
            }
            if (draftPoints.size >= 2) {
                val linePoints = if (draftPoints.size >= 3) draftPoints + draftPoints.first() else draftPoints
                addPolyline(
                    PolylineOptions()
                        .addAll(linePoints.map { LatLng(it.latitude, it.longitude) })
                        .color(mapSelectionColor)
                        .width(5f)
                )
            }
        }
    }

    DisposableEffect(map) {
        map?.let { loadedMap ->
            val listener = MapLibreMap.OnMapClickListener { point ->
                currentMapTap?.invoke(GeoPoint(point.latitude, point.longitude))
                currentMapTap != null
            }
            loadedMap.addOnMapClickListener(listener)
            onDispose { loadedMap.removeOnMapClickListener(listener) }
        } ?: onDispose { }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}
