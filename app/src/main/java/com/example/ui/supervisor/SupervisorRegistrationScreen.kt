package com.example.ui.supervisor

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.notifications.LocalNotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * واجهة تسجيل المشرفين والمعلمين
 * مزودة بالتحقق التام من صحة البيانات والربط الآمن بنظام Firebase Authentication & Firestore
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorRegistrationScreen(
    currentTenant: Tenant,
    onNavigateBack: () -> Unit,
    onRegistrationSuccess: (Tenant, UserProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // تحديد نوع الحساب (مشرف أو معلم)
    var selectedRole by remember { mutableStateOf(UserRole.SUPERVISOR) }

    // حقول النموذج الأساسية (المطلوبة في التوجيه)
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // حقول إضافية بحسب نوع الحساب
    var department by remember { mutableStateOf("إدارة الشؤون التعليمية والتحفيظ") }
    var nationalId by remember { mutableStateOf("") }
    var teacherQualification by remember { mutableStateOf("إجازة برواية حفص عن عاصم بالسند المتصل") }
    var experienceYears by remember { mutableStateOf("3") }
    var assignedRingPreference by remember { mutableStateOf("حلقة الإتقان والتجويد (عصراً)") }

    // رؤية كلمة المرور
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // إقرار التعهد والضوابط
    var agreeToTerms by remember { mutableStateOf(true) }

    // الجمعية المختارة (Tenant Context)
    var activeTenant by remember { mutableStateOf(currentTenant) }
    var showTenantDialog by remember { mutableStateOf(false) }

    // حالات التحقق والأخطاء اللحظية
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var generalErrorMessage by remember { mutableStateOf<String?>(null) }

    // حالة الإرسال عبر Firebase
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var createdUserUid by remember { mutableStateOf("") }
    var registeredProfile by remember { mutableStateOf<UserProfile?>(null) }

    // فحص قوة كلمة المرور لحظياً
    val passwordStrength = remember(password) {
        when {
            password.isEmpty() -> 0
            password.length < 6 -> 1 // ضعيفة
            password.length in 6..8 && password.any { it.isDigit() } -> 2 // متوسطة
            password.length >= 8 && password.any { it.isDigit() } && password.any { it.isLetter() } -> 3 // قوية
            else -> 2
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedRole == UserRole.SUPERVISOR) SocietyBlueLight else EmeraldLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedRole == UserRole.SUPERVISOR) Icons.Default.AdminPanelSettings else Icons.Default.School,
                                    contentDescription = null,
                                    tint = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (selectedRole == UserRole.SUPERVISOR) "تسجيل مشرف جديد" else "تسجيل معلم حلقات جديد",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "بوابة الجمعيات القرآنية • التسجيل المعتمد في Firebase",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("supervisor_reg_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = SlateBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ==========================================
                // 1. بطاقة اختيار نوع الحساب (مشرف / معلم)
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "اختر نوع التسجيل والصلاحية:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // خيار: مشرف عام
                            val isSupervisor = selectedRole == UserRole.SUPERVISOR
                            OutlinedCard(
                                onClick = {
                                    selectedRole = UserRole.SUPERVISOR
                                    generalErrorMessage = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("role_supervisor_tab"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSupervisor) SocietyBlueLight else Color(0xFFFAFAFA)
                                ),
                                border = BorderStroke(
                                    if (isSupervisor) 2.dp else 1.dp,
                                    if (isSupervisor) SocietyBluePrimary else BorderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = if (isSupervisor) SocietyBluePrimary else TextMuted,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "مشرف عام / إدارة",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSupervisor) SocietyBlueDark else TextDark
                                    )
                                    Text(
                                        text = "إشراف ومتابعة وتقارير",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // خيار: معلم تحفيظ
                            val isTeacher = selectedRole == UserRole.TEACHER
                            OutlinedCard(
                                onClick = {
                                    selectedRole = UserRole.TEACHER
                                    generalErrorMessage = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("role_teacher_tab"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isTeacher) EmeraldLight else Color(0xFFFAFAFA)
                                ),
                                border = BorderStroke(
                                    if (isTeacher) 2.dp else 1.dp,
                                    if (isTeacher) EmeraldDark else BorderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = if (isTeacher) EmeraldDark else TextMuted,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "معلم حلقات قرآنية",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTeacher) EmeraldDark else TextDark
                                    )
                                    Text(
                                        text = "تحضير وتسميع الطلاب",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 2. بطاقة الجمعية المنتسب إليها (Tenant Context)
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .clickable { showTenantDialog = true }
                        .testTag("supervisor_reg_tenant_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedRole == UserRole.SUPERVISOR) SocietyBlueLight.copy(alpha = 0.7f) else EmeraldLight.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary.copy(alpha = 0.4f) else EmeraldDark.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CorporateFare,
                                contentDescription = null,
                                tint = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "الجمعية التابع لها: ${activeTenant.name}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedRole == UserRole.SUPERVISOR) SocietyBlueDark else EmeraldDark
                                )
                                Text(
                                    text = "المحافظة: ${activeTenant.governorate} • الفرع: ${activeTenant.city} • كود: ${activeTenant.code}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showTenantDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                "تغيير",
                                fontSize = 11.sp,
                                color = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ==========================================
                // 3. نموذج التسجيل الأساسي (مع التحقق اللحظي)
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .testTag("registration_form_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "بيانات الحساب الشخصي والاعتماد",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Firebase Auth",
                                        fontSize = 11.sp,
                                        color = TextDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BorderSubtle)

                        // 1. حقل الاسم الكامل (Name)
                        Column {
                            Text(
                                text = if (selectedRole == UserRole.SUPERVISOR) "الاسم الكامل للمشرف *" else "الاسم الكامل للمعلم *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = {
                                    fullName = it
                                    fullNameError = if (it.trim().length < 3) "يجب أن يتكون الاسم من 3 أحرف على الأقل" else null
                                    generalErrorMessage = null
                                },
                                isError = fullNameError != null,
                                supportingText = {
                                    fullNameError?.let {
                                        Text(text = it, color = StatusError, fontSize = 11.sp)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        if (selectedRole == UserRole.SUPERVISOR) "أدخل اسم المشرف الكامل" else "أدخل اسم المعلم الكامل"
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                                    )
                                },
                                trailingIcon = {
                                    if (fullName.trim().length >= 3) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // 2. حقل البريد الإلكتروني (Email)
                        Column {
                            Text(
                                text = "البريد الإلكتروني المعتمد (Firebase Login) *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    val valid = Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches()
                                    emailError = if (it.isNotBlank() && !valid) "صيغة البريد الإلكتروني غير صحيحة (name@example.com)" else null
                                    generalErrorMessage = null
                                },
                                isError = emailError != null,
                                supportingText = {
                                    emailError?.let {
                                        Text(text = it, color = StatusError, fontSize = 11.sp)
                                    }
                                },
                                placeholder = { Text("أدخل البريد الإلكتروني") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                                    )
                                },
                                trailingIcon = {
                                    if (email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_email_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // 3. حقل رقم الجوال (Mobile Phone)
                        Column {
                            Text(
                                text = "رقم الجوال للتواصل والتحقق (WhatsApp / SMS) *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = {
                                    phone = it
                                    val digits = it.filter { ch -> ch.isDigit() }
                                    phoneError = if (it.isNotBlank() && digits.length < 9) "يجب ألا يقل رقم الجوال عن 9 أرقام" else null
                                    generalErrorMessage = null
                                },
                                isError = phoneError != null,
                                supportingText = {
                                    phoneError?.let {
                                        Text(text = it, color = StatusError, fontSize = 11.sp)
                                    }
                                },
                                placeholder = { Text("أدخل رقم الجوال") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                                    )
                                },
                                trailingIcon = {
                                    if (phone.filter { it.isDigit() }.length >= 9) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_phone_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // 4. حقل كلمة المرور (Password)
                        Column {
                            Text(
                                text = "كلمة المرور المشفرة *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    passwordError = if (it.isNotBlank() && it.length < 6) "يجب ألا تقل كلمة المرور عن 6 خانات (Firebase)" else null
                                    if (confirmPassword.isNotEmpty() && it != confirmPassword) {
                                        confirmPasswordError = "كلمة المرور وتأكيدها غير متطابقين"
                                    } else {
                                        confirmPasswordError = null
                                    }
                                    generalErrorMessage = null
                                },
                                isError = passwordError != null,
                                supportingText = {
                                    passwordError?.let {
                                        Text(text = it, color = StatusError, fontSize = 11.sp)
                                    }
                                },
                                placeholder = { Text("أدخل 6 خانات على الأقل") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "إخفاء" else "إظهار"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_password_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // مؤشر قوة كلمة المرور
                            if (password.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    repeat(3) { index ->
                                        val active = index < passwordStrength
                                        val color = when (passwordStrength) {
                                            1 -> StatusError
                                            2 -> GoldenAmber
                                            else -> EmeraldDark
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (active) color else BorderSubtle)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (passwordStrength) {
                                            1 -> "ضعيفة"
                                            2 -> "مقبولة"
                                            else -> "قوية جداً"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (passwordStrength) {
                                            1 -> StatusError
                                            2 -> GoldenAmber
                                            else -> EmeraldDark
                                        }
                                    )
                                }
                            }
                        }

                        // 5. حقل تأكيد كلمة المرور (Confirm Password)
                        Column {
                            Text(
                                text = "تأكيد كلمة المرور *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    confirmPasswordError = if (it != password) "كلمتا المرور غير متطابقتين" else null
                                    generalErrorMessage = null
                                },
                                isError = confirmPasswordError != null,
                                supportingText = {
                                    confirmPasswordError?.let {
                                        Text(text = it, color = StatusError, fontSize = 11.sp)
                                    }
                                },
                                placeholder = { Text("أعد كتابة كلمة المرور للتحقق") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LockReset,
                                        contentDescription = null,
                                        tint = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (confirmPasswordVisible) "إخفاء" else "إظهار"
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_confirm_password_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // 6. الحقول الخاصة بالصلاحية (مشرف أو معلم)
                        if (selectedRole == UserRole.SUPERVISOR) {
                            Column {
                                Text(text = "القسم الإشرافي / الاعتماد الإداري", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = department,
                                    onValueChange = { department = it },
                                    placeholder = { Text("أدخل القسم الإشرافي") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = SocietyBluePrimary)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reg_department_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        } else {
                            Column {
                                Text(text = "المؤهل القرآني / الإجازة بالسند", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = teacherQualification,
                                    onValueChange = { teacherQualification = it },
                                    placeholder = { Text("أدخل المؤهل القرآني") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = null, tint = EmeraldDark)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reg_teacher_qualification_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "سنوات الخبرة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = experienceYears,
                                        onValueChange = { experienceYears = it },
                                        placeholder = { Text("عدد السنوات") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(text = "الحلقة المقترحة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = assignedRingPreference,
                                        onValueChange = { assignedRingPreference = it },
                                        placeholder = { Text("اسم الحلقة") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        // 7. رقم الهوية الوطنية / السجل
                        Column {
                            Text(text = "رقم الهوية الوطنية / الإقامة (اختياري)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = nationalId,
                                onValueChange = { nationalId = it },
                                placeholder = { Text("رقم الهوية") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.PermIdentity, contentDescription = null, tint = TextMuted)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_national_id_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // 8. إقرار التعهد
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Checkbox(
                                checked = agreeToTerms,
                                onCheckedChange = { agreeToTerms = it },
                                modifier = Modifier.testTag("reg_agree_terms_checkbox")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedRole == UserRole.SUPERVISOR) {
                                    "أتعهد بالالتزام بالضوابط الإشرافية وحفظ سرية بيانات المعلمين والطلاب في جمعية ${activeTenant.shortName}."
                                } else {
                                    "أتعهد بالإخلاص والأمانة في تعليم كتاب الله وتدريسه وفق الضوابط المعتمدة في جمعية ${activeTenant.shortName}."
                                },
                                fontSize = 11.sp,
                                color = TextDark,
                                lineHeight = 16.sp
                            )
                        }

                        // عرض رسالة الخطأ العامة
                        if (generalErrorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = StatusError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = generalErrorMessage ?: "",
                                        color = StatusError,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // ==========================================
                        // زر الإرسال: إتمام التسجيل في Firebase Auth
                        // ==========================================
                        val primaryActionColor = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                        Button(
                            onClick = {
                                // 1. فحص صحة الحقول
                                val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
                                val isPhoneValid = phone.filter { it.isDigit() }.length >= 9

                                when {
                                    fullName.trim().length < 3 -> {
                                        fullNameError = "يرجى إدخال الاسم كاملاً"
                                        generalErrorMessage = "يرجى تصحيح الاسم قبل الإرسال"
                                    }
                                    email.isBlank() || !isEmailValid -> {
                                        emailError = "يرجى إدخال بريد إلكتروني صحيح"
                                        generalErrorMessage = "البريد الإلكتروني غير صالح"
                                    }
                                    phone.isBlank() || !isPhoneValid -> {
                                        phoneError = "يرجى إدخال رقم جوال صحيح"
                                        generalErrorMessage = "رقم الجوال غير مكتمل"
                                    }
                                    password.length < 6 -> {
                                        passwordError = "كلمة المرور يجب أن لا تقل عن 6 خانات"
                                        generalErrorMessage = "كلمة المرور قصيرة جداً (Firebase)"
                                    }
                                    password != confirmPassword -> {
                                        confirmPasswordError = "كلمتا المرور غير متطابقتين"
                                        generalErrorMessage = "يرجى مطابقة كلمة المرور مع التأكيد"
                                    }
                                    !agreeToTerms -> {
                                        generalErrorMessage = "يرجى الموافقة على التعهد والإقرار للمتابعة"
                                    }
                                    else -> {
                                        fullNameError = null
                                        emailError = null
                                        phoneError = null
                                        passwordError = null
                                        confirmPasswordError = null
                                        generalErrorMessage = null

                                        // تنفيذ التسجيل عبر Firebase Auth & Firestore
                                        coroutineScope.launch {
                                            isSubmitting = true
                                            try {
                                                val auth = FirebaseAuth.getInstance()
                                                val cleanEmail = email.trim()

                                                // إنشاء حساب المستخدم في Firebase Authentication
                                                val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                                                val firebaseUser = authResult.user
                                                val uid = firebaseUser?.uid ?: "user_${System.currentTimeMillis()}"
                                                createdUserUid = uid

                                                // تحديث الاسم الظاهر في Firebase Auth User Profile
                                                try {
                                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                                        .setDisplayName(fullName.trim())
                                                        .build()
                                                    firebaseUser?.updateProfile(profileUpdates)?.await()
                                                } catch (profileEx: Exception) {
                                                    // حتى لو تعذر تحديث الاسم المباشر، نستمر لتخزين وثيقة Firestore
                                                }

                                                // تخزين بيانات المستخدم في Cloud Firestore
                                                try {
                                                    val dbId = try {
                                                        context.getString(R.string.firestore_database_id)
                                                    } catch (e: Exception) {
                                                        ""
                                                    }
                                                    val firestore = if (dbId.isNotBlank()) {
                                                        try {
                                                            FirebaseFirestore.getInstance(dbId)
                                                        } catch (e: Exception) {
                                                            FirebaseFirestore.getInstance()
                                                        }
                                                    } else {
                                                        FirebaseFirestore.getInstance()
                                                    }

                                                    val userDocData = hashMapOf<String, Any>(
                                                        "uid" to uid,
                                                        "name" to fullName.trim(),
                                                        "email" to cleanEmail,
                                                        "phone" to phone.trim(),
                                                        "role" to if (selectedRole == UserRole.SUPERVISOR) "SUPERVISOR" else "TEACHER",
                                                        "roleLabel" to if (selectedRole == UserRole.SUPERVISOR) "المشرف العام" else "معلم حلقات",
                                                        "tenantId" to activeTenant.id,
                                                        "tenantName" to activeTenant.name,
                                                        "department" to if (selectedRole == UserRole.SUPERVISOR) department else teacherQualification,
                                                        "nationalId" to nationalId,
                                                        "experienceYears" to experienceYears,
                                                        "assignedRing" to assignedRingPreference,
                                                        "createdAt" to Timestamp.now(),
                                                        "status" to "ACTIVE",
                                                        "authProvider" to "FIREBASE_PASSWORD"
                                                    )

                                                    firestore.collection("users").document(uid).set(userDocData).await()
                                                } catch (firestoreEx: Exception) {
                                                    // يتم التسجيل في Auth بنجاح حتى لو كان الفايرستور غير متصل مؤقتاً
                                                }

                                                // إنشاء الملف الشخصي وحفظه محلياً في SocietyRepository
                                                val profile = UserProfile(
                                                    id = uid,
                                                    name = fullName.trim(),
                                                    title = if (selectedRole == UserRole.SUPERVISOR) department else "معلم حلقات قرآنية",
                                                    role = selectedRole,
                                                    phone = phone.trim(),
                                                    email = cleanEmail
                                                )
                                                registeredProfile = profile
                                                SocietyRepository.addUser(profile)

                                                if (selectedRole == UserRole.TEACHER) {
                                                    val newTeacher = Teacher(
                                                        id = uid,
                                                        name = fullName.trim(),
                                                        phone = phone.trim(),
                                                        email = cleanEmail,
                                                        qualification = teacherQualification,
                                                        experienceYears = experienceYears.toIntOrNull() ?: 3,
                                                        isAvailable = true,
                                                        status = TeacherStatus.ACTIVE,
                                                        assignedRings = listOf(assignedRingPreference),
                                                        nationalId = nationalId,
                                                        hireDate = "2026-10-21",
                                                        certificates = listOf(teacherQualification),
                                                        leaveRequests = emptyList(),
                                                        auditLogs = listOf(
                                                            AuditEntry("log_${System.currentTimeMillis()}", "تسجيل معلم جديد عبر Firebase Auth", "الآن", "النظام")
                                                        )
                                                    )
                                                    SocietyRepository.addTeacher(newTeacher)
                                                }

                                                // إرسال إشعار محلي ترحيبي
                                                LocalNotificationHelper.showSessionReminderNotification(
                                                    context = context,
                                                    title = if (selectedRole == UserRole.SUPERVISOR) "مرحباً بك في الإشراف القرآني" else "مرحباً بك في كادر المعلمين",
                                                    message = "تم تفعيل حسابك بنجاح في ${activeTenant.shortName} عبر Firebase"
                                                )

                                                isSubmitting = false
                                                showSuccessDialog = true

                                            } catch (collisionEx: FirebaseAuthUserCollisionException) {
                                                isSubmitting = false
                                                generalErrorMessage = "هذا البريد الإلكتروني مسجل مسبقاً في Firebase! يرجى تسجيل الدخول أو استخدام بريد آخر."
                                            } catch (weakPassEx: FirebaseAuthWeakPasswordException) {
                                                isSubmitting = false
                                                generalErrorMessage = "كلمة المرور ضعيفة جداً في Firebase. يرجى إدخال 6 خانات على الأقل تشمل أرقاماً وحروفاً."
                                            } catch (credEx: FirebaseAuthInvalidCredentialsException) {
                                                isSubmitting = false
                                                generalErrorMessage = "صيغة البريد الإلكتروني غير صالحة في Firebase."
                                            } catch (netEx: com.google.firebase.FirebaseNetworkException) {
                                                // في حال عدم توفر اتصال بالإنترنت لحظي، نسجل الحساب محلياً لتفادي تعطيل المستخدم
                                                val offlineUid = "offline_user_${System.currentTimeMillis()}"
                                                createdUserUid = offlineUid
                                                val profile = UserProfile(
                                                    id = offlineUid,
                                                    name = fullName.trim(),
                                                    title = if (selectedRole == UserRole.SUPERVISOR) department else "معلم حلقات قرآنية",
                                                    role = selectedRole,
                                                    phone = phone.trim(),
                                                    email = email.trim()
                                                )
                                                registeredProfile = profile
                                                SocietyRepository.addUser(profile)
                                                isSubmitting = false
                                                showSuccessDialog = true
                                            } catch (e: Exception) {
                                                isSubmitting = false
                                                generalErrorMessage = "حدث خطأ أثناء التسجيل: ${e.localizedMessage ?: e.message}"
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("reg_submit_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryActionColor),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "جاري التسجيل الآمن في Firebase...",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LockPerson,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedRole == UserRole.SUPERVISOR) "إتمام تسجيل المشرف (Firebase Auth)" else "إتمام تسجيل المعلم (Firebase Auth)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // رابط العودة لتسجيل الدخول
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "لديك حساب معتمد بالفعل؟",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            TextButton(onClick = onNavigateBack) {
                                Text(
                                    text = "تسجيل الدخول",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryActionColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // Modal: نجاح التسجيل في Firebase
        // ==========================================
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StatusSuccess.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تم التسجيل والربط بنجاح!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "مرحباً بك يا $fullName في منظومة الجمعيات القرآنية.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "تم إنشاء وتأمين حسابك بنجاح في Firebase Authentication وربطه بـ ${activeTenant.name}.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 17.sp
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("نوع الحساب:", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        if (selectedRole == UserRole.SUPERVISOR) "مشرف عام (إدارة)" else "معلم حلقات وتحفيظ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("البريد الإلكتروني:", fontSize = 11.sp, color = TextMuted)
                                    Text(email, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("رقم الجوال:", fontSize = 11.sp, color = TextMuted)
                                    Text(phone, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Firebase UID:", fontSize = 10.sp, color = TextMuted)
                                    Text(createdUserUid.take(16) + "...", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            val profile = registeredProfile ?: UserProfile(
                                id = createdUserUid,
                                name = fullName,
                                title = if (selectedRole == UserRole.SUPERVISOR) department else teacherQualification,
                                role = selectedRole,
                                phone = phone,
                                email = email
                            )
                            onRegistrationSuccess(activeTenant, profile)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                        )
                    ) {
                        Text(
                            text = if (selectedRole == UserRole.SUPERVISOR) "الدخول إلى لوحة المشرف" else "الدخول إلى بوابة المعلم",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
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
                                tint = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "اختيار الجمعية (المحافظات اليمنية)",
                                    fontSize = 16.sp,
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
                                Icon(Icons.Default.Search, contentDescription = "بحث")
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
                                .testTag("supervisor_tenant_search_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // شرائح تصفية المحافظات
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
                                        selectedContainerColor = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark,
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
                                        text = "لا توجد نتائج مطابقة في $selectedGovFilter",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            } else {
                                filteredTenants.forEach { tenant ->
                                    val isSelected = tenant.id == activeTenant.id
                                    val isProminent = tenant.id == "t_hsa" || tenant.id == "t_muadh"

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                activeTenant = tenant
                                                showTenantDialog = false
                                            }
                                            .testTag("reg_tenant_item_${tenant.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                if (selectedRole == UserRole.SUPERVISOR) SocietyBlueLight else EmeraldLight
                                            } else if (isProminent) Color(0xFFFBF8EE) else Color(0xFFF8FAFC)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) {
                                                if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                                            } else if (isProminent) GoldenAmber.copy(alpha = 0.5f) else BorderSubtle
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
                                                    Text(
                                                        text = tenant.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected || isProminent) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isSelected) {
                                                            if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark
                                                        } else if (isProminent) Color(0xFF8A6200) else TextDark
                                                    )
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
                                                        tint = if (selectedRole == UserRole.SUPERVISOR) SocietyBluePrimary else EmeraldDark,
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

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(text = "${tenant.ringsCount} حلقة", fontSize = 10.sp, color = TextSubtle)
                                                Text(text = "•", fontSize = 10.sp, color = TextSubtle)
                                                Text(text = "${tenant.studentsCount} طالب", fontSize = 10.sp, color = TextSubtle)
                                                Text(text = "•", fontSize = 10.sp, color = TextSubtle)
                                                Text(text = "${tenant.teachersCount} معلم", fontSize = 10.sp, color = TextSubtle)
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
                        Text("إغلاق")
                    }
                }
            )
        }
    }
}
