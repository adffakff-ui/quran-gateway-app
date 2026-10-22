package com.example.ui.parent

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPortalScreen(
    currentTenant: Tenant,
    student: CircleStudent = SocietyRepository.getInitialCircleStudents().first(),
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var recordingsList by remember { mutableStateOf(SocietyRepository.getInitialParentRecordings()) }
    var privacyStatus by remember { mutableStateOf("خاص للمعلم") } // خاص للمعلم, مشترك مع ولي الأمر الآخر, مشاهد للعامة
    var showUploadModal by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                // ==========================================
                // رأس: اسم الطالب | صورة | مستوى
                // - زر بارز: "رفع تسجيل اختباري" (Parent Upload)
                // - حالة خصوصية التسجيل: [خاص للمعلم] / [مشترك] / [مشاهد للعامة]
                // ==========================================
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SocietyBluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = student.name.take(1),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = student.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "المستوى: متوسط • الحلقة: حلقة الإمام عاصم",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("parent_portal_back_btn")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        // زر بارز: "رفع تسجيل اختباري" (Parent Upload)
                        Button(
                            onClick = { showUploadModal = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                            modifier = Modifier
                                .testTag("parent_upload_recording_btn")
                                .padding(end = 8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("رفع تسجيل اختباري", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                // شارة عزل البيانات وحماية الخصوصية لولي الأمر
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF5F3FF),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "صلاحية مخصصة: ولي أمر • بيانات باقي الطلاب وسجلات الجمعية معزولة تماماً لحماية الخصوصية 🛡️",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5B21B6)
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
                // شريط خصوصية التسجيلات والمنهج
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "حالة خصوصية التسجيلات:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("خاص للمعلم", "مشترك مع ولي الأمر الآخر", "مشاهد للعامة").forEach { priv ->
                                    FilterChip(
                                        selected = privacyStatus == priv,
                                        onClick = {
                                            privacyStatus = priv
                                            snackbarMessage = "تم تحديث خصوصية التسجيلات إلى: $priv"
                                        },
                                        label = { Text(priv, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "المقرر القرآني الحالي: حفظ سورة النبأ والنازعات • خطة المنهج: الجزء 30",
                            fontSize = 11.sp,
                            color = SocietyBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ==========================================
                // بطاقة ترويجية لرفع التسجيل الصوتي
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SocietyBlueLight),
                    border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SocietyBluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "تسميع واختبار قرآني منزلي",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "تسجيل التلاوة مع فحص سريع فوري وإرسال التقرير للمعلم",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Button(
                            onClick = { showUploadModal = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("تسجيل الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ==========================================
                // قائمة تسجيلات ولي الأمر وحالات المراجعة
                // ==========================================
                Text(
                    text = "سجل تسجيلات وتسميع الطالب (${recordingsList.size}):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recordingsList.forEach { rec ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${rec.surahName} (${rec.ayahRange})",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "القراءة: ${rec.qiraaName} • ${rec.mode}",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }

                                    // شارة الحالة
                                    Badge(
                                        containerColor = if (rec.status == "accepted") EmeraldLight else GoldenAmberLight
                                    ) {
                                        Text(
                                            text = if (rec.status == "accepted") "معتمد من المعلم" else "بانتظار مراجعة المعلم",
                                            fontSize = 10.sp,
                                            color = if (rec.status == "accepted") EmeraldDark else GoldenAmber,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // نتيجة Quick-Feedback الفورية
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "التقييم السريع الذكي (Quick Scan): ${rec.quickScore}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SocietyBluePrimary
                                            )
                                        }
                                        Text(
                                            text = rec.quickFeedback,
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                // تعليق المعلم إن وجد
                                if (rec.teacherFeedback != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = EmeraldLight.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Filled.ChatBubble, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "ملاحظة المعلم: ${rec.teacherFeedback}",
                                                fontSize = 11.sp,
                                                color = EmeraldDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المدة: ${rec.durationSeconds} ثانية • ${rec.uploadedAt}",
                                        fontSize = 10.sp,
                                        color = TextSubtle
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilledTonalIconButton(onClick = { snackbarMessage = "تشغيل التسجيل الصوتي..." }, modifier = Modifier.size(28.dp)) {
                                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "تشغيل", modifier = Modifier.size(16.dp))
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
        // Modal: رفع التسجيل الصوتي لولي الأمر (Parent Recording Upload)
        // - نوع الإرسال: اختبار منزلي (Practice) / تسليم رسمي (Submission)
        // - تحديد السورة والآيات
        // - اختيار القراءة والرواية
        // - محاكي التسجيل المباشر (Record / Pause / Stop)
        // - تنبيهات بصرية: "اقرأ بوضوح وتجنب الضوضاء"
        // ==========================================
        if (showUploadModal) {
            var uploadMode by remember { mutableStateOf("اختبار منزلي (Practice)") }
            var surahName by remember { mutableStateOf("سورة النبأ") }
            var startAyah by remember { mutableStateOf("1") }
            var endAyah by remember { mutableStateOf("20") }
            var selectedQiraa by remember { mutableStateOf("حفص عن عاصم") }
            var isRecordingActive by remember { mutableStateOf(false) }
            var recordingDuration by remember { mutableStateOf(0) }
            var parentNote by remember { mutableStateOf("") }
            var notifyTeacherCheck by remember { mutableStateOf(true) }
            var autoQuickScanCheck by remember { mutableStateOf(true) }

            AlertDialog(
                onDismissRequest = { showUploadModal = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = SocietyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("رفع تسجيل قرآني للطالب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // حقل: نوع الإرسال
                        Text(text = "نوع الإرسال:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("اختبار منزلي (Practice)", "تسليم رسمي (Submission)").forEach { m ->
                                FilterChip(
                                    selected = uploadMode == m,
                                    onClick = { uploadMode = m },
                                    label = { Text(m, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // حقل: تحديد النص / المقطع القرآني
                        Text(text = "تحديد المقطع القرآني:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = surahName,
                            onValueChange = { surahName = it },
                            label = { Text("اسم السورة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startAyah,
                                onValueChange = { startAyah = it },
                                label = { Text("من آية") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = endAyah,
                                onValueChange = { endAyah = it },
                                label = { Text("إلى آية") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        // حقل: اختيار القراءة / الرواية
                        Text(text = "القراءة والرواية المطلوبة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = selectedQiraa,
                            onValueChange = { selectedQiraa = it },
                            label = { Text("القراءة والرواية") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // منطقة التسجيل المباشر
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isRecordingActive) "🔴 جاري التسجيل الآن..." else "جاهز لتسجيل صوت الطالب",
                                    color = if (isRecordingActive) StatusError else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = {
                                            isRecordingActive = !isRecordingActive
                                            if (isRecordingActive) recordingDuration = 35
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = if (isRecordingActive) StatusError else SocietyBluePrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isRecordingActive) Icons.Default.Stop else Icons.Default.Mic,
                                            contentDescription = "تسجيل",
                                            tint = Color.White
                                        )
                                    }

                                    Text(
                                        text = if (isRecordingActive) "00:35" else "00:00",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💡 نصيحة: اقرأ بوضوح وتجنّب الضوضاء المحيطة لضمان دقة التحليل",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // ملاحظة لولي الأمر
                        OutlinedTextField(
                            value = parentNote,
                            onValueChange = { parentNote = it },
                            label = { Text("ملاحظة نصية للمعلم (اختياري)") },
                            placeholder = { Text("أدخل الملاحظة") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        // خيارات المشاركة
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = notifyTeacherCheck, onCheckedChange = { notifyTeacherCheck = it })
                            Text(text = "إشعار المعلم فوراً عند إتمام الرفع", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = autoQuickScanCheck, onCheckedChange = { autoQuickScanCheck = it })
                            Text(text = "إجراء فحص سريع فوري (Quick Scan)", fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newRec = ParentRecordingItem(
                                id = "prec_${System.currentTimeMillis()}",
                                studentName = student.name,
                                surahName = surahName,
                                ayahRange = "الآيات $startAyah - $endAyah",
                                qiraaName = selectedQiraa,
                                mode = uploadMode,
                                privacy = privacyStatus,
                                durationSeconds = if (recordingDuration > 0) recordingDuration else 45,
                                status = "quick_done",
                                quickScore = "89%",
                                quickFeedback = "أداء ممتاز مع إتقان لمخارج الحروف الأساسية",
                                teacherFeedback = null,
                                uploadedAt = "الآن"
                            )
                            recordingsList = listOf(newRec) + recordingsList
                            snackbarMessage = "تم رفع التسجيل بنجاح وإنجاز الفحص السريع الفوري بنسبة 89%!"
                            showUploadModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("رفع وإرسال التلاوة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUploadModal = false }) { Text("إلغاء") }
                }
            )
        }
    }
}
