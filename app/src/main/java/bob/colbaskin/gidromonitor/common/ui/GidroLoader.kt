package bob.colbaskin.gidromonitor.common.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

private data class LoaderFrame(
    val at: Float,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f
)

@Composable
fun GidroLoader(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    showBackground: Boolean = false,
    color: Color = LocalContentColor.current
) {
    val transition = rememberInfiniteTransition(label = "gidroLoader")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(3_600, easing = LinearEasing)),
        label = "gidroLoaderPhase"
    )
    val mainPath = remember { PathParser().parsePathString(MainPathData).toPath() }
    val dotPath = remember { PathParser().parsePathString(DotPathData).toPath() }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasScale = min(this.size.width, this.size.height) / 32f
        val offset = Offset((this.size.width - 32f * canvasScale) / 2f, (this.size.height - 32f * canvasScale) / 2f)
        val logoColor = if (showBackground) LogoOnBackground else color
        withTransform({ translate(offset.x, offset.y); scale(canvasScale, canvasScale, Offset.Zero) }) {
            if (showBackground) {
                drawRoundRect(
                    color = Background,
                    topLeft = Offset.Zero,
                    size = Size(32f, 32f),
                    cornerRadius = CornerRadius(6.4f, 6.4f)
                )
            }
            drawPulse(phase.frame(CollectorFrames), Offset(25.5f, 25.5f), logoColor)
            drawLogoPath(mainPath, phase.frame(MainFrames), Offset(16f, 16f), logoColor)
            drawLogoPath(dotPath, phase.frame(DotFrames), Offset(9f, 23f), logoColor)
            drawParticle(Offset(7.5f, 8f), phase.frame(ParticleOneFrames), 1.2f, logoColor)
            drawParticle(Offset(14.5f, 12.5f), phase.frame(ParticleTwoFrames), 1f, logoColor)
            drawParticle(Offset(22.5f, 9.5f), phase.frame(ParticleThreeFrames), 1.15f, logoColor)
        }
        }
    }
}

private fun DrawScope.drawLogoPath(path: Path, frame: LoaderFrame, pivot: Offset, color: Color) {
    withTransform({
        translate(frame.x, frame.y)
        rotate(frame.rotation, pivot)
        scale(frame.scale, frame.scale, pivot)
    }) {
        drawPath(path, color, alpha = frame.alpha)
    }
}

private fun DrawScope.drawPulse(frame: LoaderFrame, center: Offset, color: Color) {
    drawCircle(color, radius = 1.1f * frame.scale, center = center, alpha = frame.alpha)
}

private fun DrawScope.drawParticle(center: Offset, frame: LoaderFrame, radius: Float, color: Color) {
    drawCircle(
        color = color,
        radius = radius * frame.scale,
        center = center + Offset(frame.x, frame.y),
        alpha = frame.alpha
    )
}

private fun Float.frame(frames: List<LoaderFrame>): LoaderFrame {
    val nextIndex = frames.indexOfFirst { this <= it.at }.takeIf { it >= 0 } ?: return frames.last()
    if (nextIndex == 0) return frames.first()
    val previous = frames[nextIndex - 1]
    val next = frames[nextIndex]
    val fraction = ((this - previous.at) / (next.at - previous.at)).coerceIn(0f, 1f)
    return LoaderFrame(
        at = this,
        x = previous.x + (next.x - previous.x) * fraction,
        y = previous.y + (next.y - previous.y) * fraction,
        scale = previous.scale + (next.scale - previous.scale) * fraction,
        rotation = previous.rotation + (next.rotation - previous.rotation) * fraction,
        alpha = previous.alpha + (next.alpha - previous.alpha) * fraction
    )
}

private val MainFrames = listOf(
    LoaderFrame(0f), LoaderFrame(14f), LoaderFrame(30f, 4f, 4f, 0.66f, 5f, 0.82f),
    LoaderFrame(42f, 10.5f, 10.5f, 0.08f, 18f, 0.08f), LoaderFrame(67f, 10.5f, 10.5f, 0.08f, 18f, 0.08f),
    LoaderFrame(76f, -0.5f, -0.5f, 1.05f, -2f), LoaderFrame(84f, 0f, 0f, 0.98f),
    LoaderFrame(92f), LoaderFrame(100f)
)

private val DotFrames = listOf(
    LoaderFrame(0f), LoaderFrame(14f), LoaderFrame(30f, 8f, 1.5f, 0.62f, alpha = 0.86f),
    LoaderFrame(42f, 16f, 2.5f, 0.1f, alpha = 0.08f), LoaderFrame(51f, 16f, 2.5f, 0.1f, alpha = 0.08f),
    LoaderFrame(59f, -0.6f, -0.2f, 1.08f), LoaderFrame(67f), LoaderFrame(92f), LoaderFrame(100f)
)

private val CollectorFrames = listOf(
    LoaderFrame(0f, scale = 0.35f, alpha = 0f), LoaderFrame(24f, scale = 0.35f, alpha = 0f),
    LoaderFrame(39f, scale = 1f, alpha = 0.85f), LoaderFrame(48f, scale = 1.9f, alpha = 0.38f),
    LoaderFrame(57f, scale = 0.75f, alpha = 0.7f), LoaderFrame(75f, scale = 0.35f, alpha = 0f),
    LoaderFrame(100f, scale = 0.35f, alpha = 0f)
)

private val ParticleOneFrames = particleFrames(43f, 48f, 57f, 65f, 18f, 17.5f, 13f, 12f)
private val ParticleTwoFrames = particleFrames(49f, 54f, 63f, 71f, 11f, 13f, 7f, 8f)
private val ParticleThreeFrames = particleFrames(55f, 60f, 69f, 77f, 3f, 16f, 2f, 9f)

private fun particleFrames(
    hiddenUntil: Float,
    emergeAt: Float,
    settleAt: Float,
    hiddenAt: Float,
    startX: Float,
    startY: Float,
    middleX: Float,
    middleY: Float
) = listOf(
    LoaderFrame(0f, startX, startY, 0.25f, alpha = 0f),
    LoaderFrame(hiddenUntil, startX, startY, 0.25f, alpha = 0f),
    LoaderFrame(emergeAt, middleX, middleY, 0.72f),
    LoaderFrame(settleAt, scale = 1f, alpha = 0.92f),
    LoaderFrame(hiddenAt, scale = 0.8f, alpha = 0f),
    LoaderFrame(100f, scale = 0.8f, alpha = 0f)
)

private val Background = Color(0xFF215D61)
private val LogoOnBackground = Color(0xFFFBFEFE)

private const val MainPathData = "M8.39832 3.01749C9.03627 2.95483 10.0385 3.06647 10.6514 3.2575C13.8626 4.2582 14.0974 7.32741 14.942 10.0323C15.3281 11.2579 15.832 12.4419 16.4464 13.567C17.2765 15.1096 18.4278 16.8716 19.7938 17.9723C20.4161 18.4737 21.036 18.7948 21.7989 18.3669C22.3271 18.0704 22.5267 17.5685 22.4606 16.9701C22.441 16.7928 22.1891 16.3989 22.1474 16.1982C21.8521 15.3302 21.4894 14.4881 21.139 13.6421C20.731 12.6729 20.2276 11.7791 20.1396 10.7081C20.0338 9.41984 20.5636 8.07224 21.7333 7.4478C23.4875 6.51142 26.1911 6.83195 27.1915 8.74953C28.054 10.4033 27.374 11.9009 26.6011 13.3848C25.6162 15.3772 24.619 17.1756 25.7749 19.3448C26.0251 19.7832 26.3528 20.13 26.6939 20.4974C27.4315 21.2915 27.8724 22.0984 27.9765 23.2006C28.083 24.3314 27.8345 25.4859 27.0949 26.3628C26.305 27.2555 25.0009 27.9101 23.8401 27.9893C21.4591 28.1516 19.566 26.4431 18.8148 24.2589C18.6544 23.7927 18.4922 23.302 18.3231 22.8372C18.0776 22.1625 17.7641 21.4643 17.4371 20.8236C16.6453 19.2627 15.5114 17.9078 14.1229 16.8637C12.9281 15.9775 11.4432 15.2544 10.0129 14.8626C8.16282 14.3559 6.4769 14.3155 4.96744 12.9466C2.49595 10.7052 2.35115 7.29246 4.54331 4.79964C5.59962 3.59681 6.8521 3.12182 8.39832 3.01749Z"
private const val DotPathData = "M8.52715 17.6319C8.53622 17.6302 8.54526 17.6284 8.55433 17.6267C10.0463 17.5008 11.5037 17.8854 12.686 18.8364C14.8821 20.603 15.262 23.674 13.5034 25.9138C12.5778 27.0755 11.2359 27.8151 9.77377 27.9693C9.76019 27.9718 9.74563 27.9749 9.72627 27.9763C8.18822 28.0763 6.61859 27.6026 5.42944 26.6076C4.44696 25.7929 3.82835 24.6101 3.71326 23.3262C3.57777 21.9772 3.98239 20.6299 4.83571 19.5882C5.77627 18.4339 7.06767 17.7728 8.52715 17.6319Z"
