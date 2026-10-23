package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppDestinations
import com.example.R
import com.example.data.Tenant
import com.example.ui.theme.*

data class NavDrawerItem(
    val route: String,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val badge: String? = null,
    val isPrimary: Boolean = false
)

/**
 * زر قائمة Hamburger قابل لإعادة الاستخدام في أشرطة التنقل العلوية
 * مع تحقيق معايير سهولة الوصول وحجم النقر (48dp).
 */
@Composable
fun HamburgerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextDark
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("top_bar_hamburger_button")
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "فتح قائمة التنقل الرئيسية",
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * القائمة الجانبية (Hamburger Drawer Sheet)
 * تتيح تصفح جميع روابط المنظومة بسهولة ومرونة على الشاشات الصغيرة،
 * مع مراعاة التباعد المريح للأزرار والعناصر (Button Spacing).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationDrawerSheet(
    currentRoute: String?,
    currentTenant: Tenant,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    onSwitchTenant: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
            .testTag("collapsible_hamburger_drawer"),
        drawerContainerColor = Color.White,
        drawerTonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
        ) {
            // ==========================================
            // رأس القائمة الجانبية: شعار وهوية الجمعية
            // ==========================================
            Surface(
                color = SocietyBlueLight.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(SocietyBluePrimary, EmeraldGreen))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_society_logo),
                            contentDescription = "شعار الجمعية",
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTenant.shortName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "فرع: ${currentTenant.city}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Surface(
                                color = SocietyBluePrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = currentTenant.code,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SocietyBluePrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // زر إغلاق القائمة
                    IconButton(
                        onClick = onCloseDrawer,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("drawer_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق القائمة",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // قائمة الروابط والصفحات مع تباعد مريح
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // القسم 1: الإشراف والحلقات
                DrawerSectionHeader(title = "الإشراف والحلقات القرآنية")

                val mainLinks = listOf(
                    NavDrawerItem(
                        route = AppDestinations.SUPERVISOR_DASHBOARD,
                        title = "لوحة المشرف العام",
                        subtitle = "مؤشرات الأداء والأنشطة",
                        icon = Icons.Outlined.Dashboard,
                        badge = "الرئيسية"
                    ),
                    NavDrawerItem(
                        route = AppDestinations.TEACHERS,
                        title = "إدارة المعلمين",
                        subtitle = "بيانات وإسناد الحلقات",
                        icon = Icons.Outlined.School,
                        badge = "${currentTenant.teachersCount}"
                    ),
                    NavDrawerItem(
                        route = AppDestinations.CIRCLE_DETAIL,
                        title = "تفاصيل الحلقة والطلاب",
                        subtitle = "السعة، المقاعد، والمستويات",
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        badge = "${currentTenant.ringsCount} حلقة"
                    ),
                    NavDrawerItem(
                        route = AppDestinations.ATTENDANCE,
                        title = "تسجيل الحضور والغياب",
                        subtitle = "التحضير السريع ورموز QR",
                        icon = Icons.Outlined.CheckCircle
                    )
                )

                mainLinks.forEach { item ->
                    DrawerLinkRow(
                        item = item,
                        isSelected = currentRoute == item.route,
                        onClick = {
                            onNavigate(item.route)
                            onCloseDrawer()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.7f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // القسم 2: البوابات والتسميع الصوتي
                DrawerSectionHeader(title = "البوابات والتسميع والتقارير")

                val secondaryLinks = listOf(
                    NavDrawerItem(
                        route = AppDestinations.TEACHER_PORTAL,
                        title = "بوابة المعلم اليومية",
                        subtitle = "تقييم الحفظ والخطط الدراسية",
                        icon = Icons.Outlined.RecordVoiceOver
                    ),
                    NavDrawerItem(
                        route = AppDestinations.AUDIO_RECITATION,
                        title = "التسميع الصوتي المباشر",
                        subtitle = "التلاوة الذكية والقراءات العشر",
                        icon = Icons.Outlined.Headphones,
                        badge = "مباشر"
                    ),
                    NavDrawerItem(
                        route = AppDestinations.PARENT_PORTAL,
                        title = "بوابة ولي الأمر",
                        subtitle = "ملف الطالب والإشعارات الصوتية",
                        icon = Icons.Outlined.FamilyRestroom
                    ),
                    NavDrawerItem(
                        route = AppDestinations.REPORTS,
                        title = "التقارير وتوليد الشهادات",
                        subtitle = "إحصائيات الإنجاز والشهادات",
                        icon = Icons.Outlined.Assessment
                    ),
                    NavDrawerItem(
                        route = AppDestinations.TENANT_BACKUP_RESTORE,
                        title = "النسخ الاحتياطي واستعادة البيانات",
                        subtitle = "تصدير JSON إلى Firebase Storage واستيرادها",
                        icon = Icons.Outlined.CloudSync,
                        badge = "JSON"
                    )
                )

                secondaryLinks.forEach { item ->
                    DrawerLinkRow(
                        item = item,
                        isSelected = currentRoute == item.route,
                        onClick = {
                            onNavigate(item.route)
                            onCloseDrawer()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.7f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // القسم 3: الإدارة والحساب
                DrawerSectionHeader(title = "الإدارة والحساب")

                DrawerLinkRow(
                    item = NavDrawerItem(
                        route = AppDestinations.SUPERVISOR_REGISTRATION,
                        title = "تسجيل مشرف جديد",
                        subtitle = "نموذج اعتماد مشرف للجمعية",
                        icon = Icons.Outlined.PersonAdd
                    ),
                    isSelected = currentRoute == AppDestinations.SUPERVISOR_REGISTRATION,
                    onClick = {
                        onNavigate(AppDestinations.SUPERVISOR_REGISTRATION)
                        onCloseDrawer()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // أزرار سفلية: تبديل الجمعية وتسجيل الخروج
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // زر تبديل الجمعية
                OutlinedButton(
                    onClick = {
                        onCloseDrawer()
                        onSwitchTenant()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("drawer_switch_tenant_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SocietyBluePrimary.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SocietyBlueLight.copy(alpha = 0.5f),
                        contentColor = SocietyBluePrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تبديل الجمعية (${currentTenant.shortName})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // زر تسجيل الخروج
                Button(
                    onClick = {
                        onCloseDrawer()
                        onLogout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("drawer_logout_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEE2E2),
                        contentColor = StatusError
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = StatusError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل الخروج من المنظومة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerLinkRow(
    item: NavDrawerItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (isSelected) SocietyBluePrimary else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        },
        label = {
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) SocietyBluePrimary else TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.subtitle != null) {
                    Text(
                        text = item.subtitle,
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        badge = {
            if (item.badge != null) {
                Surface(
                    color = if (isSelected) SocietyBluePrimary else SocietyBlueLight,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else SocietyBluePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = SocietyBlueLight,
            unselectedContainerColor = Color.Transparent,
            selectedTextColor = SocietyBluePrimary,
            unselectedTextColor = TextDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("drawer_link_${item.route}")
    )
}
