package com.enderplusbayzuiship.edupage2.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.enderplusbayzuiship.edupage2.network.BackendRegistrationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var backendRegistrationManager: BackendRegistrationManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            backendRegistrationManager.registerIfPossible(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        FirebaseNotificationHandler.showNotification(
            context = this,
            data = message.data,
            title = message.notification?.title,
            body = message.notification?.body,
        )
    }
}
