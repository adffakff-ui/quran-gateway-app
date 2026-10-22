package com.example.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirestoreTenantRepository
import com.example.data.SocietyRepository
import com.example.data.Tenant
import com.example.ui.notifications.LocalNotificationHelper
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * خيارات لوحة الألوان الأساسية الخاصة بالجمعية
 */
data class ColorThemeOption(
    val id: String,
    val name: String,
    val hex: String,
    val description: String
)

val SocietyColorPresets = listOf(
    ColorThemeOption("royal_blue", "الأزرق الملكي", "#1976D2", "الطابع المؤسسي المعتمد للجمعيات"),
    ColorThemeOption("emerald", "الأخضر الزمردي", "#059669", "طابع قرآني مستوحى من رياض الجنة"),
    ColorThemeOption("teal", "التركواز الإسلامي", "#0D9488", "طابع حديث وهادئ يجمع بين السكينة والإتقان"),
    ColorThemeOption("amber", "الذهب الكلاسيكي", "#D97706", "طابع شرف القرآن والتكريم القرآني"),
    ColorThemeOption("purple", "البنفسجي الوقور", "#7C3AED", "طابع راقٍ للإدارات والمراكز النموذجية"),
    ColorThemeOption("navy", "الكحلي الليلي", "#0F172A", "طابع رسمي دقيق وعالي التباين")
)

val SocietyLogoPresets = listOf("🕌", "📖", "🏛️", "🌟", "🕋", "✨")

/**
 * تحويل كود Hex إلى لون Jetpack Compose بأمان
 */
fun parseHexColor(hex: String, fallback: Color = SocietyBluePrimary): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#$clean"))
            8 -> Color(android.graphics.Color.parseColor("#$clean"))
            else -> fallback
        }
    } catch (e: Exception) {
        fallback
    }
}

/**
 * SettingsScreen
 *
 * شاشة إعدادات الجمعية المخصصة للمسؤولين والمشرفين:
 * 1. تعديل بيانات وهوية الجمعية (الاسم، الاسم المختصر، المدينة، رقم الترخيص، وشعار الجمعية).
 * 2. تخصيص الألوان الأساسية وثيم الجمعية مع معاينة حية تفاعلية.
 * 3. ضبط خيارات الإشعارات والتنبيهات الميدانية (حلقات، واتساب، تقارير عاجلة، أصوات).
 * 4. الربط المباشر بقاعدة بيانات Cloud Firestore لتخزين ومزامنة التفضيلات.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTenant: Tenant,
    onTenantUpdated: (Tenant) -> Unit = {},
    darkModePref: String = "system",
    onDarkModeChange: (String) -> Unit = {},
    onNavigateToConnectedDevices: () -> Unit = {},
    onNavigateToAboutSociety: () -> Unit = {},
    onNavigateToOwnerSettings: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestoreRepo = remember { FirestoreTenantRepository(context) }

    // الرجوع الخلفي
    BackHandler {
        onBack()
    }

    // -------------------------------------------------------------
    // 1. هوية الجمعية والمقر
    // -------------------------------------------------------------
    var editName by remember { mutableStateOf(currentTenant.name) }
    var editShortName by remember { mutableStateOf(currentTenant.shortName) }
    var editCity by remember { mutableStateOf(currentTenant.city) }
    var editGovernorate by remember { mutableStateOf(currentTenant.governorate) }
    var editLicense by remember { mutableStateOf(currentTenant.licenseNumber) }
    var selectedLogo by remember { mutableStateOf(currentTenant.logoIdentifier.ifBlank { "🕌" }) }

    // -------------------------------------------------------------
    // 2. تخصيص الألوان الأساسية
    // -------------------------------------------------------------
    var selectedColorHex by remember { mutableStateOf(currentTenant.primaryColorHex.ifBlank { "#1976D2" }) }
    val activeColor = remember(selectedColorHex) { parseHexColor(selectedColorHex) }

    // -------------------------------------------------------------
    // 3. ضبط إعدادات الإشعارات
    // -------------------------------------------------------------
    var systemWideNotificationsEnabled by remember { mutableStateOf(true) }
    var dailyAttendanceAlertsEnabled by remember { mutableStateOf(true) }
    var whatsappAlertsEnabled by remember { mutableStateOf(true) }
    var urgentSupervisorReportsEnabled by remember { mutableStateOf(true) }
    var examCertificatesNotificationsEnabled by remember { mutableStateOf(true) }
    var soundVibrationEnabled by remember { mutableStateOf(true) }

    // -------------------------------------------------------------
    // 4. حالة المزامنة مع Firestore
    // -------------------------------------------------------------
    var isSyncingToFirestore by remember { mutableStateOf(false) }
    var isPullingFromFirestore by remember { mutableStateOf(false) }
    var lastSyncedTimestamp by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showTestNotificationBanner by remember { mutableStateOf(false) }

    // استرجاع التفضيلات المخزنة مسبقاً في Firestore عند فتح الشاشة
    LaunchedEffect(currentTenant.id) {
        isPullingFromFirestore = true
        try {
            val prefs = firestoreRepo.getTenantPreferences(currentTenant.id)
            if (prefs != null) {
                (prefs["name"] as? String)?.takeIf { it.isNotBlank() }?.let { editName = it }
                (prefs["shortName"] as? String)?.takeIf { it.isNotBlank() }?.let { editShortName = it }
                (prefs["city"] as? String)?.takeIf { it.isNotBlank() }?.let { editCity = it }
                (prefs["governorate"] as? String)?.takeIf { it.isNotBlank() }?.let { editGovernorate = it }
                (prefs["licenseNumber"] as? String)?.takeIf { it.isNotBlank() }?.let { editLicense = it }
                (prefs["primaryColorHex"] as? String)?.takeIf { it.isNotBlank() }?.let { selectedColorHex = it }
                (prefs["logoIdentifier"] as? String)?.takeIf { it.isNotBlank() }?.let { selectedLogo = it }

                (prefs["notificationSettings"] as? Map<*, *>)?.let { notifs ->
                    (notifs["systemWide"] as? Boolean)?.let { systemWideNotificationsEnabled = it }
                    (notifs["dailyAttendance"] as? Boolean)?.let { dailyAttendanceAlertsEnabled = it }
                    (notifs["whatsappAlerts"] as? Boolean)?.let { whatsappAlertsEnabled = it }
                    (notifs["urgentSupervisor"] as? Boolean)?.let { urgentSupervisorReportsEnabled = it }
                    (notifs["examCertificates"] as? Boolean)?.let { examCertificatesNotificationsEnabled = it }
                    (notifs["soundVibration"] as? Boolean)?.let { soundVibrationEnabled = it }
                }
                (prefs["updatedAt"] as? String)?.let { lastSyncedTimestamp = it }
            }
        } catch (e: Exception) {
            // استخدام القيم المحلية في حال عدم توفر اتصال
        } finally {
            isPullingFromFirestore = false
        }
    }

    // دالة حفظ ومزامنة التفضيلات في Firestore
    fun savePreferencesToFirestore() {
        coroutineScope.launch {
            isSyncingToFirestore = true
            try {
                val updatedTenant = currentTenant.copy(
                    name = editName.ifBlank { currentTenant.name },
                    shortName = editShortName.ifBlank { currentTenant.shortName },
                    city = editCity.ifBlank { currentTenant.city },
                    governorate = editGovernorate.ifBlank { currentTenant.governorate },
                    licenseNumber = editLicense.ifBlank { currentTenant.licenseNumber },
                    primaryColorHex = selectedColorHex,
                    logoIdentifier = selectedLogo
                )

                val notifMap = mapOf(
                    "systemWide" to systemWideNotificationsEnabled,
                    "dailyAttendance" to dailyAttendanceAlertsEnabled,
                    "whatsappAlerts" to whatsappAlertsEnabled,
                    "urgentSupervisor" to urgentSupervisorReportsEnabled,
                    "examCertificates" to examCertificatesNotificationsEnabled,
                    "soundVibration" to soundVibrationEnabled
                )

                // تخزين في Firestore سحابياً
                firestoreRepo.saveTenantPreferences(updatedTenant, notifMap)

                // تحديث الذاكرة المحلية والواجهة
                SocietyRepository.updateTenant(updatedTenant)
                onTenantUpdated(updatedTenant)

                val nowTime = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
                lastSyncedTimestamp = nowTime
                snackbarMessage = "تم حفظ ومزامنة إعدادات الجمعية بنجاح في Cloud Firestore!"
                Toast.makeText(context, "تم حفظ الإعدادات سحابياً بنجاح ($nowTime)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                snackbarMessage = "تعذر المزامنة السحابية: ${e.message}"
                Toast.makeText(context, "فشل الحفظ في السحابة: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isSyncingToFirestore = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(activeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedLogo,
                                fontSize = 20.sp
                            )
                        }
                        Column {
                            Text(
                                text = "إعدادات الجمعية والتفضيلات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = editShortName.ifBlank { currentTenant.shortName },
                                fontSize = 11.sp,
                                color = activeColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // زر الترجمة
                    LanguageToggleChip()

                    // زر بوابة المالك (حماية - بشكل أنيق ومصغر)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onNavigateToOwnerSettings,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp).testTag("owner_security_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = "بوابة المالك",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // زر استرجاع ومزامنة من السحابة
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isPullingFromFirestore = true
                                val prefs = firestoreRepo.getTenantPreferences(currentTenant.id)
                                if (prefs != null) {
                                    (prefs["name"] as? String)?.let { editName = it }
                                    (prefs["shortName"] as? String)?.let { editShortName = it }
                                    (prefs["primaryColorHex"] as? String)?.let { selectedColorHex = it }
                                    snackbarMessage = "تم تحديث البيانات من Cloud Firestore."
                                    Toast.makeText(context, "تم جلب البيانات السحابية", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "قاعدة البيانات السحابية محدثة بالفعل", Toast.LENGTH_SHORT).show()
                                }
                                isPullingFromFirestore = false
                            }
                        },
                        enabled = !isPullingFromFirestore && !isSyncingToFirestore,
                        modifier = Modifier.testTag("settings_pull_firestore_btn")
                    ) {
                        if (isPullingFromFirestore) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "مزامنة سحابية",
                                tint = activeColor
                            )
                        }
                    }

                    // زر الحفظ السحابي السريع
                    IconButton(
                        onClick = { savePreferencesToFirestore() },
                        enabled = !isSyncingToFirestore,
                        modifier = Modifier.testTag("settings_save_firestore_btn")
                    ) {
                        if (isSyncingToFirestore) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = activeColor)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "حفظ التفضيلات في Firestore",
                                tint = activeColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =================================================================
            // بطاقة حالة المزامنة مع Firestore
            // =================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "تخزين التفضيلات متصل بـ Firestore",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF14532D)
                                )
                                Text(
                                    text = if (lastSyncedTimestamp != null)
                                        "آخر مزامنة: $lastSyncedTimestamp • الجمعية: ${currentTenant.code}"
                                    else
                                        "جاهز للمزامنة السحابية المباشرة • كود: ${currentTenant.code}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }

                        if (isSyncingToFirestore) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF16A34A))
                        }
                    }
                }
            }

            // إشعار نجاح أو تنبيه
            if (snackbarMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = activeColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, activeColor.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = snackbarMessage ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { snackbarMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // تجربة إشعار فوري
            if (showTestNotificationBanner) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = activeColor.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, activeColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(activeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🔔 تجربة الإشعار: إشعار حلقة القرآن الفوري",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = activeColor
                                )
                                Text(
                                    text = "تم بث التنبيه الفوري بنجاح. تفضيلات الإشعارات مرتبطة ومفعلة للجمعية.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { showTestNotificationBanner = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = activeColor)
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 1. هوية وبيانات الجمعية (Society Name & Branding)
            // =================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_society_identity_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(activeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "هوية وبيانات الجمعية",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تعديل اسم الجمعية ومقرها والرمز والشعار المعتمد",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // اختيار أيقونة / شعار الجمعية
                        Text(
                            text = "شعار / أيقونة الجمعية الرمزية:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SocietyLogoPresets.forEach { logo ->
                                val isSelected = selectedLogo == logo
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) activeColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) activeColor else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedLogo = logo },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = logo, fontSize = 22.sp)
                                }
                            }
                        }

                        // اسم الجمعية الكامل
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("اسم الجمعية الرسمي الكامل") },
                            placeholder = { Text("أدخل اسم الجمعية") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_edit_name"),
                            leadingIcon = {
                                Icon(Icons.Default.CorporateFare, contentDescription = null, tint = activeColor)
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // الاسم المختصر
                        OutlinedTextField(
                            value = editShortName,
                            onValueChange = { editShortName = it },
                            label = { Text("الاسم المختصر للجمعية") },
                            placeholder = { Text("أدخل الاسم المختصر") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_edit_shortname"),
                            leadingIcon = {
                                Icon(Icons.Default.Label, contentDescription = null, tint = activeColor)
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // المدينة والمحافظة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = editCity,
                                onValueChange = { editCity = it },
                                label = { Text("المدينة") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_edit_city"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editGovernorate,
                                onValueChange = { editGovernorate = it },
                                label = { Text("المحافظة") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_edit_governorate"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // رقم الترخيص
                        OutlinedTextField(
                            value = editLicense,
                            onValueChange = { editLicense = it },
                            label = { Text("رقم الترخيص / الاعتماد الرسمي") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_edit_license"),
                            leadingIcon = {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = activeColor)
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // =================================================================
            // 2. تخصيص الألوان الأساسية وثيم الجمعية (Primary Color Customization)
            // =================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_color_customization_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(activeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "تخصيص الألوان الأساسية وثيم الجمعية",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "اختر اللون المعبر عن هوية جمعيتك لحفظه سحابياً",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // بطاقة معاينة مباشرة لطابع الجمعية
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = activeColor.copy(alpha = 0.08f)),
                            border = BorderStroke(1.5.dp, activeColor.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(selectedLogo, fontSize = 20.sp)
                                        Text(
                                            text = editShortName.ifBlank { currentTenant.shortName },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = activeColor
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = activeColor
                                    ) {
                                        Text(
                                            text = "معاينة حية",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "هكذا ستظهر الأزرار والشارات وبطاقات التحفيظ باللون المختار.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // شبكة خيارات الألوان المجهزة
                        Text(
                            text = "نماذج الألوان المعتمدة للجمعيات:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SocietyColorPresets) { preset ->
                                val isSelected = selectedColorHex.equals(preset.hex, ignoreCase = true)
                                val presetColor = parseHexColor(preset.hex)

                                Card(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable { selectedColorHex = preset.hex },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) presetColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) presetColor else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(presetColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = preset.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = preset.hex,
                                            fontSize = 9.sp,
                                            color = TextMuted,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // إدخال مخصص لكود اللون (Custom Hex)
                        OutlinedTextField(
                            value = selectedColorHex,
                            onValueChange = {
                                selectedColorHex = it
                            },
                            label = { Text("كود اللون الأساسي (HEX)") },
                            placeholder = { Text("#1976D2") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_custom_hex_input"),
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(activeColor)
                                        .border(1.dp, Color.Gray, CircleShape)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // =================================================================
            // 3. ضبط وتخصيص إعدادات الإشعارات (Granular Notification Controls)
            // =================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_notifications_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(activeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "إعدادات الإشعارات والتنبيهات الميدانية",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "التحكم في وصول رسائل التحفيظ وتنبيهات أولياء الأمور والمشرفين",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // المفتاح الرئيسي الشامل للإشعارات
                        NotificationToggleRow(
                            title = "تفعيل نظام الإشعارات للجمعية",
                            subtitle = "المفتاح الرئيسي لإرسال واستقبال كافة التنبيهات الميدانية",
                            icon = Icons.Default.NotificationsActive,
                            checked = systemWideNotificationsEnabled,
                            activeColor = activeColor,
                            onCheckedChange = { systemWideNotificationsEnabled = it }
                        )

                        // التبديلات الفرعية (تنشط فقط عند تفعيل المفتاح العام)
                        AnimatedVisibility(visible = systemWideNotificationsEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                NotificationToggleRow(
                                    title = "تنبيهات تحضير الحلقات اليومية",
                                    subtitle = "إشعار المعلمين والطلاب قبل بدء الحلقة ومتابعة الغياب",
                                    icon = Icons.Default.EventAvailable,
                                    checked = dailyAttendanceAlertsEnabled,
                                    activeColor = activeColor,
                                    onCheckedChange = { dailyAttendanceAlertsEnabled = it }
                                )

                                NotificationToggleRow(
                                    title = "رسائل واتساب الآلية لأولياء الأمور",
                                    subtitle = "إرسال تقرير الحفظ اليومي ودرجات التسميع لواتساب ولي الأمر",
                                    icon = Icons.AutoMirrored.Filled.Chat,
                                    checked = whatsappAlertsEnabled,
                                    activeColor = activeColor,
                                    onCheckedChange = { whatsappAlertsEnabled = it }
                                )

                                NotificationToggleRow(
                                    title = "تنبيهات وتقارير المشرفين العاجلة",
                                    subtitle = "إشعار فوري عند انخفاض نسبة حضور أي حلقة عن 80%",
                                    icon = Icons.Default.Shield,
                                    checked = urgentSupervisorReportsEnabled,
                                    activeColor = activeColor,
                                    onCheckedChange = { urgentSupervisorReportsEnabled = it }
                                )

                                NotificationToggleRow(
                                    title = "إشعارات الاختبارات الشهرية والإجازات",
                                    subtitle = "تنبيه عند اعتماد موعد اختبار طالب أو استحقاق شهادة حفظ",
                                    icon = Icons.Default.WorkspacePremium,
                                    checked = examCertificatesNotificationsEnabled,
                                    activeColor = activeColor,
                                    onCheckedChange = { examCertificatesNotificationsEnabled = it }
                                )

                                NotificationToggleRow(
                                    title = "أصوات واهتزاز التنبيهات",
                                    subtitle = "تشغيل نغمة لطيفة واهتزاز مع كل إشعار رسمي وارد",
                                    icon = Icons.Default.VolumeUp,
                                    checked = soundVibrationEnabled,
                                    activeColor = activeColor,
                                    onCheckedChange = { soundVibrationEnabled = it }
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // زر إرسال إشعار تجريبي لاختبار التوافق على الجهاز
                                OutlinedButton(
                                    onClick = {
                                        LocalNotificationHelper.showSessionReminderNotification(
                                            context = context,
                                            title = "🔔 تجربة تنبيه جمعية ${editShortName.ifBlank { currentTenant.shortName }}",
                                            message = "نظام الإشعارات مفعل ومربوط بنجاح. تعمل التنبيهات بكفاءة عالية."
                                        )
                                        showTestNotificationBanner = true
                                        Toast.makeText(context, "تم إرسال إشعار تجريبي للجهاز", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, activeColor.copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = activeColor.copy(alpha = 0.05f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationAdd,
                                        contentDescription = null,
                                        tint = activeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "إرسال إشعار تجريبي فوري",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = activeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 4. زر الحفظ النهائي والمزامنة مع Firestore
            // =================================================================
            item {
                Button(
                    onClick = { savePreferencesToFirestore() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("settings_save_all_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                    enabled = !isSyncingToFirestore
                ) {
                    if (isSyncingToFirestore) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("جاري الحفظ والمزامنة السحابية في Firestore...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حفظ وتثبيت التفضيلات في Cloud Firestore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // قسم إضافي للتنقل للروابط السريعة
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToConnectedDevices,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الأجهزة المتصلة", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onNavigateToAboutSociety,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عن الجمعية", fontSize = 11.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * صف لعنصر تبديل خيار إشعار مخصص
 */
@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    activeColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked) activeColor.copy(alpha = 0.05f) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (checked) activeColor.copy(alpha = 0.25f) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (checked) activeColor.copy(alpha = 0.15f) else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) activeColor else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = activeColor
                )
            )
        }
    }
}
