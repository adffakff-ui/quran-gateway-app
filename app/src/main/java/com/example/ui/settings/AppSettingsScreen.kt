package com.example.ui.settings

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Tenant
import com.example.ui.security.BiometricHelper
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    currentTenant: Tenant,
    onTenantUpdated: (Tenant) -> Unit,
    darkModePref: String = "system",
    onDarkModeChange: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Handle back press
    BackHandler {
        onBack()
    }

    // Editable Tenant state
    var editName by remember { mutableStateOf(currentTenant.name) }
    var editShortName by remember { mutableStateOf(currentTenant.shortName) }
    var editCity by remember { mutableStateOf(currentTenant.city) }
    var editCode by remember { mutableStateOf(currentTenant.code) }
    var editLicense by remember { mutableStateOf(currentTenant.licenseNumber) }

    // Notification Preferences
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var whatsappAlertsEnabled by remember { mutableStateOf(true) }
    var dailyRecitationReminder by remember { mutableStateOf(true) }
    var soundVibrationEnabled by remember { mutableStateOf(true) }

    // Security & Biometric setting
    var biometricLockEnabled by remember { mutableStateOf(true) }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "إعدادات التطبيق والجمعية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
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
            // 1. Theme / Appearance Settings Card (Dark / Light mode)
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    text = "إدارة السمات والمظهر (Dark/Light Mode)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "اختر المظهر المفضل للواجهات ومطابقة النظام",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf(
                                "system" to "تلقائي (النظام)",
                                "light" to "الوضع الفاتح ☀️",
                                "dark" to "الوضع الداكن 🌙"
                            )

                            themes.forEach { (key, label) ->
                                val selected = darkModePref == key
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onDarkModeChange(key)
                                        snackbarMessage = "تم تحديث السمة بنجاح: $label"
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_$key"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Society Details Customization Card
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Business,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300)
                                )
                            }
                            Column {
                                Text(
                                    text = "تخصيص بيانات الجمعية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تعديل اسم الجمعية، المدينة، والترخيص",
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
                            value = editCode,
                            onValueChange = { editCode = it },
                            label = { Text("رمز الجمعية التعريفي") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_edit_code"),
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

            // 3. Notification Preferences Card
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32)
                                )
                            }
                            Column {
                                Text(
                                    text = "تفضيلات التنبيهات والإشعارات",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تخصيص التنبيهات الفورية وإرسال رسائل WhatsApp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingSwitchRow(
                            title = "الإشعارات الفورية (Push Notifications)",
                            subtitle = "تنبيهات الحضور، الحفظ، ومتابعة الطلاب",
                            checked = pushNotificationsEnabled,
                            onCheckedChange = {
                                pushNotificationsEnabled = it
                                snackbarMessage = if (it) "تم تفعيل الإشعارات الفورية" else "تم إيقاف الإشعارات الفورية"
                            },
                            testTag = "switch_push_notifications"
                        )

                        SettingSwitchRow(
                            title = "تنبيهات واتساب الآلية (WhatsApp Alerts)",
                            subtitle = "إرسال تقارير الحفظ والتقييم لولي الأمر تلقائياً",
                            checked = whatsappAlertsEnabled,
                            onCheckedChange = {
                                whatsappAlertsEnabled = it
                                snackbarMessage = if (it) "تم تفعيل تنبيهات واتساب" else "تم إيقاف تنبيهات واتساب"
                            },
                            testTag = "switch_whatsapp_alerts"
                        )

                        SettingSwitchRow(
                            title = "تذكير التلاوة اليومي (Daily Alarms)",
                            subtitle = "تنبيه للمشرف والمعلم بمواعيد الحلقات",
                            checked = dailyRecitationReminder,
                            onCheckedChange = {
                                dailyRecitationReminder = it
                                snackbarMessage = if (it) "تم تفعيل التذكير اليومي" else "تم إيقاف التذكير اليومي"
                            },
                            testTag = "switch_daily_reminder"
                        )

                        SettingSwitchRow(
                            title = "الأصوات والاهتزاز (Sound & Vibration)",
                            subtitle = "تشغيل تأثيرات تنبيه عند التفاعل مع السجل",
                            checked = soundVibrationEnabled,
                            onCheckedChange = {
                                soundVibrationEnabled = it
                                snackbarMessage = if (it) "تم تشغيل الأصوات والاهتزاز" else "تم كتم الأصوات"
                            },
                            testTag = "switch_sound_vibration"
                        )
                    }
                }
            }

            // 4. Biometric Security Settings Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_security_card"),
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(GoldenAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = null,
                                    tint = GoldenAmber
                                )
                            }
                            Column {
                                Text(
                                    text = "الأمان والمصادقة الحيوية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تأمين الشاشات الإدارية بالبصمة وقفل الجهاز",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingSwitchRow(
                            title = "قفل البصمة الإداري المشدد",
                            subtitle = "مطلوب بصمة الإصبع أو الوجه للوصول لبيانات المالك والإعدادات",
                            checked = biometricLockEnabled,
                            onCheckedChange = {
                                biometricLockEnabled = it
                                snackbarMessage = if (it) "تم تفعيل القفل البيومتري الإداري" else "تم إيقاف القفل البيومتري"
                            },
                            testTag = "switch_biometric_lock"
                        )

                        OutlinedButton(
                            onClick = {
                                BiometricHelper.authenticateForAdministrativeAccess(
                                    context = context,
                                    operationName = "فحص واختبار مستشعر الأمان الحيوي",
                                    onSuccess = {
                                        snackbarMessage = "نجح التحقق من البصمة الحيوية بكفاءة عالية!"
                                    },
                                    onError = { err ->
                                        snackbarMessage = "نتيجة اختبار البصمة: $err"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختبار البصمة الحيوية الفوري", color = GoldenAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Snackbar notification
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
}

@Composable
fun SettingSwitchRow(
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
