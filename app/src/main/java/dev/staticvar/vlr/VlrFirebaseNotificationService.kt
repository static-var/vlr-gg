package dev.staticvar.vlr

import com.google.firebase.messaging.FirebaseMessagingService

class VlrFirebaseNotificationService(): FirebaseMessagingService() {
    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
    }
}