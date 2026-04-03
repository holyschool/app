package com.enderplusbayzuiship.edupage2.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme

private val ShapeCircle = RoundedPolygon.circle(numVertices = 8)

private val ShapeStar = RoundedPolygon.star(
    numVerticesPerRadius = 6,
    innerRadius = 0.5f,
    rounding = CornerRounding(radius = 0.15f)
)

private val ShapePentagon = RoundedPolygon(
    numVertices = 5,
    rounding = CornerRounding(radius = 0.2f)
)

private val ShapeClover = RoundedPolygon.star(
    numVerticesPerRadius = 4,
    innerRadius = 0.55f,
    rounding = CornerRounding(radius = 0.4f),
    innerRounding = CornerRounding(radius = 0.4f)
)

private val ShapeSunny = RoundedPolygon.star(
    numVerticesPerRadius = 8,
    innerRadius = 0.75f,
    rounding = CornerRounding(radius = 0.12f)
)

private val ShapeHexagon = RoundedPolygon(
    numVertices = 6,
    rounding = CornerRounding(radius = 0.15f)
)

private val MorphShapes = listOf(
    ShapeCircle,
    ShapeStar,
    ShapePentagon,
    ShapeClover,
    ShapeSunny,
    ShapeHexagon,
)

private val Morphs: List<Morph> by lazy {
    buildList {
        for (i in MorphShapes.indices) {
            val next = (i + 1) % MorphShapes.size
            add(Morph(MorphShapes[i], MorphShapes[next]))
        }
    }
}

@Composable
fun SplashScreen(
    onAutoLoginSuccess: () -> Unit,
    onAutoLoginFailed: (username: String, subdomain: String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SplashUiState.Success    -> onAutoLoginSuccess()
            is SplashUiState.GoToLogin  -> onAutoLoginFailed(state.prefillUsername, state.prefillSubdomain)
            else                        -> Unit
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MorphingLoadingIndicator(
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.splash_signing_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MorphingLoadingIndicator(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    val morphProgress  = remember { Animatable(0f) }
    val globalRotation = remember { Animatable(0f) }
    var currentMorph   by remember { mutableIntStateOf(0) }
    var rotationTarget by remember { mutableIntStateOf(90) }

    LaunchedEffect(Unit) {
        launch {
            globalRotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 4800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
        launch {
            val morphSpec = spring<Float>(dampingRatio = 0.6f, stiffness = 200f, visibilityThreshold = 0.1f)
            while (true) {
                val deferred = async {
                    val result = morphProgress.animateTo(1f, animationSpec = morphSpec)
                    if (result.endReason == AnimationEndReason.Finished) {
                        currentMorph  = (currentMorph + 1) % Morphs.size
                        morphProgress.snapTo(0f)
                        rotationTarget = (rotationTarget + 90) % 360
                    }
                }
                delay(650L)
                deferred.await()
            }
        }
    }

    Box(
        modifier = modifier.drawWithCache {
            val path        = Path()
            val matrix      = Matrix()
            val morphList   = Morphs
            val sz          = Size(size.width, size.height)
            val scale       = min(size.width, size.height) * 0.82f

            onDrawBehind {
                val progress  = morphProgress.value
                val morph     = morphList[currentMorph]
                val angle     = progress * 90f + rotationTarget + globalRotation.value

                val androidPath = morph.toPath(progress = progress.coerceIn(0f, 1f))
                path.reset()
                path.addPath(androidPath.asComposePath())

                matrix.reset()
                matrix.scale(scale / 2f, scale / 2f)
                matrix.translate(1f, 1f)
                path.transform(matrix)

                val bounds   = path.getBounds()
                val offsetX  = (sz.width  - bounds.width)  / 2f - bounds.left
                val offsetY  = (sz.height - bounds.height) / 2f - bounds.top
                path.translate(Offset(offsetX, offsetY))

                rotate(angle, pivot = Offset(sz.width / 2f, sz.height / 2f)) {
                    drawPath(path, color = color)
                }
            }
        }
    )
}

@Preview(name = "Splash – Light", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Splash – Dark", showBackground = true, widthDp = 360, heightDp = 640, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SplashScreenPreview() {
    Edupage2Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    MorphingLoadingIndicator(
                        modifier = Modifier.size(72.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.splash_signing_in),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "MorphingIndicator", showBackground = true, widthDp = 120, heightDp = 120)
@Composable
private fun MorphingIndicatorPreview() {
    Edupage2Theme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            MorphingLoadingIndicator(modifier = Modifier.size(72.dp))
        }
    }
}
