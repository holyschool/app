package com.enderplusbayzuiship.edupage2.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.enderplusbayzuiship.edupage2.data.AccentColor

val Primary40          = Color(0xFF1A6DB5)
val OnPrimary10        = Color(0xFFFFFFFF)
val PrimaryContainer90 = Color(0xFFD3E5FF)
val OnPrimaryContainer10 = Color(0xFF001C3B)

val Secondary40        = Color(0xFF1A7A6E)
val OnSecondary10      = Color(0xFFFFFFFF)
val SecondaryContainer90 = Color(0xFFB7EFEA)
val OnSecondaryContainer10 = Color(0xFF002421)

val Tertiary40         = Color(0xFF7A4F1A)
val OnTertiary10       = Color(0xFFFFFFFF)
val TertiaryContainer90 = Color(0xFFFFDDB5)
val OnTertiaryContainer10 = Color(0xFF2B1700)

val Error40            = Color(0xFFB3261E)
val OnError10          = Color(0xFFFFFFFF)
val ErrorContainer90   = Color(0xFFF9DEDC)
val OnErrorContainer10 = Color(0xFF410E0B)

val Background99       = Color(0xFFFBFCFF)
val OnBackground10     = Color(0xFF191C20)
val Surface99          = Color(0xFFFBFCFF)
val OnSurface10        = Color(0xFF191C20)
val SurfaceVariant94   = Color(0xFFDDE3EA)
val OnSurfaceVariant30 = Color(0xFF41474E)
val Outline50          = Color(0xFF72787E)

val SurfaceContainerLowest = Color(0xFFEFF3F7)
val SurfaceContainerLow    = Color(0xFFDFE5EC)
val SurfaceContainer       = Color(0xFFD2D9E2)
val SurfaceContainerHigh   = Color(0xFFC6CED9)
val SurfaceContainerHighest= Color(0xFFBBC4D1)
val SurfaceBrightLight     = Color(0xFFDDE2E8)
val SurfaceDimLight        = Color(0xFFC9CFD8)

val Primary80          = Color(0xFFA4C8FF)
val OnPrimary20        = Color(0xFF003062)
val PrimaryContainer30 = Color(0xFF004787)
val OnPrimaryContainer90 = Color(0xFFD3E5FF)

val Secondary80        = Color(0xFF81D3CC)
val OnSecondary20      = Color(0xFF003D38)
val SecondaryContainer30 = Color(0xFF005550)
val OnSecondaryContainer90 = Color(0xFFB7EFEA)

val Tertiary80         = Color(0xFFEFBD80)
val OnTertiary20       = Color(0xFF462A00)
val TertiaryContainer30 = Color(0xFF5E3D00)
val OnTertiaryContainer90 = Color(0xFFFFDDB5)

val Error80            = Color(0xFFF2B8B5)
val OnError20          = Color(0xFF601410)
val ErrorContainer30   = Color(0xFF8C1D18)
val OnErrorContainer90 = Color(0xFFF9DEDC)

val Background6        = Color(0xFF111318)
val OnBackground90     = Color(0xFFE2E2E9)
val Surface6           = Color(0xFF111318)
val OnSurface90        = Color(0xFFE2E2E9)
val SurfaceVariant17   = Color(0xFF41474E)
val OnSurfaceVariant80 = Color(0xFFC1C7CE)
val Outline60          = Color(0xFF8B9198)

val SurfaceContainerLowestDark = Color(0xFF090C10)
val SurfaceContainerLowDark    = Color(0xFF14171B)
val SurfaceContainerDark       = Color(0xFF181B20)
val SurfaceContainerHighDark   = Color(0xFF22252B)
val SurfaceContainerHighestDark= Color(0xFF2D3138)
val SurfaceBrightDark          = Color(0xFF202329)
val SurfaceDimDark             = Color(0xFF0F1216)

data class TonalRoles(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
)

private val lightRoles = TonalRoles(Primary40, OnPrimary10, PrimaryContainer90, OnPrimaryContainer10)
private val darkRoles = TonalRoles(Primary80, OnPrimary20, PrimaryContainer30, OnPrimaryContainer90)

private fun accent(primary: Color, onPrimary: Color, container: Color, onContainer: Color) =
    TonalRoles(primary, onPrimary, container, onContainer)

fun accentLightPrimary(accent: AccentColor): TonalRoles = when (accent) {
    AccentColor.BLUE   -> lightRoles
    AccentColor.PURPLE -> accent(Color(0xFF6A4FBF), Color.White, Color(0xFFE7DEFF), Color(0xFF1E0060))
    AccentColor.GREEN  -> accent(Color(0xFF2E7D32), Color.White, Color(0xFFB7F0C0), Color(0xFF00210A))
    AccentColor.ORANGE -> accent(Color(0xFFB35A00), Color.White, Color(0xFFFFDCC3), Color(0xFF2F1500))
    AccentColor.RED    -> accent(Color(0xFFB3261E), Color.White, Color(0xFFF9DEDC), Color(0xFF410E0B))
    AccentColor.TEAL   -> accent(Color(0xFF00796B), Color.White, Color(0xFFB2F3E9), Color(0xFF00201C))
    AccentColor.PINK   -> accent(Color(0xFFC2185B), Color.White, Color(0xFFFFD9E2), Color(0xFF3E001D))
    AccentColor.SLATE  -> accent(Color(0xFF455A64), Color.White, Color(0xFFCFE8F3), Color(0xFF001F29))
}

fun accentDarkPrimary(accent: AccentColor): TonalRoles = when (accent) {
    AccentColor.BLUE   -> darkRoles
    AccentColor.PURPLE -> accent(Color(0xFFC9BDFF), Color(0xFF35009A), Color(0xFF4F33A4), Color(0xFFE7DEFF))
    AccentColor.GREEN  -> accent(Color(0xFF79DD89), Color(0xFF003912), Color(0xFF005320), Color(0xFFB7F0C0))
    AccentColor.ORANGE -> accent(Color(0xFFFFB87A), Color(0xFF4A2800), Color(0xFF7A3F00), Color(0xFFFFDCC3))
    AccentColor.RED    -> accent(Color(0xFFF2B8B5), Color(0xFF601410), Color(0xFF8C1D18), Color(0xFFF9DEDC))
    AccentColor.TEAL   -> accent(Color(0xFF74D7C8), Color(0xFF003733), Color(0xFF005049), Color(0xFFB2F3E9))
    AccentColor.PINK   -> accent(Color(0xFFFFB1C8), Color(0xFF660033), Color(0xFF8C0046), Color(0xFFFFD9E2))
    AccentColor.SLATE  -> accent(Color(0xFFA9CCDB), Color(0xFF0E3442), Color(0xFF2E4753), Color(0xFFCFE8F3))
}

/**
 * Builds [TonalRoles] from an arbitrary accent ARGB, used by the
 * "custom accent color" experimental appearance option.
 */
fun accentRolesFromArgb(argb: Int, dark: Boolean): TonalRoles {
    val base = Color(argb)
    val darkContent = base.luminance() > 0.55f
    return if (dark) {
        TonalRoles(
            primary = lerp(base, Color.White, 0.32f),
            onPrimary = if (darkContent) Color(0xFF1A1A1A) else Color.White,
            container = lerp(base, Color.Black, 0.48f),
            onContainer = lerp(base, Color.White, 0.60f),
        )
    } else {
        TonalRoles(
            primary = base,
            onPrimary = if (darkContent) Color(0xFF1A1A1A) else Color.White,
            container = lerp(base, Color.White, 0.74f),
            onContainer = lerp(base, Color.Black, 0.58f),
        )
    }
}

