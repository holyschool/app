package com.wiffles.edupage.ui.util

import android.content.Context
import com.wiffles.edupage.R
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

fun Throwable.isNetworkError(): Boolean = when (this) {
    is UnknownHostException,
    is SocketTimeoutException,
    is SSLException,
    is IOException -> true
    else -> false
}

/**
 * Picks a user-facing message for a failed load:
 *  - offline with nothing cached  -> "not saved for offline yet"
 *  - a connectivity failure        -> the generic network error
 *  - anything else                 -> the caller's fallback message
 */
fun loadErrorMessage(context: Context, error: Throwable?, fallbackRes: Int): String = when {
    !ConnectivityObserver.isOnline.value -> context.getString(R.string.offline_no_cache)
    error?.isNetworkError() == true -> context.getString(R.string.network_error)
    else -> error?.message ?: context.getString(fallbackRes)
}