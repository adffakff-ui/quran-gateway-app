package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

// Core Data Models
data class Tenant(val id: String, val name: String, val shortName: String, val city: String, val code: String, val ringsCount: Int, val studentsCount: Int, val teachersCount: Int, val attendanceRate: Int, val unreadMessagesCount: Int, val licenseNumber: String = "10492", val country: String = "اليمن", val governorate: String = "تعز", val primaryColorHex: String = "#1976D2", val logoIdentifier: String = "🕌")
data class UserProfile(val id: String, val name: String, val title: String, val role: UserRole, val phone: String, val email: String)
enum class UserRole(val label: String) { OWNER("مالك التطبيق (Super Admin)"), SUPERVISOR("المشرف العام"), TEACHER("معلم حلقات"), GUARDIAN("ولي أمر") }
data class Teacher(val id: String, val name: String, val phone: String, val email: String, val qualification: String, val experienceYears: Int, val isAvailable: Boolean, val status: TeacherStatus, val assignedRings: List<String>, val nationalId: String, val hireDate: String, val certificates: List<String>, val leaveRequests: List<LeaveRequest>, val auditLogs: List<AuditEntry>)
enum class TeacherStatus(val label: String) { ACTIVE("مفعل"), SUSPENDED("معلق") }
data class LeaveRequest(val id: String, val type: String, val dateRange: String, val substituteTeacher: String?, val reason: String, val status: String)
data class AuditEntry(val id: String, val action: String, val timestamp: String, val performedBy: String)
data class QuranRing(val id: String, val name: String, val teacherName: String, val timeSlot: String, val studentCount: Int, val maxCapacity: Int, val currentJuz: String, val attendancePercent: Int, val level: String = "متوسط", val location: String = "جامع الراجحي - قاعة الفاروق (حضوري)", val zoomLink: String = "https://zoom.us/j/9823419827", val description: String = "")
enum class AttendanceStatus(val label: String, val symbol: String) { PRESENT("حاضر", "✓"), ABSENT("غائب", "✖"), LATE("متأخر", "⏰"), EXCUSED("مأذون", "📝") }
data class CircleStudent(val id: String, val name: String, val guardianName: String, val guardianPhone: String, val attendanceStatus: AttendanceStatus = AttendanceStatus.PRESENT, val lastEvaluation: String = "", val registrationOrder: Int = 1, val note: String = "")
data class WaitlistStudent(val id: String, val studentName: String, val guardianName: String, val guardianPhone: String, val position: Int, val joinedDate: String, val notes: String = "")
data class CircleResource(val id: String, val title: String, val type: String, val size: String, val uploadedDate: String)
data class CircleAssignment(val id: String, val title: String, val dueDate: String, val type: String, val completedCount: Int, val totalCount: Int)
data class CapacityAuditRecord(val id: String, val actorName: String, val actorRole: String, val circleName: String, val oldCapacity: Int, val newCapacity: Int, val actionOnOverflow: String, val affectedCount: Int, val timestamp: String, val reason: String = "")
data class ActivityFeedItem(val id: String, val title: String, val description: String, val category: String, val timestamp: String)
data class QiraaInfo(val id: String, val name: String, val riwayat: List<String>)
data class RecitationSegment(val start: Float, val end: Float, val text: String, val errorType: String, val expectedBySelectedQira: String, val suggestion: String, val confidence: Float)
data class RecitationAnalysisResult(val analysisId: String, val selectedQira: String, val selectedRiwaya: String, val detectedQira: String?, val detectedConfidence: Float, val score: Float, val scoreAdjustedForQira: Float, val overallLevel: String, val segments: List<RecitationSegment>)
data class ParentRecordingItem(val id: String, val studentName: String, val surahName: String, val ayahRange: String, val qiraaName: String, val mode: String, val privacy: String, val durationSeconds: Int, val status: String, val quickScore: String, val quickFeedback: String, val teacherFeedback: String? = null, val uploadedAt: String)
data class EligibleCertificateStudent(val id: String, val studentName: String, val ringName: String, val level: String, val completionDate: String, val finalScore: String, val isSelected: Boolean = true)
data class ExportLogRecord(val id: String, val reportType: String, val exportDate: String, val exportedBy: String, val recordsCount: Int, val format: String)
data class CertificateTemplate(
    val title: String = "شهادة إتمام حفظ وتجويد القرآن الكريم",
    val bodyText: String = "تشهد إدارة الجمعية بأن الطالب قد أتم بنجاح حفظ وتجويد المقرر القرآني المعتمد بتقدير ممتاز.",
    val organizationName: String = "جمعية تحفيظ القرآن الكريم",
    val dateText: String = "16 أكتوبر 2026 م",
    val supervisorSignature: String = "أ. عبد الرحمن السعيد"
)

/**
 * نموذج الأوقاف والكفالات والتبرعات الخاصة بحلقات القرآن الكريم
 */
data class WaqfContribution(
    val id: String,
    val donorName: String,
    val donorPhone: String,
    val amount: Double,
    val type: String, // "كفالة حلقة قرآنية", "كفالة طالب حافظ", "صدقة جارية / وقف مصاحف", "دعم عام للجمعية"
    val targetRingName: String,
    val date: String,
    val receiptNumber: String,
    val notes: String = ""
)

/**
 * سجل الإنجاز والتسميع اليومي السريع للطالب
 */
data class DailyHifzRecord(
    val id: String,
    val studentName: String,
    val ringName: String,
    val surahName: String,
    val ayahFrom: Int,
    val ayahTo: Int,
    val tajweedRating: Int, // 1 to 5 stars
    val date: String,
    val notes: String = "",
    val parentNotifiedViaWhatsApp: Boolean = false
)


object SocietyRepository {
    val defaultRealTenant = Tenant("t_official_main", "الجمعية الخيرية", "الجمعية المركزية", "المركز الرئيسي", "QCA-MAIN", 12, 180, 15, 96, 2)
    val defaultRing = QuranRing("ring_placeholder", "حلقة جديدة", "لم يُعين", "عصراً", 0, 25, "جزء عمّ", 100)
    val defaultStudent = CircleStudent("std_placeholder", "طالب جديد", "ولي الأمر", "0500000000")
    val masterOwner = UserProfile("owner_master", "مالك التطبيق", "Owner", UserRole.OWNER, "775484201", "adffakff@gmail.com")
    val defaultSupervisor = UserProfile("sup_01", "المشرف العام", "المشرف العام", UserRole.SUPERVISOR, "0551234567", "supervisor@maknoon.org.sa")

    var tenants by mutableStateOf<List<Tenant>>(listOf(defaultRealTenant))
    var allTeachers by mutableStateOf<List<Teacher>>(
        listOf(
            Teacher(
                id = "tch_01",
                name = "الشيخ عبد الله القدسي",
                phone = "771234567",
                email = "qudsi@maknoon.org.sa",
                qualification = "بكالوريوس قراءات ودراسات إسلامية",
                experienceYears = 8,
                isAvailable = true,
                status = TeacherStatus.ACTIVE,
                assignedRings = listOf("حلقة الإمام عاصم"),
                nationalId = "1045678901",
                hireDate = "01/01/2022",
                certificates = listOf("إجازة بقراءة عاصم", "شهادة التجويد المتقدم"),
                leaveRequests = emptyList(),
                auditLogs = emptyList()
            ),
            Teacher(
                id = "tch_02",
                name = "الشيخ محمد بن سالم الزبير",
                phone = "738766810",
                email = "salem@maknoon.org.sa",
                qualification = "ماجستير التفسير وعلوم القرآن",
                experienceYears = 12,
                isAvailable = true,
                status = TeacherStatus.ACTIVE,
                assignedRings = listOf("حلقة الإمام نافع"),
                nationalId = "1056789012",
                hireDate = "15/03/2021",
                certificates = listOf("إجازة في القراءات السبع"),
                leaveRequests = emptyList(),
                auditLogs = emptyList()
            )
        )
    )
    var allRings by mutableStateOf<List<QuranRing>>(
        listOf(
            QuranRing(
                id = "ring_01",
                name = "حلقة الإمام عاصم",
                teacherName = "الشيخ عبد الله القدسي",
                timeSlot = "عصراً (4:00 - 5:30)",
                studentCount = 18,
                maxCapacity = 25,
                currentJuz = "الجزء الخامس عشر",
                attendancePercent = 94,
                level = "متقدم",
                location = "جامع الراجحي - قاعة الفاروق (حضوري)",
                description = "حلقة مخصصة لحفظ وتجويد القرآن بقراءة عاصم"
            ),
            QuranRing(
                id = "ring_02",
                name = "حلقة الإمام نافع",
                teacherName = "الشيخ محمد بن سالم الزبير",
                timeSlot = "مغرباً (6:30 - 8:00)",
                studentCount = 20,
                maxCapacity = 25,
                currentJuz = "الجزء الثلاثون (عم)",
                attendancePercent = 96,
                level = "متوسط",
                location = "جامع البركة - رواق النور",
                description = "حلقة الإتقان ومخارج الحروف"
            )
        )
    )
    var allStudents by mutableStateOf<List<CircleStudent>>(
        listOf(
            CircleStudent("std_1", "عبد الله أحمد اليماني", "أحمد اليماني", "775484201", AttendanceStatus.PRESENT, "حفظ ممتاز (سورة البقرة)", 1),
            CircleStudent("std_2", "عمر خالد القدسي", "خالد القدسي", "738766810", AttendanceStatus.PRESENT, "تجويد متقن (سورة الكهف)", 2),
            CircleStudent("std_3", "محمد بن سالم", "سالم الزبير", "0500112233", AttendanceStatus.PRESENT, "إتقان الأحكام (سورة يس)", 3)
        )
    )
    var allUsers by mutableStateOf<List<UserProfile>>(
        listOf(
            defaultSupervisor,
            UserProfile("sup_02", "أ. محمود صالح الأهدل", "مشرف الشؤون التعليمية", UserRole.SUPERVISOR, "770001122", "m.ahdal@maknoon.org.sa"),
            masterOwner
        )
    )
    
    var isProductionMode by mutableStateOf(true)
    var isBiometricLoginEnabled by mutableStateOf(true)
    var systemMaintenanceMode by mutableStateOf(false)
    var globalRegistrationActive by mutableStateOf(true)

    // الأوقاف والتبرعات وكفالة الحلقات
    var allWaqfContributions by mutableStateOf<List<WaqfContribution>>(
        listOf(
            WaqfContribution("wqf_1", "مؤسسة البركة الوقفية", "738766810", 6500.0, "كفالة حلقة قرآنية كاملة", "حلقة الإمام نافع", "2026/10/18", "REC-2026-001", "كفالة سنوية مستمرة"),
            WaqfContribution("wqf_2", "فاعل خير - تعز", "775484201", 3200.0, "كفالة طالب حافظ متميز", "حلقة الإمام عاصم", "2026/10/20", "REC-2026-002", "سداد مكافأة شهرية"),
            WaqfContribution("wqf_3", "فاعل خير", "0551234567", 1500.0, "صدقة جارية ومصاحف", "جميع الحلقات", "2026/10/21", "REC-2026-003", "توزيع مصاحف معتمدة")
        )
    )

    // المتابعة والتسميع اليومي السريع
    var allDailyHifzRecords by mutableStateOf<List<DailyHifzRecord>>(
        listOf(
            DailyHifzRecord("hifz_1", "عبد الله أحمد اليماني", "حلقة الإمام عاصم", "سورة البقرة", 1, 25, 5, "2026/10/21", "إتقان ممتاز لمخارج الحروف وأحكام النون الساكنة", true),
            DailyHifzRecord("hifz_2", "عمر خالد القدسي", "حلقة الإمام نافع", "سورة الكهف", 1, 15, 4, "2026/10/21", "تجويد طيب مع تنبيه على مراتب القلقلة", true),
            DailyHifzRecord("hifz_3", "محمد بن سالم", "حلقة الإمام عاصم", "سورة يس", 1, 30, 5, "2026/10/21", "حفظ متقن ومتميز جداً ما شاء الله", false)
        )
    )

    fun addWaqfContribution(contribution: WaqfContribution) {
        allWaqfContributions = listOf(contribution) + allWaqfContributions
    }

    fun removeWaqfContribution(id: String) {
        allWaqfContributions = allWaqfContributions.filterNot { it.id == id }
    }

    fun addDailyHifzRecord(record: DailyHifzRecord) {
        allDailyHifzRecords = listOf(record) + allDailyHifzRecords
    }

    fun getInitialTeachers(): List<Teacher> = allTeachers
    fun getInitialRings(): List<QuranRing> = allRings
    fun getInitialCircleStudents(): List<CircleStudent> = allStudents
    fun getInitialWaitlist(): List<WaitlistStudent> = emptyList()
    fun getInitialResources(): List<CircleResource> = emptyList()
    fun getInitialAssignments(): List<CircleAssignment> = emptyList()
    fun getInitialCapacityAudits(): List<CapacityAuditRecord> = emptyList()
    fun getInitialActivities(): List<ActivityFeedItem> = emptyList()
    fun getTenQiraat(): List<QiraaInfo> = emptyList()
    fun getSampleRecitationAnalysis(selectedQira: String, selectedRiwaya: String): RecitationAnalysisResult? = null
    fun getInitialParentRecordings(): List<ParentRecordingItem> = emptyList()
    fun getInitialEligibleStudents(): List<EligibleCertificateStudent> = emptyList()
    fun getInitialExportLogs(): List<ExportLogRecord> = emptyList()

    fun approveNewSociety(name: String, governorate: String, supervisorName: String, phone: String, email: String, notes: String): Tenant {
        val newTenant = Tenant(
            id = "t_${System.currentTimeMillis()}",
            name = name,
            shortName = name.take(20),
            city = governorate,
            code = "QCA-${(100..999).random()}",
            ringsCount = notes.toIntOrNull() ?: 0,
            studentsCount = 0,
            teachersCount = 0,
            attendanceRate = 100,
            unreadMessagesCount = 0,
            governorate = governorate
        )
        addTenant(newTenant)
        return newTenant
    }
    fun isOwner(input: String): Boolean = false
    fun removeTenant(id: String) {
        tenants = tenants.filterNot { it.id == id }
    }
    fun updateTenant(tenant: Tenant) {
        val list = tenants.map { if (it.id == tenant.id) tenant else it }
        tenants = if (list.any { it.id == tenant.id }) list else list + tenant
    }
    fun addTeacher(teacher: Teacher) {
        allTeachers = allTeachers.filterNot { it.id == teacher.id } + teacher
    }
    fun updateTeacher(teacher: Teacher) {
        allTeachers = allTeachers.map { if (it.id == teacher.id) teacher else it }
    }
    fun removeTeacher(id: String) {
        allTeachers = allTeachers.filterNot { it.id == id }
    }
    fun addRing(ring: QuranRing) {
        allRings = allRings.filterNot { it.id == ring.id } + ring
    }
    fun updateRing(ring: QuranRing) {
        allRings = allRings.map { if (it.id == ring.id) ring else it }
    }
    fun removeRing(id: String) {
        allRings = allRings.filterNot { it.id == id }
    }
    fun addStudent(student: CircleStudent) {
        allStudents = allStudents.filterNot { it.id == student.id } + student
    }
    fun updateStudent(student: CircleStudent) {
        allStudents = allStudents.map { if (it.id == student.id) student else it }
    }
    fun removeStudent(id: String) {
        allStudents = allStudents.filterNot { it.id == id }
    }
    fun updateUserRole(id: String, role: UserRole) {
        allUsers = allUsers.map { if (it.id == id) it.copy(role = role) else it }
    }
    fun addUser(user: UserProfile) {
        allUsers = allUsers.filterNot { it.id == user.id || it.email.equals(user.email, ignoreCase = true) } + user
    }
    fun removeUser(id: String) {
        allUsers = allUsers.filterNot { it.id == id }
    }
    fun addTenant(tenant: Tenant) {
        if (!tenants.any { it.id == tenant.id }) {
            tenants = tenants + tenant
        }
    }
    fun resetToFactoryDefaults() {
        allUsers = listOf(defaultSupervisor, masterOwner)
        allTeachers = emptyList()
        allStudents = emptyList()
        allRings = emptyList()
    }
    fun wipeAllDataPreservingOwner() {
        allUsers = listOf(masterOwner)
        allTeachers = emptyList()
        allStudents = emptyList()
        allRings = emptyList()
    }
    fun resetAllSocietiesDataClean() {
        allUsers = listOf(defaultSupervisor, masterOwner)
        allTeachers = emptyList()
        allStudents = emptyList()
        allRings = emptyList()
    }
    fun isAuthorizedOwner(input: String): Boolean = OwnerSecurityManager.verifyOwnerSecret(input)

    fun findRegisteredUser(query: String, defaultTenant: Tenant): ResolvedUser {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            return ResolvedUser(
                name = defaultSupervisor.name,
                phone = defaultSupervisor.phone,
                email = defaultSupervisor.email,
                role = UserRole.SUPERVISOR,
                societyName = defaultTenant.shortName
            )
        }

        // Search in registered users
        val userMatch = allUsers.firstOrNull {
            it.phone.contains(q) || it.email.lowercase().contains(q) || it.name.lowercase().contains(q)
        }
        if (userMatch != null) {
            return ResolvedUser(userMatch.name, userMatch.phone, userMatch.email, userMatch.role, defaultTenant.shortName)
        }

        // Search in teachers
        val teacherMatch = allTeachers.firstOrNull {
            it.phone.contains(q) || it.email.lowercase().contains(q) || it.name.lowercase().contains(q)
        }
        if (teacherMatch != null) {
            return ResolvedUser(teacherMatch.name, teacherMatch.phone, teacherMatch.email, UserRole.TEACHER, defaultTenant.shortName)
        }

        // Search in students / guardians
        val studentMatch = allStudents.firstOrNull {
            it.guardianPhone.contains(q) || it.guardianName.lowercase().contains(q) || it.name.lowercase().contains(q)
        }
        if (studentMatch != null) {
            return ResolvedUser(studentMatch.guardianName, studentMatch.guardianPhone, "", UserRole.GUARDIAN, defaultTenant.shortName)
        }

        // Check if supervisor
        if (defaultSupervisor.phone.contains(q) || defaultSupervisor.email.lowercase().contains(q) || q.contains("super") || q.contains("مشرف")) {
            return ResolvedUser(defaultSupervisor.name, defaultSupervisor.phone, defaultSupervisor.email, UserRole.SUPERVISOR, defaultTenant.shortName)
        }

        val isEmail = q.contains("@")
        return ResolvedUser(
            name = if (isEmail) q.substringBefore("@") else "مستخدم مسجل ($q)",
            phone = if (!isEmail) q else "",
            email = if (isEmail) q else "",
            role = UserRole.SUPERVISOR,
            societyName = defaultTenant.shortName
        )
    }
}

data class ResolvedUser(
    val name: String,
    val phone: String,
    val email: String,
    val role: UserRole,
    val societyName: String = ""
)
