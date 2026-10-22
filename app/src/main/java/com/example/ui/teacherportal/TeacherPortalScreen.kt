package com.example.ui.teacherportal

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherPortalScreen(
    currentTenant: Tenant,
    teacher: Teacher = SocietyRepository.getInitialTeachers().firstOrNull() ?: Teacher(
        id = "tch_01",
        name = "الشيخ عبد الله القدسي",
        phone = "771234567",
        email = "qudsi@maknoon.org.sa",
        qualification = "بكالوريوس قراءات ودراسات إسلامية",
        experienceYears = 8,
        isAvailable = true,
        status = TeacherStatus.ACTIVE,
        assignedRings = listOf("حلقة الإمام عاصم"),
        nationalId = "1045678901",
        hireDate = "01/01/2022",
        certificates = listOf("إجازة بقراءة عاصم"),
        leaveRequests = emptyList(),
        auditLogs = emptyList()
    ),
    onOpenAudioRecitation: (CircleStudent) -> Unit,
    onOpenAttendance: () -> Unit,
    onBack: () -> Unit
) {
    val rings = remember { SocietyRepository.getInitialRings().take(3) }
    var selectedRing by remember { mutableStateOf<QuranRing?>(rings.firstOrNull() ?: SocietyRepository.defaultRing) }
    var activeSubTab by remember { mutableStateOf(0) } // 0: مواد الحلقة, 1: تقييمات الطلاب
    var studentsList by remember { mutableStateOf(SocietyRepository.getInitialCircleStudents().take(15)) }

    // Dialogs
    var showSubstituteRequestModal by remember { mutableStateOf(false) }
    var showAddEvaluationModal by remember { mutableStateOf<CircleStudent?>(null) }
    var showSendMessageModal by remember { mutableStateOf(false) }
    var selectedTemplateText by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                // ==========================================
                // رأس: [اسم المعلم] | إشعارات | اليوم: قائمة الحلقات
                // ==========================================
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = teacher.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = EmeraldLight) {
                                    Text(
                                        text = "معلم معتمد",
                                        fontSize = 10.sp,
                                        color = EmeraldDark,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "اليوم: ${rings.size} حلقات مجدولة في ${currentTenant.shortName}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("teacher_portal_back_btn")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        IconButton(onClick = { snackbarMessage = "لديك تنبيهان جديدان من المشرف العام" }) {
                            BadgedBox(badge = { Badge { Text("2") } }) {
                                Icon(imageVector = Icons.Default.Notifications, contentDescription = "إشعارات")
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
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // شارة عزل البيانات وحماية الخصوصية للمهام الممنوحة
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "صلاحية مخصصة: معلم حلقات • تم عزل السجلات المالية وإدارة الحلقات الأخرى لحماية الخصوصية 🛡️",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }

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
                // قائمة الحلقات (بطاقات صغيرة)
                // لكل بطاقة: اسم الحلقة | وقت الانعقاد التالي | زر: فتح الحلقة
                // ==========================================
                Text(
                    text = "حلقاتي المقررة اليوم:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rings.forEach { r ->
                        val isSelected = selectedRing?.id == r.id
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRing = r },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SocietyBlueLight else Color.White
                            ),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) SocietyBluePrimary else BorderSubtle
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = r.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SocietyBluePrimary else TextDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = r.timeSlot, fontSize = 10.sp, color = TextMuted, maxLines = 1)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { selectedRing = r },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) SocietyBluePrimary else Color(0xFFE2E8F0),
                                        contentColor = if (isSelected) Color.White else TextDark
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text(if (isSelected) "الحلقة المفتوحة" else "فتح الحلقة", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // داخل صفحة حلقة (عند الضغط):
                // - قائمة طلاب مختصرة + زر: تسجيل حضور سريع
                // - تبويب: مواد الحلقة (رفع ملف / تسجيل صوتي)
                // - تبويب: تقييمات الطالب (إضافة نتيجة)
                // - زر: طلب غياب / استبدال (يرسل للمشرف)
                // ==========================================
                if (selectedRing != null) {
                    val currentRing = selectedRing!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderSubtle),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // ترويسة الحلقة وإجراء الاستبدال
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = currentRing.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "الموقع: ${currentRing.location} • الموعد: ${currentRing.timeSlot}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                // زر: طلب غياب / استبدال (يرسل للمشرف)
                                OutlinedButton(
                                    onClick = { showSubstituteRequestModal = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldenAmber),
                                    border = BorderStroke(1.dp, GoldenAmber),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("request_substitute_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("طلب استبدال / إجازة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderSubtle)
                            Spacer(modifier = Modifier.height(10.dp))

                            // قائمة الطلاب المختصرة + زر: تسجيل حضور سريع
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الطلاب الحاضرون (${studentsList.size} طلاب):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )

                                // زر: تسجيل حضور سريع
                                Button(
                                    onClick = onOpenAttendance,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("quick_attendance_nav_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسجيل حضور سريع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // قائمة الطلاب مع زر رفع تلاوة وتقييم
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                studentsList.take(6).forEach { student ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(SocietyBluePrimary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = student.name.take(1), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(text = student.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                                    Text(text = "آخر تقييم: ${student.lastEvaluation}", fontSize = 10.sp, color = TextMuted)
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                // زر: رفع تسجيل صوتي وتقييم التلاوة الذكي (القراءات العشر)
                                                FilledTonalButton(
                                                    onClick = { onOpenAudioRecitation(student) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = SocietyBlueLight,
                                                        contentColor = SocietyBluePrimary
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("تقييم التلاوة", fontSize = 11.sp)
                                                }

                                                // زر: إضافة نتيجة تقييم
                                                OutlinedButton(
                                                    onClick = { showAddEvaluationModal = student },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("+ درجة", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // تبويبات الحلقة (مواد الحلقة / تقييمات الطلاب)
                            TabRow(
                                selectedTabIndex = activeSubTab,
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = SocietyBluePrimary
                            ) {
                                Tab(
                                    selected = activeSubTab == 0,
                                    onClick = { activeSubTab = 0 },
                                    text = { Text("مواد الحلقة والمصاحف", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = activeSubTab == 1,
                                    onClick = { activeSubTab = 1 },
                                    text = { Text("سجل التقييمات والنتائج", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (activeSubTab == 0) {
                                // تبويب: مواد الحلقة (رفع ملف / تسجيل صوتي)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = EmeraldLight.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = EmeraldDark)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(text = "المراجعة الذكية للتلاوة (القراءات العشر)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                                                    Text(text = "فحص مخارج الحروف وأحكام التجويد والروايات المعتمدة آلياً", fontSize = 10.sp, color = TextMuted)
                                                }
                                            }
                                            Button(
                                                onClick = { onOpenAudioRecitation(studentsList.first()) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("بدء التحليل", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                // تبويب: تقييمات الطلاب
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    studentsList.take(4).forEach { st ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = st.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "الدرجة: ${st.lastEvaluation} • إتقان التجويد: ممتاز", fontSize = 11.sp, color = SocietyBluePrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // أسفل: لوحة رسائل سريعة للتواصل مع أولياء الأمور (قوائم مخاطبة)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = SocietyBluePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "رسائل سريعة لأولياء الأمور (قوائم مخاطبة)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "إرسال قوالب جاهزة بضغطة زر عبر WhatsApp وSMS",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val templates = listOf(
                            "📢 تذكير: غداً موعد اختبار سورة النبأ المقررة بالحلقة",
                            "🌟 نبارك لكم إتقان ابنكم لحفظ الجزء المطلوب بتقدير ممتاز",
                            "⏰ نأمل الحرص على الحضور في الموعد المحدد بعد صلاة العصر",
                            "📖 تم رفع التلاوة الصوتية والواجب المنزلي عبر المنصة"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            templates.forEach { tmpl ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = tmpl, fontSize = 11.sp, color = TextDark, modifier = Modifier.weight(1f))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                selectedTemplateText = tmpl
                                                showSendMessageModal = true
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("إرسال", fontSize = 10.sp)
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
        // Modal: طلب غياب / استبدال (يرسل للمشرف)
        // ==========================================
        if (showSubstituteRequestModal) {
            var reasonTxt by remember { mutableStateOf("") }
            var substituteTeacherName by remember { mutableStateOf("الشيخ إبراهيم القرني") }
            var dateRangeTxt by remember { mutableStateOf("غداً - الفترة المسائية") }

            AlertDialog(
                onDismissRequest = { showSubstituteRequestModal = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = GoldenAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("طلب استبدال حلقة أو إجازة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("سيتم إرسال الطلب للمشرف العام لاعتماده وإشعار المعلم البديل:", fontSize = 12.sp, color = TextMuted)
                        OutlinedTextField(
                            value = dateRangeTxt,
                            onValueChange = { dateRangeTxt = it },
                            label = { Text("فترة الغياب أو الاستبدال") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = substituteTeacherName,
                            onValueChange = { substituteTeacherName = it },
                            label = { Text("المعلم البديل المقترح") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = reasonTxt,
                            onValueChange = { reasonTxt = it },
                            label = { Text("السبب أو العذر") },
                            placeholder = { Text("أدخل سبب الاعتذار") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            snackbarMessage = "تم إرسال طلب الاستبدال إلى المشرف العام بنجاح!"
                            showSubstituteRequestModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber)
                    ) {
                        Text("إرسال الطلب للمشرف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSubstituteRequestModal = false }) { Text("إلغاء") }
                }
            )
        }

        // ==========================================
        // Modal: إضافة نتيجة تقييم لطالب
        // ==========================================
        if (showAddEvaluationModal != null) {
            val st = showAddEvaluationModal!!
            var scoreVal by remember { mutableStateOf("95") }
            var evalNotes by remember { mutableStateOf("إتقان ممتاز لمخارج الحروف والتجويد") }

            AlertDialog(
                onDismissRequest = { showAddEvaluationModal = null },
                title = { Text("إضافة تقييم للطالب: ${st.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = scoreVal,
                            onValueChange = { scoreVal = it },
                            label = { Text("الدرجة (من 100)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = evalNotes,
                            onValueChange = { evalNotes = it },
                            label = { Text("ملاحظات التقييم") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            studentsList = studentsList.map {
                                if (it.id == st.id) it.copy(lastEvaluation = "$scoreVal%") else it
                            }
                            snackbarMessage = "تم تسجيل تقييم الطالب ${st.name}: $scoreVal% بنجاح!"
                            showAddEvaluationModal = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("حفظ النتيجة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddEvaluationModal = null }) { Text("إلغاء") }
                }
            )
        }

        // ==========================================
        // Modal: إرسال قالب رسالة لأولياء الأمور
        // ==========================================
        if (showSendMessageModal) {
            AlertDialog(
                onDismissRequest = { showSendMessageModal = false },
                title = { Text("إرسال رسالة لأولياء الأمور", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                text = {
                    Column {
                        Text("سيتم إرسال الرسالة إلى أولياء أمور طلاب حلقة ${selectedRing?.name}:", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = SocietyBlueLight)) {
                            Text(text = selectedTemplateText, modifier = Modifier.padding(10.dp), fontSize = 12.sp, color = SocietyBlueDark)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            snackbarMessage = "تم إرسال الرسالة لجميع أولياء الأمور عبر WhatsApp وSMS!"
                            showSendMessageModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("تأكيد البث الآن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSendMessageModal = false }) { Text("إلغاء") }
                }
            )
        }
    }
}
