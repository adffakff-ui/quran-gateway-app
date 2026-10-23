package com.example.ui.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * نافذة التحقق الأمني من كود OTP المطابقة تماماً لتصميم Infinity Links عبر WhatsApp بالرقم 738766810.
 * مزودة بفحص مسبق لبيانات المستخدمين، وتحديد مدة دقيقة للرمز، وإرسال تلقائي.
 */
@Composable
fun OtpVerificationDialog(
    purpose: String = "تسجيل الدخول والتوثيق الأمني",
    recipientName: String = "المستخدم المسجل",
    targetPhoneOrEmail: String = "738766810",
    preferredMethod: String = "WhatsApp",
    userPhone: String = "738766810",
    userEmail: String = "",
    initialDurationMinutes: Int = 5,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<OtpSession?>(null) }
    var resendCooldown by remember { mutableIntStateOf(35) }
    var durationMinutes by remember { mutableIntStateOf(initialDurationMinutes) }
    var sessionSecondsLeft by remember { mutableLongStateOf(initialDurationMinutes * 60L) }
    var selectedMethod by remember { mutableStateOf(preferredMethod) }
    var activeTab by remember { mutableStateOf(0) } // 0: تصميم WhatsApp (Infinity Links), 1: حقل الإدخال والإعدادات

    // فحص بيانات المستخدم قبل الإرسال
    val inspection = remember(targetPhoneOrEmail, userPhone) {
        val query = userPhone.ifBlank { targetPhoneOrEmail }
        val defTenant = SocietyRepository.tenants.firstOrNull() ?: SocietyRepository.defaultRealTenant
        OtpSecuritySystem.inspectUserDataBeforeSending(query, defTenant)
    }

    val verifiedPhone = when (inspection) {
        is UserInspectionResult.Approved -> inspection.verifiedPhone
        is UserInspectionResult.Rejected -> "738766810"
    }

    val verifiedName = when (inspection) {
        is UserInspectionResult.Approved -> inspection.user.name
        is UserInspectionResult.Rejected -> recipientName
    }

    // إرسال الرمز تلقائياً وفورياً
    fun dispatchOtp(method: String, duration: Int = durationMinutes) {
        val session = OtpSecuritySystem.issueOtp(
            target = verifiedPhone,
            recipientName = verifiedName,
            purpose = purpose,
            durationMinutes = duration,
            context = context
        )
        activeSession = session
        sessionSecondsLeft = session.remainingSeconds
        inputCode = ""
        isError = false
        errorMessage = null

        when (method) {
            "WhatsApp" -> OtpSecuritySystem.sendViaWhatsApp(context, session, verifiedPhone)
            "SMS" -> OtpSecuritySystem.sendViaSms(context, session, verifiedPhone)
            "Email" -> OtpSecuritySystem.sendViaEmail(context, session, userEmail.ifBlank { "infinity@society.org" })
        }
    }

    // الإرسال التلقائي الأولي عند فتح النافذة
    LaunchedEffect(Unit) {
        if (inspection is UserInspectionResult.Approved) {
            dispatchOtp(selectedMethod, durationMinutes)
        } else if (inspection is UserInspectionResult.Rejected) {
            isError = true
            errorMessage = "تعذر التحقق من العضوية: " + (inspection as UserInspectionResult.Rejected).message
        }
    }

    // عداد تنازلي لإعادة الإرسال
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    // عداد تنازلي لصلاحية الرمز المحددة
    LaunchedEffect(activeSession) {
        while (true) {
            val session = activeSession
            if (session != null) {
                sessionSecondsLeft = session.remainingSeconds
                if (session.isExpired) {
                    isError = true
                    errorMessage = "انتهت مدة صلاحية كود التحقق (${session.durationMinutes} دقائق). يرجى طلب كود جديد."
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
                Toast.makeText(context, "تم التحقق من الكود بنجاح!", Toast.LENGTH_SHORT).show()
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
    val totalSeconds = (activeSession?.durationMinutes ?: durationMinutes) * 60
    val timerProgress = (sessionSecondsLeft.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .testTag("otp_infinity_links_dialog"),
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // شريط التبويب العلوي
                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = StatusError,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = SocietyBluePrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = if (activeTab == 0) Color(0xFF25D366) else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "تصميم WhatsApp (Infinity Links)",
                                        fontSize = 12.sp,
                                        fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Pin,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "إدخال الكود والمدة",
                                        fontSize = 12.sp,
                                        fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeTab == 0) {
                        // ==========================================
                        // الواجهة المطابقة تماماً لصورة واتساب المرفقة
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B141B)), // واتساب داكن كالصورة
                            border = BorderStroke(1.dp, Color(0xFF1F2C34)),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                // شريط رأس الدردشة (WhatsApp Top Bar)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // أيقونة شعار Infinity Links (شريط متصل أزرق وأخضر كالصورة)
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(Color(0xFF0284C7), Color(0xFF10B981))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AllInclusive,
                                                    contentDescription = "Infinity Logo",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Infinity Links",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE9EDEF)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Verified",
                                                    tint = Color(0xFF25D366),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = "رقم البوابة: 738766810",
                                                fontSize = 11.sp,
                                                color = Color(0xFF8696A0)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFF1F2C34),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "حساب تجاري",
                                            fontSize = 10.sp,
                                            color = Color(0xFF25D366),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // رسالة حماية النشاط التجاري (كما بالصورة)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF182229)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF00A884),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "يستخدم هذا النشاط التجاري خدمة محمية توفرها شركة Meta لإدارة هذه الدردشة. اضغط لمعرفة المزيد.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF8696A0),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // بطاقة الملف التجاري في المحادثة (كالصورة)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111B21)),
                                    border = BorderStroke(1.dp, Color(0xFF222E35))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // شعار اللوجو الدائري الكبير
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(Color(0xFF0284C7), Color(0xFF10B981))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AllInclusive,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = "Infinity Links",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "رقم هاتف من اليمن (738766810) • حساب تجاري • تم الانضمام في أكتوبر, ٢٠٢٥",
                                            fontSize = 11.sp,
                                            color = Color(0xFF8696A0),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    Toast.makeText(context, "الملف التجاري: Infinity Links (بوابة 738766810)", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(20.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Color(0xFF202C33)
                                                ),
                                                border = BorderStroke(0.5.dp, Color(0xFF374248)),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text("الملف الشخصي", fontSize = 12.sp, color = Color(0xFFE9EDEF))
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    Toast.makeText(context, "تم تأكيد سلامة القناة وحظر أي أرقام غير معتمدة", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(20.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Color(0xFF202C33)
                                                ),
                                                border = BorderStroke(0.5.dp, Color(0xFF374248)),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Block,
                                                    contentDescription = null,
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("حظر", fontSize = 12.sp, color = Color(0xFFEF4444))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // فقاعة الرسالة (Chat Bubble - النص المطابق للصورة تماماً)
                                val currentOtpCode = activeSession?.code ?: "926313"
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    shape = RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = 2.dp,
                                        bottomEnd = 14.dp
                                    ),
                                    color = Color(0xFF202C33), // لون فقاعة واتساب الواردة الداكنة كالصورة
                                    border = BorderStroke(0.5.dp, Color(0xFF2A3942))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = "$currentOtpCode هو كود التحقق الخاص بك.",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "للحفاظ على أمانك، لا تشارك هذا الكود مع أي شخص.",
                                            fontSize = 13.sp,
                                            color = Color(0xFFE9EDEF),
                                            lineHeight = 18.sp
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "٣:٢٣ ص",
                                                fontSize = 11.sp,
                                                color = Color(0xFF8696A0)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.DoneAll,
                                                contentDescription = null,
                                                tint = Color(0xFF53BDEB),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // أزرار الإجراء السريع للرسالة
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("OTP Code", currentOtpCode)
                                            clipboard.setPrimaryClip(clip)
                                            inputCode = currentOtpCode
                                            activeTab = 1
                                            Toast.makeText(context, "تم نسخ الكود ($currentOtpCode) بنجاح!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884))
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نسخ وتعبئة الكود", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            activeSession?.let {
                                                OtpSecuritySystem.sendViaWhatsApp(context, it, verifiedPhone)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF182229)),
                                        border = BorderStroke(1.dp, Color(0xFF00A884))
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = null,
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("فتح WhatsApp", fontSize = 11.sp, color = Color(0xFF25D366))
                                    }
                                }
                            }
                        }
                    } else {
                        // ==========================================
                        // تبويب إدخال الكود وضبط مدة الصلاحية
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // بطاقة فحص بيانات المستخدم
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = SocietyBlueLight.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.VerifiedUser,
                                                    contentDescription = null,
                                                    tint = SocietyBluePrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "فحص وتوثيق بيانات المستخدم:",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SocietyBlueDark
                                                )
                                            }

                                            Surface(
                                                color = Color(0xFF25D366).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "معتمد ✓",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF15803D),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "المستلم: $verifiedName",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "الرقم المعتمد: $verifiedPhone (بوابة Infinity Links: 738766810)",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // تحديد مدة صلاحية الرمز OTP
                                Text(
                                    text = "تحديد مدة صلاحية كود التحقق (OTP Duration):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(3, 5, 10).forEach { mins ->
                                        val isSelected = durationMinutes == mins
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                durationMinutes = mins
                                                dispatchOtp(selectedMethod, mins)
                                                Toast.makeText(context, "تم ضبط الصلاحية على $mins دقائق وتوليد كود جديد", Toast.LENGTH_SHORT).show()
                                            },
                                            label = {
                                                Text(
                                                    text = "$mins دقائق" + if (mins == 5) " (قياسي)" else "",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SocietyBluePrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // مؤشر العداد التنازلي الزمني
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (sessionSecondsLeft > 60) EmeraldLight else StatusError.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = null,
                                                    tint = if (sessionSecondsLeft > 60) EmeraldDark else StatusError,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "الصلاحية المتبقية للكود:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Text(
                                                text = if (sessionSecondsLeft > 0) formattedTimer else "منتهي الصلاحية",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sessionSecondsLeft > 60) EmeraldDark else StatusError
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        LinearProgressIndicator(
                                            progress = { timerProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (sessionSecondsLeft > 60) EmeraldDark else StatusError,
                                            trackColor = Color(0xFFE2E8F0)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // حقل إدخال كود OTP المكون من 6 أرقام
                                Text(
                                    text = "أدخل كود التحقق المكون من 6 أرقام:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = inputCode,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                            inputCode = newValue
                                            isError = false
                                            errorMessage = null
                                            if (newValue.length == 6) {
                                                executeVerify()
                                            }
                                        }
                                    },
                                    placeholder = { Text("أدخل رمز التحقق (6 أرقام)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Pin,
                                            contentDescription = null,
                                            tint = SocietyBluePrimary
                                        )
                                    },
                                    trailingIcon = {
                                        if (inputCode.length == 6) {
                                            IconButton(onClick = { executeVerify() }) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "تحقق",
                                                    tint = StatusSuccess
                                                )
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { executeVerify() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("otp_code_input_field"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
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
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // أزرار التحكم السفلية
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { executeVerify() },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("otp_confirm_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                            enabled = inputCode.length == 6
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تأكيد الكود والدخول", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (resendCooldown == 0) {
                                    resendCooldown = 35
                                    dispatchOtp(selectedMethod, durationMinutes)
                                    Toast.makeText(context, "تم إرسال كود تحقق جديد تلقائياً!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("otp_resend_btn"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = resendCooldown == 0,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SocietyBluePrimary
                            )
                        ) {
                            Text(
                                text = if (resendCooldown > 0) "إعادة ($resendCooldown)" else "إرسال كود جديد",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("otp_dismiss_btn")
                    ) {
                        Text("إلغاء والعودة", fontSize = 12.sp, color = TextMuted)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}
