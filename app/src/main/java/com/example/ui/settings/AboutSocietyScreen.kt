package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Tenant
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSocietyScreen(
    currentTenant: Tenant,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    // Direct Feedback form state
    var feedbackSenderName by remember { mutableStateOf("") }
    var feedbackSubject by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "عن الجمعية والمنظومة الرقمية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("about_society_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // بطاقة الهيدر والتعريف بالجمعية
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_hero_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        SocietyBluePrimary,
                                        SocietyBlueDark
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_society_logo),
                                    contentDescription = "شعار الجمعية",
                                    modifier = Modifier.size(54.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = currentTenant.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                color = EmeraldGreen.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "ترخيص وزاري رسمي رقم ${currentTenant.licenseNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "منظومة إلكترونية رائدة لإدارة حلقات تحفيظ القرآن الكريم، التسميع الذكي، والتواصل الفعّال",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // الرؤية والرسالة
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // بطاقة الرؤية
                    VisionMissionCard(
                        title = "رؤيتنا الرقمية",
                        subtitle = "الريادة في تحفيظ القرآن الكريم وتعليم علومه عبر بيئة تقنية سحابية ذكية ومستدامة، تعزز إتقان التلاوة وتيسر التعلم لكافة شرائح المجتمع.",
                        icon = Icons.Outlined.Visibility,
                        accentColor = SocietyBluePrimary,
                        modifier = Modifier.weight(1f)
                    )

                    // بطاقة الرسالة
                    VisionMissionCard(
                        title = "رسالتنا السامية",
                        subtitle = "تطوير منظومة قرآنية تفاعلية متكاملة تجمع بين الإسناد القرآني المتصل والحلول الرقمية الرائدة لإدارة الحلقات وقياس الأثر بدقة ومصداقية.",
                        icon = Icons.Outlined.Lightbulb,
                        accentColor = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ==========================================
            // الأهداف الاستراتيجية للجمعية الرقمية
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SocietyBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TrackChanges,
                                    contentDescription = null,
                                    tint = SocietyBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "أهداف المنظومة الرقمية للجمعية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val goals = listOf(
                            Pair("أتمتة إدارة الحلقات القرآنية", "إدارة وتوثيق الحضور، السعة الاستيعابية، وإسناد المعلمين بدقة بدون أوراق."),
                            Pair("دعم التسميع الصوتي والقراءات العشر", "تمكين التلاوة المباشرة والتقييم الصوتي الذكي بمختلف الروايات القرآنية المعتمدة."),
                            Pair("تعزيز الشراكة مع أولياء الأمور", "بوابة تفاعلية تمكّن الأسرة من متابعة إنجاز الأبناء اليومي والتواصل المباشر مع المعلمين."),
                            Pair("توليد الشهادات والتقارير المعتمدة", "إصدار فوري لشهادات الإتمام وتقارير الأداء المعتمدة بختم رسمي موثق."),
                            Pair("الأمان والنسخ الاحتياطي السحابي", "حماية بيانات الحلقات وسرية السجلات من خلال تشفير متقدم ومزامنة سحابية مستمرة.")
                        )

                        goals.forEachIndexed { index, (goalTitle, goalDesc) ->
                            GoalRowItem(
                                number = index + 1,
                                title = goalTitle,
                                description = goalDesc
                            )
                            if (index < goals.size - 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            // ==========================================
            // إحصائيات وأثر الجمعية
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "إحصائيات المنظومة والأثر التعليمي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImpactStatBadge(title = "الحلقات القرآنية", value = "${currentTenant.ringsCount} حلقة", color = SocietyBluePrimary, modifier = Modifier.weight(1f))
                            ImpactStatBadge(title = "الطلاب المسجلون", value = "${currentTenant.studentsCount} طالب", color = EmeraldGreen, modifier = Modifier.weight(1f))
                            ImpactStatBadge(title = "المعلمون والمشرفون", value = "${currentTenant.teachersCount} معلم", color = WarmAmber, modifier = Modifier.weight(1f))
                            ImpactStatBadge(title = "نسبة الانضباط", value = "${currentTenant.attendanceRate}%", color = PurpleAccent, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // ==========================================
            // معلومات وقنوات التواصل المباشرة
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_contact_info_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HeadsetMic,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "قنوات التواصل المباشر مع الجمعية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "فريق الإشراف والدعم الفني متاح لخدمتكم يومياً",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // اتصال هاتفي مباشر
                        ContactActionRow(
                            icon = Icons.Outlined.Phone,
                            title = "الاتصال الهاتفي المباشر",
                            subtitle = "+967 775 484 201 / 011 405 0000",
                            actionLabel = "اتصال الآن",
                            accentColor = SocietyBluePrimary,
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:967775484201"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح تطبيق الهاتف", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // واتساب مباشر
                        ContactActionRow(
                            icon = Icons.Outlined.Chat,
                            title = "محادثة واتساب الرسمية",
                            subtitle = "رد فوري وتوثيق الاستفسارات والشكاوى",
                            actionLabel = "فتح واتساب",
                            accentColor = EmeraldGreen,
                            onClick = {
                                val whatsappUrl = "https://wa.me/967775484201?text=${Uri.encode("السلام عليكم ورحمة الله، استفسار بشأن منظومة جمعية تحفيظ القرآن الكريم")}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // بريد إلكتروني
                        ContactActionRow(
                            icon = Icons.Outlined.Email,
                            title = "البريد الإلكتروني الرسمي",
                            subtitle = "info@maknoon.org.sa / support@society.org",
                            actionLabel = "إرسال بريد",
                            accentColor = PurpleAccent,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:info@maknoon.org.sa?subject=${Uri.encode("استفسار بخصوص الجمعية والمنظومة القرآنية")}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح تطبيق البريد", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // الموقع الإلكتروني
                        ContactActionRow(
                            icon = Icons.Outlined.Language,
                            title = "البوابة الإلكترونية الرسمية",
                            subtitle = "https://maknoon.org.sa",
                            actionLabel = "زيارة الموقع",
                            accentColor = SocietyBluePrimary,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maknoon.org.sa"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح المتصفح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // المقر الإداري
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "المقر الرئيسي والإدارة العامة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "${currentTenant.governorate} - ${currentTenant.city} - المركز التعليمي الرئيسي",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // نموذج إرسال استفسار مباشر
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "نموذج تواصل مباشر مع إدارة الجمعية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                        Text(
                            text = "يمكنك كتابة رسالتك أو اقتراحك هنا وسيتم إرسالها مباشرة للإدارة المعنية:",
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        OutlinedTextField(
                            value = feedbackSenderName,
                            onValueChange = { feedbackSenderName = it },
                            label = { Text("الاسم الكريم") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = feedbackSubject,
                            onValueChange = { feedbackSubject = it },
                            label = { Text("موضوع الرسالة") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = feedbackMessage,
                            onValueChange = { feedbackMessage = it },
                            label = { Text("نص الرسالة أو الاستفسار") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                if (feedbackMessage.isBlank()) {
                                    Toast.makeText(context, "يرجى كتابة نص الرسالة أولاً", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val subject = feedbackSubject.ifBlank { "استفسار من: ${feedbackSenderName.ifBlank { "مستخدم المنظومة" }}" }
                                val body = "الاسم: $feedbackSenderName\nالجمعية: ${currentTenant.name}\n\nنص الرسالة:\n$feedbackMessage"
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:info@maknoon.org.sa?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
                                }
                                try {
                                    context.startActivity(intent)
                                    snackbarMessage = "تم فتح تطبيق البريد لإرسال رسالتك مباشرة"
                                    feedbackMessage = ""
                                    feedbackSubject = ""
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح البريد", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_about_feedback_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال الرسالة إلى إدارة الجمعية", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Snackbar
        if (snackbarMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("حسناً", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                ) {
                    Text(snackbarMessage!!)
                }
            }
        }
    }
}

@Composable
fun VisionMissionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun GoalRowItem(
    number: Int,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SocietyBluePrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 11.sp, color = TextMuted, lineHeight = 16.sp)
        }
    }
}

@Composable
fun ImpactStatBadge(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = color)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = title, fontSize = 9.sp, color = TextDark, textAlign = TextAlign.Center)
    }
}

@Composable
fun ContactActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextDark
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            FilledTonalButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
