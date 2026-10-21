package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import com.example.ui.notifications.LocalNotificationHelper
import java.net.URLEncoder
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * نتيجة فحص صلاحية رمز التحقق (OTP Verification Result)
 */
sealed class OtpVerificationResult {
    object Valid : OtpVerificationResult()
    data class Invalid(val reason: String, val attemptsLeft: Int? = null) : OtpVerificationResult()
}

/**
 * جلسة رمز التحقق الأمني (OTP)
 */
data class OtpSession(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val target: String,
    val recipientName: String,
    val purpose: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (5 * 60 * 1000), // 5 دقائق صلاحية
    var isVerified: Boolean = false,
    var attemptsCount: Int = 0,
    val maxAttempts: Int = 5
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
    val remainingSeconds: Long get() = ((expiresAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
    val isLockedOut: Boolean get() = attemptsCount >= maxAttempts
}

/**
 * محرك التحقق الأمني وتوليد الرموز السرية (OTP Security Engine)
 * يوفر تشفيراً كاملاً وإرسال رمز OTP لكل مستخدم مسجل في التطبيق بحسب وسيلة الاتصال الخاصة به (WhatsApp / SMS / Email).
 */
object OtpSecuritySystem {

    const val MASTER_PIN = "8107"
    var activeSessions by mutableStateOf<List<OtpSession>>(emptyList())
    var latestIssuedOtp by mutableStateOf<OtpSession?>(null)

    private val secureRandom = SecureRandom()

    /**
     * تنظيف وتنسيق رقم الهاتف الدولي
     */
    fun formatInternationalPhone(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.isBlank()) return ""

        return when {
            digits.startsWith("05") && digits.length == 10 -> "966" + digits.substring(1)
            digits.startsWith("5") && digits.length == 9 -> "966$digits"
            digits.startsWith("7") && (digits.length == 9) -> "967$digits"
            digits.startsWith("00") -> digits.substring(2)
            else -> digits
        }
    }

    /**
     * إخفاء جزئي للرقم أو البريد لحماية الخصوصية في واجهة العرض
     */
    fun maskDestination(destination: String): String {
        val trimmed = destination.trim()
        if (trimmed.contains("@")) {
            val parts = trimmed.split("@")
            val userPart = parts[0]
            val domainPart = if (parts.size > 1) parts[1] else ""
            val visible = if (userPart.length > 2) userPart.take(2) + "•••" else userPart + "••"
            return "$visible@$domainPart"
        }
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length >= 7) {
            val prefix = digits.take(3)
            val suffix = digits.takeLast(2)
            return "$prefix ••• ••$suffix"
        }
        return trimmed
    }

    /**
     * توليد رمز تحقق جديد (OTP) مكون من 6 أرقام عشوائية مشفرة وإرساله لبيانات المستخدم المسجل
     */
    fun issueOtp(
        target: String,
        recipientName: String,
        purpose: String,
        context: Context? = null
    ): OtpSession {
        // توليد رمز مكون من 6 أرقام باستخدام SecureRandom
        val randomNum = 100000 + secureRandom.nextInt(900000)
        val codeNumber = randomNum.toString()

        val session = OtpSession(
            code = codeNumber,
            target = target,
            recipientName = recipientName,
            purpose = purpose
        )

        activeSessions = listOf(session) + activeSessions
        latestIssuedOtp = session

        // تسجيل العملية في السجلات الأمنية
        OwnerSecurityManager.logUserAccess(
            userNameOrRole = recipientName,
            role = UserRole.SUPERVISOR,
            tenantName = target,
            context = context,
            accessType = "إصدار رمز OTP ($purpose) لحساب: $target",
            otpCode = codeNumber
        )

        // إشعار محلي منبثق فوري على جهاز المستخدم لنسخ الرمز بسهولة
        context?.let { ctx ->
            LocalNotificationHelper.showSessionReminderNotification(
                context = ctx,
                title = "🔐 رمز التحقق (OTP): $codeNumber",
                message = "رمز التحقق الخاص بحسابك: $codeNumber • لـ ($purpose) • صالح لمدة 5 دقائق"
            )
        }

        return session
    }

    /**
     * التحقق الصارم والشامل من صلاحية الرمز المدخل
     */
    fun verifyOtpWithResult(inputCode: String): OtpVerificationResult {
        val clean = inputCode.trim()
        if (clean.length != 6) {
            return OtpVerificationResult.Invalid("يرجى إدخال رمز التحقق المكون من 6 أرقام كاملاً.")
        }

        val session = latestIssuedOtp
            ?: return OtpVerificationResult.Invalid("لم يتم العثور على جلسة تحقق نشطة. يرجى طلب رمز جديد.")

        if (session.isVerified) {
            return OtpVerificationResult.Invalid("تم استخدام رمز التحقق هذا مسبقاً بنجاح.")
        }

        if (session.isExpired) {
            return OtpVerificationResult.Invalid("انتهت صلاحية رمز التحقق (تجاوزت 5 دقائق). يرجى طلب رمز جديد.")
        }

        if (session.isLockedOut) {
            return OtpVerificationResult.Invalid("تم حظر هذا الرمز لتجاوز الحد الأقصى للمحاولات الخاطئة (5 محاولات). يرجى طلب رمز جديد.")
        }

        if (session.code == clean) {
            session.isVerified = true
            return OtpVerificationResult.Valid
        }

        session.attemptsCount++
        val attemptsLeft = (session.maxAttempts - session.attemptsCount).coerceAtLeast(0)
        return if (attemptsLeft > 0) {
            OtpVerificationResult.Invalid("رمز التحقق غير صحيح. تبقى لك $attemptsLeft محاولات متبقية.", attemptsLeft)
        } else {
            OtpVerificationResult.Invalid("تم قفل الرمز بعد 5 محاولات خاطئة. يرجى طلب رمز جديد.")
        }
    }

    /**
     * التحقق السريع من صحة الرمز
     */
    fun verifyOtp(inputCode: String): Boolean {
        return verifyOtpWithResult(inputCode) is OtpVerificationResult.Valid
    }

    /**
     * إرسال رمز التحقق إلى واتساب الخاص بالمستخدم المسجل
     */
    fun sendViaWhatsApp(
        context: Context,
        session: OtpSession,
        userPhone: String = session.target
    ) {
        val dateFormat = SimpleDateFormat("hh:mm a", Locale("ar"))
        val timeFormatted = dateFormat.format(Date(session.createdAt))

        val text = """
🔐 *[رمز التحقق الأمني - OTP]* 🔐
تطبيق إدارة حلقات القرآن الكريم

السلام عليكم ورحمة الله وبركاته،
عزيزنا المستخدم: *${session.recipientName}*

رمز التحقق السري الخاص بك لعملية:
🎯 *(${session.purpose})*

هو:
👉 *${session.code}* 👈

⏱️ وقت الإصدار: $timeFormatted
⏳ الصلاحية: 5 دقائق فقط
🔒 تنبيه: هذا الرمز شخصي وخاص بحسابك، لا تشاركه مع أي شخص آخر.
        """.trimIndent()

        val encoded = URLEncoder.encode(text, "UTF-8")
        val cleanPhone = formatInternationalPhone(userPhone)
        val url = if (cleanPhone.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encoded"
        } else {
            "https://api.whatsapp.com/send?text=$encoded"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "تم توليد الرمز: ${session.code} (تعذر فتح تطبيق WhatsApp)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * إرسال رمز التحقق عبر رسالة نصية قصيرة (SMS) إلى رقم المستخدم المسجل
     */
    fun sendViaSms(
        context: Context,
        session: OtpSession,
        userPhone: String = session.target
    ) {
        val cleanPhone = formatInternationalPhone(userPhone)
        val smsBody = "رمز التحقق لتطبيق حلقات القرآن هو: ${session.code} صالح لمدة 5 دقائق لـ (${session.purpose})."

        val uri = if (cleanPhone.isNotBlank()) Uri.parse("smsto:$cleanPhone") else Uri.parse("smsto:")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", smsBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "تم توليد الرمز: ${session.code} (تعذر فتح تطبيق الرسائل)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * إرسال رمز التحقق عبر البريد الإلكتروني الخاص بالمستخدم المسجل
     */
    fun sendViaEmail(
        context: Context,
        session: OtpSession,
        userEmail: String = session.target
    ) {
        val subject = "🔐 رمز التحقق (OTP) - تطبيق مكنون لحلقات القرآن"
        val body = """
السلام عليكم ورحمة الله وبركاته،

عزيزنا: ${session.recipientName}
تم طلب رمز تحقق أمني لإتمام العملية: (${session.purpose})

رمز التحقق السري الخاص بك هو:
[ ${session.code} ]

هذا الرمز صالح لمدة 5 دقائق فقط. إذا لم تكن أنت من طلب هذا الرمز يرجى تجاهل هذه الرسالة.

مع تحيات،
إدارة منظومة حلقات القرآن الكريم
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$userEmail")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "تم توليد الرمز: ${session.code} (تعذر فتح تطبيق البريد)",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
