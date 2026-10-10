package com.wiffles.edupage.ui.about

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wiffles.edupage.R
import com.wiffles.edupage.ui.util.rememberAppHaptics
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "AboutScreen"
private const val AUTHOR_URL = "https://github.com/FoxyIsCoding"
private const val AUTHOR_AVATAR = "https://github.com/FoxyIsCoding.png?size=240"
private const val PROJECT_URL = "https://github.com/holyschool/app"
private const val ISSUES_URL = "https://github.com/holyschool/app/issues/new"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { haptics.virtualKey(); onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            AboutFooter(
                onOpenLink = { url ->
                    Log.i(TAG, "opening $url")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        .onFailure { Log.w(TAG, "no browser to open $url") }
                },
                onOpenLicenses = {
                    Log.i(TAG, "opening OSS licenses")
                    context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                },
            )

            Spacer(Modifier.height(12.dp))

            ContributorsSection(
                onOpenLink = { url ->
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        .onFailure { Log.w(TAG, "no browser to open $url") }
                },
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AboutFooter(
    onOpenLink: (String) -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val avatar = rememberRemoteImage(AUTHOR_AVATAR)
    val haptics = rememberAppHaptics()

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val cookie = remember { CookieShape() }
            Surface(
                shape = cookie,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(84.dp),
            ) {
                if (avatar != null) {
                    Image(
                        bitmap = avatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(cookie),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.about_made_by, "FoxyIsCoding"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.about_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FooterAction(
                        icon = Icons.Rounded.Code,
                        label = stringResource(R.string.about_action_source),
                        modifier = Modifier.weight(1f),
                        onClick = { haptics.virtualKey(); onOpenLink(PROJECT_URL) },
                    )
                    VerticalDivider(
                        modifier = Modifier.height(48.dp).align(Alignment.CenterVertically),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    FooterAction(
                        icon = Icons.Rounded.BugReport,
                        label = stringResource(R.string.about_action_issue),
                        modifier = Modifier.weight(1f),
                        onClick = { haptics.virtualKey(); onOpenLink(ISSUES_URL) },
                    )
                    VerticalDivider(
                        modifier = Modifier.height(48.dp).align(Alignment.CenterVertically),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    FooterAction(
                        icon = Icons.Rounded.Description,
                        label = stringResource(R.string.about_action_licenses),
                        modifier = Modifier.weight(1f),
                        onClick = { haptics.virtualKey(); onOpenLicenses() },
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun rememberRemoteImage(url: String): ImageBitmap? {
    var image by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        image = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = java.net.URL(url).openStream().use { it.readBytes() }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    return image
}

private class CookieShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outer = minOf(centerX, centerY)
        val inner = outer * 0.86f
        val steps = 24

        val points = (0 until steps).map { i ->
            val radius = if (i % 2 == 0) outer else inner
            val angle = (i.toDouble() / steps) * 2.0 * PI
            Offset(
                (centerX + radius * cos(angle)).toFloat(),
                (centerY + radius * sin(angle)).toFloat(),
            )
        }

        val start = Offset(
            (points[0].x + points[1].x) / 2f,
            (points[0].y + points[1].y) / 2f,
        )
        path.moveTo(start.x, start.y)
        for (i in 0 until steps) {
            val vertex = points[i]
            val next = points[(i + 1) % steps]
            val mid = Offset((vertex.x + next.x) / 2f, (vertex.y + next.y) / 2f)
            path.quadraticBezierTo(vertex.x, vertex.y, mid.x, mid.y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

private data class Contributor(
    val login: String,
    val avatarUrl: String,
    val htmlUrl: String,
    val contributions: Int,
)

private const val CONTRIBUTORS_API =
    "https://api.github.com/repos/holyschool/app/contributors?per_page=100"

@Composable
private fun ContributorsSection(onOpenLink: (String) -> Unit) {
    var contributors by remember { mutableStateOf<List<Contributor>>(emptyList()) }

    LaunchedEffect(Unit) {
        contributors = withContext(Dispatchers.IO) { fetchContributors() }
            .filterNot { it.login.equals("FoxyIsCoding", ignoreCase = true) }
    }

    if (contributors.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.about_contributors),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            contributors.forEachIndexed { index, contributor ->
                ContributorRow(contributor = contributor, onOpenLink = onOpenLink)
                if (index < contributors.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ContributorRow(contributor: Contributor, onOpenLink: (String) -> Unit) {
    val haptics = rememberAppHaptics()
    val avatar = rememberRemoteImage(contributor.avatarUrl)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                haptics.virtualKey()
                onOpenLink(contributor.htmlUrl)
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(36.dp),
        ) {
            if (avatar != null) {
                Image(
                    bitmap = avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contributor.login,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.about_contributions, contributor.contributions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun fetchContributors(): List<Contributor> = runCatching {
    val connection = (java.net.URL(CONTRIBUTORS_API).openConnection() as HttpURLConnection).apply {
        setRequestProperty("User-Agent", "Edupage2-App")
        setRequestProperty("Accept", "application/vnd.github+json")
        connectTimeout = 8000
        readTimeout = 8000
    }
    val body = connection.inputStream.bufferedReader().use { it.readText() }
    val array = JSONArray(body)
    (0 until array.length()).mapNotNull { index ->
        val obj = array.optJSONObject(index) ?: return@mapNotNull null
        if (obj.optString("type").equals("Bot", ignoreCase = true)) return@mapNotNull null
        val login = obj.optString("login").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Contributor(
            login = login,
            avatarUrl = obj.optString("avatar_url"),
            htmlUrl = obj.optString("html_url"),
            contributions = obj.optInt("contributions"),
        )
    }
}.getOrElse { emptyList() }
