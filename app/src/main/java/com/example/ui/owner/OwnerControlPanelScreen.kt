package com.example.ui.owner

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.dashboard.ActiveVsPendingRegistrationsDashboard
import com.example.ui.security.BiometricHelper
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import com.example.data.FirestoreTenantRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerControlPanelScreen(
    currentTenant: Tenant,
    userRole: UserRole = UserRole.OWNER,
    onTenantSelected: (Tenant) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSupervisorDashboard: () -> Unit,
    onNavigateToTeachers: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToChallenges: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToParentPortal: () -> Unit = {},
    onNavigateToTeacherPortal: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToFirestoreTenants: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToOwnerMasterSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestoreRepo = remember { FirestoreTenantRepository(context) }

    // Handle back button
    BackHandler {
        onNavigateBack()
    }

    // =========================================================================
    // حاجز أمان حقيقي ومشدد: منع أي شخص غير مالك التطبيق من الدخول لهذه الشاشة
    // =========================================================================
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.5.dp, StatusError)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(StatusError.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "وصول محظور تماماً (Access Denied)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        LaunchedEffect(Unit) {
                            OwnerSecurityManager.logIntrusionAttempt(
                                context = context,
                                attemptType = "محاولة دخول غير مصرح بها إلى لوحة المالك",
                                attemptedInput = "رتبة المستخدم: ${userRole.label}"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هذه اللوحة مشفرة ومخصصة حصرياً لمالك التطبيق المصرح له.\nتم حجب بيانات المالك الحساسة (${OwnerSecurityManager.maskEmail()}) لأسباب أمنية.\n\n⚠️ تم توثيق محاولة الدخول وإرسال إنذار أمني فوري لواتساب وبريد المالك.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("العودة إلى شاشتي السابقة")
                        }
                    }
                }
            }
        }
        return
    }

    // الحالة التفاعلية
    var selectedTab by remember { mutableStateOf(0) } // 0: الجمعيات, 1: المعلمون والصلاحيات, 2: الحلقات والطلاب, 3: تهيئة وبيانات النظام, 4: الوصول السريع لكافة البوابات
    var searchQuery by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var isSensitiveInfoRevealed by remember { mutableStateOf(false) }

    // حوارات الإضافة والتعديل
    var showAddTenantDialog by remember { mutableStateOf(false) }
    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var showAddRingDialog by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }

    // فلاتر البيانات
    val allTenants = SocietyRepository.tenants
    val filteredTenants = remember(searchQuery, allTenants) {
        if (searchQuery.isBlank()) allTenants
        else allTenants.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.city.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    val allTeachers = SocietyRepository.allTeachers
    val allRings = SocietyRepository.allRings
    val allStudents = SocietyRepository.allStudents
    val allUsers = SocietyRepository.allUsers

    val totalRings = remember(allTenants) { allTenants.sumOf { it.ringsCount } }
    val totalStudents = remember(allTenants) { allTenants.sumOf { it.studentsCount } }
    val totalTeachers = remember(allTenants) { allTenants.sumOf { it.teachersCount } }

    val syncStatus by SyncManager.syncStatus.collectAsState()
    val lastSyncedTime by SyncManager.lastSyncedTime.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = GoldenAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "لوحة المالك والتحكم الشامل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                        Text(
                            text = if (isSensitiveInfoRevealed) "مالك التطبيق: ${OwnerSecurityManager.OWNER_EMAIL} | هاتف: ${OwnerSecurityManager.OWNER_PHONE}" else "مالك التطبيق: ${OwnerSecurityManager.maskEmail()} | هاتف: ${OwnerSecurityManager.maskPhone()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // WhatsApp Quick Alert Button
                    IconButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://wa.me/967775484201?text=${Uri.encode("تنبيه أمني من لوحة تحكم مالك تطبيق الحلقات القرآنية")}")
                            )
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                snackbarMessage = "رقم واتساب المالك: 775484201"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "واتساب المالك",
                            tint = Color(0xFF25D366)
                        )
                    }

                    // Maintenance status badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (SocietyRepository.systemMaintenanceMode) StatusError.copy(alpha = 0.15f) else Color(0xFF2E7D32).copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (SocietyRepository.systemMaintenanceMode) StatusError else Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (SocietyRepository.systemMaintenanceMode) "صيانة" else "نشط",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (SocietyRepository.systemMaintenanceMode) StatusError else Color(0xFF2E7D32)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val ownerTabs = listOf(
                Pair("الجمعيات (${allTenants.size})", Icons.Outlined.Apartment),
                Pair("المعلمون والصلاحيات", Icons.Outlined.AdminPanelSettings),
                Pair("الحلقات والطلاب", Icons.AutoMirrored.Outlined.MenuBook),
                Pair("إدارة البيانات والنسخ", Icons.Outlined.SettingsBackupRestore),
                Pair("بوابات God Mode", Icons.AutoMirrored.Outlined.Launch),
                Pair("درع الأمان (${OwnerSecurityManager.securityIncidents.size})", Icons.Default.Security),
                Pair("مؤشر التسجيلات", Icons.Default.BarChart),
                Pair("بوابة تسجيل المستخدمين", Icons.Default.PersonAdd),
                Pair("بوابة المستخدمين Zero-Trust", Icons.Default.Fingerprint)
            )

            // شريط هيدر المالك فائق الصغر والأناقة (Compact Owner Strip)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GoldenAmber.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = GoldenAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "صاحب التطبيق والمالك الحصري (Root)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GoldenAmber.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "تحكم 100%",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        color = GoldenAmber,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${allTenants.size} جمعيات • $totalRings حلقة • $totalStudents طالب • $totalTeachers معلم",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp
                            )
                        }
                    }

                    // قائمة منسدلة سريعة للتنقل المباشر بين جميع التبويبات بنقرة واحدة
                    var showTabsDropdown by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { showTabsDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenAmber),
                            border = BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("التبويبات (${selectedTab + 1}/7)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showTabsDropdown,
                            onDismissRequest = { showTabsDropdown = false }
                        ) {
                            ownerTabs.forEachIndexed { index, tab ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = tab.first,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == index) GoldenAmber else Color.Unspecified,
                                            fontSize = 11.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = tab.second,
                                            contentDescription = null,
                                            tint = if (selectedTab == index) GoldenAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        selectedTab = index
                                        showTabsDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // شريط تبويبات مدمج وقابل للتمرير بارتفاع مضغوط (40dp) لترك أقصى مساحة للمحتوى
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GoldenAmber,
                edgePadding = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                ownerTabs.forEachIndexed { index, tabItem ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tabItem.second,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (selectedTab == index) GoldenAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tabItem.first,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) GoldenAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    )
                }
            }

            // حاوية المحتوى المشغولة بنسبة 100% من المساحة المتبقية لمنع أي اقتطاع للشاشة
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                0 -> {
                    // ==========================================
                    // تبويب 0: إدارة الجمعيات والمستأجرين بالكامل
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // زر الانتقال لواجهة Firestore السحابية الحصرية
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clickable { onNavigateToFirestoreTenants() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, GoldenAmber)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("واجهة Firestore السحابية المباشرة (إضافة / تعديل / حذف)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text("إدارة قاعدة بيانات Cloud Firestore السحابية مباشرة ومزامنة الجمعيات", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = GoldenAmber)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("بحث باسم الجمعية، المدينة، الكود...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showAddTenantDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة جمعية")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTenants) { tenant ->
                                val isCurrentActive = tenant.id == currentTenant.id
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentActive) SocietyBlueLight else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrentActive) SocietyBluePrimary else BorderSubtle
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tenant.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "المدينة: ${tenant.city} • الكود: ${tenant.code} • الترخيص: ${tenant.licenseNumber}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted
                                                )
                                            }
                                            if (isCurrentActive) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = SocietyBluePrimary
                                                ) {
                                                    Text(
                                                        text = "النشطة",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(shape = RoundedCornerShape(8.dp), color = SlateBackground, border = BorderStroke(0.5.dp, BorderSubtle)) {
                                                Text("${tenant.ringsCount} حلقة", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = TextDark)
                                            }
                                            Surface(shape = RoundedCornerShape(8.dp), color = SlateBackground, border = BorderStroke(0.5.dp, BorderSubtle)) {
                                                Text("${tenant.studentsCount} طالب", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = TextDark)
                                            }
                                            Surface(shape = RoundedCornerShape(8.dp), color = SlateBackground, border = BorderStroke(0.5.dp, BorderSubtle)) {
                                                Text("${tenant.teachersCount} معلم", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = TextDark)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    onTenantSelected(tenant)
                                                    onNavigateToSupervisorDashboard()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("دخول كمالك للجمعية", fontSize = 12.sp)
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val updated = tenant.copy(attendanceRate = (85..99).random())
                                                        SocietyRepository.updateTenant(updated)
                                                        coroutineScope.launch {
                                                            try {
                                                                firestoreRepo.saveTenant(updated)
                                                            } catch (e: Exception) {
                                                                snackbarMessage = "خطأ في المزامنة السحابية: ${e.message}"
                                                            }
                                                        }
                                                        snackbarMessage = "تم تحديث بيانات جمعية ${tenant.shortName}"
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("تعديل", fontSize = 11.sp)
                                                }

                                                IconButton(
                                                    onClick = {
                                                        SocietyRepository.removeTenant(tenant.id)
                                                        coroutineScope.launch {
                                                            try {
                                                                firestoreRepo.deleteTenant(tenant.id)
                                                            } catch (e: Exception) {
                                                                snackbarMessage = "خطأ في الحذف السحابي: ${e.message}"
                                                            }
                                                        }
                                                        snackbarMessage = "تم حذف الجمعية بنجاح"
                                                    }
                                                ) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = StatusError)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ==========================================
                    // تبويب 1: إدارة المعلمين ومنح الصلاحيات حصراً للمالك
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = GoldenAmber.copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LockPerson, contentDescription = null, tint = GoldenAmber)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ميزة حصرية للمالك: منح وترقية وتعديل صلاحيات المشرفين والمعلمين وإلغاء صلاحية أي مستخدم فوراً.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Users and Roles List
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المستخدمون ومنح الصلاحيات (${allUsers.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        val newUser = UserProfile(
                                            id = "usr_${System.currentTimeMillis()}",
                                            name = "مستخدم جديد",
                                            title = "مشرف معتمد",
                                            role = UserRole.SUPERVISOR,
                                            phone = "0500000000",
                                            email = "new_user@society.ye"
                                        )
                                        SocietyRepository.addUser(newUser)
                                        snackbarMessage = "تم إضافة مستخدم جديد بنجاح"
                                    }
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة مستخدم")
                                }
                            }
                        }

                        items(allUsers) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (user.role == UserRole.OWNER) GoldenAmber.copy(alpha = 0.2f)
                                                        else SocietyBlueLight
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (user.role == UserRole.OWNER) Icons.Default.Shield else Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = if (user.role == UserRole.OWNER) GoldenAmber else SocietyBluePrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${user.email} • ${user.phone}", fontSize = 11.sp, color = TextMuted)
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (user.role) {
                                                UserRole.OWNER -> GoldenAmber.copy(alpha = 0.2f)
                                                UserRole.SUPERVISOR -> SocietyBluePrimary.copy(alpha = 0.15f)
                                                UserRole.TEACHER -> StatusSuccess.copy(alpha = 0.15f)
                                                UserRole.GUARDIAN -> TextMuted.copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = user.role.label,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (user.role) {
                                                    UserRole.OWNER -> GoldenAmber
                                                    UserRole.SUPERVISOR -> SocietyBluePrimary
                                                    UserRole.TEACHER -> StatusSuccess
                                                    UserRole.GUARDIAN -> TextDark
                                                }
                                            )
                                        }
                                    }

                                    // Role assignment buttons for Owner
                                    if (user.role != UserRole.OWNER) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("تغيير الصلاحية:", fontSize = 11.sp, color = TextMuted)
                                            listOf(UserRole.SUPERVISOR, UserRole.TEACHER, UserRole.GUARDIAN).forEach { targetRole ->
                                                val isCurrent = user.role == targetRole
                                                OutlinedButton(
                                                    onClick = {
                                                        SocietyRepository.updateUserRole(user.id, targetRole)
                                                        snackbarMessage = "تم تغيير دور ${user.name} إلى ${targetRole.label}"
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (isCurrent) SocietyBlueLight else Color.Transparent
                                                    )
                                                ) {
                                                    Text(targetRole.label.take(10), fontSize = 10.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = {
                                                    SocietyRepository.removeUser(user.id)
                                                    snackbarMessage = "تم حذف المستخدم من المنظومة"
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = StatusError, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Teachers list
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قائمة معلمي المنظومة (${allTeachers.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { showAddTeacherDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة معلم", fontSize = 12.sp)
                                }
                            }
                        }

                        items(allTeachers) { teacher ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(teacher.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("المؤهل: ${teacher.qualification}", fontSize = 11.sp, color = TextMuted)
                                        Text("الهاتف: ${teacher.phone} • الحلقات: ${teacher.assignedRings.joinToString()}", fontSize = 10.sp, color = SocietyBluePrimary)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val nextStatus = if (teacher.status == TeacherStatus.ACTIVE) TeacherStatus.SUSPENDED else TeacherStatus.ACTIVE
                                                SocietyRepository.updateTeacher(teacher.copy(status = nextStatus))
                                                snackbarMessage = "تم تغيير حالة المعلم ${teacher.name} إلى ${nextStatus.label}"
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(if (teacher.status == TeacherStatus.ACTIVE) "تجميد" else "تفعيل", fontSize = 11.sp)
                                        }

                                        IconButton(
                                            onClick = {
                                                SocietyRepository.removeTeacher(teacher.id)
                                                snackbarMessage = "تم حذف المعلم بنجاح"
                                            }
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = StatusError)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ==========================================
                    // تبويب 2: إدارة الحلقات والطلاب بالكامل
                    // ==========================================
                    var subSection by remember { mutableStateOf(0) } // 0: الحلقات, 1: الطلاب
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = subSection == 0,
                                onClick = { subSection = 0 },
                                label = { Text("الحلقات القرآنية (${allRings.size})") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = subSection == 1,
                                onClick = { subSection = 1 },
                                label = { Text("الطلاب المسجلين (${allStudents.size})") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (subSection == 0) {
                            // Rings Management
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("كافة الحلقات النشطة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Button(
                                    onClick = { showAddRingDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حلقة جديدة")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(allRings) { ring ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(ring.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("المعلم: ${ring.teacherName} • الموعد: ${ring.timeSlot}", fontSize = 11.sp, color = TextMuted)
                                                Text("الطلاب: ${ring.studentCount}/${ring.maxCapacity} • المحتوى: ${ring.currentJuz}", fontSize = 10.sp, color = SocietyBluePrimary)
                                            }

                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        val updated = ring.copy(studentCount = ring.studentCount + 1)
                                                        SocietyRepository.updateRing(updated)
                                                        snackbarMessage = "تم تحديث بيانات حلقة ${ring.name}"
                                                    }
                                                ) {
                                                    Icon(Icons.Outlined.Edit, contentDescription = "تعديل", tint = SocietyBluePrimary)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        SocietyRepository.removeRing(ring.id)
                                                        snackbarMessage = "تم حذف الحلقة بنجاح"
                                                    }
                                                ) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = StatusError)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Students Management
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الطلاب في المنظومة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Button(
                                    onClick = { showAddStudentDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسجيل طالب")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(allStudents.take(30)) { student ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(student.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("ولي الأمر: ${student.guardianName} • هاتف: ${student.guardianPhone}", fontSize = 11.sp, color = TextMuted)
                                                Text("التقييم: ${student.lastEvaluation} • الحالة: ${student.attendanceStatus.label}", fontSize = 10.sp, color = StatusSuccess)
                                            }

                                            IconButton(
                                                onClick = {
                                                    SocietyRepository.removeStudent(student.id)
                                                    snackbarMessage = "تم حذف الطالب بنجاح"
                                                }
                                            ) {
                                                Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = StatusError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // ==========================================
                    // تبويب 3: تهيئة بيانات التطبيق بالكامل وتصفير وإدارة النظام
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentPadding = PaddingValues(bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // النسخ الاحتياطي السحابي واستعادة الجمعية عبر Firebase Storage
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, GoldenAmber)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(26.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "النسخ الاحتياطي السحابي (Firebase Storage)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "تصدير بيانات ${currentTenant.shortName} إلى JSON واستيرادها لحل مشاكل المزامنة",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = GoldenAmber.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "JSON سحابي",
                                                color = GoldenAmber,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "يتيح هذا الخيار للمشرف والمالك تصدير كافة الطلاب والمعلمين وسجلات الحضور في ملف JSON موحد وتخزينه في Firebase Storage، مع إمكانية استيرادها واستعادتها بضغطة واحدة في حال انقطاع الشبكة أو تعارض المزامنة.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0),
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onNavigateToBackup,
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("فتح مركز النسخ والاستعادة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val res = FirebaseBackupManager.exportTenantToFirebaseStorage(
                                                        tenant = currentTenant,
                                                        context = context,
                                                        performedBy = "مالك النظام (لوحة التحكم)"
                                                    )
                                                    res.onSuccess {
                                                        snackbarMessage = "تم تصدير نسخة ${it.fileName} بنجاح إلى Firebase Storage!"
                                                    }.onFailure {
                                                        snackbarMessage = "فشل التصدير: ${it.localizedMessage}"
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                        ) {
                                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تصدير سريع", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // تهيئة بيانات التطبيق كاملة (Factory Reset / Default Seed)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SocietyBlueLight),
                                border = BorderStroke(1.dp, SocietyBluePrimary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = SocietyBluePrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "تهيئة بيانات التطبيق بالكامل (Factory Reset & Seed)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SocietyBluePrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "يقوم هذا الخيار بإعادة تهيئة كافة الجمعيات القرآنية اليمنية، الحلقات، المعلمين، الطلاب، والمناهج لحالتها الأساسية النموذجية.",
                                        fontSize = 12.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { showResetConfirmDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تهيئة واستعادة بيانات التطبيق كاملة")
                                    }
                                }
                            }
                        }

                        // تصفير وتفريغ بيانات التطبيق (Purge)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusError)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "تفريغ وتصفير بيانات التطبيق (Purge All Data)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = StatusError
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "حذف كافة الجمعيات والطلاب والمعلمين من قاعدة البيانات مع الحفاظ حصرياً على حساب المالك الرئيسي (${OwnerSecurityManager.maskEmail()}).",
                                        fontSize = 12.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { showPurgeConfirmDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تفريغ وتصفير كافة البيانات الآن")
                                    }
                                }
                            }
                        }

                        // الأمان المشدد والمصادقة الحيوية لمسؤولي النظام
                        item {
                            val biometricStatus = remember { BiometricHelper.checkBiometricStatus(context) }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "المصادقة الحيوية الإدارية (Biometric Security)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = GoldenAmber
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (biometricStatus.isUsable) StatusSuccess.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (biometricStatus.isUsable) "نشط ومفعل" else "يتطلب تسجيل",
                                                color = if (biometricStatus.isUsable) StatusSuccess else Color(0xFFF59E0B),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = biometricStatus.message,
                                        fontSize = 12.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "تُستخدم تقنية androidx.biometric لتأمين كشف بيانات المالك، التصفير الشامل، والوصول للعمليات الحساسة عبر بصمة الإصبع أو الوجه أو قفل الشاشة المشفر.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            BiometricHelper.authenticateForAdministrativeAccess(
                                                context = context,
                                                operationName = "اختبار مستشعر البصمة والمصادقة الحيوية",
                                                onSuccess = {
                                                    snackbarMessage = "نجحت المصادقة الحيوية! المستشعر والتحقق البيومتري يعملان بكفاءة تامة."
                                                },
                                                onError = { err ->
                                                    snackbarMessage = "نتيجة الفحص البيومتري: $err"
                                                }
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, GoldenAmber)
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("اختبار المصادقة الحيوية الآن", color = GoldenAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // المزامنة السحابية الفورية لكافة البيانات
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = SocietyBluePrimary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("مزامنة السحابة الشاملة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (syncStatus == SyncStatus.SYNCED) StatusSuccess.copy(alpha = 0.15f) else GoldenAmber.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (syncStatus == SyncStatus.SYNCED) "متزامن" else "جارِ المزامنة",
                                                color = if (syncStatus == SyncStatus.SYNCED) StatusSuccess else GoldenAmber,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "آخر حالة مزامنة: $lastSyncedTime", fontSize = 12.sp, color = TextMuted)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                SyncManager.triggerSync()
                                                snackbarMessage = "تمت مزامنة كافة بيانات التطبيق مع السحابة بنجاح!"
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                                    ) {
                                        Icon(Icons.Default.Sync, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("مزامنة كافة البيانات الآن")
                                    }
                                }
                            }
                        }

                        // وضع الصيانة الشامل
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("وضع الصيانة الشامل (Maintenance Mode)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("تقييد دخول كافة المشرفين والمعلمين وحصر التطبيق بالمالك فقط.", fontSize = 11.sp, color = TextMuted)
                                    }
                                    Switch(
                                        checked = SocietyRepository.systemMaintenanceMode,
                                        onCheckedChange = {
                                            SocietyRepository.systemMaintenanceMode = it
                                            snackbarMessage = if (it) "تم تفعيل وضع الصيانة العام للنظام" else "تم إيقاف وضع الصيانة"
                                        }
                                    )
                                }
                            }
                        }

                        // تسجيل المشرفين الجدد
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("قبول طلبات تسجيل المشرفين الجدد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("السماح أو إيقاف استقبال تسجيل الجمعيات والمشرفين الجدد.", fontSize = 11.sp, color = TextMuted)
                                    }
                                    Switch(
                                        checked = SocietyRepository.globalRegistrationActive,
                                        onCheckedChange = {
                                            SocietyRepository.globalRegistrationActive = it
                                            snackbarMessage = if (it) "تم فتح تسجيل المشرفين الجدد" else "تم إيقاف التسجيل العام"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // ==========================================
                    // تبويب 4: الوصول المباشر لكافة بوابات التطبيق (God Mode)
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "الوصول المباشر لكافة بوابات وأقسام التطبيق بصلاحية المالك الكاملة:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }

                        val godModeRoutes = listOf(
                            Triple("لوحة المشرف العام الرئيسية", "استعراض الإحصائيات وإدارة سياق الجمعية", onNavigateToSupervisorDashboard),
                            Triple("إدارة المعلمين والحلقات", "استعراض المعلمين وسجلات الحضور", onNavigateToTeachers),
                            Triple("مخطط الحفظ والمراجعة الذكي", "جدول الحفظ اليومي ومتابعة الخطط", onNavigateToPlanner),
                            Triple("التحديات والمسابقات القرآنية", "المسابقات اليومية والمسابقات التنافسية لجميع الأعمار", onNavigateToChallenges),
                            Triple("تسجيل الحضور السريع والـ QR", "حضور الطلاب والمعلمين عبر الباركود", onNavigateToAttendance),
                            Triple("بوابة المعلم وتقييم التلاوة", "تقييم التلاوات وتسجيل الحفظ بالذكاء الاصطناعي", onNavigateToTeacherPortal),
                            Triple("بوابة أولياء الأمور", "متابعة الأبناء وإشعارات الحفظ اليومية", onNavigateToParentPortal),
                            Triple("التقارير وتوليد الشهادات", "إصدار وتصدير شهادات التخرج والتقارير", onNavigateToReports)
                        )

                        items(godModeRoutes) { (title, subtitle, onClick) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onClick() },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(subtitle, fontSize = 11.sp, color = TextMuted)
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = SocietyBluePrimary
                                    )
                                }
                            }
                        }
                    }
                }

                5 -> {
                    // ==========================================
                    // تبويب 5: درع الأمان ورصد الاختراق والإنذار الفوري
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. بطاقة درع الأمان المركزي والحماية المشفرة
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(GoldenAmber.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = null,
                                                    tint = GoldenAmber,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "منظومة الحماية ورصد الاختراق",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "تشفير وحجب بيانات المالك + إنذار فوري",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (OwnerSecurityManager.isLockdownActive) StatusError.copy(alpha = 0.2f) else Color(0xFF059669).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (OwnerSecurityManager.isLockdownActive) "تجميد أمني" else "الدرع نشط",
                                                color = if (OwnerSecurityManager.isLockdownActive) StatusError else Color(0xFF34D399),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // بيانات المالك المشفرة
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("البريد الإلكتروني المشفر للمالك:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            Text(
                                                text = if (isSensitiveInfoRevealed) OwnerSecurityManager.OWNER_EMAIL else OwnerSecurityManager.maskEmail(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        IconButton(onClick = {
                                            if (!isSensitiveInfoRevealed) {
                                                BiometricHelper.authenticateForAdministrativeAccess(
                                                    context = context,
                                                    operationName = "كشف البريد السري ورقم هاتف المالك",
                                                    onSuccess = {
                                                        isSensitiveInfoRevealed = true
                                                        snackbarMessage = "تم التحقق من بصمة المالك وإظهار البيانات الحساسة."
                                                    },
                                                    onError = { err ->
                                                        snackbarMessage = "مرفوض: فشل التحقق الحيوي لكشف البيانات ($err)"
                                                    }
                                                )
                                            } else {
                                                isSensitiveInfoRevealed = false
                                            }
                                        }) {
                                            Icon(
                                                imageVector = if (isSensitiveInfoRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "إظهار/إخفاء البيانات الحساسة",
                                                tint = GoldenAmber
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("رقم هاتف / واتساب المالك للإنذارات:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            Text(
                                                text = if (isSensitiveInfoRevealed) OwnerSecurityManager.OWNER_PHONE else OwnerSecurityManager.maskPhone(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Text(
                                            text = "رمز PIN: ••••",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }

                                    if (OwnerSecurityManager.isLockdownActive) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                OwnerSecurityManager.resetSecurityLockdown()
                                                snackbarMessage = "تم فك التجميد الأمني وتصفير عداد المحاولات الفاشلة بنجاح."
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.LockOpen, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("فك التجميد الأمني وإعادة تصفير العداد", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. بطاقة إرسال بلاغات أمنية تجريبية للواتساب والإيميل
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "اختبار قنوات الإنذار الفوري:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "اضغط للتأكد من وصول بلاغات الأمان فوراً إلى هاتفك أو بريدك الإلكتروني:",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                OwnerSecurityManager.sendTestSecurityDispatch(context, viaWhatsApp = true)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تجربة الواتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Button(
                                            onClick = {
                                                OwnerSecurityManager.sendTestSecurityDispatch(context, viaWhatsApp = false)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تجربة البريد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // 3. سجل أحداث ومحاولات الاختراق الحية (Live Intrusion Log)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سجل محاولات الاختراق والتسلل (${OwnerSecurityManager.securityIncidents.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (OwnerSecurityManager.securityIncidents.isNotEmpty()) {
                                    TextButton(onClick = {
                                        OwnerSecurityManager.clearIncidents()
                                        snackbarMessage = "تم مسح سجل الإنذارات بنجاح."
                                    }) {
                                        Text("مسح السجل", color = StatusError, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (OwnerSecurityManager.securityIncidents.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("لا توجد محاولات تسلل مسجلة حالياً", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("منظومة الحماية نشطة وتراقب كافة العمليات على مدار الساعة.", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                            }
                        } else {
                            items(OwnerSecurityManager.securityIncidents) { incident ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, if (incident.severity.contains("Critical") || incident.severity.contains("Lockdown")) StatusError.copy(alpha = 0.5f) else BorderSubtle)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = StatusError,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(incident.attemptType, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = StatusError.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = incident.severity,
                                                    fontSize = 9.sp,
                                                    color = StatusError,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("📅 ${incident.timestamp} • 🌐 ${incident.clientIp}", fontSize = 11.sp, color = TextMuted)
                                        Text("📱 الجهاز: ${incident.deviceModel}", fontSize = 11.sp, color = TextDark)
                                        Text("🔒 التفاصيل: ${incident.attemptedValueMasked}", fontSize = 11.sp, color = TextMuted)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val intent = OwnerSecurityManager.createWhatsAppAlertIntent(incident)
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        snackbarMessage = "واتساب غير متوفر"
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("إرسال لواتساب", fontSize = 10.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val intent = OwnerSecurityManager.createEmailAlertIntent(incident)
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        snackbarMessage = "تطبيق البريد غير متوفر"
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("إرسال للبريد", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. مركز توليد وإرسال رموز التحقق السرية (OTP Dispatcher)
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.VpnKey,
                                                contentDescription = null,
                                                tint = GoldenAmber,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "مركز إصدار رموز التحقق (OTP) للمستخدمين",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = GoldenAmber.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "توليد تلقائي مشفر (6 أرقام)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "توليد وإرسال رمز أمان مشفر فوري لربطه بالواتساب وإشعار المالك والزائرين مباشرة:",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val session = OtpSecuritySystem.issueOtp(
                                                target = OwnerSecurityManager.OWNER_PHONE_INTL,
                                                recipientName = "مالك النظام (Super Admin)",
                                                purpose = "فحص واعتماد تصريح أمني فوري",
                                                context = context
                                            )
                                            OtpSecuritySystem.sendViaWhatsApp(context, session)
                                            snackbarMessage = "تم إصدار الرمز (${session.code}) وتوجيهه لواتساب المالك."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "توليد رمز أمان OTP وإرساله للرقم المعتمد",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // 5. سجل تسجيلات الدخول والأنشطة لجميع المستخدمين والزائرين
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سجل دخول المستخدمين والزائرين (${OwnerSecurityManager.userAccessLogs.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (OwnerSecurityManager.userAccessLogs.isNotEmpty()) {
                                    TextButton(onClick = {
                                        OwnerSecurityManager.clearUserAccessLogs()
                                        snackbarMessage = "تم مسح سجل دخول المستخدمين."
                                    }) {
                                        Text("مسح السجل", color = StatusError, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        items(OwnerSecurityManager.userAccessLogs) { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = log.userNameOrRole,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = TextDark
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = SocietyBlueLight
                                            ) {
                                                Text(
                                                    text = log.role.label,
                                                    fontSize = 9.sp,
                                                    color = SocietyBlueDark,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${log.accessType} • ${log.tenantName}",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "📱 ${log.deviceModel} • 📅 ${log.timestamp}",
                                            fontSize = 9.sp,
                                            color = TextSubtle
                                        )
                                    }

                                    if (log.generatedOtp != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF3C7),
                                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                        ) {
                                            Text(
                                                text = "OTP: ${log.generatedOtp}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                6 -> {
                    // ==========================================
                    // تبويب 6: لوحة التسجيلات النشطة مقابل المعلقة لجميع الجمعيات
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ActiveVsPendingRegistrationsDashboard(
                            tenants = allTenants,
                            isDarkMode = true,
                            onTenantClick = { tenant ->
                                onTenantSelected(tenant)
                            }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                7 -> {
                    // ==========================================
                    // تبويب 7: بوابة تسجيل وتوثيق المستخدمين الجدد
                    // ==========================================
                    var selectedRegRole by remember { mutableStateOf(UserRole.GUARDIAN) }
                    var regName by remember { mutableStateOf("") }
                    var regId by remember { mutableStateOf("") }
                    var regPhone by remember { mutableStateOf("") }
                    var regSupervisorDept by remember { mutableStateOf("") }
                    var regSuccess by remember { mutableStateOf(false) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, GoldenAmber)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            tint = GoldenAmber,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "بوابة تسجيل وتوثيق المستخدمين الجدد",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "تسجيل وإضافة مشرفين، معلمين، وأولياء أمور جدد وتفعيل الصلاحيات",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    if (!regSuccess) {
                                        Text(
                                            text = "اختر نوع وحساب المستخدم الجديد في ${currentTenant.name}:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = selectedRegRole == UserRole.GUARDIAN,
                                                onClick = { selectedRegRole = UserRole.GUARDIAN },
                                                label = { Text("ولي أمر", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                leadingIcon = { Icon(Icons.Default.FamilyRestroom, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = selectedRegRole == UserRole.TEACHER,
                                                onClick = { selectedRegRole = UserRole.TEACHER },
                                                label = { Text("معلم", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = selectedRegRole == UserRole.SUPERVISOR,
                                                onClick = { selectedRegRole = UserRole.SUPERVISOR },
                                                label = { Text("مشرف", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = regName,
                                            onValueChange = { regName = it },
                                            label = { Text("الاسم الثلاثي") },
                                            placeholder = { Text("أدخل الاسم الكامل") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = regId,
                                            onValueChange = { regId = it },
                                            label = { Text("رقم الهوية الوطنية / الإقامة") },
                                            placeholder = { Text("أدخل رقم الهوية") },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = regPhone,
                                            onValueChange = { regPhone = it },
                                            label = { Text("رقم الجوال (05xxxxxxxx)") },
                                            placeholder = { Text("أدخل رقم الجوال") },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        if (selectedRegRole == UserRole.SUPERVISOR) {
                                            OutlinedTextField(
                                                value = regSupervisorDept,
                                                onValueChange = { regSupervisorDept = it },
                                                label = { Text("القسم الإشرافي / الاعتماد") },
                                                placeholder = { Text("أدخل القسم الإشرافي") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                if (regName.isBlank() || regPhone.isBlank()) {
                                                    Toast.makeText(context, "يرجى إدخال الاسم ورقم الجوال على الأقل", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                val newUser = UserProfile(
                                                    id = "user_" + System.currentTimeMillis(),
                                                    name = regName,
                                                    title = regSupervisorDept.ifBlank { selectedRegRole.label },
                                                    role = selectedRegRole,
                                                    phone = regPhone,
                                                    email = "$regPhone@society.org"
                                                )
                                                SocietyRepository.allUsers = listOf(newUser) + SocietyRepository.allUsers
                                                OwnerSecurityManager.logUserAccess(
                                                    userNameOrRole = regName,
                                                    role = selectedRegRole,
                                                    tenantName = currentTenant.name,
                                                    context = context,
                                                    accessType = "تسجيل مستخدم جديد عبر بوابة المالك المركزية"
                                                )
                                                regSuccess = true
                                                Toast.makeText(context, "تم تسجيل وتوثيق الحساب بنجاح!", Toast.LENGTH_LONG).show()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("إتمام وتوثيق تسجيل المستخدم الجديد", fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = StatusSuccess,
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "تم تسجيل وتوثيق الحساب بنجاح في النظام!",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "المستخدم جاهز الآن لتسجيل الدخول بالبصمة أو رقم الجوال.",
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    regName = ""
                                                    regId = ""
                                                    regPhone = ""
                                                    regSupervisorDept = ""
                                                    regSuccess = false
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, GoldenAmber)
                                            ) {
                                                Text("تسجيل مستخدم آخر", color = GoldenAmber, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                8 -> {
                    // ==========================================
                    // تبويب 8: بوابة المستخدمين المسجلين Zero-Trust
                    // ==========================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("إدارة بوابة Zero-Trust للمستخدمين", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                        Text("عرض وحذف وتعديل صلاحيات جميع المستخدمين الموثقين في النظام", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }

                        items(allUsers) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, if (user.role == UserRole.OWNER) GoldenAmber else BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (user.role == UserRole.OWNER) GoldenAmber.copy(alpha = 0.2f) else SocietyBlueLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (user.role == UserRole.OWNER) Icons.Default.Shield else Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = if (user.role == UserRole.OWNER) GoldenAmber else SocietyBluePrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(user.email, fontSize = 11.sp, color = TextMuted)
                                                Text(user.phone, fontSize = 11.sp, color = TextMuted)
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (user.role) {
                                                UserRole.OWNER -> GoldenAmber.copy(alpha = 0.2f)
                                                UserRole.SUPERVISOR -> SocietyBluePrimary.copy(alpha = 0.15f)
                                                UserRole.TEACHER -> StatusSuccess.copy(alpha = 0.15f)
                                                UserRole.GUARDIAN -> TextMuted.copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = user.role.label,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (user.role) {
                                                    UserRole.OWNER -> GoldenAmber
                                                    UserRole.SUPERVISOR -> SocietyBluePrimary
                                                    UserRole.TEACHER -> StatusSuccess
                                                    UserRole.GUARDIAN -> TextDark
                                                }
                                            )
                                        }
                                    }

                                    if (user.role != UserRole.OWNER) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    SocietyRepository.removeUser(user.id)
                                                    snackbarMessage = "تم حذف وتصفير صلاحيات المستخدم ${user.name} بنجاح"
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = StatusError)
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("سحب الصلاحيات والحذف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

        // ==========================================
        // حوارات التأكيد والإضافة
        // ==========================================

        // حوار إضافة جمعية جديدة
        if (showAddTenantDialog) {
            var name by remember { mutableStateOf("") }
            var city by remember { mutableStateOf("صنعاء") }
            var rings by remember { mutableStateOf("20") }
            AlertDialog(
                onDismissRequest = { showAddTenantDialog = false },
                icon = { Icon(Icons.Default.AddBusiness, contentDescription = null, tint = SocietyBluePrimary) },
                title = { Text("إضافة جمعية قرآنية جديدة", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("اسم الجمعية / المؤسسة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("المحافظة / المدينة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = rings,
                            onValueChange = { rings = it },
                            label = { Text("عدد الحلقات التقديري") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val newTenant = SocietyRepository.approveNewSociety(
                                    name = name,
                                    governorate = city,
                                    supervisorName = "المشرف المعتمد",
                                    phone = "775484201",
                                    email = "info@society.ye",
                                    notes = rings
                                )
                                coroutineScope.launch {
                                    try {
                                        firestoreRepo.saveTenant(newTenant)
                                    } catch (e: Exception) {
                                        snackbarMessage = "خطأ في المزامنة السحابية: ${e.message}"
                                    }
                                }
                                showAddTenantDialog = false
                                snackbarMessage = "تم إضافة وتفعيل جمعية $name بنجاح!"
                            }
                        }
                    ) {
                        Text("اعتماد وإضافة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTenantDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // حوار إضافة معلم جديد
        if (showAddTeacherDialog) {
            var tchName by remember { mutableStateOf("") }
            var tchPhone by remember { mutableStateOf("0500000000") }
            var tchQual by remember { mutableStateOf("إجازة في القرآن الكريم") }
            var tchRing by remember { mutableStateOf("حلقة الإمام عاصم") }
            AlertDialog(
                onDismissRequest = { showAddTeacherDialog = false },
                icon = { Icon(Icons.Default.School, contentDescription = null, tint = SocietyBluePrimary) },
                title = { Text("إضافة معلم جديد للحلقات", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tchName,
                            onValueChange = { tchName = it },
                            label = { Text("اسم المعلم كاملاً") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tchPhone,
                            onValueChange = { tchPhone = it },
                            label = { Text("رقم الهاتف") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tchQual,
                            onValueChange = { tchQual = it },
                            label = { Text("المؤهل والإجازات") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tchRing,
                            onValueChange = { tchRing = it },
                            label = { Text("الحلقة المسندة إليه") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tchName.isNotBlank()) {
                                val newTeacher = Teacher(
                                    id = "tch_${System.currentTimeMillis()}",
                                    name = tchName,
                                    phone = tchPhone,
                                    email = "${tchName.take(5).trim()}@society.ye",
                                    qualification = tchQual,
                                    experienceYears = 3,
                                    isAvailable = true,
                                    status = TeacherStatus.ACTIVE,
                                    assignedRings = listOf(tchRing),
                                    nationalId = "1000000000",
                                    hireDate = "اليوم",
                                    certificates = listOf("شهادة معتمدة"),
                                    leaveRequests = emptyList(),
                                    auditLogs = listOf(AuditEntry("a_${System.currentTimeMillis()}", "تم تعيين المعلم بواسطة المالك", "الآن", "مالك التطبيق"))
                                )
                                SocietyRepository.addTeacher(newTeacher)
                                showAddTeacherDialog = false
                                snackbarMessage = "تم إضافة المعلم $tchName بنجاح!"
                            }
                        }
                    ) {
                        Text("إضافة المعلم")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTeacherDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // حوار إضافة حلقة جديدة
        if (showAddRingDialog) {
            var ringName by remember { mutableStateOf("") }
            var teacherName by remember { mutableStateOf("الشيخ عبد الله بن محمد المنصور") }
            var timeSlot by remember { mutableStateOf("عصراً") }
            var targetJuz by remember { mutableStateOf("جزء عمّ") }
            AlertDialog(
                onDismissRequest = { showAddRingDialog = false },
                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = SocietyBluePrimary) },
                title = { Text("إضافة حلقة قرآنية جديدة", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ringName,
                            onValueChange = { ringName = it },
                            label = { Text("اسم الحلقة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = teacherName,
                            onValueChange = { teacherName = it },
                            label = { Text("معلم الحلقة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = timeSlot,
                            onValueChange = { timeSlot = it },
                            label = { Text("وقت انعقاد الحلقة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = targetJuz,
                            onValueChange = { targetJuz = it },
                            label = { Text("المستوى والمرحلة") },
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
                                    teacherName = teacherName,
                                    timeSlot = timeSlot,
                                    studentCount = 15,
                                    maxCapacity = 25,
                                    currentJuz = targetJuz,
                                    attendancePercent = 95,
                                    level = "مبتدئ إلى متوسط"
                                )
                                SocietyRepository.addRing(newRing)
                                showAddRingDialog = false
                                snackbarMessage = "تم إضافة حلقة $ringName بنجاح!"
                            }
                        }
                    ) {
                        Text("إضافة الحلقة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddRingDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // حوار إضافة طالب جديد
        if (showAddStudentDialog) {
            var stName by remember { mutableStateOf("") }
            var stGuardian by remember { mutableStateOf("") }
            var stPhone by remember { mutableStateOf("0500000000") }
            AlertDialog(
                onDismissRequest = { showAddStudentDialog = false },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SocietyBluePrimary) },
                title = { Text("تسجيل طالب جديد", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stName,
                            onValueChange = { stName = it },
                            label = { Text("اسم الطالب الرباعي") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = stGuardian,
                            onValueChange = { stGuardian = it },
                            label = { Text("اسم ولي الأمر") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = stPhone,
                            onValueChange = { stPhone = it },
                            label = { Text("هاتف ولي الأمر") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (stName.isNotBlank()) {
                                val newSt = CircleStudent(
                                    id = "std_${System.currentTimeMillis()}",
                                    name = stName,
                                    guardianName = if (stGuardian.isNotBlank()) stGuardian else "ولي أمر الطالب",
                                    guardianPhone = stPhone,
                                    attendanceStatus = AttendanceStatus.PRESENT,
                                    lastEvaluation = "ممتاز 95%",
                                    registrationOrder = allStudents.size + 1,
                                    note = "مسجل بواسطة المالك"
                                )
                                SocietyRepository.addStudent(newSt)
                                showAddStudentDialog = false
                                snackbarMessage = "تم تسجيل الطالب $stName بنجاح!"
                            }
                        }
                    ) {
                        Text("تسجيل الطالب")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddStudentDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // حوار تأكيد تهيئة بيانات التطبيق كاملة
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                icon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = SocietyBluePrimary) },
                title = { Text("تأكيد تهيئة بيانات التطبيق", fontWeight = FontWeight.Bold) },
                text = {
                    Text("هل أنت متأكد من إعادة تهيئة كافة بيانات التطبيق واستعادة الجمعيات القرآنية والحلقات النموذجية الافتراضية؟")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            BiometricHelper.authenticateForAdministrativeAccess(
                                context = context,
                                operationName = "إعادة تهيئة واستعادة بيانات التطبيق",
                                onSuccess = {
                                    SocietyRepository.resetToFactoryDefaults()
                                    showResetConfirmDialog = false
                                    snackbarMessage = "تم التحقق بالبصمة وإعادة تهيئة بيانات التطبيق النموذجية بنجاح!"
                                },
                                onError = { err ->
                                    snackbarMessage = "فشلت المصادقة الحيوية: $err"
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("نعم، تهيئة البيانات")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // حوار تأكيد تصفير وتفريغ البيانات
        if (showPurgeConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showPurgeConfirmDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError) },
                title = { Text("تحذير أمني: تصفير وتفريغ البيانات", fontWeight = FontWeight.Bold, color = StatusError) },
                text = {
                    Text("سيتم حذف وتصفير جميع الجمعيات والطلاب والمعلمين والحلقات فوراً، مع الإبقاء حصراً على حساب المالك العام (${OwnerSecurityManager.maskEmail()}). يتطلب هذا الإجراء بصمة حيوية مصرحة.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            BiometricHelper.authenticateForAdministrativeAccess(
                                context = context,
                                operationName = "تصفير وتفريغ كافة بيانات الجمعيات والطلاب",
                                onSuccess = {
                                    SocietyRepository.wipeAllDataPreservingOwner()
                                    showPurgeConfirmDialog = false
                                    snackbarMessage = "تم التحقق بالبصمة الحيوية وتصفير كافة البيانات بنجاح مع الحفاظ على حساب المالك."
                                },
                                onError = { err ->
                                    snackbarMessage = "فشلت المصادقة الحيوية للمسح الشامل: $err"
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Text("تأكيد المسح الشامل بالبصمة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPurgeConfirmDialog = false }) { Text("تراجع") }
                }
            )
        }

        // Snackbar
        if (snackbarMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("إغلاق", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF1E293B)
                ) {
                    Text(snackbarMessage!!, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OwnerMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
    }
}
