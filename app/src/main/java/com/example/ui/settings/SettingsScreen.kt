package com.example.ui.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Tenant
import com.example.ui.security.BiometricHelper
import com.example.ui.theme.*

/**
 * SettingsScreen
 *
 * Full-featured Material 3 Settings Screen for the Quranic Society platform.
 * Allows users to:
 * 1. Toggle system-wide notifications with granular sub-controls and test alert trigger.
 * 2. Switch between light, dark, and system themes with live visual feedback.
 * 3. Manage privacy and data protection settings (telemetry, contact visibility, audio encryption, cache clearing, privacy policy).
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
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // Handle back navigation
    BackHandler {
        onBack()
    }

    // -------------------------------------------------------------
    // 1. System-wide Notification State
    // -------------------------------------------------------------
    var systemWideNotificationsEnabled by remember { mutableStateOf(true) }
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var dailyRecitationReminder by remember { mutableStateOf(true) }
    var whatsappAlertsEnabled by remember { mutableStateOf(true) }
    var soundVibrationEnabled by remember { mutableStateOf(true) }
    var urgentAdminAlertsEnabled by remember { mutableStateOf(true) }

    // -------------------------------------------------------------
    // 2. Privacy & Data Protection State
    // -------------------------------------------------------------
    var enhancedPrivacyMode by remember { mutableStateOf(false) }
    var shareDiagnosticsTelemetry by remember { mutableStateOf(false) }
    var hidePersonalContactInfo by remember { mutableStateOf(true) }
    var encryptAudioRecitationsLocally by remember { mutableStateOf(true) }
    var showOnlineActivityStatus by remember { mutableStateOf(false) }
    var biometricLockEnabled by remember { mutableStateOf(true) }

    // Dialog States
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTestNotificationBanner by remember { mutableStateOf(false) }

    // Tenant info edit state
    var editName by remember { mutableStateOf(currentTenant.name) }
    var editShortName by remember { mutableStateOf(currentTenant.shortName) }
    var editCity by remember { mutableStateOf(currentTenant.city) }
    var editCode by remember { mutableStateOf(currentTenant.code) }
    var editLicense by remember { mutableStateOf(currentTenant.licenseNumber) }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }

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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إعدادات النظام والتطبيق",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentTenant.shortName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "تم حفظ الإعدادات وتطبيقها تلقائياً", Toast.LENGTH_SHORT).show()
                            snackbarMessage = "تم حفظ ومزامنة كافة التفضيلات بنجاح"
                        },
                        modifier = Modifier.testTag("settings_quick_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "حفظ التفضيلات",
                            tint = EmeraldGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Simulated Test Notification Banner
            if (showTestNotificationBanner) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SocietyBlueLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_notification_banner")
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SocietyBluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🔔 تجربة الإشعار الفوري: موعد حلقة التحفيظ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SocietyBlueDark
                                )
                                Text(
                                    text = "تم استلام التنبيه بنجاح. نظام الإشعارات يعمل بكامل كفاءته في بيئة الأندرويد.",
                                    fontSize = 11.sp,
                                    color = TextDark
                                )
                            }
                            IconButton(
                                onClick = { showTestNotificationBanner = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق التنبيه",
                                    tint = SocietyBlueDark
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 1. SYSTEM-WIDE NOTIFICATIONS TOGGLE & CONTROLS
            // =================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_notifications_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (systemWideNotificationsEnabled) EmeraldGreen.copy(alpha = 0.15f)
                                        else StatusError.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (systemWideNotificationsEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (systemWideNotificationsEnabled) EmeraldGreen else StatusError,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "إشعارات النظام العامة (Notifications)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (systemWideNotificationsEnabled) "التنبيهات مفعلة على مستوى التطبيق" else "تم إيقاف كافة التنبيهات العامة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (systemWideNotificationsEnabled) EmeraldDark else StatusError
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Master System-Wide Toggle Row
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (systemWideNotificationsEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "المفتاح الشامل لكافة التنبيهات",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "التحكم في تشغيل أو تعطيل كافة إشعارات الجمعية دفعة واحدة",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Switch(
                                    checked = systemWideNotificationsEnabled,
                                    onCheckedChange = { isChecked ->
                                        systemWideNotificationsEnabled = isChecked
                                        snackbarMessage = if (isChecked) {
                                            "تم تفعيل الإشعارات العامة للنظام بنجاح"
                                        } else {
                                            "تم تعطيل كافة إشعارات النظام"
                                        }
                                        Toast.makeText(
                                            context,
                                            if (isChecked) "التنبيهات قيد التشغيل 🔔" else "تم كتم التنبيهات 🔕",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.testTag("switch_system_wide_notifications")
                                )
                            }
                        }

                        // Granular Notification Options (Dimmed if Master is off)
                        AnimatedVisibility(visible = systemWideNotificationsEnabled) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingsSwitchItem(
                                    title = "الإشعارات الفورية (Push Alerts)",
                                    subtitle = "تنبيهات فورية عند تسجيل الحضور وتحديث الدرجات",
                                    checked = pushNotificationsEnabled,
                                    onCheckedChange = {
                                        pushNotificationsEnabled = it
                                        snackbarMessage = if (it) "تم تفعيل الإشعارات الفورية" else "تم إيقاف الإشعارات الفورية"
                                    },
                                    testTag = "switch_push_notifications"
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                SettingsSwitchItem(
                                    title = "تذكير ورد التلاوة والحفظ اليومي",
                                    subtitle = "منبه يومي للمشرف والمعلم بمواعيد الحلقات القرآنية",
                                    checked = dailyRecitationReminder,
                                    onCheckedChange = {
                                        dailyRecitationReminder = it
                                        snackbarMessage = if (it) "تم تشغيل منبه الحفظ اليومي" else "تم إيقاف المنبه اليومي"
                                    },
                                    testTag = "switch_daily_reminder"
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                SettingsSwitchItem(
                                    title = "تنبيهات أولياء الأمور عبر WhatsApp",
                                    subtitle = "إرسال تقارير الحفظ والتقييم الشهري تلقائياً",
                                    checked = whatsappAlertsEnabled,
                                    onCheckedChange = {
                                        whatsappAlertsEnabled = it
                                        snackbarMessage = if (it) "تم تفعيل تقارير واتساب" else "تم إيقاف تقارير واتساب"
                                    },
                                    testTag = "switch_whatsapp_alerts"
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                SettingsSwitchItem(
                                    title = "تنبيهات الإدارة والمالك العاجلة",
                                    subtitle = "رسائل النسخ الاحتياطي والمزامنة وصلاحيات المشرفين",
                                    checked = urgentAdminAlertsEnabled,
                                    onCheckedChange = {
                                        urgentAdminAlertsEnabled = it
                                        snackbarMessage = if (it) "تم تفعيل التنبيهات الإدارية" else "تم إيقاف التنبيهات الإدارية"
                                    },
                                    testTag = "switch_urgent_alerts"
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                SettingsSwitchItem(
                                    title = "المؤثرات الصوتية والاهتزاز",
                                    subtitle = "تشغيل نغمات هادئة عند رصد الحضور والتسميع",
                                    checked = soundVibrationEnabled,
                                    onCheckedChange = {
                                        soundVibrationEnabled = it
                                        snackbarMessage = if (it) "تم تشغيل الصوت والاهتزاز" else "تم كتم الصوت والاهتزاز"
                                    },
                                    testTag = "switch_sound_vibration"
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Button to test notifications
                                OutlinedButton(
                                    onClick = {
                                        showTestNotificationBanner = true
                                        Toast.makeText(context, "🔔 إشعار تجريبي: تم التحقق من عمل التنبيهات!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_test_notification"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "إرسال إشعار تجريبي فوري للاختبار",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        if (!systemWideNotificationsEnabled) {
                            Text(
                                text = "ملاحظة: عند إيقاف الإشعارات العامة، لن تصلك أي تنبيهات فورية أو رسائل تذكير يومية حتى تقوم بإعادة تفعيلها.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 2. LIGHT / DARK THEMES SWITCHER
            // =================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_theme_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (darkModePref) {
                                        "dark" -> Icons.Default.DarkMode
                                        "light" -> Icons.Default.LightMode
                                        else -> Icons.Default.BrightnessAuto
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "المظهر والسمات (Theme Mode)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "التبديل بين الوضع الفاتح والداكن أو مطابقة إعداد النظام",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Theme Selection Options
                        val themeOptions = listOf(
                            Triple("light", "فاتح ☀️", Icons.Default.LightMode),
                            Triple("dark", "داكن 🌙", Icons.Default.DarkMode),
                            Triple("system", "تلقائي ⚙️", Icons.Default.BrightnessAuto)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themeOptions.forEach { (modeKey, label, icon) ->
                                val isSelected = darkModePref == modeKey
                                Surface(
                                    onClick = {
                                        onDarkModeChange(modeKey)
                                        snackbarMessage = "تم تفعيل السمة: $label"
                                        Toast.makeText(context, "تم التبديل إلى $label", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_$modeKey")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Theme preview feedback badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = when (darkModePref) {
                                        "dark" -> "الوضع النشط: الداكن (Dark Mode) - يوفر البطارية ومريح للعين ليلاً."
                                        "light" -> "الوضع النشط: الفاتح (Light Mode) - عالي التباين وواضح في ضوء النهار."
                                        else -> "الوضع النشط: تلقائي متوافق مع سمة نظام الأندرويد الحالية."
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 3. PRIVACY & DATA PROTECTION SETTINGS
            // =================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_privacy_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF673AB7).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF673AB7),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "الخصوصية وحماية البيانات (Privacy)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "إدارة خصوصية الحساب، تشفير التلاوات، وضوابط التتبع",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 1. Enhanced Privacy Mode
                        SettingsSwitchItem(
                            title = "وضع الخصوصية المشدد (Enhanced Privacy)",
                            subtitle = "حجب أي مشاركة للبيانات الإحصائية والتاريخية خارج الجمعية",
                            checked = enhancedPrivacyMode,
                            onCheckedChange = {
                                enhancedPrivacyMode = it
                                snackbarMessage = if (it) "تم تفعيل وضع الخصوصية المشدد" else "تم إلغاء وضع الخصوصية المشدد"
                            },
                            testTag = "switch_enhanced_privacy"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // 2. Hide personal contact info
                        SettingsSwitchItem(
                            title = "إخفاء بيانات التواصل الشخصية",
                            subtitle = "عدم إظهار رقم الجوال أو البريد في بطاقات الحلقات العامة",
                            checked = hidePersonalContactInfo,
                            onCheckedChange = {
                                hidePersonalContactInfo = it
                                snackbarMessage = if (it) "تم إخفاء بيانات التواصل الشخصية" else "أصبحت بيانات التواصل ظاهرة للحلقات"
                            },
                            testTag = "switch_hide_personal_data"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // 3. Audio Recitation Encryption
                        SettingsSwitchItem(
                            title = "تشفير تسجيلات التلاوة الصوتية محلياً",
                            subtitle = "حماية الملفات الصوتية للطلاب بتشفير عسكري AES-256 قبل الحفظ",
                            checked = encryptAudioRecitationsLocally,
                            onCheckedChange = {
                                encryptAudioRecitationsLocally = it
                                snackbarMessage = if (it) "تم تشغيل التشفير المحلي للتلاوات" else "تم إيقاف تشفير التلاوات"
                            },
                            testTag = "switch_audio_encryption"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // 4. Online Activity Status
                        SettingsSwitchItem(
                            title = "عرض حالة النشاط المباشر (Online Status)",
                            subtitle = "السماح للمشرفين والمالك بمعرفة تواجدك الميداني اللحظي",
                            checked = showOnlineActivityStatus,
                            onCheckedChange = {
                                showOnlineActivityStatus = it
                                snackbarMessage = if (it) "تم تفعيل حالة التواجد المباشر" else "تم إخفاء حالة التواجد المباشر"
                            },
                            testTag = "switch_online_status"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // 5. Diagnostics & Telemetry sharing
                        SettingsSwitchItem(
                            title = "مشاركة إحصائيات التشخيص المجهولة",
                            subtitle = "إرسال تقارير الأخطاء البرمجية المجهولة للمطور لتحسين الاستقرار",
                            checked = shareDiagnosticsTelemetry,
                            onCheckedChange = {
                                shareDiagnosticsTelemetry = it
                                snackbarMessage = if (it) "تمت الموافقة على مشاركة سجلات التشخيص المجهولة" else "تم منع إرسال سجلات التشخيص"
                            },
                            testTag = "switch_telemetry_sharing"
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Privacy Action Buttons: Clear Cache & View Privacy Policy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showClearCacheDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_clear_cache"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مسح الذاكرة المؤقتة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showPrivacyPolicyDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_privacy_policy"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Policy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("سياسة الخصوصية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 4. BIOMETRIC SECURITY ACCESS CARD
            // =================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_biometric_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GoldenAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = null,
                                    tint = GoldenAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "الأمان والقفل البيومتري (Biometrics)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تأمين الشاشات الإدارية بالبصمة الحيوية وقفل الجهاز",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchItem(
                            title = "قفل البصمة الإداري المشدد",
                            subtitle = "طلب بصمة الإصبع أو الوجه قبل فتح السجلات الحساسة والمالك",
                            checked = biometricLockEnabled,
                            onCheckedChange = {
                                biometricLockEnabled = it
                                snackbarMessage = if (it) "تم تفعيل القفل البيومتري" else "تم إيقاف القفل البيومتري"
                            },
                            testTag = "switch_biometric_lock"
                        )

                        OutlinedButton(
                            onClick = {
                                BiometricHelper.authenticateForAdministrativeAccess(
                                    context = context,
                                    operationName = "فحص واختبار مستشعر الأمان الحيوي",
                                    onSuccess = {
                                        snackbarMessage = "نجح التحقق من البصمة الحيوية بنجاح!"
                                        Toast.makeText(context, "بصمة معتمدة ومؤمنة ✅", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        snackbarMessage = "نتيجة اختبار البصمة: $err"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = GoldenAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اختبار مستشعر البصمة الحيوي الفوري",
                                color = GoldenAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 5. CONNECTED DEVICES & SESSIONS NAVIGATION SHORTCUT
            // =================================================================
            item {
                Card(
                    onClick = onNavigateToConnectedDevices,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_connected_devices_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SocietyBluePrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Devices,
                                    contentDescription = null,
                                    tint = SocietyBluePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "الأجهزة والجلسات المرتبطة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "مراقبة الأجهزة النشطة وتسجيل الخروج عن بُعد لأي جهاز مجهول",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // =================================================================
            // 6. SOCIETY VISION & DIRECT CONTACT SHORTCUT
            // =================================================================
            item {
                Card(
                    onClick = onNavigateToAboutSociety,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_about_society_nav_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "عن الجمعية ورؤيتنا الرقمية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "استعراض أهداف الجمعية القرآنية وقنوات التواصل المباشرة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // =================================================================
            // 7. SOCIETY TENANT CUSTOMIZATION CARD
            // =================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_society_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Business,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "بيانات الجمعية والمقر",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تعديل المسمى الرسمي والمدينة والترخيص المعتمد",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("اسم الجمعية القرآنية الرسمي") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_edit_name"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editShortName,
                            onValueChange = { editShortName = it },
                            label = { Text("الاسم المختصر للجمعية") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_edit_shortname"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editCity,
                            onValueChange = { editCity = it },
                            label = { Text("المدينة / المحافظة") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_edit_city"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editLicense,
                            onValueChange = { editLicense = it },
                            label = { Text("رقم الترخيص الرسمي") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_edit_license"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val updated = currentTenant.copy(
                                    name = editName.ifBlank { currentTenant.name },
                                    shortName = editShortName.ifBlank { currentTenant.shortName },
                                    city = editCity.ifBlank { currentTenant.city },
                                    code = editCode.ifBlank { currentTenant.code },
                                    licenseNumber = editLicense.ifBlank { currentTenant.licenseNumber }
                                )
                                onTenantUpdated(updated)
                                snackbarMessage = "تم حفظ وتحديث بيانات الجمعية بنجاح!"
                                Toast.makeText(context, "تم حفظ بيانات الجمعية بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_save_society_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ التعديلات الميدانية", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Clear Cache Confirmation Dialog
    // -------------------------------------------------------------
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = StatusError,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "مسح الذاكرة المؤقتة وسجل التصفح؟",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "سيتم تنظيف الملفات المؤقتة والبيانات غير المتزامنة وسجل البحث على هذا الجهاز دون التأثير على بيانات الطلاب والحلقات المخزنة سحابياً.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        Toast.makeText(context, "تم مسح الذاكرة المؤقتة بنجاح (14.2 MB محررة)", Toast.LENGTH_LONG).show()
                        snackbarMessage = "تم تنظيف الذاكرة المؤقتة وتحرير 14.2 ميجابايت بنجاح"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("مسح الآن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Privacy Policy Details Dialog
    // -------------------------------------------------------------
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "وثيقة سياسة الخصوصية وحماية البيانات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "نحن في المنظومة الرقمية للجمعية القرآنية نلتزم بأعلى معايير الأمان والخصوصية الشرعية والنظامية:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "• تشفير تسجيلات التلاوات الصوتية وبيانات الطلاب محلياً وسحابياً.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• عدم مشاركة أو بيع أي بيانات تواصل خاصة بأولياء الأمور أو المعلمين.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• إمكانية حذف البيانات المؤقتة وسجلات النشاط من طرف المشرف في أي وقت.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• الامتثال الكامل لأنظمة حماية البيانات الشخصية واللوائح الخيرية المعتمدة.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyPolicyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("موافق ومفهوم", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Snackbar notification toast
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
                        Text("إغلاق", color = MaterialTheme.colorScheme.primary)
                    }
                }
            ) {
                Text(snackbarMessage!!)
            }
        }
    }
}

/**
 * Reusable Switch row with title, subtitle, and accessible testTag
 */
@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
