package com.example.ui.profile

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class PermissionModule(
    val key: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePermissionManagementScreen(
    currentTenant: Tenant,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val usersList = remember { SocietyRepository.allUsers }
    var selectedUser by remember { mutableStateOf(usersList.firstOrNull()) }

    // Modules available for permission control
    val modules = remember {
        listOf(
            PermissionModule("students", "إدارة الطلاب وتسجيلهم", "عرض وتعديل بيانات الطلاب، أولياء الأمور، وقوائم الانتظار", Icons.Outlined.Groups),
            PermissionModule("teachers", "إدارة المعلمين والحلقات", "عرض وتعديل ملفات المعلمين وتسكين الحلقات القرآنية", Icons.Outlined.School),
            PermissionModule("attendance", "سجلات الحضور والغياب", "تسجيل ومتابعة حضور الطلاب والمعلمين اليومي", Icons.Outlined.Checklist),
            PermissionModule("reports", "التقارير والشهادات", "إصدار وتصدير تقارير الإنجاز وشهادات إتمام الحفظ", Icons.Outlined.Assessment),
            PermissionModule("finances", "الرسوم والدعم المالي", "متابعة الاشتراكات والتبرعات والدعم المالي للجمعية", Icons.Outlined.AccountBalanceWallet),
            PermissionModule("settings", "إعدادات الجمعية والنظام", "تعديل اسم الجمعية، بيانات الترخيص، وميزات النظام", Icons.Outlined.Settings)
        )
    }

    // Permission state map: userId -> (moduleKey -> Pair(canView, canEdit))
    val permissionsState = remember {
        mutableStateMapOf<String, MutableMap<String, Pair<Boolean, Boolean>>>().apply {
            usersList.forEach { user ->
                val map = mutableMapOf<String, Pair<Boolean, Boolean>>()
                modules.forEach { mod ->
                    val defaultView = true
                    val defaultEdit = when (user.role) {
                        UserRole.OWNER, UserRole.SUPERVISOR -> true
                        UserRole.TEACHER -> mod.key in listOf("students", "attendance")
                        UserRole.GUARDIAN -> false
                    }
                    map[mod.key] = Pair(defaultView, defaultEdit)
                }
                this[user.id] = map
            }
        }
    }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إدارة صلاحيات المستخدمين (RBAC)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${currentTenant.shortName} • تخصيص العرض والتعديل",
                            fontSize = 11.sp,
                            color = SocietyBluePrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SlateBackground
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left / Main Column: User Selector & Permission Matrix
            Column(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SocietyBlueLight),
                    border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = SocietyBluePrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "التحكم بالصلاحيات بحسب الدور (Role-Based Access Control)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SocietyBlueDark
                            )
                            Text(
                                text = "حدد المستخدم من القائمة الجانبية لتخصيص صلاحيات العرض (View) والتعديل (Edit) لكل وحدة في النظام فوراً.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                if (snackbarMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SocietyBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = snackbarMessage ?: "", fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f))
                            IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                            }
                        }
                    }
                }

                // Selected User Header Badge
                selectedUser?.let { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SocietyBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SocietyBluePrimary)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "${user.title} • الدور: ${user.role.label}", fontSize = 11.sp, color = TextMuted)
                                }
                            }

                            Button(
                                onClick = {
                                    snackbarMessage = "تم حفظ صلاحيات المستخدم ${user.name} بنجاح ومزامنتها سحابياً"
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ التغييرات", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Modules Permission List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(modules) { module ->
                        val currentUserPerms = selectedUser?.let { u ->
                            permissionsState[u.id]?.get(module.key) ?: Pair(true, false)
                        } ?: Pair(true, false)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = module.icon, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = module.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = module.description, fontSize = 11.sp, color = TextMuted)
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // View Toggle
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("عرض (View)", fontSize = 9.sp, color = TextSubtle)
                                        Switch(
                                            checked = currentUserPerms.first,
                                            onCheckedChange = { newVal ->
                                                selectedUser?.let { u ->
                                                    val map = permissionsState[u.id] ?: mutableMapOf()
                                                    val editVal = if (!newVal) false else currentUserPerms.second
                                                    map[module.key] = Pair(newVal, editVal)
                                                    permissionsState[u.id] = map
                                                }
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = SocietyBluePrimary)
                                        )
                                    }

                                    // Edit Toggle
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("تعديل (Edit)", fontSize = 9.sp, color = TextSubtle)
                                        Switch(
                                            checked = currentUserPerms.second,
                                            enabled = currentUserPerms.first,
                                            onCheckedChange = { newVal ->
                                                selectedUser?.let { u ->
                                                    val map = permissionsState[u.id] ?: mutableMapOf()
                                                    map[module.key] = Pair(currentUserPerms.first, newVal)
                                                    permissionsState[u.id] = map
                                                }
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = StatusSuccess)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right Column: Users List Selector
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "اختر المستخدم للتخصيص (${usersList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(usersList) { user ->
                            val isSelected = selectedUser?.id == user.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedUser = user },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SocietyBlueLight else SlateBackground
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) SocietyBluePrimary else BorderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) SocietyBluePrimary else Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else SocietyBluePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) SocietyBlueDark else TextDark
                                        )
                                        Text(
                                            text = user.role.label,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SocietyBluePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
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
