package com.example.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * خدمة إرسال واستقبال الإشعارات الفورية (FCM Service)
 */
class SocietyFCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("SocietyFCMService", "From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("SocietyFCMService", "Message Notification Body: ${it.body}")
            // هنا يمكنك إضافة منطق عرض الإشعار محلياً أو تحديث واجهة المستخدم
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("SocietyFCMService", "Refreshed token: $token")
        // هنا يمكنك إرسال التوكن الجديد إلى خادمك
    }
}
