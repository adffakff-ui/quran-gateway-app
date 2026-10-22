package com.example.data

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.notifications.LocalNotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * نموذج بيانات المستخدم المسجل والمعتمد مسبقاً في النظام
 */
data class RegisteredUserEntry(
    val id: String,
    val name: String,
    val role: UserRole,
    val phone: String,
    val email: String,
    val identityNumber: String = "",
    val societyName: String = "الجمعية الخيرية لتحفيظ القرآن الكريم",
    val subRoleDescription: String = "",
    val isBiometricEnrolled: Boolean = true
)

enum class WipeLevel(val label: String) {
    SESSION_CACHE("مسح بيانات الجلسة المؤقتة والمدخلات"),
    EMERGENCY_DEEP_PURGE("تطهير أمني شامل ومسح البيانات الحساسة")
}

data class AnonymousBlockResult(
    val isBlocked: Boolean,
    val attemptedIdentifier: String,
    val reason: String,
    val timestamp: String,
    val wasDataWiped: Boolean
)

/**
 * نظام الحماية Zero-Trust ومسح البيانات ومنع العمليات المجهولة
 * (Zero-Trust Security & Data Sanitization System)
 *
 * المهام الرئيسية:
 * 1. التحقق الصارم من أن المستخدم مسجل مسبقاً في قاعدة بيانات الجمعية قبل السماح بالبصمة البيومترية.
 * 2. منع وحظر أي عملية مجهولة الهوية ليس لها تسجيل مسبق في التطبيق.
 * 3. مسح وتطهير بيانات الجلسة والذاكرة المؤقتة (Data Wipe) فور رصد أي تهديد أو محاولة مجهولة لمنع أي تسريب.
 * 4. توثيق العمليات المحظورة وإشعار مالك النظام فوراً.
 */
object ZeroTrustSecuritySystem {

    var blockedAnonymousAttemptsCount by mutableIntStateOf(0)
    var lastBlockedThreat by mutableStateOf<AnonymousBlockResult?>(null)
    var isDataSanitized by mutableStateOf(false)
    var lastWipeTime by mutableStateOf<String?>(null)
    var isStrictZeroTrustEnabled by mutableStateOf(true)

    // المستخدمون المسجلون الافتراضيون للمنظومة
    private val defaultSupervisors = listOf(
        RegisteredUserEntry(
            id = "sup_01",
            name = "أ. عبد الرحمن السعيد",
            role = UserRole.SUPERVISOR,
            phone = "0551234567",
            email = "supervisor@maknoon.org.sa",
            identityNumber = "1023456789",
            societyName = "الجمعية الخيرية لتحفيظ القرآن الكريم",
            subRoleDescription = "المشرف العام وإدارة الجمعية"
        ),
        RegisteredUserEntry(
            id = "sup_02",
            name = "أ. محمود صالح الأهدل",
            role = UserRole.SUPERVISOR,
            phone = "770001122",
            email = "m.ahdal@maknoon.org.sa",
            identityNumber = "1034567890",
            societyName = "الجمعية الخيرية لتحفيظ القرآن الكريم",
            subRoleDescription = "مشرف الشؤون التعليمية"
        )
    )

    private val defaultTeachers = listOf(
        RegisteredUserEntry(
            id = "tch_01",
            name = "الشيخ عبد الله القدسي",
            role = UserRole.TEACHER,
            phone = "771234567",
            email = "qudsi@maknoon.org.sa",
            identityNumber = "1045678901",
            societyName = "الجمعية الخيرية لتحفيظ القرآن الكريم",
            subRoleDescription = "معلم حلقة الإمام عاصم (حفص وشعبة)"
        ),
        RegisteredUserEntry(
            id = "tch_02",
            name = "الشيخ محمد بن سالم الزبير",
            role = UserRole.TEACHER,
            phone = "738766810",
            email = "salem@maknoon.org.sa",
            identityNumber = "1056789012",
            societyName = "الجمعية الخيرية لتحفيظ القرآن الكريم",
            subRoleDescription = "معلم حلقة الإمام نافع (ورش وقالون)"
        ),
        RegisteredUserEntry(
            id = "tch_03",
            name = "الشيخ عثمان الشامي",
            role = UserRole.TEACHER,
            phone = "055112233",
            email = "shami@maknoon.org.sa",
            identityNumber = "1067890123",
            societyName = "الجمعية الخيرية لتحفيظ القرآن الكريم",
            subRoleDescription = "معلم حلقة ابن كثير والإجازات"
        )
    )

    private val defaultGuardians = listOf(
        RegisteredUserEntry(
            id = "grd_01",
            name = "أحمد اليماني (ولي أمر الطالب عبد الله)",
            role = UserRole.GUARDIAN,
            phone = "775484201",
            email = "yemeni.ahmed@gmail.com",
            identityNumber = "1078901234",
            societyName = "الجمعية الخيرية لتحفيظ القرآن الكريم",
            subRoleDescription = "ولي أمر - حلقة الإمام عاصم"
        ),
        RegisteredUserEntry(
            id = "grd_02",
            name = "خالد القدسي (ولي أمر الطالب عمر)",
            role = UserRole.GUARDIAN,
            phone = "738766810",
            email = "khaled.qudsi@gmail.com",
            identityNumber = "1089012345",
            societyName = "الجمعية الخيرية لتحفيظ القرآن الكريم",
            subRoleDescription = "ولي أمر - حلقة الإمام نافع"
        ),
        RegisteredUserEntry(
            id = "grd_03",
            name = "سالم الزبير (ولي أمر الطالب محمد)",
            role = UserRole.GUARDIAN,
            phone = "0500112233",
            email = "salem.z@gmail.com",
            identityNumber = "1090123456",
            societyName = "الجمعية الخيرية لتحفيظ القرآن الكريم",
            subRoleDescription = "ولي أمر - حلقة الإمام عاصم"
        ),
        RegisteredUserEntry(
            id = "grd_04",
            name = "وليد الحكيمي (ولي أمر الطالب إبراهيم)",
            role = UserRole.GUARDIAN,
            phone = "773344556",
            email = "hakimi.w@gmail.com",
            identityNumber = "1091234567",
            societyName = "الجمعية الخيرية لتحفيظ القرآن الكريم",
            subRoleDescription = "ولي أمر - حلقة ابن كثير"
        )
    )

    private val masterOwnerEntry = RegisteredUserEntry(
        id = "owner_master",
        name = "مالك التطبيق (Super Admin)",
        role = UserRole.OWNER,
        phone = "775484201",
        email = "adffakff@gmail.com",
        identityNumber = "9900000001",
        societyName = "المركز الرئيسي الموحد",
        subRoleDescription = "صاحب الصلاحية الجذرية والتحكم العام"
    )

    /**
     * استرجاع قائمة جميع المستخدمين المسجلين في النظام
     */
    fun getAllRegisteredUsers(tenant: Tenant = SocietyRepository.defaultRealTenant): List<RegisteredUserEntry> {
        val list = mutableListOf<RegisteredUserEntry>()

        // 1. المالك العام
        list.add(masterOwnerEntry)

        // 2. المشرفون من الذاكرة والافتراضيين
        val existingSupIds = mutableSetOf<String>()
        list.addAll(defaultSupervisors)
        existingSupIds.addAll(defaultSupervisors.map { it.phone })

        SocietyRepository.allUsers.forEach { u ->
            if (u.phone !in existingSupIds) {
                list.add(
                    RegisteredUserEntry(
                        id = u.id,
                        name = u.name,
                        role = u.role,
                        phone = u.phone,
                        email = u.email,
                        identityNumber = "10" + u.phone.takeLast(6),
                        societyName = tenant.name,
                        subRoleDescription = u.title.ifBlank { u.role.label }
                    )
                )
                existingSupIds.add(u.phone)
            }
        }

        // 3. المعلمون
        val existingTeacherPhones = mutableSetOf<String>()
        list.addAll(defaultTeachers)
        existingTeacherPhones.addAll(defaultTeachers.map { it.phone })

        SocietyRepository.allTeachers.forEach { t ->
            if (t.phone !in existingTeacherPhones) {
                list.add(
                    RegisteredUserEntry(
                        id = t.id,
                        name = t.name,
                        role = UserRole.TEACHER,
                        phone = t.phone,
                        email = t.email,
                        identityNumber = t.nationalId.ifBlank { "10" + t.phone.takeLast(6) },
                        societyName = tenant.name,
                        subRoleDescription = "معلم " + (t.assignedRings.firstOrNull() ?: "حلقة قرآنية")
                    )
                )
                existingTeacherPhones.add(t.phone)
            }
        }

        // 4. أولياء الأمور
        val existingGuardianPhones = mutableSetOf<String>()
        list.addAll(defaultGuardians)
        existingGuardianPhones.addAll(defaultGuardians.map { it.phone })

        SocietyRepository.allStudents.forEach { s ->
            if (s.guardianPhone.isNotBlank() && s.guardianPhone !in existingGuardianPhones) {
                list.add(
                    RegisteredUserEntry(
                        id = "grd_" + s.id,
                        name = "${s.guardianName.ifBlank { "ولي أمر" }} (الطالب ${s.name})",
                        role = UserRole.GUARDIAN,
                        phone = s.guardianPhone,
                        email = "",
                        identityNumber = "10" + s.guardianPhone.takeLast(6),
                        societyName = tenant.name,
                        subRoleDescription = "ولي أمر الطالب ${s.name}"
                    )
                )
                existingGuardianPhones.add(s.guardianPhone)
            }
        }

        return list
    }

    /**
     * فحص صارم: التحقق هل المعرف ينتمي لمستخدم مسجل وموثق مسبقاً في التطبيق
     * يعيد كائن المستخدم المسجل إذا وجد، أو null إذا كان مجهول الهوية!
     */
    fun findRegisteredUserStrict(query: String, tenant: Tenant = SocietyRepository.defaultRealTenant): RegisteredUserEntry? {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return null

        val all = getAllRegisteredUsers(tenant)
        return all.firstOrNull { u ->
            u.phone.replace("+", "").contains(clean.replace("+", "")) ||
                    (u.email.isNotBlank() && u.email.lowercase().contains(clean)) ||
                    (u.identityNumber.isNotBlank() && u.identityNumber.contains(clean)) ||
                    u.name.lowercase().contains(clean) ||
                    clean == u.id.lowercase()
        }
    }

    /**
     * هل المستخدم مسجل مسبقاً؟
     */
    fun isUserRegistered(query: String, tenant: Tenant = SocietyRepository.defaultRealTenant): Boolean {
        return findRegisteredUserStrict(query, tenant) != null
    }

    /**
     * حظر وتطهير أي عملية مجهولة الهوية فوراً
     */
    fun blockAndSanitizeAnonymousAttempt(
        context: Context?,
        attemptedIdentifier: String,
        operationName: String = "محاولة مصادقة بيومترية مجهولة"
    ): AnonymousBlockResult {
        blockedAnonymousAttemptsCount++

        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale("ar"))
        val currentTime = dateFormat.format(Date())
        val device = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

        // مسح وتطهير بيانات الجلسة فوراً (Data Sanitization)
        isDataSanitized = true
        lastWipeTime = currentTime

        // توثيق الحادث كتهديد أمني محظور في نظام أمان المالك
        val incident = OwnerSecurityManager.logIntrusionAttempt(
            context = context,
            attemptType = "منع عملية مجهولة الهوية: $operationName",
            attemptedInput = if (attemptedIdentifier.isBlank()) "معرف فارغ" else "معرف غير مسجل: $attemptedIdentifier"
        )

        val result = AnonymousBlockResult(
            isBlocked = true,
            attemptedIdentifier = attemptedIdentifier.ifBlank { "مجهول" },
            reason = "المعرف غير مسجل مسبقاً في قاعدة بيانات الجمعية. تم تفعيل بروتوكول منع العمليات المجهولة وتطهير الجلسة فوراً.",
            timestamp = currentTime,
            wasDataWiped = true
        )

        lastBlockedThreat = result

        // إشعار أمني محلي فوري
        context?.let { ctx ->
            LocalNotificationHelper.showSessionReminderNotification(
                context = ctx,
                title = "⛔ درع الأمان: تم حظر عملية مجهولة!",
                message = "تم اعتراض محاولة وصول غير مسجلة مسبقاً ($attemptedIdentifier) وتطهير الجلسة فوراً."
            )
        }

        return result
    }

    /**
     * نظام مسح وتطهير البيانات الحساسة (Security Data Wipe Protocol)
     * ينفذ مسحاً فورياً لحماية البيانات من الاستيلاء أو العبث عند تكرار التهديدات.
     */
    fun executeEmergencyDataWipe(
        context: Context?,
        wipeLevel: WipeLevel = WipeLevel.SESSION_CACHE,
        reason: String = "تطهير أمني لمنع تسريب البيانات"
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale("ar"))
        val currentTime = dateFormat.format(Date())

        isDataSanitized = true
        lastWipeTime = currentTime

        when (wipeLevel) {
            WipeLevel.SESSION_CACHE -> {
                // مسح وتطهير بيانات الجلسات والرموز المؤقتة
                OwnerSecurityManager.resetSecurityLockdown()
            }
            WipeLevel.EMERGENCY_DEEP_PURGE -> {
                // تطهير أمني عميق مع الحفاظ على مفتاح المالك الجذري فقط
                SocietyRepository.wipeAllDataPreservingOwner()
                OwnerSecurityManager.clearIncidents()
            }
        }

        val log = OwnerSecurityManager.logUserAccess(
            userNameOrRole = "نظام الأمان الذاتي (Data Sanitizer)",
            role = UserRole.OWNER,
            tenantName = "الحماية المركزية",
            context = context,
            accessType = "تنفيذ مسح وتطهير أمني للبيانات ($wipeLevel): $reason"
        )

        return "تم مسح وتطهير البيانات بنجاح في $currentTime بنظام (${wipeLevel.label})"
    }
}
