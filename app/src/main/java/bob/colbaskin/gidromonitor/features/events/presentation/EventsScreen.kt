package bob.colbaskin.gidromonitor.features.events.presentation

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import bob.colbaskin.gidromonitor.common.UiState
import bob.colbaskin.gidromonitor.common.ui.PullRefreshContainer
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisHistoryItem
import bob.colbaskin.gidromonitor.features.map.domain.model.BaseMapStyle
import bob.colbaskin.gidromonitor.features.map.presentation.GidroMapSurface
import bob.colbaskin.gidromonitor.R
import bob.colbaskin.gidromonitor.design_system.theme.GidroTextStyles
import bob.colbaskin.gidromonitor.design_system.theme.GidroTheme

@Composable
fun EventsRoute(
    onOpenAnalysis: (String) -> Unit,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val filtered = (state.history as? UiState.Success<List<AnalysisHistoryItem>>)?.data.orEmpty().filter { item ->
        item.title.contains(state.query, ignoreCase = true) || item.id.contains(state.query, ignoreCase = true)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        EventsHeader()
        Text("Измерения", modifier = Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleLarge)
        Text("История выполненных анализов", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            placeholder = { Text("Поиск по территории или измерению") },
            singleLine = true
        )
        PullRefreshContainer(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f)
        ) {
            when (val history = state.history) {
                UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        history.title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        history.text,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Потяните вниз, чтобы повторить запрос.",
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is UiState.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) { items(filtered, key = { it.id }) { EventCard(it, onOpenAnalysis) } }
            }
        }
    }
}

@Composable
private fun EventCard(item: AnalysisHistoryItem, onOpenAnalysis: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenAnalysis(item.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniMapPreview(item = item, onClick = { onOpenAnalysis(item.id) }, modifier = Modifier.size(width = 92.dp, height = 76.dp))
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text("${item.dateBefore} → ${item.dateAfter}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    eventStatusLabel(item),
                    modifier = Modifier.padding(top = 6.dp),
                    color = if (item.status.equals("processing", ignoreCase = true)) GidroTheme.colors.info else if (item.floodedHectares > 0) GidroTheme.colors.warning else GidroTheme.colors.success,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun eventStatusLabel(item: AnalysisHistoryItem): String = when (item.status.lowercase()) {
    "processing", "pending" -> "Обработка выполняется"
    "failed", "error" -> "Обработка не завершена"
    else -> if (item.floodedHectares > 0) "Новое затопление · ${item.floodedHectares} га" else "Расчётные данные недоступны"
}

@Composable
private fun EventsHeader() {
    Surface(
        modifier = Modifier.padding(top = 12.dp),
            shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gidro_monitor),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = Color.Unspecified
            )
            Text("ГидроМонитор", modifier = Modifier.padding(start = 8.dp), style = GidroTextStyles.BrandName)
        }
    }
}

@Composable
private fun MiniMapPreview(item: AnalysisHistoryItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        GidroMapSurface(
            mapStyle = BaseMapStyle.SATELLITE,
            cameraFocusPoints = item.previewGeometry,
            cameraFocusPadding = 6.dp,
            initialZoom = 13.0,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().clickable(onClick = onClick))
    }
}
