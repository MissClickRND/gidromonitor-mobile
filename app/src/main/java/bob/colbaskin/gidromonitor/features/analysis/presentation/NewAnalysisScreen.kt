package bob.colbaskin.gidromonitor.features.analysis.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import bob.colbaskin.gidromonitor.common.UiState
import bob.colbaskin.gidromonitor.common.ui.GidroLoader
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisResult
import bob.colbaskin.gidromonitor.features.analysis.domain.model.AnalysisArea
import bob.colbaskin.gidromonitor.features.map.domain.model.BaseMapStyle
import bob.colbaskin.gidromonitor.features.map.presentation.GidroMapSurface
import bob.colbaskin.gidromonitor.R
import bob.colbaskin.gidromonitor.design_system.theme.GidroTextStyles
import bob.colbaskin.gidromonitor.design_system.theme.GidroTheme
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun NewAnalysisRoute(
    onAnalysisReady: (String) -> Unit,
    viewModel: NewAnalysisViewModel = hiltViewModel()
) {
    val success = viewModel.state.resultState as? UiState.Success<AnalysisResult>
    LaunchedEffect(success?.data?.id) { success?.data?.id?.let(onAnalysisReady) }
    when (val resultState = viewModel.state.resultState) {
        UiState.Loading -> AnalysisLoadingScreen()
        is UiState.Error -> AnalysisErrorScreen(
            onRetry = { viewModel.onAction(NewAnalysisAction.RunAnalysis) },
            onReturnToForm = { viewModel.onAction(NewAnalysisAction.DismissResult) }
        )
        else -> NewAnalysisScreen(state = viewModel.state, onAction = viewModel::onAction)
    }
}

@Composable
private fun AnalysisLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GidroLoader(size = 52.dp)
            Text("Отправляем территорию на обработку…", modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AnalysisErrorScreen(
    onRetry: () -> Unit,
    onReturnToForm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Не удалось запустить анализ",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Проверьте подключение к интернету и попробуйте ещё раз.",
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().padding(top = 20.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Повторить запрос")
        }
        OutlinedButton(onClick = onReturnToForm, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Вернуться к параметрам")
        }
    }
}

@Composable
private fun NewAnalysisScreen(state: NewAnalysisState, onAction: (NewAnalysisAction) -> Unit) {
    var showForm by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        GidroMapSurface(
            mapStyle = state.mapStyle,
            draftPoints = state.draftPoints,
            onMapTap = { point ->
                if (state.drawingMode == DrawMode.NONE) {
                    Toast.makeText(context, "Выберите инструмент рисования", Toast.LENGTH_SHORT).show()
                } else {
                    onAction(NewAnalysisAction.AddMapPoint(point))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        MapHeader()
        DrawingControls(
            state = state,
            onAction = onAction,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp)
        )
        IconButton(
            onClick = {
                onAction(NewAnalysisAction.ChangeBaseMap(
                    if (state.mapStyle == BaseMapStyle.SATELLITE) BaseMapStyle.STREETS else BaseMapStyle.SATELLITE
                ))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .zIndex(1f)
        ) { Icon(Icons.Outlined.Layers, "Сменить подложку: ${state.mapStyle.label}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (state.area == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .widthIn(max = 220.dp)
                    .padding(start = 16.dp, bottom = 16.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                contentColor = GidroTheme.colors.info
            ) {
                Text(
                    text = "Выберите инструмент и отметьте территорию на карте",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                icon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                text = { Text("Параметры анализа") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
    if (showForm) AnalysisFormSheet(state, onAction) { showForm = false }
}

@Composable
private fun MapHeader() {
    Row(
        modifier = Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 3.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Icon(
                    painterResource(R.drawable.ic_gidro_monitor),
                    contentDescription = null,
                    modifier = Modifier.height(24.dp),
                    tint = Color.Unspecified
                )
                Text(
                    "ГидроМонитор",
                    modifier = Modifier.padding(start = 8.dp),
                    style = GidroTextStyles.BrandName
                )
            }
        }
    }
}

@Composable
private fun DrawingControls(
    state: NewAnalysisState,
    onAction: (NewAnalysisAction) -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolButton(Icons.Outlined.CropSquare, "Нарисовать прямоугольник", state.drawingMode == DrawMode.RECTANGLE) {
            onAction(NewAnalysisAction.StartDrawing(DrawMode.RECTANGLE))
        }
        ToolButton(Icons.Outlined.Edit, "Нарисовать полигон", state.drawingMode == DrawMode.POLYGON) {
            onAction(NewAnalysisAction.StartDrawing(DrawMode.POLYGON))
        }
        ToolButton(Icons.Outlined.Undo, "Отменить последнюю точку", enabled = state.draftPoints.isNotEmpty()) {
            onAction(NewAnalysisAction.RemoveLastPoint)
        }
        ToolButton(Icons.Outlined.DeleteOutline, "Удалить область", enabled = state.area != null || state.draftPoints.isNotEmpty()) {
            onAction(NewAnalysisAction.ClearArea)
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val targetContainerColor = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 1f else 0.56f)
    }
    val targetContentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        active -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(180),
        label = "drawingToolContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(180),
        label = "drawingToolContent"
    )
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Icon(icon, description, tint = contentColor)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AnalysisFormSheet(
    state: NewAnalysisState,
    onAction: (NewAnalysisAction) -> Unit,
    onDismiss: () -> Unit
) {
    var activeDatePicker by remember { mutableStateOf<DatePickerTarget?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Новый анализ", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

            StepHeading(number = "1", title = "Выберите территорию", modifier = Modifier.padding(top = 16.dp))
            TextField(
                value = state.title,
                onValueChange = { onAction(NewAnalysisAction.UpdateTitle(it)) },
                placeholder = { Text("Напишите название для территории") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                colors = compactTextFieldColors()
            )
            Text(
                text = when (val area = state.area) {
                    null -> "Выберите инструмент и отметьте область на карте."
                    is AnalysisArea.Polygon -> "Полигон выбран: ${area.points.size} вершин."
                    is AnalysisArea.BoundingBox -> "Прямоугольник выбран."
                },
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            StepHeading(number = "2", title = "Выберите даты", modifier = Modifier.padding(top = 16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Дата ДО", state.dateBefore, { activeDatePicker = DatePickerTarget.BEFORE }, Modifier.weight(1f))
                DateField("Дата ПОСЛЕ", state.dateAfter, { activeDatePicker = DatePickerTarget.AFTER }, Modifier.weight(1f))
            }
            state.validationMessage?.let { Text(it, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { onAction(NewAnalysisAction.RunAnalysis) },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                enabled = state.resultState !is UiState.Loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.resultState is UiState.Loading) {
                    GidroLoader(size = 20.dp)
                } else Text("Запустить анализ")
            }
        }
    }
    activeDatePicker?.let { target ->
        AnalysisDatePickerDialog(
            onDismiss = { activeDatePicker = null },
            onDateSelected = { date ->
                onAction(
                    when (target) {
                        DatePickerTarget.BEFORE -> NewAnalysisAction.UpdateDateBefore(date)
                        DatePickerTarget.AFTER -> NewAnalysisAction.UpdateDateAfter(date)
                    }
                )
                activeDatePicker = null
            }
        )
    }
}

@Composable
private fun DateField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            TextField(
                value = value,
                onValueChange = {},
                placeholder = { Text("Выберите") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                colors = compactTextFieldColors()
            )
            Box(modifier = Modifier.matchParentSize().clickable(onClick = onClick))
        }
    }
}

private enum class DatePickerTarget { BEFORE, AFTER }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AnalysisDatePickerDialog(onDismiss: () -> Unit, onDateSelected: (String) -> Unit) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .format(DateTimeFormatter.ofPattern("dd.MM.uuuu"))
                        onDateSelected(date)
                    }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) { Text("Выбрать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    ) { DatePicker(state = datePickerState) }
}

@Composable
private fun StepHeading(number: String, title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Text(number, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
        }
        Text(title, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun compactTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
)
