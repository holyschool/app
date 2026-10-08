package com.enderplusbayzuiship.edupage2.notification

import android.util.Log

object NotificationErrorHandler {
    private const val TAG = "NotificationError"

    enum class ErrorSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    enum class ErrorType {
        NETWORK_ERROR,
        AUTHENTICATION_ERROR,
        SESSION_EXPIRED,
        API_ERROR,
        PARSING_ERROR,
        SYSTEM_ERROR,
        PERMISSION_ERROR,
        BATTERY_OPTIMIZATION,
        WORKER_FAILURE
    }

    data class NotificationError(
        val type: ErrorType,
        val severity: ErrorSeverity,
        val message: String,
        val context: String = "",
        val throwable: Throwable? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val canRetry: Boolean = true
    )

    fun handleError(
        error: NotificationError,
        retryAction: (() -> Unit)? = null
    ) {

        val logMessage = buildErrorMessage(error)

        when (error.severity) {
            ErrorSeverity.LOW -> {
                Log.i(TAG, logMessage, error.throwable)
            }
            ErrorSeverity.MEDIUM -> {
                Log.w(TAG, logMessage, error.throwable)
            }
            ErrorSeverity.HIGH, ErrorSeverity.CRITICAL -> {
                Log.e(TAG, logMessage, error.throwable)
            }
        }

        if (error.canRetry && retryAction != null && error.severity >= ErrorSeverity.HIGH) {
            Log.i(TAG, "Scheduling retry for: ${error.context}")
            retryAction.invoke()
        }
    }

    fun createNetworkError(
        context: String,
        throwable: Throwable? = null,
        canRetry: Boolean = true
    ) = NotificationError(
        type = ErrorType.NETWORK_ERROR,
        severity = ErrorSeverity.MEDIUM,
        message = "Network operation failed",
        context = context,
        throwable = throwable,
        canRetry = canRetry
    )

    fun createAuthError(
        context: String,
        throwable: Throwable? = null
    ) = NotificationError(
        type = ErrorType.AUTHENTICATION_ERROR,
        severity = ErrorSeverity.HIGH,
        message = "Authentication failed",
        context = context,
        throwable = throwable,
        canRetry = true
    )

    fun createSessionExpiredError(
        context: String,
        throwable: Throwable? = null
    ) = NotificationError(
        type = ErrorType.SESSION_EXPIRED,
        severity = ErrorSeverity.HIGH,
        message = "Session expired, re-authentication required",
        context = context,
        throwable = throwable,
        canRetry = true
    )

    fun createBatteryOptimizationError(
        context: String
    ) = NotificationError(
        type = ErrorType.BATTERY_OPTIMIZATION,
        severity = ErrorSeverity.HIGH,
        message = "App is being battery optimized, background processing may be limited",
        context = context,
        canRetry = false
    )

    fun createWorkerError(
        workerName: String,
        throwable: Throwable? = null,
        canRetry: Boolean = true
    ) = NotificationError(
        type = ErrorType.WORKER_FAILURE,
        severity = ErrorSeverity.MEDIUM,
        message = "Worker execution failed",
        context = "Worker: $workerName",
        throwable = throwable,
        canRetry = canRetry
    )

    fun createServiceError(
        serviceName: String,
        throwable: Throwable? = null,
        canRetry: Boolean = true
    ) = NotificationError(
        type = ErrorType.SYSTEM_ERROR,
        severity = ErrorSeverity.MEDIUM,
        message = "Service error occurred",
        context = "Service: $serviceName",
        throwable = throwable,
        canRetry = canRetry
    )

    fun createSchedulerError(
        operation: String,
        throwable: Throwable? = null,
        canRetry: Boolean = true
    ) = NotificationError(
        type = ErrorType.SYSTEM_ERROR,
        severity = ErrorSeverity.MEDIUM,
        message = "Scheduler operation failed",
        context = "Operation: $operation",
        throwable = throwable,
        canRetry = canRetry
    )

    private fun buildErrorMessage(error: NotificationError): String {
        return buildString {
            append("[${error.type}] ")
            append(error.message)
            if (error.context.isNotEmpty()) {
                append(" | Context: ${error.context}")
            }
            append(" | Severity: ${error.severity}")
            append(" | Retry: ${error.canRetry}")
            if (error.throwable != null) {
                append(" | Cause: ${error.throwable.message}")
            }
        }
    }
}

