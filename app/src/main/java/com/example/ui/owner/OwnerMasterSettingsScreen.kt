package com.example.ui.owner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة إعدادات المالك المطلقة (Owner Master Settings & Access Control)
 * منظمة بتبويبات (Tabs) تفاعلية عصرية:
 * 1. 👑 حساب المالك والجذر (Master Account & Root Security)
 * 2. 🔔 إدارة الإشعارات المركزية (Notification & WhatsApp Gateway 738766810)
 * 3. 🛡️ الصلاحيات وإدارة المستخدمين (RBAC & User Access / Ban Control)
 * 4. 💰 الأوقاف والتبرعات والمالية (Waqf & Financial Contributions Hub)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerMasterSettingsScreen(
    currentTenant: Tenant,
    userRole: UserRole,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler { onNavigateBack() }

    // التحقق من صلاحية المالك أو إدخال رمز التحقق السري
    var isUnlockedByPin by remember { mutableStateOf(userRole == UserRole.OWNER) }
    var enteredGatewayPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Owner Account States
    var ownerName by remember { mutableStateOf("مالك التطبيق (Super Admin)") }
    var ownerEmail by remember { mutableStateOf("adffakff@gmail.com") }
    var ownerPhone by remember { mutableStateOf("738766810") }
    var masterPin by remember { mutableStateOf("8107") }
    var showPinVisible by remember { mutableStateOf(false) }

    // Selected Tab State (0: Account, 1: Notifications, 2: Permissions/RBAC, 3: Waqf & Contributions)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "👑 حساب المالك",
        "🔔 الإشعارات",
        "🛡️ الصلاحيات",
        "💰 الأوقاف والمالية"
    )

    // Notification Preferences
    var globalNotifActive by remember { mutableStateOf(true) }
    var dailyReminders by remember { mutableStateOf(true) }
    var whatsappAlerts by remember { mutableStateOf(true) }
    var urgentBroadcasts by remember { mutableStateOf(true) }
    var soundVibrate by remember { mutableStateOf(true) }

    // Access Control & Role Lockout States
    var lockedUsers by remember { mutableStateOf(setOf<String>()) }
    var rolePermissionsMap by remember {
        mutableStateOf(
            mutableMapOf(
                UserRole.SUPERVISOR to true,
                UserRole.TEACHER to true,
                UserRole.GUARDIAN to true
            )
        )
    }

    // Modal: Add User
    var showAddUserDialog by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var newUserEmail by remember { mutableStateOf("") }
    var newUserPhone by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf(UserRole.SUPERVISOR) }

    // Modal: Add Waqf Contribution
    var showAddWaqfDialog by remember { mutableStateOf(false) }
    var donorNameInput by remember { mutableStateOf("") }
    var donorPhoneInput by remember { mutableStateOf("") }
    var donorAmountInput by remember { mutableStateOf("") }
    var donorTypeInput by remember { mutableStateOf("كفالة حلقة قرآنية") }
    var donorRingTargetInput by remember { mutableStateOf("حلقة الإمام عاصم") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // إذا لم يكن مصرحاً به بعد، نعرض بوابة إدخال الرمز السري دون حظر عشوائي
        if (!isUnlockedByPin) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = GoldenAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بوابة المالك المحمية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                containerColor = SlateBackground
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderSubtle),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(GoldenAmberLight, GoldenAmber.copy(alpha = 0.2f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = GoldenAmber,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "التحقق من هوية مالك النظام",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )

                            Text(
                                text = "أدخل الرمز السري الجذري (Master PIN) للوصول إلى أدوات التحكم المطلقة والصلاحيات والأوقاف.",
                                fontSize = 13.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = enteredGatewayPin,
                                onValueChange = {
                                    enteredGatewayPin = it
                                    pinError = false
                                },
                                label = { Text("الرمز السري (PIN)") },
                                placeholder = { Text("أدخل الرمز السري") },
                                singleLine = true,
                                isError = pinError,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("owner_gateway_pin_input")
                            )

                            if (pinError) {
                                Text(
                                    text = "الرمز السري غير صحيح! يرجى التأكد والمحاولة ثانية.",
                                    color = StatusError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    if (enteredGatewayPin == "8107" || enteredGatewayPin == "738766810" || enteredGatewayPin == masterPin) {
                                        isUnlockedByPin = true
                                        Toast.makeText(context, "تم التحقق من هوية المالك بنجاح!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        pinError = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("owner_gateway_unlock_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فتح لوحة المالك", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // الواجهة الرئيسية لإعدادات المالك مع التبويبات الأربعة
            Scaffold(
            topBar = {
                Column(modifier = Modifier.background(Color.White)) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = GoldenAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "👑 لوحة إعدادات المالك وصلاحيات النظام",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "التحكم الشامل • الأوقاف والمالية • الإشعارات • الحظر الفوري",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("owner_master_back_btn")) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )

                    // تبويبات (TabRow) أنيقة لتنظيم خيارات المالك
                    PrimaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.White,
                        contentColor = GoldenAmber,
                        modifier = Modifier.fillMaxWidth().testTag("owner_settings_tab_row")
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }
                }
            },
            containerColor = SlateBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTabIndex) {
                    0 -> OwnerAccountTab(
                        ownerName = ownerName,
                        onNameChange = { ownerName = it },
                        ownerEmail = ownerEmail,
                        onEmailChange = { ownerEmail = it },
                        ownerPhone = ownerPhone,
                        onPhoneChange = { ownerPhone = it },
                        masterPin = masterPin,
                        onPinChange = { masterPin = it },
                        showPinVisible = showPinVisible,
                        onTogglePinVisibility = { showPinVisible = !showPinVisible },
                        onSave = {
                            Toast.makeText(context, "تم حفظ وتحديث تفضيلات حساب المالك الجذري بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    1 -> OwnerNotificationsTab(
                        globalNotifActive = globalNotifActive,
                        onGlobalNotifChange = { globalNotifActive = it },
                        dailyReminders = dailyReminders,
                        onDailyRemindersChange = { dailyReminders = it },
                        whatsappAlerts = whatsappAlerts,
                        onWhatsappAlertsChange = { whatsappAlerts = it },
                        urgentBroadcasts = urgentBroadcasts,
                        onUrgentBroadcastsChange = { urgentBroadcasts = it },
                        soundVibrate = soundVibrate,
                        onSoundVibrateChange = { soundVibrate = it }
                    )

                    2 -> OwnerPermissionsTab(
                        rolePermissionsMap = rolePermissionsMap,
                        onRolePermissionChange = { role, allowed ->
                            rolePermissionsMap = rolePermissionsMap.toMutableMap().apply { put(role, allowed) }
                        },
                        lockedUsers = lockedUsers,
                        onToggleUserLock = { userId ->
                            lockedUsers = if (lockedUsers.contains(userId)) lockedUsers - userId else lockedUsers + userId
                        },
                        onOpenAddUser = { showAddUserDialog = true }
                    )

                    3 -> OwnerWaqfFinanceTab(
                        onOpenAddWaqf = { showAddWaqfDialog = true },
                        onShareWhatsAppReceipt = { waqf ->
                            val msg = "السلام عليكم ورحمة الله وبركاته،\n" +
                                    "تشكركم جمعية تحفيظ القرآن الكريم على مساهمتكم الكريمة:\n" +
                                    "المتبرع: ${waqf.donorName}\n" +
                                    "النوع: ${waqf.type}\n" +
                                    "المبلغ: ${waqf.amount} ر.ي\n" +
                                    "الحلقة المستفيدة: ${waqf.targetRingName}\n" +
                                    "رقم السند: ${waqf.receiptNumber}\n" +
                                    "بارك الله في أموالكم وأهليكم وجعلها صدقة جارية في ميزان حسناتكم."
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=${waqf.donorPhone}&text=${Uri.encode(msg)}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر فتح تطبيق واتساب مباشرة", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // نافذة إضافة مستخدم جديد
            if (showAddUserDialog) {
                AlertDialog(
                    onDismissRequest = { showAddUserDialog = false },
                    title = { Text("إضافة مستخدم جديد ومنح الصلاحيات", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = newUserName,
                                onValueChange = { newUserName = it },
                                label = { Text("الاسم الكامل") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = newUserEmail,
                                onValueChange = { newUserEmail = it },
                                label = { Text("البريد الإلكتروني") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = newUserPhone,
                                onValueChange = { newUserPhone = it },
                                label = { Text("رقم الجوال (واتساب)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newUserName.isNotBlank()) {
                                    val newUser = UserProfile(
                                        id = "usr_${System.currentTimeMillis()}",
                                        name = newUserName.trim(),
                                        title = "مستخدم معتمد",
                                        role = newUserRole,
                                        phone = newUserPhone.trim(),
                                        email = newUserEmail.trim()
                                    )
                                    SocietyRepository.addUser(newUser)
                                    showAddUserDialog = false
                                    newUserName = ""
                                    newUserEmail = ""
                                    newUserPhone = ""
                                    Toast.makeText(context, "تمت إضافة المستخدم ومنحه الصلاحيات بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                        ) {
                            Text("إضافة وحفظ")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddUserDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // نافذة إضافة تبرع / كفالة وقفية جديدة
            if (showAddWaqfDialog) {
                AlertDialog(
                    onDismissRequest = { showAddWaqfDialog = false },
                    title = { Text("إضافة كفالة أو تبرع وقفي جديد", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = donorNameInput,
                                onValueChange = { donorNameInput = it },
                                label = { Text("اسم المتبرع / الكافل") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = donorPhoneInput,
                                onValueChange = { donorPhoneInput = it },
                                label = { Text("رقم جوال الكافل (لإرسال السند عبر واتساب)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            OutlinedTextField(
                                value = donorAmountInput,
                                onValueChange = { donorAmountInput = it },
                                label = { Text("المبلغ (ر.ي / ر.س)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = donorTypeInput,
                                onValueChange = { donorTypeInput = it },
                                label = { Text("نوع المساهمة (كفالة حلقة / طالب / وقف)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = donorRingTargetInput,
                                onValueChange = { donorRingTargetInput = it },
                                label = { Text("الحلقة المستفيدة") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (donorNameInput.isNotBlank() && donorAmountInput.isNotBlank()) {
                                    val amt = donorAmountInput.toDoubleOrNull() ?: 0.0
                                    val newContribution = WaqfContribution(
                                        id = "wqf_${System.currentTimeMillis()}",
                                        donorName = donorNameInput.trim(),
                                        donorPhone = donorPhoneInput.trim(),
                                        amount = amt,
                                        type = donorTypeInput.trim(),
                                        targetRingName = donorRingTargetInput.trim(),
                                        date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()),
                                        receiptNumber = "REC-${System.currentTimeMillis() % 100000}"
                                    )
                                    SocietyRepository.addWaqfContribution(newContribution)
                                    showAddWaqfDialog = false
                                    donorNameInput = ""
                                    donorPhoneInput = ""
                                    donorAmountInput = ""
                                    Toast.makeText(context, "تم تسجيل المساهمة الوقفية وإصدار السند بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                        ) {
                            Text("إصدار السند وحفظ المساهمة")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddWaqfDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }
    }
}
}

// -------------------------------------------------------------
// 1. تبويب حساب المالك والأمان الجذري
// -------------------------------------------------------------
@Composable
fun OwnerAccountTab(
    ownerName: String,
    onNameChange: (String) -> Unit,
    ownerEmail: String,
    onEmailChange: (String) -> Unit,
    ownerPhone: String,
    onPhoneChange: (String) -> Unit,
    masterPin: String,
    onPinChange: (String) -> Unit,
    showPinVisible: Boolean,
    onTogglePinVisibility: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = GoldenAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تفضيلات حساب مالك التطبيق والاعتماد الجذري (Root)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = onNameChange,
                    label = { Text("اسم المالك المعتمد") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ownerEmail,
                    onValueChange = onEmailChange,
                    label = { Text("البريد الإلكتروني للإدارة العُليا") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ownerPhone,
                    onValueChange = onPhoneChange,
                    label = { Text("رقم الواتساب المعتمد لبوابة النظام (738766810)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = masterPin,
                    onValueChange = onPinChange,
                    label = { Text("رمز الدخول الجذري السري (Master PIN)") },
                    trailingIcon = {
                        IconButton(onClick = onTogglePinVisibility) {
                            Icon(
                                imageVector = if (showPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showPinVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ تفضيلات الحساب المالك", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. تبويب إدارة الإشعارات والإنذارات المركزية
// -------------------------------------------------------------
@Composable
fun OwnerNotificationsTab(
    globalNotifActive: Boolean,
    onGlobalNotifChange: (Boolean) -> Unit,
    dailyReminders: Boolean,
    onDailyRemindersChange: (Boolean) -> Unit,
    whatsappAlerts: Boolean,
    onWhatsappAlertsChange: (Boolean) -> Unit,
    urgentBroadcasts: Boolean,
    onUrgentBroadcastsChange: (Boolean) -> Unit,
    soundVibrate: Boolean,
    onSoundVibrateChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SocietyBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "التحكم في إعدادات الإشعارات والإنذارات الفورية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                NotificationRow(
                    title = "تفعيل الإشعارات العامة للنظام",
                    subtitle = "إرسال التنبيهات لجميع المستخدمين عبر أجهزة الهاتف",
                    checked = globalNotifActive,
                    onCheckedChange = onGlobalNotifChange
                )

                NotificationRow(
                    title = "التذكيرات اليومية بالحلقات والقرآن",
                    subtitle = "مواعيد التسميع اليومي وجدول الحفظ الميداني",
                    checked = dailyReminders,
                    onCheckedChange = onDailyRemindersChange
                )

                NotificationRow(
                    title = "إشعارات WhatsApp التلقائية (بوابة 738766810)",
                    subtitle = "إرسال رموز OTP وتقارير الحلقات لأولياء الأمور والمعلمين",
                    checked = whatsappAlerts,
                    onCheckedChange = onWhatsappAlertsChange
                )

                NotificationRow(
                    title = "تقارير وإنذارات المشرفين العاجلة",
                    subtitle = "تنبيهات فورية للمالك عند حدوث أي تعديل إداري هام",
                    checked = urgentBroadcasts,
                    onCheckedChange = onUrgentBroadcastsChange
                )

                NotificationRow(
                    title = "التنبيه الصوتي والاهتزاز المخصص",
                    subtitle = "تشغيل نغمات التنبيه التفاعلية عند وصول إشعار جديد",
                    checked = soundVibrate,
                    onCheckedChange = onSoundVibrateChange
                )
            }
        }
    }
}

@Composable
fun NotificationRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Text(subtitle, fontSize = 11.sp, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = StatusSuccess)
        )
    }
}

// -------------------------------------------------------------
// 3. تبويب الصلاحيات المطلقة وإدارة المستخدمين (RBAC)
// -------------------------------------------------------------
@Composable
fun OwnerPermissionsTab(
    rolePermissionsMap: Map<UserRole, Boolean>,
    onRolePermissionChange: (UserRole, Boolean) -> Unit,
    lockedUsers: Set<String>,
    onToggleUserLock: (String) -> Unit,
    onOpenAddUser: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة نظام الحماية ومسح البيانات لمنع العمليات المجهولة (Zero-Trust)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.2.dp, Color(0xFFDC2626)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFDC2626).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "نظام الحماية ومسح البيانات (Zero-Trust)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "حظر أي وصول مجهول الهوية وتطهير الجلسة فوراً",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Switch(
                        checked = ZeroTrustSecuritySystem.isStrictZeroTrustEnabled,
                        onCheckedChange = { ZeroTrustSecuritySystem.isStrictZeroTrustEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFEF4444),
                            checkedTrackColor = Color(0xFF7F1D1D)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF334155))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${ZeroTrustSecuritySystem.blockedAnonymousAttemptsCount}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEF4444)
                            )
                            Text("محاولات مجهولة محظورة", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${ZeroTrustSecuritySystem.getAllRegisteredUsers().size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981)
                            )
                            Text("مستخدمون مسجلون معتمدون", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }

                // زر مسح وتطهير البيانات الحساسة اليدوي
                val ctx = LocalContext.current
                OutlinedButton(
                    onClick = {
                        val result = ZeroTrustSecuritySystem.executeEmergencyDataWipe(
                            context = ctx,
                            wipeLevel = WipeLevel.EMERGENCY_DEEP_PURGE,
                            reason = "تطهير أمني يدوي من قبل مالك النظام"
                        )
                        Toast.makeText(ctx, result, Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مسح وتطهير البيانات الحساسة فوراً (Emergency Wipe)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockPerson, contentDescription = null, tint = StatusError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "منح وسحب الصلاحيات وإغلاق التطبيق على من يشاء",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Text(
                    text = "بصفتك مالك التطبيق، يمكنك التحكم في منح حق الدخول لكل دور أو إغلاق التطبيق فوراً على أي حساب مخالف.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )

                HorizontalDivider(color = BorderSubtle)

                Text("صلاحيات الأدوار العامة (Global RBAC):", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                rolePermissionsMap.forEach { (role, isAllowed) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isAllowed) Color(0xFFF0FDF4) else Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الدور: ${role.label}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAllowed) EmeraldDark else StatusError
                            )
                        }
                        Switch(
                            checked = isAllowed,
                            onCheckedChange = { onRolePermissionChange(role, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = StatusSuccess)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إدارة حسابات المستخدمين:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Button(
                        onClick = onOpenAddUser,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة مستخدم", fontSize = 11.sp)
                    }
                }

                SocietyRepository.allUsers.forEach { user ->
                    var expandedRole by remember { mutableStateOf(false) }
                    val isLocked = lockedUsers.contains(user.id)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (!isLocked) Color(0xFFF8FAFC) else Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, if (!isLocked) BorderSubtle else StatusError.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (!isLocked) Icons.Default.Person else Icons.Default.Block,
                                    contentDescription = null,
                                    tint = if (!isLocked) SocietyBluePrimary else StatusError,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = user.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "الدور: ${user.role.label} • ${user.phone}", fontSize = 11.sp, color = TextMuted)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Switch(
                                        checked = !isLocked,
                                        onCheckedChange = { onToggleUserLock(user.id) }
                                    )
                                    Text(
                                        text = if (isLocked) "محظور" else "مفعل",
                                        fontSize = 10.sp,
                                        color = if (isLocked) StatusError else StatusSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    OutlinedButton(
                                        onClick = { expandedRole = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("تغيير الدور ▾", fontSize = 11.sp)
                                    }
                                    DropdownMenu(expanded = expandedRole, onDismissRequest = { expandedRole = false }) {
                                        UserRole.values().forEach { role ->
                                            DropdownMenuItem(
                                                text = { Text(role.label) },
                                                onClick = {
                                                    SocietyRepository.updateUserRole(user.id, role)
                                                    expandedRole = false
                                                }
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { SocietyRepository.removeUser(user.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف مستخدم", tint = StatusError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. تبويب الأوقاف والتبرعات وكفالة الحلقات (Waqf & Financial Hub)
// -------------------------------------------------------------
@Composable
fun OwnerWaqfFinanceTab(
    onOpenAddWaqf: () -> Unit,
    onShareWhatsAppReceipt: (WaqfContribution) -> Unit
) {
    val contributions = SocietyRepository.allWaqfContributions
    val totalAmount = remember(contributions) { contributions.sumOf { it.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة إحصائيات الأوقاف والمالية المجمعة
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, EmeraldDark.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF064E3B), EmeraldDark)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "صندوق الأوقاف وكفالة الحلقات",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "مباشر • معتمد",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "إجمالي التبرعات والأوقاف المحصلة:",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )

                    Text(
                        text = "${String.format("%,.0f", totalAmount)} ر.ي",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "عدد المساهمات: ${contributions.size}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "الحلقات المكفولة: 3 حلقات",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // زر إضافة تبرع / كفالة جديدة
        Button(
            onClick = onOpenAddWaqf,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_waqf_donation_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
        ) {
            Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل مساهمة وقفية أو كفالة حلقة جديدة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        // قائمة السندات والمساهمات
        Text(
            text = "سجل الكفالات والتبرعات الوقفية الصادرة:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        contributions.forEach { waqf ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSubtle),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = waqf.donorName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            Text(text = "${waqf.type} • ${waqf.targetRingName}", fontSize = 11.sp, color = TextMuted)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4)
                        ) {
                            Text(
                                text = "${String.format("%,.0f", waqf.amount)} ر.ي",
                                color = EmeraldDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سند: ${waqf.receiptNumber} • ${waqf.date}",
                            fontSize = 10.sp,
                            color = TextSubtle
                        )

                        // زر مشاركة السند عبر واتساب
                        FilledTonalButton(
                            onClick = { onShareWhatsAppReceipt(waqf) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إرسال السند واتساب", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
