package com.example.ui.security

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OtpSecuritySystem
import com.example.data.OtpSession
import com.example.data.OtpVerificationResult
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * نافذة حوار التحقق الأمني الذكية بالرمز السري (OTP Verification Dialog)
 * متكاملة لجميع المستخدمين المسجلين وترسل الرموز حصرياً لبيانات المستلم (WhatsApp / SMS / Email)
 * مع فحص دقيق وشامل لصلاحية الرمز وعدد المحاولات ومدة الانتهاء (5 دقائق).
 */
@Composable
fun OtpVerificationDialog(
    purpose: String = "تسجيل الدخول والتوثيق الأمني",
    recipientName: String = "المستخدم المسجل",
    targetPhoneOrEmail: String = "",
    preferredMethod: String = "WhatsApp",
    userPhone: String = "",
    userEmail: String = "",
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<OtpSession?>(null) }
    var resendCooldown by remember { mutableIntStateOf(45) }
    var sessionSecondsLeft by remember { mutableLongStateOf(300L) }
    var selectedMethod by remember { mutableStateOf(preferredMethod) }

    // تحديد الوجهة الفعلية بناء على الوسيلة المختارة
    val effectiveTarget = when (selectedMethod) {
        "Email" -> userEmail.ifBlank { if (targetPhoneOrEmail.contains("@")) targetPhoneOrEmail else "user@society.org" }
        "SMS" -> userPhone.ifBlank { targetPhoneOrEmail.filter { it.isDigit() } }
        else -> userPhone.ifBlank { targetPhoneOrEmail.filter { it.isDigit() } }
    }

    // إرسال الرمز وتوليد الجلسة
    fun dispatchOtp(method: String) {
        val session = OtpSecuritySystem.issueOtp(
            target = effectiveTarget,
            recipientName = recipientName,
            purpose = purpose,
            context = context
        )
        activeSession = session
        sessionSecondsLeft = session.remainingSeconds
        inputCode = ""
        isError = false
        errorMessage = null

        when (method) {
            "WhatsApp" -> OtpSecuritySystem.sendViaWhatsApp(context, session, effectiveTarget)
            "SMS" -> OtpSecuritySystem.sendViaSms(context, session, effectiveTarget)
            "Email" -> OtpSecuritySystem.sendViaEmail(context, session, effectiveTarget)
        }
    }

    // إصدار أولي للرمز عند فتح النافذة
    LaunchedEffect(Unit) {
        dispatchOtp(selectedMethod)
    }

    // عداد تنازلي لإعادة إرسال الرمز (Cooldown)
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    // عداد تنازلي لصلاحية جلسة الرمز (5 دقائق)
    LaunchedEffect(activeSession) {
        while (true) {
            val session = activeSession
            if (session != null) {
                sessionSecondsLeft = session.remainingSeconds
                if (session.isExpired) {
                    isError = true
                    errorMessage = "انتهت صلاحية رمز التحقق (أكثر من 5 دقائق). يرجى الضغط على زر إعادة الإرسال."
                    break
                }
            }
            delay(1000)
        }
    }

    fun executeVerify() {
        when (val result = OtpSecuritySystem.verifyOtpWithResult(inputCode)) {
            is OtpVerificationResult.Valid -> {
                isError = false
                errorMessage = null
                onVerified()
            }
            is OtpVerificationResult.Invalid -> {
                isError = true
                errorMessage = result.reason
            }
        }
    }

    val minutes = sessionSecondsLeft / 60
    val seconds = sessionSecondsLeft % 60
    val formattedTimer = String.format("%02d:%02d", minutes, seconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SocietyBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = SocietyBluePrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "رمز التحقق السري (OTP)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تم إرسال رمز الأمان للتحقق من هوية الحساب",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // بطاقة معلومات المستلم والغرض والوجهة المشفرة
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("العملية:", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = purpose,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SocietyBluePrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المستلم:", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = recipientName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الوجهة:", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = OtpSecuritySystem.maskDestination(effectiveTarget),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldDark
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("صلاحية الرمز:", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = if (sessionSecondsLeft > 0) "متبقي $formattedTimer دقيقة" else "انتهت الصلاحية",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sessionSecondsLeft > 60) EmeraldDark else StatusError
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // اختيار وسيلة الإرسال التفاعلية للمستخدم المسجل
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedMethod == "WhatsApp",
                        onClick = {
                            selectedMethod = "WhatsApp"
                            dispatchOtp("WhatsApp")
                        },
                        label = { Text("WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (selectedMethod == "WhatsApp") Color(0xFF15803D) else TextMuted
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedMethod == "Email",
                        onClick = {
                            selectedMethod = "Email"
                            dispatchOtp("Email")
                        },
                        label = { Text("البريد", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (selectedMethod == "Email") SocietyBluePrimary else TextMuted
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedMethod == "SMS",
                        onClick = {
                            selectedMethod = "SMS"
                            dispatchOtp("SMS")
                        },
                        label = { Text("SMS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (selectedMethod == "SMS") GoldenAmber else TextMuted
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // حقل إدخال رمز التحقق المكون من 6 أرقام
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isDigit() }
                        if (filtered.length <= 6) {
                            inputCode = filtered
                            isError = false
                            errorMessage = null
                            if (filtered.length == 6) {
                                executeVerify()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("otp_input_field"),
                    placeholder = {
                        Text(
                            text = "أدخل رمز التحقق (6 أرقام)",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 13.sp,
                            color = TextSubtle
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { executeVerify() }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    isError = isError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SocietyBluePrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = StatusError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // زر إرسال الرمز التفاعلي الخاص ببيانات المستخدم المسجل
                when (selectedMethod) {
                    "WhatsApp" -> {
                        OutlinedButton(
                            onClick = {
                                activeSession?.let {
                                    OtpSecuritySystem.sendViaWhatsApp(context, it, effectiveTarget)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF25D366).copy(alpha = 0.08f)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إرسال الرمز عبر WhatsApp إلى الرقم المسجل",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                    "Email" -> {
                        OutlinedButton(
                            onClick = {
                                activeSession?.let {
                                    OtpSecuritySystem.sendViaEmail(context, it, effectiveTarget)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SocietyBlueLight.copy(alpha = 0.4f)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = SocietyBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إرسال الرمز إلى البريد الإلكتروني المسجل",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SocietyBluePrimary
                            )
                        }
                    }
                    "SMS" -> {
                        OutlinedButton(
                            onClick = {
                                activeSession?.let {
                                    OtpSecuritySystem.sendViaSms(context, it, effectiveTarget)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = GoldenAmber.copy(alpha = 0.08f)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إرسال الرمز كرسالة SMS إلى الرقم المسجل",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // عداد إعادة الإرسال
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (resendCooldown > 0) {
                        Text(
                            text = "إعادة إرسال الرمز بعد $resendCooldown ثانية",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    } else {
                        TextButton(
                            onClick = {
                                resendCooldown = 45
                                dispatchOtp(selectedMethod)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = SocietyBluePrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إعادة توليد وإرسال رمز جديد الآن",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SocietyBluePrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { executeVerify() },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                modifier = Modifier.testTag("verify_otp_confirm_btn")
            ) {
                Text("تأكيد الرمز والدخول", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
