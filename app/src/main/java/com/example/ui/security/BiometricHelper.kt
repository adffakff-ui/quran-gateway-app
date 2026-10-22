package com.example.ui.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.OwnerSecurityManager
import com.example.data.UserRole

/**
 * Biometric authentication helper utilizing androidx.biometric.
 * Provides fingerprint and facial authentication as a high-security layer
 * for administrative access, sensitive data reveals, and supervisor operations.
 */
object BiometricHelper {

    enum class BiometricStatus(val message: String, val isUsable: Boolean) {
        AVAILABLE("مستشعر البصمة والمصادقة الحيوية جاهز ومفعل", true),
        NOT_ENROLLED("لم يتم تسجيل أي بصمة في إعدادات الجهاز حتى الآن", false),
        NO_HARDWARE("الجهاز لا يحتوي على مستشعر بصمة أو كاميرا تعرف على الوجه", false),
        HW_UNAVAILABLE("مستشعر البصمة غير متاح حالياً أو قيد الاستخدام", false),
        SECURITY_UPDATE_REQUIRED("يتطلب تحديث أمني لمستشعر البصمة", false),
        UNSUPPORTED_CONTEXT("سياق التطبيق لا يدعم واجهة FragmentActivity", false)
    }

    /**
     * Unwraps any ContextWrapper chain (like ContextThemeWrapper in Compose) to locate FragmentActivity.
     */
    fun findFragmentActivity(context: Context): FragmentActivity? {
        var current: Context? = context
        while (current != null) {
            if (current is FragmentActivity) return current
            current = if (current is ContextWrapper) current.baseContext else null
        }
        return null
    }

    /**
     * Checks biometric availability status on this device.
     */
    fun checkBiometricStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        return when (canAuth) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            else -> BiometricStatus.NOT_ENROLLED
        }
    }

    /**
     * Direct boolean helper for checking if biometric or device credential can authenticate.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        return checkBiometricStatus(context).isUsable
    }

    /**
     * Displays a biometric prompt for general quick-login.
     */
    fun showBiometricPrompt(
        context: Context,
        title: String = "تسجيل الدخول بالبصمة",
        subtitle: String = "استخدم بصمة الإصبع أو قفل الجهاز للمتابعة",
        negativeButtonText: String = "إلغاء",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = findFragmentActivity(context)
        if (activity == null) {
            onError("تعذر العثور على نشاط متوافق (FragmentActivity)")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Error code 10 or 13 represents user cancellation
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        onError(errString.toString())
                    } else {
                        onError("تم إلغاء عملية المصادقة الحيوية")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("فشلت مطابقة البصمة، يرجى المحاولة مجدداً")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "خطأ في تشغيل مستشعر البصمة")
        }
    }

    /**
     * المصادقة الحيوية المخصصة حسب الرتبة (مشرف عام / معلم حلقات / ولي أمر / مالك)
     * ترفع مستوى الأمان وتوجه المستخدم مباشرة لبوابته المعتمدة.
     */
    fun showRoleBiometricPrompt(
        context: Context,
        role: UserRole,
        tenantName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val (title, subtitle) = when (role) {
            UserRole.SUPERVISOR -> Pair("مصادقة المشرف العام بالبصمة", "مسح بصمة الإصبع للوصول المباشر إلى لوحة تحكم المشرف وإدارة الجمعية")
            UserRole.TEACHER -> Pair("مصادقة معلم الحلقات بالبصمة", "مسح بصمة الإصبع للوصول إلى بوابة المعلم وحلقات التسميع")
            UserRole.GUARDIAN -> Pair("مصادقة ولي الأمر بالبصمة", "مسح بصمة الإصبع لمتابعة إنجازات وتسميع الأبناء القرآني")
            UserRole.OWNER -> Pair("مصادقة مالك النظام بالبصمة", "الوصول المشدد والآمن لمالك المنظومة")
        }

        showBiometricPrompt(
            context = context,
            title = title,
            subtitle = subtitle,
            negativeButtonText = "إلغاء",
            onSuccess = {
                OwnerSecurityManager.logUserAccess(
                    userNameOrRole = "الدخول السريع بالبصمة: ${role.label}",
                    role = role,
                    tenantName = tenantName,
                    accessType = "مصادقة بيومترية ناجحة ($role)"
                )
                onSuccess()
            },
            onError = onError
        )
    }

    /**
     * المصادقة البيومترية الصارمة للمستخدم المسجل حصرياً (Zero-Trust Biometric Gate)
     * تمنع أي عملية لمستخدم مجهول الهوية ليس له تسجيل مسبق في النظام.
     */
    fun showStrictBiometricForRegisteredUser(
        context: Context,
        user: com.example.data.RegisteredUserEntry,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val isVerified = com.example.data.ZeroTrustSecuritySystem.isUserRegistered(user.phone)
        if (!isVerified) {
            val blocked = com.example.data.ZeroTrustSecuritySystem.blockAndSanitizeAnonymousAttempt(
                context = context,
                attemptedIdentifier = user.phone,
                operationName = "محاولة مصادقة بيومترية لمستخدم غير مسجل"
            )
            onError(blocked.reason)
            return
        }

        val title = "المصادقة البيومترية للمستخدم المسجل"
        val subtitle = "المستخدم: ${user.name} (${user.role.label}) • هاتف: ${user.phone}"

        showBiometricPrompt(
            context = context,
            title = title,
            subtitle = subtitle,
            negativeButtonText = "إلغاء",
            onSuccess = {
                OwnerSecurityManager.logUserAccess(
                    userNameOrRole = user.name,
                    role = user.role,
                    tenantName = user.societyName,
                    accessType = "مصادقة بيومترية معتمدة وموثقة (${user.role.label})"
                )
                onSuccess()
            },
            onError = onError
        )
    }

    /**
     * Dedicated security prompt for administrative access (Supervisor / Super Admin / Owner).
     * Includes automated logging of intrusion attempts or successful verification.
     */
    fun authenticateForAdministrativeAccess(
        context: Context,
        operationName: String = "الوصول الإداري المشدد",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = findFragmentActivity(context)
        if (activity == null) {
            onError("واجهة النظام لا تدعم مستشعر البصمة حالياً")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Log successful administrative access
                    OwnerSecurityManager.logIntrusionAttempt(
                        context = context,
                        attemptType = "المصادقة الحيوية الإدارية: $operationName",
                        attemptedInput = "تم التحقق بنجاح عبر البصمة الحيوية / قفل الجهاز"
                    )
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val isCanceled = (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED)

                    if (!isCanceled) {
                        OwnerSecurityManager.logIntrusionAttempt(
                            context = context,
                            attemptType = "فشل المصادقة الحيوية الإدارية: $operationName",
                            attemptedInput = "رمز الخطأ: $errorCode ($errString)"
                        )
                        onError("فشل التحقق الأمني: $errString")
                    } else {
                        onError("تم إلغاء المصادقة الحيوية")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("بصمة غير مطابقة، يرجى المحاولة مجدداً")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("المصادقة الحيوية المشددة للإدارة")
            .setSubtitle("استخدم بصمة الإصبع أو الوجه أو قفل الجهاز لتأكيد الهوية")
            .setDescription("مطلوب تحقق بيومتري مصرح به لتنفيذ عملية: $operationName")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError("تعذر تشغيل واجهة المصادقة الحيوية: ${e.localizedMessage}")
        }
    }
}
