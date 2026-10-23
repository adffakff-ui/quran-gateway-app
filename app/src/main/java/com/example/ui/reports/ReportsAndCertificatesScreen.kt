package com.example.ui.reports

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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAndCertificatesScreen(
    currentTenant: Tenant,
    onBack: () -> Unit
) {
    val rings = remember { SocietyRepository.getInitialRings() }
    var selectedRingFilter by remember { mutableStateOf("جميع الحلقات") }
    var selectedLevelFilter by remember { mutableStateOf("جميع المستويات") }
    var datePeriodFilter by remember { mutableStateOf("الشهر الحالي (أكتوبر 2026)") }

    // Section sub-tabs
    var activeReportTab by remember { mutableStateOf(0) } // 0: تقرير الحضور, 1: تقرير التقدم, 2: توليد الشهادات, 3: سجل التصدير

    // State data
    var eligibleStudents by remember { mutableStateOf(SocietyRepository.getInitialEligibleStudents()) }
    var exportLogs by remember { mutableStateOf(SocietyRepository.getInitialExportLogs()) }
    var certificateTemplate by remember { mutableStateOf(CertificateTemplate()) }

    // Dialogs
    var showTemplateEditorModal by remember { mutableStateOf(false) }
    var showCertificatePreviewModal by remember { mutableStateOf<EligibleCertificateStudent?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                // رأس: تصدير تقارير | فلتر الفترة، الحلقة، المستوى
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "التقارير وتوليد الشهادات",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Text(
                                text = "إدارة التقارير الإحصائية وإصدار شهادات الحفظ في ${currentTenant.shortName}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("reports_back_btn")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                snackbarMessage = "تم تجهيز وتصدير التقرير الشامل بصيغة PDF بنجاح!"
                                exportLogs = listOf(
                                    ExportLogRecord("exp_${System.currentTimeMillis()}", "التقرير الشامل للحلقات والشهادات", "الآن", "المشرف العام", 724, "PDF")
                                ) + exportLogs
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                            modifier = Modifier.padding(end = 8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير تقرير", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                // ==========================================
                // شريط الفلاتر العامة: الفترة، الحلقة، المستوى
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "الفلاتر:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)

                        FilterChip(
                            selected = true,
                            onClick = {
                                datePeriodFilter = when (datePeriodFilter) {
                                    "الشهر الحالي (أكتوبر 2026)" -> "الأسبوع الحالي"
                                    "الأسبوع الحالي" -> "الفصل الدراسي الحالي"
                                    else -> "الشهر الحالي (أكتوبر 2026)"
                                }
                            },
                            label = { Text(datePeriodFilter, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )

                        FilterChip(
                            selected = true,
                            onClick = {
                                val ringNames = listOf("جميع الحلقات") + rings.map { it.name }
                                val nextIdx = (ringNames.indexOf(selectedRingFilter) + 1) % ringNames.size
                                selectedRingFilter = ringNames[nextIdx]
                            },
                            label = { Text(selectedRingFilter, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )

                        FilterChip(
                            selected = true,
                            onClick = {
                                selectedLevelFilter = when (selectedLevelFilter) {
                                    "جميع المستويات" -> "مستوى المبتدئين"
                                    "مستوى المبتدئين" -> "مستوى المتوسط"
                                    "مستوى المتوسط" -> "مستوى الإتقان والخاتمين"
                                    else -> "جميع المستويات"
                                }
                            },
                            label = { Text(selectedLevelFilter, fontSize = 11.sp) }
                        )
                    }
                }

                // ==========================================
                // التبويبات الأربعة للأقسام:
                // 1. تقرير الحضور
                // 2. تقرير التقدم
                // 3. توليد الشهادات
                // 4. سجل التصدير
                // ==========================================
                TabRow(
                    selectedTabIndex = activeReportTab,
                    containerColor = Color.White,
                    contentColor = SocietyBluePrimary
                ) {
                    Tab(
                        selected = activeReportTab == 0,
                        onClick = { activeReportTab = 0 },
                        text = { Text("تقرير الحضور", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeReportTab == 1,
                        onClick = { activeReportTab = 1 },
                        text = { Text("تقرير التقدم", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeReportTab == 2,
                        onClick = { activeReportTab = 2 },
                        text = { Text("توليد الشهادات", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeReportTab == 3,
                        onClick = { activeReportTab = 3 },
                        text = { Text("سجل التصدير", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                when (activeReportTab) {
                    // ==========================================
                    // قسم: تقرير الحضور (عرض جدول + تنزيل CSV/PDF)
                    // ==========================================
                    0 -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "جدول حضور الحلقات الشهري:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { snackbarMessage = "تم تنزيل كشف الحضور بصيغة CSV بنجاح" },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("تنزيل CSV", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { snackbarMessage = "تم تجهيز وتنزيل تقرير الحضور PDF بنجاح" },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("تنزيل PDF", fontSize = 11.sp)
                                        }
                                    }
                                }

                                rings.forEach { r ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = r.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(text = "المعلم: ${r.teacherName} • المسجلون: ${r.studentCount}", fontSize = 10.sp, color = TextMuted)
                                            }
                                            Text(text = "نسبة الحضور: ${r.attendancePercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // قسم: تقرير التقدم (مخططات + فلترة للطالب / الحلقة)
                    // ==========================================
                    1 -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = "معدل تقدم الحفظ والإتقان السنوي:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)

                                val progressItems = listOf(
                                    Pair("الخاتمون لكتاب الله كاملاً", 42),
                                    Pair("مجتازو اختبارات الأجزاء العشرين", 98),
                                    Pair("مجتازو اختبارات جزء عمّ وتبارك", 312),
                                    Pair("طلاب مرحلة التأسيس والقاعدة النورانية", 272)
                                )

                                progressItems.forEach { (label, count) ->
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = label, fontSize = 11.sp, color = TextDark)
                                            Text(text = "$count طالباً", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SocietyBluePrimary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { (count / 350f).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = SocietyBluePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // قسم: توليد شهادات
                    // - قائمة طلاب مستوفين للشروط
                    // - خيار: إنشاء شهادات فردية / مجمعة (PDF)
                    // - قالب الشهادة: تحرير (نص، شعار، تاريخ)
                    // ==========================================
                    2 -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "الطلاب المستوفون لشروط منح الشهادة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Text(text = "تم فحص درجات الاختبار وسجلات الحضور بنجاح", fontSize = 10.sp, color = TextMuted)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // زر تحرير قالب الشهادة
                                        OutlinedButton(
                                            onClick = { showTemplateEditorModal = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تحرير القالب", fontSize = 11.sp)
                                        }

                                        // إنشاء مجمعة
                                        Button(
                                            onClick = {
                                                val selectedCount = eligibleStudents.count { it.isSelected }
                                                snackbarMessage = "تم توليد وتصدير $selectedCount شهادة معتمدة مجمعة بصيغة PDF!"
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إنشاء مجمعة (PDF)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                HorizontalDivider(color = BorderSubtle)

                                eligibleStudents.forEach { st ->
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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = st.isSelected,
                                                    onCheckedChange = { chk ->
                                                        eligibleStudents = eligibleStudents.map {
                                                            if (it.id == st.id) it.copy(isSelected = chk == true) else it
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Column {
                                                    Text(text = st.studentName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                                    Text(text = "${st.ringName} • المقرر: ${st.level}", fontSize = 11.sp, color = TextMuted)
                                                    Text(text = "الدرجة النهائية: ${st.finalScore}", fontSize = 10.sp, color = SocietyBluePrimary, fontWeight = FontWeight.SemiBold)
                                                }
                                            }

                                            Button(
                                                onClick = { showCertificatePreviewModal = st },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("معاينة الشهادة", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // قسم: سجل التصدير (تاريخ، مستخدم، عدد السجلات)
                    // ==========================================
                    3 -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = "سجل عمليات التصدير السابقة (Audit):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)

                                exportLogs.forEach { log ->
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
                                                Icon(
                                                    imageVector = if (log.format == "PDF") Icons.Default.PictureAsPdf else Icons.Default.TableChart,
                                                    contentDescription = null,
                                                    tint = if (log.format == "PDF") StatusError else EmeraldDark
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(text = log.reportType, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                                    Text(text = "بواسطة: ${log.exportedBy} • السجلات: ${log.recordsCount}", fontSize = 10.sp, color = TextMuted)
                                                }
                                            }
                                            Text(text = log.exportDate, fontSize = 10.sp, color = TextSubtle)
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
        // Modal: تحرير قالب الشهادة (نص، شعار، تاريخ)
        // ==========================================
        if (showTemplateEditorModal) {
            var tempTitle by remember { mutableStateOf(certificateTemplate.title) }
            var tempBody by remember { mutableStateOf(certificateTemplate.bodyText) }
            var tempDate by remember { mutableStateOf(certificateTemplate.dateText) }

            AlertDialog(
                onDismissRequest = { showTemplateEditorModal = false },
                title = { Text("تحرير وتخصيص قالب الشهادة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tempTitle,
                            onValueChange = { tempTitle = it },
                            label = { Text("عنوان الشهادة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempBody,
                            onValueChange = { tempBody = it },
                            label = { Text("صيغة التقدير والاعتماد") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        OutlinedTextField(
                            value = tempDate,
                            onValueChange = { tempDate = it },
                            label = { Text("تاريخ الإصدار") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            certificateTemplate = certificateTemplate.copy(
                                title = tempTitle,
                                bodyText = tempBody,
                                dateText = tempDate
                            )
                            snackbarMessage = "تم حفظ تحديثات قالب الشهادة بنجاح!"
                            showTemplateEditorModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        Text("حفظ القالب")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTemplateEditorModal = false }) { Text("إلغاء") }
                }
            )
        }

        // ==========================================
        // Modal: معاينة الشهادة الفردية
        // ==========================================
        if (showCertificatePreviewModal != null) {
            val st = showCertificatePreviewModal!!
            AlertDialog(
                onDismissRequest = { showCertificatePreviewModal = null },
                title = { Text("معاينة الشهادة المعتمدة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF9C3)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, GoldenAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = certificateTemplate.organizationName, fontSize = 12.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = certificateTemplate.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "تمنح هذه الشهادة للطالب النجيب:",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = st.studentName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SocietyBluePrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "تقديراً لاجتيازه: ${st.level} بتقدير: ${st.finalScore}",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "التاريخ: ${certificateTemplate.dateText}", fontSize = 10.sp, color = TextMuted)
                                Text(text = "الاعتماد: ${certificateTemplate.supervisorSignature}", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            snackbarMessage = "تم إصدار وتنزيل شهادة ${st.studentName} بصيغة PDF"
                            showCertificatePreviewModal = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                    ) {
                        Text("طباعة / تنزيل PDF")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCertificatePreviewModal = null }) { Text("إغلاق") }
                }
            )
        }
    }
}
