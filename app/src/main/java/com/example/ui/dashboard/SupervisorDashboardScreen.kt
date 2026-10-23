package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.ui.security.BiometricHelper
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboardScreen(
    currentTenant: Tenant,
    userRole: UserRole = UserRole.SUPERVISOR,
    onNavigateToTeachers: () -> Unit,
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToChallenges: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToOwnerControl: () -> Unit = {},
    onNavigateToFirestoreTenants: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSwitchTenant: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentNavSelection by remember { mutableStateOf("dashboard") }
    var showSupervisorMenu by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }

    // Quick Actions Modals
    var showAddRingDialog by remember { mutableStateOf(false) }
    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var showSendNotificationDialog by remember { mutableStateOf(false) }

    // KPI Detail Modal
    var activeKpiDetail by remember { mutableStateOf<String?>(null) }

    // Dynamic Lists State
    var ringsList by remember { mutableStateOf(if (SocietyRepository.isProductionMode) SocietyRepository.allRings else SocietyRepository.getInitialRings()) }
    var activitiesList by remember { mutableStateOf(if (SocietyRepository.isProductionMode) emptyList() else SocietyRepository.getInitialActivities()) }
    var systemSnackbarMessage by remember { mutableStateOf<String?>(null) }
    var showResetDataConfirmDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                // ==========================================
                // قائمة جانبية يمين (Vertical Nav - collapsible):
                // - لوحة التحكم (مُلخّص)
                // - الحلقات
                // - المعلمون
                // - الطلاب
                // - الحضور
                // - التقارير والشهادات
                // - الإشعارات/الرسائل
                // - الإعدادات (بوابة الدفع، القوالب)
                // - النسخ الاحتياطي/استعادة
                // ==========================================
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    drawerContainerColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // هيدر القائمة الجانبية
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SocietyBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_society_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = currentTenant.shortName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "لوحة المشرف العام",
                                    fontSize = 11.sp,
                                    color = SocietyBluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalDivider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(10.dp))

                        // عناصر القائمة الجانبية (يتم إخفاء أقسام المالك تماماً عن غيره)
                        val navItems = buildList {
                            add(Triple("dashboard", "لوحة التحكم (مُلخّص)", Icons.Outlined.Dashboard))
                            add(Triple("rings", "الحلقات", Icons.AutoMirrored.Outlined.MenuBook))
                            add(Triple("teachers", "المعلمون", Icons.Outlined.School))
                            add(Triple("students", "الطلاب", Icons.Outlined.People))
                            add(Triple("attendance", "الحضور", Icons.Outlined.CheckCircle))
                            add(Triple("hifz_planner", "مخطط الحفظ الذكي", Icons.AutoMirrored.Outlined.EventNote))
                            add(Triple("challenges", "التحديات والمسابقات القرآنية", Icons.Outlined.WorkspacePremium))
                            add(Triple("profile", "معلومات الحساب والصلاحيات", Icons.Outlined.PersonPin))
                            if (userRole == UserRole.OWNER) {
                                add(Triple("owner_control", "👑 لوحة تحكم مالك النظام (Super Admin)", Icons.Default.Shield))
                                add(Triple("owner_firestore", "⚡ إدارة جمعيات Firestore (خاص بالمالك)", Icons.Default.CloudQueue))
                            }
                            add(Triple("reports", "التقارير والشهادات", Icons.Outlined.Assessment))
                            add(Triple("notifications", "الإشعارات / الرسائل", Icons.Outlined.Notifications))
                            add(Triple("settings", "الإعدادات (الدفع، القوالب)", Icons.Outlined.Settings))
                            add(Triple("backup", "النسخ الاحتياطي / استعادة", Icons.Outlined.CloudSync))
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            navItems.forEach { (id, label, icon) ->
                                val isSelected = currentNavSelection == id
                                NavigationDrawerItem(
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) SocietyBluePrimary else TextMuted
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    selected = isSelected,
                                    onClick = {
                                        if ((id == "settings" || id == "backup") && userRole != UserRole.SUPERVISOR && userRole != UserRole.OWNER) {
                                            coroutineScope.launch { drawerState.close() }
                                            Toast.makeText(context, "عزل الصلاحيات: عذراً، هذا القسم مخصص للمشرف العام فقط. دورك الحالي (${userRole.label}) لا يملك صلاحية التعديل.", Toast.LENGTH_LONG).show()
                                        } else {
                                            currentNavSelection = id
                                            coroutineScope.launch { drawerState.close() }
                                            if (id == "teachers") {
                                                onNavigateToTeachers()
                                            } else if (id == "hifz_planner") {
                                                onNavigateToPlanner()
                                            } else if (id == "challenges") {
                                                onNavigateToChallenges()
                                            } else if (id == "profile") {
                                                onNavigateToProfile()
                                            } else if (id == "owner_control") {
                                                BiometricHelper.authenticateForAdministrativeAccess(
                                                    context = context,
                                                    operationName = "الوصول إلى لوحة المالك الشاملة",
                                                    onSuccess = { onNavigateToOwnerControl() },
                                                    onError = { Toast.makeText(context, "فشلت المصادقة الحيوية للإدارة: $it", Toast.LENGTH_SHORT).show() }
                                                )
                                            } else if (id == "owner_firestore") {
                                                BiometricHelper.authenticateForAdministrativeAccess(
                                                    context = context,
                                                    operationName = "إدارة قاعدة بيانات Firestore للجمعيات",
                                                    onSuccess = { onNavigateToFirestoreTenants() },
                                                    onError = { Toast.makeText(context, "فشلت المصادقة الحيوية للإدارة: $it", Toast.LENGTH_SHORT).show() }
                                                )
                                            } else if (id == "backup") {
                                                BiometricHelper.authenticateForAdministrativeAccess(
                                                    context = context,
                                                    operationName = "النسخ الاحتياطي واستعادة البيانات",
                                                    onSuccess = { onNavigateToBackup() },
                                                    onError = { Toast.makeText(context, "فشلت المصادقة الحيوية للإدارة: $it", Toast.LENGTH_SHORT).show() }
                                                )
                                            } else if (id != "dashboard") {
                                                activeKpiDetail = id
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = SocietyBlueLight,
                                        selectedTextColor = SocietyBluePrimary,
                                        unselectedTextColor = TextDark
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(8.dp))

                        // زر تبديل الجمعية وتسجيل الخروج
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLogout() }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "تسجيل الخروج", color = StatusError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    // ==========================================
                    // رأس الصفحة (Header - يمين):
                    // [شعار الجمعية] [اسم الجمعية] [بحث 🔍] [إشعارات 🔔] [اسم المشرف ▼]
                    // ==========================================
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // زر فتح القائمة الجانبية
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("nav_drawer_toggle")
                                ) {
                                    Icon(imageVector = Icons.Default.Menu, contentDescription = "القائمة الجانبية")
                                }

                                // [شعار الجمعية]
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SocietyBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_society_logo),
                                        contentDescription = "شعار الجمعية",
                                        modifier = Modifier.size(28.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // [اسم الجمعية]
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTenant.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "المستأجر: ${currentTenant.code} • ${currentTenant.city}",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        },
                        actions = {
                            // [بحث 🔍]
                            IconButton(
                                onClick = { showSearchDialog = true },
                                modifier = Modifier.testTag("header_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "بحث سريع في النظام",
                                    tint = TextDark
                                )
                            }

                            // [إشعارات 🔔]
                            IconButton(
                                onClick = { showNotificationSheet = true },
                                modifier = Modifier.testTag("header_notifications_btn")
                            ) {
                                BadgedBox(badge = {
                                    Badge(containerColor = StatusWarning) {
                                        Text(text = "${currentTenant.unreadMessagesCount}")
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "الإشعارات",
                                        tint = TextDark
                                    )
                                }
                            }

                            // [اسم المشرف ▼]
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showSupervisorMenu = true }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("supervisor_profile_menu")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(SocietyBluePrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ع",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = SocietyRepository.defaultSupervisor.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSupervisorMenu,
                                    onDismissRequest = { showSupervisorMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("تبديل الجمعية (Tenant)") },
                                        leadingIcon = { Icon(Icons.Default.CorporateFare, contentDescription = null) },
                                        onClick = {
                                            showSupervisorMenu = false
                                            onSwitchTenant()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("إدارة المعلمين") },
                                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                                        onClick = {
                                            showSupervisorMenu = false
                                            onNavigateToTeachers()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("إعدادات التطبيق والنظام") },
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = SocietyBluePrimary) },
                                        onClick = {
                                            showSupervisorMenu = false
                                            onNavigateToSettings()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("تسجيل الخروج") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = StatusError) },
                                        onClick = {
                                            showSupervisorMenu = false
                                            onLogout()
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = onNavigateToSettings,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "إعدادات النظام والتطبيق"
                            )
                        },
                        text = {
                            Text(
                                text = "الإعدادات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        },
                        containerColor = SocietyBluePrimary,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier.testTag("dashboard_settings_fab")
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
                    if (systemSnackbarMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SocietyBlueLight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SocietyBluePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = systemSnackbarMessage ?: "", fontSize = 12.sp, color = SocietyBlueDark, modifier = Modifier.weight(1f))
                                IconButton(onClick = { systemSnackbarMessage = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                                }
                            }
                        }
                    }

                    // Production Mode banner hidden for cleaner UI as requested


                    // ==========================================
                    // صف KPI أعلى (4 بطاقات)
                    // 1. عدد الحلقات | ايقونة | رابط "عرض الكل"
                    // 2. عدد الطلاب المسجلين
                    // 3. حضور اليوم: نسبة %
                    // 4. رسائل غير مقروءة
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // بطاقة 1: عدد الحلقات
                        KpiCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_rings_card"),
                            title = "عدد الحلقات",
                            value = "${ringsList.size}",
                            subtitle = "حلقة قرآنية",
                            actionText = "عرض الكل",
                            icon = Icons.AutoMirrored.Outlined.MenuBook,
                            accentColor = SocietyBluePrimary,
                            onClick = { activeKpiDetail = "rings" }
                        )

                        // بطاقة 2: عدد الطلاب المسجلين
                        KpiCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_students_card"),
                            title = "الطلاب المسجلون",
                            value = "${currentTenant.studentsCount}",
                            subtitle = "+8% هذا الشهر",
                            actionText = "عرض السجل",
                            icon = Icons.Outlined.People,
                            accentColor = EmeraldDark,
                            onClick = { activeKpiDetail = "students" }
                        )

                        // بطاقة 3: حضور اليوم: نسبة %
                        KpiCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_attendance_card"),
                            title = "حضور اليوم",
                            value = "${currentTenant.attendanceRate}%",
                            subtitle = "ممتاز جداً",
                            actionText = "سجل التحضير",
                            icon = Icons.Outlined.CheckCircle,
                            accentColor = StatusSuccess,
                            onClick = { activeKpiDetail = "attendance" }
                        )

                        // بطاقة 4: رسائل غير مقروءة
                        KpiCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_messages_card"),
                            title = "رسائل غير مقروءة",
                            value = "${currentTenant.unreadMessagesCount}",
                            subtitle = "طلبات واستفسارات",
                            actionText = "فتح البريد",
                            icon = Icons.Outlined.MarkChatUnread,
                            accentColor = GoldenAmber,
                            onClick = { activeKpiDetail = "notifications" }
                        )
                    }

                    // ==========================================
                    // أزرار إجراءات سريعة:
                    // + إضافة حلقة   + إضافة معلم   + إرسال إشعار
                    // ==========================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "إجراءات سريعة:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // + إضافة حلقة
                                Button(
                                    onClick = { showAddRingDialog = true },
                                    modifier = Modifier.testTag("quick_action_add_ring"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ إضافة حلقة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // + إضافة معلم
                                Button(
                                    onClick = { showAddTeacherDialog = true },
                                    modifier = Modifier.testTag("quick_action_add_teacher"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ إضافة معلم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // + إرسال إشعار
                                OutlinedButton(
                                    onClick = { showSendNotificationDialog = true },
                                    modifier = Modifier.testTag("quick_action_send_notice"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenAmber),
                                    border = BorderStroke(1.dp, GoldenAmber),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ إرسال إشعار", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // النسخ الاحتياطي السحابي (Firebase Storage)
                                Button(
                                    onClick = onNavigateToBackup,
                                    modifier = Modifier.testTag("quick_action_backup_storage"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SocietyBlueDark),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("النسخ والاستعادة (Storage)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // ==========================================
                    // منطقة المخطط والنشاطات (Split / Grid):
                    // - مخطط تقدم الحفظ الشهري (يمين-في-المحتوى)
                    // - جدول سريع: آخر 5 نشاطات (يسار-في-المحتوى)
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // مخطط تقدم الحفظ وتحليل تصنيف السور (مكية ومدنية)
                        Column(modifier = Modifier.weight(1.3f)) {
                            QuranMemorizationChartSection(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("progress_chart_card")
                            )

                            // زر تصدير التقرير إلى PDF أسفل المخطط مباشرة
                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "جاري تحضير التقرير وتصديره بصيغة PDF...", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .testTag("export_pdf_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SocietyBluePrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SocietyBluePrimary)
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تصدير التقرير إلى PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // جدول سريع / رؤية سريعة (يسار-في-المحتوى): آخر 5 نشاطات
                        Card(
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("recent_activities_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "آخر 5 نشاطات وتحديثات",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    TextButton(onClick = { activeKpiDetail = "activities" }) {
                                        Text("عرض الكل", fontSize = 11.sp, color = SocietyBluePrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    activitiesList.take(5).forEach { activity ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (activity.category) {
                                                            "تعيين" -> SocietyBlueLight
                                                            "إضافة" -> EmeraldLight
                                                            "شهادة" -> GoldenAmberLight
                                                            else -> Color(0xFFF1F5F9)
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (activity.category) {
                                                        "تعيين" -> Icons.Default.School
                                                        "إضافة" -> Icons.Default.PersonAdd
                                                        "شهادة" -> Icons.Default.EmojiEvents
                                                        "حضور" -> Icons.Default.Check
                                                        else -> Icons.Default.Campaign
                                                    },
                                                    contentDescription = null,
                                                    tint = when (activity.category) {
                                                        "تعيين" -> SocietyBluePrimary
                                                        "إضافة" -> EmeraldDark
                                                        "شهادة" -> GoldenAmber
                                                        else -> TextDark
                                                    },
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = activity.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = activity.description,
                                                    fontSize = 10.sp,
                                                    color = TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = activity.timestamp,
                                                fontSize = 9.sp,
                                                color = TextSubtle
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // لوحة مؤشرات التسجيلات النشطة والمعلقة لجميع الجمعيات
                    // ==========================================
                    ActiveVsPendingRegistrationsDashboard(
                        tenants = SocietyRepository.tenants,
                        isDarkMode = false
                    )

                    // ==========================================
                    // ميزة جديدة: المتابعة اليومية السريعة للتسميع القرآني وتكريم الطلاب
                    // ==========================================
                    DailyHifzQuickTrackerSection()

                    // بنر ترويجي سريع لربط شاشة إدارة المعلمين
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToTeachers() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SocietyBlueLight.copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.School, contentDescription = null, tint = SocietyBluePrimary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "إدارة المعلمين وتسكين الحلقات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Text(text = "عرض ملفات المعلمين، طلبات الإجازات والتبادل، وتعيين الحلقات", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                            Button(
                                onClick = onNavigateToTeachers,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("فتح إدارة المعلمين", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // ==========================================
        // Modal: تصفير البيانات والبدء بالنسخة الحقيقية للنظام
        // ==========================================
        if (showResetDataConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetDataConfirmDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = StatusError,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "تصفير البيانات وبدء النظام الفعلي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "سيتم تصفير وإفراغ كافة بيانات الجمعيات والحلقات والطلاب التجريبية فوراً، وتهيئة بيئة نظيفة 100% لبدء إدخال الحلقات والطلاب والمعلمين الفعليين بدون أي شوائب أو أخطاء برمجية.\n\nهل ترغب في تأكيد التصفير وبدء النسخة الحقيقية؟",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextDark
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            BiometricHelper.authenticateForAdministrativeAccess(
                                context = context,
                                operationName = "تصفير البيانات وبدء النظام الفعلي",
                                onSuccess = {
                                    SocietyRepository.resetAllSocietiesDataClean()
                                    ringsList = emptyList()
                                    activitiesList = emptyList()
                                    showResetDataConfirmDialog = false
                                    systemSnackbarMessage = "تم التحقق بالبصمة وتصفير جميع البيانات التجريبية بنجاح! النظام الآن في وضع التشغيل الفعلي."
                                },
                                onError = { err ->
                                    Toast.makeText(context, "فشلت المصادقة الحيوية للتصفير: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تأكيد التصفير وبدء النظام الفعلي")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDataConfirmDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // ==========================================
        // Modal: إضافة حلقة قرآنية
        // ==========================================
        if (showAddRingDialog) {
            var ringName by remember { mutableStateOf("") }
            var ringTeacher by remember { mutableStateOf("الشيخ عبد الله المنصور") }
            var ringTime by remember { mutableStateOf("بعد صلاة العصر") }
            var ringCapacity by remember { mutableStateOf("25") }

            AlertDialog(
                onDismissRequest = { showAddRingDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = SocietyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة حلقة تحفيظ جديدة", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "إنشاء حلقة جديدة في ${currentTenant.shortName}:", fontSize = 12.sp, color = TextMuted)
                        OutlinedTextField(
                            value = ringName,
                            onValueChange = { ringName = it },
                            label = { Text("اسم الحلقة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ringTeacher,
                            onValueChange = { ringTeacher = it },
                            label = { Text("المعلم المشرف على الحلقة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ringTime,
                            onValueChange = { ringTime = it },
                            label = { Text("وقت انعقاد الحلقة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ringCapacity,
                            onValueChange = { ringCapacity = it },
                            label = { Text("الطاقة الاستيعابية للطلاب") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (ringName.isNotBlank()) {
                                val newRing = QuranRing(
                                    id = "ring_${System.currentTimeMillis()}",
                                    name = ringName,
                                    teacherName = ringTeacher,
                                    timeSlot = ringTime,
                                    studentCount = 0,
                                    maxCapacity = ringCapacity.toIntOrNull() ?: 25,
                                    currentJuz = "جزء عمّ",
                                    attendancePercent = 100
                                )
                                ringsList = listOf(newRing) + ringsList
                                activitiesList = listOf(
                                    ActivityFeedItem("act_${System.currentTimeMillis()}", "إضافة $ringName", "تم تدشين الحلقة بإشراف $ringTeacher", "إضافة", "الآن")
                                ) + activitiesList
                                systemSnackbarMessage = "تمت إضافة $ringName بنجاح!"
                                showAddRingDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("حفظ الحلقة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddRingDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // ==========================================
        // Modal: إضافة معلم سريع
        // ==========================================
        if (showAddTeacherDialog) {
            AlertDialog(
                onDismissRequest = { showAddTeacherDialog = false },
                title = { Text("إضافة معلم", fontWeight = FontWeight.Bold) },
                text = {
                    Text("هل ترغب في الانتقال إلى شاشة إدارة المعلمين لإدخال الملف التعاقدي والشهادات والتسكين؟")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAddTeacherDialog = false
                            onNavigateToTeachers()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                    ) {
                        Text("الانتقال لإدارة المعلمين")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTeacherDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // ==========================================
        // Modal: إرسال إشعار عام
        // ==========================================
        if (showSendNotificationDialog) {
            var noticeTitle by remember { mutableStateOf("") }
            var noticeBody by remember { mutableStateOf("") }
            var targetAudience by remember { mutableStateOf("جميع المعلمين وأولياء الأمور") }

            AlertDialog(
                onDismissRequest = { showSendNotificationDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = GoldenAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال تعميم / إشعار", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("إرسال إشعار موحد عبر التطبيق ورسائل SMS:", fontSize = 12.sp, color = TextMuted)
                        OutlinedTextField(
                            value = noticeTitle,
                            onValueChange = { noticeTitle = it },
                            label = { Text("عنوان الإشعار") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = noticeBody,
                            onValueChange = { noticeBody = it },
                            label = { Text("نص الرسالة أو التعميم") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (noticeTitle.isNotBlank()) {
                                activitiesList = listOf(
                                    ActivityFeedItem("act_${System.currentTimeMillis()}", "إرسال تعميم: $noticeTitle", "بث إلى $targetAudience", "إشعار", "الآن")
                                ) + activitiesList
                                systemSnackbarMessage = "تم إرسال الإشعار بنجاح لجميع المستفيدين!"
                                showSendNotificationDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber)
                    ) {
                        Text("إرسال الآن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSendNotificationDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // ==========================================
        // Modal: تفاصيل البطاقات (Drilldown View)
        // ==========================================
        if (activeKpiDetail != null) {
            val title = when (activeKpiDetail) {
                "rings" -> "قائمة الحلقات القرآنية (${ringsList.size})"
                "students" -> "سجل الطلاب المسجلين (${currentTenant.studentsCount})"
                "attendance" -> "تقرير الحضور اليومي (${currentTenant.attendanceRate}%)"
                "notifications" -> "مركز الرسائل والإشعارات"
                else -> "تفاصيل النشاطات"
            }

            AlertDialog(
                onDismissRequest = { activeKpiDetail = null },
                title = { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (activeKpiDetail) {
                            "rings" -> {
                                ringsList.forEach { ring ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = ring.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                            Text(text = "المعلم: ${ring.teacherName} • الموعد: ${ring.timeSlot}", fontSize = 11.sp, color = TextMuted)
                                            Text(text = "الطلاب: ${ring.studentCount}/${ring.maxCapacity} • الحضور: ${ring.attendancePercent}%", fontSize = 11.sp, color = SocietyBluePrimary)
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = "تم جلب البيانات الحية من سياق الجمعية: ${currentTenant.name}.",
                                    fontSize = 12.sp,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الفرع: ${currentTenant.city} • كود المستأجر: ${currentTenant.code} • معدل الإنجاز العام ممتاز.",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeKpiDetail = null }) {
                        Text("إغلاق")
                    }
                }
            )
        }

        // ==========================================
        // Modal: بحث عام
        // ==========================================
        if (showSearchDialog) {
            var searchTxt by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showSearchDialog = false },
                title = { Text("بحث شامل في جمعية ${currentTenant.shortName}", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = searchTxt,
                            onValueChange = { searchTxt = it },
                            placeholder = { Text("ابحث عن حلقة، معلم، طالب، أو تقرير...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "نتائج فورية داخل سياق المستأجر الحالي", fontSize = 11.sp, color = TextMuted)
                    }
                },
                confirmButton = {
                    Button(onClick = { showSearchDialog = false }) { Text("بحث") }
                },
                dismissButton = {
                    TextButton(onClick = { showSearchDialog = false }) { Text("إغلاق") }
                }
            )
        }

        // ==========================================
        // Modal: الإشعارات
        // ==========================================
        if (showNotificationSheet) {
            AlertDialog(
                onDismissRequest = { showNotificationSheet = false },
                title = { Text("مركز الإشعارات والرسائل", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("لديك ${currentTenant.unreadMessagesCount} تنبيهات غير مقروءة:", fontSize = 12.sp, color = TextMuted)
                        activitiesList.take(3).forEach { act ->
                            Text("• ${act.title} (${act.timestamp})", fontSize = 12.sp, color = TextDark)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showNotificationSheet = false }) { Text("تم الاطلاع") }
                }
            )
        }
    }
}

// ==========================================
// بطاقة KPI الفردية (قابلة للنقر والانتقال للشاشة التفصيلية)
// ==========================================
@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    actionText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }

                Text(
                    text = actionText,
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

// ==========================================
// مخطط تقدم الحفظ الشهري (Bar Chart)
// ==========================================
@Composable
fun MonthlyProgressChart() {
    val monthsData = listOf(
        Pair("محرم", 76),
        Pair("صفر", 81),
        Pair("ربيع ١", 85),
        Pair("ربيع ٢", 89),
        Pair("جمادى ١", 92),
        Pair("جمادى ٢", 95)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            monthsData.forEach { (month, percent) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$percent%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (percent >= 90) EmeraldDark else SocietyBluePrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height((percent * 1.1).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (percent >= 90) listOf(EmeraldGreen, EmeraldDark)
                                    else listOf(SocietyBluePrimary, SocietyBlueDark)
                                )
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = month,
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * ميزة جديدة: قسم المتابعة القرآنية اليومية السريعة والتقييم بالنجوم
 */
@Composable
fun DailyHifzQuickTrackerSection() {
    val context = LocalContext.current
    val records = SocietyRepository.allDailyHifzRecords

    var showAddHifzDialog by remember { mutableStateOf(false) }
    var studentNameInput by remember { mutableStateOf("") }
    var ringNameInput by remember { mutableStateOf("حلقة الإمام عاصم") }
    var surahNameInput by remember { mutableStateOf("سورة البقرة") }
    var fromAyahInput by remember { mutableStateOf("1") }
    var toAyahInput by remember { mutableStateOf("25") }
    var tajweedRatingInput by remember { mutableIntStateOf(5) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_hifz_quick_tracker_card"),
        shape = RoundedCornerShape(18.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "📖 المتابعة اليومية السريعة للتسميع القرآني",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "تسجيل فوري للحفظ • تقييم التجويد • مباركة واتساب لأولياء الأمور",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = { showAddHifzDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسجيل تسميع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = BorderSubtle)

            // بطاقات الطلاب الذين سمعوا اليوم
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                records.take(4).forEach { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = record.studentName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldLight
                                    ) {
                                        Text(
                                            text = record.ringName,
                                            fontSize = 10.sp,
                                            color = EmeraldDark,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${record.surahName} (الآيات ${record.ayahFrom} - ${record.ayahTo})",
                                    fontSize = 11.sp,
                                    color = SocietyBluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = record.notes,
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row {
                                    repeat(record.tajweedRating) {
                                        Text("⭐", fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        val celebrationMsg = "السلام عليكم ورحمة الله،\n" +
                                                "نزف إليكم بشرى تميز ابنكم البار (${record.studentName}) في حلقة (${record.ringName}) اليوم:\n" +
                                                "المقرر المنجز: ${record.surahName} من آية ${record.ayahFrom} إلى ${record.ayahTo}\n" +
                                                "درجة التجويد والإتقان: ${record.tajweedRating}/5 نجوم ⭐\n" +
                                                "ملاحظة المعلم: ${record.notes}\n" +
                                                "هنيئاً لكم تاج الوقار، ونسأل الله أن يجعله من أهل القرآن."
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(celebrationMsg)}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "تعذر فتح واتساب للمباركة", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(12.dp), tint = EmeraldDark)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مباركة واتساب", fontSize = 10.sp, color = EmeraldDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة تسجيل تسميع جديد
    if (showAddHifzDialog) {
        AlertDialog(
            onDismissRequest = { showAddHifzDialog = false },
            title = { Text("تسجيل تسميع وإنجاز قرآني جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = studentNameInput,
                        onValueChange = { studentNameInput = it },
                        label = { Text("اسم الطالب") },
                        placeholder = { Text("أدخل اسم الطالب") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = surahNameInput,
                        onValueChange = { surahNameInput = it },
                        label = { Text("السورة الكريمة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromAyahInput,
                            onValueChange = { fromAyahInput = it },
                            label = { Text("من آية") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = toAyahInput,
                            onValueChange = { toAyahInput = it },
                            label = { Text("إلى آية") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    Text("تقييم الإتقان والتجويد:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { tajweedRatingInput = star }) {
                                Text(
                                    text = if (star <= tajweedRatingInput) "⭐" else "☆",
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (studentNameInput.isNotBlank()) {
                            val newRec = DailyHifzRecord(
                                id = "hifz_${System.currentTimeMillis()}",
                                studentName = studentNameInput.trim(),
                                ringName = ringNameInput,
                                surahName = surahNameInput.trim(),
                                ayahFrom = fromAyahInput.toIntOrNull() ?: 1,
                                ayahTo = toAyahInput.toIntOrNull() ?: 20,
                                tajweedRating = tajweedRatingInput,
                                date = "اليوم",
                                notes = "إتقان ممتاز لمقرر التسميع",
                                parentNotifiedViaWhatsApp = false
                            )
                            SocietyRepository.addDailyHifzRecord(newRec)
                            showAddHifzDialog = false
                            studentNameInput = ""
                            Toast.makeText(context, "تم تسجيل التسميع بنجاح في سجل الحلقة!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                ) {
                    Text("حفظ التسميع")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHifzDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

