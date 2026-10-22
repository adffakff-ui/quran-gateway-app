package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

/**
 * مخطط تقدم الحفظ وتحليل تصنيف السور (مكية ومدنية)
 * يوفر رؤية تحليلية وتربوية عميقة للمشرفين العامين عن مسار حفظ الطلاب
 */
@Composable
fun QuranMemorizationChartSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedChartTab by remember { mutableStateOf(0) } // 0: مكية ومدنية, 1: شهري, 2: السور المقررة
    val overview = remember { QuranClassificationHelper.getOverviewStats() }
    val trackedSurahs = remember { QuranClassificationHelper.getTrackedSurahsMetrics() }

    fun exportToPdf() {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("تقرير مسار الحفظ والتقدم (رسمي)", 50f, 50f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("الجمعية: ${SocietyRepository.defaultRealTenant.name}", 50f, 100f, paint)
            canvas.drawText("عدد السور المكية المنجزة: ${overview.completedMakkiCount}", 50f, 130f, paint)
            canvas.drawText("عدد السور المدنية المنجزة: ${overview.completedMadaniCount}", 50f, 160f, paint)
            canvas.drawText("نسبة الإتقان العام: 92%", 50f, 190f, paint)

            pdfDocument.finishPage(page)

            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "quran_report_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            Toast.makeText(context, "تم تصدير التقرير بنجاح إلى: ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "فشل تصدير PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = modifier.testTag("quran_progress_chart_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // هيدر المخطط والتبديل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = SocietyBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مخطط مسار الحفظ وتصنيف السور",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                    Text(
                        text = "رؤية تحليلية لمسار حفظ الطلاب حسب السور المكية والمدنية",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldLight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldDark)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "إتقان عام: 92%",
                            color = EmeraldDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // أزرار التبديل التفاعلية الثلاثة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    Triple(0, "🕋 و 🕌 تصنيف السور", "مكية ومدنية"),
                    Triple(1, "📊 التقدم الشهري", "أعمدة بيانية"),
                    Triple(2, "📋 إتقان السور", "المقررة")
                )

                tabs.forEach { (index, title, sub) ->
                    val isSelected = selectedChartTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { selectedChartTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SocietyBluePrimary else TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // محتوى التبويب المختار
            AnimatedContent(
                targetState = selectedChartTab,
                label = "chart_tab_content"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        // ==========================================
                        // تبويب 0: تصنيف السور (مكية ومدنية) - الميزة المطلوبة
                        // ==========================================
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // بطاقتان مقارنتان: مكية vs مدنية
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // بطاقة السور المكية
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🕋", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "السور المكية",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF92400E)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFEF3C7)
                                            ) {
                                                Text(
                                                    text = "${overview.totalMakkiSurahs} سورة",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF92400E),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "تم إنجاز: ${overview.completedMakkiCount} سورة (${(overview.makkiCompletionRate * 100).toInt()}%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDark
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        // شريط تقدم الإنجاز المكي
                                        LinearProgressIndicator(
                                            progress = { overview.makkiCompletionRate },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = Color(0xFFD97706),
                                            trackColor = Color(0xFFFDE68A)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("معدل الإتقان:", fontSize = 10.sp, color = TextMuted)
                                            Text(
                                                "${overview.makkiAverageMastery}% (ممتاز)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldDark
                                            )
                                        }
                                    }
                                }

                                // بطاقة السور المدنية
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🕌", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "السور المدنية",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF166534)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFDCFCE7)
                                            ) {
                                                Text(
                                                    text = "${overview.totalMadaniSurahs} سورة",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF166534),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "تم إنجاز: ${overview.completedMadaniCount} سورة (${(overview.madaniCompletionRate * 100).toInt()}%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDark
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        // شريط تقدم الإنجاز المدني
                                        LinearProgressIndicator(
                                            progress = { overview.madaniCompletionRate },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = Color(0xFF10B981),
                                            trackColor = Color(0xFFBBF7D0)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("معدل الإتقان:", fontSize = 10.sp, color = TextMuted)
                                            Text(
                                                "${overview.madaniAverageMastery}% (جيد جداً)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SocietyBluePrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // بطاقة الرؤية التحليلية التربوية للمشرف العام
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = GoldenAmber,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "رؤية تحليلية للمشرف العام عن مسار الحفظ:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = overview.supervisorInsightNote,
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            // شريط عينات السور المقررة مصنفة (مكية / مدنية)
                            Column {
                                Text(
                                    text = "عينات من السور المتابعة في الحلقات:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val sampleSurahs = listOf(
                                        Pair("سورة النبأ", SurahClassification.MAKKI),
                                        Pair("سورة البقرة", SurahClassification.MADANI),
                                        Pair("سورة الملك", SurahClassification.MAKKI),
                                        Pair("سورة الحجرات", SurahClassification.MADANI)
                                    )
                                    sampleSurahs.forEach { (name, classification) ->
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (classification == SurahClassification.MAKKI) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
                                            border = BorderStroke(1.dp, if (classification == SurahClassification.MAKKI) Color(0xFFFDE68A) else Color(0xFFBBF7D0))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = classification.badge,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (classification == SurahClassification.MAKKI) Color(0xFF92400E) else Color(0xFF166534)
                                                )
                                                Text(
                                                    text = name,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDark,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // ==========================================
                        // تبويب 1: التقدم الشهري بالأعمدة (Bar Chart)
                        // ==========================================
                        MonthlyProgressBars()
                    }

                    2 -> {
                        // ==========================================
                        // تبويب 2: إتقان السور المقررة
                        // ==========================================
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            trackedSurahs.take(5).forEach { metric ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = metric.classification.badge,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (metric.classification == SurahClassification.MAKKI) Color(0xFF92400E) else Color(0xFF166534)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = metric.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                            Text(
                                                text = "الجزء ${metric.juzNumber} • ${metric.ayatCount} آية",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${metric.masteryPercent}% إتقان",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (metric.masteryPercent >= 90) EmeraldDark else SocietyBluePrimary
                                        )
                                        Text(
                                            text = "${metric.memorizedStudentsCount} طالب منجز",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(14.dp))

            // زر تصدير التقرير إلى PDF المذكور في المتطلبات
            Button(
                onClick = { exportToPdf() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_pdf_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SocietyBlueDark,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تصدير التقرير التحليلي إلى PDF (رسمي)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "إجمالي السور المتابعة: 114 سورة قرآنية",
                    fontSize = 11.sp,
                    color = SocietyBluePrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "المعدل: 86 مكية • 28 مدنية",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun MonthlyProgressBars() {
    val monthsData = listOf(
        Pair("محرم", 76),
        Pair("صفر", 81),
        Pair("ربيع ١", 85),
        Pair("ربيع ٢", 89),
        Pair("جمادى ١", 92),
        Pair("جمادى ٢", 95)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            monthsData.forEach { (month, percent) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$percent%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (percent >= 90) EmeraldDark else SocietyBluePrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height((percent * 1.05).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (percent >= 90) listOf(EmeraldGreen, EmeraldDark)
                                    else listOf(SocietyBluePrimary, SocietyBlueDark)
                                )
                            )
                        )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = month,
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
