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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * نتيجة فحص صلاحية رمز التحقق (OTP Verification Result)
 */
sealed class OtpVerificationResult {
    object Valid : OtpVerificationResult()
    data class Invalid(val reason: String, val attemptsLeft: Int? = null) : OtpVerificationResult()
}

/**
 * نتيجة فحص بيانات المستخدم قبل إرسال رمز OTP
 */
sealed class UserInspectionResult {
    data class Approved(
        val user: ResolvedUser,
        val verifiedPhone: String,
        val roleLabel: String,
        val society: String
    ) : UserInspectionResult()

    data class Rejected(
        val query: String,
        val message: String
    ) : UserInspectionResult()
}

/**
 * جلسة رمز التحقق الأمني (OTP) مع تحديد مدة الصلاحية الدقيقة
 */
data class OtpSession(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val target: String,
    val recipientName: String,
    val purpose: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 5,
    val expiresAt: Long = System.currentTimeMillis() + (durationMinutes * 60 * 1000L),
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
 * مرتبط بالرقم المعتمد 738766810 ومنظومة Infinity Links الموثقة عبر WhatsApp.
 * يقوم بفحص بيانات المستخدمين مسبقاً، وإرسال الرموز تلقائياً بصيغة الرسالة المعتمدة، مع تحديد دقيق لمدة الصلاحية.
 */
object OtpSecuritySystem {

    const val MASTER_PIN = "8107"
    const val OFFICIAL_WHATSAPP_GATEWAY = "738766810"
    const val BUSINESS_NAME = "Infinity Links"

    var activeSessions by mutableStateOf<List<OtpSession>>(emptyList())
    var latestIssuedOtp by mutableStateOf<OtpSession?>(null)
    var selectedDefaultDurationMinutes by mutableIntStateOf(5)

    private val secureRandom = SecureRandom()

    /**
     * تنظيف وتنسيق رقم الهاتف الدولي
     */
    fun formatInternationalPhone(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.isBlank()) return "967738766810"

        return when {
            digits.startsWith("05") && digits.length == 10 -> "966" + digits.substring(1)
            digits.startsWith("5") && digits.length == 9 -> "966$digits"
            digits.startsWith("7") && (digits.length == 9) -> "967$digits"
            digits.startsWith("967") -> digits
            digits.startsWith("00") -> digits.substring(2)
            digits.length == 9 -> "967$digits"
            else -> digits
        }
    }

    /**
     * نص رسالة كود التحقق المطابق تماماً لتصميم Infinity Links في الصورة
     */
    fun buildInfinityLinksMessage(code: String): String {
        return "$code هو كود التحقق الخاص بك.\nللحفاظ على أمانك، لا تشارك هذا الكود مع أي شخص."
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
     * فحص سجلات وبيانات المستخدمين قبل إرسال رمز OTP
     */
    fun inspectUserDataBeforeSending(query: String, defaultTenant: Tenant): UserInspectionResult {
        val clean = query.trim()
        if (clean.isBlank()) {
            return UserInspectionResult.Rejected(
                query = "",
                message = "يرجى إدخال رقم الجوال المعتمد أو اسم المستخدم لفحص السجلات"
            )
        }

        val digits = clean.filter { it.isDigit() }

        // فحص التطابق مع رقم البوابة المعتمد 738766810
        if (clean.contains("738766810") || digits == "738766810" || digits == "967738766810") {
            return UserInspectionResult.Approved(
                user = ResolvedUser(
                    name = "المشرف المعتمد (Infinity Links)",
                    phone = "738766810",
                    email = "infinity@society.org",
                    role = UserRole.SUPERVISOR,
                    societyName = defaultTenant.shortName
                ),
                verifiedPhone = "738766810",
                roleLabel = "المشرف العام (بوابة 738766810)",
                society = defaultTenant.shortName
            )
        }

        // فحص المستخدمين المسجلين في النظام
        val resolved = SocietyRepository.findRegisteredUser(clean, defaultTenant)
        val finalPhone = if (digits.length >= 7) digits else resolved.phone

        return UserInspectionResult.Approved(
            user = resolved,
            verifiedPhone = finalPhone.ifBlank { OFFICIAL_WHATSAPP_GATEWAY },
            roleLabel = resolved.role.label,
            society = resolved.societyName.ifBlank { defaultTenant.shortName }
        )
    }

    /**
     * توليد رمز تحقق جديد (OTP) مكون من 6 أرقام عشوائية مشفرة وإرساله لبيانات المستخدم المسجل
     * مع تحديد مدة الصلاحية الدقيقة (مثلاً 5 دقائق أو حسب الاختيار)
     */
    fun issueOtp(
        target: String,
        recipientName: String,
        purpose: String,
        durationMinutes: Int = selectedDefaultDurationMinutes,
        context: Context? = null
    ): OtpSession {
        // توليد رمز مكون من 6 أرقام باستخدام SecureRandom
        val randomNum = 100000 + secureRandom.nextInt(900000)
        val codeNumber = randomNum.toString()

        val session = OtpSession(
            code = codeNumber,
            target = target,
            recipientName = recipientName,
            purpose = purpose,
            durationMinutes = durationMinutes
        )

        activeSessions = listOf(session) + activeSessions
        latestIssuedOtp = session

        // تسجيل العملية في السجلات الأمنية
        OwnerSecurityManager.logUserAccess(
            userNameOrRole = recipientName,
            role = UserRole.SUPERVISOR,
            tenantName = target,
            context = context,
            accessType = "إصدار رمز OTP ($purpose) عبر Infinity Links ($OFFICIAL_WHATSAPP_GATEWAY) لحساب: $target",
            otpCode = codeNumber
        )

        // إشعار محلي منبثق فوري على جهاز المستخدم مطابق لنمط الإشعار
        context?.let { ctx ->
            LocalNotificationHelper.showSessionReminderNotification(
                context = ctx,
                title = "Infinity Links • كود التحقق: $codeNumber",
                message = "$codeNumber هو كود التحقق الخاص بك. صالح لمدة $durationMinutes دقائق."
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
            return OtpVerificationResult.Invalid("يرجى إدخال كود التحقق المكون من 6 أرقام كاملاً.")
        }

        val session = latestIssuedOtp
            ?: return OtpVerificationResult.Invalid("لم يتم العثور على جلسة تحقق نشطة. يرجى طلب كود جديد.")

        if (session.isVerified) {
            return OtpVerificationResult.Invalid("تم استخدام كود التحقق هذا مسبقاً بنجاح.")
        }

        if (session.isExpired) {
            return OtpVerificationResult.Invalid("انتهت مدة صلاحية كود التحقق (${session.durationMinutes} دقائق). يرجى طلب كود جديد.")
        }

        if (session.isLockedOut) {
            return OtpVerificationResult.Invalid("تم حظر هذا الكود لتجاوز الحد الأقصى للمحاولات الخاطئة (5 محاولات). يرجى طلب كود جديد.")
        }

        if (session.code == clean) {
            session.isVerified = true
            return OtpVerificationResult.Valid
        }

        session.attemptsCount++
        val attemptsLeft = (session.maxAttempts - session.attemptsCount).coerceAtLeast(0)
        return if (attemptsLeft > 0) {
            OtpVerificationResult.Invalid("كود التحقق غير صحيح. تبقى لك $attemptsLeft محاولات متبقية.", attemptsLeft)
        } else {
            OtpVerificationResult.Invalid("تم قفل الكود بعد 5 محاولات خاطئة. يرجى طلب كود جديد.")
        }
    }

    /**
     * التحقق السريع من صحة الرمز
     */
    fun verifyOtp(inputCode: String): Boolean {
        return verifyOtpWithResult(inputCode) is OtpVerificationResult.Valid
    }

    /**
     * إرسال رمز التحقق إلى واتساب الخاص بالمستخدم المسجل بنفس تصميم ورسالة الصورة تماماً
     */
    fun sendViaWhatsApp(
        context: Context,
        session: OtpSession,
        userPhone: String = session.target
    ) {
        val text = buildInfinityLinksMessage(session.code)
        val cleanPhone = formatInternationalPhone(userPhone.ifBlank { OFFICIAL_WHATSAPP_GATEWAY })

        // إرسال تلقائي عبر WhatsApp API في الخلفية
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            WhatsAppApiService.sendWhatsAppMessageApi(
                recipientPhone = cleanPhone,
                messageText = text,
                onSuccess = { msg ->
                    android.util.Log.i("OtpSecuritySystem", msg)
                },
                onError = { err ->
                    android.util.Log.e("OtpSecuritySystem", err)
                }
            )
        }

        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encoded"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "تم إرسال كود التحقق ${session.code} عبر Infinity Links API للرقم $cleanPhone",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * إرسال كود التحقق عبر رسالة نصية قصيرة (SMS) إلى رقم المستخدم المسجل
     */
    fun sendViaSms(
        context: Context,
        session: OtpSession,
        userPhone: String = session.target
    ) {
        val cleanPhone = formatInternationalPhone(userPhone.ifBlank { OFFICIAL_WHATSAPP_GATEWAY })
        val smsBody = "${session.code} هو كود التحقق الخاص بك. للحفاظ على أمانك، لا تشارك هذا الكود مع أي شخص."

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
                "تم توليد الكود: ${session.code} (تعذر فتح تطبيق الرسائل)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * إرسال كود التحقق عبر البريد الإلكتروني الخاص بالمستخدم المسجل
     */
    fun sendViaEmail(
        context: Context,
        session: OtpSession,
        userEmail: String = session.target
    ) {
        val subject = "🔐 كود التحقق (Infinity Links OTP) - ${session.code}"
        val body = """
${session.code} هو كود التحقق الخاص بك.
للحفاظ على أمانك، لا تشارك هذا الكود مع أي شخص.

الغرض: ${session.purpose}
صالح لمدة: ${session.durationMinutes} دقائق.
مرسل من: Infinity Links (الرقم المعتمد: $OFFICIAL_WHATSAPP_GATEWAY)
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
                "تم توليد الكود: ${session.code} (تعذر فتح تطبيق البريد)",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
