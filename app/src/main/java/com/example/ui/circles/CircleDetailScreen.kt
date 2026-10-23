package com.example.ui.circles

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(
    currentTenant: Tenant,
    ring: QuranRing,
    userRole: UserRole = UserRole.SUPERVISOR,
    onBack: () -> Unit,
    onNavigateToAttendance: () -> Unit
) {
    // Capacity & students state
    var currentCapacity by remember { mutableStateOf(ring.maxCapacity) }
    var studentsList by remember { mutableStateOf(SocietyRepository.getInitialCircleStudents()) }
    var waitlist by remember { mutableStateOf(SocietyRepository.getInitialWaitlist()) }
    var resourcesList by remember { mutableStateOf(SocietyRepository.getInitialResources()) }
    var assignmentsList by remember { mutableStateOf(SocietyRepository.getInitialAssignments()) }
    var auditLogs by remember { mutableStateOf(SocietyRepository.getInitialCapacityAudits()) }

    // Multi-selection state for students table
    val selectedStudentIds = remember { mutableStateListOf<String>() }

    // Overflow highlight IDs (when capacity was reduced below enrolled students)
    val overflowHighlightedIds = remember { mutableStateListOf<String>() }

    // Dialogs & Modals
    var showCapacityModal by remember { mutableStateOf(false) }
    var showAddResourceModal by remember { mutableStateOf(false) }
    var showWaitlistSheet by remember { mutableStateOf(false) }
    var showAuditSheet by remember { mutableStateOf(false) }
    var showBatchMessageModal by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Navigation sub-tab (0: Students list, 1: Circle Info & Resources, 2: Waitlist, 3: Capacity Audit)
    var activeSubSection by remember { mutableStateOf(0) }

    val enrolledCount = studentsList.size
    val isFull = enrolledCount >= currentCapacity
    val isNearCapacity = !isFull && (currentCapacity - enrolledCount) <= (currentCapacity * 0.1f)
    val occupancyRatio = if (currentCapacity > 0) enrolledCount.toFloat() / currentCapacity else 0f

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                // ==========================================
                // رأس: اسم الحلقة | مستوى: متوسط | عدد المقاعد: 100 | المعلم: اسم
                // - عرض حي: (المشغولة / الإجمالي) 75 / 100
                // ==========================================
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ring.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = SocietyBlueLight) {
                                    Text(
                                        text = "مستوى: ${ring.level}",
                                        fontSize = 10.sp,
                                        color = SocietyBluePrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "المعلم: ${ring.teacherName}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(text = "•", fontSize = 11.sp, color = TextSubtle)
                                Text(
                                    text = "المقاعد: $enrolledCount / $currentCapacity",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFull) StatusError else SocietyBluePrimary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("circle_detail_back_btn")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        // زر تعديل عدد المقاعد (للمشرف والمعلم فقط)
                        if (userRole == UserRole.SUPERVISOR || userRole == UserRole.TEACHER) {
                            Button(
                                onClick = { showCapacityModal = true },
                                modifier = Modifier
                                    .testTag("edit_capacity_button")
                                    .padding(end = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعديل عدد المقاعد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = SlateBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // شريط إشعار النظام السريع
                if (snackbarMessage != null) {
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
                            Text(text = snackbarMessage ?: "", fontSize = 12.sp, color = SocietyBlueDark, modifier = Modifier.weight(1f))
                            IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                    }
                }

                // ==========================================
                // شريط العرض الحي للسعة وشارات التنبيه
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "العرض الحي للمقاعد:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "($enrolledCount / $currentCapacity)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFull) StatusError else SocietyBluePrimary
                                )
                            }

                            // شارات الامتلاء والتنبيه
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isFull) {
                                    Badge(containerColor = StatusError.copy(alpha = 0.15f)) {
                                        Text(
                                            text = "ممتلئة تماماً",
                                            color = StatusError,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                } else if (isNearCapacity) {
                                    Badge(containerColor = GoldenAmberLight) {
                                        Text(
                                            text = "متبقي ${currentCapacity - enrolledCount} مقاعد فقط",
                                            color = GoldenAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                } else {
                                    Badge(containerColor = EmeraldLight) {
                                        Text(
                                            text = "متوفر ${currentCapacity - enrolledCount} مقعداً",
                                            color = EmeraldDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // شريط تقدم السعة (Occupancy Progress Bar)
                        LinearProgressIndicator(
                            progress = { occupancyRatio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = when {
                                isFull -> StatusError
                                isNearCapacity -> GoldenAmber
                                else -> SocietyBluePrimary
                            },
                            trackColor = Color(0xFFE2E8F0)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // أزرار سريعة للانتظار وسجل التدقيق
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نسبة الإشغال: ${(occupancyRatio * 100).toInt()}% • قائمة الانتظار: ${waitlist.size} طالب",
                                fontSize = 11.sp,
                                color = TextMuted
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { showWaitlistSheet = true },
                                    modifier = Modifier.testTag("open_waitlist_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Queue, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("قائمة الانتظار (${waitlist.size})", fontSize = 11.sp, color = SocietyBluePrimary)
                                }

                                TextButton(
                                    onClick = { showAuditSheet = true },
                                    modifier = Modifier.testTag("open_audit_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("سجل التعديلات", fontSize = 11.sp, color = TextDark)
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // التبويبات الثلاثية للواجهة:
                // 1. قائمة الطلاب (وسط)
                // 2. معلومات الحلقة والموارد (يمين ويسار)
                // ==========================================
                TabRow(
                    selectedTabIndex = activeSubSection,
                    containerColor = Color.White,
                    contentColor = SocietyBluePrimary
                ) {
                    Tab(
                        selected = activeSubSection == 0,
                        onClick = { activeSubSection = 0 },
                        text = { Text("قائمة الطلاب ($enrolledCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = activeSubSection == 1,
                        onClick = { activeSubSection = 1 },
                        text = { Text("معلومات الحلقة والجدول", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = activeSubSection == 2,
                        onClick = { activeSubSection = 2 },
                        text = { Text("موارد الحلقة والواجبات (${resourcesList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                // المحتوى بحسب التبويب
                when (activeSubSection) {
                    // ==========================================
                    // قسم: قائمة الطلاب (وسط)
                    // - جدول: صورة | اسم الطالب | ولي الأمر | حالة الحضور (اليوم) | تقييم آخر
                    // - أزرار على رأس الجدول: تحديد متعدد | تسجيل حضور جماعي | إرسال رسالة
                    // - الصفوف المتأثرة تُوسم (highlight) إذا خُفضت السعة
                    // ==========================================
                    0 -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("students_table_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // رأس الجدول والأزرار التفاعلية
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedStudentIds.size == studentsList.size && studentsList.isNotEmpty(),
                                            onCheckedChange = { checked ->
                                                selectedStudentIds.clear()
                                                if (checked) {
                                                    selectedStudentIds.addAll(studentsList.map { it.id })
                                                }
                                            }
                                        )
                                        Text(
                                            text = if (selectedStudentIds.isEmpty()) "تحديد الكل" else "محدد (${selectedStudentIds.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // زر: تسجيل حضور جماعي -> ينقل لشاشة الحضور
                                        OutlinedButton(
                                            onClick = onNavigateToAttendance,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldDark),
                                            border = BorderStroke(1.dp, EmeraldDark),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("batch_attendance_btn")
                                        ) {
                                            Icon(imageVector = Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تسجيل حضور جماعي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // زر: إرسال رسالة للطلاب المحددين
                                        Button(
                                            onClick = {
                                                if (selectedStudentIds.isNotEmpty()) {
                                                    showBatchMessageModal = true
                                                } else {
                                                    snackbarMessage = "يرجى تحديد طلاب لإرسال الرسالة"
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("send_batch_message_btn")
                                        ) {
                                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إرسال رسالة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (overflowHighlightedIds.isNotEmpty()) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.12f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "تنبيه: تم تمييز ${overflowHighlightedIds.size} طلاب متأثرين بتقليص السعة لنقلهم أو إزالتهم.",
                                                fontSize = 11.sp,
                                                color = StatusWarning,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                                // جدول الطلاب
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(studentsList, key = { it.id }) { student ->
                                        val isSelected = selectedStudentIds.contains(student.id)
                                        val isOverflowed = overflowHighlightedIds.contains(student.id)

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isSelected) selectedStudentIds.remove(student.id)
                                                    else selectedStudentIds.add(student.id)
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when {
                                                    isOverflowed -> StatusWarning.copy(alpha = 0.15f)
                                                    isSelected -> SocietyBlueLight
                                                    else -> Color(0xFFF8FAFC)
                                                }
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    isOverflowed -> StatusWarning
                                                    isSelected -> SocietyBluePrimary
                                                    else -> BorderSubtle
                                                }
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { checked ->
                                                            if (checked == true) selectedStudentIds.add(student.id)
                                                            else selectedStudentIds.remove(student.id)
                                                        }
                                                    )

                                                    // صورة / أيقونة الطالب
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isOverflowed) StatusWarning else SocietyBluePrimary),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = student.name.take(1),
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = student.name,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextDark
                                                            )
                                                            if (isOverflowed) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Badge(containerColor = StatusWarning) {
                                                                    Text("سعة زائدة", color = Color.White, fontSize = 9.sp)
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = "ولي الأمر: ${student.guardianName} (${student.guardianPhone})",
                                                            fontSize = 11.sp,
                                                            color = TextMuted
                                                        )
                                                    }
                                                }

                                                // حالة الحضور + آخر تقييم
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // شارة الحضور اليومي
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(
                                                                when (student.attendanceStatus) {
                                                                    AttendanceStatus.PRESENT -> StatusSuccess.copy(alpha = 0.15f)
                                                                    AttendanceStatus.ABSENT -> StatusError.copy(alpha = 0.15f)
                                                                    AttendanceStatus.LATE -> GoldenAmberLight
                                                                    AttendanceStatus.EXCUSED -> SocietyBlueLight
                                                                }
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "${student.attendanceStatus.symbol} ${student.attendanceStatus.label}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when (student.attendanceStatus) {
                                                                AttendanceStatus.PRESENT -> StatusSuccess
                                                                AttendanceStatus.ABSENT -> StatusError
                                                                AttendanceStatus.LATE -> GoldenAmber
                                                                AttendanceStatus.EXCUSED -> SocietyBluePrimary
                                                            }
                                                        )
                                                    }

                                                    // آخر تقييم
                                                    Text(
                                                        text = student.lastEvaluation,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextDark
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
                    // قسم: معلومات الحلقة (يمين)
                    // ==========================================
                    1 -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(text = "بيانات وجدول الحلقة", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)

                                InfoCardItem("المعلم المشرف:", ring.teacherName, Icons.Outlined.Person)
                                InfoCardItem("جدول الأوقات:", ring.timeSlot, Icons.Outlined.AccessTime)
                                InfoCardItem("الموقع والمقر:", ring.location, Icons.Outlined.Place)
                                InfoCardItem("رابط القاعة / زوم:", ring.zoomLink, Icons.Outlined.VideoCall)
                                InfoCardItem("المسار والمستوى:", "${ring.level} • ${ring.currentJuz}", Icons.AutoMirrored.Outlined.MenuBook)

                                HorizontalDivider(color = BorderSubtle)

                                Text(text = "وصف المحتوى والأهداف:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(
                                    text = ring.description,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    lineHeight = 18.sp
                                )

                                HorizontalDivider(color = BorderSubtle)

                                // سعة الحلقة للمستخدمين
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SocietyBlueLight.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "ضوابط التسجيل وسعة الحلقة:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SocietyBluePrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "المقاعد الإجمالية: $currentCapacity • المشغولة حالياً: $enrolledCount",
                                            fontSize = 12.sp,
                                            color = TextDark
                                        )
                                        if (isFull) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "الحلقة ممتلئة! يتم توجيه الطلاب الجدد تلقائياً إلى قائمة الانتظار.",
                                                fontSize = 11.sp,
                                                color = StatusError,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // قسم: موارد الحلقة والواجبات (يسار)
                    // ==========================================
                    2 -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "ملفات ومصاحف الحلقة المرفوعة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Button(
                                        onClick = { showAddResourceModal = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إضافة ملف", fontSize = 11.sp)
                                    }
                                }

                                resourcesList.forEach { res ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when (res.type) {
                                                        "PDF" -> Icons.Default.PictureAsPdf
                                                        "Audio" -> Icons.Default.Audiotrack
                                                        else -> Icons.Default.VideoLibrary
                                                    },
                                                    contentDescription = null,
                                                    tint = when (res.type) {
                                                        "PDF" -> StatusError
                                                        "Audio" -> EmeraldDark
                                                        else -> SocietyBluePrimary
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(text = res.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                                    Text(text = "الحجم: ${res.size} • تاريخ الرفع: ${res.uploadedDate}", fontSize = 10.sp, color = TextMuted)
                                                }
                                            }
                                            IconButton(onClick = { snackbarMessage = "جاري تحميل ${res.title}" }) {
                                                Icon(imageVector = Icons.Default.Download, contentDescription = "تحميل", tint = SocietyBluePrimary)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = BorderSubtle)
                                Spacer(modifier = Modifier.height(4.dp))

                                Text(text = "الخط الزمني للواجبات والاختبارات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)

                                assignmentsList.forEach { asgn ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(text = asgn.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                                Text(text = "الموعد النهائي: ${asgn.dueDate} • النوع: ${asgn.type}", fontSize = 10.sp, color = TextMuted)
                                            }
                                            Text(
                                                text = "أنجز: ${asgn.completedCount}/${asgn.totalCount}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SocietyBluePrimary
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

        // ==========================================
        // Modal: تعديل عدد المقاعد (Capacity Editor Modal)
        // - حقل رقمي مع أزرار +/-
        // - نص مساعد: "الحد الأدنى المسموح به هو عدد الطلاب المسجلين حالياً: 75"
        // - خيار: "نقل الطلاب الزائدين إلى قائمة انتظار تلقائياً" (تبديل)
        // - تحذير ملون إذا كانت القيمة أقل من المسجلين
        // - حفظ + إلغاء + تسجيل في الـ Audit Log
        // ==========================================
        if (showCapacityModal) {
            var tempCapacity by remember { mutableStateOf(currentCapacity) }
            var autoMoveToWaitlist by remember { mutableStateOf(true) }
            var editReason by remember { mutableStateOf("") }

            val minAllowed = enrolledCount
            val isReducingBelowEnrolled = tempCapacity < minAllowed
            val affectedCount = if (isReducingBelowEnrolled) minAllowed - tempCapacity else 0

            AlertDialog(
                onDismissRequest = { showCapacityModal = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = SocietyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تعديل سعة مقاعد الحلقة", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "الحلقة: ${ring.name} • المسجلون حالياً: $enrolledCount",
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        // الحقل الرقمي مع أزرار +/-
                        Text(text = "عدد المقاعد المعتمد:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FilledIconButton(
                                onClick = {
                                    if (tempCapacity > 1) tempCapacity--
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = SocietyBlueLight)
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "إنقاص", tint = SocietyBluePrimary)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "$tempCapacity",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isReducingBelowEnrolled) StatusWarning else SocietyBluePrimary,
                                modifier = Modifier.widthIn(min = 60.dp),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            FilledIconButton(
                                onClick = {
                                    if (tempCapacity < 300) tempCapacity++
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = SocietyBlueLight)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "زيادة", tint = SocietyBluePrimary)
                            }
                        }

                        // نص مساعد تحت الحقل
                        Text(
                            text = "الحد الأدنى الاعتيادي هو عدد الطلاب المسجلين حالياً: $minAllowed طالب (الحد الأقصى: 300)",
                            fontSize = 11.sp,
                            color = if (isReducingBelowEnrolled) StatusWarning else TextSubtle
                        )

                        // تحذير واضح عند محاولة تخفيض السعة تحت عدد المسجلين
                        if (isReducingBelowEnrolled) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, StatusWarning)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تحذير: هناك $affectedCount طلاب سيتأثرون!",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusWarning
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "اختر طريقة التعامل: يمكنك نقلهم تلقائياً لقائمة الانتظار أو الإزالة اليدوية.",
                                        fontSize = 11.sp,
                                        color = TextDark
                                    )
                                }
                            }
                        }

                        // خيار: نقل الطلاب الزائدين إلى قائمة انتظار تلقائياً (تبديل)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "نقل الطلاب الزائدين لقائمة انتظار تلقائياً",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "حفظ أسبقية التسجيل وإرسال إشعار فوري لأولياء الأمور",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = autoMoveToWaitlist,
                                onCheckedChange = { autoMoveToWaitlist = it }
                            )
                        }

                        // سبب التعديل (اختياري للـ Audit Log)
                        OutlinedTextField(
                            value = editReason,
                            onValueChange = { editReason = it },
                            label = { Text("سبب تعديل السعة (يسجل في سجل التدقيق)") },
                            placeholder = { Text("أدخل سبب التعديل") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val oldCap = currentCapacity
                            val newCap = tempCapacity
                            currentCapacity = newCap

                            // معالجة النقصان أو الزيادة
                            if (newCap < enrolledCount) {
                                val overflowCount = enrolledCount - newCap
                                overflowHighlightedIds.clear()

                                if (autoMoveToWaitlist) {
                                    // نقل آخر الطلاب تسجيلاً لقائمة الانتظار
                                    val affected = studentsList.takeLast(overflowCount)
                                    val newWaitlistItems = affected.mapIndexed { idx, st ->
                                        WaitlistStudent(
                                            id = "wl_trans_${System.currentTimeMillis()}_$idx",
                                            studentName = st.name,
                                            guardianName = st.guardianName,
                                            guardianPhone = st.guardianPhone,
                                            position = waitlist.size + idx + 1,
                                            joinedDate = "اليوم",
                                            notes = "نُقل تلقائياً إثر تقليص سعة الحلقة من $oldCap إلى $newCap"
                                        )
                                    }
                                    waitlist = waitlist + newWaitlistItems
                                    studentsList = studentsList.dropLast(overflowCount)
                                    snackbarMessage = "تم نقل $overflowCount طلاب إلى قائمة الانتظار وإرسال إشعارات لأولياء الأمور"
                                } else {
                                    // وسم الصفوف المتأثرة للتعامل اليدوي
                                    val affected = studentsList.takeLast(overflowCount)
                                    overflowHighlightedIds.addAll(affected.map { it.id })
                                    snackbarMessage = "تم تقليص السعة وتمييز $overflowCount طلاب للتعامل اليدوي"
                                }
                            } else if (newCap > oldCap) {
                                snackbarMessage = "تمت زيادة السعة إلى $newCap مقعداً، وتم إشعار قائمة الانتظار بالفرص الشاغرة!"
                            } else {
                                snackbarMessage = "تم تحديث السعة إلى $newCap مقعداً"
                            }

                            // تسجيل في Audit Log
                            val record = CapacityAuditRecord(
                                id = "aud_${System.currentTimeMillis()}",
                                actorName = SocietyRepository.defaultSupervisor.name,
                                actorRole = "المشرف العام",
                                circleName = ring.name,
                                oldCapacity = oldCap,
                                newCapacity = newCap,
                                actionOnOverflow = if (autoMoveToWaitlist) "نقل إلى قائمة الانتظار" else "وسم يدوي",
                                affectedCount = if (newCap < enrolledCount) enrolledCount - newCap else 0,
                                timestamp = "الآن",
                                reason = if (editReason.isNotBlank()) editReason else "تحديث سعة الحلقة"
                            )
                            auditLogs = listOf(record) + auditLogs
                            showCapacityModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("حفظ التغييرات")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCapacityModal = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // ==========================================
        // Sheet / Modal: قائمة الانتظار (Waitlist)
        // ==========================================
        if (showWaitlistSheet) {
            AlertDialog(
                onDismissRequest = { showWaitlistSheet = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Queue, contentDescription = null, tint = SocietyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("قائمة انتظار الحلقة (${waitlist.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ترتيب الطلاب في قائمة الانتظار للحصول على مقعد شاغر عند توفر السعة:",
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        if (waitlist.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                Text("لا يوجد طلاب في قائمة الانتظار حالياً", fontSize = 12.sp, color = TextMuted)
                            }
                        } else {
                            waitlist.forEach { wl ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(GoldenAmberLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = "#${wl.position}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GoldenAmber)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(text = wl.studentName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                                Text(text = "الولي: ${wl.guardianName} (${wl.guardianPhone})", fontSize = 10.sp, color = TextMuted)
                                            }
                                        }

                                        // قبول يدوي
                                        Button(
                                            onClick = {
                                                if (studentsList.size < currentCapacity) {
                                                    val newStd = CircleStudent(
                                                        id = "std_wl_${System.currentTimeMillis()}",
                                                        name = wl.studentName,
                                                        guardianName = wl.guardianName,
                                                        guardianPhone = wl.guardianPhone,
                                                        attendanceStatus = AttendanceStatus.PRESENT,
                                                        lastEvaluation = "جديد",
                                                        registrationOrder = studentsList.size + 1
                                                    )
                                                    studentsList = studentsList + newStd
                                                    waitlist = waitlist.filter { it.id != wl.id }
                                                    snackbarMessage = "تم قبول الطالب ${wl.studentName} في الحلقة بنجاح!"
                                                } else {
                                                    snackbarMessage = "لا يمكن القبول لأن الحلقة ممتلئة. قم بزيادة السعة أولاً."
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("قبول فوري", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showWaitlistSheet = false }) { Text("إغلاق") }
                }
            )
        }

        // ==========================================
        // Modal: سجل التعديلات والتدقيق (Capacity Audit Log)
        // ==========================================
        if (showAuditSheet) {
            AlertDialog(
                onDismissRequest = { showAuditSheet = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = SocietyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("سجل تدقيق تعديلات السعة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        auditLogs.forEach { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "السعة: ${log.oldCapacity} ➔ ${log.newCapacity} مقعداً", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SocietyBluePrimary)
                                        Text(text = log.timestamp, fontSize = 10.sp, color = TextSubtle)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "المعدل: ${log.actorName} (${log.actorRole})", fontSize = 11.sp, color = TextDark)
                                    Text(text = "الإجراء: ${log.actionOnOverflow} • السبب: ${log.reason}", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAuditSheet = false }) { Text("إغلاق") }
                }
            )
        }

        // ==========================================
        // Modal: إرسال رسالة للطلاب المحددين
        // ==========================================
        if (showBatchMessageModal) {
            var messageTxt by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showBatchMessageModal = false },
                title = { Text("إرسال رسالة إلى ${selectedStudentIds.size} طلاب", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column {
                        Text("سيتم إرسال الرسالة لأولياء أمور الطلاب المحددين عبر WhatsApp وSMS:", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = messageTxt,
                            onValueChange = { messageTxt = it },
                            placeholder = { Text("اكتب نص الرسالة هنا...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            snackbarMessage = "تم إرسال الرسالة إلى ${selectedStudentIds.size} أولياء أمور بنجاح!"
                            showBatchMessageModal = false
                            selectedStudentIds.clear()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("إرسال الآن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchMessageModal = false }) { Text("إلغاء") }
                }
            )
        }

        // ==========================================
        // Modal: إضافة ملف أو مورد للحلقة
        // ==========================================
        if (showAddResourceModal) {
            var fileTitle by remember { mutableStateOf("") }
            var fileType by remember { mutableStateOf("PDF") }

            AlertDialog(
                onDismissRequest = { showAddResourceModal = false },
                title = { Text("إضافة مورد للحلقة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fileTitle,
                            onValueChange = { fileTitle = it },
                            label = { Text("عنوان الملف أو المورد") },
                            placeholder = { Text("أدخل عنوان المورد") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = fileType == "PDF",
                                onClick = { fileType = "PDF" },
                                label = { Text("PDF") }
                            )
                            FilterChip(
                                selected = fileType == "Audio",
                                onClick = { fileType = "Audio" },
                                label = { Text("صوتي") }
                            )
                            FilterChip(
                                selected = fileType == "Video",
                                onClick = { fileType = "Video" },
                                label = { Text("مرئي") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (fileTitle.isNotBlank()) {
                                resourcesList = listOf(
                                    CircleResource("res_${System.currentTimeMillis()}", fileTitle, fileType, "2.4 MB", "اليوم")
                                ) + resourcesList
                                snackbarMessage = "تم رفع $fileTitle بنجاح!"
                                showAddResourceModal = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("رفع الملف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddResourceModal = false }) { Text("إلغاء") }
                }
            )
        }
    }
}

@Composable
private fun InfoCardItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 12.sp, color = TextMuted)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.weight(1f))
    }
}
