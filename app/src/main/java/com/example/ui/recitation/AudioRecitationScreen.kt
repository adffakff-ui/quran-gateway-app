package com.example.ui.recitation

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
fun AudioRecitationScreen(
    currentTenant: Tenant,
    student: CircleStudent = SocietyRepository.getInitialCircleStudents().first(),
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val qiraatList = remember { SocietyRepository.getTenQiraat() }
    val allStudents = remember { SocietyRepository.getInitialCircleStudents().take(10) }

    // Form inputs
    var selectedStudent by remember { mutableStateOf(student) }
    var fileType by remember { mutableStateOf("تلاوة واختبار حفظ") } // واجب, تلاوة, اختبار
    var selectedQiraInfo by remember { mutableStateOf(qiraatList.first()) }
    var selectedRiwaya by remember { mutableStateOf(qiraatList.first().riwayat.first()) }
    var useAutoDetect by remember { mutableStateOf(false) }
    var hasReferenceAudio by remember { mutableStateOf(false) }
    var audioQuality by remember { mutableStateOf("عالي (Studio HD)") } // منخفض, عادي, عالي
    var teacherNotes by remember { mutableStateOf("") }

    // Analysis Mode
    var isLiveMode by remember { mutableStateOf(false) } // Toggle: Live vs Batch
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableStateOf(0f) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    // Result State
    var analysisResult by remember {
        mutableStateOf<RecitationAnalysisResult?>(
            SocietyRepository.getSampleRecitationAnalysis(qiraatList.first().name, qiraatList.first().riwayat.first())
        )
    }

    // Dropdown expansion states
    var showStudentDropdown by remember { mutableStateOf(false) }
    var showFileTypeDropdown by remember { mutableStateOf(false) }
    var showQiraDropdown by remember { mutableStateOf(false) }
    var showRiwayaDropdown by remember { mutableStateOf(false) }

    // Dialogs & Feedback
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showAddNoteModal by remember { mutableStateOf(false) }
    var customFeedbackText by remember { mutableStateOf("") }
    var showMismatchWarningDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                // رأس: رفع ملف صوتي للطالب (دعم القراءات العشر)
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "رفع وتقييم التلاوة (القراءات العشر)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Text(
                                text = "مراجعة ذكية فائقة الدقة متوافقة مع أحكام التجويد والروايات",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("recitation_back_btn")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        // شارة القراءة الحالية
                        Badge(containerColor = SocietyBlueLight, modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = if (useAutoDetect) "كشف تلقائي للقراءة" else selectedRiwaya,
                                fontSize = 11.sp,
                                color = SocietyBluePrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(4.dp)
                            )
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

                // Microphone Recorder Card
                MicRecorderCard(
                    onRecordingSaved = { path ->
                        snackbarMessage = "تم حفظ ملف التسجيل الصوتي محلياً بنجاح ($path)"
                    }
                )

                // ==========================================
                // قسم الإدخال وتحديد القراءة والرواية
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "بيانات المقطع القرآني والقراءة المعتمدة:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        // حقل: اختيار الطالب
                        Column {
                            Text(text = "الطالب المراد تقييم تلاوته:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                OutlinedButton(
                                    onClick = { showStudentDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC))
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = selectedStudent.name, fontSize = 13.sp, color = TextDark, modifier = Modifier.weight(1f))
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(expanded = showStudentDropdown, onDismissRequest = { showStudentDropdown = false }) {
                                    allStudents.forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(st.name, fontSize = 12.sp) },
                                            onClick = {
                                                selectedStudent = st
                                                showStudentDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // حقل: نوع الملف (واجب / تلاوة / اختبار)
                        Column {
                            Text(text = "نوع التسجيل المرفوع:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("تلاوة واختبار حفظ", "واجب منزلي", "اختبار فصلي رسمي").forEach { t ->
                                    FilterChip(
                                        selected = fileType == t,
                                        onClick = { fileType = t },
                                        label = { Text(t, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // حقل جديد: اختيار القراءة (Qira'a) [قائمة منسدلة للقراءات العشر]
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "اختيار القراءة والرواية القرآنية:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )

                                // خيار: كشف تلقائي (Auto-detect)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = useAutoDetect,
                                        onCheckedChange = { useAutoDetect = it }
                                    )
                                    Text(text = "كشف تلقائي (Auto-detect)", fontSize = 11.sp, color = SocietyBluePrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!useAutoDetect) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // اختيار القراءة من القراءات العشر
                                    Box(modifier = Modifier.weight(1.2f)) {
                                        OutlinedButton(
                                            onClick = { showQiraDropdown = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(text = selectedQiraInfo.name, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = showQiraDropdown, onDismissRequest = { showQiraDropdown = false }) {
                                            qiraatList.forEach { q ->
                                                DropdownMenuItem(
                                                    text = { Text(q.name, fontSize = 12.sp) },
                                                    onClick = {
                                                        selectedQiraInfo = q
                                                        selectedRiwaya = q.riwayat.first()
                                                        showQiraDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // اختيار الرواية
                                    Box(modifier = Modifier.weight(1.1f)) {
                                        OutlinedButton(
                                            onClick = { showRiwayaDropdown = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(text = selectedRiwaya, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = showRiwayaDropdown, onDismissRequest = { showRiwayaDropdown = false }) {
                                            selectedQiraInfo.riwayat.forEach { rw ->
                                                DropdownMenuItem(
                                                    text = { Text(rw, fontSize = 12.sp) },
                                                    onClick = {
                                                        selectedRiwaya = rw
                                                        showRiwayaDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SocietyBlueLight.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "مفعل: سيقوم محرك ASR الذكي بالتعرف التلقائي على القراءة والرواية عند بدء الاستماع.",
                                        fontSize = 11.sp,
                                        color = SocietyBluePrimary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        // رفع مقطع مرجعي (اختياري)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "رفع مقطع صوتي مرجعي (اختياري)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "استخدام نموذج نطق صحيح للمعلم لمقارنة التلاوة به", fontSize = 10.sp, color = TextMuted)
                            }
                            Switch(checked = hasReferenceAudio, onCheckedChange = { hasReferenceAudio = it })
                        }
                    }
                }

                // ==========================================
                // منطقة المعاينة والموجة الصوتية (Audio Waveform & Player)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "معاينة الملف الصوتي:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "وضع التحليل: ", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = if (isLiveMode) "مباشر (Live)" else "بعد الرفع (Batch)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLiveMode) StatusSuccess else SocietyBluePrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = isLiveMode,
                                    onCheckedChange = { isLiveMode = it }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // الموجة الصوتية البصرية (Audio Waveform Visualization)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val barHeights = listOf(14, 28, 42, 20, 35, 48, 52, 38, 22, 45, 50, 33, 18, 40, 46, 28, 36, 52, 24, 18)
                                barHeights.forEachIndexed { idx, h ->
                                    val isHighlightedError = idx in 6..8
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (isHighlightedError) StatusWarning
                                                else if (isPlayingPreview) EmeraldGreen
                                                else SocietyBluePrimary
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // أزرار ومشغل الصوت المدمج المتطور
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(SocietyBluePrimary)
                                            .clickable { isPlayingPreview = !isPlayingPreview },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "تشغيل",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isPlayingPreview) "جاري تشغيل التلاوة..." else "متوقف مؤقتاً",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                        Text(
                                            text = if (isPlayingPreview) "00:24 / 01:15" else "00:00 / 01:15",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                var currentSpeed by remember { mutableStateOf("1.0x") }
                                OutlinedButton(
                                    onClick = {
                                        currentSpeed = when (currentSpeed) {
                                            "1.0x" -> "1.25x"
                                            "1.25x" -> "1.5x"
                                            else -> "1.0x"
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(currentSpeed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            var sliderPos by remember { mutableStateOf(0.32f) }
                            Slider(
                                value = sliderPos,
                                onValueChange = { sliderPos = it },
                                modifier = Modifier.fillMaxWidth().height(24.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = SocietyBluePrimary,
                                    activeTrackColor = SocietyBluePrimary
                                )
                            )

                            // زر بدء التحليل بالذكاء الاصطناعي
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isAnalyzing = true
                                        analysisProgress = 0.2f
                                        delay(400)
                                        analysisProgress = 0.6f
                                        delay(400)
                                        analysisProgress = 1.0f
                                        isAnalyzing = false
                                        analysisResult = SocietyRepository.getSampleRecitationAnalysis(selectedQiraInfo.name, selectedRiwaya)
                                        snackbarMessage = "تم اكتمال التحليل الدقيق للتلاوة بنجاح بنسبة 88%!"
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                modifier = Modifier.fillMaxWidth().testTag("start_recitation_analysis_btn")
                            ) {
                                Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فحص التلاوة بالذكاء الاصطناعي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isAnalyzing) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { analysisProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = EmeraldGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "جاري التحليل ومطابقة القواعد مع رواية $selectedRiwaya...",
                                fontSize = 11.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ==========================================
                // لوحة نتائج التحليل الذكي للتلاوة
                // ==========================================
                if (analysisResult != null) {
                    val result = analysisResult!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderSubtle),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = EmeraldDark)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تقرير الفحص التجويدي المتقدم",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                }

                                Badge(containerColor = EmeraldLight) {
                                    Text(
                                        text = "الدرجة الكلية: ${(result.scoreAdjustedForQira * 100).toInt()}%",
                                        color = EmeraldDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }

                            // بيانات القراءة المكتشفة والمطابقة
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "القراءة المعتمدة:", fontSize = 11.sp, color = TextMuted)
                                        Text(text = "${result.selectedQira} - ${result.selectedRiwaya}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "القراءة المكتشفة صوتياً:", fontSize = 11.sp, color = TextMuted)
                                        Text(text = "${result.detectedQira ?: "مطابقة للرواية"} (ثقة ${(result.detectedConfidence * 100).toInt()}%)", fontSize = 11.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "المستوى التقديري:", fontSize = 11.sp, color = TextMuted)
                                        Text(text = result.overallLevel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SocietyBluePrimary)
                                    }
                                }
                            }

                            HorizontalDivider(color = BorderSubtle)

                            Text(
                                text = "ملاحظات وتنبيهات الآيات ومخارج الحروف:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )

                            // المقاطع والتنبيهات
                            result.segments.forEach { seg ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldenAmber.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = seg.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                            Badge(containerColor = GoldenAmberLight) {
                                                Text(text = seg.errorType, fontSize = 10.sp, color = GoldenAmber, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "المطلوب برواية ${result.selectedRiwaya}: ${seg.expectedBySelectedQira}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                        Text(text = "التوجيه: ${seg.suggestion}", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // ==========================================
                            // أزرار المتابعة (المحدثة):
                            // - قبول النتيجة حسب القراءة
                            // - إعادة التحليل وفق قراءة أخرى
                            // - رفض / طلب إعادة
                            // - أضف ملاحظة صوتية / تعليق نصي
                            // ==========================================
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // قبول النتيجة حسب القراءة
                                Button(
                                    onClick = {
                                        snackbarMessage = "تم قبول النتيجة واعتماد ${(result.scoreAdjustedForQira * 100).toInt()}% برواية ${result.selectedRiwaya} في سجل الطالب!"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("قبول النتيجة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // إعادة التحليل وفق قراءة أخرى
                                OutlinedButton(
                                    onClick = { showMismatchWarningDialog = true },
                                    modifier = Modifier.weight(1.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SocietyBluePrimary),
                                    border = BorderStroke(1.dp, SocietyBluePrimary),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إعادة برواية أخرى", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // رفض / طلب إعادة
                                OutlinedButton(
                                    onClick = {
                                        snackbarMessage = "تم إرسال إشعار لولي الأمر والطالب بطلب إعادة التسميع مع التوجيهات"
                                    },
                                    modifier = Modifier.weight(0.9f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    border = BorderStroke(1.dp, StatusError),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("طلب إعادة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // أضف ملاحظة صوتية
                                FilledTonalButton(
                                    onClick = { snackbarMessage = "تم تسجيل ملاحظة صوتية للمعلم وإرفاقها للتقرير" },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ملاحظة صوتية", fontSize = 11.sp)
                                }

                                // أضف تعليق نصي
                                FilledTonalButton(
                                    onClick = { showAddNoteModal = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعليق نصي", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // معرض ملاحظات المعلم التفاعلي (Teacher Feedback Gallery)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.SpeakerNotes, contentDescription = null, tint = SocietyBluePrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "معرض ملاحظات وتوجيهات المعلمين",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Badge(containerColor = SocietyBlueLight) {
                                Text(text = "ملاحظتان نشطتان", color = SocietyBluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                            }
                        }

                        val feedbackList = remember {
                            listOf(
                                Triple("الشيخ د. عبدالكريم السلمي", "احرص على إتمام المد العارض للسكون وتحقيق الغنة في النون المشددة.", "0:45"),
                                Triple("الأستاذ محمد القاضي", "أداء طيب وممتاز، يرجى مراعاة تفخيم الراء في كلمة (القرآن).", "0:30")
                            )
                        }

                        feedbackList.forEach { (teacher, note, duration) ->
                            var isPlayingVoice by remember { mutableStateOf(false) }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(SocietyBluePrimary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = teacher, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        }
                                        Badge(containerColor = EmeraldLight) {
                                            Text(text = "ملاحظة صوتية ونصحية", color = EmeraldDark, fontSize = 10.sp)
                                        }
                                    }

                                    Text(text = note, fontSize = 12.sp, color = TextDark)

                                    // مشغل الملاحظة الصوتية المدمج
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SocietyBlueLight.copy(alpha = 0.4f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { isPlayingVoice = !isPlayingVoice },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlayingVoice) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = SocietyBluePrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isPlayingVoice) "جاري الاستماع $duration..." else "تسجيل صوتي توجيهي $duration",
                                                fontSize = 11.sp,
                                                color = SocietyBlueDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // Modal: إعادة التحليل وفق قراءة أخرى
        // ==========================================
        if (showMismatchWarningDialog) {
            AlertDialog(
                onDismissRequest = { showMismatchWarningDialog = false },
                title = { Text("إعادة التحليل برواية بديلة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text("يمكنك إعادة تشغيل فحص الذكاء الاصطناعي السريع أو العميق بمقارنة التلاوة مع رواية ورش عن نافع أو رواية الدوري عن أبي عمرو.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedRiwaya = "ورش عن نافع"
                            analysisResult = SocietyRepository.getSampleRecitationAnalysis(selectedQiraInfo.name, "ورش عن نافع")
                            snackbarMessage = "تمت إعادة تحليل المقطع القرآني وفق رواية ورش عن نافع!"
                            showMismatchWarningDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("تحليل برواية ورش")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMismatchWarningDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // ==========================================
        // Modal: كتابة تعليق نصي للمعلم
        // ==========================================
        if (showAddNoteModal) {
            AlertDialog(
                onDismissRequest = { showAddNoteModal = false },
                title = { Text("إضافة تعليق المعلم", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column {
                        Text("اكتب تعليقك التوجيهي ليظهر في إشعار الطالب وولي أمره:", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customFeedbackText,
                            onValueChange = { customFeedbackText = it },
                            placeholder = { Text("اكتب الملاحظات والتوجيهات هنا...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            snackbarMessage = "تم حفظ التعليق وإرساله لولي الأمر"
                            showAddNoteModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("إرسال التعليق")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddNoteModal = false }) { Text("إلغاء") }
                }
            )
        }
    }
}
