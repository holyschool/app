package com.wiffles.edupage.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wiffles.edupage.applicationScope
import com.wiffles.edupage.network.BackendRegistrationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var backendRegistrationManager: BackendRegistrationManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        applicationScope.launch {
            backendRegistrationManager.registerIfPossible(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        FirebaseNotificationHandler.showNotification(
            context = this,
            data = message.data,
            title = message.notification?.title ?: message.data["title"],
            body = message.notification?.body ?: message.data["body"],
        )

    }
}

