package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.*
import com.example.ui.attendance.AttendanceScreen
import com.example.ui.circles.CircleDetailScreen
import com.example.ui.dashboard.SupervisorDashboardScreen
import com.example.ui.login.LoginScreen
import com.example.ui.login.BiometricOptionalLoginScreen
import com.example.ui.navigation.AppNavigationDrawerSheet
import com.example.ui.parent.ParentPortalScreen
import com.example.ui.recitation.AudioRecitationScreen
import com.example.ui.reports.ReportsAndCertificatesScreen
import com.example.ui.supervisor.SupervisorRegistrationScreen
import com.example.ui.teacherportal.TeacherPortalScreen
import com.example.ui.teachers.TeacherManagementScreen
import com.example.ui.planner.HifzPlannerScreen
import com.example.ui.challenges.QuranChallengesScreen
import com.example.ui.profile.AccountProfileAndPermissionsScreen
import com.example.ui.profile.RolePermissionManagementScreen
import com.example.ui.backup.TenantBackupRestoreScreen
import com.example.ui.settings.AppSettingsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.AboutSocietyScreen
import com.example.ui.security.ConnectedDevicesScreen
import com.example.ui.owner.OwnerControlPanelScreen
import com.example.ui.owner.OwnerFirestoreTenantsScreen
import com.example.ui.owner.OwnerMasterSettingsScreen
import com.example.ui.theme.*
import com.example.data.SocietyRepository
import kotlinx.coroutines.launch

object AppDestinations {
    const val LOGIN = "login"
    const val SUPERVISOR_REGISTRATION = "supervisor_registration"
    const val SUPERVISOR_DASHBOARD = "supervisor_dashboard"
    const val TEACHERS = "teachers"
    const val CIRCLE_DETAIL = "circle_detail"
    const val ATTENDANCE = "attendance"
    const val TEACHER_PORTAL = "teacher_portal"
    const val AUDIO_RECITATION = "audio_recitation"
    const val PARENT_PORTAL = "parent_portal"
    const val REPORTS = "reports"
    const val HIFZ_PLANNER = "hifz_planner"
    const val QURAN_CHALLENGES = "quran_challenges"
    const val ACCOUNT_PROFILE = "account_profile"
    const val OWNER_CONTROL = "owner_control"
    const val OWNER_MASTER_SETTINGS = "owner_master_settings"
    const val OWNER_FIRESTORE_TENANTS = "owner_firestore_tenants"
    const val TENANT_BACKUP_RESTORE = "tenant_backup_restore"
    const val ROLE_PERMISSIONS = "role_permissions"
    const val SETTINGS = "app_settings"
    const val BIOMETRIC_GATE = "biometric_gate"
    const val CONNECTED_DEVICES = "connected_devices"
    const val ABOUT_SOCIETY = "about_society"
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkModePref by remember { mutableStateOf("system") } // "system", "dark", "light"
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (darkModePref) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            MyApplicationTheme(darkTheme = effectiveDark) {
                SocietyApp(
                    darkModePref = darkModePref,
                    onDarkModeChange = { darkModePref = it }
                )
            }
        }
    }
}

@Composable
fun SocietyApp(
    darkModePref: String = "system",
    onDarkModeChange: (String) -> Unit = {}
) {
    val navController = rememberNavController()

    // Current Active Tenant & State
    var currentTenant by remember { mutableStateOf(SocietyRepository.tenants.firstOrNull() ?: SocietyRepository.defaultRealTenant) }
    var activeUserRole by remember { mutableStateOf(UserRole.SUPERVISOR) }
    var selectedRingForDetail by remember { mutableStateOf(SocietyRepository.allRings.firstOrNull() ?: SocietyRepository.getInitialRings().firstOrNull() ?: SocietyRepository.defaultRing) }
    var selectedStudentForRecitation by remember { mutableStateOf(SocietyRepository.allStudents.firstOrNull() ?: SocietyRepository.getInitialCircleStudents().firstOrNull() ?: SocietyRepository.defaultStudent) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show bottom navigation bar on primary supervisor tabs, giving full screen space to Owner and dedicated tools
    val showBottomBar = currentRoute in listOf(
        AppDestinations.SUPERVISOR_DASHBOARD,
        AppDestinations.TEACHERS,
        AppDestinations.CIRCLE_DETAIL,
        AppDestinations.ATTENDANCE
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                if (showBottomBar) {
                    AppNavigationDrawerSheet(
                        currentRoute = currentRoute,
                        currentTenant = currentTenant,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(AppDestinations.SUPERVISOR_DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onCloseDrawer = {
                            coroutineScope.launch { drawerState.close() }
                        },
                        onSwitchTenant = {
                            navController.navigate(AppDestinations.LOGIN) {
                                popUpTo(0)
                            }
                        },
                        onLogout = {
                            navController.navigate(AppDestinations.LOGIN) {
                                popUpTo(0)
                            }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("app_navigation_bar")
                        ) {
                            // لوحة المشرف
                            NavigationBarItem(
                                selected = currentRoute == AppDestinations.SUPERVISOR_DASHBOARD,
                                onClick = {
                                    navController.navigate(AppDestinations.SUPERVISOR_DASHBOARD) {
                                        popUpTo(AppDestinations.SUPERVISOR_DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute == AppDestinations.SUPERVISOR_DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                        contentDescription = "المشرف"
                                    )
                                },
                                label = { Text("المشرف", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SocietyBluePrimary,
                                    selectedTextColor = SocietyBluePrimary,
                                    indicatorColor = SocietyBlueLight
                                )
                            )

                            // إدارة المعلمين
                            NavigationBarItem(
                                selected = currentRoute == AppDestinations.TEACHERS,
                                onClick = {
                                    navController.navigate(AppDestinations.TEACHERS) {
                                        popUpTo(AppDestinations.SUPERVISOR_DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute == AppDestinations.TEACHERS) Icons.Default.School else Icons.Outlined.School,
                                        contentDescription = "المعلمون"
                                    )
                                },
                                label = { Text("المعلمون", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SocietyBluePrimary,
                                    selectedTextColor = SocietyBluePrimary,
                                    indicatorColor = SocietyBlueLight
                                )
                            )

                            // تفاصيل الحلقة
                            NavigationBarItem(
                                selected = currentRoute == AppDestinations.CIRCLE_DETAIL,
                                onClick = {
                                    navController.navigate(AppDestinations.CIRCLE_DETAIL) {
                                        popUpTo(AppDestinations.SUPERVISOR_DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute == AppDestinations.CIRCLE_DETAIL) Icons.Default.MenuBook else Icons.Outlined.MenuBook,
                                        contentDescription = "الحلقة"
                                    )
                                },
                                label = { Text("الحلقة", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SocietyBluePrimary,
                                    selectedTextColor = SocietyBluePrimary,
                                    indicatorColor = SocietyBlueLight
                                )
                            )

                            // تسجيل الحضور
                            NavigationBarItem(
                                selected = currentRoute == AppDestinations.ATTENDANCE,
                                onClick = {
                                    navController.navigate(AppDestinations.ATTENDANCE) {
                                        popUpTo(AppDestinations.SUPERVISOR_DASHBOARD) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute == AppDestinations.ATTENDANCE) Icons.Default.Checklist else Icons.Outlined.Checklist,
                                        contentDescription = "الحضور"
                                    )
                                },
                                label = { Text("الحضور", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SocietyBluePrimary,
                                    selectedTextColor = SocietyBluePrimary,
                                    indicatorColor = SocietyBlueLight
                                )
                            )

                            // قائمة Hamburger (المزيد) - تفتح القائمة الجانبية الكاملة لجميع الروابط
                            NavigationBarItem(
                                selected = drawerState.isOpen || (currentRoute != null && currentRoute !in listOf(
                                    AppDestinations.SUPERVISOR_DASHBOARD,
                                    AppDestinations.TEACHERS,
                                    AppDestinations.CIRCLE_DETAIL,
                                    AppDestinations.ATTENDANCE
                                )),
                                onClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "قائمة الروابط والتنقل"
                                    )
                                },
                                label = { Text("المزيد ☰", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SocietyBluePrimary,
                                    selectedTextColor = SocietyBluePrimary,
                                    indicatorColor = SocietyBlueLight
                                ),
                                modifier = Modifier.testTag("nav_bottom_hamburger_menu_btn")
                            )
                        }
                    }
                },
                containerColor = SlateBackground
            ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestinations.LOGIN,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // شاشة تسجيل الدخول
                composable(AppDestinations.LOGIN) {
                    LoginScreen(
                        currentTenant = currentTenant,
                        onTenantSelected = { currentTenant = it },
                        onLoginSuccess = { tenant, role ->
                            currentTenant = tenant
                            activeUserRole = role
                            if (SocietyRepository.isBiometricLoginEnabled) {
                                navController.navigate(AppDestinations.BIOMETRIC_GATE) {
                                    popUpTo(AppDestinations.LOGIN) { inclusive = true }
                                }
                            } else {
                                when (role) {
                                    UserRole.OWNER -> navController.navigate(AppDestinations.OWNER_CONTROL) { popUpTo(AppDestinations.LOGIN) { inclusive = true } }
                                    UserRole.TEACHER -> navController.navigate(AppDestinations.TEACHER_PORTAL) { popUpTo(AppDestinations.LOGIN) { inclusive = true } }
                                    UserRole.GUARDIAN -> navController.navigate(AppDestinations.PARENT_PORTAL) { popUpTo(AppDestinations.LOGIN) { inclusive = true } }
                                    else -> navController.navigate(AppDestinations.SUPERVISOR_DASHBOARD) { popUpTo(AppDestinations.LOGIN) { inclusive = true } }
                                }
                            }
                        },
                        onBiometricQuickLoginSuccess = { tenant, role ->
                            currentTenant = tenant
                            activeUserRole = role
                            when (role) {
                                UserRole.OWNER -> navController.navigate(AppDestinations.OWNER_CONTROL) { popUpTo(AppDestinations.LOGIN) { inclusive = true } }
                                UserRole.TEACHER -> navController.navigate(AppDestinations.TEACHER_PORTAL) { popUpTo(AppDestinations.LOGIN) { inclusive = true } }
                                UserRole.GUARDIAN -> navController.navigate(AppDestinations.PARENT_PORTAL) { popUpTo(AppDestinations.LOGIN) { inclusive = true } }
                                else -> navController.navigate(AppDestinations.SUPERVISOR_DASHBOARD) { popUpTo(AppDestinations.LOGIN) { inclusive = true } }
                            }
                        },
                        onNavigateToSupervisorRegistration = {
                            navController.navigate(AppDestinations.SUPERVISOR_REGISTRATION)
                        }
                    )
                }

                composable(AppDestinations.BIOMETRIC_GATE) {
                    BiometricOptionalLoginScreen(
                        userRole = activeUserRole,
                        onSuccess = { 
                            when (activeUserRole) {
                                UserRole.OWNER -> navController.navigate(AppDestinations.OWNER_CONTROL) { popUpTo(AppDestinations.BIOMETRIC_GATE) { inclusive = true } }
                                UserRole.TEACHER -> navController.navigate(AppDestinations.TEACHER_PORTAL) { popUpTo(AppDestinations.BIOMETRIC_GATE) { inclusive = true } }
                                UserRole.GUARDIAN -> navController.navigate(AppDestinations.PARENT_PORTAL) { popUpTo(AppDestinations.BIOMETRIC_GATE) { inclusive = true } }
                                else -> navController.navigate(AppDestinations.SUPERVISOR_DASHBOARD) { popUpTo(AppDestinations.BIOMETRIC_GATE) { inclusive = true } }
                            }
                        },
                        onError = { navController.navigate(AppDestinations.LOGIN) { popUpTo(AppDestinations.BIOMETRIC_GATE) { inclusive = true } } }
                    )
                }

                // شاشة تسجيل المشرف الجديد (المكون المستقل)
                composable(AppDestinations.SUPERVISOR_REGISTRATION) {
                    SupervisorRegistrationScreen(
                        currentTenant = currentTenant,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onRegistrationSuccess = { tenant, profile ->
                            currentTenant = tenant
                            activeUserRole = profile.role
                            if (profile.role == UserRole.TEACHER) {
                                navController.navigate(AppDestinations.TEACHER_PORTAL) {
                                    popUpTo(AppDestinations.LOGIN) { inclusive = true }
                                }
                            } else {
                                navController.navigate(AppDestinations.SUPERVISOR_DASHBOARD) {
                                    popUpTo(AppDestinations.LOGIN) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // لوحة المشرف العام
                composable(AppDestinations.SUPERVISOR_DASHBOARD) {
                    SupervisorDashboardScreen(
                        currentTenant = currentTenant,
                        userRole = activeUserRole,
                        onNavigateToTeachers = { navController.navigate(AppDestinations.TEACHERS) },
                        onNavigateToPlanner = { navController.navigate(AppDestinations.HIFZ_PLANNER) },
                        onNavigateToChallenges = { navController.navigate(AppDestinations.QURAN_CHALLENGES) },
                        onNavigateToProfile = { navController.navigate(AppDestinations.ACCOUNT_PROFILE) },
                        onNavigateToOwnerControl = { navController.navigate(AppDestinations.OWNER_CONTROL) },
                        onNavigateToFirestoreTenants = { navController.navigate(AppDestinations.OWNER_FIRESTORE_TENANTS) },
                        onNavigateToBackup = { navController.navigate(AppDestinations.TENANT_BACKUP_RESTORE) },
                        onNavigateToSettings = { navController.navigate(AppDestinations.SETTINGS) },
                        onSwitchTenant = {
                            navController.navigate(AppDestinations.LOGIN) {
                                popUpTo(0)
                            }
                        },
                        onLogout = {
                            navController.navigate(AppDestinations.LOGIN) {
                                popUpTo(0)
                            }
                        }
                    )
                }

                // إدارة المعلمين
                composable(AppDestinations.TEACHERS) {
                    TeacherManagementScreen(
                        currentTenant = currentTenant,
                        onBack = { navController.popBackStack() }
                    )
                }

                // مخطط الحفظ والمراجعة الذكي
                composable(AppDestinations.HIFZ_PLANNER) {
                    HifzPlannerScreen(
                        currentTenant = currentTenant,
                        onBack = { navController.popBackStack() }
                    )
                }

                // التحديات والمسابقات القرآنية
                composable(AppDestinations.QURAN_CHALLENGES) {
                    QuranChallengesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // معلومات الحساب والصلاحيات
                composable(AppDestinations.ACCOUNT_PROFILE) {
                    AccountProfileAndPermissionsScreen(
                        currentTenant = currentTenant,
                        userRole = activeUserRole,
                        darkModePref = darkModePref,
                        onDarkModeChange = onDarkModeChange,
                        onNavigateToRolePermissions = { navController.navigate(AppDestinations.ROLE_PERMISSIONS) },
                        onNavigateToConnectedDevices = { navController.navigate(AppDestinations.CONNECTED_DEVICES) },
                        onNavigateToAboutSociety = { navController.navigate(AppDestinations.ABOUT_SOCIETY) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // إدارة صلاحيات المستخدمين (RBAC)
                composable(AppDestinations.ROLE_PERMISSIONS) {
                    RolePermissionManagementScreen(
                        currentTenant = currentTenant,
                        onBack = { navController.popBackStack() }
                    )
                }

                // تفاصيل الحلقة وإدارة السعة
                composable(AppDestinations.CIRCLE_DETAIL) {
                    CircleDetailScreen(
                        currentTenant = currentTenant,
                        ring = selectedRingForDetail,
                        userRole = activeUserRole,
                        onBack = { navController.popBackStack() },
                        onNavigateToAttendance = { navController.navigate(AppDestinations.ATTENDANCE) }
                    )
                }

                // تسجيل الحضور والـ QR
                composable(AppDestinations.ATTENDANCE) {
                    AttendanceScreen(
                        currentTenant = currentTenant,
                        onBack = { navController.popBackStack() }
                    )
                }

                // بوابة المعلم
                composable(AppDestinations.TEACHER_PORTAL) {
                    TeacherPortalScreen(
                        currentTenant = currentTenant,
                        onOpenAudioRecitation = { st ->
                            selectedStudentForRecitation = st
                            navController.navigate(AppDestinations.AUDIO_RECITATION)
                        },
                        onOpenAttendance = { navController.navigate(AppDestinations.ATTENDANCE) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // تقييم التلاوة الذكي ودعم القراءات العشر
                composable(AppDestinations.AUDIO_RECITATION) {
                    AudioRecitationScreen(
                        currentTenant = currentTenant,
                        student = selectedStudentForRecitation,
                        onBack = { navController.popBackStack() }
                    )
                }

                // بوابة ولي الأمر
                composable(AppDestinations.PARENT_PORTAL) {
                    ParentPortalScreen(
                        currentTenant = currentTenant,
                        onBack = { navController.popBackStack() }
                    )
                }

                // التقارير وتوليد الشهادات
                composable(AppDestinations.REPORTS) {
                    ReportsAndCertificatesScreen(
                        currentTenant = currentTenant,
                        onBack = { navController.popBackStack() }
                    )
                }

                // لوحة المالك والتحكم الشامل
                composable(AppDestinations.OWNER_CONTROL) {
                    OwnerControlPanelScreen(
                        currentTenant = currentTenant,
                        userRole = activeUserRole,
                        onTenantSelected = { currentTenant = it },
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSupervisorDashboard = { navController.navigate(AppDestinations.SUPERVISOR_DASHBOARD) },
                        onNavigateToTeachers = { navController.navigate(AppDestinations.TEACHERS) },
                        onNavigateToPlanner = { navController.navigate(AppDestinations.HIFZ_PLANNER) },
                        onNavigateToChallenges = { navController.navigate(AppDestinations.QURAN_CHALLENGES) },
                        onNavigateToAttendance = { navController.navigate(AppDestinations.ATTENDANCE) },
                        onNavigateToParentPortal = { navController.navigate(AppDestinations.PARENT_PORTAL) },
                        onNavigateToTeacherPortal = { navController.navigate(AppDestinations.TEACHER_PORTAL) },
                        onNavigateToReports = { navController.navigate(AppDestinations.REPORTS) },
                        onNavigateToFirestoreTenants = { navController.navigate(AppDestinations.OWNER_FIRESTORE_TENANTS) },
                        onNavigateToBackup = { navController.navigate(AppDestinations.TENANT_BACKUP_RESTORE) },
                        onNavigateToOwnerMasterSettings = { navController.navigate(AppDestinations.OWNER_MASTER_SETTINGS) }
                    )
                }

                // شاشة إعدادات المالك المطلقة والصلاحيات
                composable(AppDestinations.OWNER_MASTER_SETTINGS) {
                    OwnerMasterSettingsScreen(
                        currentTenant = currentTenant,
                        userRole = activeUserRole,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // واجهة تحكم المالك المباشرة بقاعدة بيانات Firestore للجمعيات (حصرية ومخفية عن غيره)
                composable(AppDestinations.OWNER_FIRESTORE_TENANTS) {
                    OwnerFirestoreTenantsScreen(
                        currentTenant = currentTenant,
                        userRole = activeUserRole,
                        onTenantSelected = { currentTenant = it },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // النسخ الاحتياطي واستعادة البيانات عبر Firebase Storage
                composable(AppDestinations.TENANT_BACKUP_RESTORE) {
                    TenantBackupRestoreScreen(
                        currentTenant = currentTenant,
                        onBack = { navController.popBackStack() }
                    )
                }

                // شاشة إعدادات التطبيق والسمات والخصوصية
                composable(AppDestinations.SETTINGS) {
                    SettingsScreen(
                        currentTenant = currentTenant,
                        onTenantUpdated = { currentTenant = it },
                        darkModePref = darkModePref,
                        onDarkModeChange = onDarkModeChange,
                        onNavigateToConnectedDevices = { navController.navigate(AppDestinations.CONNECTED_DEVICES) },
                        onNavigateToAboutSociety = { navController.navigate(AppDestinations.ABOUT_SOCIETY) },
                        onNavigateToOwnerSettings = { navController.navigate(AppDestinations.OWNER_MASTER_SETTINGS) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // شاشة إدارة الأجهزة المرتبطة والجلسات النشطة للمشرف
                composable(AppDestinations.CONNECTED_DEVICES) {
                    ConnectedDevicesScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                // صفحة تعريفية تستعرض رؤية وأهداف الجمعية الرقمية ومعلومات التواصل المباشرة
                composable(AppDestinations.ABOUT_SOCIETY) {
                    AboutSocietyScreen(
                        currentTenant = currentTenant,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
}
