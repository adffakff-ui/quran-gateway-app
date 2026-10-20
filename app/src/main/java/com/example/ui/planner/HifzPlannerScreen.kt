package com.example.ui.planner

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
import androidx.compose.ui.graphics.Brush
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

data class HifzPlanItem(
    val id: String,
    val studentName: String,
    val circleName: String,
    val targetSurah: String,
    val targetAyahRange: String,
    val planType: String, // "حفظ جديد", "مراجعة صغرى", "مراجعة كبرى"
    val masteryRate: Float, // 0.0 to 1.0
    val status: String, // "مكتمل", "قيد التنفيذ", "متأخر"
    val supervisorNotes: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HifzPlannerScreen(
    currentTenant: Tenant,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: خطط الحفظ, 1: تتبع الإتقان, 2: التوصيات الذكية
    var searchFilter by remember { mutableStateOf("") }
    var selectedPlanTypeFilter by remember { mutableStateOf("الكل") }

    var plansList by remember {
        mutableStateOf(
            listOf(
                HifzPlanItem("p1", "عبد الرحمن محمد الصالح", "حلقة الإمام الشاطبي", "سورة البقرة", "الآيات 141 - 176", "حفظ جديد", 0.85f, "قيد التنفيذ", "إتقان ممتاز مع تجوید متقن"),
                HifzPlanItem("p2", "عمر خالد العمري", "حلقة ابن الجزري", "سورة آل عمران", "الآيات 30 - 75", "مراجعة كبرى", 0.92f, "مكتمل", "مراجعة متينة ومتميزة"),
                HifzPlanItem("p3", "إبراهيم وليد القاضي", "حلقة عاصم الكوفي", "سورة النساء", "الآيات 1 - 25", "حفظ جديد", 0.60f, "قيد التنفيذ", "يحتاج متابعة في أحكام المدود"),
                HifzPlanItem("p4", "يوسف أحمد البعداني", "حلقة دار القرآن", "سورة المائدة", "الآيات 50 - 82", "مراجعة صغرى", 0.78f, "قيد التنفيذ", "حفظ مستقر وثابت"),
                HifzPlanItem("p5", "حمزة توفيق العديني", "حلقة الإمام الشاطبي", "سورة الأنعام", "الآيات 10 - 45", "حفظ جديد", 0.45f, "متأخر", "تأخر في إنجاز الورد الأسبوعي")
            )
        )
    }

    var showAddPlanDialog by remember { mutableStateOf(false) }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }

    // New Plan form fields
    var newStudentName by remember { mutableStateOf("") }
    var newTargetSurah by remember { mutableStateOf("") }
    var newTargetAyahs by remember { mutableStateOf("") }
    var newPlanType by remember { mutableStateOf("حفظ جديد") }

    val filteredPlans = remember(plansList, searchFilter, selectedPlanTypeFilter) {
        plansList.filter { plan ->
            val matchesSearch = searchFilter.isBlank() ||
                    plan.studentName.contains(searchFilter, ignoreCase = true) ||
                    plan.circleName.contains(searchFilter, ignoreCase = true) ||
                    plan.targetSurah.contains(searchFilter, ignoreCase = true)
            val matchesType = selectedPlanTypeFilter == "الكل" || plan.planType == selectedPlanTypeFilter
            matchesSearch && matchesType
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.EventNote, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مخطط الحفظ والمراجعة الذكي",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Text(
                                text = "إدارة الخطط القرآنية ومؤشرات الإتقان في ${currentTenant.shortName}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("planner_back_btn")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        Button(
                            onClick = { showAddPlanDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                            modifier = Modifier.padding(end = 8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("خطة جديدة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                if (snackbarMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SocietyBlueLight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SocietyBluePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = snackbarMsg ?: "", fontSize = 12.sp, color = SocietyBlueDark, modifier = Modifier.weight(1f))
                            IconButton(onClick = { snackbarMsg = null }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                    }
                }

                // ==========================================
                // التبويبات الرئيسية (Tabs)
                // ==========================================
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    edgePadding = 0.dp,
                    contentColor = SocietyBluePrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("خطط الحفظ النشطة (${plansList.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("مؤشرات الإتقان والتحليل", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("التوصيات الذكية AI", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // ==========================================
                        // تبويب خطط الحفظ النشطة
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                OutlinedTextField(
                                    value = searchFilter,
                                    onValueChange = { searchFilter = it },
                                    placeholder = { Text("ابحث باسم الطالب أو السورة أو الحلقة...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SocietyBluePrimary) },
                                    trailingIcon = {
                                        if (searchFilter.isNotEmpty()) {
                                            IconButton(onClick = { searchFilter = "" }) { Icon(Icons.Default.Clear, contentDescription = null) }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val types = listOf("الكل", "حفظ جديد", "مراجعة صغرى", "مراجعة كبرى")
                                    types.forEach { t ->
                                        FilterChip(
                                            selected = selectedPlanTypeFilter == t,
                                            onClick = { selectedPlanTypeFilter = t },
                                            label = { Text(t, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SocietyBlueLight,
                                                selectedLabelColor = SocietyBlueDark
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredPlans.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("لا توجد خطط حفظ مطابقة للبحث", color = TextMuted, fontSize = 14.sp)
                                }
                            }
                        } else {
                            filteredPlans.forEach { plan ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(SocietyBlueLight),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(20.dp))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(text = plan.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                                    Text(text = plan.circleName, fontSize = 11.sp, color = TextMuted)
                                                }
                                            }

                                            Surface(
                                                color = when (plan.status) {
                                                    "مكتمل" -> EmeraldLight
                                                    "قيد التنفيذ" -> SocietyBlueLight
                                                    else -> Color(0xFFFEF2F2)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = plan.status,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (plan.status) {
                                                        "مكتمل" -> EmeraldGreen
                                                        "قيد التنفيذ" -> SocietyBluePrimary
                                                        else -> Color(0xFFDC2626)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = BorderSubtle)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(text = "الهدف القرآني والمسار:", fontSize = 11.sp, color = TextMuted)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = "${plan.targetSurah} (${plan.targetAyahRange})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    val classification = QuranClassificationHelper.getClassification(plan.targetSurah)
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (classification == SurahClassification.MAKKI) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                                                    ) {
                                                        Text(
                                                            text = classification.badge,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (classification == SurahClassification.MAKKI) Color(0xFF92400E) else Color(0xFF166534),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(text = "نوع الخطة:", fontSize = 11.sp, color = TextMuted)
                                                Text(text = plan.planType, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SocietyBluePrimary)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Progress Bar for Mastery
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "نسبة الإتقان المحققة", fontSize = 11.sp, color = TextMuted)
                                                Text(text = "${(plan.masteryRate * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SocietyBluePrimary)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { plan.masteryRate },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = if (plan.masteryRate >= 0.8f) EmeraldGreen else SocietyBluePrimary,
                                                trackColor = SocietyBlueLight
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(text = "ملاحظات المشرف: ${plan.supervisorNotes}", fontSize = 11.sp, color = TextDark)

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    snackbarMsg = "تم تحديث تقييم إتقان الطالب ${plan.studentName} بنجاح."
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تحديث التقييم", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // ==========================================
                        // تبويب مؤشرات الإتقان والتحليل
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "مؤشرات أداء الحفظ والإتقان الإجمالي", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "إحصائيات تحليلية مبنية على رصد الحلقات القرآنية في ${currentTenant.shortName}", fontSize = 12.sp, color = TextMuted)

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = SocietyBlueLight.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("متوسط الإتقان العام", fontSize = 11.sp, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("84.6%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SocietyBluePrimary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("↑ 3.2% عن الشهر السابق", fontSize = 10.sp, color = EmeraldGreen)
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = EmeraldLight.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("الخطط المكتملة", fontSize = 11.sp, color = TextMuted)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("142 خطة", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("بنسبة إنجاز 91%", fontSize = 10.sp, color = TextDark)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(text = "توزيع مستويات الإتقان للحلقات:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                Spacer(modifier = Modifier.height(10.dp))

                                val levels = listOf(
                                    Triple("ممتاز (90% - 100%)", 0.68f, EmeraldGreen),
                                    Triple("جيد جداً (80% - 89%)", 0.22f, SocietyBluePrimary),
                                    Triple("جيد ومقبول (أقل من 80%)", 0.10f, GoldenAmber)
                                )

                                levels.forEach { (label, ratio, color) ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                            Text("${(ratio * 100).toInt()}% من الطلاب", fontSize = 11.sp, color = TextMuted)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { ratio },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = color,
                                            trackColor = SlateBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // ==========================================
                        // تبويب التوصيات الذكية AI
                        // ==========================================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SocietyBluePrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "التوصيات الذكية لرفع كفاءة الحفظ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "بناءً على تحليل البيانات الصوتية ومعدلات تسميع الحلقات في ${currentTenant.shortName}، يقترح نظام الذكاء الاصطناعي التوجيهات الآتية:",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                val recommendations = listOf(
                                    Pair("توصية للمراجعة المكثفة", "تم رصد تراجع طفيف في إتقان سورة آل عمران لدى حلقة الإمام الشاطبي. يوصى بجدولة أسبوع تلاوة ومراجعة مكثفة."),
                                    Pair("توصية بالحفظ الجديد", "أظهر الطلاب المتفوقون في حلقة ابن الجزري سرعة إنجاز عالية؛ يُنصح بزيادة الورد اليومي بمقدار نصف وجه إضافي."),
                                    Pair("توصية بالتشجيع والتحفيز", "الطالب عبد الرحمن الصالح حقق أعلى معدل التزام خلال الشهر الحالي؛ يُقترح تكريمه بدرع التميز القرآني.")
                                )

                                recommendations.forEach { (title, desc) ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = SocietyBlueLight.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = GoldenAmber, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SocietyBlueDark)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(desc, fontSize = 11.sp, color = TextDark)
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
        // Modal: إضافة خطة حفظ جديدة
        // ==========================================
        if (showAddPlanDialog) {
            AlertDialog(
                onDismissRequest = { showAddPlanDialog = false },
                title = { Text("إضافة خطة حفظ ومراجعة جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newStudentName,
                            onValueChange = { newStudentName = it },
                            label = { Text("اسم الطالب الرباعي") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newTargetSurah,
                            onValueChange = { newTargetSurah = it },
                            label = { Text("السورة المستهدفة (مثال: سورة البقرة)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newTargetAyahs,
                            onValueChange = { newTargetAyahs = it },
                            label = { Text("نطاق الآيات (مثال: الآيات 1 - 50)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newStudentName.isNotBlank() && newTargetSurah.isNotBlank()) {
                                val item = HifzPlanItem(
                                    id = "p_${System.currentTimeMillis()}",
                                    studentName = newStudentName.trim(),
                                    circleName = "حلقة الإمام الشاطبي",
                                    targetSurah = newTargetSurah.trim(),
                                    targetAyahRange = newTargetAyahs.ifBlank { "الآيات 1 - 20" },
                                    planType = newPlanType,
                                    masteryRate = 0.50f,
                                    status = "قيد التنفيذ",
                                    supervisorNotes = "خطة جديدة تمت إضافتها للتو"
                                )
                                plansList = listOf(item) + plansList
                                snackbarMsg = "تمت إضافة خطة الحفظ للطالب ${newStudentName} بنجاح."
                                newStudentName = ""
                                newTargetSurah = ""
                                newTargetAyahs = ""
                                showAddPlanDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ وإضافة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPlanDialog = false }) {
                        Text("إلغاء", color = TextMuted)
                    }
                },
                containerColor = Color.White
            )
        }
    }
}
