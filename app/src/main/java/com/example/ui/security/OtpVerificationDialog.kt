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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OtpSecuritySystem
import com.example.data.OtpSession
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * نافذة حوار التحقق الأمني الذكية بالرمز السري (OTP Verification Dialog)
 */
@Composable
fun OtpVerificationDialog(
    purpose: String = "تسجيل الدخول والتوثيق الأمني",
    recipientName: String = "المستخدم",
    targetPhoneOrEmail: String = "775484201",
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<OtpSession?>(null) }
    var resendCooldown by remember { mutableIntStateOf(45) }

    // إرسال الرمز تلقائياً عند فتح النافذة
    LaunchedEffect(Unit) {
        val session = OtpSecuritySystem.issueOtp(
            target = targetPhoneOrEmail,
            recipientName = recipientName,
            purpose = purpose,
            context = context
        )
        activeSession = session
    }

    // عداد تنازلي لإعادة الإرسال
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(SocietyBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = SocietyBluePrimary,
                    modifier = Modifier.size(30.dp)
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
                    text = "تم إرسال رمز الأمان للتحقق من هوية المستخدم",
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
                // بطاقة معلومات المستلم والغرض
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("العملية:", fontSize = 11.sp, color = TextMuted)
                            Text(purpose, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SocietyBluePrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المستلم:", fontSize = 11.sp, color = TextMuted)
                            Text(recipientName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // حقل إدخال الرمز السري المكون من 6 أرقام
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = {
                        if (it.length <= 6) {
                            inputCode = it
                            isError = false
                            errorMessage = null
                            if (it.length == 6 || it == OtpSecuritySystem.MASTER_PIN) {
                                if (OtpSecuritySystem.verifyOtp(it)) {
                                    onVerified()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("otp_input_field"),
                    placeholder = {
                        Text(
                            text = "أدخل الرمز هنا (مثال: 8107)",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    isError = isError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SocietyBluePrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = StatusError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // زر استدعاء الرمز السري الرئيسي 8107 بنقرة واحدة لتسهيل التجربة
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            inputCode = OtpSecuritySystem.MASTER_PIN
                            if (OtpSecuritySystem.verifyOtp(OtpSecuritySystem.MASTER_PIN)) {
                                onVerified()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFF92400E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "استخدام الرمز السري الرئيسي (${OtpSecuritySystem.MASTER_PIN})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // خيار إرسال الرمز لواتساب المالك مباشرة
                OutlinedButton(
                    onClick = {
                        activeSession?.let { session ->
                            OtpSecuritySystem.sendViaWhatsApp(context, session)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF25D366).copy(alpha = 0.08f)
                    ),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إرسال الرمز لواتساب المالك (+967775484201)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // إعادة الإرسال
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
                                val session = OtpSecuritySystem.issueOtp(
                                    target = targetPhoneOrEmail,
                                    recipientName = recipientName,
                                    purpose = purpose,
                                    context = context
                                )
                                activeSession = session
                            }
                        ) {
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
                onClick = {
                    if (OtpSecuritySystem.verifyOtp(inputCode)) {
                        onVerified()
                    } else {
                        isError = true
                        errorMessage = "رمز التحقق غير صحيح، يرجى المحاولة أو استخدام ${OtpSecuritySystem.MASTER_PIN}"
                    }
                },
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
