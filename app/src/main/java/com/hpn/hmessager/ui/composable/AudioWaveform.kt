package com.hpn.hmessager.ui.composable

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt

private val MinSpikeWidthDp: Dp = 1.dp
private val MaxSpikeWidthDp: Dp = 24.dp
private val MinSpikePaddingDp: Dp = 0.dp
private val MaxSpikePaddingDp: Dp = 12.dp
private val MinSpikeRadiusDp: Dp = 0.dp
private val MaxSpikeRadiusDp: Dp = 12.dp

private const val MinProgress: Float = 0F
private const val MaxProgress: Float = 1F

private const val MinSpikeHeight: Float = 1F

enum class AmplitudeType {
    Avg, Min, Max
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AudioWaveform(
    modifier: Modifier = Modifier,
    style: DrawStyle = Fill,
    waveformBrush: Brush = SolidColor(MaterialTheme.colorScheme.onPrimary),
    progressBrush: Brush = SolidColor(MaterialTheme.colorScheme.onSecondary),
    waveformAlignment: Alignment.Vertical = Alignment.Top,
    amplitudeType: AmplitudeType = AmplitudeType.Avg,
    onProgressChangeFinished: (() -> Unit)? = null,
    spikeWidth: Dp = 4.dp,
    spikeRadius: Dp = 8.dp,
    spikePadding: Dp = 1.dp,
    progress: Float = 0F,
    amplitudes: List<Int>,
    onProgressChange: (Float) -> Unit,
) {
    val _progress = remember(progress) { progress.coerceIn(MinProgress, MaxProgress) }
    val _spikeWidth = remember(spikeWidth) { spikeWidth.coerceIn(MinSpikeWidthDp, MaxSpikeWidthDp) }
    val _spikePadding =
        remember(spikePadding) { spikePadding.coerceIn(MinSpikePaddingDp, MaxSpikePaddingDp) }
    val _spikeRadius =
        remember(spikeRadius) { spikeRadius.coerceIn(MinSpikeRadiusDp, MaxSpikeRadiusDp) }
    val spikeTotalWidth = remember(spikeWidth, spikePadding) { _spikeWidth + _spikePadding }

    var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }
    var spikes by remember { mutableFloatStateOf(0F) }
    val spikesAmplitudes = remember(amplitudes, spikes, amplitudeType) {
        amplitudes.toDrawableAmplitudes(
            amplitudeType = amplitudeType,
            spikes = spikes.toInt(),
            minHeight = MinSpikeHeight,
            maxHeight = canvasSize.height.coerceAtLeast(MinSpikeHeight)
        )
    }

    // Draw part
    Canvas(modifier = Modifier
        .requiredHeight(55.dp)
        .alpha(0.99F) // Not 1 because the progress indicator will be fully opaque (rectangle)
        .pointerInteropFilter {
            return@pointerInteropFilter when (it.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE,
                -> {
                    if (it.x in 0F..canvasSize.width) {
                        onProgressChange(it.x / canvasSize.width)
                        true
                    } else false
                }

                MotionEvent.ACTION_UP -> {
                    onProgressChangeFinished?.invoke()
                    true
                }

                else -> false
            }
        }
        .then(modifier)) {
        canvasSize = size
        spikes = size.width / spikeTotalWidth.toPx()
        spikesAmplitudes.forEachIndexed { index, amplitude ->
            drawRoundRect(
                brush = waveformBrush, topLeft = Offset(
                    x = index * spikeTotalWidth.toPx(), y = when (waveformAlignment) {
                        Alignment.Top -> 0F
                        Alignment.Bottom -> size.height - amplitude
                        Alignment.CenterVertically -> size.height / 2F - amplitude / 2F
                        else -> 0F
                    }
                ), size = Size(
                    width = _spikeWidth.toPx(), height = amplitude
                ), cornerRadius = CornerRadius(_spikeRadius.toPx()), style = style
            )
            drawRect(
                brush = progressBrush, size = Size(
                    width = _progress * size.width, height = size.height
                ), blendMode = BlendMode.SrcAtop
            )
        }
    }
}

private fun List<Int>.toDrawableAmplitudes(
    amplitudeType: AmplitudeType,
    spikes: Int,
    minHeight: Float,
    maxHeight: Float,
): List<Float> {
    if (isEmpty() || spikes == 0) {
        return List(spikes) { minHeight }
    }

    // Cast to float and normalize
    val amplitudes = map(Int::toFloat).normalize(minHeight, maxHeight)

    val transform = { data: List<Float> ->
        when (amplitudeType) {
            AmplitudeType.Avg -> data.average()
            AmplitudeType.Max -> data.max()
            AmplitudeType.Min -> data.min()
        }.toFloat()
    }

    return when {
        spikes > amplitudes.count() -> amplitudes.fillToSize(spikes, transform)
        else -> amplitudes.chunkToSize(spikes, transform)
    }
}

internal fun <T> Iterable<T>.fillToSize(size: Int, transform: (List<T>) -> T): List<T> {
    val capacity = ceil(size / count().toFloat()).roundToInt()

    return map { data -> List(capacity) { data } }.flatten().chunkToSize(size, transform)
}

internal fun <T> Iterable<T>.chunkToSize(size: Int, transform: (List<T>) -> T): List<T> {
    val chunkSize = count() / size
    val remainder = count() % size
    val remainderIndex = ceil(count().safeDiv(remainder)).roundToInt()
    val chunkIteration = filterIndexed { index, _ ->
        remainderIndex == 0 || index % remainderIndex != 0
    }.chunked(chunkSize, transform)
    return when (size) {
        chunkIteration.count() -> chunkIteration
        else -> chunkIteration.chunkToSize(size, transform)
    }
}

internal fun Iterable<Float>.normalize(min: Float, max: Float): List<Float> {
    val minL = min()
    val maxL = max()

    return map { (max - min) * ((it - minL).safeDiv(maxL - minL)) + min }
}

private fun Int.safeDiv(value: Int): Float {
    return if (value == 0) return 0F else this / value.toFloat()
}

private infix fun Float.safeDiv(value: Float): Float {
    return if (value == 0f) return 0F else this / value
}