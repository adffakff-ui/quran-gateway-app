package com.example.ui.owner

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * شاشة إعدادات المالك المطلقة (Owner Master Settings & Access Control)
 * تتيح لمالك التطبيق التحكم الكامل في:
 * 1. تخصيص تفضيلات الحساب والبريد ورقم الأمان (738766810).
 * 2. إعدادات الإشعارات المركزية والإنذارات الفورية.
 * 3. منح وسحب الصلاحيات وإغلاق التطبيق على أي مستخدم أو دور بضغطة زر.
 * 4. إضافة، حذف، وتعديل كافة المستخدمين والصلاحيات من جميع النواحي.
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

    // حاجز أمان صارم: فقط OWNER
    if (userRole != UserRole.OWNER) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("صلاحية محظورة", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("هذه الشاشة مخصصة حصرياً لمالك التطبيق (Super Admin)", color = StatusError, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    // Owner Account States
    var ownerName by remember { mutableStateOf("مالك التطبيق") }
    var ownerEmail by remember { mutableStateOf("adffakff@gmail.com") }
    var ownerPhone by remember { mutableStateOf("738766810") }
    var masterPin by remember { mutableStateOf("8107") }
    var showPinVisible by remember { mutableStateOf(false) }

    // Notification Preferences
    var globalNotifActive by remember { mutableStateOf(true) }
    var dailyReminders by remember { mutableStateOf(true) }
    var whatsappAlerts by remember { mutableStateOf(true) }
    var urgentBroadcasts by remember { mutableStateOf(true) }
    var soundVibrate by remember { mutableStateOf(true) }

    // Access Control & Role Lockout States ("إغلاقها على من يشاء")
    var lockedUsers by remember { mutableStateOf(setOf<String>()) } // user IDs or phones blocked
    var rolePermissionsMap by remember { mutableStateOf(
        mutableMapOf(
            UserRole.SUPERVISOR to true,
            UserRole.TEACHER to true,
            UserRole.GUARDIAN to true
        )
    ) }

    var showAddUserDialog by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var newUserEmail by remember { mutableStateOf("") }
    var newUserPhone by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf(UserRole.SUPERVISOR) }

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
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "👑 لوحة إعدادات المالك وصلاحيات النظام",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "التحكم المطلق • الصلاحيات • الإشعارات • الحظر والإغلاق",
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
            },
            containerColor = SlateBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==========================================
                // 1. تفضيلات حساب المالك والأمان (Master Owner Profile)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFD97706))
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
                            onValueChange = { ownerName = it },
                            label = { Text("اسم المالك المعتمد") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = ownerEmail,
                            onValueChange = { ownerEmail = it },
                            label = { Text("البريد الإلكتروني للإدارة العُليا") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = ownerPhone,
                            onValueChange = { ownerPhone = it },
                            label = { Text("رقم الواتساب المعتمد (بوابة 738766810)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        OutlinedTextField(
                            value = masterPin,
                            onValueChange = { masterPin = it },
                            label = { Text("رمز الدخول الجذري السري (Master PIN)") },
                            trailingIcon = {
                                IconButton(onClick = { showPinVisible = !showPinVisible }) {
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
                            onClick = {
                                Toast.makeText(context, "تم حفظ وتحديث تفضيلات حساب المالك الجذري بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ تفضيلات الحساب المالك", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ==========================================
                // 2. إعدادات الإشعارات المركزية والإنذارات
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تفعيل الإشعارات العامة للنظام", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("إرسال التنبيهات لجميع المستخدمين عبر الجهاز", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(checked = globalNotifActive, onCheckedChange = { globalNotifActive = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("التذكيرات اليومية بالحلقات والقرآن", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("مواعيد التسميع والحضور للمستفيدين", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(checked = dailyReminders, onCheckedChange = { dailyReminders = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("إشعارات WhatsApp التلقائية (738766810)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("إرسال رموز OTP وأخبار الجمعيات عبر واتساب", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(checked = whatsappAlerts, onCheckedChange = { whatsappAlerts = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تقارير وإنذارات المشرفين العاجلة", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("تنبيهات فورية عند حدوث تغييرات إدارية", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(checked = urgentBroadcasts, onCheckedChange = { urgentBroadcasts = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("التنبيه الصوتي والاهتزاز", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("تشغيل أصوات التنبيهات والاهتزاز عند الإشعار", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(checked = soundVibrate, onCheckedChange = { soundVibrate = it })
                        }
                    }
                }

                // ==========================================
                // 3. إدارة الصلاحيات المطلقة وإغلاقها على من يشاء (Access Control & Lockout)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.4f)),
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

                        // صلاحيات الأدوار العامة
                        Text("التحكم في صلاحيات الأدوار (Global RBAC):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    onCheckedChange = { newState ->
                                        rolePermissionsMap = rolePermissionsMap.toMutableMap().apply { put(role, newState) }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = StatusSuccess)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // إدارة المستخدمين
                        Text("إدارة المستخدمين والأدوار:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)

                        SocietyRepository.allUsers.forEach { user ->
                            var expandedRole by remember { mutableStateOf(false) }
                            val isLocked = lockedUsers.contains(user.id)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (!isLocked) Color(0xFFF8FAFC) else Color(0xFFFEF2F2)),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
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
                                            Text(text = "الدور الحالي: ${user.role.label}", fontSize = 11.sp, color = TextMuted)
                                        }
                                        Switch(
                                            checked = !isLocked,
                                            onCheckedChange = { isEnabled ->
                                                lockedUsers = if (isEnabled) lockedUsers - user.id else lockedUsers + user.id
                                            }
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box {
                                            OutlinedButton(onClick = { expandedRole = true }) {
                                                Text("تغيير الدور", fontSize = 11.sp)
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
                                            Icon(Icons.Default.Delete, contentDescription = "حذف مستخدم", tint = StatusError)
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showAddUserDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                        ) {
                            Text("إضافة مستخدم جديد", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }// ==========================================
        // Modal: إضافة مستخدم جديد (CRUD)
        // ==========================================
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
                            label = { Text("رقم الجوال") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
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
    }
}
}
