package com.wiffles.edupage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application-lifetime coroutine scope for work that must outlive the component that
 * starts it (app startup, FCM callbacks, notification actions). Unlike the old
 * fire-and-forget `CoroutineScope(Dispatchers.IO)` instances this one has a
 * [SupervisorJob], so a failing child never cancels unrelated work.
 */
val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
