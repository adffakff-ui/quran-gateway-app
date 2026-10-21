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

object SocietyRepository {
    var tenants by mutableStateOf<List<Tenant>>(emptyList())
    var allTeachers by mutableStateOf<List<Teacher>>(emptyList())
    var allRings by mutableStateOf<List<QuranRing>>(emptyList())
    var allStudents by mutableStateOf<List<CircleStudent>>(emptyList())
    var allUsers by mutableStateOf<List<UserProfile>>(emptyList())
    
    var isProductionMode by mutableStateOf(true)
    var isBiometricLoginEnabled by mutableStateOf(false)
    var systemMaintenanceMode by mutableStateOf(false)
    var globalRegistrationActive by mutableStateOf(true)

    val defaultRealTenant = Tenant("t_official_main", "الجمعية الخيرية", "الجمعية المركزية", "المركز الرئيسي", "QCA-MAIN", 0, 0, 0, 100, 0)
    val defaultRing = QuranRing("ring_placeholder", "حلقة جديدة", "لم يُعين", "عصراً", 0, 25, "جزء عمّ", 100)
    val defaultStudent = CircleStudent("std_placeholder", "طالب جديد", "ولي الأمر", "0500000000")
    val masterOwner = UserProfile("owner_master", "مالك التطبيق", "Owner", UserRole.OWNER, "775484201", "adffakff@gmail.com")
    val defaultSupervisor = UserProfile("sup_01", "المشرف العام", "المشرف العام", UserRole.SUPERVISOR, "0551234567", "supervisor@maknoon.org.sa")

    fun getInitialTeachers(): List<Teacher> = emptyList()
    fun getInitialRings(): List<QuranRing> = emptyList()
    fun getInitialCircleStudents(): List<CircleStudent> = emptyList()
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

    fun approveNewSociety(name: String, governorate: String, supervisorName: String, phone: String, email: String, notes: String): Tenant = defaultRealTenant
    fun isOwner(input: String): Boolean = false
    fun removeTenant(id: String) {}
    fun updateTenant(tenant: Tenant) {}
    fun addTeacher(teacher: Teacher) {}
    fun updateTeacher(teacher: Teacher) {}
    fun removeTeacher(id: String) {}
    fun addRing(ring: QuranRing) {}
    fun updateRing(ring: QuranRing) {}
    fun removeRing(id: String) {}
    fun addStudent(student: CircleStudent) {}
    fun updateStudent(student: CircleStudent) {}
    fun removeStudent(id: String) {}
    fun updateUserRole(id: String, role: UserRole) {}
    fun addUser(user: UserProfile) {}
    fun removeUser(id: String) {}
    fun addTenant(tenant: Tenant) {}
    fun resetToFactoryDefaults() {}
    fun wipeAllDataPreservingOwner() {}
    fun resetAllSocietiesDataClean() {}
    fun isAuthorizedOwner(input: String): Boolean = false
}
