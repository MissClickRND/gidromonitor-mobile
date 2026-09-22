package bob.colbaskin.gidromonitor.features.report.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import bob.colbaskin.gidromonitor.common.UiState
import bob.colbaskin.gidromonitor.common.ui.GidroLoader
import bob.colbaskin.gidromonitor.common.ui.PullRefreshContainer
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisArea
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisStatus
import bob.colbaskin.gidromonitor.features.analysis.domain.model.GeoPoint
import bob.colbaskin.gidromonitor.features.analysis.domain.model.ObservationLayerType
import bob.colbaskin.gidromonitor.features.map.domain.model.BaseMapStyle
import bob.colbaskin.gidromonitor.features.map.presentation.GidroMapSurface
import bob.colbaskin.gidromonitor.features.map.presentation.RasterOverlay
import bob.colbaskin.gidromonitor.R
import bob.colbaskin.gidromonitor.design_system.theme.GidroTextStyles
import bob.colbaskin.gidromonitor.design_system.theme.GidroTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

@Composable
fun AnalyticsDetailsRoute(
    analysisId: String,
    onBack: () -> Unit,
    onNewAnalysis: () -> Unit,
    onOpenComparison: (String) -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val geoJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/geo+json")
    ) { uri ->
        val content = (viewModel.state.geoJson as? UiState.Success<String>)?.data ?: return@rememberLauncherForActivityResult
        uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(content) } }
        viewModel.consumeExport()
    }
    val reportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val result = (viewModel.state.result as? UiState.Success<AnalysisResult>)?.data ?: return@rememberLauncherForActivityResult
        uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(machineReportJson(result)) } }
    }
    LaunchedEffect(analysisId) { viewModel.load(analysisId) }
    LaunchedEffect(viewModel.state.geoJson) {
        if (viewModel.state.geoJson is UiState.Success) geoJsonLauncher.launch("$analysisId.geojson")
    }
    val refresh = { viewModel.refresh(analysisId) }
    when (val resultState = viewModel.state.result) {
        UiState.Loading -> LoadingScreen("Загрузка территории…")
        is UiState.Error -> PullRefreshContainer(
            isRefreshing = viewModel.state.isRefreshing,
            onRefresh = refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            ErrorScreen(onRetry = refresh)
        }
        is UiState.Success -> PullRefreshContainer(
            isRefreshing = viewModel.state.isRefreshing,
            onRefresh = refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            AnalyticsDetailsScreen(
                result = resultState.data,
                occurrencePreview = viewModel.state.occurrencePreview,
                exportError = (viewModel.state.geoJson as? UiState.Error)?.text,
                onBack = onBack,
                onNewAnalysis = onNewAnalysis,
                onOpenComparison = onOpenComparison,
                onExportGeoJson = { viewModel.prepareGeoJson(resultState.data.id) },
                onExportReport = { reportLauncher.launch("${resultState.data.id}_report.json") }
            )
        }
    }
}

@Composable
private fun LoadingScreen(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GidroLoader(size = 52.dp)
            Text(text, modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ErrorScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Не удалось открыть территорию", style = MaterialTheme.typography.titleLarge)
        Text("Проверьте подключение к интернету и попробуйте ещё раз.", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 18.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Повторить запрос")
        }
        Text("Также можно потянуть экран вниз для обновления.", modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AnalyticsDetailsScreen(
    result: AnalysisResult,
    occurrencePreview: bob.colbaskin.gidromonitor.features.map.data.CachedCogPreview?,
    exportError: String?,
    onBack: () -> Unit,
    onNewAnalysis: () -> Unit,
    onOpenComparison: (String) -> Unit,
    onExportGeoJson: () -> Unit,
    onExportReport: () -> Unit
) {
    val sheetDragDistance = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxSize()) {
        GidroMapSurface(
            mapStyle = BaseMapStyle.SATELLITE,
            observationLayers = if (occurrencePreview == null) result.layers else emptyList(),
            visibleLayerTypes = if (occurrencePreview == null) setOf(ObservationLayerType.WATER_BEFORE, ObservationLayerType.WATER_AFTER, ObservationLayerType.FLOOD_GAIN) else emptySet(),
            areaOutlinePoints = if (occurrencePreview == null) result.request.area.boundaryPoints() else emptyList(),
            rasterOverlay = occurrencePreview?.let { RasterOverlay(filePath = it.filePath, north = it.north, south = it.south, west = it.west, east = it.east) },
            cameraFocusPoints = result.request.area.boundaryPoints(),
            modifier = Modifier.fillMaxSize().clickable { onOpenComparison(result.id) }
        )
        ReportHeader(onBack)
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)
            ) {
                IconButton(
                    onClick = { onOpenComparison(result.id) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp)
                        .size(40.dp)
                        .pointerInput(result.id) {
                            detectVerticalDragGestures(
                                onDragStart = { sheetDragDistance.floatValue = 0f },
                                onVerticalDrag = { _, dragAmount -> sheetDragDistance.floatValue += dragAmount },
                                onDragEnd = {
                                    if (sheetDragDistance.floatValue > 24f) onOpenComparison(result.id)
                                }
                            )
                        }
                ) {
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Закрыть детали и открыть карту сравнения",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Территория", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleMedium)
                Text(result.request.title, modifier = Modifier.padding(top = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${result.request.dateBefore} — ${result.request.dateAfter}",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (result.hasComputedData) MetricCards(result) else RemoteStatusCard(result.status)
                exportError?.let { Text("Не удалось подготовить файл для выгрузки. Попробуйте ещё раз.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
                Text("Экспорт данных", modifier = Modifier.padding(top = 20.dp), style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportButton("JSON", onExportReport, Modifier.weight(1f), primary = true)
                    ExportButton("GeoJSON", onExportGeoJson, Modifier.weight(1f), primary = false)
                }
                Button(
                    onClick = onNewAnalysis,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Новый анализ") }
            }
        }
    }
}

@Composable
private fun RemoteStatusCard(status: AnalysisStatus) {
    val (title, description) = when (status) {
        AnalysisStatus.PROCESSING -> "Территория обрабатывается" to "Карта, даты и контур сохранены на сервере. Расчётные показатели и слои появятся после завершения обработки."
        AnalysisStatus.FAILED -> "Данные анализа не получены" to "Сервис не вернул результаты для этой территории. Попробуйте обновить экран позже."
        AnalysisStatus.COMPLETED, AnalysisStatus.UNKNOWN -> "Данные сравнения пока недоступны" to "Сервис пока не передал показатели затопления и слои наблюдений для этой территории."
    }
    val containerColor = when (status) {
        AnalysisStatus.PROCESSING -> GidroTheme.colors.primaryLight
        AnalysisStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        AnalysisStatus.COMPLETED, AnalysisStatus.UNKNOWN -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (status) {
        AnalysisStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        AnalysisStatus.COMPLETED, AnalysisStatus.UNKNOWN -> MaterialTheme.colorScheme.onTertiaryContainer
        AnalysisStatus.PROCESSING -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = contentColor, style = MaterialTheme.typography.titleSmall)
            Text(description, modifier = Modifier.padding(top = 5.dp), color = contentColor, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ComparisonPreview(result: AnalysisResult, onOpenComparison: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(210.dp).padding(top = 16.dp).clickable { onOpenComparison(result.id) },
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            GidroMapSurface(
                mapStyle = BaseMapStyle.SATELLITE,
                observationLayers = result.layers,
                visibleLayerTypes = setOf(ObservationLayerType.WATER_BEFORE, ObservationLayerType.WATER_AFTER, ObservationLayerType.FLOOD_GAIN),
                areaOutlinePoints = result.request.area.boundaryPoints(),
                cameraFocusPoints = result.request.area.boundaryPoints(),
                modifier = Modifier.fillMaxSize()
            )
            Card(
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).clickable { onOpenComparison(result.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Fullscreen, contentDescription = null)
                    Text("Посмотреть карту сравнения", modifier = Modifier.padding(start = 6.dp))
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
private fun MetricCards(result: AnalysisResult) {
    Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "Новое затопление",
                "${result.floodedArea.hectares} га",
                "${result.floodedArea.squareKilometers} км²",
                Modifier.weight(1f),
                valueColor = MaterialTheme.colorScheme.primary
            )
            MetricCard("Доля затопления", "${result.floodedSharePercent} %", "от площади анализа", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Вода до события", "${result.waterBefore.hectares} га", "${result.waterBefore.squareKilometers} км²", Modifier.weight(1f))
            MetricCard("Вода на пике", "${result.waterAfter.hectares} га", "${result.waterAfter.squareKilometers} км²", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GidroTheme.colors.dataCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GidroTheme.colors.dataCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(value, modifier = Modifier.padding(top = 5.dp), color = valueColor, style = MaterialTheme.typography.titleMedium)
            Text(caption, modifier = Modifier.padding(top = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ExportButton(label: String, onClick: () -> Unit, modifier: Modifier, primary: Boolean) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = if (primary) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(Icons.Outlined.FileDownload, contentDescription = null)
        Text(label, modifier = Modifier.padding(start = 7.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ReportHeader(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Вернуться назад",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_gidro_monitor),
                    contentDescription = null,
                    modifier = Modifier.height(26.dp),
                    tint = Color.Unspecified
                )
                Text("ГидроМонитор", style = GidroTextStyles.BrandName)
            }
        }
    }
}

private fun machineReportJson(result: AnalysisResult): String = buildString {
    append("{\n")
    append("  \"analysisId\": \"").append(result.id).append("\",\n")
    append("  \"title\": \"").append(result.request.title.jsonEscaped()).append("\",\n")
    append("  \"dateBefore\": \"").append(result.request.dateBefore).append("\",\n")
    append("  \"dateAfter\": \"").append(result.request.dateAfter).append("\",\n")
    append("  \"waterBeforeHectares\": ").append(result.waterBefore.hectares).append(",\n")
    append("  \"waterAfterHectares\": ").append(result.waterAfter.hectares).append(",\n")
    append("  \"floodedAreaHectares\": ").append(result.floodedArea.hectares).append(",\n")
    append("  \"floodedSharePercent\": ").append(result.floodedSharePercent).append("\n")
    append("}\n")
}

private fun String.jsonEscaped(): String = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
