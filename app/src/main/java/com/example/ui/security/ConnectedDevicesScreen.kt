package com.example.ui.security

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConnectedDevice
import com.example.data.DeviceSecurityManager
import com.example.data.DeviceType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedDevicesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    var deviceToLogout by remember { mutableStateOf<ConnectedDevice?>(null) }
    var showLogoutAllDialog by remember { mutableStateOf(false) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val devices = DeviceSecurityManager.devices
    val currentDevice = devices.firstOrNull { it.isCurrentDevice }
    val otherDevices = devices.filterNot { it.isCurrentDevice }
    val suspectedCount = devices.count { it.isSuspected || !it.isTrusted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "الأجهزة والجلسات المرتبطة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "إدارة أمان جلسات المشرف وتسجيل الخروج عن بُعد",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("connected_devices_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showAddDeviceDialog = true
                        },
                        modifier = Modifier.testTag("pair_new_device_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "ربط جهاز جديد عبر QR",
                            tint = MaterialTheme.colorScheme.primary
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // شريط التنبيه الأمني عند وجود أجهزة غير معروفة
            // ==========================================
            if (suspectedCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_suspected_alert_card"),
                        colors = CardDefaults.cardColors(containerColor = CoralRed.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, CoralRed.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CoralRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تنبيه أمان: رصد جلسة مجهولة!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CoralRed
                                )
                                Text(
                                    text = "يوجد $suspectedCount جهاز غير موثوق به نشط حالياً. ننصح بإنهاء جلسته فوراً لحماية بيانات الجمعية.",
                                    fontSize = 11.sp,
                                    color = TextDark
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // إحصائيات سريعة للجلسات
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricMiniCard(
                        title = "إجمالي الجلسات",
                        value = "${devices.size}",
                        icon = Icons.Outlined.Devices,
                        color = SocietyBluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "أجهزة موثوقة",
                        value = "${devices.count { it.isTrusted }}",
                        icon = Icons.Outlined.VerifiedUser,
                        color = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "أجهزة غير معروفة",
                        value = "$suspectedCount",
                        icon = Icons.Outlined.SecurityUpdateWarning,
                        color = if (suspectedCount > 0) CoralRed else TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ==========================================
            // بطاقة الجهاز الحالي
            // ==========================================
            if (currentDevice != null) {
                item {
                    Text(
                        text = "الجهاز الحالي (قيد الاستخدام)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SocietyBluePrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DeviceItemCard(
                        device = currentDevice,
                        onRemoteLogout = {},
                        onToggleTrust = {},
                        isCurrent = true
                    )
                }
            }

            // ==========================================
            // عنوان قسم الأجهزة الأخرى وأزرار الإجراءات
            // ==========================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الأجهزة والجلسات الأخرى (${otherDevices.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )

                    if (otherDevices.isNotEmpty()) {
                        TextButton(
                            onClick = { showLogoutAllDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = CoralRed),
                            modifier = Modifier.testTag("logout_all_other_devices_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تسجيل الخروج من الكل",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ==========================================
            // قائمة الأجهزة الأخرى
            // ==========================================
            if (otherDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد جلسات أخرى مفتوحة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                            Text(
                                text = "حسابك نشط حالياً فقط على هذا الجهاز، وهو في أمان تام.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(otherDevices, key = { it.id }) { device ->
                    DeviceItemCard(
                        device = device,
                        onRemoteLogout = {
                            deviceToLogout = device
                        },
                        onToggleTrust = {
                            DeviceSecurityManager.toggleDeviceTrust(device.id)
                            snackbarMessage = "تم تحديث حالة وثوق الجهاز بنجاح"
                        },
                        isCurrent = false
                    )
                }
            }

            // مساحة إضافية في الأسفل
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ==========================================
        // حوار تأكيد تسجيل الخروج عن بُعد
        // ==========================================
        deviceToLogout?.let { dev ->
            AlertDialog(
                onDismissRequest = { deviceToLogout = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "تسجيل الخروج عن بُعد؟",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "هل أنت متأكد من إنهاء جلسة \"${dev.deviceName}\" فوراً؟",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "سيتم إبطال رمز الدخول وحظر وصول هذا الجهاز لبيانات الحلقات والطلاب على الفور، وسيتعين عليه إعادة إدخال بيانات الاعتماد وتأكيد البصمة للدخول مجدداً.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = DeviceSecurityManager.remoteLogoutDevice(dev.id)
                            if (success) {
                                snackbarMessage = "تم تسجيل الخروج عن بُعد من جهاز (${dev.deviceName}) بنجاح"
                                Toast.makeText(context, "تم إنهاء جلسة الجهاز عن بُعد", Toast.LENGTH_SHORT).show()
                            }
                            deviceToLogout = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        modifier = Modifier.testTag("confirm_remote_logout_btn")
                    ) {
                        Text("إنهاء الجلسة فوراً", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deviceToLogout = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // ==========================================
        // حوار إنهاء جميع الجلسات الأخرى
        // ==========================================
        if (showLogoutAllDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutAllDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "تسجيل الخروج من كافة الأجهزة الأخرى؟",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "سيؤدي هذا الإجراء إلى إغلاق كافة الجلسات المفتوحة على الحواسيب والأجهزة اللوحية الأخرى والإبقاء على هذا الجهاز فقط.",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val count = DeviceSecurityManager.remoteLogoutAllOtherDevices()
                            snackbarMessage = "تم تسجيل الخروج بنجاح من $count جهاز آخر"
                            Toast.makeText(context, "تم إنهاء جميع الجلسات الأخرى", Toast.LENGTH_SHORT).show()
                            showLogoutAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        modifier = Modifier.testTag("confirm_logout_all_btn")
                    ) {
                        Text("إنهاء جميع الجلسات", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutAllDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // ==========================================
        // حوار ربط جهاز جديد
        // ==========================================
        if (showAddDeviceDialog) {
            var newName by remember { mutableStateOf("") }
            var newModel by remember { mutableStateOf("") }
            var selectedType by remember { mutableStateOf(DeviceType.SMARTPHONE) }

            AlertDialog(
                onDismissRequest = { showAddDeviceDialog = false },
                title = {
                    Text("إضافة جهاز إشرافي جديد", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "يمكنك إضافة وتوثيق جهاز مشرف مساعد للوصول السريع إلى الحلقات:",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("اسم الجهاز (مثل: جوال المشرف الميداني)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newModel,
                            onValueChange = { newModel = it },
                            label = { Text("موديل الجهاز (مثل: Galaxy A54)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            DeviceSecurityManager.registerNewDevice(
                                deviceName = newName,
                                deviceModel = newModel,
                                deviceType = selectedType,
                                location = "الرياض، المملكة العربية السعودية"
                            )
                            snackbarMessage = "تمت إضافة الجهاز وتوثيقه بنجاح"
                            showAddDeviceDialog = false
                        },
                        modifier = Modifier.testTag("submit_new_device_btn")
                    ) {
                        Text("توثيق الجهاز")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDeviceDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // ==========================================
        // Snackbar
        // ==========================================
        if (snackbarMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("حسناً", color = MaterialTheme.colorScheme.primary)
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
fun DeviceItemCard(
    device: ConnectedDevice,
    onRemoteLogout: () -> Unit,
    onToggleTrust: () -> Unit,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_card_${device.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) SocietyBlueLight.copy(alpha = 0.45f)
            else if (device.isSuspected) CoralRed.copy(alpha = 0.05f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else if (device.isSuspected) 1.5.dp else 1.dp,
            color = if (isCurrent) SocietyBluePrimary.copy(alpha = 0.6f)
            else if (device.isSuspected) CoralRed
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // أيقونة نوع الجهاز
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCurrent) SocietyBluePrimary.copy(alpha = 0.15f)
                            else if (device.isSuspected) CoralRed.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (device.deviceType) {
                            DeviceType.SMARTPHONE -> Icons.Outlined.Smartphone
                            DeviceType.TABLET -> Icons.Outlined.Tablet
                            DeviceType.DESKTOP -> Icons.Outlined.Laptop
                            DeviceType.UNKNOWN -> Icons.Outlined.DeviceUnknown
                        },
                        contentDescription = null,
                        tint = if (isCurrent) SocietyBluePrimary
                        else if (device.isSuspected) CoralRed
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // تفاصيل الجهاز
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = device.deviceName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark
                        )

                        if (isCurrent) {
                            Surface(
                                color = EmeraldGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "هذا الجهاز",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (device.isSuspected) {
                            Surface(
                                color = CoralRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "مجهول / غير معتاد",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${device.deviceModel} • ${device.osVersion}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // معلومات الموقع والـ IP
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = device.location,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Text(
                            text = "IP: ${device.ipAddress}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = if (device.lastActive.contains("الآن")) EmeraldGreen else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "آخر نشاط: ${device.lastActive}",
                            fontSize = 11.sp,
                            color = if (device.lastActive.contains("الآن")) EmeraldGreen else TextMuted,
                            fontWeight = if (device.lastActive.contains("الآن")) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // أزرار التحكم للأجهزة الأخرى
            if (!isCurrent) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // زر تغيير حالة الوثوق
                    TextButton(
                        onClick = onToggleTrust,
                        modifier = Modifier.testTag("toggle_trust_btn_${device.id}")
                    ) {
                        Icon(
                            imageVector = if (device.isTrusted) Icons.Outlined.ShieldMoon else Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (device.isTrusted) TextMuted else EmeraldGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (device.isTrusted) "إلغاء الوثوق" else "تعيين كموثوق",
                            fontSize = 11.sp,
                            color = if (device.isTrusted) TextMuted else EmeraldGreen
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // زر تسجيل الخروج عن بُعد
                    Button(
                        onClick = onRemoteLogout,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralRed.copy(alpha = 0.12f),
                            contentColor = CoralRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("remote_logout_btn_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تسجيل خروج عن بُعد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricMiniCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(text = title, fontSize = 10.sp, color = TextMuted, textAlign = TextAlign.Center)
        }
    }
}
