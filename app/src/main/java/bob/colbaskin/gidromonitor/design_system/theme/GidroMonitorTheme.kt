package bob.colbaskin.gidromonitor.design_system.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import bob.colbaskin.gidromonitor.R

data class GidroColors(
    val backgroundDeep: Color, val primaryPressed: Color, val primaryLight: Color, val accent: Color,
    val success: Color, val warning: Color, val info: Color, val mapSelection: Color,
    val mapSelectionFill: Color, val dataCard: Color, val dataCardBorder: Color, val metricBlue: Color
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0076A8), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF3FB), onPrimaryContainer = Color(0xFF163042),
    secondary = Color(0xFF2E9D72), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6F5EF), onSecondaryContainer = Color(0xFF163042),
    tertiary = Color(0xFFE8A23A), onTertiary = Color(0xFF163042),
    tertiaryContainer = Color(0xFFFFF0D9), onTertiaryContainer = Color(0xFF163042),
    background = Color(0xFFF4F8FA), onBackground = Color(0xFF163042),
    surface = Color.White, onSurface = Color(0xFF163042),
    surfaceVariant = Color(0xFFEEF5F8), onSurfaceVariant = Color(0xFF60788A),
    outline = Color(0xFFD8E4EA), outlineVariant = Color(0xFFE3EBEF),
    error = Color(0xFFE25555), onError = Color.White,
    errorContainer = Color(0xFFFFE8E8), onErrorContainer = Color(0xFF7A2020),
    inverseSurface = Color(0xFF163042), inverseOnSurface = Color.White,
    inversePrimary = Color(0xFF13A7E3), surfaceTint = Color(0xFF0076A8), scrim = Color(0xFF163042)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF079DE0), onPrimary = Color(0xFF061727),
    primaryContainer = Color(0xFF123B52), onPrimaryContainer = Color(0xFFF3F8FB),
    secondary = Color(0xFF4BC995), onSecondary = Color(0xFF061727),
    secondaryContainer = Color(0xFF123D35), onSecondaryContainer = Color(0xFFF3F8FB),
    tertiary = Color(0xFFF0B24E), onTertiary = Color(0xFF061727),
    tertiaryContainer = Color(0xFF503A17), onTertiaryContainer = Color(0xFFF3F8FB),
    background = Color(0xFF061727), onBackground = Color(0xFFF3F8FB),
    surface = Color(0xFF0B2235), onSurface = Color(0xFFF3F8FB),
    surfaceVariant = Color(0xFF102A3F), onSurfaceVariant = Color(0xFFA9C0CF),
    outline = Color(0xFF1D4561), outlineVariant = Color(0xFF18384F),
    error = Color(0xFFFF6767), onError = Color(0xFF061727),
    errorContainer = Color(0xFF54272D), onErrorContainer = Color(0xFFFFE9E9),
    inverseSurface = Color(0xFFF3F8FB), inverseOnSurface = Color(0xFF061727),
    inversePrimary = Color(0xFF0076A8), surfaceTint = Color(0xFF079DE0), scrim = Color(0xFF04111D)
)

private val LightGidroColors = GidroColors(
    backgroundDeep = Color(0xFFF4F8FA), primaryPressed = Color(0xFF00638E), primaryLight = Color(0xFFDDF3FB), accent = Color(0xFF13A7E3),
    success = Color(0xFF2E9D72), warning = Color(0xFFE8A23A), info = Color(0xFF168BD2),
    mapSelection = Color(0xFF007DB5), mapSelectionFill = Color(0x3D007DB5),
    dataCard = Color(0xFFF2F8FB), dataCardBorder = Color(0xFFD8E8F0), metricBlue = Color(0xFF0076A8)
)

private val DarkGidroColors = GidroColors(
    backgroundDeep = Color(0xFF04111D), primaryPressed = Color(0xFF078BC6), primaryLight = Color(0xFF123B52), accent = Color(0xFF22B9F2),
    success = Color(0xFF4BC995), warning = Color(0xFFF0B24E), info = Color(0xFF23A9ED),
    mapSelection = Color(0xFF00A5EA), mapSelectionFill = Color(0x4D00A5EA),
    dataCard = Color(0xFF0D263A), dataCardBorder = Color(0xFF19425E), metricBlue = Color(0xFF26B7F3)
)

private val LocalGidroColors = staticCompositionLocalOf { LightGidroColors }

object GidroTheme {
    val colors: GidroColors
        @Composable get() = LocalGidroColors.current
}

object GidroFonts {
    val Brand = FontFamily(
        Font(R.font.inter_variable, weight = FontWeight.Normal),
        Font(R.font.inter_variable, weight = FontWeight.Medium),
        Font(R.font.inter_variable, weight = FontWeight.SemiBold)
    )
    val Display = FontFamily(Font(R.font.cormorant_garamond_italic_variable, weight = FontWeight.Normal))
    val LandingInitial = FontFamily(Font(R.font.great_vibes_regular, weight = FontWeight.Normal))
    val LandingTitle = FontFamily(Font(R.font.ys_text_regular, weight = FontWeight.Normal))
    val Metric = Brand
}

object GidroTextStyles {
    val BrandName = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val LandingInitial = TextStyle(fontFamily = GidroFonts.LandingInitial, fontWeight = FontWeight.Normal, fontSize = 96.sp, lineHeight = 76.95.sp)
    val LandingTitle = TextStyle(fontFamily = GidroFonts.LandingTitle, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 38.sp)
}

private val GidroTypography = Typography(
    displayLarge = TextStyle(fontFamily = GidroFonts.Display, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 44.sp),
    displayMedium = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp),
    headlineLarge = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleLarge = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    titleSmall = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = GidroFonts.Brand, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
fun GidroMonitorTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    CompositionLocalProvider(LocalGidroColors provides if (darkTheme) DarkGidroColors else LightGidroColors) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = GidroTypography, content = content)
    }
}
