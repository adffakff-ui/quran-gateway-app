package com.example.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SocietyRepository
import com.example.data.Tenant
import com.example.data.UserRole
import com.example.data.OwnerSecurityManager
import com.example.data.SecurityIncident
import com.example.data.OtpSecuritySystem
import com.example.ui.security.OtpVerificationDialog
import com.example.ui.theme.*
import com.example.ui.settings.LanguageToggleChip
import com.example.ui.security.BiometricHelper
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    currentTenant: Tenant,
    onTenantSelected: (Tenant) -> Unit,
    onLoginSuccess: (Tenant, UserRole) -> Unit,
    onNavigateToSupervisorRegistration: () -> Unit = {},
    onBiometricQuickLoginSuccess: (Tenant, UserRole) -> Unit = { t, r -> onLoginSuccess(t, r) }
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var usernameOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog & Sheet States
    var showTenantDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var quickAuthMethod by remember { mutableStateOf<String?>(null) } // "WhatsApp", "SMS", "Email"
    var isContextLoading by remember { mutableStateOf(false) }
    var loadingStatusText by remember { mutableStateOf("") }

    // Owner Security & Intrusion Detection States
    var showOwnerSecretDialog by remember { mutableStateOf(false) }
    var ownerSecretInput by remember { mutableStateOf("") }
    var isOwnerSecretHidden by remember { mutableStateOf(true) }
    var ownerAuthErrorMessage by remember { mutableStateOf<String?>(null) }
    var ownerAuthSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showIntrusionAlertDialog by remember { mutableStateOf(false) }
    var currentIncidentForAlert by remember { mutableStateOf<SecurityIncident?>(null) }

    // Enforce RTL
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF0F4FA),
                            Color(0xFFE2EAF8),
                            Color(0xFFF8FAFC)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Framer Motion style subtle entrance animations for feature cards
                var startAnim by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    startAnim = true
                }
                val cardAlpha by animateFloatAsState(
                    targetValue = if (startAnim) 1f else 0f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    label = "cardAlpha"
                )
                val cardScale by animateFloatAsState(
                    targetValue = if (startAnim) 1f else 0.95f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "cardScale"
                )

                // Language Toggle Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = cardAlpha, scaleX = cardScale, scaleY = cardScale),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguageToggleChip()
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==========================================
                // رأس الصفحة (Header): [شعار الجمعية] [اختيار الجمعية ▼]
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = cardAlpha, scaleX = cardScale, scaleY = cardScale)
                        .testTag("tenant_header_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // شعار واسم الجمعية المختارة
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SocietyBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_society_logo),
                                    contentDescription = "شعار الجمعية",
                                    modifier = Modifier.size(38.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentTenant.shortName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "فرع: ${currentTenant.city} • كود: ${currentTenant.code}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // زر اختيار الجمعية ▼
                        OutlinedButton(
                            onClick = { showTenantDialog = true },
                            modifier = Modifier.testTag("choose_tenant_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SocietyBlueLight.copy(alpha = 0.5f),
                                contentColor = SocietyBluePrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "اختيار الجمعية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "اختيار الجمعية",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // مربع تسجيل الدخول (وسط الصفحة، RTL)
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .graphicsLayer(alpha = cardAlpha, scaleX = cardScale, scaleY = cardScale)
                        .testTag("login_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // عنوان المربع والترحيب
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SocietyBluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "تسجيل الدخول",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "بوابة الجمعية الموحدة • نظام متعدد المستأجرين",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            color = BorderSubtle
                        )

                        // حقل: رقم الجوال / البريد [يمين]
                        Text(
                            text = "رقم الجوال أو البريد الإلكتروني",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = usernameOrPhone,
                            onValueChange = {
                                usernameOrPhone = it
                                errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_phone_input"),
                            placeholder = {
                                Text(
                                    "05xxxxxxxx أو user@example.com",
                                    fontSize = 13.sp,
                                    color = TextSubtle
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = SocietyBluePrimary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SocietyBluePrimary,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // حقل: كلمة المرور [يمين]
                        Text(
                            text = "كلمة المرور",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            placeholder = { Text("أدخل كلمة المرور", fontSize = 13.sp, color = TextSubtle) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Key,
                                    contentDescription = null,
                                    tint = SocietyBluePrimary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                        tint = TextMuted
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val trimmed = usernameOrPhone.trim()
                                    if (trimmed.isBlank()) {
                                        errorMessage = "يرجى إدخال اسم المستخدم أو رقم الهاتف للدخول."
                                    } else if (password.isBlank()) {
                                        errorMessage = "يرجى إدخال كلمة المرور."
                                    } else if (trimmed == "demo" || trimmed == "test" || trimmed == "supervisor" || trimmed == "123456") {
                                        errorMessage = "تم تصفير وإلغاء كافة الحسابات والأكواد التجريبية نهائياً. يرجى إدخال بياناتك المعتمدة أو رمز التحقق (OTP)."
                                    } else {
                                        errorMessage = null
                                        startLoginProcess(
                                            userInput = usernameOrPhone,
                                            currentTenant = currentTenant,
                                            coroutineScope = coroutineScope,
                                            setLoading = { isContextLoading = it },
                                            setStatus = { loadingStatusText = it },
                                            onSuccess = onLoginSuccess
                                        )
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SocietyBluePrimary,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = StatusError,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // زر: تسجيل الدخول (أزرق) [محاذاة يمين]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Button(
                                onClick = {
                                    val trimmed = usernameOrPhone.trim()
                                    if (trimmed.isBlank()) {
                                        errorMessage = "يرجى إدخال اسم المستخدم أو رقم الهاتف للدخول."
                                    } else if (password.isBlank()) {
                                        errorMessage = "يرجى إدخال كلمة المرور."
                                    } else if (trimmed == "demo" || trimmed == "test" || trimmed == "supervisor" || trimmed == "123456") {
                                        errorMessage = "تم تصفير وإلغاء كافة الحسابات والأكواد التجريبية نهائياً. يرجى إدخال بياناتك المعتمدة أو رمز التحقق (OTP)."
                                    } else {
                                        errorMessage = null
                                        startLoginProcess(
                                            userInput = usernameOrPhone,
                                            currentTenant = currentTenant,
                                            coroutineScope = coroutineScope,
                                            setLoading = { isContextLoading = it },
                                            setStatus = { loadingStatusText = it },
                                            onSuccess = onLoginSuccess
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "تسجيل الدخول",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // الدخول السريع ببصمة الإصبع للمشرفين
                        if (SocietyRepository.isBiometricLoginEnabled) {
                            OutlinedButton(
                                onClick = {
                                    BiometricHelper.showBiometricPrompt(
                                        context = context,
                                        title = "الدخول السريع للمشرف العام",
                                        subtitle = "استخدم بصمة الإصبع للدخول المباشر إلى لوحة التحكم",
                                        negativeButtonText = "إلغاء",
                                        onSuccess = {
                                            onBiometricQuickLoginSuccess(currentTenant, UserRole.SUPERVISOR)
                                        },
                                        onError = { err ->
                                            errorMessage = "فشل التحقق بالبصمة: $err"
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("supervisor_biometric_login_btn"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, EmeraldDark.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = EmeraldLight.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "بصمة المشرف",
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "الدخول السريع ببصمة الإصبع (للمشرفين)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // زر وصول مالك النظام المشفر والمحمي (Owner Root Access - بدون إظهار أي بريد أو هاتف)
                        OutlinedButton(
                            onClick = {
                                ownerSecretInput = ""
                                ownerAuthErrorMessage = null
                                showOwnerSecretDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("owner_root_portal_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "👑 بوابة المالك والتحكم المركزي (مشفر)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFD97706).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "حماية Root",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // رابط: نسيت كلمة المرور؟ [يمين تحته]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "نسيت كلمة المرور؟",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SocietyBluePrimary,
                                modifier = Modifier
                                    .clickable { showForgotPasswordDialog = true }
                                    .padding(vertical = 4.dp)
                                    .testTag("forgot_password_link")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // فاصل خفيف
                        HorizontalDivider(color = BorderSubtle)

                        Spacer(modifier = Modifier.height(14.dp))

                        // روابط التسجيل
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextButton(
                                onClick = { showRegisterDialog = true },
                                modifier = Modifier.testTag("register_new_account_link")
                            ) {
                                Text(
                                    text = "تسجيل حساب جديد (ولي أمر / معلم)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // أسفل: أزرار تسجيل بواسطة: WhatsApp | SMS | Email
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                        Text(
                            text = "أو الدخول السريع بواسطة",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Biometric Login Button
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            BiometricHelper.showBiometricPrompt(
                                context = context,
                                onSuccess = {
                                    onBiometricQuickLoginSuccess(currentTenant, UserRole.SUPERVISOR)
                                },
                                onError = { err ->
                                    errorMessage = "فشل المصادقة الحيوية: $err"
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("biometric_login_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SocietyBlueLight.copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "بصمة",
                            tint = SocietyBluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل الدخول السريع بالبصمة الحيوية",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SocietyBluePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // WhatsApp
                        OutlinedButton(
                            onClick = { quickAuthMethod = "WhatsApp" },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("auth_whatsapp_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF25D366).copy(alpha = 0.08f)
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "WhatsApp",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WhatsApp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }

                        // SMS
                        OutlinedButton(
                            onClick = { quickAuthMethod = "SMS" },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("auth_sms_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = GoldenAmberLight.copy(alpha = 0.4f)
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = "SMS",
                                tint = GoldenAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SMS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldenAmber
                            )
                        }

                        // Email
                        OutlinedButton(
                            onClick = { quickAuthMethod = "Email" },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("auth_email_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SocietyBlueLight.copy(alpha = 0.6f)
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = SocietyBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Email",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SocietyBluePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ==========================================
                    // نموذج 'تواصل معنا' لطلب انضمام جمعية للنظام
                    // ==========================================
                    ContactUsJoinSection(
                        onSocietyApproved = { approvedTenant ->
                            onTenantSelected(approvedTenant)
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "نظام سحابي آمن متوافق مع ضوابط الأمن السيبراني للجمعيات",
                        fontSize = 11.sp,
                        color = TextSubtle,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ==========================================
            // شاشة تحميل سياق الجمعية (Tenant Context Loader)
            // ==========================================
            AnimatedVisibility(
                visible = isContextLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(28.dp)
                            .widthIn(max = 380.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = SocietyBluePrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "جاري تحميل سياق الجمعية",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentTenant.name,
                                fontSize = 13.sp,
                                color = SocietyBluePrimary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = loadingStatusText,
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Modal: اختيار الجمعية (Tenant Selector)
        // ==========================================
        if (showTenantDialog) {
            var tenantSearchQuery by remember { mutableStateOf("") }
            var selectedGovFilter by remember { mutableStateOf("الكل") }

            val governorates = listOf(
                "الكل", "تعز", "أمانة العاصمة", "صنعاء", "عدن",
                "حضرموت", "مأرب", "إب", "الحديدة", "شبوة",
                "ذمار", "البيضاء", "لحج", "أبين", "المهرة",
                "سقطرى", "حجة", "صعدة", "الجوف", "الضالع", "المحويت", "عمران", "ريمة"
            )

            val filteredTenants = remember(tenantSearchQuery, selectedGovFilter) {
                SocietyRepository.tenants.filter { tenant ->
                    val matchesGov = if (selectedGovFilter == "الكل") true else tenant.governorate.contains(selectedGovFilter)
                    val matchesSearch = tenantSearchQuery.isEmpty() ||
                            tenant.name.contains(tenantSearchQuery, ignoreCase = true) ||
                            tenant.shortName.contains(tenantSearchQuery, ignoreCase = true) ||
                            tenant.city.contains(tenantSearchQuery, ignoreCase = true) ||
                            tenant.governorate.contains(tenantSearchQuery, ignoreCase = true) ||
                            tenant.code.contains(tenantSearchQuery, ignoreCase = true)
                    matchesGov && matchesSearch
                }
            }

            AlertDialog(
                onDismissRequest = { showTenantDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CorporateFare,
                                contentDescription = null,
                                tint = SocietyBluePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "اختيار الجمعية (الجمهورية اليمنية)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${filteredTenants.size} جمعية ومؤسسة قرآنية متاحة",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                    ) {
                        // حقل البحث
                        OutlinedTextField(
                            value = tenantSearchQuery,
                            onValueChange = { tenantSearchQuery = it },
                            placeholder = { Text("ابحث باسم الجمعية، المحافظة، أو المدينة...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "بحث", tint = SocietyBluePrimary)
                            },
                            trailingIcon = {
                                if (tenantSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { tenantSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tenant_search_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // شرائح تصفية المحافظات (Scrollable Chips)
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(governorates.size) { index ->
                                val gov = governorates[index]
                                val isSelected = selectedGovFilter == gov
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedGovFilter = gov },
                                    label = { Text(gov, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SocietyBluePrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // قائمة الجمعيات
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (filteredTenants.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لا توجد نتائج مطابقة لبحثك في $selectedGovFilter",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            } else {
                                filteredTenants.forEach { tenant ->
                                    val isSelected = tenant.id == currentTenant.id
                                    val isProminent = tenant.id == "t_hsa" || tenant.id == "t_muadh"

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                onTenantSelected(tenant)
                                                showTenantDialog = false
                                            }
                                            .testTag("tenant_item_${tenant.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) SocietyBlueLight else if (isProminent) Color(0xFFFBF8EE) else Color(0xFFF8FAFC)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) SocietyBluePrimary else if (isProminent) GoldenAmber.copy(alpha = 0.5f) else BorderSubtle
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = tenant.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected || isProminent) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = if (isSelected) SocietyBluePrimary else if (isProminent) Color(0xFF8A6200) else TextDark
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "المحافظة: ${tenant.governorate} • المدينة: ${tenant.city} • كود: ${tenant.code}",
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }

                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "محددة",
                                                        tint = SocietyBluePrimary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                } else if (isProminent) {
                                                    Surface(
                                                        color = GoldenAmberLight,
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(0.5.dp, GoldenAmber)
                                                    ) {
                                                        Text(
                                                            text = "مؤسسة رائدة",
                                                            fontSize = 10.sp,
                                                            color = GoldenAmber,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "${tenant.ringsCount} حلقة",
                                                    fontSize = 10.sp,
                                                    color = TextSubtle
                                                )
                                                Text(
                                                    text = "•",
                                                    fontSize = 10.sp,
                                                    color = TextSubtle
                                                )
                                                Text(
                                                    text = "${tenant.studentsCount} طالب",
                                                    fontSize = 10.sp,
                                                    color = TextSubtle
                                                )
                                                Text(
                                                    text = "•",
                                                    fontSize = 10.sp,
                                                    color = TextSubtle
                                                )
                                                Text(
                                                    text = "${tenant.teachersCount} معلماً",
                                                    fontSize = 10.sp,
                                                    color = TextSubtle
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTenantDialog = false }) {
                        Text("إغلاق", color = SocietyBluePrimary)
                    }
                }
            )
        }

        // ==========================================
        // Modal: استعادة كلمة المرور
        // ==========================================
        if (showForgotPasswordDialog) {
            var resetInput by remember { mutableStateOf(usernameOrPhone) }
            var isSent by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.LockReset,
                        contentDescription = null,
                        tint = SocietyBluePrimary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "استعادة كلمة المرور",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column {
                        if (!isSent) {
                            Text(
                                text = "أدخل رقم الجوال المسجل أو البريد الإلكتروني وسنرسل لك رمز تحقق OTP لإعادة تعيين كلمة المرور:",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = resetInput,
                                onValueChange = { resetInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("05xxxxxxxx") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        } else {
                            Text(
                                text = "تم إرسال رمز التحقق (OTP) بنجاح إلى $resetInput. يرجى إدخال الرمز المكون من 4 أرقام.",
                                fontSize = 13.sp,
                                color = EmeraldDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!isSent) {
                                isSent = true
                            } else {
                                showForgotPasswordDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text(if (!isSent) "إرسال الرمز" else "حسناً")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // ==========================================
        // Modal: تسجيل حساب جديد (ولي/معلم)
        // ==========================================
        if (showRegisterDialog) {
            var selectedRole by remember { mutableStateOf(UserRole.GUARDIAN) }
            var regName by remember { mutableStateOf("") }
            var regId by remember { mutableStateOf("") }
            var regPhone by remember { mutableStateOf("") }
            var regSupervisorDept by remember { mutableStateOf("") }
            var regSuccess by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showRegisterDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = EmeraldDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل حساب جديد",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (!regSuccess) {
                            Text(
                                text = "اختر نوع الحساب في ${currentTenant.shortName}:",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // اختيار الدور (ولي أمر / معلم / مشرف)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedRole == UserRole.GUARDIAN,
                                    onClick = { selectedRole = UserRole.GUARDIAN },
                                    label = { Text("ولي أمر", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FamilyRestroom,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedRole == UserRole.TEACHER,
                                    onClick = { selectedRole = UserRole.TEACHER },
                                    label = { Text("معلم", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedRole == UserRole.SUPERVISOR,
                                    onClick = { selectedRole = UserRole.SUPERVISOR },
                                    label = { Text("مشرف", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = regName,
                                onValueChange = { regName = it },
                                label = { Text("الاسم الثلاثي") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = regId,
                                onValueChange = { regId = it },
                                label = { Text("رقم الهوية الوطنية / الإقامة") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = regPhone,
                                onValueChange = { regPhone = it },
                                label = { Text("رقم الجوال (05xxxxxxxx)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (selectedRole == UserRole.SUPERVISOR) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = regSupervisorDept,
                                    onValueChange = { regSupervisorDept = it },
                                    label = { Text("القسم الإشرافي / الاعتماد") },
                                    placeholder = { Text("مثال: إدارة الشؤون التعليمية والتحفيظ") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        showRegisterDialog = false
                                        onNavigateToSupervisorRegistration()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("open_dedicated_supervisor_screen_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فتح شاشة تسجيل المشرف الكاملة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            val roleTitle = when (selectedRole) {
                                UserRole.OWNER -> "مالك التطبيق (Super Admin)"
                                UserRole.SUPERVISOR -> "المشرف العام"
                                UserRole.TEACHER -> "معلم حلقات"
                                UserRole.GUARDIAN -> "ولي أمر"
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "تم إنشاء حساب $roleTitle بنجاح!",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "تم إرسال بيانات الدخول وتفعيل صلاحيات الحساب في سياق ${currentTenant.shortName}",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!regSuccess) {
                                regSuccess = true
                            } else {
                                if (regPhone.isNotBlank()) {
                                    usernameOrPhone = regPhone
                                }
                                showRegisterDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                    ) {
                        Text(if (!regSuccess) "إتمام التسجيل" else "تسجيل الدخول الآن")
                    }
                },
                dismissButton = {
                    if (!regSuccess) {
                        TextButton(onClick = { showRegisterDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                }
            )
        }

        // ==========================================
        // Modal: تسجيل بواسطة WhatsApp / SMS / Email OTP
        // ==========================================
        if (quickAuthMethod != null) {
            val method = quickAuthMethod!!
            val resolvedUser = SocietyRepository.findRegisteredUser(usernameOrPhone, currentTenant)
            val effectiveTarget = if (method == "Email") {
                resolvedUser.email.ifBlank { usernameOrPhone.takeIf { it.contains("@") } ?: "user@society.org" }
            } else {
                resolvedUser.phone.ifBlank { usernameOrPhone.filter { it.isDigit() } }
            }

            OtpVerificationDialog(
                purpose = "تسجيل دخول وتوثيق سريع عبر $method",
                recipientName = resolvedUser.name,
                targetPhoneOrEmail = effectiveTarget,
                preferredMethod = method,
                userPhone = resolvedUser.phone.ifBlank { usernameOrPhone.filter { it.isDigit() } },
                userEmail = resolvedUser.email.ifBlank { if (usernameOrPhone.contains("@")) usernameOrPhone else "" },
                onDismiss = { quickAuthMethod = null },
                onVerified = {
                    val usedInput = usernameOrPhone.ifBlank { resolvedUser.phone }
                    quickAuthMethod = null
                    startLoginProcess(
                        userInput = usedInput,
                        currentTenant = currentTenant,
                        coroutineScope = coroutineScope,
                        setLoading = { isContextLoading = it },
                        setStatus = { loadingStatusText = it },
                        onSuccess = { tenant, _ ->
                            onLoginSuccess(tenant, resolvedUser.role)
                        }
                    )
                }
            )
        }

        // نافذة التحقق الأمني المحمية لبوابة المالك (Owner Root Security Gateway)
        if (showOwnerSecretDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!OwnerSecurityManager.isLockdownActive) {
                        showOwnerSecretDialog = false
                    }
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "بوابة المالك والتحكم المركزي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "نظام التوثيق الأمني والمراقبة الفورية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // بطاقة هوية المالك المحجوبة أمنياً (Masked Credentials)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = GoldenAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "بوابة المالك والتحكم المركزي المعتمدة",
                                        color = GoldenAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "جلسة دخول مشفرة ومحمية بأنظمة التشفير العالية 🔒",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (OwnerSecurityManager.isLockdownActive) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = StatusError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تم تجميد الوصول مؤقتاً لتكرار المحاولات الخاطئة! استخدم رمز OTP المعتمد لفك الحظر.",
                                        fontSize = 11.sp,
                                        color = StatusError,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = "أدخل المفتاح السري أو رمز التحقق (OTP):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        // أزرار إرسال رمز التحقق الحقيقي إلى واتساب أو بريد المالك
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val session = OtpSecuritySystem.issueOtp(
                                        target = OwnerSecurityManager.OWNER_PHONE_INTL,
                                        recipientName = "مالك التطبيق (Super Admin)",
                                        purpose = "توثيق دخول بوابة المالك المركزي",
                                        context = context
                                    )
                                    OtpSecuritySystem.sendViaWhatsApp(context, session, OwnerSecurityManager.OWNER_PHONE_INTL)
                                    ownerAuthSuccessMessage = "تم إصدار الرمز وإرساله لواتساب المالك."
                                    ownerAuthErrorMessage = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF25D366).copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رمز واتساب", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val session = OtpSecuritySystem.issueOtp(
                                        target = OwnerSecurityManager.OWNER_EMAIL,
                                        recipientName = "مالك التطبيق (Super Admin)",
                                        purpose = "توثيق دخول بوابة المالك المركزي",
                                        context = context
                                    )
                                    OtpSecuritySystem.sendViaEmail(context, session, OwnerSecurityManager.OWNER_EMAIL)
                                    ownerAuthSuccessMessage = "تم إصدار الرمز وإرساله لبريد المالك."
                                    ownerAuthErrorMessage = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SocietyBlueLight.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = SocietyBluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رمز بريد", fontSize = 11.sp, color = SocietyBluePrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (ownerAuthSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = ownerAuthSuccessMessage ?: "",
                                    color = Color(0xFF065F46),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = ownerSecretInput,
                            onValueChange = {
                                ownerSecretInput = it
                                ownerAuthErrorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_secret_field"),
                            placeholder = { Text("المفتاح السري / رمز التحقق (OTP)", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (isOwnerSecretHidden) PasswordVisualTransformation() else VisualTransformation.None,
                            trailingIcon = {
                                IconButton(onClick = { isOwnerSecretHidden = !isOwnerSecretHidden }) {
                                    Icon(
                                        imageVector = if (isOwnerSecretHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isOwnerSecretHidden) "إظهار الرمز" else "إخفاء الرمز",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            isError = ownerAuthErrorMessage != null,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (ownerAuthErrorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = ownerAuthErrorMessage ?: "",
                                    color = StatusError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // تحذير أمني مباشر
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الرصد الفوري مفعل: أي محاولة خاطئة ترسل تنبيهاً فورياً وتخضع للرقابة المشفرة.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (ownerSecretInput.isBlank()) {
                                ownerAuthErrorMessage = "يرجى إدخال المفتاح السري أو رمز التحقق"
                                return@Button
                            }
                            val isSuccess = OwnerSecurityManager.verifyOwnerSecret(ownerSecretInput, context)
                            if (isSuccess) {
                                showOwnerSecretDialog = false
                                ownerAuthErrorMessage = null
                                ownerAuthSuccessMessage = null
                                startLoginProcess(
                                    userInput = ownerSecretInput,
                                    currentTenant = currentTenant,
                                    coroutineScope = coroutineScope,
                                    setLoading = { isContextLoading = it },
                                    setStatus = { loadingStatusText = it },
                                    onSuccess = onLoginSuccess
                                )
                            } else {
                                ownerAuthErrorMessage = "المفتاح أو رمز التحقق غير صحيح، يرجى المحاولة مجدداً."
                                if (OwnerSecurityManager.failedAttemptsCount >= 3) {
                                    currentIncidentForAlert = OwnerSecurityManager.securityIncidents.firstOrNull()
                                    showIntrusionAlertDialog = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("توثيق ودخول المالك", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showOwnerSecretDialog = false
                        ownerAuthErrorMessage = null
                        ownerAuthSuccessMessage = null
                    }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // نافذة الإنذار الأمني الفوري عند رصد محاولة اختراق (Intrusion Alert Dispatch Modal)
        if (showIntrusionAlertDialog && currentIncidentForAlert != null) {
            val incident = currentIncidentForAlert!!
            AlertDialog(
                onDismissRequest = { showIntrusionAlertDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(StatusError.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "🚨 إنذار أمني: رصد محاولة غير مصرح بها!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StatusError,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "تم رصد محاولة دخول خاطئة وغير مصرح بها لبوابة المالك وتوثيقها فورياً في سجل أمان النظام.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("📅 التوقيت: ${incident.timestamp}", color = Color.White, fontSize = 11.sp)
                                Text("📱 الجهاز: ${incident.deviceModel}", color = Color.White, fontSize = 11.sp)
                                Text("⚡ مستوى الإنذار: ${incident.severity}", color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("⚠️ نوع المحاولة: ${incident.attemptType}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "تم تجهيز البلاغ الأمني لإرساله مباشرة إلى واتساب المالك (${OwnerSecurityManager.maskPhone()}) وبريده الإلكتروني (${OwnerSecurityManager.maskEmail()}). يمكنك إرساله يدوياً بالضغط أدناه:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // زر إرسال مباشر للواتساب
                        Button(
                            onClick = {
                                try {
                                    val intent = OwnerSecurityManager.createWhatsAppAlertIntent(incident)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "واتساب غير متوفر على الجهاز", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال البلاغ فوراً إلى واتساب المالك", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // زر إرسال مباشر للإيميل
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = OwnerSecurityManager.createEmailAlertIntent(incident)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تطبيق البريد غير متوفر", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال البلاغ فوراً إلى بريد المالك", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showIntrusionAlertDialog = false }) {
                        Text("إغلاق التنبيه")
                    }
                }
            )
        }
    }
}

// دالة محاكاة تحميل سياق الجمعية والتحقق الأمني من الرتبة
private fun startLoginProcess(
    userInput: String = "",
    currentTenant: Tenant,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    setLoading: (Boolean) -> Unit,
    setStatus: (String) -> Unit,
    onSuccess: (Tenant, UserRole) -> Unit
) {
    coroutineScope.launch {
        setLoading(true)
        val isOwnerUser = SocietyRepository.isOwner(userInput)
        if (isOwnerUser) {
            setStatus("توثيق هوية مالك النظام وتشفير الاتصال • منح صلاحيات Root الكاملة...")
            OwnerSecurityManager.logUserAccess(
                userNameOrRole = "مالك التطبيق العام (Super Admin)",
                role = UserRole.OWNER,
                tenantName = currentTenant.shortName,
                accessType = "تسجيل دخول المالك والتحقق الأمني الموثق"
            )
            delay(400)
            setLoading(false)
            onSuccess(currentTenant, UserRole.OWNER)
        } else {
            setStatus("تهيئة الجلسة الرقمية وتوثيق الحساب...")
            OwnerSecurityManager.logUserAccess(
                userNameOrRole = if (userInput.isNotBlank()) "المشرف: $userInput" else "المشرف العام",
                role = UserRole.SUPERVISOR,
                tenantName = currentTenant.shortName,
                accessType = "تسجيل دخول مستخدم / زائر"
            )
            delay(350)
            setStatus("تحميل بيانات سياق المستأجر: ${currentTenant.shortName}...")
            delay(350)
            setStatus("مزامنة حلقات التحفيظ ومعلمي الجمعية...")
            delay(300)
            setLoading(false)
            onSuccess(currentTenant, UserRole.SUPERVISOR)
        }
    }
}
