package com.enderplusbayzuiship.edupage2.ui.util

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

