package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SocietyRepository
import com.example.data.Tenant
import com.example.ui.theme.*

data class TenantRegistrationStats(
    val tenantId: String,
    val tenantName: String,
    val shortName: String,
    val city: String,
    val activeRegistrations: Int,
    val pendingRegistrations: Int
) {
    val totalRegistrations: Int get() = activeRegistrations + pendingRegistrations
    val activeRatio: Float get() = if (totalRegistrations > 0) activeRegistrations.toFloat() / totalRegistrations else 1f
    val pendingRatio: Float get() = if (totalRegistrations > 0) pendingRegistrations.toFloat() / totalRegistrations else 0f
}

@Composable
fun ActiveVsPendingRegistrationsDashboard(
    modifier: Modifier = Modifier,
    tenants: List<Tenant> = SocietyRepository.tenants,
    isDarkMode: Boolean = false,
    onTenantClick: (Tenant) -> Unit = {}
) {
    // Generate real-time registration stats for each tenant
    val waitlistStudents = remember { SocietyRepository.getInitialWaitlist() }

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, HIGH_PENDING, TOP_ACTIVE
    var searchQuery by remember { mutableStateOf("") }
    var isLiveSyncing by remember { mutableStateOf(false) }
    var selectedTenantDetail by remember { mutableStateOf<TenantRegistrationStats?>(null) }

    val rawStats = remember(tenants) {
        tenants.mapIndexed { index, tenant ->
            // Pending registrations calculated from waitlist + tenant ratio
            val basePending = (tenant.studentsCount * 0.075).toInt().coerceAtLeast(8)
            val pendingWithWaitlist = if (index == 0) basePending + waitlistStudents.size else basePending
            TenantRegistrationStats(
                tenantId = tenant.id,
                tenantName = tenant.name,
                shortName = tenant.shortName,
                city = tenant.city,
                activeRegistrations = tenant.studentsCount,
                pendingRegistrations = pendingWithWaitlist
            )
        }
    }

    val totalActive = remember(rawStats) { rawStats.sumOf { it.activeRegistrations } }
    val totalPending = remember(rawStats) { rawStats.sumOf { it.pendingRegistrations } }
    val grandTotal = totalActive + totalPending
    val overallActivePercent = if (grandTotal > 0) (totalActive * 100f / grandTotal) else 100f
    val overallPendingPercent = if (grandTotal > 0) (totalPending * 100f / grandTotal) else 0f

    val filteredStats = remember(rawStats, selectedFilter, searchQuery) {
        rawStats.filter {
            if (searchQuery.isBlank()) true
            else it.tenantName.contains(searchQuery, ignoreCase = true) ||
                    it.city.contains(searchQuery, ignoreCase = true) ||
                    it.shortName.contains(searchQuery, ignoreCase = true)
        }.let { list ->
            when (selectedFilter) {
                "HIGH_PENDING" -> list.sortedByDescending { it.pendingRegistrations }
                "TOP_ACTIVE" -> list.sortedByDescending { it.activeRegistrations }
                else -> list
            }
        }
    }

    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color.White else TextDark
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else TextMuted
    val borderColor = if (isDarkMode) Color(0xFF334155) else BorderSubtle

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_vs_pending_dashboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkMode) Color(0xFF0F172A) else SocietyBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "مخطط التسجيلات",
                            tint = SocietyBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "مؤشر التسجيلات النشطة مقابل المعلقة",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Live pulse indicator
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldGreen.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                                        text = "مباشر لجميع الجمعيات",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark
                                    )
                                }
                            }
                        }
                        Text(
                            text = "إحصائيات فورية ومقارنة الطلاب النشطين بقوائم الانتظار عبر كافة الفروع",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                // Refresh Button
                IconButton(
                    onClick = {
                        isLiveSyncing = true
                    },
                    modifier = Modifier.testTag("refresh_dashboard_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث فوري",
                        tint = SocietyBluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // KPI Summary Row (Active vs Pending vs Conversion)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Total Active Registrations
                RegistrationKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "إجمالي النشطين",
                    count = "%,d".format(totalActive),
                    percentage = "${"%.1f".format(overallActivePercent)}%",
                    labelColor = EmeraldDark,
                    bgColor = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.3f) else EmeraldLight,
                    icon = Icons.Default.CheckCircle,
                    isDarkMode = isDarkMode
                )

                // 2. Total Pending Registrations
                RegistrationKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "قيد الانتظار والمعلقة",
                    count = "%,d".format(totalPending),
                    percentage = "${"%.1f".format(overallPendingPercent)}%",
                    labelColor = GoldenAmber,
                    bgColor = if (isDarkMode) Color(0xFF78350F).copy(alpha = 0.3f) else GoldenAmberLight,
                    icon = Icons.Default.PendingActions,
                    isDarkMode = isDarkMode
                )

                // 3. Total Across All Tenants
                RegistrationKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "إجمالي الطلبات",
                    count = "%,d".format(grandTotal),
                    percentage = "${tenants.size} جمعية",
                    labelColor = SocietyBluePrimary,
                    bgColor = if (isDarkMode) Color(0xFF1E3A8A).copy(alpha = 0.3f) else SocietyBlueLight,
                    icon = Icons.Default.Groups,
                    isDarkMode = isDarkMode
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // Main Chart: Interactive Visual Comparison Bars
            // ==========================================
            Text(
                text = "مقارنة التسجيلات (أعمدة بيانية لكل جمعية):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(EmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "نشطون ومقيدون", fontSize = 11.sp, color = textSecondary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(GoldenAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "قيد الانتظار والمراجعة", fontSize = 11.sp, color = textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual comparative bars component
            ActiveVsPendingBarChart(
                stats = filteredStats.take(6),
                isDarkMode = isDarkMode,
                onSelectTenant = { stats ->
                    selectedTenantDetail = stats
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // Search & Filter Controls
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("الكل", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_all_btn")
                    )
                    FilterChip(
                        selected = selectedFilter == "HIGH_PENDING",
                        onClick = { selectedFilter = "HIGH_PENDING" },
                        label = { Text("الأعلى انتظاراً", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_pending_btn")
                    )
                    FilterChip(
                        selected = selectedFilter == "TOP_ACTIVE",
                        onClick = { selectedFilter = "TOP_ACTIVE" },
                        label = { Text("الأعلى نشاطاً", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_active_btn")
                    )
                }

                Text(
                    text = "${filteredStats.size} جمعية مسجلة",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // Tenant Detailed Ratio Breakdown (Top 4 shown)
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredStats.take(5).forEach { stat ->
                    TenantRegistrationRow(
                        stat = stat,
                        isDarkMode = isDarkMode,
                        onClick = {
                            val matched = tenants.find { it.id == stat.tenantId }
                            if (matched != null) onTenantClick(matched)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegistrationKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    count: String,
    percentage: String,
    labelColor: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkMode: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, labelColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = percentage,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color.White else TextDark
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = if (isDarkMode) Color(0xFFCBD5E1) else TextMuted
            )
        }
    }
}

@Composable
fun ActiveVsPendingBarChart(
    stats: List<TenantRegistrationStats>,
    isDarkMode: Boolean,
    onSelectTenant: (TenantRegistrationStats) -> Unit
) {
    val maxActive = remember(stats) { stats.maxOfOrNull { it.activeRegistrations }?.coerceAtLeast(1) ?: 100 }
    val maxPending = remember(stats) { stats.maxOfOrNull { it.pendingRegistrations }?.coerceAtLeast(1) ?: 20 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.forEach { stat ->
                    val activeHeightFraction = (stat.activeRegistrations.toFloat() / maxActive).coerceIn(0.12f, 1f)
                    val pendingHeightFraction = (stat.pendingRegistrations.toFloat() / maxPending).coerceIn(0.12f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectTenant(stat) }
                            .padding(horizontal = 4.dp)
                    ) {
                        // Counts tooltip header
                        Text(
                            text = "${stat.activeRegistrations}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                        Text(
                            text = "${stat.pendingRegistrations}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldenAmber
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Dual Bars: Active (Green) and Pending (Amber)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(100.dp)
                        ) {
                            // Active Bar
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(activeHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(EmeraldGreen, EmeraldDark)
                                        )
                                    )
                            )
                            // Pending Bar
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(pendingHeightFraction * 0.75f) // scaled relative visual
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(GoldenAmber, Color(0xFFB45309))
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Label (Tenant short name)
                        Text(
                            text = stat.shortName.take(8),
                            fontSize = 9.sp,
                            color = if (isDarkMode) Color(0xFF94A3B8) else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TenantRegistrationRow(
    stat: TenantRegistrationStats,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stat.tenantName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "المدينة: ${stat.city} • إجمالي الطلبات: ${stat.totalRegistrations}",
                        fontSize = 10.sp,
                        color = if (isDarkMode) Color(0xFF94A3B8) else TextMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(containerColor = EmeraldLight) {
                        Text(
                            text = "${stat.activeRegistrations} نشط",
                            color = EmeraldDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                    Badge(containerColor = GoldenAmberLight) {
                        Text(
                            text = "${stat.pendingRegistrations} معلق",
                            color = GoldenAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual Progress Ratio Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(GoldenAmber.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stat.activeRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(EmeraldGreen)
                )
            }
        }
    }
}
