package com.example.ui.attendance

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
fun AttendanceScreen(
    currentTenant: Tenant,
    onBack: () -> Unit
) {
    val rings = remember { SocietyRepository.getInitialRings() }
    var selectedRing by remember { mutableStateOf(rings.firstOrNull()) }
    var selectedDate by remember { mutableStateOf("الأربعاء، 16 أكتوبر 2026") }
    var studentsAttendance by remember { mutableStateOf(SocietyRepository.getInitialCircleStudents().take(25)) }

    // Dropdown states
    var isRingMenuExpanded by remember { mutableStateOf(false) }

    // Dialogs
    var noteStudentTarget by remember { mutableStateOf<CircleStudent?>(null) }
    var studentNoteText by remember { mutableStateOf("") }
    var showGroupNoteModal by remember { mutableStateOf(false) }
    var groupNoteText by remember { mutableStateOf("") }
    var showQrScannerModal by remember { mutableStateOf(false) }
    var showExportSuccessModal by remember { mutableStateOf(false) }
    var showAbsenteesNoticeModal by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Attendance stats
    val presentCount = studentsAttendance.count { it.attendanceStatus == AttendanceStatus.PRESENT }
    val absentCount = studentsAttendance.count { it.attendanceStatus == AttendanceStatus.ABSENT }
    val lateCount = studentsAttendance.count { it.attendanceStatus == AttendanceStatus.LATE }
    val excusedCount = studentsAttendance.count { it.attendanceStatus == AttendanceStatus.EXCUSED }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                // ==========================================
                // رأس الصفحة:
                // اختيار حلقة [قائمة منسدلة] | تاريخ: [تاريخ اليوم] | زر: حفظ الكل
                // ==========================================
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // اختيار الحلقة منسدلة
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { isRingMenuExpanded = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = SocietyBlueLight.copy(alpha = 0.5f),
                                            contentColor = TextDark
                                        ),
                                        border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("attendance_ring_dropdown")
                                    ) {
                                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = selectedRing?.name ?: "لا توجد حلقات",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                                    }

                                    DropdownMenu(
                                        expanded = isRingMenuExpanded,
                                        onDismissRequest = { isRingMenuExpanded = false }
                                    ) {
                                        rings.forEach { r ->
                                            DropdownMenuItem(
                                                text = { Text(r.name, fontSize = 12.sp) },
                                                onClick = {
                                                    selectedRing = r
                                                    isRingMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // التاريخ
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = selectedDate, fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("attendance_back_btn")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        // زر: حفظ الكل
                        Button(
                            onClick = {
                                snackbarMessage = "تم حفظ سجل الحضور لـ ${studentsAttendance.size} طالباً بنجاح وتحديث قاعدة البيانات!"
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                            modifier = Modifier
                                .testTag("save_all_attendance_btn")
                                .padding(end = 8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                // ==========================================
                // شريط أدوات (أسفل / ثابت):
                // - زر: إرسال إشعار للغائبين
                // - زر: تصدير CSV
                // - زر: إضافة ملاحظة جماعية
                // - زر: مسح QR للحضور
                // ==========================================
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // مسح QR للحضور
                            Button(
                                onClick = { showQrScannerModal = true },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("scan_qr_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مسح QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // إرسال إشعار للغائبين
                            OutlinedButton(
                                onClick = {
                                    if (absentCount > 0) {
                                        showAbsenteesNoticeModal = true
                                    } else {
                                        snackbarMessage = "لا يوجد طلاب غائبون اليوم في هذه الحلقة"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("notify_absentees_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.NotificationImportant, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إشعار الغياب ($absentCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // إضافة ملاحظة جماعية
                            OutlinedButton(
                                onClick = { showGroupNoteModal = true },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("group_note_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SocietyBluePrimary),
                                border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.NoteAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ملاحظة جماعية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // تصدير CSV
                            OutlinedButton(
                                onClick = { showExportSuccessModal = true },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("export_csv_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                                border = BorderStroke(1.dp, BorderSubtle),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            containerColor = SlateBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (snackbarMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SocietyBlueLight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
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

                // شريط إحصائي سريع للحضور اليومي
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AttendanceStatPill("حاضر: $presentCount", StatusSuccess, StatusSuccess.copy(alpha = 0.12f))
                        AttendanceStatPill("غائب: $absentCount", StatusError, StatusError.copy(alpha = 0.12f))
                        AttendanceStatPill("متأخر: $lateCount", GoldenAmber, GoldenAmberLight)
                        AttendanceStatPill("مأذون: $excusedCount", SocietyBluePrimary, SocietyBlueLight)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // قائمة الطلاب (قائمة طولية)
                // لكل صف: صورة | اسم الطالب | حالة: [حاضر ✓] [غائب ✖] [متأخر ⏰] [مأذون 📝] | ملاحظة
                // اختيارات سريعة: حضور يومي باللمس / نقرة
                // ==========================================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(studentsAttendance, key = { it.id }) { student ->
                        AttendanceStudentRow(
                            student = student,
                            onStatusChanged = { newStatus ->
                                studentsAttendance = studentsAttendance.map {
                                    if (it.id == student.id) it.copy(attendanceStatus = newStatus)
                                    else it
                                }
                            },
                            onNoteClick = {
                                noteStudentTarget = student
                                studentNoteText = student.note
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // Modal: تدوين ملاحظة للطالب
        // ==========================================
        if (noteStudentTarget != null) {
            val student = noteStudentTarget!!
            AlertDialog(
                onDismissRequest = { noteStudentTarget = null },
                title = { Text("ملاحظة حضور للطالب: ${student.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("سجل ملاحظة المعلم حول الحضور أو الاستئذان أو التسميع:", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = studentNoteText,
                            onValueChange = { studentNoteText = it },
                            placeholder = { Text("مثال: تأخر لعذر مواصلات / أتقن التسميع اليوم") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            studentsAttendance = studentsAttendance.map {
                                if (it.id == student.id) it.copy(note = studentNoteText) else it
                            }
                            snackbarMessage = "تم حفظ الملاحظة للطالب ${student.name}"
                            noteStudentTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteStudentTarget = null }) { Text("إلغاء") }
                }
            )
        }

        // ==========================================
        // Modal: ملاحظة جماعية للحلقة
        // ==========================================
        if (showGroupNoteModal) {
            AlertDialog(
                onDismissRequest = { showGroupNoteModal = false },
                title = { Text("إضافة ملاحظة جماعية للحلقة", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("هذه الملاحظة ستسجل في تقرير اليوم لجميع طلاب حلقة ${selectedRing?.name ?: ""}:", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = groupNoteText,
                            onValueChange = { groupNoteText = it },
                            placeholder = { Text("اكتب الملاحظة الجماعية (مثال: تم عقد مراجعة مكثفة لجزء عمّ)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            snackbarMessage = "تم حفظ الملاحظة الجماعية لتقرير الحلقة!"
                            showGroupNoteModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("حفظ وتثبيت")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGroupNoteModal = false }) { Text("إلغاء") }
                }
            )
        }

        // ==========================================
        // Modal: إشعار أولياء أمور الغائبين
        // ==========================================
        if (showAbsenteesNoticeModal) {
            val absentees = studentsAttendance.filter { it.attendanceStatus == AttendanceStatus.ABSENT }
            AlertDialog(
                onDismissRequest = { showAbsenteesNoticeModal = false },
                title = { Text("إرسال إشعار غياب فوري (${absentees.size} طلاب)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column {
                        Text(
                            text = "سيتم إرسال رسائل SMS وتنبيهات عبر التطبيق لأولياء أمور الطلاب الغائبين في حلقة ${selectedRing?.name ?: ""}:",
                            fontSize = 12.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        absentees.take(5).forEach { st ->
                            Text("• ${st.name} (ولي الأمر: ${st.guardianPhone})", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            snackbarMessage = "تم إرسال إشعارات الغياب بنجاح إلى ${absentees.size} من أولياء الأمور!"
                            showAbsenteesNoticeModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Text("إرسال الإشعارات الآن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAbsenteesNoticeModal = false }) { Text("إلغاء") }
                }
            )
        }

        // ==========================================
        // Modal: محاكاة مسح QR للحضور
        // ==========================================
        if (showQrScannerModal) {
            AlertDialog(
                onDismissRequest = { showQrScannerModal = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = EmeraldDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ماسح QR للحضور الذكي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("وجّه الكاميرا نحو بطاقة أو باركود الطالب للتسجيل الفوري:", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(14.dp))

                        // محاكاة واجهة الماسح الضوئي (Scanner HUD)
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = EmeraldGreen.copy(alpha = 0.8f),
                                modifier = Modifier.size(100.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(EmeraldGreen)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "نظام الحضور الذكي معتمد بالربط مع بطاقات الطلاب",
                            fontSize = 11.sp,
                            color = EmeraldDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // محاكاة تحضير طالب عشوائي
                            val unverified = studentsAttendance.firstOrNull { it.attendanceStatus != AttendanceStatus.PRESENT }
                            if (unverified != null) {
                                studentsAttendance = studentsAttendance.map {
                                    if (it.id == unverified.id) it.copy(attendanceStatus = AttendanceStatus.PRESENT, note = "حضور عبر بطاقة QR الذكية")
                                    else it
                                }
                                snackbarMessage = "تم تسجيل حضور الطالب ${unverified.name} بنجاح عبر QR!"
                            }
                            showQrScannerModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                    ) {
                        Text("محاكاة مسح بطاقة طالب")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQrScannerModal = false }) { Text("إغلاق") }
                }
            )
        }

        // ==========================================
        // Modal: نجاح تصدير CSV
        // ==========================================
        if (showExportSuccessModal) {
            AlertDialog(
                onDismissRequest = { showExportSuccessModal = false },
                title = { Text("تصدير ملف الحضور CSV", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text(
                        text = "تم إنشاء ملف الحضور بنجاح بصيغة CSV جاهز للفتح في Excel متضمناً حالة كل طالب وملاحظات اليوم وتوقيتات الحضور.",
                        fontSize = 12.sp,
                        color = TextDark
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            snackbarMessage = "تم تنزيل كشف الحضور: attendance_${selectedRing?.id ?: ""}_20261016.csv"
                            showExportSuccessModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("تنزيل الكشف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportSuccessModal = false }) { Text("إغلاق") }
                }
            )
        }
    }
}

// ==========================================
// صف الطالب في سجل الحضور (باللمس السريع)
// ==========================================
@Composable
fun AttendanceStudentRow(
    student: CircleStudent,
    onStatusChanged: (AttendanceStatus) -> Unit,
    onNoteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // صورة واسم الطالب
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SocietyBluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.name.take(1),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = student.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ولي الأمر: ${student.guardianName}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                // أيقونة الملاحظة
                IconButton(onClick = onNoteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (student.note.isNotBlank()) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "ملاحظة",
                        tint = if (student.note.isNotBlank()) SocietyBluePrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (student.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ملاحظة: ${student.note}",
                    fontSize = 10.sp,
                    color = SocietyBluePrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // اختيارات سريعة: [حاضر ✓] [غائب ✖] [متأخر ⏰] [مأذون 📝]
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttendanceToggleButton(
                    modifier = Modifier.weight(1f),
                    label = "حاضر ✓",
                    isSelected = student.attendanceStatus == AttendanceStatus.PRESENT,
                    activeColor = StatusSuccess,
                    onClick = { onStatusChanged(AttendanceStatus.PRESENT) }
                )
                AttendanceToggleButton(
                    modifier = Modifier.weight(1f),
                    label = "غائب ✖",
                    isSelected = student.attendanceStatus == AttendanceStatus.ABSENT,
                    activeColor = StatusError,
                    onClick = { onStatusChanged(AttendanceStatus.ABSENT) }
                )
                AttendanceToggleButton(
                    modifier = Modifier.weight(1f),
                    label = "متأخر ⏰",
                    isSelected = student.attendanceStatus == AttendanceStatus.LATE,
                    activeColor = GoldenAmber,
                    onClick = { onStatusChanged(AttendanceStatus.LATE) }
                )
                AttendanceToggleButton(
                    modifier = Modifier.weight(1f),
                    label = "مأذون 📝",
                    isSelected = student.attendanceStatus == AttendanceStatus.EXCUSED,
                    activeColor = SocietyBluePrimary,
                    onClick = { onStatusChanged(AttendanceStatus.EXCUSED) }
                )
            }
        }
    }
}

@Composable
fun AttendanceToggleButton(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeColor else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextDark,
            maxLines = 1
        )
    }
}

@Composable
fun AttendanceStatPill(text: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}
