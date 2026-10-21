package com.example.ui.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.data.SocietyRepository
import com.example.data.Tenant
import com.example.data.UserRole
import com.example.ui.security.BiometricHelper
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountProfileAndPermissionsScreen(
    currentTenant: Tenant,
    userRole: UserRole = UserRole.SUPERVISOR,
    darkModePref: String = "system",
    onDarkModeChange: (String) -> Unit = {},
    onNavigateToRolePermissions: (() -> Unit)? = null,
    onNavigateToConnectedDevices: (() -> Unit)? = null,
    onNavigateToAboutSociety: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    var showUpdateDialog by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "معلومات الحساب والصلاحيات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
            // Dark Mode / Appearance Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                                    text = "إعدادات المظهر والوضع الداكن",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "التبديل التلقائي بناءً على النظام أو التبديل اليدوي",
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
                            val options = listOf(
                                "system" to "تلقائي (النظام)",
                                "dark" to "الوضع الداكن",
                                "light" to "الوضع الفاتح"
                            )
                            options.forEach { (key, label) ->
                                val isSelected = darkModePref == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onDarkModeChange(key) },
                                    label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f),
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

            // Biometric Login Switch
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل تسجيل الدخول بالبصمة", fontWeight = FontWeight.Bold)
                    Switch(
                        checked = SocietyRepository.isBiometricLoginEnabled,
                        onCheckedChange = { SocietyRepository.isBiometricLoginEnabled = it }
                    )
                }
            }

            // RBAC Management Card
            if (onNavigateToRolePermissions != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToRolePermissions() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFFFFB300))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFFFFB300))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "إدارة صلاحيات المستخدمين (RBAC)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "تخصيص صلاحيات العرض والتعديل لكل مستخدم في النظام",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFFFFB300))
                        }
                    }
                }
            }

            // Biometric Authentication & Admin Protection Card
            item {
                val context = LocalContext.current
                val biometricStatus = remember { BiometricHelper.checkBiometricStatus(context) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Fingerprint,
                                        contentDescription = "بصمة",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "المصادقة الحيوية المشددة (androidx.biometric)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "طبقة أمان إضافية للوصول الإداري والعمليات الحساسة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (biometricStatus.isUsable) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (biometricStatus.isUsable) "مفعل ومتاح" else "بحاجة إعداد",
                                    color = if (biometricStatus.isUsable) Color(0xFF2E7D32) else Color(0xFFF59E0B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Text(
                            text = biometricStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                BiometricHelper.authenticateForAdministrativeAccess(
                                    context = context,
                                    operationName = "التحقق من هوية المشرف الإداري",
                                    onSuccess = {
                                        snackbarMessage = "تم التحقق من البصمة الحيوية بنجاح! الصلاحية الإدارية مؤكدة."
                                    },
                                    onError = { err ->
                                        snackbarMessage = "فشل التحقق الحيوي: $err"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختبار البصمة الحيوية (إصبع / وجه / قفل الجهاز)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // زر إدارة الأجهزة المرتبطة والجلسات
                        if (onNavigateToConnectedDevices != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Button(
                                onClick = onNavigateToConnectedDevices,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SocietyBluePrimary
                                )
                            ) {
                                Icon(imageVector = Icons.Outlined.Devices, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إدارة الأجهزة والجلسات المرتبطة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // زر عن الجمعية ورؤيتها الرقمية
                        if (onNavigateToAboutSociety != null) {
                            OutlinedButton(
                                onClick = onNavigateToAboutSociety,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("عن الجمعية ورؤيتنا الرقمية والتواصل", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldGreen)
                            }
                        }
                    }
                }
            }

            // Header Info Card (Phone & Account Number)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "رقم الهاتف",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "738766810",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "رقم الحساب",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "492",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Section 1: Personal Information (المعلومات الشخصية)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "المعلومات الشخصية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Item 1: Username
                        ProfileRowItem(
                            icon = Icons.Outlined.Person,
                            iconTint = Color(0xFFFFB300),
                            title = "اسم الدخول",
                            subtitle = "738766810",
                            actionText = "تحديث",
                            onActionClick = { showUpdateDialog = "اسم الدخول" }
                        )

                        // Item 2: Password
                        ProfileRowItem(
                            icon = Icons.Outlined.Lock,
                            iconTint = Color(0xFFE91E63),
                            title = "كلمة السر",
                            subtitle = "••••••••••••",
                            actionText = "تحديث",
                            onActionClick = { showUpdateDialog = "كلمة السر" }
                        )

                        // Item 3: Address
                        ProfileRowItem(
                            icon = Icons.Outlined.LocationOn,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "العنوان",
                            subtitle = "${currentTenant.city} - ${currentTenant.name}",
                            actionText = null,
                            onActionClick = {}
                        )

                        // Item 4: Sub Accounts
                        ProfileRowItem(
                            icon = Icons.Outlined.Group,
                            iconTint = Color(0xFF4CAF50),
                            title = "الحسابات الفرعية",
                            subtitle = "معلومات الحسابات المرتبطة",
                            actionText = "عرض",
                            onActionClick = { snackbarMessage = "جميع الحسابات الفرعية مرتبطة بنجاح." }
                        )

                        // Item 5: Delete Account
                        ProfileRowItem(
                            icon = Icons.Outlined.Block,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = "إلغاء وحذف الحساب",
                            subtitle = "حذف نهائي للبيانات",
                            actionText = "حذف",
                            onActionClick = { snackbarMessage = "لا يمكن حذف الحساب النشط حالياً." }
                        )
                    }
                }
            }

            // Section 2: Account Permissions (صلاحيات الحساب)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "صلاحيات الحساب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        val permissions = listOf(
                            Triple("صلاحية المشرف العام", Icons.Outlined.AdminPanelSettings, true),
                            Triple("صلاحية المعلم", Icons.Outlined.School, true),
                            Triple("صلاحية الفرع", Icons.Outlined.Business, false),
                            Triple("صلاحية تطبيقي", Icons.Outlined.PhoneAndroid, true),
                            Triple("صلاحية واتساب", Icons.Outlined.Chat, true),
                            Triple("صلاحية الموزع / المساعد", Icons.Outlined.SupervisorAccount, false)
                        )

                        permissions.forEach { (permTitle, permIcon, isGranted) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = permIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = permTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Status Icon (Check or Block)
                                    Icon(
                                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp)
                                    )

                                    // Info button
                                    IconButton(
                                        onClick = { snackbarMessage = "تفاصيل الصلاحية: $permTitle (${if (isGranted) "مفعلة" else "غير مفعلة"})" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = "معلومات",
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

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
fun ProfileRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    actionText: String?,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (actionText != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
