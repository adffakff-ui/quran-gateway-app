package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

// Tenant (الجمعية / المستأجر)
data class Tenant(
    val id: String,
    val name: String,
    val shortName: String,
    val city: String,
    val code: String,
    val ringsCount: Int,
    val studentsCount: Int,
    val teachersCount: Int,
    val attendanceRate: Int,
    val unreadMessagesCount: Int,
    val licenseNumber: String = "10492",
    val country: String = "اليمن",
    val governorate: String = "تعز",
    val primaryColorHex: String = "#1976D2",
    val logoIdentifier: String = "🕌"
)

// Current Logged-in User
data class UserProfile(
    val id: String,
    val name: String,
    val title: String,
    val role: UserRole,
    val phone: String,
    val email: String
)

enum class UserRole(val label: String) {
    OWNER("مالك التطبيق (Super Admin)"),
    SUPERVISOR("المشرف العام"),
    TEACHER("معلم حلقات"),
    GUARDIAN("ولي أمر")
}

// Teacher (المعلم)
data class Teacher(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val qualification: String, // مؤهل
    val experienceYears: Int,
    val isAvailable: Boolean,
    val status: TeacherStatus,
    val assignedRings: List<String>,
    val nationalId: String,
    val hireDate: String,
    val certificates: List<String>,
    val leaveRequests: List<LeaveRequest>,
    val auditLogs: List<AuditEntry>
)

enum class TeacherStatus(val label: String) {
    ACTIVE("مفعل"),
    SUSPENDED("معلق")
}

data class LeaveRequest(
    val id: String,
    val type: String, // إجازة اعتيادية, إجازة اضطرارية, تبادل حلقة
    val dateRange: String,
    val substituteTeacher: String?,
    val reason: String,
    val status: String // قيد المراجعة, معتمد, مرفوض
)

data class AuditEntry(
    val id: String,
    val action: String,
    val timestamp: String,
    val performedBy: String
)

// Quran Ring (الحلقة)
data class QuranRing(
    val id: String,
    val name: String,
    val teacherName: String,
    val timeSlot: String,
    val studentCount: Int,
    val maxCapacity: Int,
    val currentJuz: String,
    val attendancePercent: Int,
    val level: String = "متوسط",
    val location: String = "جامع الراجحي - قاعة الفاروق (حضوري)",
    val zoomLink: String = "https://zoom.us/j/9823419827",
    val description: String = "حلقة مخصصة لضبط مخارج الحروف وأحكام التجويد وإتقان الحفظ المتصل مع التفسير الميسر."
)

// حالة الحضور
enum class AttendanceStatus(val label: String, val symbol: String) {
    PRESENT("حاضر", "✓"),
    ABSENT("غائب", "✖"),
    LATE("متأخر", "⏰"),
    EXCUSED("مأذون", "📝")
}

// طالب الحلقة
data class CircleStudent(
    val id: String,
    val name: String,
    val guardianName: String,
    val guardianPhone: String,
    val attendanceStatus: AttendanceStatus = AttendanceStatus.PRESENT,
    val lastEvaluation: String = "ممتاز 98%",
    val registrationOrder: Int = 1,
    val note: String = ""
)

// طالب في قائمة الانتظار
data class WaitlistStudent(
    val id: String,
    val studentName: String,
    val guardianName: String,
    val guardianPhone: String,
    val position: Int,
    val joinedDate: String,
    val notes: String = "مستعد للانضمام فور توفر مقعد"
)

// موارد الحلقة
data class CircleResource(
    val id: String,
    val title: String,
    val type: String, // PDF, Audio, Video
    val size: String,
    val uploadedDate: String
)

// واجب / اختبار في الحلقة
data class CircleAssignment(
    val id: String,
    val title: String,
    val dueDate: String,
    val type: String, // حفظ, مراجعة, اختبار فصلي
    val completedCount: Int,
    val totalCount: Int
)

// سجل تدقيق تعديل السعة
data class CapacityAuditRecord(
    val id: String,
    val actorName: String,
    val actorRole: String,
    val circleName: String,
    val oldCapacity: Int,
    val newCapacity: Int,
    val actionOnOverflow: String,
    val affectedCount: Int,
    val timestamp: String,
    val reason: String = ""
)

// Activity Feed Item (النشاطات الأخيرة)
data class ActivityFeedItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // تعيين, إضافة, شهادة, حضور, إشعار
    val timestamp: String
)

// القراءات العشر والروايات
data class QiraaInfo(
    val id: String,
    val name: String,
    val riwayat: List<String>
)

data class RecitationSegment(
    val start: Float,
    val end: Float,
    val text: String,
    val errorType: String,
    val expectedBySelectedQira: String,
    val suggestion: String,
    val confidence: Float
)

data class RecitationAnalysisResult(
    val analysisId: String,
    val selectedQira: String,
    val selectedRiwaya: String,
    val detectedQira: String?,
    val detectedConfidence: Float,
    val score: Float,
    val scoreAdjustedForQira: Float,
    val overallLevel: String,
    val segments: List<RecitationSegment>
)

// تسجيل ولي الأمر القرآني
data class ParentRecordingItem(
    val id: String,
    val studentName: String,
    val surahName: String,
    val ayahRange: String,
    val qiraaName: String,
    val mode: String, // اختبار منزلي (Practice), تسليم رسمي (Submission)
    val privacy: String, // خاص للمعلم, مشترك مع ولي الأمر الآخر, مشاهد للعامة
    val durationSeconds: Int,
    val status: String, // quick_done, pending_review, reviewed, accepted
    val quickScore: String,
    val quickFeedback: String,
    val teacherFeedback: String? = null,
    val uploadedAt: String
)

// طالب مستوفٍ لشروط الشهادة
data class EligibleCertificateStudent(
    val id: String,
    val studentName: String,
    val ringName: String,
    val level: String,
    val completionDate: String,
    val finalScore: String,
    val isSelected: Boolean = true
)

// قالب الشهادة
data class CertificateTemplate(
    val title: String = "شهادة إتمام حفظ وتجويد القرآن الكريم",
    val bodyText: String = "تشهد إدارة الجمعية بأن الطالب قد أتم بنجاح حفظ وتجويد المقرر القرآني المعتمد بتقدير ممتاز.",
    val organizationName: String = "جمعية تحفيظ القرآن الكريم",
    val dateText: String = "16 أكتوبر 2026 م",
    val supervisorSignature: String = "أ. عبد الرحمن السعيد"
)

// سجل التصدير
data class ExportLogRecord(
    val id: String,
    val reportType: String,
    val exportDate: String,
    val exportedBy: String,
    val recordsCount: Int,
    val format: String // PDF, CSV
)



// Sample In-Memory Data Repository
object SocietyRepository {
    private val initialTenants = listOf(
        // الجمعيات الرائدة المطلوبة
        Tenant(
            id = "t_hsa",
            name = "جمعية هائل سعيد أنعم الخيرية لتحفيظ القرآن الكريم",
            shortName = "جمعية هائل سعيد أنعم",
            city = "تعز",
            code = "HSA-TAZ",
            ringsCount = 160,
            studentsCount = 3850,
            teachersCount = 145,
            attendanceRate = 97,
            unreadMessagesCount = 8,
            licenseNumber = "YE-TAZ-001",
            country = "اليمن",
            governorate = "تعز"
        ),
        Tenant(
            id = "t_muadh",
            name = "جمعية معاذ العلمية لخدمة القرآن الكريم والسنة النبوية",
            shortName = "جمعية معاذ العلمية",
            city = "تعز",
            code = "MUA-TAZ",
            ringsCount = 140,
            studentsCount = 3200,
            teachersCount = 120,
            attendanceRate = 96,
            unreadMessagesCount = 11,
            licenseNumber = "YE-TAZ-002",
            country = "اليمن",
            governorate = "تعز"
        ),

        // أمانة العاصمة صنعاء ومحافظة صنعاء
        Tenant(
            id = "t_ye_san",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - المركز الرئيسي (صنعاء)",
            shortName = "خيرية القرآن (صنعاء)",
            city = "صنعاء",
            code = "QCA-SAN",
            ringsCount = 180,
            studentsCount = 4200,
            teachersCount = 160,
            attendanceRate = 95,
            unreadMessagesCount = 14,
            licenseNumber = "YE-SAN-001",
            country = "اليمن",
            governorate = "أمانة العاصمة"
        ),
        Tenant(
            id = "t_ye_snm",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة صنعاء",
            shortName = "خيرية القرآن (محافظة صنعاء)",
            city = "محافظة صنعاء",
            code = "QCA-SNM",
            ringsCount = 52,
            studentsCount = 1150,
            teachersCount = 44,
            attendanceRate = 92,
            unreadMessagesCount = 5,
            licenseNumber = "YE-SNM-001",
            country = "اليمن",
            governorate = "صنعاء"
        ),

        // عدن
        Tenant(
            id = "t_ye_adn",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع عدن",
            shortName = "خيرية القرآن (عدن)",
            city = "عدن",
            code = "QCA-ADN",
            ringsCount = 78,
            studentsCount = 1720,
            teachersCount = 65,
            attendanceRate = 94,
            unreadMessagesCount = 9,
            licenseNumber = "YE-ADN-001",
            country = "اليمن",
            governorate = "عدن"
        ),
        Tenant(
            id = "t_ye_mua_adn",
            name = "مؤسسة معاذ التنموية والقرآنية - فرع عدن",
            shortName = "مؤسسة معاذ (عدن)",
            city = "عدن",
            code = "MUA-ADN",
            ringsCount = 42,
            studentsCount = 910,
            teachersCount = 36,
            attendanceRate = 93,
            unreadMessagesCount = 4,
            licenseNumber = "YE-ADN-002",
            country = "اليمن",
            governorate = "عدن"
        ),

        // تعز (فرع الجمعية الخيرية)
        Tenant(
            id = "t_ye_taz",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع تعز",
            shortName = "خيرية القرآن (تعز)",
            city = "تعز",
            code = "QCA-TAZ",
            ringsCount = 95,
            studentsCount = 2100,
            teachersCount = 82,
            attendanceRate = 94,
            unreadMessagesCount = 7,
            licenseNumber = "YE-TAZ-003",
            country = "اليمن",
            governorate = "تعز"
        ),

        // حضرموت (الوادي والساحل وتريم)
        Tenant(
            id = "t_ye_siy",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - وادي حضرموت (سيئون)",
            shortName = "خيرية القرآن (سيئون)",
            city = "سيئون",
            code = "QCA-SIY",
            ringsCount = 115,
            studentsCount = 2680,
            teachersCount = 98,
            attendanceRate = 96,
            unreadMessagesCount = 6,
            licenseNumber = "YE-HAD-001",
            country = "اليمن",
            governorate = "حضرموت"
        ),
        Tenant(
            id = "t_ye_trm",
            name = "جمعية الفرقان لخدمة القرآن الكريم وتأهيل الحفاظ (تريم)",
            shortName = "جمعية الفرقان (تريم)",
            city = "تريم",
            code = "FRQ-TRM",
            ringsCount = 68,
            studentsCount = 1520,
            teachersCount = 58,
            attendanceRate = 96,
            unreadMessagesCount = 5,
            licenseNumber = "YE-HAD-002",
            country = "اليمن",
            governorate = "حضرموت"
        ),
        Tenant(
            id = "t_ye_muk",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - ساحل حضرموت (المكلا)",
            shortName = "خيرية القرآن (المكلا)",
            city = "المكلا",
            code = "QCA-MUK",
            ringsCount = 90,
            studentsCount = 2050,
            teachersCount = 75,
            attendanceRate = 95,
            unreadMessagesCount = 8,
            licenseNumber = "YE-HAD-003",
            country = "اليمن",
            governorate = "حضرموت"
        ),

        // مأرب
        Tenant(
            id = "t_ye_mar",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة مأرب",
            shortName = "خيرية القرآن (مأرب)",
            city = "مأرب",
            code = "QCA-MAR",
            ringsCount = 84,
            studentsCount = 1890,
            teachersCount = 70,
            attendanceRate = 94,
            unreadMessagesCount = 10,
            licenseNumber = "YE-MAR-001",
            country = "اليمن",
            governorate = "مأرب"
        ),
        Tenant(
            id = "t_ye_bas",
            name = "مؤسسة بصمات لتعليم القرآن وتأهيل الحفاظ (مأرب)",
            shortName = "مؤسسة بصمات (مأرب)",
            city = "مأرب",
            code = "BAS-MAR",
            ringsCount = 38,
            studentsCount = 790,
            teachersCount = 30,
            attendanceRate = 92,
            unreadMessagesCount = 3,
            licenseNumber = "YE-MAR-002",
            country = "اليمن",
            governorate = "مأرب"
        ),

        // إب
        Tenant(
            id = "t_ye_ibb",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة إب",
            shortName = "خيرية القرآن (إب)",
            city = "إب",
            code = "QCA-IBB",
            ringsCount = 88,
            studentsCount = 1960,
            teachersCount = 74,
            attendanceRate = 93,
            unreadMessagesCount = 6,
            licenseNumber = "YE-IBB-001",
            country = "اليمن",
            governorate = "إب"
        ),
        Tenant(
            id = "t_ye_yan",
            name = "جمعية ينابيع الخير لتعليم القرآن الكريم (إب)",
            shortName = "ينابيع الخير (إب)",
            city = "إب",
            code = "YAN-IBB",
            ringsCount = 40,
            studentsCount = 820,
            teachersCount = 32,
            attendanceRate = 91,
            unreadMessagesCount = 4,
            licenseNumber = "YE-IBB-002",
            country = "اليمن",
            governorate = "إب"
        ),

        // الحديدة
        Tenant(
            id = "t_ye_hud",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة الحديدة",
            shortName = "خيرية القرآن (الحديدة)",
            city = "الحديدة",
            code = "QCA-HUD",
            ringsCount = 75,
            studentsCount = 1620,
            teachersCount = 60,
            attendanceRate = 91,
            unreadMessagesCount = 5,
            licenseNumber = "YE-HUD-001",
            country = "اليمن",
            governorate = "الحديدة"
        ),
        Tenant(
            id = "t_ye_nur",
            name = "جمعية النور لتعليم القرآن وعلومه (زبيد / الحديدة)",
            shortName = "جمعية النور (زبيد)",
            city = "زبيد",
            code = "NUR-ZBD",
            ringsCount = 44,
            studentsCount = 960,
            teachersCount = 36,
            attendanceRate = 93,
            unreadMessagesCount = 3,
            licenseNumber = "YE-HUD-002",
            country = "اليمن",
            governorate = "الحديدة"
        ),

        // شبوة
        Tenant(
            id = "t_ye_shb",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة شبوة (عتق)",
            shortName = "خيرية القرآن (عتق)",
            city = "عتق",
            code = "QCA-SHB",
            ringsCount = 46,
            studentsCount = 990,
            teachersCount = 38,
            attendanceRate = 92,
            unreadMessagesCount = 4,
            licenseNumber = "YE-SHB-001",
            country = "اليمن",
            governorate = "شبوة"
        ),

        // ذمار
        Tenant(
            id = "t_ye_dhm",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة ذمار",
            shortName = "خيرية القرآن (ذمار)",
            city = "ذمار",
            code = "QCA-DHM",
            ringsCount = 48,
            studentsCount = 1040,
            teachersCount = 40,
            attendanceRate = 91,
            unreadMessagesCount = 5,
            licenseNumber = "YE-DHM-001",
            country = "اليمن",
            governorate = "ذمار"
        ),

        // البيضاء
        Tenant(
            id = "t_ye_bay",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة البيضاء (رداع)",
            shortName = "خيرية القرآن (البيضاء)",
            city = "رداع",
            code = "QCA-BAY",
            ringsCount = 42,
            studentsCount = 890,
            teachersCount = 34,
            attendanceRate = 90,
            unreadMessagesCount = 4,
            licenseNumber = "YE-BAY-001",
            country = "اليمن",
            governorate = "البيضاء"
        ),

        // لحج
        Tenant(
            id = "t_ye_lhj",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة لحج",
            shortName = "خيرية القرآن (لحج)",
            city = "الحوطة",
            code = "QCA-LHJ",
            ringsCount = 44,
            studentsCount = 940,
            teachersCount = 36,
            attendanceRate = 91,
            unreadMessagesCount = 3,
            licenseNumber = "YE-LHJ-001",
            country = "اليمن",
            governorate = "لحج"
        ),

        // أبين
        Tenant(
            id = "t_ye_aby",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة أبين",
            shortName = "خيرية القرآن (أبين)",
            city = "زنجبار",
            code = "QCA-ABY",
            ringsCount = 40,
            studentsCount = 860,
            teachersCount = 32,
            attendanceRate = 90,
            unreadMessagesCount = 4,
            licenseNumber = "YE-ABY-001",
            country = "اليمن",
            governorate = "أبين"
        ),

        // المهرة
        Tenant(
            id = "t_ye_mhr",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة المهرة",
            shortName = "خيرية القرآن (الغيضة)",
            city = "الغيضة",
            code = "QCA-MHR",
            ringsCount = 32,
            studentsCount = 660,
            teachersCount = 26,
            attendanceRate = 92,
            unreadMessagesCount = 2,
            licenseNumber = "YE-MHR-001",
            country = "اليمن",
            governorate = "المهرة"
        ),

        // أرخبيل سقطرى
        Tenant(
            id = "t_ye_soq",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - أرخبيل سقطرى (حديبو)",
            shortName = "خيرية القرآن (سقطرى)",
            city = "حديبو",
            code = "QCA-SOQ",
            ringsCount = 26,
            studentsCount = 530,
            teachersCount = 21,
            attendanceRate = 93,
            unreadMessagesCount = 2,
            licenseNumber = "YE-SOQ-001",
            country = "اليمن",
            governorate = "سقطرى"
        ),

        // حجة
        Tenant(
            id = "t_ye_haj",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة حجة",
            shortName = "خيرية القرآن (حجة)",
            city = "حجة",
            code = "QCA-HAJ",
            ringsCount = 46,
            studentsCount = 980,
            teachersCount = 38,
            attendanceRate = 90,
            unreadMessagesCount = 3,
            licenseNumber = "YE-HAJ-001",
            country = "اليمن",
            governorate = "حجة"
        ),

        // صعدة
        Tenant(
            id = "t_ye_sad",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة صعدة",
            shortName = "خيرية القرآن (صعدة)",
            city = "صعدة",
            code = "QCA-SAD",
            ringsCount = 38,
            studentsCount = 810,
            teachersCount = 30,
            attendanceRate = 89,
            unreadMessagesCount = 3,
            licenseNumber = "YE-SAD-001",
            country = "اليمن",
            governorate = "صعدة"
        ),

        // الجوف
        Tenant(
            id = "t_ye_jwf",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة الجوف (الحزم)",
            shortName = "خيرية القرآن (الجوف)",
            city = "الحزم",
            code = "QCA-JWF",
            ringsCount = 34,
            studentsCount = 730,
            teachersCount = 27,
            attendanceRate = 90,
            unreadMessagesCount = 3,
            licenseNumber = "YE-JWF-001",
            country = "اليمن",
            governorate = "الجوف"
        ),

        // الضالع
        Tenant(
            id = "t_ye_dhl",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة الضالع",
            shortName = "خيرية القرآن (الضالع)",
            city = "الضالع",
            code = "QCA-DHL",
            ringsCount = 36,
            studentsCount = 760,
            teachersCount = 28,
            attendanceRate = 91,
            unreadMessagesCount = 2,
            licenseNumber = "YE-DHL-001",
            country = "اليمن",
            governorate = "الضالع"
        ),

        // المحويت
        Tenant(
            id = "t_ye_mhw",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة المحويت",
            shortName = "خيرية القرآن (المحويت)",
            city = "المحويت",
            code = "QCA-MHW",
            ringsCount = 30,
            studentsCount = 630,
            teachersCount = 24,
            attendanceRate = 90,
            unreadMessagesCount = 2,
            licenseNumber = "YE-MHW-001",
            country = "اليمن",
            governorate = "المحويت"
        ),

        // عمران
        Tenant(
            id = "t_ye_amr",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة عمران",
            shortName = "خيرية القرآن (عمران)",
            city = "عمران",
            code = "QCA-AMR",
            ringsCount = 40,
            studentsCount = 860,
            teachersCount = 32,
            attendanceRate = 90,
            unreadMessagesCount = 3,
            licenseNumber = "YE-AMR-001",
            country = "اليمن",
            governorate = "عمران"
        ),

        // ريمة
        Tenant(
            id = "t_ye_rym",
            name = "الجمعية الخيرية لتعليم القرآن الكريم - فرع محافظة ريمة (الجبين)",
            shortName = "خيرية القرآن (ريمة)",
            city = "الجبين",
            code = "QCA-RYM",
            ringsCount = 28,
            studentsCount = 580,
            teachersCount = 22,
            attendanceRate = 90,
            unreadMessagesCount = 2,
            licenseNumber = "YE-RYM-001",
            country = "اليمن",
            governorate = "ريمة"
        )
    )

    val defaultRealTenant = Tenant(
        id = "t_official_main",
        name = "الجمعية الخيرية لتحفيظ القرآن الكريم",
        shortName = "الجمعية المركزية",
        city = "المركز الرئيسي",
        code = "QCA-MAIN",
        ringsCount = 0,
        studentsCount = 0,
        teachersCount = 0,
        attendanceRate = 100,
        unreadMessagesCount = 0,
        licenseNumber = "10492",
        country = "اليمن",
        governorate = "تعز",
        primaryColorHex = "#1976D2",
        logoIdentifier = "🕌"
    )

    val defaultRing = QuranRing(
        id = "ring_placeholder",
        name = "حلقة جديدة",
        teacherName = "لم يُعين معلم بعد",
        timeSlot = "عصراً",
        studentCount = 0,
        maxCapacity = 25,
        currentJuz = "جزء عمّ وقصار السور",
        attendancePercent = 100
    )

    val defaultStudent = CircleStudent(
        id = "std_placeholder",
        name = "طالب جديد",
        guardianName = "ولي الأمر",
        guardianPhone = "0500000000"
    )

    var isProductionMode by mutableStateOf(false)

    var tenants by mutableStateOf<List<Tenant>>(initialTenants)

    /**
     * ميزة المطور والإدارة: فحص واعتماد طلب انضمام جمعية جديدة
     * وإدراجها فوراً ضمن الجمعيات المعتمدة في النظام وتجهيز بيئتها.
     */
    fun approveNewSociety(
        name: String,
        governorate: String,
        supervisorName: String,
        phone: String,
        email: String,
        notes: String
    ): Tenant {
        val estimatedRings = notes.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 500) ?: 15
        val codePrefix = when {
            governorate.contains("تعز") -> "TAZ"
            governorate.contains("صنعاء") -> "SAN"
            governorate.contains("عدن") -> "ADN"
            governorate.contains("حضرموت") -> "HAD"
            governorate.contains("مأرب") -> "MAR"
            governorate.contains("إب") -> "IBB"
            governorate.contains("الحديدة") -> "HUD"
            else -> "YEM"
        }
        val randomSuffix = (100..999).random()
        val approvedTenant = Tenant(
            id = "t_approved_${System.currentTimeMillis()}",
            name = name.trim(),
            shortName = if (name.trim().length > 25) name.trim().take(25) else name.trim(),
            city = if (governorate.isNotBlank()) governorate.trim() else "اليمن",
            code = "$codePrefix-$randomSuffix",
            ringsCount = estimatedRings,
            studentsCount = estimatedRings * 20,
            teachersCount = (estimatedRings * 1.2).toInt().coerceAtLeast(3),
            attendanceRate = 96,
            unreadMessagesCount = 1,
            licenseNumber = "LIC-YE-${(1000..9999).random()}",
            country = "اليمن",
            governorate = if (governorate.isNotBlank()) governorate.trim() else "اليمن"
        )
        tenants = listOf(approvedTenant) + tenants
        return approvedTenant
    }

    val masterOwner = UserProfile(
        id = "owner_master",
        name = "مالك التطبيق والنظام",
        title = "Super Administrator & Platform Owner",
        role = UserRole.OWNER,
        phone = "775484201",
        email = "adffakff@gmail.com"
    )

    var systemMaintenanceMode by mutableStateOf(false)
    var globalRegistrationActive by mutableStateOf(true)

    fun isOwner(input: String): Boolean {
        return OwnerSecurityManager.verifyOwnerSecret(input)
    }

    fun removeTenant(tenantId: String) {
        tenants = tenants.filter { it.id != tenantId }
    }

    val defaultSupervisor = UserProfile(
        id = "sup_01",
        name = "أ. عبد الرحمن السعيد",
        title = "المشرف العام على الحلقات",
        role = UserRole.SUPERVISOR,
        phone = "0551234567",
        email = "supervisor@maknoon.org.sa"
    )

    fun getInitialTeachers(): List<Teacher> = listOf(
        Teacher(
            id = "tch_01",
            name = "الشيخ عبد الله بن محمد المنصور",
            phone = "0503456781",
            email = "almansoor@maknoon.org.sa",
            qualification = "إجازة بالسند المتصل بالقراءات العشر الصغرى",
            experienceYears = 12,
            isAvailable = true,
            status = TeacherStatus.ACTIVE,
            assignedRings = listOf("حلقة الإمام عاصم (الإتقان والخاتمين)", "حلقة الإمام نافع"),
            nationalId = "1083920194",
            hireDate = "15/04/2018",
            certificates = listOf("إجازة القراءات العشر (عاصم ونافع وابن كثير)", "شهادة معلم معتمد فئة (أ)", "دبلوم التجويد والأداء القرآني"),
            leaveRequests = listOf(
                LeaveRequest("lr_01", "إجازة اعتيادية", "10/11/2026 - 15/11/2026", "الشيخ إبراهيم القرني", "سفر لأداء مناسك العمرة", "معتمد")
            ),
            auditLogs = listOf(
                AuditEntry("a1", "تعيين لحلقة الإمام عاصم", "منذ أسبوع", "المشرف العام"),
                AuditEntry("a2", "تحديث المؤهل العلمي وإرفاق الشهادة", "منذ شهر", "أ. عبد الرحمن السعيد")
            )
        ),
        Teacher(
            id = "tch_02",
            name = "الشيخ فهد بن سليمان الدوسري",
            phone = "0548912345",
            email = "f.aldosari@maknoon.org.sa",
            qualification = "بكالوريوس القراءات والدراسات القرآنية",
            experienceYears = 8,
            isAvailable = true,
            status = TeacherStatus.ACTIVE,
            assignedRings = listOf("حلقة أبي بن كعب (المتوسطة)"),
            nationalId = "1092817263",
            hireDate = "01/09/2020",
            certificates = listOf("إجازة برواية حفص عن عاصم", "شهادة مهارات تدريس القاعدة النورانية"),
            leaveRequests = emptyList(),
            auditLogs = listOf(
                AuditEntry("a3", "تسجيل الحضور الفصلي بنسبة 98%", "منذ 3 أيام", "النظام الآلي")
            )
        ),
        Teacher(
            id = "tch_03",
            name = "الشيخ إبراهيم بن صالح القرني",
            phone = "0561239874",
            email = "i.alqarni@maknoon.org.sa",
            qualification = "ماجستير التفسير وعلوم القرآن",
            experienceYears = 14,
            isAvailable = false,
            status = TeacherStatus.ACTIVE,
            assignedRings = listOf("حلقة الإمام الشاطبي (المتقدمة)", "حلقة الكبار المسائية"),
            nationalId = "1048291048",
            hireDate = "12/02/2016",
            certificates = listOf("إجازة في طيبة النشر والشاطبية", "دورة تأهيل المشرفين الميدانيين"),
            leaveRequests = listOf(
                LeaveRequest("lr_02", "طلب تبادل حلقة", "اليوم - الفترة المسائية", "الشيخ عبد الله المنصور", "ظرف عائلي طارئ", "قيد المراجعة")
            ),
            auditLogs = listOf(
                AuditEntry("a4", "تقديم طلب تبادل حلقة للفترة المسائية", "منذ ساعتين", "الشيخ إبراهيم القرني")
            )
        ),
        Teacher(
            id = "tch_04",
            name = "الشيخ خالد بن ناصر العتيبي",
            phone = "0559876543",
            email = "k.alotaibi@maknoon.org.sa",
            qualification = "حفظ القرآن الكريم كاملاً مع إتقان التجويد",
            experienceYears = 4,
            isAvailable = true,
            status = TeacherStatus.ACTIVE,
            assignedRings = listOf("حلقة البراعم والأشبال"),
            nationalId = "1104829174",
            hireDate = "10/10/2022",
            certificates = listOf("شهادة حفظ القرآن بتقدير ممتاز", "دورة أساليب تحفيز البراعم"),
            leaveRequests = emptyList(),
            auditLogs = listOf(
                AuditEntry("a5", "تكريم المعلم لتميز حلقة البراعم", "الأسبوع الماضي", "المشرف العام")
            )
        ),
        Teacher(
            id = "tch_05",
            name = "الشيخ طارق بن يوسف الغامدي",
            phone = "0532109876",
            email = "t.alghamdi@maknoon.org.sa",
            qualification = "دبلوم عالي في الدراسات الإسلامية",
            experienceYears = 6,
            isAvailable = false,
            status = TeacherStatus.SUSPENDED,
            assignedRings = emptyList(),
            nationalId = "1073829102",
            hireDate = "15/01/2021",
            certificates = listOf("شهادة إتمام حفظ القرآن الكريم"),
            leaveRequests = emptyList(),
            auditLogs = listOf(
                AuditEntry("a6", "إيقاف الحساب مؤقتاً بطلب من المعلم لظروف خاصة", "منذ أسبوعين", "المشرف العام")
            )
        ),
        Teacher(
            id = "tch_06",
            name = "الشيخ زياد بن سالم التميمي",
            phone = "0519823456",
            email = "z.altamimi@maknoon.org.sa",
            qualification = "إجازة بروايتي حفص وشعبة عن عاصم",
            experienceYears = 9,
            isAvailable = true,
            status = TeacherStatus.ACTIVE,
            assignedRings = listOf("حلقة الإمام حمزة الكوفي"),
            nationalId = "1062948172",
            hireDate = "05/03/2019",
            certificates = listOf("شهادة إجازة عاصم", "شهادة التحكيم في المسابقات القرآنية"),
            leaveRequests = emptyList(),
            auditLogs = listOf(
                AuditEntry("a7", "المشاركة في تحكيم التصفيات النهائية", "منذ شهر", "إدارة الجمعية")
            )
        )
    )

    fun getInitialRings(): List<QuranRing> = listOf(
        QuranRing(
            id = "r1",
            name = "حلقة الإمام عاصم (الإتقان والخاتمين)",
            teacherName = "الشيخ عبد الله بن محمد المنصور",
            timeSlot = "الأحد - الخميس (بعد صلاة العصر)",
            studentCount = 75,
            maxCapacity = 100,
            currentJuz = "الجزء 29 - 30",
            attendancePercent = 96,
            level = "متوسط",
            location = "جامع الراجحي - قاعة الفاروق (حضوري)",
            zoomLink = "https://zoom.us/j/9823419827",
            description = "حلقة مخصصة لضبط مخارج الحروف وأحكام التجويد وإتقان الحفظ المتصل مع التفسير الميسر."
        ),
        QuranRing(
            id = "r2",
            name = "حلقة الإمام نافع (المستوى المتقدم)",
            teacherName = "الشيخ عبد الله بن محمد المنصور",
            timeSlot = "الأحد - الأربعاء (بعد صلاة المغرب)",
            studentCount = 22,
            maxCapacity = 25,
            currentJuz = "الجزء 15 - 20",
            attendancePercent = 94,
            level = "متقدم"
        ),
        QuranRing(
            id = "r3",
            name = "حلقة أبي بن كعب (المتوسطة)",
            teacherName = "الشيخ فهد بن سليمان الدوسري",
            timeSlot = "يومياً (بعد صلاة العصر)",
            studentCount = 24,
            maxCapacity = 25,
            currentJuz = "الجزء 5 - 10",
            attendancePercent = 91,
            level = "متوسط"
        ),
        QuranRing(
            id = "r4",
            name = "حلقة الإمام الشاطبي (المتقدمة)",
            teacherName = "الشيخ إبراهيم بن صالح القرني",
            timeSlot = "السبت والإثنين والأربعاء (مسائي)",
            studentCount = 16,
            maxCapacity = 20,
            currentJuz = "الجزء 20 - 25",
            attendancePercent = 93,
            level = "متقدم"
        ),
        QuranRing(
            id = "r5",
            name = "حلقة البراعم والأشبال (تأسيس وتلقين)",
            teacherName = "الشيخ خالد بن ناصر العتيبي",
            timeSlot = "الأحد - الخميس (بعد صلاة العصر)",
            studentCount = 28,
            maxCapacity = 30,
            currentJuz = "جزء عمّ وقصار السور",
            attendancePercent = 97,
            level = "مبتدئ"
        ),
        QuranRing(
            id = "r6",
            name = "حلقة الإمام حمزة الكوفي",
            teacherName = "الشيخ زياد بن سالم التميمي",
            timeSlot = "يومياً (بعد صلاة المغرب)",
            studentCount = 19,
            maxCapacity = 22,
            currentJuz = "الجزء 10 - 15",
            attendancePercent = 88,
            level = "متوسط"
        )
    )

    // الطلاب في حلقة الإمام عاصم (نموذج حي للـ 75 طالباً مع المعاينة)
    fun getInitialCircleStudents(): List<CircleStudent> {
        val names = listOf(
            Pair("أنس بن خالد العمري", "خالد العمري"),
            Pair("عمر بن عبد العزيز الحارثي", "عبد العزيز الحارثي"),
            Pair("يوسف بن سليمان الشهري", "سليمان الشهري"),
            Pair("محمد بن إبراهيم القحطاني", "إبراهيم القحطاني"),
            Pair("عبد الرحمن بن أحمد السعدون", "أحمد السعدون"),
            Pair("سعود بن ناصر الغامدي", "ناصر الغامدي"),
            Pair("سلمان بن ماجد الدوسري", "ماجد الدوسري"),
            Pair("فيصل بن عبد الله التميمي", "عبد الله التميمي"),
            Pair("زيد بن هشام الزهراني", "هشام الزهراني"),
            Pair("براء بن عثمان المطيري", "عثمان المطيري"),
            Pair("ريان بن منصور الحربي", "منصور الحربي"),
            Pair("حمزة بن ياسر السبيعي", "ياسر السبيعي"),
            Pair("إياس بن صالح المالكي", "صالح المالكي"),
            Pair("معاذ بن طارق الشمري", "طارق الشمري"),
            Pair("حذيفة بن بدر العتيبي", "بدر العتيبي")
        )

        return (1..75).map { index ->
            val namePair = names[(index - 1) % names.size]
            val suffix = if (index > names.size) " ($index)" else ""
            val status = when {
                index % 11 == 0 -> AttendanceStatus.ABSENT
                index % 14 == 0 -> AttendanceStatus.LATE
                index % 19 == 0 -> AttendanceStatus.EXCUSED
                else -> AttendanceStatus.PRESENT
            }
            CircleStudent(
                id = "std_$index",
                name = "${namePair.first}$suffix",
                guardianName = namePair.second,
                guardianPhone = "055${(1000000 + index)}",
                attendanceStatus = status,
                lastEvaluation = if (index % 4 == 0) "ممتاز 99%" else "جيد جداً 92%",
                registrationOrder = index,
                note = if (index % 5 == 0) "إتقان سورة النبأ والنازعات" else ""
            )
        }
    }

    fun getInitialWaitlist(): List<WaitlistStudent> = listOf(
        WaitlistStudent("wl_1", "عاصم بن طلال العنزي", "طلال العنزي", "0509876541", 1, "12/10/2026", "ينتظر شاغر في الفترة المسائية"),
        WaitlistStudent("wl_2", "تميم بن فواز الخالدي", "فواز الخالدي", "0543216789", 2, "13/10/2026", "تم التحقق من إتقان جزء عمّ"),
        WaitlistStudent("wl_3", "أسامة بن وليد البقمي", "وليد البقمي", "0567891234", 3, "14/10/2026", "مسجل مسبقاً في قائمة التأسيس"),
        WaitlistStudent("wl_4", "ياسين بن مراد الصالح", "مراد الصالح", "0554321987", 4, "15/10/2026", "جاهز للاختبار القبلي")
    )

    fun getInitialResources(): List<CircleResource> = listOf(
        CircleResource("res_1", "متن تحفة الأطفال في علم التجويد (محققة)", "PDF", "1.8 MB", "01/10/2026"),
        CircleResource("res_2", "تلاوة تعليمية مصحوبة بالترتيل - الحزب 59", "Audio", "14.2 MB", "05/10/2026"),
        CircleResource("res_3", "شرح مخارج الحروف والصفات مرئياً", "Video", "45.0 MB", "08/10/2026"),
        CircleResource("res_4", "جدول الورد اليومي ومتابعة الحفظ المنزلي", "PDF", "850 KB", "10/10/2026")
    )

    fun getInitialAssignments(): List<CircleAssignment> = listOf(
        CircleAssignment("as_1", "حفظ سورة النبأ كاملة مع التجويد", "غداً 05:00 م", "حفظ", 62, 75),
        CircleAssignment("as_2", "مراجعة سورة المعارج والقيامة", "الأحد القادم", "مراجعة", 48, 75),
        CircleAssignment("as_3", "الاختبار الفصلي الأول في جزء تبارك", "الخميس 25 أكتوبر", "اختبار فصلي", 0, 75)
    )

    fun getInitialCapacityAudits(): List<CapacityAuditRecord> = listOf(
        CapacityAuditRecord("cap_1", "أ. عبد الرحمن السعيد", "المشرف العام", "حلقة الإمام عاصم", 80, 100, "زيادة مقاعد لاستيعاب الطلاب", 0, "منذ 4 أيام", "توسعة القاعة وتوفير معيد مساند"),
        CapacityAuditRecord("cap_2", "الشيخ عبد الله المنصور", "معلم الحلقة", "حلقة الإمام عاصم", 70, 80, "زيادة مقاعد تدريجية", 0, "منذ أسبوعين", "طلب من إدارة الجمعية")
    )

    fun getInitialActivities(): List<ActivityFeedItem> = listOf(
        ActivityFeedItem("act_1", "تعيين الشيخ عبد الله المنصور", "تم إسناد حلقة الإمام عاصم والخاتمين للمعلم", "تعيين", "منذ 15 دقيقة"),
        ActivityFeedItem("act_2", "تسجيل 14 طالباً جديداً", "إلحاق طلاب جدد ببرنامج البراعم وحلقة التأسيس", "إضافة", "منذ 45 دقيقة"),
        ActivityFeedItem("act_3", "إصدار شهادة ختم القرآن", "منح شهادة الإتقان للطالب أنس بن خالد العمري", "شهادة", "اليوم 11:20 ص"),
        ActivityFeedItem("act_4", "اكتمال تقرير الحضور اليومي", "سجلت الحلقات اليوم نسبة حضور ممتازة بلغت 94.2%", "حضور", "اليوم 09:30 ص"),
        ActivityFeedItem("act_5", "إرسال تعميم الاختبارات الفصلية", "إشعار موجه لأولياء الأمور والمعلمين بمواعيد الاختبارات", "إشعار", "أمس 04:15 م")
    )

    fun getTenQiraat(): List<QiraaInfo> = listOf(
        QiraaInfo("naafi", "الإمام نافع المدني", listOf("ورش عن نافع", "قالون عن نافع")),
        QiraaInfo("asim", "الإمام عاصم الكوفي", listOf("حفص عن عاصم", "شعبة عن عاصم")),
        QiraaInfo("ibn_katheer", "الإمام ابن كثير المكي", listOf("البزي عن ابن كثير", "قنبل عن ابن كثير")),
        QiraaInfo("abu_amr", "الإمام أبو عمرو البصري", listOf("الدوري عن أبي عمرو", "السوسي عن أبي عمرو")),
        QiraaInfo("ibn_aamir", "الإمام ابن عامر الشامي", listOf("هشام عن ابن عامر", "ابن ذكوان عن ابن عامر")),
        QiraaInfo("hamza", "الإمام حمزة الكوفي", listOf("خلف عن حمزة", "خلاد عن حمزة")),
        QiraaInfo("al_kisai", "الإمام الكسائي الكوفي", listOf("أبو الحارث عن الكسائي", "الدوري عن الكسائي")),
        QiraaInfo("abu_jaafar", "الإمام أبو جعفر المدني", listOf("ابن وردان عن أبي جعفر", "ابن جماز عن أبي جعفر")),
        QiraaInfo("yaqoob", "الإمام يعقوب الحضرمي", listOf("رويس عن يعقوب", "روح عن يعقوب")),
        QiraaInfo("khalaf_10", "الإمام خلف العاشر", listOf("إسحاق عن خلف", "إدريس عن خلف"))
    )

    fun getSampleRecitationAnalysis(selectedQira: String, selectedRiwaya: String): RecitationAnalysisResult {
        return RecitationAnalysisResult(
            analysisId = "rec_analysis_982",
            selectedQira = selectedQira,
            selectedRiwaya = selectedRiwaya,
            detectedQira = selectedQira,
            detectedConfidence = 0.94f,
            score = 0.88f,
            scoreAdjustedForQira = 0.85f,
            overallLevel = "جيد جداً مرتفع",
            segments = listOf(
                RecitationSegment(
                    start = 1.2f,
                    end = 2.4f,
                    text = "﴿الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ﴾",
                    errorType = "إتقان مخارج الحروف",
                    expectedBySelectedQira = "مخرج الحاء واللام الساكنة",
                    suggestion = "تطبيق توسط حرف اللام وإيضاح مخرج الحاء بدون تكلف",
                    confidence = 0.92f
                ),
                RecitationSegment(
                    start = 3.1f,
                    end = 4.8f,
                    text = "﴿مَالِكِ يَوْمِ الدِّينِ﴾",
                    errorType = "أوجه القراءة المحددة",
                    expectedBySelectedQira = if (selectedRiwaya.contains("ورش") || selectedRiwaya.contains("السوسي")) "قراءة (مَلِكِ) بحذف الألف" else "قراءة (مَالِكِ) بإثبات الألف",
                    suggestion = if (selectedRiwaya.contains("ورش")) "قرأ الطالب بإثبات الألف (مالك)، والمطلوب برواية ورش هو القصر (مَلِكِ)" else "قراءة صحيحة متوافقة مع الرواية المعتمدة",
                    confidence = 0.89f
                ),
                RecitationSegment(
                    start = 5.2f,
                    end = 7.0f,
                    text = "﴿اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ﴾",
                    errorType = "أحكام الصاد والإشمام",
                    expectedBySelectedQira = if (selectedRiwaya.contains("حمزة") || selectedRiwaya.contains("خلف")) "إشمام الصاد زايًا" else "إخلاص الصاد مفخمة",
                    suggestion = "تأكد من تطبيق قاعدة الصاد الخالصة وعدم الخلط بروايات الإشمام",
                    confidence = 0.87f
                )
            )
        )
    }

    fun getInitialParentRecordings(): List<ParentRecordingItem> = listOf(
        ParentRecordingItem(
            id = "prec_1",
            studentName = "أنس بن خالد العمري",
            surahName = "سورة النبأ",
            ayahRange = "الآيات 1 - 16",
            qiraaName = "حفص عن عاصم",
            mode = "تسليم رسمي (Submission)",
            privacy = "خاص للمعلم",
            durationSeconds = 48,
            status = "accepted",
            quickScore = "92%",
            quickFeedback = "أداء سليم متقن مع جودة في المد المتصل",
            teacherFeedback = "أحسنت يا أنس، تم اعتماد التسميع وحفظ الجزء بنجاح بارك الله فيك.",
            uploadedAt = "اليوم 10:30 ص"
        ),
        ParentRecordingItem(
            id = "prec_2",
            studentName = "أنس بن خالد العمري",
            surahName = "سورة النازعات",
            ayahRange = "الآيات 1 - 25",
            qiraaName = "ورش عن نافع",
            mode = "اختبار منزلي (Practice)",
            privacy = "مشترك مع ولي الأمر الآخر",
            durationSeconds = 62,
            status = "quick_done",
            quickScore = "85%",
            quickFeedback = "تحسين مطلوب في ترقيق الراءات وتغليظ اللامات حسب رواية ورش",
            teacherFeedback = null,
            uploadedAt = "أمس 08:15 م"
        )
    )

    fun getInitialEligibleStudents(): List<EligibleCertificateStudent> = listOf(
        EligibleCertificateStudent("cert_st_1", "أنس بن خالد العمري", "حلقة الإمام عاصم", "إتمام جزء عمّ وتبارك", "12/10/2026", "98% (ممتاز)", true),
        EligibleCertificateStudent("cert_st_2", "عمر بن عبد العزيز الحارثي", "حلقة الإمام نافع", "إتقان القراءات العشر (المستوى الأول)", "14/10/2026", "96% (ممتاز)", true),
        EligibleCertificateStudent("cert_st_3", "يوسف بن سليمان الشهري", "حلقة أبي بن كعب", "إتمام حفظ القرآن الكريم كاملاً", "15/10/2026", "99% (مرتفع جداً)", true),
        EligibleCertificateStudent("cert_st_4", "سلمان بن ماجد الدوسري", "حلقة الإمام الشاطبي", "دبلوم التجويد والأداء القرآني", "10/10/2026", "94% (ممتاز)", true),
        EligibleCertificateStudent("cert_st_5", "فيصل بن عبد الله التميمي", "حلقة البراعم والأشبال", "تأسيس القاعدة النورانية وحفظ المفصل", "16/10/2026", "95% (ممتاز)", false)
    )

    fun getInitialExportLogs(): List<ExportLogRecord> = listOf(
        ExportLogRecord("exp_1", "تقرير الحضور الشهري للحلقات", "16/10/2026 11:30 ص", "أ. عبد الرحمن السعيد (المشرف)", 724, "PDF"),
        ExportLogRecord("exp_2", "كشف درجات الاختبارات الفصلية", "15/10/2026 04:15 م", "أ. عبد الرحمن السعيد (المشرف)", 148, "CSV"),
        ExportLogRecord("exp_3", "إصدار شهادات إتمام الحفظ دفعة ربيع الأول", "12/10/2026 09:00 ص", "أ. عبد الرحمن السعيد (المشرف)", 42, "PDF")
    )

    // ==========================================
    // إدارة وتحكم المالك الشامل (Owner Master State & CRUD)
    // ==========================================
    var allTeachers by mutableStateOf<List<Teacher>>(emptyList())
    var allRings by mutableStateOf<List<QuranRing>>(emptyList())
    var allStudents by mutableStateOf<List<CircleStudent>>(emptyList())
    var allUsers by mutableStateOf(listOf(masterOwner))

    fun addTenant(tenant: Tenant) {
        tenants = listOf(tenant) + tenants
    }

    fun updateTenant(tenant: Tenant) {
        tenants = tenants.map { if (it.id == tenant.id) tenant else it }
    }

    fun addTeacher(teacher: Teacher) {
        allTeachers = listOf(teacher) + allTeachers
    }

    fun updateTeacher(teacher: Teacher) {
        allTeachers = allTeachers.map { if (it.id == teacher.id) teacher else it }
    }

    fun removeTeacher(teacherId: String) {
        allTeachers = allTeachers.filter { it.id != teacherId }
    }

    fun addRing(ring: QuranRing) {
        allRings = listOf(ring) + allRings
    }

    fun updateRing(ring: QuranRing) {
        allRings = allRings.map { if (it.id == ring.id) ring else it }
    }

    fun removeRing(ringId: String) {
        allRings = allRings.filter { it.id != ringId }
    }

    fun addStudent(student: CircleStudent) {
        allStudents = listOf(student) + allStudents
    }

    fun updateStudent(student: CircleStudent) {
        allStudents = allStudents.map { if (it.id == student.id) student else it }
    }

    fun removeStudent(studentId: String) {
        allStudents = allStudents.filter { it.id != studentId }
    }

    fun updateUserRole(userId: String, newRole: UserRole) {
        allUsers = allUsers.map { if (it.id == userId) it.copy(role = newRole) else it }
    }

    fun addUser(user: UserProfile) {
        allUsers = listOf(user) + allUsers
    }

    fun removeUser(userId: String) {
        if (userId != masterOwner.id) {
            allUsers = allUsers.filter { it.id != userId }
        }
    }

    // تهيئة واستعادة كافة بيانات التطبيق للحالة الافتراضية الأصلية
    fun resetToFactoryDefaults() {
        tenants = initialTenants
        allTeachers = getInitialTeachers()
        allRings = getInitialRings()
        allStudents = getInitialCircleStudents()
        allUsers = listOf(
            masterOwner,
            defaultSupervisor,
            UserProfile("usr_tch_1", "الشيخ عبد الله بن محمد المنصور", "معلم حلقة الإتقان", UserRole.TEACHER, "0503456781", "almansoor@maknoon.org.sa"),
            UserProfile("usr_tch_2", "الشيخ فهد بن سليمان الدوسري", "معلم حلقة أبي بن كعب", UserRole.TEACHER, "0548912345", "f.aldosari@maknoon.org.sa"),
            UserProfile("usr_par_1", "أ. خالد العمري (ولي أمر)", "ولي أمر الطالب أنس", UserRole.GUARDIAN, "0561239874", "khaled.omari@gmail.com")
        )
        systemMaintenanceMode = false
        globalRegistrationActive = true
    }

    // تفريغ وتصفير بيانات التطبيق بالكامل مع الحفاظ على حساب المالك وبدء النسخة الحقيقية للنظام
    fun wipeAllDataPreservingOwner() {
        tenants = listOf(defaultRealTenant)
        allTeachers = emptyList()
        allRings = emptyList()
        allStudents = emptyList()
        allUsers = listOf(masterOwner, defaultSupervisor)
        isProductionMode = true
    }

    // تصفير جميع بيانات الجمعيات وجعل النظام جاهزاً للعمل الفعلي بدون أخطاء
    fun resetAllSocietiesDataClean() {
        tenants = listOf(defaultRealTenant)
        allTeachers = emptyList()
        allRings = emptyList()
        allStudents = emptyList()
        allUsers = listOf(masterOwner, defaultSupervisor)
        isProductionMode = true
    }

    fun isAuthorizedOwner(emailOrPhone: String): Boolean {
        return OwnerSecurityManager.verifyOwnerSecret(emailOrPhone)
    }
}


