package bob.colbaskin.gidromonitor.features.comparison.presentation

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import bob.colbaskin.gidromonitor.common.UiState
import bob.colbaskin.gidromonitor.common.ui.GidroLoader
import bob.colbaskin.gidromonitor.design_system.theme.GidroFonts
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisRasterFile
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisArea
import bob.colbaskin.gidromonitor.features.analysis.domain.model.GeoPoint
import bob.colbaskin.gidromonitor.features.analysis.domain.model.ObservationLayer
import bob.colbaskin.gidromonitor.features.analysis.domain.model.ObservationLayerType
import bob.colbaskin.gidromonitor.features.map.domain.model.BaseMapStyle
import bob.colbaskin.gidromonitor.features.map.presentation.GidroMapSurface
import bob.colbaskin.gidromonitor.features.map.data.DecodedCogRaster
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun ComparisonRoute(
    analysisId: String,
    onBack: () -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel()
) {
    LaunchedEffect(analysisId) { viewModel.load(analysisId) }
    BackHandler(onBack = onBack)
    val state = viewModel.state
    val result = (state.result as? UiState.Success)?.data
    FullScreenComparison(
            result = result,
            splitPosition = state.splitPosition,
            enabledLayers = state.enabledLayers,
            rasterFiles = (state.rasterFiles as? UiState.Success)?.data.orEmpty(),
            enabledRasterFiles = state.enabledRasterFiles,
            decodedRasters = state.decodedRasters.values.filter { it.fileName in state.enabledRasterFiles },
            enabledCogChannels = state.enabledCogChannels,
            cogOverlayOpacity = state.cogOverlayOpacity,
            isRasterLoading = state.isDecodingRaster || state.rasterFiles is UiState.Loading,
            rasterLoadError = state.rasterLoadError,
            mapStyle = state.mapStyle,
            onSetSplit = viewModel::setSplitPosition,
            onToggleLayer = viewModel::selectLayer,
            onToggleRasterFile = viewModel::toggleRasterFile,
            onToggleCogChannel = viewModel::toggleCogChannel,
            onRetryRasterFiles = viewModel::retryRasterFiles,
            onSetCogOverlayOpacity = viewModel::setCogOverlayOpacity,
            onToggleBaseMap = viewModel::toggleBaseMap,
            onBack = onBack
        )
}

@Composable
private fun FullScreenComparison(
    result: AnalysisResult?,
    splitPosition: Float,
    enabledLayers: Set<ObservationLayerType>,
    rasterFiles: List<AnalysisRasterFile>,
    enabledRasterFiles: Set<String>,
    decodedRasters: List<DecodedCogRaster>,
    enabledCogChannels: Set<String>,
    cogOverlayOpacity: Float,
    isRasterLoading: Boolean,
    rasterLoadError: String?,
    mapStyle: BaseMapStyle,
    onSetSplit: (Float) -> Unit,
    onToggleLayer: (ObservationLayerType) -> Unit,
    onToggleRasterFile: (AnalysisRasterFile) -> Unit,
    onToggleCogChannel: (String) -> Unit,
    onRetryRasterFiles: () -> Unit,
    onSetCogOverlayOpacity: (Float) -> Unit,
    onToggleBaseMap: () -> Unit,
    onBack: () -> Unit
) {
    var showObservationLayers by remember { mutableStateOf(true) }
    var showCogOpacityControl by remember { mutableStateOf(false) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var cameraRevision by remember { mutableStateOf(0) }
    var dragSplitPosition by remember { mutableStateOf<Float?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val dividerPosition = (dragSplitPosition ?: splitPosition).coerceIn(0.08f, 0.92f)
        val currentDividerPosition by rememberUpdatedState(dividerPosition)
        val dividerOffset = maxWidth * dividerPosition
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }

        val analysisFocusPoints = result?.layers?.flatMap { it.geometry }
            .orEmpty()
            .ifEmpty { result?.request?.area?.boundaryPoints().orEmpty() }
        val rasterFocusPoints = decodedRasters.flatMap { raster ->
            listOf(
                GeoPoint(raster.north, raster.west),
                GeoPoint(raster.north, raster.east),
                GeoPoint(raster.south, raster.east),
                GeoPoint(raster.south, raster.west)
            )
        }
        GidroMapSurface(
            mapStyle = mapStyle,
            cameraFocusPoints = analysisFocusPoints.ifEmpty { rasterFocusPoints },
            onMapReady = {
                mapLibreMap = it
                cameraRevision++
            },
            onCameraChanged = {
                mapLibreMap = it
                cameraRevision++
            },
            modifier = Modifier.fillMaxSize()
        )
        RemoteRasterComparisonOverlay(
            rasters = decodedRasters,
            enabledChannelIds = enabledCogChannels,
            opacity = cogOverlayOpacity,
            splitPosition = dividerPosition,
            mapLibreMap = mapLibreMap,
            cameraRevision = cameraRevision,
            modifier = Modifier.fillMaxSize().clipToBounds()
        )
        if (result?.hasComputedData == true) {
            ComparisonLayerOverlay(
                layers = result.layers,
                enabledLayers = enabledLayers,
                splitPosition = dividerPosition,
                mapLibreMap = mapLibreMap,
                cameraRevision = cameraRevision,
                modifier = Modifier.fillMaxSize().clipToBounds()
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), RoundedCornerShape(12.dp))
                .zIndex(2f)
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Вернуться к аналитике",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        result?.let { analysis ->
            DateBadge(
                title = "Дата ДО",
                date = analysis.request.dateBefore,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 12.dp, top = 68.dp).zIndex(2f)
            )
            DateBadge(
                title = "Дата ПОСЛЕ",
                date = analysis.request.dateAfter,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 12.dp, top = 68.dp).zIndex(2f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = dividerOffset - 28.dp)
                .fillMaxHeight()
                .width(56.dp)
                .zIndex(1f)
                .pointerInput(maxWidth) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragSplitPosition = currentDividerPosition },
                        onDragCancel = { dragSplitPosition = null },
                        onDragEnd = {
                            dragSplitPosition?.let(onSetSplit)
                            dragSplitPosition = null
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val basePosition = dragSplitPosition ?: currentDividerPosition
                        dragSplitPosition = (basePosition + dragAmount / widthPx).coerceIn(0.08f, 0.92f)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.surface)
            )
            Card(
                modifier = Modifier.align(Alignment.Center).size(44.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    Icons.Outlined.SwapHoriz,
                    contentDescription = "Переместить разделитель",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 72.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (decodedRasters.any { it.channels.isNotEmpty() }) {
                CogOverlayOpacityPanel(
                    opacity = cogOverlayOpacity,
                    expanded = showCogOpacityControl,
                    onExpandedChange = { showCogOpacityControl = it },
                    onOpacityChange = onSetCogOverlayOpacity,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            ObservationLayersPanel(
                layers = result?.layers.orEmpty(),
                enabledLayers = enabledLayers,
                rasterFiles = rasterFiles,
                enabledRasterFiles = enabledRasterFiles,
                cogChannels = decodedRasters.flatMap { it.channels },
                enabledCogChannels = enabledCogChannels,
                isRasterLoading = isRasterLoading,
                rasterLoadError = rasterLoadError,
                expanded = showObservationLayers,
                onExpandedChange = { showObservationLayers = it },
                onToggleLayer = onToggleLayer,
                onToggleRasterFile = onToggleRasterFile,
                onToggleCogChannel = onToggleCogChannel,
                onRetryRasterFiles = onRetryRasterFiles
            )
        }
        Card(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp).size(48.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            IconButton(onClick = onToggleBaseMap) {
                Icon(Icons.Outlined.Layers, "Сменить подложку: ${mapStyle.label}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RemoteRasterComparisonOverlay(
    rasters: List<DecodedCogRaster>,
    enabledChannelIds: Set<String>,
    opacity: Float,
    splitPosition: Float,
    mapLibreMap: MapLibreMap?,
    cameraRevision: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val map = mapLibreMap ?: return@Canvas
        cameraRevision
        val splitX = size.width * splitPosition
        clipRect(left = splitX) {
            rasters.forEach { raster ->
                val topLeft = map.projection.toScreenLocation(LatLng(raster.north, raster.west))
                val topRight = map.projection.toScreenLocation(LatLng(raster.north, raster.east))
                val bottomLeft = map.projection.toScreenLocation(LatLng(raster.south, raster.west))
                val topEdgeWidth = hypot(
                    (topRight.x - topLeft.x).toDouble(),
                    (topRight.y - topLeft.y).toDouble()
                ).toFloat()
                val leftEdgeHeight = hypot(
                    (bottomLeft.x - topLeft.x).toDouble(),
                    (bottomLeft.y - topLeft.y).toDouble()
                ).toFloat()
                if (topEdgeWidth > 0f && leftEdgeHeight > 0f) {
                    raster.channels
                        .filter { it.id in enabledChannelIds }
                        .forEach { channel ->
                            val bearingDegrees = Math.toDegrees(
                                atan2(
                                    (topRight.y - topLeft.y).toDouble(),
                                    (topRight.x - topLeft.x).toDouble()
                                )
                            ).toFloat()
                            withTransform({
                                translate(topLeft.x, topLeft.y)
                                rotate(bearingDegrees, pivot = Offset.Zero)
                                scale(
                                    topEdgeWidth / channel.bitmap.width,
                                    leftEdgeHeight / channel.bitmap.height,
                                    pivot = Offset.Zero
                                )
                            }) {
                                drawImage(channel.bitmap.asImageBitmap(), alpha = opacity)
                            }
                        }
                }
            }
        }
    }
}

private fun AnalysisArea.boundaryPoints(): List<GeoPoint> = when (this) {
    is AnalysisArea.Polygon -> points
    is AnalysisArea.BoundingBox -> listOf(
        southWest,
        GeoPoint(southWest.latitude, northEast.longitude),
        northEast,
        GeoPoint(northEast.latitude, southWest.longitude)
    )
}

@Composable
private fun ComparisonLayerOverlay(
    layers: List<ObservationLayer>,
    enabledLayers: Set<ObservationLayerType>,
    splitPosition: Float,
    mapLibreMap: MapLibreMap?,
    cameraRevision: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val map = mapLibreMap ?: return@Canvas
        cameraRevision
        val selectedType = enabledLayers.firstOrNull() ?: return@Canvas
        val leftLayers = when (selectedType) {
            ObservationLayerType.WATER_BEFORE,
            ObservationLayerType.WATER_AFTER -> layers.filter { it.type == ObservationLayerType.WATER_BEFORE }
            ObservationLayerType.PERMANENT_WATER -> layers.filter { it.type == ObservationLayerType.PERMANENT_WATER }
            else -> emptyList()
        }
        val rightLayers = when (selectedType) {
            ObservationLayerType.WATER_BEFORE,
            ObservationLayerType.WATER_AFTER -> layers.filter { it.type == ObservationLayerType.WATER_AFTER }
            else -> layers.filter { it.type == selectedType }
        }
        val splitX = size.width * splitPosition
        clipRect(right = splitX) {
            leftLayers.forEach { drawObservationLayer(it, map) }
        }
        clipRect(left = splitX) {
            rightLayers.forEach { drawObservationLayer(it, map) }
        }
    }
}

private fun DrawScope.drawObservationLayer(
    layer: ObservationLayer,
    mapLibreMap: MapLibreMap
) {
    if (layer.geometry.size < 3) return
    val path = Path()
    layer.geometry.forEachIndexed { index, point ->
        val screenPoint = mapLibreMap.projection.toScreenLocation(LatLng(point.latitude, point.longitude))
        if (index == 0) path.moveTo(screenPoint.x, screenPoint.y) else path.lineTo(screenPoint.x, screenPoint.y)
    }
    path.close()
    val color = Color(AndroidColor.parseColor(layer.colorHex))
    drawPath(path = path, color = color.copy(alpha = 0.62f))
    drawPath(path = path, color = color, style = Stroke(width = 3.dp.toPx()))
}

@Composable
private fun ObservationLayersPanel(
    layers: List<ObservationLayer>,
    enabledLayers: Set<ObservationLayerType>,
    rasterFiles: List<AnalysisRasterFile>,
    enabledRasterFiles: Set<String>,
    cogChannels: List<bob.colbaskin.gidromonitor.features.map.data.DecodedCogChannel>,
    enabledCogChannels: Set<String>,
    isRasterLoading: Boolean,
    rasterLoadError: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleLayer: (ObservationLayerType) -> Unit,
    onToggleRasterFile: (AnalysisRasterFile) -> Unit,
    onToggleCogChannel: (String) -> Unit,
    onRetryRasterFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collapsedWidth by animateDpAsState(
        targetValue = if (isRasterLoading) 228.dp else 196.dp,
        animationSpec = tween(180),
        label = "layersPanelWidth"
    )
    Card(
        modifier = modifier.width(if (expanded) 270.dp else collapsedWidth),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onExpandedChange(!expanded) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Слои наблюдения",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                AnimatedVisibility(
                    visible = isRasterLoading,
                    enter = fadeIn(tween(160)) + expandHorizontally(tween(160)),
                    exit = fadeOut(tween(120)) + shrinkHorizontally(tween(120))
                ) {
                    GidroLoader(modifier = Modifier.padding(start = 8.dp), size = 18.dp)
                }
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null)
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(220)) + fadeIn(tween(160)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
            ) {
                Column {
                layers.forEach { layer ->
                    ObservationLayerRow(
                        layer = layer,
                        checked = layer.type in enabledLayers,
                        onToggle = { onToggleLayer(layer.type) }
                    )
                }
                if (layers.isNotEmpty() && (cogChannels.isNotEmpty() || rasterFiles.isNotEmpty())) HorizontalDivider()
                if (cogChannels.isNotEmpty()) {
                    cogChannels.forEachIndexed { index, channel ->
                        AnimatedCogChannelRow(
                            channel = channel,
                            checked = channel.id in enabledCogChannels,
                            delayMillis = index * 45,
                            onToggle = { onToggleCogChannel(channel.id) }
                        )
                    }
                } else if (isRasterLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GidroLoader(size = 20.dp)
                        Text("Читаем каналы COG…", modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (rasterLoadError != null) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(rasterLoadError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onRetryRasterFiles, modifier = Modifier.padding(top = 10.dp)) { Text("Повторить") }
                    }
                } else {
                    Text(
                        "Слои COG недоступны",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun CogOverlayOpacityPanel(
    opacity: Float,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.size(48.dp).clickable { onExpandedChange(!expanded) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.width(224.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Прозрачность каналов", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.10f..1f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ObservationLayerRow(layer: ObservationLayer, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Layers,
            contentDescription = layer.title,
            tint = Color(AndroidColor.parseColor(layer.colorHex)),
            modifier = Modifier.size(20.dp)
        )
        Text(layer.title, modifier = Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.bodyMedium)
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun RasterFileRow(file: AnalysisRasterFile, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Layers,
            contentDescription = file.fileName,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(file.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text("COG · ${file.sizeBytes / (1024 * 1024)} МБ", style = MaterialTheme.typography.labelSmall)
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun CogChannelRow(
    channel: bob.colbaskin.gidromonitor.features.map.data.DecodedCogChannel,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Layers,
            contentDescription = channel.title,
            tint = Color(channel.color),
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(channel.title, style = MaterialTheme.typography.bodyMedium)
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun AnimatedCogChannelRow(
    channel: bob.colbaskin.gidromonitor.features.map.data.DecodedCogChannel,
    checked: Boolean,
    delayMillis: Int,
    onToggle: () -> Unit
) {
    val visibility = remember(channel.id) {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = visibility,
        enter = expandVertically(tween(180, delayMillis = delayMillis)) + fadeIn(tween(180, delayMillis = delayMillis)),
        exit = shrinkVertically(tween(120)) + fadeOut(tween(100))
    ) {
        CogChannelRow(channel, checked, onToggle)
    }
}

@Composable
private fun DateBadge(title: String, date: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = GidroFonts.Metric, fontSize = 12.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(date, style = MaterialTheme.typography.labelLarge.copy(fontFamily = GidroFonts.Metric, fontSize = 12.sp, lineHeight = 18.sp))
        }
    }
}
