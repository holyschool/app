package com.wiffles.edupage.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edupage.api.Edupage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads EduPage-hosted images once and hands back a memory-cached, downsampled
 * [Bitmap]. Used for message-attachment and cloud-file previews, which would otherwise
 * require a full download to view.
 */
@Singleton
class ThumbnailLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
) {
    companion object {
        private const val TAG = "ThumbnailLoader"
        private const val MAX_CACHE_BYTES = 24 * 1024 * 1024
    }

    private val memory = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
    }

    private val inFlight = ConcurrentHashMap<String, Deferred<Bitmap?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun cached(key: String): Bitmap? = memory.get(key)

    /** Downloads [remotePath] if needed and returns a downsampled bitmap cached under [key]. */
    suspend fun load(key: String, remotePath: String, targetPx: Int = 512): Bitmap? {
        memory.get(key)?.let { return it }
        val deferred = inFlight.getOrPut(key) {
            // runCatching keeps an inline async body from cancelling the parent scope.
            scope.async {
                runCatching { fetch(key, remotePath, targetPx) }
                    .onFailure { Log.w(TAG, "thumbnail failed for $remotePath: ${it.message}") }
                    .getOrNull()
            }
        }
        val bitmap = try {
            deferred.await()
        } finally {
            inFlight.remove(key)
        }
        bitmap?.let { memory.put(key, it) }
        return bitmap
    }

    /** Warms several thumbnails at once (e.g. when a gallery page is shown). */
    suspend fun warm(keysAndPaths: List<Pair<String, String>>) = kotlinx.coroutines.coroutineScope {
        keysAndPaths.map { (key, path) -> async { load(key, path) } }.awaitAll()
    }

    private suspend fun fetch(key: String, remotePath: String, targetPx: Int): Bitmap? {
        val dir = File(context.cacheDir, "thumbs").apply { mkdirs() }
        val file = File(dir, Integer.toHexString(key.hashCode()) + ".img")
        if (!file.exists() || file.length() == 0L) {
            edupage.cloudDownload(remotePath, file)
        }
        if (file.length() == 0L) return null
        return decodeSampled(file, targetPx)
    }

    private fun decodeSampled(file: File, targetPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        var largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / (sample * 2) >= targetPx) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThumbnailLoaderEntryPoint {
    fun thumbnailLoader(): ThumbnailLoader
}

@Composable
fun rememberThumbnailLoader(): ThumbnailLoader {
    val context = LocalContext.current
    return remember(context) {
        dagger.hilt.android.EntryPointAccessors
            .fromApplication(context.applicationContext, ThumbnailLoaderEntryPoint::class.java)
            .thumbnailLoader()
    }
}

/**
 * Renders a remote EduPage image as a thumbnail, showing a placeholder while it loads.
 * The image is downloaded via the shared [ThumbnailLoader] and cached in memory/on disk.
 */
@Composable
fun ThumbnailImage(
    cacheKey: String,
    remotePath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    targetPx: Int = 512,
) {
    val loader = rememberThumbnailLoader()
    var bitmap by remember(cacheKey) { mutableStateOf(loader.cached(cacheKey)) }

    LaunchedEffect(cacheKey, remotePath) {
        if (bitmap == null) bitmap = loader.load(cacheKey, remotePath, targetPx)
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}