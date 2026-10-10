package com.wiffles.edupage.ui.modifiers

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.isActive
import org.intellij.lang.annotations.Language
import kotlin.math.abs

@Language("AGSL")
private const val SWIPE_BLUR_AGSL = """
    uniform shader composable;
    uniform float2 resolution;
    uniform float swipeVelocity;
    uniform float horizontal;

    half4 main(float2 fragCoord) {
        const int SAMPLES = 8;
        half4 color = half4(0.0);
        float totalWeight = 0.0;

        float blurMagnitude = clamp(swipeVelocity * 20.0, -48.0, 48.0);

        for (int i = 0; i < SAMPLES; i++) {
            float offset = (float(i) / float(SAMPLES - 1) - 0.5) * blurMagnitude;
            float2 sampleCoord;
            if (horizontal > 0.5) {
                float clampedX = clamp(fragCoord.x + offset, 0.0, resolution.x);
                sampleCoord = float2(clampedX, fragCoord.y);
            } else {
                float clampedY = clamp(fragCoord.y + offset, 0.0, resolution.y);
                sampleCoord = float2(fragCoord.x, clampedY);
            }

            float weight = 1.0 - abs(offset / (abs(blurMagnitude) + 0.001)) * 0.5;

            color += composable.eval(sampleCoord) * weight;
            totalWeight += weight;
        }

        return color / totalWeight;
    }
"""

fun Modifier.swipeMotionBlur(
    velocity: Float,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed this
    }
    SwipeBlurEffect(modifier = this, velocity = velocity)
}

fun Modifier.transitionMotionBlur(progress01: Float): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed this
    }
    TransitionBlurEffect(modifier = this, progress01 = progress01)
}

@Composable
private fun TransitionBlurEffect(
    modifier: Modifier,
    progress01: Float,
): Modifier {
    if (!MotionBlurGate.forChrome()) {
        return modifier
    }
    val shader = remember { RuntimeShader(SWIPE_BLUR_AGSL) }
    val peak = progress01 * (1f - progress01) * 4f

    return modifier.graphicsLayer {
        if (peak > 0.02f) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("swipeVelocity", peak * 1.6f)
            shader.setFloatUniform("horizontal", 1.0f)
            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "composable")
                .asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

@Composable
fun <T> BlurStepTransition(
    targetState: T,
    isForward: (from: T, to: T) -> Boolean,
    durationMs: Int = 400,
    content: @Composable (T) -> Unit,
) {
    var renderState by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(targetState) }
    var dir by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(1) }
    var transitioning by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(targetState) {
        if (targetState == renderState) return@LaunchedEffect
        dir = if (isForward(renderState, targetState)) 1 else -1
        transitioning = true
        progress.snapTo(0f)
        progress.animateTo(1f, androidx.compose.animation.core.tween(durationMs))
        renderState = targetState
        transitioning = false
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        if (transitioning) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -dir * progress.value * size.width
                    }
                    .transitionMotionBlur(progress.value),
            ) {
                content(renderState)
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = dir * (1f - progress.value) * size.width
                    }
                    .transitionMotionBlur(progress.value),
            ) {
                content(targetState)
            }
        } else {
            content(renderState)
        }
    }
}

@Composable
fun OverlayTransition(
    visible: Boolean,
    durationMs: Int = 320,
    motionBlur: Boolean = true,
    content: @Composable () -> Unit,
) {
    var rendered by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(visible) }
    val progress = remember { Animatable(if (visible) 1f else 0f) }

    LaunchedEffect(visible) {
        if (visible) {
            rendered = true
            progress.animateTo(1f, androidx.compose.animation.core.tween(durationMs))
        } else {
            progress.animateTo(0f, androidx.compose.animation.core.tween(durationMs))
            rendered = false
        }
    }

    if (rendered) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (1f - progress.value) * size.width
                }
                .then(if (motionBlur) Modifier.transitionMotionBlur(progress.value) else Modifier),
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeBlurEffect(
    modifier: Modifier,
    velocity: Float,
): Modifier {
    val shader = remember { RuntimeShader(SWIPE_BLUR_AGSL) }

    return modifier.graphicsLayer {
        val scaled = velocity * MotionBlurGate.scale
        if (abs(scaled) > 0.05f) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("swipeVelocity", scaled)
            shader.setFloatUniform("horizontal", 1.0f)
            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "composable")
                .asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

object MotionBlurGate {
    @Volatile
    var enabled: Boolean = true

    @Volatile
    var scope: com.wiffles.edupage.data.MotionBlurScope =
        com.wiffles.edupage.data.MotionBlurScope.FULL

    @Volatile
    var scale: Float = 1.0f

    fun forTabs(): Boolean =
        enabled && (scope == com.wiffles.edupage.data.MotionBlurScope.FULL ||
            scope == com.wiffles.edupage.data.MotionBlurScope.TABS)

    fun forChrome(): Boolean =
        enabled && scope == com.wiffles.edupage.data.MotionBlurScope.FULL
}

fun Modifier.scrollMotionBlur(
    scrollState: ScrollState,
    enabled: Boolean = true,
    horizontal: Boolean = false,
): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed this
    }
    val animatedVelocity = remember { Animatable(0f) }

    LaunchedEffect(scrollState) {
        var prevValue = scrollState.value
        var prevTimeNanos = 0L
        while (isActive) {
            var next = 0f
            withFrameNanos { frameTimeNanos ->
                val current = scrollState.value
                if (prevTimeNanos != 0L) {
                    val dtMs = (frameTimeNanos - prevTimeNanos) / 1_000_000.0f
                    if (dtMs in 1f..100f) {
                        val delta = (current - prevValue).toFloat()
                        val target = (delta / dtMs).coerceIn(-3f, 3f)
                        next = if (abs(delta) > 0.1f && scrollState.isScrollInProgress) {
                            animatedVelocity.value * 0.35f + target * 0.65f
                        } else {
                            val decayed = animatedVelocity.value * 0.45f
                            if (abs(decayed) < 0.01f) 0f else decayed
                        }
                    }
                }
                prevValue = current
                prevTimeNanos = frameTimeNanos
            }
            animatedVelocity.snapTo(next)
        }
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            animatedVelocity.animateTo(0f, tween(60))
        }
    }

    ScrollBlurEffect(modifier = this, animatedVelocity = animatedVelocity, horizontal = horizontal)
}

fun Modifier.scrollMotionBlur(
    lazyListState: LazyListState,
    enabled: Boolean = true,
    horizontal: Boolean = false,
): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed this
    }
    val animatedVelocity = remember { Animatable(0f) }

    LaunchedEffect(lazyListState) {
        var prevIndex = lazyListState.firstVisibleItemIndex
        var prevOffset = lazyListState.firstVisibleItemScrollOffset
        var prevTimeNanos = 0L
        while (isActive) {
            var next = 0f
            withFrameNanos { frameTimeNanos ->
                val currentIndex = lazyListState.firstVisibleItemIndex
                val currentOffset = lazyListState.firstVisibleItemScrollOffset
                if (prevTimeNanos != 0L) {
                    val dtMs = (frameTimeNanos - prevTimeNanos) / 1_000_000.0f
                    if (dtMs in 1f..100f) {
                        val totalDelta = ((currentIndex - prevIndex) * 80f) +
                            (currentOffset - prevOffset)
                        val target = (totalDelta / dtMs).coerceIn(-3f, 3f)
                        next = if (abs(totalDelta) > 0.1f && lazyListState.isScrollInProgress) {
                            animatedVelocity.value * 0.35f + target * 0.65f
                        } else {
                            val decayed = animatedVelocity.value * 0.45f
                            if (abs(decayed) < 0.01f) 0f else decayed
                        }
                    }
                }
                prevIndex = currentIndex
                prevOffset = currentOffset
                prevTimeNanos = frameTimeNanos
            }
            animatedVelocity.snapTo(next)
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            animatedVelocity.animateTo(0f, tween(60))
        }
    }

    ScrollBlurEffect(modifier = this, animatedVelocity = animatedVelocity, horizontal = horizontal)
}

@Composable
private fun ScrollBlurEffect(
    modifier: Modifier,
    animatedVelocity: Animatable<Float, *>,
    horizontal: Boolean,
): Modifier {
    val shader = remember { RuntimeShader(SWIPE_BLUR_AGSL) }

    return modifier.graphicsLayer {
        val vel = animatedVelocity.value * MotionBlurGate.scale * if (horizontal) 1f else -1f
        if (abs(vel) > 0.05f) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("swipeVelocity", vel)
            shader.setFloatUniform("horizontal", if (horizontal) 1.0f else 0.0f)
            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "composable")
                .asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

