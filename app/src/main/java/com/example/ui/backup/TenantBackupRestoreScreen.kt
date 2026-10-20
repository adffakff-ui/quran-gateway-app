package com.example.ui.backup

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantBackupRestoreScreen(
    currentTenant: Tenant,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var isExporting by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var isLoadingBackups by remember { mutableStateOf(false) }
    var backupsList by remember { mutableStateOf<List<BackupItem>>(emptyList()) }
    var lastExportedItem by remember { mutableStateOf<BackupItem?>(null) }
    var restoreSummaryResult by remember { mutableStateOf<RestoreSummary?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Manual JSON Import
    var showManualImportDialog by remember { mutableStateOf(false) }
    var manualJsonText by remember { mutableStateOf("") }
    var confirmRestoreTarget by remember { mutableStateOf<BackupItem?>(null) }

    // تحميل النسخ المتوفرة عند فتح الشاشة
    fun refreshBackups() {
        coroutineScope.launch {
            isLoadingBackups = true
            backupsList = FirebaseBackupManager.getAvailableBackups(currentTenant, context)
            isLoadingBackups = false
        }
    }

    LaunchedEffect(currentTenant.id) {
        refreshBackups()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "النسخ الاحتياطي واستعادة البيانات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${currentTenant.shortName} • Firebase Storage",
                            fontSize = 11.sp,
                            color = SocietyBluePrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("backup_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshBackups() },
                        modifier = Modifier.testTag("refresh_backups_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث النسخ", tint = SocietyBluePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SlateBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // بطاقة حالة الجمعية الحالية والبيانات القابلة للتصدير
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SocietyBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = SocietyBluePrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentTenant.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "المدينة: ${currentTenant.city} • الرمز: ${currentTenant.code}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = EmeraldLight
                        ) {
                            Text(
                                text = "Firebase Storage جاهز",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(14.dp))

                    // عدادات البيانات الحالية
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DataCounterBox(
                            count = "${SocietyRepository.allStudents.size}",
                            label = "طالب مسجل",
                            icon = Icons.Outlined.People,
                            color = SocietyBluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        DataCounterBox(
                            count = "${SocietyRepository.allTeachers.size}",
                            label = "معلم ومحفّظ",
                            icon = Icons.Outlined.School,
                            color = EmeraldDark,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        DataCounterBox(
                            count = "${SocietyRepository.allStudents.size}",
                            label = "سجل حضور",
                            icon = Icons.Outlined.CheckCircle,
                            color = GoldenAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ==========================================
            // بطاقة زر التصدير إلى Firebase Storage بصيغة JSON
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = SocietyBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تصدير بيانات الجمعية إلى Firebase Storage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يقوم هذا الإجراء بتجميع بيانات الطلاب والمعلمين وسجلات الحضور وتحويلها لملف JSON مشفر ورفعه إلى مسار backups/${currentTenant.id}/ في Firebase Storage لحمايتها من الفقدان.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isExporting = true
                                errorMessage = null
                                val result = FirebaseBackupManager.exportTenantToFirebaseStorage(
                                    tenant = currentTenant,
                                    context = context,
                                    performedBy = "المشرف العام (${currentTenant.shortName})"
                                )
                                isExporting = false
                                result.onSuccess { item ->
                                    lastExportedItem = item
                                    snackbarMessage = "تم تصدير النسخة الاحتياطية بنجاح إلى Firebase Storage!"
                                    refreshBackups()
                                }.onFailure { error ->
                                    errorMessage = "فشل التصدير: ${error.localizedMessage}"
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_backup_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارِ تحويل ورفع ملف JSON إلى Firebase Storage...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تصدير النسخة الاحتياطية الآن (JSON)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // بطاقة نجاح التصدير الأخير
                    AnimatedVisibility(visible = lastExportedItem != null) {
                        lastExportedItem?.let { item ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldLight,
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تم حفظ ورفع النسخة بنجاح!", fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 12.sp)
                                        }
                                        Text("${item.sizeKb} KB", fontSize = 11.sp, color = TextMuted)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "المسار: ${item.storagePath}",
                                        fontSize = 11.sp,
                                        color = TextDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "الرابط: ${if (item.isCloudSynced) "Firebase Storage (مرفوع سحابياً)" else "محلي في ذاكرة التطبيق"}",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // بطاقة خيارات الاستيراد والاستعادة عند مشاكل المزامنة
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = GoldenAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "النسخ المتوفرة واستعادة المزامنة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                        }

                        // زر استيراد يدوي بنص JSON
                        OutlinedButton(
                            onClick = { showManualImportDialog = true },
                            modifier = Modifier.testTag("manual_import_btn"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, GoldenAmber)
                        ) {
                            Icon(Icons.Default.DataObject, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استيراد يدوي (JSON)", fontSize = 11.sp, color = GoldenAmber)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "في حال واجهت مشاكل في مزامنة الطلاب أو المعلمين أو فقدان سجلات الحضور، يمكنك استعادة أي نسخة احتياطية سابقة بضغطة واحدة.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLoadingBackups) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = SocietyBluePrimary)
                        }
                    } else if (backupsList.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لا توجد نسخ احتياطية مسجلة لهذه الجمعية بعد", fontSize = 12.sp, color = TextMuted)
                                Text("اضغط على زر التصدير أعلاه لإنشاء أول نسخة في Firebase Storage", fontSize = 11.sp, color = TextSubtle)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            backupsList.forEach { backup ->
                                BackupItemCard(
                                    item = backup,
                                    onRestoreClick = {
                                        confirmRestoreTarget = backup
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ==========================================
    // حوار تأكيد الاستعادة من النسخة المحددة
    // ==========================================
    confirmRestoreTarget?.let { targetBackup ->
        AlertDialog(
            onDismissRequest = { confirmRestoreTarget = null },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(32.dp)) },
            title = { Text("تأكيد استعادة بيانات الجمعية", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("أنت على وشك استعادة بيانات الجمعية من النسخة:")
                    Text(
                        text = targetBackup.fileName,
                        fontWeight = FontWeight.Bold,
                        color = SocietyBluePrimary,
                        fontSize = 12.sp
                    )
                    Text("تاريخ النسخة: ${targetBackup.formattedDate}", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "سيتم استبدال سجلات الطلاب والمعلمين والحلقات الحالية بمحتوى هذه النسخة الاحتياطية وإعادة مزامنة النظام فوراً.",
                        fontSize = 11.sp,
                        color = TextDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backupToRestore = targetBackup
                        confirmRestoreTarget = null
                        coroutineScope.launch {
                            isRestoring = true
                            val result = FirebaseBackupManager.downloadAndRestoreFromStorage(
                                backupItem = backupToRestore,
                                targetTenant = currentTenant,
                                context = context
                            )
                            isRestoring = false
                            result.onSuccess { summary ->
                                restoreSummaryResult = summary
                                snackbarMessage = "تمت استعادة بيانات الجمعية بنجاح!"
                            }.onFailure { err ->
                                errorMessage = "فشل في الاستعادة: ${err.localizedMessage}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber)
                ) {
                    Text("تأكيد الاستعادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreTarget = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ==========================================
    // حوار الاستيراد اليدوي لنص JSON (Manual Import)
    // ==========================================
    if (showManualImportDialog) {
        AlertDialog(
            onDismissRequest = { showManualImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DataObject, contentDescription = null, tint = GoldenAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استيراد بيانات من JSON مباشر", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "الصق محتوى ملف النسخة الاحتياطية (JSON) هنا لاستيراد الطلاب والمعلمين وسجلات الحضور فوراً:",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualJsonText,
                        onValueChange = { manualJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("manual_json_input"),
                        placeholder = { Text("{\n  \"version\": 1,\n  \"students\": [...],\n  \"teachers\": [...]\n}", fontSize = 10.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualJsonText.isBlank()) {
                            Toast.makeText(context, "الرجاء لصق نص JSON صالح", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showManualImportDialog = false
                        coroutineScope.launch {
                            isRestoring = true
                            val result = FirebaseBackupManager.restoreTenantFromJson(
                                jsonString = manualJsonText,
                                targetTenant = currentTenant,
                                sourceFileName = "manual_paste.json"
                            )
                            isRestoring = false
                            result.onSuccess { summary ->
                                restoreSummaryResult = summary
                                snackbarMessage = "تم استيراد واستعادة البيانات يدوياً بنجاح!"
                                manualJsonText = ""
                            }.onFailure { err ->
                                errorMessage = "صيغة JSON غير صالحة: ${err.localizedMessage}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary)
                ) {
                    Text("فحص واستعادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualImportDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ==========================================
    // حوار ملخص نتائج الاستعادة الناجحة
    // ==========================================
    restoreSummaryResult?.let { summary ->
        AlertDialog(
            onDismissRequest = { restoreSummaryResult = null },
            icon = { Icon(Icons.Default.TaskAlt, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(36.dp)) },
            title = { Text("اكتملت الاستعادة بنجاح", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تمت استعادة ومزامنة بيانات جمعية ${summary.tenantName}:", fontSize = 12.sp, color = TextDark)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الطلاب المستعادين:", fontSize = 11.sp, color = TextMuted)
                                Text("${summary.restoredStudentsCount} طالب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المعلمين المستعادين:", fontSize = 11.sp, color = TextMuted)
                                Text("${summary.restoredTeachersCount} معلم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SocietyBluePrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("الحلقات القرآنية:", fontSize = 11.sp, color = TextMuted)
                                Text("${summary.restoredRingsCount} حلقة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("سجلات الحضور:", fontSize = 11.sp, color = TextMuted)
                                Text("${summary.restoredAttendanceCount} سجل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldenAmber)
                            }
                            HorizontalDivider(color = BorderSubtle)
                            Text("تاريخ تصدير النسخة: ${summary.exportDate}", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                    Text(
                        "تم تحديث الذاكرة المحلية وقاعدة البيانات وإعادة تشغيل المزامنة الفورية.",
                        fontSize = 11.sp,
                        color = StatusSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { restoreSummaryResult = null },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) {
                    Text("تم")
                }
            }
        )
    }

    // رسائل التنبيه أو الأخطاء
    errorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError) },
            title = { Text("تنبيه في النسخ الاحتياطي", fontWeight = FontWeight.Bold) },
            text = { Text(err, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("حسناً")
                }
            }
        )
    }
}

@Composable
private fun DataCounterBox(
    count: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun BackupItemCard(
    item: BackupItem,
    onRestoreClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (item.isCloudSynced) SocietyBlueLight else GoldenAmberLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isCloudSynced) Icons.Default.CloudDone else Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = if (item.isCloudSynced) SocietyBluePrimary else GoldenAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.formattedDate} • ${item.sizeKb} KB",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.isCloudSynced) EmeraldLight else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = if (item.isCloudSynced) "سحابي (Storage)" else "محلي",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isCloudSynced) EmeraldDark else TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "يحتوي على: ${item.studentCount} طالب • ${item.teacherCount} معلم",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Button(
                    onClick = onRestoreClick,
                    modifier = Modifier.height(34.dp).testTag("restore_btn_${item.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenAmber),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استعادة هذه النسخة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
