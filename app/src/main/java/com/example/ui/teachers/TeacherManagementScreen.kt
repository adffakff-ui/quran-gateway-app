package com.example.ui.teachers

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherManagementScreen(
    currentTenant: Tenant,
    onBack: () -> Unit
) {
    // Teacher state repository
    var teachersList by remember { mutableStateOf(SocietyRepository.getInitialTeachers()) }
    var availableRings by remember { mutableStateOf(SocietyRepository.getInitialRings()) }

    // Filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<TeacherStatus?>(null) } // null = All
    var filterAvailableOnly by remember { mutableStateOf(false) }

    var selectedTeacher by remember { mutableStateOf<Teacher?>(teachersList.firstOrNull()) }
    var selectedDetailTab by remember { mutableStateOf(0) } // 0: Profile, 1: Rings, 2: Leaves, 3: Audit
    var mobileViewTab by remember { mutableStateOf(0) } // 0: list, 1: detail (adaptive mobile)

    // Dialogs
    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var assigningTeacherToRing by remember { mutableStateOf<Teacher?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Filtered items
    val filteredTeachers = teachersList.filter { teacher ->
        val matchesSearch = searchQuery.isBlank() ||
                teacher.name.contains(searchQuery, ignoreCase = true) ||
                teacher.phone.contains(searchQuery)
        val matchesStatus = selectedStatusFilter == null || teacher.status == selectedStatusFilter
        val matchesAvailable = !filterAvailableOnly || teacher.isAvailable
        matchesSearch && matchesStatus && matchesAvailable
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                // رأس الصفحة: "إدارة المعلمين" [زر: إضافة معلم جديد]
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "إدارة المعلمين",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = SocietyBlueLight,
                                contentColor = SocietyBluePrimary
                            ) {
                                Text(
                                    text = "${filteredTeachers.size} معلم",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("teacher_mgmt_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع إلى لوحة المشرف"
                            )
                        }
                    },
                    actions = {
                        // زر: إضافة معلم جديد
                        Button(
                            onClick = { showAddTeacherDialog = true },
                            modifier = Modifier
                                .testTag("add_teacher_button")
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إضافة معلم جديد",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = SlateBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ==========================================
                // فلتر / بحث (يمين)
                // - بحث باسم / جوال
                // - حالة: مفعل / معلق
                // - مؤهل / متاح
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // حقل البحث
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث باسم المعلم أو رقم الجوال...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = SocietyBluePrimary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("teacher_search_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SocietyBluePrimary,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // رقاقات الفلترة (حالة: مفعل / معلق، متاح)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الفلاتر:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )

                            // الكل
                            FilterChip(
                                selected = selectedStatusFilter == null && !filterAvailableOnly,
                                onClick = {
                                    selectedStatusFilter = null
                                    filterAvailableOnly = false
                                },
                                label = { Text("الكل", fontSize = 12.sp) }
                            )

                            // مفعل
                            FilterChip(
                                selected = selectedStatusFilter == TeacherStatus.ACTIVE,
                                onClick = {
                                    selectedStatusFilter = if (selectedStatusFilter == TeacherStatus.ACTIVE) null else TeacherStatus.ACTIVE
                                },
                                label = { Text("مفعل", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )

                            // معلق
                            FilterChip(
                                selected = selectedStatusFilter == TeacherStatus.SUSPENDED,
                                onClick = {
                                    selectedStatusFilter = if (selectedStatusFilter == TeacherStatus.SUSPENDED) null else TeacherStatus.SUSPENDED
                                },
                                label = { Text("معلق", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = StatusError,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )

                            // متاح
                            FilterChip(
                                selected = filterAvailableOnly,
                                onClick = { filterAvailableOnly = !filterAvailableOnly },
                                label = { Text("متاح حالياً", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // التنبيه السريع إذا وجد
                if (snackbarMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SocietyBlueLight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SocietyBluePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = snackbarMessage ?: "", fontSize = 12.sp, color = SocietyBlueDark, modifier = Modifier.weight(1f))
                            IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = SocietyBluePrimary)
                            }
                        }
                    }
                }

                // ==========================================
                // المحتوى الرئيسي: قائمة المعلمين + العمود التفصيلي
                // ==========================================
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isCompact = maxWidth < 650.dp
                    if (isCompact) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TabRow(
                                selectedTabIndex = mobileViewTab,
                                containerColor = Color.White,
                                contentColor = SocietyBluePrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Tab(
                                    selected = mobileViewTab == 0,
                                    onClick = { mobileViewTab = 0 },
                                    text = { Text("قائمة المعلمين (${filteredTeachers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = mobileViewTab == 1,
                                    onClick = { mobileViewTab = 1 },
                                    text = {
                                        Text(
                                            text = selectedTeacher?.name?.let { "تفاصيل: $it" } ?: "تفاصيل المعلم",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (mobileViewTab == 0) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 32.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (filteredTeachers.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.PersonOff,
                                                        contentDescription = null,
                                                        tint = TextSubtle,
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "لا يوجد معلمون مطابقون لمعايير البحث",
                                                        fontSize = 13.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    items(filteredTeachers, key = { it.id }) { teacher ->
                                        val isSelected = selectedTeacher?.id == teacher.id
                                        TeacherCard(
                                            teacher = teacher,
                                            isSelected = isSelected,
                                            onSelect = {
                                                selectedTeacher = teacher
                                                mobileViewTab = 1
                                            },
                                            onAssignToRing = { assigningTeacherToRing = teacher },
                                            onToggleStatus = {
                                                teachersList = teachersList.map {
                                                    if (it.id == teacher.id) {
                                                        val newStatus = if (it.status == TeacherStatus.ACTIVE) TeacherStatus.SUSPENDED else TeacherStatus.ACTIVE
                                                        val updated = it.copy(status = newStatus)
                                                        if (selectedTeacher?.id == it.id) selectedTeacher = updated
                                                        updated
                                                    } else it
                                                }
                                                snackbarMessage = if (teacher.status == TeacherStatus.ACTIVE)
                                                    "تم إيقاف حساب ${teacher.name} مؤقتاً"
                                                else
                                                    "تم تنشيط حساب ${teacher.name} بنجاح"
                                            }
                                        )
                                    }
                                }
                            } else {
                                if (selectedTeacher != null) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("teacher_detail_column"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, BorderSubtle),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        TeacherDetailPane(
                                            teacher = selectedTeacher!!,
                                            selectedTab = selectedDetailTab,
                                            onTabSelected = { selectedDetailTab = it },
                                            onAssignRingClick = { assigningTeacherToRing = selectedTeacher },
                                            onRemoveRing = { ringName ->
                                                teachersList = teachersList.map {
                                                    if (it.id == selectedTeacher!!.id) {
                                                        val updated = it.copy(assignedRings = it.assignedRings.filter { r -> r != ringName })
                                                        selectedTeacher = updated
                                                        updated
                                                    } else it
                                                }
                                                snackbarMessage = "تم إلغاء تعيين $ringName"
                                            }
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("يرجى اختيار معلم من القائمة لعرض تفاصيله", color = TextMuted)
                                    }
                                }
                            }
                        }
                    } else {
                        // Wide screen / Tablet: side-by-side Row
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .fillMaxHeight(),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (filteredTeachers.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(40.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PersonOff,
                                                    contentDescription = null,
                                                    tint = TextSubtle,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "لا يوجد معلمون مطابقون لمعايير البحث",
                                                    fontSize = 13.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                }

                                items(filteredTeachers, key = { it.id }) { teacher ->
                                    val isSelected = selectedTeacher?.id == teacher.id
                                    TeacherCard(
                                        teacher = teacher,
                                        isSelected = isSelected,
                                        onSelect = { selectedTeacher = teacher },
                                        onAssignToRing = { assigningTeacherToRing = teacher },
                                        onToggleStatus = {
                                            teachersList = teachersList.map {
                                                if (it.id == teacher.id) {
                                                    val newStatus = if (it.status == TeacherStatus.ACTIVE) TeacherStatus.SUSPENDED else TeacherStatus.ACTIVE
                                                    val updated = it.copy(status = newStatus)
                                                    if (selectedTeacher?.id == it.id) selectedTeacher = updated
                                                    updated
                                                } else it
                                            }
                                            snackbarMessage = if (teacher.status == TeacherStatus.ACTIVE)
                                                "تم إيقاف حساب ${teacher.name} مؤقتاً"
                                            else
                                                "تم تنشيط حساب ${teacher.name} بنجاح"
                                        }
                                    )
                                }
                            }

                            if (selectedTeacher != null) {
                                Card(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .fillMaxHeight()
                                        .testTag("teacher_detail_column"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    TeacherDetailPane(
                                        teacher = selectedTeacher!!,
                                        selectedTab = selectedDetailTab,
                                        onTabSelected = { selectedDetailTab = it },
                                        onAssignRingClick = { assigningTeacherToRing = selectedTeacher },
                                        onRemoveRing = { ringName ->
                                            teachersList = teachersList.map {
                                                if (it.id == selectedTeacher!!.id) {
                                                    val updated = it.copy(assignedRings = it.assignedRings.filter { r -> r != ringName })
                                                    selectedTeacher = updated
                                                    updated
                                                } else it
                                            }
                                            snackbarMessage = "تم إلغاء تعيين $ringName"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // Modal: إضافة معلم جديد
        // ==========================================
        if (showAddTeacherDialog) {
            var newName by remember { mutableStateOf("") }
            var newPhone by remember { mutableStateOf("") }
            var newEmail by remember { mutableStateOf("") }
            var newQualification by remember { mutableStateOf("") }
            var newNationalId by remember { mutableStateOf("") }
            var newExperience by remember { mutableStateOf("5") }

            AlertDialog(
                onDismissRequest = { showAddTeacherDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = SocietyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة معلم جديد للجمعية", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "تسجيل بيانات المعلم في سياق ${currentTenant.shortName}:",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("اسم المعلم كاملاً") },
                            placeholder = { Text("مثال: الشيخ عبد الرحمن بن راشد") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("رقم الجوال") },
                            placeholder = { Text("05xxxxxxxx") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text("البريد الإلكتروني") },
                            placeholder = { Text("teacher@maknoon.org.sa") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newQualification,
                            onValueChange = { newQualification = it },
                            label = { Text("المؤهل القرآني / العلمي") },
                            placeholder = { Text("مثال: إجازة برواية حفص وشعبة / بكالوريوس شريعة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newNationalId,
                            onValueChange = { newNationalId = it },
                            label = { Text("رقم الهوية الوطنية / الإقامة") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newExperience,
                            onValueChange = { newExperience = it },
                            label = { Text("سنوات الخبرة") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                val createdTeacher = Teacher(
                                    id = "tch_${System.currentTimeMillis()}",
                                    name = newName,
                                    phone = if (newPhone.isNotBlank()) newPhone else "0501112233",
                                    email = if (newEmail.isNotBlank()) newEmail else "teacher@society.org",
                                    qualification = if (newQualification.isNotBlank()) newQualification else "إجازة حفظ القرآن الكريم وتجويده",
                                    experienceYears = newExperience.toIntOrNull() ?: 3,
                                    isAvailable = true,
                                    status = TeacherStatus.ACTIVE,
                                    assignedRings = emptyList(),
                                    nationalId = if (newNationalId.isNotBlank()) newNationalId else "1098765432",
                                    hireDate = "اليوم",
                                    certificates = listOf("شهادة إجازة معتمدة من جمعية تحفيظ القرآن"),
                                    leaveRequests = emptyList(),
                                    auditLogs = listOf(
                                        AuditEntry("aud_new", "تسجيل معلم جديد بالنظام", "الآن", "المشرف العام")
                                    )
                                )
                                teachersList = listOf(createdTeacher) + teachersList
                                selectedTeacher = createdTeacher
                                showAddTeacherDialog = false
                                snackbarMessage = "تمت إضافة المعلم ${createdTeacher.name} بنجاح!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("حفظ وإضافة")
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
        // Modal: تعيين لحلقة تحفيظ
        // ==========================================
        if (assigningTeacherToRing != null) {
            val teacher = assigningTeacherToRing!!
            var selectedRingToAssign by remember { mutableStateOf(availableRings.firstOrNull()) }

            AlertDialog(
                onDismissRequest = { assigningTeacherToRing = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AssignmentInd, contentDescription = null, tint = SocietyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تعيين لحلقة تحفيظ", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "اختر الحلقة المناسبة لإسنادها إلى ${teacher.name}:",
                            fontSize = 13.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        availableRings.forEach { ring ->
                            val isSelected = selectedRingToAssign?.id == ring.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedRingToAssign = ring },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SocietyBlueLight else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) SocietyBluePrimary else BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = ring.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "الموعد: ${ring.timeSlot} • ${ring.studentCount} طالباً",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SocietyBluePrimary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedRingToAssign != null) {
                                val ringName = selectedRingToAssign!!.name
                                teachersList = teachersList.map {
                                    if (it.id == teacher.id) {
                                        val updatedRings = if (it.assignedRings.contains(ringName)) it.assignedRings else it.assignedRings + ringName
                                        val updatedAudit = listOf(AuditEntry("a_${System.currentTimeMillis()}", "تعيين لحلقة: $ringName", "الآن", "المشرف العام")) + it.auditLogs
                                        val updated = it.copy(assignedRings = updatedRings, auditLogs = updatedAudit)
                                        if (selectedTeacher?.id == it.id) selectedTeacher = updated
                                        updated
                                    } else it
                                }
                                snackbarMessage = "تم تعيين ${teacher.name} لـ $ringName بنجاح!"
                                assigningTeacherToRing = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("تأكيد التعيين")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { assigningTeacherToRing = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

// ==========================================
// بطاقة المعلم الفردية (Teacher Card)
// ==========================================
@Composable
fun TeacherCard(
    teacher: Teacher,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAssignToRing: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("teacher_card_${teacher.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SocietyBlueLight.copy(alpha = 0.5f) else Color.White
        ),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) SocietyBluePrimary else BorderSubtle
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // صورة + اسم المعلم + الحالة
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // رمز المعلم / الأفاتار
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (teacher.status == TeacherStatus.ACTIVE) SocietyBluePrimary else Color(0xFF94A3B8)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = teacher.name.split(" ").getOrNull(1)?.take(1)
                                ?: teacher.name.take(1),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = teacher.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "هاتف: ${teacher.phone} • خبرة: ${teacher.experienceYears} سنوات",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // شارة الحالة (مفعل / معلق)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (teacher.status == TeacherStatus.ACTIVE) StatusSuccess.copy(alpha = 0.12f)
                            else StatusWarning.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = teacher.status.label,
                        color = if (teacher.status == TeacherStatus.ACTIVE) StatusSuccess else StatusWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // مستوى / مؤهل
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = EmeraldDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = teacher.qualification,
                    fontSize = 12.sp,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // الحلقات المعين لها (معاينة)
            if (teacher.assignedRings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = TextSubtle,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "الحلقات: ${teacher.assignedRings.joinToString("، ")}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))

            // أزرار: عرض الملف | تعيين لحلقة | إيقاف الحساب
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // عرض الملف
                FilledTonalButton(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SocietyBlueLight,
                        contentColor = SocietyBluePrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("عرض الملف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // تعيين لحلقة
                OutlinedButton(
                    onClick = onAssignToRing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldDark),
                    border = BorderStroke(1.dp, EmeraldDark.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعيين لحلقة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // إيقاف الحساب / تنشيط
                OutlinedButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (teacher.status == TeacherStatus.ACTIVE) StatusError else StatusSuccess
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (teacher.status == TeacherStatus.ACTIVE) StatusError.copy(alpha = 0.4f) else StatusSuccess.copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (teacher.status == TeacherStatus.ACTIVE) Icons.Default.Block else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (teacher.status == TeacherStatus.ACTIVE) "إيقاف الحساب" else "تنشيط",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// العمود التفصيلي (عند اختيار معلم)
// تبويب 1: الملف الشخصي (بيانات، شهادات، تواصل)
// تبويب 2: الحلقات المعيّن لها
// تبويب 3: طلبات إجازة / تبادل الحلقات
// تبويب 4: سجل النشاط (audit)
// ==========================================
@Composable
fun TeacherDetailPane(
    teacher: Teacher,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAssignRingClick: () -> Unit,
    onRemoveRing: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // رأس اللوحة التفصيلية
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SocietyBlueLight.copy(alpha = 0.6f))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(text = teacher.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(text = "رقم الهوية: ${teacher.nationalId} • تاريخ التعيين: ${teacher.hireDate}", fontSize = 11.sp, color = TextMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (teacher.status == TeacherStatus.ACTIVE) StatusSuccess else StatusWarning)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = teacher.status.label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // شريط التبويبات الأربعة
        val tabs = listOf(
            "الملف الشخصي",
            "الحلقات (${teacher.assignedRings.size})",
            "طلبات الإجازة",
            "سجل النشاط"
        )
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 8.dp,
            containerColor = Color.White,
            contentColor = SocietyBluePrimary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = BorderSubtle)

        // محتوى التبويب المختار
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            when (selectedTab) {
                // تبويب 1: الملف الشخصي (بيانات، شهادات، تواصل)
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // قسم التواصل
                        DetailSectionTitle(title = "بيانات التواصل", icon = Icons.Outlined.ContactPhone)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DetailRow("رقم الجوال:", teacher.phone)
                                DetailRow("البريد الإلكتروني:", teacher.email)
                                DetailRow("الهوية الوطنية:", teacher.nationalId)
                                DetailRow("تاريخ التعاقد:", teacher.hireDate)
                                DetailRow("سنوات الخبرة:", "${teacher.experienceYears} سنوات")
                            }
                        }

                        // قسم المؤهل العلمي
                        DetailSectionTitle(title = "المؤهل والإجازات القرآنية", icon = Icons.Outlined.School)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldLight.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldDark.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = teacher.qualification, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "معتمد لدى إدارة الشؤون التعليمية بالجمعية", fontSize = 11.sp, color = TextMuted)
                            }
                        }

                        // قسم الشهادات
                        DetailSectionTitle(title = "الشهادات والدورات المعتمدة", icon = Icons.Outlined.Verified)
                        teacher.certificates.forEach { cert ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = cert, fontSize = 12.sp, color = TextDark)
                                }
                            }
                        }
                    }
                }

                // تبويب 2: الحلقات المعيّن لها
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "الحلقات المسندة للمعلم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Button(
                                onClick = onAssignRingClick,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إسناد حلقة", fontSize = 11.sp)
                            }
                        }

                        if (teacher.assignedRings.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لم يتم تعيين المعلم لأي حلقة حالياً",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        } else {
                            teacher.assignedRings.forEach { ringName ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = ringName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                            Text(text = "المسار: حفظ ومراجعة • الحالة: نشطة", fontSize = 11.sp, color = TextMuted)
                                        }
                                        IconButton(
                                            onClick = { onRemoveRing(ringName) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "إلغاء التعيين", tint = StatusError)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // تبويب 3: طلبات إجازة / تبادل الحلقات
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailSectionTitle(title = "طلبات الإجازات وتبادل الحصص", icon = Icons.Outlined.EventBusy)

                        if (teacher.leaveRequests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "لا توجد طلبات إجازة أو تبادل معلقة لهذا المعلم", fontSize = 12.sp, color = TextMuted)
                            }
                        } else {
                            teacher.leaveRequests.forEach { leave ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = leave.type, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (leave.status == "معتمد") StatusSuccess.copy(alpha = 0.15f)
                                                        else GoldenAmberLight
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = leave.status,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (leave.status == "معتمد") StatusSuccess else GoldenAmber
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "الفترة: ${leave.dateRange}", fontSize = 11.sp, color = TextDark)
                                        if (leave.substituteTeacher != null) {
                                            Text(text = "المعلم البديل: ${leave.substituteTeacher}", fontSize = 11.sp, color = SocietyBluePrimary)
                                        }
                                        Text(text = "السبب: ${leave.reason}", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }

                // تبويب 4: سجل النشاط (audit)
                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailSectionTitle(title = "سجل العمليات والتدقيق (Audit Log)", icon = Icons.Outlined.History)

                        teacher.auditLogs.forEach { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SocietyBluePrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = log.action, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                        Text(text = "بواسطة: ${log.performedBy} • ${log.timestamp}", fontSize = 10.sp, color = TextMuted)
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

@Composable
private fun DetailSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = TextMuted)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }
}
