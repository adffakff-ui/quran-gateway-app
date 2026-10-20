package com.example.ui.supervisor

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
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorRegistrationScreen(
    currentTenant: Tenant,
    onNavigateBack: () -> Unit,
    onRegistrationSuccess: (Tenant, UserProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Form fields
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("إدارة الشؤون التعليمية والتحفيظ") }
    var nationalId by remember { mutableStateOf("") }

    // Password visibility
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Terms agreement
    var agreeToTerms by remember { mutableStateOf(true) }

    // Selected Tenant state
    var activeTenant by remember { mutableStateOf(currentTenant) }
    var showTenantDialog by remember { mutableStateOf(false) }

    // State & Validation
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SocietyBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = SocietyBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "تسجيل مشرف جديد",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "بوابة الجمعيات متعددة المستأجرين",
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
                // شارة الجمعية المختارة (Tenant Context)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTenantDialog = true }
                        .testTag("supervisor_reg_tenant_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SocietyBlueLight.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.4f))
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
                                tint = SocietyBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "التسجيل ضمن: ${activeTenant.name}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SocietyBlueDark
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
                            border = BorderStroke(1.dp, SocietyBluePrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("تغيير", fontSize = 11.sp, color = SocietyBluePrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // بطاقة نموذج تسجيل المشرف
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .testTag("supervisor_registration_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "بيانات الاعتماد الإشرافي والحساب",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        HorizontalDivider(color = BorderSubtle)

                        // حقل: الاسم
                        Column {
                            Text(text = "الاسم الثلاثي / اللقب الإشرافي *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = {
                                    fullName = it
                                    errorMessage = null
                                },
                                placeholder = { Text("") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SocietyBluePrimary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supervisor_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // حقل: البريد الإلكتروني
                        Column {
                            Text(text = "البريد الإلكتروني الرسمي *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                placeholder = { Text("") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = SocietyBluePrimary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supervisor_email_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // حقل: رقم الجوال
                        Column {
                            Text(text = "رقم الجوال المعتمد *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = {
                                    phone = it
                                    errorMessage = null
                                },
                                placeholder = { Text("") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = SocietyBluePrimary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supervisor_phone_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // حقل: كلمة المرور
                        Column {
                            Text(text = "كلمة المرور *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMessage = null
                                },
                                placeholder = { Text("أدخل كلمة مرور قوية (8 خانات فأكثر)") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SocietyBluePrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supervisor_password_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // تأكيد كلمة المرور
                        Column {
                            Text(text = "تأكيد كلمة المرور *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    errorMessage = null
                                },
                                placeholder = { Text("أعد كتابة كلمة المرور") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.LockReset, contentDescription = null, tint = SocietyBluePrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supervisor_confirm_password_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // حقل: القسم الإشرافي
                        Column {
                            Text(text = "القسم الإشرافي / الاعتماد الإداري", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = department,
                                onValueChange = { department = it },
                                placeholder = { Text(":") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = SocietyBluePrimary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supervisor_department_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // حقل: رقم الهوية الوطنية
                        Column {
                            Text(text = "رقم الهوية الوطنية / الإقامة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = nationalId,
                                onValueChange = { nationalId = it },
                                placeholder = { Text("") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.PermIdentity, contentDescription = null, tint = SocietyBluePrimary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supervisor_national_id_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // إقرار وضوابط الصلاحيات الإشرافية
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
                                modifier = Modifier.testTag("agree_terms_checkbox")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أتعهد بالالتزام بالضوابط الإشرافية وحفظ سرية بيانات المعلمين والطلاب في جمعية ${activeTenant.shortName}.",
                                fontSize = 11.sp,
                                color = TextDark,
                                lineHeight = 16.sp
                            )
                        }

                        // رسالة خطأ التحقق
                        if (errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = errorMessage ?: "", color = StatusError, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // زر: إتمام تسجيل المشرف
                        Button(
                            onClick = {
                                // التحقق من الحقول
                                when {
                                    fullName.isBlank() -> errorMessage = "يرجى إدخال اسم المشرف كاملاً"
                                    email.isBlank() || !email.contains("@") -> errorMessage = "يرجى إدخال بريد إلكتروني صحيح"
                                    phone.isBlank() || phone.length < 9 -> errorMessage = "يرجى إدخال رقم جوال صحيح (05xxxxxxxx)"
                                    password.isBlank() || password.length < 6 -> errorMessage = "يجب ألا تقل كلمة المرور عن 6 خانات"
                                    password != confirmPassword -> errorMessage = "كلمة المرور وتأكيدها غير متطابقين"
                                    !agreeToTerms -> errorMessage = "يرجى الموافقة على التعهد الإشرافي للإكمال"
                                    else -> {
                                        errorMessage = null
                                        coroutineScope.launch {
                                            isSubmitting = true
                                            delay(500)
                                            isSubmitting = false
                                            showSuccessDialog = true
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_supervisor_registration_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري تسجيل الحساب الإشرافي...", fontSize = 14.sp)
                            } else {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إتمام تسجيل المشرف",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // رابط العودة لتسجيل الدخول
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "لديك حساب إشرافي بالفعل؟", fontSize = 12.sp, color = TextMuted)
                            TextButton(onClick = onNavigateBack) {
                                Text(
                                    text = "تسجيل الدخول",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SocietyBluePrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // Modal: نجاح التسجيل الإشرافي
        // ==========================================
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "تم تسجيل المشرف بنجاح!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "مرحباً بك يا $fullName في منظومة الجمعية الرقمية.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "تم تفعيل حسابك الإشرافي بنجاح وربطه بـ: ${activeTenant.name} (${department}).",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SocietyBlueLight),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "اسم المستخدم: $phone", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SocietyBluePrimary)
                                Text(text = "البريد: $email", fontSize = 11.sp, color = TextDark)
                                Text(text = "الصلاحية: المشرف العام على الحلقات", fontSize = 11.sp, color = EmeraldDark)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            val profile = UserProfile(
                                id = "sup_${System.currentTimeMillis()}",
                                name = fullName,
                                title = department,
                                role = UserRole.SUPERVISOR,
                                phone = phone,
                                email = email
                            )
                            onRegistrationSuccess(activeTenant, profile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("الدخول إلى لوحة المشرف الآن")
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
                                tint = SocietyBluePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "اختيار الجمعية (المحافظات اليمنية)",
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
                                .testTag("supervisor_tenant_search_input"),
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
                                                    Text(
                                                        text = tenant.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected || isProminent) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isSelected) SocietyBluePrimary else if (isProminent) Color(0xFF8A6200) else TextDark
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
                        Text("إغلاق", color = SocietyBluePrimary)
                    }
                }
            )
        }
    }
}
