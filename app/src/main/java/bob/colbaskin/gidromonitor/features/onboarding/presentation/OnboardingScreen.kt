package bob.colbaskin.gidromonitor.features.onboarding.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import bob.colbaskin.gidromonitor.features.map.domain.model.BaseMapStyle
import bob.colbaskin.gidromonitor.features.map.presentation.GidroMapSurface
import bob.colbaskin.gidromonitor.design_system.theme.GidroTextStyles
import bob.colbaskin.gidromonitor.R

@Composable
fun OnboardingRoute(
    onStartAnalysis: () -> Unit,
    onAlreadyCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    androidx.compose.runtime.LaunchedEffect(viewModel.state.isLoading, viewModel.state.isCompleted) {
        if (!viewModel.state.isLoading && viewModel.state.isCompleted) onAlreadyCompleted()
    }
    if (!viewModel.state.isLoading) {
        OnboardingScreen(
            onStartAnalysis = {
                viewModel.startAnalysis()
                onStartAnalysis()
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun OnboardingScreen(onStartAnalysis: () -> Unit) {
    var showHowItWorks by remember { mutableStateOf(false) }
    var isMapReady by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }
    val mapAlpha by animateFloatAsState(
        targetValue = if (isMapReady) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "onboardingMapFade"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 420, delayMillis = 100),
        label = "onboardingContentFade"
    )
    val contentOffset by animateFloatAsState(
        targetValue = if (contentVisible) 0f else 24f,
        animationSpec = tween(durationMillis = 420, delayMillis = 100),
        label = "onboardingContentOffset"
    )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val titleStyle = GidroTextStyles.LandingTitle.copy(
            fontSize = if (maxWidth < 380.dp) 28.sp else 32.sp,
            lineHeight = if (maxWidth < 380.dp) 34.sp else 38.sp
        )
        Image(
            painter = painterResource(R.drawable.flood_scene),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        GidroMapSurface(
            mapStyle = BaseMapStyle.SATELLITE,
            onMapStyleLoaded = { isMapReady = true },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = mapAlpha)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1D20).copy(alpha = 0.58f))
        )
        Card(
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 20.dp, top = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    painterResource(R.drawable.ic_gidro_monitor),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
                Text("ГидроМонитор", modifier = Modifier.padding(start = 8.dp), style = GidroTextStyles.BrandName)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .graphicsLayer(alpha = contentAlpha, translationY = contentOffset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "М",
                    color = Color.White,
                    style = GidroTextStyles.LandingInitial
                )
                Text(
                    "ОНИТОРИНГ",
                    color = Color.White,
                    style = titleStyle
                )
            }
            Text(
                "ВОДНЫХ ОБЪЕКТОВ\nИ ПАВОДКОВ ИЗ КОСМОСА",
                modifier = Modifier.offset(y = (-12).dp),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = titleStyle
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 30.dp)
                .graphicsLayer(alpha = contentAlpha, translationY = contentOffset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onStartAnalysis,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Новый анализ")
                androidx.compose.material3.Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(
                onClick = { showHowItWorks = true },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
            ) { Text("Как это работает?") }
        }
    }
    if (showHowItWorks) {
        ModalBottomSheet(
            onDismissRequest = { showHowItWorks = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text("Как это работает", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Проведите анализ территории и сравните состояние воды между двумя датами.",
                    modifier = Modifier.padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(modifier = Modifier.padding(top = 18.dp), color = MaterialTheme.colorScheme.outlineVariant)
                HowItWorksStep(
                    number = "1",
                    title = "Отметьте территорию",
                    description = "Выберите прямоугольник или полигон прямо на карте.",
                    modifier = Modifier.padding(top = 16.dp)
                )
                HowItWorksStep(
                    number = "2",
                    title = "Укажите даты",
                    description = "Выберите снимки «до» и «после» интересующего события.",
                    modifier = Modifier.padding(top = 16.dp)
                )
                HowItWorksStep(
                    number = "3",
                    title = "Сравните результат",
                    description = "Откройте карту «до/после», двигайте разделитель и включайте нужные каналы COG по отдельности.",
                    modifier = Modifier.padding(top = 16.dp)
                )
                HowItWorksStep(
                    number = "4",
                    title = "Сравнение доступно офлайн",
                    description = "После первого просмотра TIFF и подготовленные маски сохраняются на устройстве. Карту можно открыть без сети, пока она есть в кэше.",
                    modifier = Modifier.padding(top = 16.dp)
                )
                HowItWorksStep(
                    number = "5",
                    title = "Память под контролем",
                    description = "Приложение хранит до 5 последних карт сравнения и не более 250 МБ. Самые давно не открывавшиеся карты удаляются первыми; их можно загрузить снова при наличии сети.",
                    modifier = Modifier.padding(top = 16.dp)
                )
                Button(
                    onClick = { showHowItWorks = false },
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Понятно") }
            }
        }
    }
}

@Composable
private fun HowItWorksStep(number: String, title: String, description: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) { Text(number, style = MaterialTheme.typography.labelMedium) }
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
