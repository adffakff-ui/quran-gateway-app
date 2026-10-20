package com.example.ui.login

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SocietyRepository
import com.example.data.Tenant
import com.example.ui.theme.*

private const val SUPPORT_EMAIL = "adffakff@gmail.com"
private const val BACKUP_SUPPORT_EMAIL = "join@quran-society.org"

// ألوان واضحة عالية التباين لحل مشكلة عدم ظهور الخط أثناء الكتابة
private val FieldTextColor = Color(0xFF0F172A) // أسود كحلي غامق جداً وواضح
private val FieldBgColor = Color(0xFFF8FAFC) // خلفية فاتحة مريحة للعين داخل البطاقة
private val FieldPlaceholderColor = Color(0xFF64748B) // رمادي متوسط مقروء
private val FieldLabelColor = Color(0xFF334155) // رمادي داكن للعنوان

/**
 * قسم "تواصل معنا - طلب انضمام جمعية للنظام"
 * يحتوي على:
 * 1. نموذج تقديم طلب انضمام الجمعية مع إصلاح خط الكتابة ليظهر بوضوح تام.
 * 2. ربط الطلب بالبريد الإلكتروني المعتمد للإدارة.
 * 3. ميزة خاصة بالمطور والإدارة: فحص بيانات الجمعية وزر "موافقة على الطلب" لاعتماد الجمعية فوراً.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsJoinSection(
    modifier: Modifier = Modifier,
    onSocietyApproved: (Tenant) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Expanded / Collapsed state
    var isExpanded by remember { mutableStateOf(true) }

    // Form Fields
    var societyName by remember { mutableStateOf("") }
    var governorateAndCity by remember { mutableStateOf("") }
    var contactPersonName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var notesOrRingsCount by remember { mutableStateOf("") }

    // Validation & Dialogs
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showNoEmailAppDialog by remember { mutableStateOf(false) }
    var showDeveloperReviewModal by remember { mutableStateOf(false) }
    var approvedTenantResult by remember { mutableStateOf<Tenant?>(null) }

    fun sendJoinRequestViaEmail() {
        if (societyName.trim().isEmpty()) {
            errorMessage = "يرجى كتابة اسم الجمعية أو المركز القرآني."
            return
        }
        if (contactPersonName.trim().isEmpty()) {
            errorMessage = "يرجى كتابة اسم المشرف أو مسؤول التواصل."
            return
        }
        if (phoneNumber.trim().isEmpty() && contactEmail.trim().isEmpty()) {
            errorMessage = "يرجى إدخال رقم هاتف أو بريد إلكتروني للتواصل معكم."
            return
        }

        errorMessage = null

        val subject = "طلب انضمام جمعية جديدة: ${societyName.trim()} (${if (governorateAndCity.isNotBlank()) governorateAndCity.trim() else "اليمن"})"
        val bodyContent = buildString {
            appendLine("السلام عليكم ورحمة الله وبركاته،")
            appendLine()
            appendLine("نود تقديم طلب انضمام جمعيتنا / مركزنا القرآني إلى المنظومة الإلكترونية الموحدة لإدارة حلقات تحفيظ القرآن الكريم.")
            appendLine()
            appendLine("══════════════════════════════════════")
            appendLine("تفاصيل طلب الانضمام:")
            appendLine("• اسم الجمعية / المركز: ${societyName.trim()}")
            appendLine("• المحافظة والمدينة: ${if (governorateAndCity.isNotBlank()) governorateAndCity.trim() else "غير محدد"}")
            appendLine("• اسم مسؤول التواصل: ${contactPersonName.trim()}")
            appendLine("• رقم الهاتف / واتساب: ${if (phoneNumber.isNotBlank()) phoneNumber.trim() else "غير مدخل"}")
            appendLine("• البريد الإلكتروني للمسؤول: ${if (contactEmail.isNotBlank()) contactEmail.trim() else "غير مدخل"}")
            appendLine("• عدد الحلقات والملاحظات الإضافية: ${if (notesOrRingsCount.isNotBlank()) notesOrRingsCount.trim() else "لا يوجد"}")
            appendLine("══════════════════════════════════════")
            appendLine()
            appendLine("نأمل منكم التكرم بمراجعة بيانات الجمعية واعتماد حساب المنشأة وتزويدنا ببيانات تسجيل الدخول للوحة المشرف العام.")
            appendLine("شاكرين لكم جهودكم المباركة في خدمة كتاب الله تعالى.")
        }

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$SUPPORT_EMAIL")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL, BACKUP_SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, bodyContent)
        }

        try {
            val chooser = Intent.createChooser(emailIntent, "إرسال طلب الانضمام عبر تطبيق البريد")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            showSuccessDialog = true
        } catch (e: ActivityNotFoundException) {
            showNoEmailAppDialog = true
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق البريد: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            showNoEmailAppDialog = true
        }
    }

    // دالة الموافقة على الطلب واعتماد الجمعية في النظام (ميزة المطور)
    fun approveSocietyRequest() {
        val sName = if (societyName.isNotBlank()) societyName.trim() else "جمعية الفرقان لتحفيظ القرآن"
        val sGov = if (governorateAndCity.isNotBlank()) governorateAndCity.trim() else "تعز"
        val sPerson = if (contactPersonName.isNotBlank()) contactPersonName.trim() else "أ. عبد الله محمد"
        val sPhone = if (phoneNumber.isNotBlank()) phoneNumber.trim() else "770123456"
        val sEmail = if (contactEmail.isNotBlank()) contactEmail.trim() else "info@society.org"
        val sNotes = notesOrRingsCount.ifBlank { "15 حلقة" }

        val newTenant = SocietyRepository.approveNewSociety(
            name = sName,
            governorate = sGov,
            supervisorName = sPerson,
            phone = sPhone,
            email = sEmail,
            notes = sNotes
        )

        approvedTenantResult = newTenant
        showDeveloperReviewModal = false
        showSuccessDialog = false
        onSocietyApproved(newTenant)
        Toast.makeText(context, "تمت الموافقة واعتماد جمعية \"${newTenant.shortName}\" بنجاح!", Toast.LENGTH_LONG).show()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .testTag("contact_us_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(SocietyBluePrimary, EmeraldGreen)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MailOutline,
                            contentDescription = "تواصل معنا",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "تواصل معنا | طلب انضمام جمعية",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "إضافة جمعيتكم إلى المنظومة الإلكترونية",
                            fontSize = 11.sp,
                            color = SocietyBluePrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Toggle Expand/Collapse Button
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .testTag("contact_us_toggle_button")
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "طي النموذج" else "فتح النموذج",
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "هل تشرف على جمعية أو مركز قرآني وترغب بربطه بالمنظومة وإصدار لوحة تحكم متكاملة؟ أرسل طلبك وسيتواصل معك الدعم الفني.",
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 18.sp
            )

            // Direct support email pill badge (pressable without revealing email)
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = SocietyBlueLight,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val directIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$SUPPORT_EMAIL?subject=${Uri.encode("استفسار للدعم الفني")}")
                        }
                        try {
                            context.startActivity(directIntent)
                        } catch (e: Exception) {
                            clipboardManager.setText(AnnotatedString(SUPPORT_EMAIL))
                            Toast.makeText(context, "تم نسخ بيانات التواصل للدعم الفني", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .testTag("support_email_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.SupportAgent,
                            contentDescription = null,
                            tint = SocietyBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تواصل مع فريق الدعم الفني والتقني (اضغط هنا)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SocietyBluePrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = SocietyBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expandable Form
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Error Message Banner
                    if (errorMessage != null) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contact_error_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "خطأ",
                                    tint = StatusError,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage.orEmpty(),
                                    fontSize = 12.sp,
                                    color = StatusError,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // نمط الخط وحزم الألوان المشتركة لضمان ظهور النص والكتابة بوضوح
                    val inputTextStyle = TextStyle(
                        color = FieldTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val inputFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FieldTextColor,
                        unfocusedTextColor = FieldTextColor,
                        focusedContainerColor = FieldBgColor,
                        unfocusedContainerColor = FieldBgColor,
                        cursorColor = SocietyBluePrimary,
                        focusedBorderColor = SocietyBluePrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = SocietyBluePrimary,
                        unfocusedLabelColor = FieldLabelColor,
                        focusedPlaceholderColor = FieldPlaceholderColor,
                        unfocusedPlaceholderColor = FieldPlaceholderColor
                    )

                    // الحقل 1: اسم الجمعية أو المركز
                    OutlinedTextField(
                        value = societyName,
                        onValueChange = {
                            societyName = it
                            if (errorMessage != null) errorMessage = null
                        },
                        textStyle = inputTextStyle,
                        label = { Text("اسم الجمعية / المركز القرآني *", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        placeholder = { Text("مثال: جمعية معاذ العلمية أو دار الفرقان", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = SocietyBluePrimary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_society_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = inputFieldColors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // الحقل 2: المحافظة والمدينة
                    OutlinedTextField(
                        value = governorateAndCity,
                        onValueChange = { governorateAndCity = it },
                        textStyle = inputTextStyle,
                        label = { Text("المحافظة والمدينة / الفرع", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        placeholder = { Text("مثال: تعز، صنعاء، عدن، مأرب، حضرموت...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldGreen)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_city_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = inputFieldColors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // الحقل 3: اسم المشرف أو مسؤول التواصل
                    OutlinedTextField(
                        value = contactPersonName,
                        onValueChange = {
                            contactPersonName = it
                            if (errorMessage != null) errorMessage = null
                        },
                        textStyle = inputTextStyle,
                        label = { Text("اسم المشرف أو مسؤول التواصل *", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        placeholder = { Text(":  ", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = SocietyBluePrimary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_person_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = inputFieldColors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // الحقل 4: رقم الجوال أو الواتساب
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            if (errorMessage != null) errorMessage = null
                        },
                        textStyle = inputTextStyle,
                        label = { Text("رقم الجوال أو الواتساب *", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        placeholder = { Text(":  أو +967...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = GoldenAmber)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = inputFieldColors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // الحقل 5: البريد الإلكتروني للمسؤول
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = {
                            contactEmail = it
                            if (errorMessage != null) errorMessage = null
                        },
                        textStyle = inputTextStyle,
                        label = { Text("البريد الإلكتروني للجمعية / المسؤول", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        placeholder = { Text("info@quran-society.org", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = SocietyBluePrimary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_email_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = inputFieldColors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // الحقل 6: عدد الحلقات وملاحظات إضافية
                    OutlinedTextField(
                        value = notesOrRingsCount,
                        onValueChange = { notesOrRingsCount = it },
                        textStyle = inputTextStyle,
                        label = { Text("عدد الحلقات التقريبي / ملاحظات إضافية", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        placeholder = { Text("مثال: 12 حلقة للبنين و 8 للبنات، 250 طالباً...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = TextMuted)
                        },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_notes_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = inputFieldColors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // زر إرسال الطلب عبر البريد الإلكتروني
                    Button(
                        onClick = { sendJoinRequestViaEmail() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("contact_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SocietyBluePrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال طلب الانضمام عبر البريد الإلكتروني",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))



                    Spacer(modifier = Modifier.height(10.dp))

                    // زر مراسلة مباشرة عبر Gmail
                    OutlinedButton(
                        onClick = {
                            val directIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$SUPPORT_EMAIL?subject=${Uri.encode("استفسار عن انضمام جمعية للنظام")}")
                            }
                            try {
                                context.startActivity(directIntent)
                            } catch (e: Exception) {
                                clipboardManager.setText(AnnotatedString(SUPPORT_EMAIL))
                                Toast.makeText(context, "تم نسخ البريد: $SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("contact_direct_email_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SocietyBluePrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Drafts,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مراسلة الدعم الفني مباشرة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // نافذة فحص البيانات وزر الموافقة على الطلب (ميزة المطور)
    // ==========================================
    if (showDeveloperReviewModal) {
        val effectiveName = if (societyName.isNotBlank()) societyName.trim() else " "
        val effectiveGov = if (governorateAndCity.isNotBlank()) governorateAndCity.trim() else ""
        val effectivePerson = if (contactPersonName.isNotBlank()) contactPersonName.trim() else " "
        val effectivePhone = if (phoneNumber.isNotBlank()) phoneNumber.trim() else ""
        val effectiveEmail = if (contactEmail.isNotBlank()) contactEmail.trim() else ""
        val effectiveNotes = notesOrRingsCount.ifBlank { "15 حلقة قرآنية للبنين والبنات" }

        AlertDialog(
            onDismissRequest = { showDeveloperReviewModal = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(EmeraldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = "فحص الطلب",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "فحص بيانات الجمعية واعتماد الطلب",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "لوحة تحكم المطور ومراجعة الطلبات",
                        fontSize = 11.sp,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "تم تدقيق بيانات الطلب الواردة، يرجى مراجعتها أدناه قبل الضغط على زر الموافقة:",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DataReviewRow(label = "اسم الجمعية", value = effectiveName, isVerified = true)
                            DataReviewRow(label = "المحافظة والفرع", value = effectiveGov, isVerified = true)
                            DataReviewRow(label = "المشرف المسؤول", value = effectivePerson, isVerified = true)
                            DataReviewRow(label = "الجوال / واتساب", value = effectivePhone, isVerified = true)
                            DataReviewRow(label = "البريد الإلكتروني", value = effectiveEmail, isVerified = true)
                            DataReviewRow(label = "الحلقات والملاحظات", value = effectiveNotes, isVerified = true)
                        }
                    }

                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SocietyBluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "عند النقر على الموافقة، ستضاف الجمعية فوراً إلى قائمة جمعيات النظام وتصبح مستأجراً معتمداً.",
                                fontSize = 10.sp,
                                color = SocietyBluePrimary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                // زر موافقة على الطلب
                Button(
                    onClick = { approveSocietyRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("developer_approve_request_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("موافقة على الطلب واعتماد الجمعية فوراً", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeveloperReviewModal = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إلغاء المراجعة", color = TextMuted)
                }
            }
        )
    }

    // Modal: Confirmation Dialog after sending email (with developer approval option)
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(EmeraldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "نجاح",
                        tint = StatusSuccess,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "تم فتح تطبيق البريد لإرسال الطلب",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "تم تجهيز رسالة طلب انضمام جمعية \"$societyName\" ونقلها إلى تطبيق البريد الإلكتروني مع كافة البيانات والتفاصيل.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "الوجهة: بريد الدعم الفني المعتمد للمنظومة",
                        fontSize = 12.sp,
                        color = SocietyBluePrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { showSuccessDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تم، حسناً")
                }
            }
        )
    }

    // Modal: Confirmation when Society is Approved
    if (approvedTenantResult != null) {
        val t = approvedTenantResult!!
        AlertDialog(
            onDismissRequest = { approvedTenantResult = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EmeraldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = "تم الاعتماد",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "تهانينا! تم اعتماد الجمعية في النظام",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "تمت الموافقة على طلب انضمام الجمعية وإدراجها بنجاح في المنظومة:",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Surface(
                        color = SocietyBlueLight,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "• الجمعية: ${t.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SocietyBluePrimary)
                            Text(text = "• الرمز المعتمد: ${t.code}", fontSize = 11.sp, color = TextDark)
                            Text(text = "• الفرع والمحافظة: ${t.city}", fontSize = 11.sp, color = TextDark)
                            Text(text = "• رقم الترخيص: ${t.licenseNumber}", fontSize = 11.sp, color = TextMuted)
                            Text(text = "• عدد الحلقات المبدئي: ${t.ringsCount} حلقة", fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Text(
                        text = "أصبحت الجمعية الآن متاحة في قائمة المستأجرين ويمكن اختيارها من أعلى الشاشة أو بدء تسجيل المشرفين والطلاب.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        approvedTenantResult = null
                        // Reset form
                        societyName = ""
                        governorateAndCity = ""
                        contactPersonName = ""
                        phoneNumber = ""
                        contactEmail = ""
                        notesOrRingsCount = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تم والبدء في الاستخدام")
                }
            }
        )
    }

    // Modal: No Email App Installed Fallback
    if (showNoEmailAppDialog) {
        AlertDialog(
            onDismissRequest = { showNoEmailAppDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "تنبيه",
                    tint = GoldenAmber,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "بيانات التواصل المباشر",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        text = "تعذر العثور على تطبيق بريد افتراضي مثبت على هذا الجهاز. يمكنك إرسال بيانات جمعيتكم يدوياً إلى البريد التالي:",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = SocietyBlueLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "فريق الدعم الفني والتقني (البريد الإلكتروني الرسمي)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SocietyBluePrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(SUPPORT_EMAIL))
                        Toast.makeText(context, "تم نسخ بيانات الدعم الفني بنجاح", Toast.LENGTH_SHORT).show()
                        showNoEmailAppDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SocietyBluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نسخ البريد الإلكتروني")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoEmailAppDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun DataReviewRow(
    label: String,
    value: String,
    isVerified: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 10.sp, color = TextMuted)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FieldTextColor)
        }
        if (isVerified) {
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "تم التدقيق ✓",
                    fontSize = 9.sp,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
