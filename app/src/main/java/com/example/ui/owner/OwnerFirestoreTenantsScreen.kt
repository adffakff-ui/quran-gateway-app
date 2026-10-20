package com.example.ui.owner

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * واجهة تحكم خاصة وحصرية بمالك التطبيق لإدارة الجمعيات (Tenants)
 * مباشرة عبر قاعدة بيانات Cloud Firestore (إضافة، تعديل، حذف، ومزامنة لحظية).
 * هذه الواجهة مخفية تماماً ومحمية من أي مستخدم آخر.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerFirestoreTenantsScreen(
    currentTenant: Tenant,
    userRole: UserRole = UserRole.OWNER,
    onTenantSelected: (Tenant) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onNavigateBack()
    }

    // =========================================================================
    // حاجز أمني صارم: إخفاء ومنع الواجهة تماماً عن أي مستخدم آخر غير مالك التطبيق
    // =========================================================================
    if (userRole != UserRole.OWNER) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("وصول محظور", fontWeight = FontWeight.Bold) },
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
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.5.dp, StatusError)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "واجهة سحابية محجوبة تماماً",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        LaunchedEffect(Unit) {
                            OwnerSecurityManager.logIntrusionAttempt(
                                context = context,
                                attemptType = "محاولة وصول غير مصرح بها لواجهة Firestore",
                                attemptedInput = "رتبة المستخدم: ${userRole.label}"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هذه الشاشة مخصصة حصراً لمالك التطبيق المصرح له. تم حجب بيانات المالك (${OwnerSecurityManager.maskEmail()}) أمنياً.\n\n⚠️ تم توثيق المحاولة وإشعار المالك فورياً عبر الواتساب والبريد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                        ) {
                            Text("رجوع")
                        }
                    }
                }
            }
        }
        return
    }

    // Repository for Firestore Tenants
    val firestoreRepo = remember { FirestoreTenantRepository(context) }
    val firestoreTenantsState by firestoreRepo.getTenantsFlow().collectAsState(initial = emptyList())

    // If Firestore has items, use them; otherwise fallback to local repository so the owner can push them
    val displayTenants = if (firestoreTenantsState.isNotEmpty()) firestoreTenantsState else SocietyRepository.tenants
    val isUsingLiveFirestore = firestoreTenantsState.isNotEmpty()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGovernorateFilter by remember { mutableStateOf("الكل") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var isOperatingOnFirestore by remember { mutableStateOf(false) }

    // Dialog States
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTenant by remember { mutableStateOf<Tenant?>(null) }
    var tenantToDelete by remember { mutableStateOf<Tenant?>(null) }

    // Filter list
    val filteredList = remember(searchQuery, selectedGovernorateFilter, displayTenants) {
        displayTenants.filter { tenant ->
            val matchesQuery = searchQuery.isBlank() ||
                    tenant.name.contains(searchQuery, ignoreCase = true) ||
                    tenant.city.contains(searchQuery, ignoreCase = true) ||
                    tenant.code.contains(searchQuery, ignoreCase = true)

            val matchesGov = selectedGovernorateFilter == "الكل" ||
                    tenant.governorate.contains(selectedGovernorateFilter, ignoreCase = true) ||
                    tenant.city.contains(selectedGovernorateFilter, ignoreCase = true)

            matchesQuery && matchesGov
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = GoldenAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إدارة الجمعيات المباشرة (Firestore)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "تحكم سحابي حصري للمالك: adffakff@gmail.com",
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
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isOperatingOnFirestore = true
                                val count = firestoreRepo.syncAllToFirestore(SocietyRepository.tenants)
                                isOperatingOnFirestore = false
                                snackbarMessage = "تمت مزامنة ورفع $count جمعية إلى قاعدة بيانات Firestore بنجاح!"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "رفع الكل إلى Firestore",
                            tint = SocietyBluePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SocietyBluePrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                text = { Text("إضافة جمعية سحابياً", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Live Firestore Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isUsingLiveFirestore) Color(0xFF22C55E) else GoldenAmber)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isUsingLiveFirestore) "قاعدة بيانات Firestore: متصلة ولحظية" else "Firestore: في انتظار أول رفع للبيانات",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = "${displayTenants.size} جمعية",
                                color = GoldenAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "المسار السحابي: /tenants/{tenantId} • موثق باسم المالك adffakff@gmail.com",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    if (!isUsingLiveFirestore) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isOperatingOnFirestore = true
                                    val count = firestoreRepo.seedInitialTenantsIfEmpty(SocietyRepository.tenants)
                                    isOperatingOnFirestore = false
                                    snackbarMessage = "تم رفع $count جمعية تأسيسية إلى Firestore بنجاح!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تهيئة ورفع الجمعيات إلى Firestore السحابية الآن", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar & Filters
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن جمعية بالاسم، المدينة، أو الكود...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Governorate Filter Chips
                val governorates = listOf("الكل", "صنعاء", "تعز", "عدن", "حضرموت", "مأرب", "إب")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(governorates) { gov ->
                        FilterChip(
                            selected = selectedGovernorateFilter == gov,
                            onClick = { selectedGovernorateFilter = gov },
                            label = { Text(gov) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Loading indicator
            if (isOperatingOnFirestore) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    color = SocietyBluePrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Tenants List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { tenant ->
                    val isCurrent = tenant.id == currentTenant.id
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) SocietyBlueLight else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) SocietyBluePrimary else BorderSubtle
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tenant.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = SocietyBluePrimary
                                            ) {
                                                Text(
                                                    text = "الحالية",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "المعرف السحابي (ID): ${tenant.id}",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "المحافظة: ${tenant.city} • الكود: ${tenant.code} • الترخيص: ${tenant.licenseNumber}",
                                        fontSize = 11.sp,
                                        color = TextDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Stats Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${tenant.ringsCount} حلقة") }
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${tenant.studentsCount} طالب") }
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${tenant.teachersCount} معلم") }
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${tenant.attendanceRate}% حضور") }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderSubtle)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons: Set Active, Edit in Firestore, Delete from Firestore
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        onTenantSelected(tenant)
                                        Toast.makeText(context, "تم تعيين ${tenant.shortName} كجمعية نشطة", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تفعيل وإدارة", fontSize = 12.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { editingTenant = tenant },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تعديل سحابي", fontSize = 12.sp)
                                    }

                                    IconButton(
                                        onClick = { tenantToDelete = tenant },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "حذف من Firestore",
                                            tint = StatusError,
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

        // ==========================================
        // Modal: إضافة جمعية جديدة مباشرة إلى Firestore
        // ==========================================
        if (showAddDialog) {
            var name by remember { mutableStateOf("") }
            var shortName by remember { mutableStateOf("") }
            var city by remember { mutableStateOf("صنعاء") }
            var code by remember { mutableStateOf("YE-${(100..999).random()}") }
            var license by remember { mutableStateOf("LIC-YE-${(1000..9999).random()}") }
            var rings by remember { mutableStateOf("25") }
            var students by remember { mutableStateOf("500") }
            var teachers by remember { mutableStateOf("20") }
            var selectedColor by remember { mutableStateOf("#1976D2") }
            var selectedLogo by remember { mutableStateOf("🕌") }

            val colorPresets = listOf(
                "#1976D2" to "أزرق",
                "#2E7D32" to "أخضر",
                "#673AB7" to "بنفسجي",
                "#C2185B" to "قرمزي",
                "#E65100" to "برتقالي",
                "#00695C" to "تركواز"
            )
            val logoPresets = listOf("🕌", "📖", "🌙", "⭐", "🏛️", "🌟", "📚", "🎓")

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = SocietyBluePrimary) },
                title = { Text("إضافة جمعية وتخصيص الواجهة", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                if (shortName.isBlank() && it.length > 5) shortName = it.take(20)
                            },
                            label = { Text("الاسم الكامل للجمعية") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = shortName,
                            onValueChange = { shortName = it },
                            label = { Text("الاسم المختصر") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("المدينة/المحافظة") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("الكود") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = license,
                            onValueChange = { license = it },
                            label = { Text("رقم الترخيص الرسمي") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rings,
                                onValueChange = { rings = it },
                                label = { Text("الحلقات") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = students,
                                onValueChange = { students = it },
                                label = { Text("الطلاب") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = teachers,
                                onValueChange = { teachers = it },
                                label = { Text("المعلمون") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("تخصيص اللون الأساسي:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            colorPresets.forEach { (hex, label) ->
                                val isSelected = selectedColor == hex
                                Surface(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { selectedColor = hex },
                                    color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { SocietyBluePrimary },
                                    border = if (isSelected) BorderStroke(2.dp, Color.Black) else null
                                ) {}
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("تخصيص شعار / رمز الجمعية (Logo):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            logoPresets.forEach { symbol ->
                                val isSelected = selectedLogo == symbol
                                Surface(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedLogo = symbol },
                                    color = if (isSelected) SocietyBluePrimary.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.3f),
                                    border = if (isSelected) BorderStroke(1.5.dp, SocietyBluePrimary) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = symbol, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val newTenant = Tenant(
                                    id = "t_fs_${System.currentTimeMillis()}",
                                    name = name.trim(),
                                    shortName = if (shortName.isNotBlank()) shortName.trim() else name.trim(),
                                    city = city.trim(),
                                    code = code.trim(),
                                    ringsCount = rings.toIntOrNull() ?: 20,
                                    studentsCount = students.toIntOrNull() ?: 400,
                                    teachersCount = teachers.toIntOrNull() ?: 15,
                                    attendanceRate = 95,
                                    unreadMessagesCount = 0,
                                    licenseNumber = license.trim(),
                                    country = "اليمن",
                                    governorate = city.trim(),
                                    primaryColorHex = selectedColor,
                                    logoIdentifier = selectedLogo
                                )
                                coroutineScope.launch {
                                    isOperatingOnFirestore = true
                                    firestoreRepo.saveTenant(newTenant)
                                    SocietyRepository.addTenant(newTenant)
                                    isOperatingOnFirestore = false
                                    showAddDialog = false
                                    snackbarMessage = "تم إضافة وحفظ وتخصيص واجهة جمعية ${newTenant.name} بنجاح!"
                                }
                            }
                        }
                    ) {
                        Text("حفظ في Firestore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // ==========================================
        // Modal: تعديل بيانات جمعية في Firestore
        // ==========================================
        editingTenant?.let { target ->
            var editName by remember { mutableStateOf(target.name) }
            var editShortName by remember { mutableStateOf(target.shortName) }
            var editCity by remember { mutableStateOf(target.city) }
            var editCode by remember { mutableStateOf(target.code) }
            var editLicense by remember { mutableStateOf(target.licenseNumber) }
            var editRings by remember { mutableStateOf(target.ringsCount.toString()) }
            var editStudents by remember { mutableStateOf(target.studentsCount.toString()) }
            var editTeachers by remember { mutableStateOf(target.teachersCount.toString()) }
            var editColor by remember { mutableStateOf(target.primaryColorHex) }
            var editLogo by remember { mutableStateOf(target.logoIdentifier) }

            val colorPresets = listOf(
                "#1976D2" to "أزرق",
                "#2E7D32" to "أخضر",
                "#673AB7" to "بنفسجي",
                "#C2185B" to "قرمزي",
                "#E65100" to "برتقالي",
                "#00695C" to "تركواز"
            )
            val logoPresets = listOf("🕌", "📖", "🌙", "⭐", "🏛️", "🌟", "📚", "🎓")

            AlertDialog(
                onDismissRequest = { editingTenant = null },
                icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = SocietyBluePrimary) },
                title = { Text("تعديل بيانات وتخصيص واجهة الجمعية", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("اسم الجمعية") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editShortName,
                            onValueChange = { editShortName = it },
                            label = { Text("الاسم المختصر") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editCity,
                                onValueChange = { editCity = it },
                                label = { Text("المدينة") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editCode,
                                onValueChange = { editCode = it },
                                label = { Text("الكود") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = editLicense,
                            onValueChange = { editLicense = it },
                            label = { Text("رقم الترخيص") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editRings,
                                onValueChange = { editRings = it },
                                label = { Text("الحلقات") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editStudents,
                                onValueChange = { editStudents = it },
                                label = { Text("الطلاب") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editTeachers,
                                onValueChange = { editTeachers = it },
                                label = { Text("المعلمون") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("تعديل اللون الأساسي:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            colorPresets.forEach { (hex, label) ->
                                val isSelected = editColor == hex
                                Surface(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { editColor = hex },
                                    color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { SocietyBluePrimary },
                                    border = if (isSelected) BorderStroke(2.dp, Color.Black) else null
                                ) {}
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("تعديل شعار / رمز الجمعية (Logo):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            logoPresets.forEach { symbol ->
                                val isSelected = editLogo == symbol
                                Surface(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { editLogo = symbol },
                                    color = if (isSelected) SocietyBluePrimary.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.3f),
                                    border = if (isSelected) BorderStroke(1.5.dp, SocietyBluePrimary) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = symbol, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updated = target.copy(
                                name = editName.trim(),
                                shortName = editShortName.trim(),
                                city = editCity.trim(),
                                code = editCode.trim(),
                                licenseNumber = editLicense.trim(),
                                ringsCount = editRings.toIntOrNull() ?: target.ringsCount,
                                studentsCount = editStudents.toIntOrNull() ?: target.studentsCount,
                                teachersCount = editTeachers.toIntOrNull() ?: target.teachersCount,
                                governorate = editCity.trim(),
                                primaryColorHex = editColor,
                                logoIdentifier = editLogo
                            )
                            coroutineScope.launch {
                                isOperatingOnFirestore = true
                                firestoreRepo.saveTenant(updated)
                                SocietyRepository.updateTenant(updated)
                                isOperatingOnFirestore = false
                                editingTenant = null
                                snackbarMessage = "تم تحديث وتخصيص بيانات ${updated.shortName} بنجاح!"
                            }
                        }
                    ) {
                        Text("حفظ التعديلات")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingTenant = null }) { Text("إلغاء") }
                }
            )
        }

        // ==========================================
        // Modal: تأكيد حذف جمعية من Firestore
        // ==========================================
        tenantToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { tenantToDelete = null },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError) },
                title = { Text("تأكيد الحذف من Firestore", fontWeight = FontWeight.Bold, color = StatusError) },
                text = {
                    Text("هل أنت متأكد من حذف مستند جمعية \"${target.name}\" (المعرف: ${target.id}) نهائياً من قاعدة بيانات Firestore؟ لن يتمكن أي مستخدم من الوصول إليها بعد الحذف.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isOperatingOnFirestore = true
                                firestoreRepo.deleteTenant(target.id)
                                SocietyRepository.removeTenant(target.id)
                                isOperatingOnFirestore = false
                                tenantToDelete = null
                                snackbarMessage = "تم حذف الجمعية بنجاح من قاعدة بيانات Firestore."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Text("تأكيد الحذف النهائي")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { tenantToDelete = null }) { Text("إلغاء") }
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
