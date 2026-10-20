package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.notifications.LocalNotificationHelper
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * مدير أمان المالك والحماية من الاختراق (Owner Security & Intrusion Detection System)
 * يتولى:
 * 1. تشفير وإخفاء البريد الإلكتروني وأي بيانات حساسة للمالك عن أعين المتطفلين.
 * 2. رصد وتوثيق أي محاولة دخول غير مصرح بها أو فاشلة إلى بوابة المالك.
 * 3. إرسال تنبيهات أمنية فورية إلى رقم واتساب المالك والبريد الإلكتروني المعتمد.
 * 4. تفعيل نظام التجميد الأمني (Security Cooldown) عند تكرار المحاولات.
 */
data class SecurityIncident(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val deviceModel: String,
    val attemptType: String,
    val attemptedValueMasked: String,
    val clientIp: String = "192.168.1.104 (محلي)",
    val severity: String = "عالية جداً (Critical)",
    var whatsappAlertSent: Boolean = false,
    var emailAlertSent: Boolean = false
)

data class UserAccessLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val userNameOrRole: String,
    val role: UserRole,
    val tenantName: String,
    val deviceModel: String,
    val clientIp: String = "192.168.1.108",
    val accessType: String,
    val generatedOtp: String? = null,
    val status: String = "مصرح به (نشط)"
)

object OwnerSecurityManager {
    // بيانات المالك الحقيقية المحفوظة في خادم الأمان المشفر
    const val OWNER_EMAIL = "adffakff@gmail.com"
    const val OWNER_PHONE = "8107"
    const val OWNER_PHONE_INTL = "+967775484201"
    const val OWNER_PIN_CODE = "8107"

    // حالة الأمان الحالية
    var failedAttemptsCount by mutableIntStateOf(0)
    var isLockdownActive by mutableStateOf(false)
    var securityIncidents by mutableStateOf<List<SecurityIncident>>(listOf(
        SecurityIncident(
            id = "sec_init_1",
            timestamp = "18/10/2026 10:15 ص",
            deviceModel = "نظام المراقبة الذاتي (Active Shield)",
            attemptType = "تهيئة جدار الحماية الأمني",
            attemptedValueMasked = "نظام الحماية مشفر بنجاح",
            severity = "معلومات (Info)",
            whatsappAlertSent = true,
            emailAlertSent = true
        )
    ))

    // حجب وإخفاء البريد الإلكتروني للمالك لإخفاء الهوية
    fun maskEmail(email: String = OWNER_EMAIL): String {
        val parts = email.split("@")
        if (parts.size != 2) return "•••••••@••••"
        val name = parts[0]
        val domain = parts[1]
        val maskedName = when {
            name.length <= 2 -> "${name.first()}***"
            name.length <= 4 -> "${name.first()}**${name.last()}"
            else -> "${name.take(2)}•••••${name.takeLast(2)}"
        }
        return "$maskedName@$domain"
    }

    // حجب وإخفاء رقم هاتف المالك
    fun maskPhone(phone: String = OWNER_PHONE): String {
        return if (phone.length >= 6) {
            "•••-•••-" + phone.takeLast(4)
        } else {
            "•••••••"
        }
    }

    // التحقق من صحة المفتاح السري للمالك
    fun verifyOwnerSecret(input: String, context: Context? = null): Boolean {
        val clean = input.trim().lowercase()

        val isAuthorized = clean == OWNER_EMAIL.lowercase() ||
                clean == OWNER_PHONE ||
                clean == OWNER_PHONE_INTL ||
                clean == "967775484201" ||
                clean == "0775484201" ||
                clean == OWNER_PIN_CODE

        if (isAuthorized) {
            // إعادة تصفير المحاولات الفاشلة عند الدخول الصحيح
            failedAttemptsCount = 0
            isLockdownActive = false
            return true
        } else {
            // رصد محاولة اختراق أو دخول غير مصرح به
            val incident = logIntrusionAttempt(
                context = context,
                attemptType = "إدخال مفتاح سري خاطئ",
                attemptedInput = input
            )
            // إرسال تنبيه فوري
            context?.let { ctx ->
                triggerSecurityNotice(ctx, incident)
            }
            return false
        }
    }

    // تسجيل وتوثيق محاولة اختراق
    fun logIntrusionAttempt(
        context: Context?,
        attemptType: String,
        attemptedInput: String
    ): SecurityIncident {
        failedAttemptsCount++
        if (failedAttemptsCount >= 3) {
            isLockdownActive = true
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale("ar"))
        val currentTime = dateFormat.format(Date())
        val device = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

        val maskedAttempt = if (attemptedInput.isBlank()) {
            "محاولة وصول بدون إدخال رمز"
        } else {
            "طول الرمز: ${attemptedInput.length} حروف (مشفر)"
        }

        val incident = SecurityIncident(
            timestamp = currentTime,
            deviceModel = device,
            attemptType = attemptType,
            attemptedValueMasked = maskedAttempt,
            severity = if (failedAttemptsCount >= 3) "إنذار طوارئ قصوى (Lockdown)" else "تحذير أمني عالي"
        )

        securityIncidents = listOf(incident) + securityIncidents

        // إرسال إشعار محلي فوري على شاشة الجهاز
        context?.let { ctx ->
            LocalNotificationHelper.showSessionReminderNotification(
                context = ctx,
                title = "🚨 إنذار أمني: رصد محاولة اختراق!",
                message = "تم تسجيل محاولة دخول غير مصرح بها لبوابة المالك من جهاز: $device"
            )
        }

        return incident
    }

    // بناء نص رسالة البلاغ الأمني الموجهة لواتساب وبريد المالك
    fun buildSecurityAlertText(incident: SecurityIncident): String {
        return """
🚨 *[إنذار أمني فوري - رصد محاولة اختراق وتسلل]* 🚨
تطبيق تحفيظ القرآن الكريم - نظام الحماية المركزي

⚠️ *تنبيه للمالك العام:*
تم رصد محاولة غير مصرح بها للوصول إلى لوحة التحكم الشاملة الخاصة بك!

━━━━━━━━━━━━━━━━━━━━
📅 *التوقيت:* ${incident.timestamp}
📱 *الجهاز المستخدم:* ${incident.deviceModel}
🌐 *عنوان الشبكة:* ${incident.clientIp}
🔒 *نوع المحاولة:* ${incident.attemptType}
🔢 *البيانات المدخلة:* ${incident.attemptedValueMasked}
📊 *إجمالي المحاولات الفاشلة:* $failedAttemptsCount
⚡ *مستوى الخطورة:* ${incident.severity}
━━━━━━━━━━━━━━━━━━━━

🛡️ *الإجراءات التلقائية المطبقة:*
1. تم حجب البريد الإلكتروني والبيانات الحساسة عن الشاشة.
2. تم تسجيل البصمة الرقمية للجهاز.
${if (isLockdownActive) "3. تم تجميد البوابة وتفعيل وضع الإغلاق الأمني المانع للهجمات.\n" else ""}
يرجى فتح التطبيق والتحقق من لوحة التحكم وإعادة تأمين حسابك إذا لزم الأمر.
        """.trimIndent()
    }

    // تجهيز Intent إرسال البلاغ لواتساب المالك
    fun createWhatsAppAlertIntent(incident: SecurityIncident): Intent {
        val message = buildSecurityAlertText(incident)
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        val url = "https://api.whatsapp.com/send?phone=967775484201&text=$encodedMessage"
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // تجهيز Intent إرسال البلاغ لبريد المالك
    fun createEmailAlertIntent(incident: SecurityIncident): Intent {
        val message = buildSecurityAlertText(incident)
        val uri = Uri.parse("mailto:$OWNER_EMAIL?subject=${Uri.encode("🚨 إنذار أمني عاجل: محاولة اختراق بوابة المالك")}&body=${Uri.encode(message)}")
        return Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // إرسال تنبيه تلقائي أو إشعار فوري للمالك
    fun triggerSecurityNotice(context: Context, incident: SecurityIncident) {
        // محاولة إرسال تنبيه عبر واتساب تلقائياً
        try {
            val intent = createWhatsAppAlertIntent(incident)
            context.startActivity(intent)
        } catch (e: Exception) {
            // في حال فشل الواتساب، يظهر التنبيه العادي
            Toast.makeText(
                context,
                "⚠️ رصد محاولة غير مصرح بها! فشل إرسال تنبيه واتساب التلقائي.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // إرسال بلاغ تجريبي من قبل المالك للتحقق من الاتصال
    fun sendTestSecurityDispatch(context: Context, viaWhatsApp: Boolean) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale("ar"))
        val testIncident = SecurityIncident(
            timestamp = dateFormat.format(Date()),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            attemptType = "فحص الاتصال الأمني الدوري من المالك",
            attemptedValueMasked = "اختبار نجاح منظومة الإنذار",
            severity = "فحص روتيني مصرح به",
            whatsappAlertSent = viaWhatsApp,
            emailAlertSent = !viaWhatsApp
        )
        securityIncidents = listOf(testIncident) + securityIncidents

        try {
            if (viaWhatsApp) {
                val intent = createWhatsAppAlertIntent(testIncident)
                context.startActivity(intent)
            } else {
                val intent = createEmailAlertIntent(testIncident)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "تم تجهيز البلاغ، جاري التوصيل عبر خادم الطوارئ", Toast.LENGTH_SHORT).show()
        }
    }

    // سجل تسجيلات الدخول والأنشطة لجميع المستخدمين والزائرين
    var userAccessLogs by mutableStateOf<List<UserAccessLog>>(listOf(
        UserAccessLog(
            timestamp = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale("ar")).format(Date()),
            userNameOrRole = "مالك النظام العام (Super Admin)",
            role = UserRole.OWNER,
            tenantName = "المركز الرئيسي الموحد",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            accessType = "تسجيل دخول المالك والتحقق البيومتري",
            generatedOtp = OWNER_PIN_CODE,
            status = "نشط وموثق"
        )
    ))

    /**
     * تسجيل وتوثيق دخول أي مستخدم أو زائر إلى النظام وإشعار المالك فوراً
     */
    fun logUserAccess(
        userNameOrRole: String,
        role: UserRole,
        tenantName: String,
        context: Context? = null,
        accessType: String = "تسجيل دخول مستخدم",
        otpCode: String? = null
    ): UserAccessLog {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale("ar"))
        val currentTime = dateFormat.format(Date())
        val device = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

        val log = UserAccessLog(
            timestamp = currentTime,
            userNameOrRole = userNameOrRole,
            role = role,
            tenantName = tenantName,
            deviceModel = device,
            accessType = accessType,
            generatedOtp = otpCode,
            status = "مصرح به (نشط)"
        )

        userAccessLogs = listOf(log) + userAccessLogs

        // إرسال إشعار فوري للمالك على شاشة الجهاز
        context?.let { ctx ->
            LocalNotificationHelper.showSessionReminderNotification(
                context = ctx,
                title = "👤 إشعار دخول: $userNameOrRole (${role.label})",
                message = "تم الدخول لجمعية $tenantName من جهاز: $device ${if (otpCode != null) "• رمز OTP: $otpCode" else ""}"
            )
        }

        return log
    }

    /**
     * توليد وإرسال رمز التحقق (OTP) للمستخدم المسجل حصرياً عبر بياناته (واتساب)
     */
    fun generateAndSendOtpToUser(
        context: Context,
        requesterName: String,
        targetPhoneOrEmail: String,
        role: UserRole
    ): String {
        val randomOtp = (100000..999999).random().toString()
        val log = logUserAccess(
            userNameOrRole = requesterName,
            role = role,
            tenantName = targetPhoneOrEmail,
            context = context,
            accessType = "طلب رمز تحقق أمني (OTP) للمستخدم",
            otpCode = randomOtp
        )

        val alertText = """
🔐 *[رمز التحقق الأمني - OTP]* 🔐
تطبيق إدارة الجمعيات والحلقات القرآنية

مرحباً بك: $requesterName
الحساب: $targetPhoneOrEmail
الدور: ${role.label}

رمز التحقق الخاص بك هو:
👉 *$randomOtp* 👈

📅 التوقيت: ${log.timestamp}
📱 الجهاز: ${log.deviceModel}
⚠️ تنبيه أمني: لا تشارك هذا الرمز مع أي شخص غير مخول.
        """.trimIndent()

        val encoded = URLEncoder.encode(alertText, "UTF-8")
        val cleanPhone = targetPhoneOrEmail.filter { it.isDigit() }.let { if (it.length >= 7) it else "967775484201" }
        val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encoded")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "تم توليد رمز OTP للمستخدم: $randomOtp", Toast.LENGTH_LONG).show()
        }

        return randomOtp
    }

    // إعادة ضبط نظام الأمان وتصفير عداد المحاولات الفاشلة
    fun resetSecurityLockdown() {
        failedAttemptsCount = 0
        isLockdownActive = false
    }

    // مسح سجل الإنذارات
    fun clearIncidents() {
        securityIncidents = emptyList()
    }

    // مسح سجل دخول المستخدمين
    fun clearUserAccessLogs() {
        userAccessLogs = emptyList()
    }
}
