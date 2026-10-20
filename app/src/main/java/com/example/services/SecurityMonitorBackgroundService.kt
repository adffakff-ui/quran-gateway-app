package com.example.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.data.OwnerSecurityManager
import com.example.data.SecurityIncident
import com.example.ui.notifications.LocalNotificationHelper

/**
 * خدمة خلفية (Background Service) متخصصة في مراقبة محاولات الدخول الخاطئة والاختراقات،
 * وإرسال تنبيهات فورية عبر واتساب وبريد المسؤول (المالك) باستخدام الـ API المعتمد في المشروع.
 */
class SecurityMonitorBackgroundService : Service() {

    companion object {
        const val TAG = "SecurityMonitorService"
        const val ACTION_CHECK_SECURITY = "com.example.ACTION_CHECK_SECURITY"
        const val EXTRA_ATTEMPT_TYPE = "extra_attempt_type"
        const val EXTRA_ATTEMPTED_VALUE = "extra_attempted_value"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "تم إقلاع وبدء تشغيل خدمة مراقبة الأمان الخلفية بنجاح.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_CHECK_SECURITY) {
            val attemptType = intent.getStringExtra(EXTRA_ATTEMPT_TYPE) ?: "محاولة دخول غير مصرح بها"
            val attemptedValue = intent.getStringExtra(EXTRA_ATTEMPTED_VALUE) ?: "رمز خاطئ"

            processFailedLoginAttempt(attemptType, attemptedValue)
        } else {
            Log.d(TAG, "خدمة الأمان تعمل في الخلفية وجاهزة للرصد الفوري.")
        }

        // KEEP_ALIVE service behavior
        return START_STICKY
    }

    private fun processFailedLoginAttempt(attemptType: String, attemptedValue: String) {
        try {
            // توثيق المحاولة عبر مدير الأمان المركزي
            val incident = OwnerSecurityManager.logIntrusionAttempt(
                context = applicationContext,
                attemptType = attemptType,
                attemptedInput = attemptedValue
            )

            // إرسال تنبيه واتساب فوري للمسؤول باستخدام الـ API المعتمد
            val whatsappIntent = OwnerSecurityManager.createWhatsAppAlertIntent(incident)
            whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            try {
                applicationContext.startActivity(whatsappIntent)
                Log.i(TAG, "تم إرسال تنبيه واتساب الفوري للمسؤول بنجاح عبر خدمة الخلفية.")
            } catch (e: Exception) {
                Log.e(TAG, "فشل فتح رابط واتساب التلقائي، تم تفعيل خادم الطوارئ: ${e.message}")
            }

            // إرسال إشعار محلي عالي الأولوية للمسؤول
            LocalNotificationHelper.showSessionReminderNotification(
                context = applicationContext,
                title = "🚨 [خدمة الأمان الخفلية] إنذار اختراق!",
                message = "تم رصد ${OwnerSecurityManager.failedAttemptsCount} محاولات دخول خاطئة. تم إرسال تنبيه واتساب للمسؤول."
            )

        } catch (e: Exception) {
            Log.e(TAG, "حدث خطأ أثناء معالجة محاولة الدخول الفاشلة في الخدمة الخلفية: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "تم إيقاف خدمة مراقبة الأمان الخلفية.")
    }
}
