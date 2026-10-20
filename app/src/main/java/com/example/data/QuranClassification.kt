package com.example.data

/**
 * تصنيف السور القرآنية (مكية ومدنية) لتوفير رؤية تحليلية وتربوية عميقة لمشرفي الحلقات
 */
enum class SurahClassification(
    val label: String,
    val badge: String,
    val iconEmoji: String,
    val description: String
) {
    MAKKI(
        label = "مكية",
        badge = "🕋 مكية",
        iconEmoji = "🕋",
        description = "نزلت في مكة المكرمة قبل الهجرة النبوية - تمتاز بالإيجاز وقوة البيان وترسيخ العقيدة والتوحيد وأصول الإيمان."
    ),
    MADANI(
        label = "مدنية",
        badge = "🕌 مدنية",
        iconEmoji = "🕌",
        description = "نزلت في المدينة المنورة بعد الهجرة النبوية - تمتاز ببناء المجتمع وبيان التشريعات والأحكام والفرائض والمعاملات."
    )
}

data class SurahMemorizationMetric(
    val id: Int,
    val name: String,
    val classification: SurahClassification,
    val ayatCount: Int,
    val juzNumber: Int,
    val memorizedStudentsCount: Int,
    val masteryPercent: Int,
    val isKeyMilestone: Boolean = false
)

data class MakkiMadaniOverview(
    val totalMakkiSurahs: Int = 86,
    val totalMadaniSurahs: Int = 28,
    val completedMakkiCount: Int = 62,
    val completedMadaniCount: Int = 19,
    val makkiCompletionRate: Float = 0.72f,
    val madaniCompletionRate: Float = 0.68f,
    val makkiAverageMastery: Int = 94,
    val madaniAverageMastery: Int = 89,
    val activeStudentsInMakki: Int = 184,
    val activeStudentsInMadani: Int = 142,
    val supervisorInsightNote: String = "إنجاز ممتاز في السور المكية (قصار السور والمفصل) بمعدل إتقان 94%. يُنصح بتركيز حصص المراجعة على السور المدنية الطويلة (البقرة، آل عمران، النساء) لرفع معدل الإتقان من 89% إلى 95%."
)

object QuranClassificationHelper {

    // قائمة السور المدنية الثماني والعشرون (28 سورة)
    private val madaniSurahsNames = setOf(
        "البقرة", "سورة البقرة",
        "آل عمران", "سورة آل عمران",
        "النساء", "سورة النساء",
        "المائدة", "سورة المائدة",
        "الأنفال", "سورة الأنفال",
        "التوبة", "سورة التوبة",
        "الرعد", "سورة الرعد",
        "الحج", "سورة الحج",
        "النور", "سورة النور",
        "الأحزاب", "سورة الأحزاب",
        "محمد", "سورة محمد",
        "الفتح", "سورة الفتح",
        "الحجرات", "سورة الحجرات",
        "الرحمن", "سورة الرحمن",
        "الحديد", "سورة الحديد",
        "المجادلة", "سورة المجادلة",
        "الحشر", "سورة الحشر",
        "الممتحنة", "سورة الممتحنة",
        "الصف", "سورة الصف",
        "الجمعة", "سورة الجمعة",
        "المنافقون", "سورة المنافقون",
        "التغابن", "سورة التغابن",
        "الطلاق", "سورة الطلاق",
        "التحريم", "سورة التحريم",
        "الإنسان", "سورة الإنسان",
        "البينة", "سورة البينة",
        "الزلزلة", "سورة الزلزلة",
        "النصر", "سورة النصر"
    )

    /**
     * فحص نوع السورة: مكية أو مدنية
     */
    fun getClassification(rawName: String): SurahClassification {
        val cleanName = rawName.trim().removePrefix("سورة").trim()
        return if (madaniSurahsNames.contains(cleanName) || madaniSurahsNames.contains(rawName.trim())) {
            SurahClassification.MADANI
        } else {
            SurahClassification.MAKKI
        }
    }

    fun isMakki(rawName: String): Boolean = getClassification(rawName) == SurahClassification.MAKKI
    fun isMadani(rawName: String): Boolean = getClassification(rawName) == SurahClassification.MADANI

    fun getOverviewStats(): MakkiMadaniOverview {
        return MakkiMadaniOverview()
    }

    /**
     * قائمة السور المتابعة في الحلقات مع تصنيفها ونسب الإتقان
     */
    fun getTrackedSurahsMetrics(): List<SurahMemorizationMetric> {
        return listOf(
            SurahMemorizationMetric(1, "سورة الفاتحة", SurahClassification.MAKKI, 7, 1, 75, 99, true),
            SurahMemorizationMetric(2, "سورة البقرة", SurahClassification.MADANI, 286, 1, 42, 88, true),
            SurahMemorizationMetric(3, "سورة آل عمران", SurahClassification.MADANI, 200, 3, 36, 86, true),
            SurahMemorizationMetric(4, "سورة النساء", SurahClassification.MADANI, 176, 4, 28, 84, true),
            SurahMemorizationMetric(5, "سورة المائدة", SurahClassification.MADANI, 120, 6, 25, 87, false),
            SurahMemorizationMetric(6, "سورة الأنعام", SurahClassification.MAKKI, 165, 7, 31, 91, false),
            SurahMemorizationMetric(18, "سورة الكهف", SurahClassification.MAKKI, 110, 15, 68, 96, true),
            SurahMemorizationMetric(36, "سورة يس", SurahClassification.MAKKI, 83, 22, 64, 95, true),
            SurahMemorizationMetric(49, "سورة الحجرات", SurahClassification.MADANI, 18, 26, 58, 93, false),
            SurahMemorizationMetric(55, "سورة الرحمن", SurahClassification.MADANI, 78, 27, 60, 94, false),
            SurahMemorizationMetric(56, "سورة الواقعة", SurahClassification.MAKKI, 96, 27, 63, 95, false),
            SurahMemorizationMetric(67, "سورة الملك", SurahClassification.MAKKI, 30, 29, 74, 98, true),
            SurahMemorizationMetric(78, "سورة النبأ", SurahClassification.MAKKI, 40, 30, 75, 97, true),
            SurahMemorizationMetric(79, "سورة النازعات", SurahClassification.MAKKI, 46, 30, 72, 96, false),
            SurahMemorizationMetric(87, "سورة الأعلى", SurahClassification.MAKKI, 19, 30, 75, 99, false),
            SurahMemorizationMetric(112, "سورة الإخلاص", SurahClassification.MAKKI, 4, 30, 75, 100, true)
        )
    }
}
