package com.enderplusbayzuiship.edupage2.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary                = Primary40,
    onPrimary              = OnPrimary10,
    primaryContainer       = PrimaryContainer90,
    onPrimaryContainer     = OnPrimaryContainer10,
    secondary              = Secondary40,
    onSecondary            = OnSecondary10,
    secondaryContainer     = SecondaryContainer90,
    onSecondaryContainer   = OnSecondaryContainer10,
    tertiary               = Tertiary40,
    onTertiary             = OnTertiary10,
    tertiaryContainer      = TertiaryContainer90,
    onTertiaryContainer    = OnTertiaryContainer10,
    error                  = Error40,
    onError                = OnError10,
    errorContainer         = ErrorContainer90,
    onErrorContainer       = OnErrorContainer10,
    background             = Background99,
    onBackground           = OnBackground10,
    surface                = Surface99,
    onSurface              = OnSurface10,
    surfaceVariant         = SurfaceVariant94,
    onSurfaceVariant       = OnSurfaceVariant30,
    outline                = Outline50,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow    = SurfaceContainerLow,
    surfaceContainer       = SurfaceContainer,
    surfaceContainerHigh   = SurfaceContainerHigh,
    surfaceContainerHighest= SurfaceContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary                = Primary80,
    onPrimary              = OnPrimary20,
    primaryContainer       = PrimaryContainer30,
    onPrimaryContainer     = OnPrimaryContainer90,
    secondary              = Secondary80,
    onSecondary            = OnSecondary20,
    secondaryContainer     = SecondaryContainer30,
    onSecondaryContainer   = OnSecondaryContainer90,
    tertiary               = Tertiary80,
    onTertiary             = OnTertiary20,
    tertiaryContainer      = TertiaryContainer30,
    onTertiaryContainer    = OnTertiaryContainer90,
    error                  = Error80,
    onError                = OnError20,
    errorContainer         = ErrorContainer30,
    onErrorContainer       = OnErrorContainer90,
    background             = Background6,
    onBackground           = OnBackground90,
    surface                = Surface6,
    onSurface              = OnSurface90,
    surfaceVariant         = SurfaceVariant17,
    onSurfaceVariant       = OnSurfaceVariant80,
    outline                = Outline60,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow    = SurfaceContainerLowDark,
    surfaceContainer       = SurfaceContainerDark,
    surfaceContainerHigh   = SurfaceContainerHighDark,
    surfaceContainerHighest= SurfaceContainerHighestDark,
)

@Composable
fun Edupage2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (API 31)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
