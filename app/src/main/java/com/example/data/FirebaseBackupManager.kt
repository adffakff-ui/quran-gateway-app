package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupItem(
    val id: String,
    val fileName: String,
    val timestamp: Long,
    val formattedDate: String,
    val sizeKb: Long,
    val downloadUrl: String,
    val tenantId: String,
    val tenantName: String,
    val studentCount: Int,
    val teacherCount: Int,
    val attendanceRecordCount: Int,
    val isCloudSynced: Boolean,
    val storagePath: String
)

data class RestoreSummary(
    val tenantName: String,
    val restoredStudentsCount: Int,
    val restoredTeachersCount: Int,
    val restoredRingsCount: Int,
    val restoredAttendanceCount: Int,
    val exportDate: String,
    val sourceFileName: String
)

object FirebaseBackupManager {

    private const val TAG = "FirebaseBackupManager"
    private val localBackupHistory = mutableListOf<BackupItem>()

    /**
     * تصدير بيانات الجمعية الحالية (طلاب، معلمين، وسجلات الحضور)
     * وحفظها كملف JSON ورفعها إلى Firebase Storage مع الاحتفاظ بنسخة محلية احتياطية
     */
    suspend fun exportTenantToFirebaseStorage(
        tenant: Tenant,
        context: Context,
        performedBy: String = "مشرف الجمعية"
    ): Result<BackupItem> = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ar"))
            val formattedDate = dateFormat.format(Date(timestamp))
            val safeDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))
            val fileName = "backup_${tenant.id}_$safeDate.json"

            // 1. تجميع بيانات الطلاب الحالية
            val students = SocietyRepository.allStudents
            val studentsJsonArray = JSONArray()
            students.forEach { s ->
                val sObj = JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("guardianName", s.guardianName)
                    put("guardianPhone", s.guardianPhone)
                    put("attendanceStatus", s.attendanceStatus.name)
                    put("lastEvaluation", s.lastEvaluation)
                    put("registrationOrder", s.registrationOrder)
                    put("note", s.note)
                }
                studentsJsonArray.put(sObj)
            }

            // 2. تجميع بيانات المعلمين
            val teachers = SocietyRepository.allTeachers
            val teachersJsonArray = JSONArray()
            teachers.forEach { t ->
                val tObj = JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("phone", t.phone)
                    put("email", t.email)
                    put("qualification", t.qualification)
                    put("experienceYears", t.experienceYears)
                    put("isAvailable", t.isAvailable)
                    put("status", t.status.name)
                    put("nationalId", t.nationalId)
                    put("hireDate", t.hireDate)
                    val ringsArray = JSONArray()
                    t.assignedRings.forEach { ringsArray.put(it) }
                    put("assignedRings", ringsArray)
                    val certsArray = JSONArray()
                    t.certificates.forEach { certsArray.put(it) }
                    put("certificates", certsArray)
                }
                teachersJsonArray.put(tObj)
            }

            // 3. تجميع بيانات الحلقات القرآنية
            val rings = SocietyRepository.allRings
            val ringsJsonArray = JSONArray()
            rings.forEach { r ->
                val rObj = JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("teacherName", r.teacherName)
                    put("timeSlot", r.timeSlot)
                    put("studentCount", r.studentCount)
                    put("maxCapacity", r.maxCapacity)
                    put("currentJuz", r.currentJuz)
                    put("attendancePercent", r.attendancePercent)
                    put("level", r.level)
                    put("location", r.location)
                    put("zoomLink", r.zoomLink)
                    put("description", r.description)
                }
                ringsJsonArray.put(rObj)
            }

            // 4. سجلات الحضور التفصيلية
            val attendanceJsonArray = JSONArray()
            students.forEach { st ->
                val attObj = JSONObject().apply {
                    put("studentId", st.id)
                    put("studentName", st.name)
                    put("status", st.attendanceStatus.name)
                    put("statusLabel", st.attendanceStatus.label)
                    put("date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp)))
                    put("note", st.note.ifBlank { "سجل حضور اعتيادي" })
                }
                attendanceJsonArray.put(attObj)
            }

            // 5. بناء الكائن الرئيسي (Root JSON Payload)
            val rootJson = JSONObject().apply {
                put("version", 1)
                put("type", "SOCIETY_BACKUP_SNAPSHOT")
                put("exportedAt", formattedDate)
                put("exportedTimestamp", timestamp)
                put("exportedBy", performedBy)

                // بيانات الجمعية
                val tenantObj = JSONObject().apply {
                    put("id", tenant.id)
                    put("name", tenant.name)
                    put("shortName", tenant.shortName)
                    put("city", tenant.city)
                    put("code", tenant.code)
                    put("ringsCount", tenant.ringsCount)
                    put("studentsCount", tenant.studentsCount)
                    put("teachersCount", tenant.teachersCount)
                    put("attendanceRate", tenant.attendanceRate)
                    put("licenseNumber", tenant.licenseNumber)
                    put("country", tenant.country)
                    put("governorate", tenant.governorate)
                }
                put("tenant", tenantObj)

                // الجداول والبيانات
                put("students", studentsJsonArray)
                put("teachers", teachersJsonArray)
                put("rings", ringsJsonArray)
                put("attendanceRecords", attendanceJsonArray)

                // إحصائيات سريعة للتحقق
                val metaObj = JSONObject().apply {
                    put("studentsCount", students.size)
                    put("teachersCount", teachers.size)
                    put("ringsCount", rings.size)
                    put("attendanceRecordsCount", attendanceJsonArray.length())
                }
                put("summary", metaObj)
            }

            val jsonString = rootJson.toString(2)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
            val sizeKb = (jsonBytes.size / 1024L).coerceAtLeast(1L)

            // 6. الحفظ المحلي الاحتياطي في ذاكرة التطبيق الداخلية
            val backupDir = File(context.filesDir, "society_backups/${tenant.id}").apply { mkdirs() }
            val localFile = File(backupDir, fileName)
            localFile.writeBytes(jsonBytes)

            // 7. الرفع إلى Firebase Storage
            var isCloudSynced = false
            var downloadUrl = ""
            val storagePath = "backups/${tenant.id}/$fileName"

            try {
                val storage = FirebaseStorage.getInstance()
                val storageRef = storage.reference.child(storagePath)

                val metadata = StorageMetadata.Builder()
                    .setContentType("application/json")
                    .setCustomMetadata("tenantId", tenant.id)
                    .setCustomMetadata("tenantName", tenant.name)
                    .setCustomMetadata("studentsCount", students.size.toString())
                    .setCustomMetadata("teachersCount", teachers.size.toString())
                    .setCustomMetadata("exportedAt", formattedDate)
                    .build()

                storageRef.putBytes(jsonBytes, metadata).await()
                downloadUrl = try {
                    storageRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    "gs://${storage.reference.bucket}/$storagePath"
                }
                isCloudSynced = true

                // نسخة إضافية كأحدث نسخة دائمة
                val latestRef = storage.reference.child("backups/${tenant.id}/latest_backup.json")
                latestRef.putBytes(jsonBytes, metadata).await()

                Log.d(TAG, "Uploaded backup to Firebase Storage successfully: $storagePath")
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Storage upload non-critical failure (retained local copy): ${e.localizedMessage}")
                downloadUrl = "file://${localFile.absolutePath}"
            }

            val backupItem = BackupItem(
                id = "bk_${safeDate}",
                fileName = fileName,
                timestamp = timestamp,
                formattedDate = formattedDate,
                sizeKb = sizeKb,
                downloadUrl = downloadUrl,
                tenantId = tenant.id,
                tenantName = tenant.name,
                studentCount = students.size,
                teacherCount = teachers.size,
                attendanceRecordCount = attendanceJsonArray.length(),
                isCloudSynced = isCloudSynced,
                storagePath = storagePath
            )

            localBackupHistory.add(0, backupItem)
            Result.success(backupItem)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export tenant data", e)
            Result.failure(e)
        }
    }

    /**
     * استيراد واستعادة بيانات الجمعية من ملف JSON لحل مشاكل المزامنة
     */
    suspend fun restoreTenantFromJson(
        jsonString: String,
        targetTenant: Tenant,
        sourceFileName: String = "imported_backup.json"
    ): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            val exportDate = root.optString("exportedAt", "تاريخ غير محدد")
            val studentsArray = root.optJSONArray("students") ?: JSONArray()
            val teachersArray = root.optJSONArray("teachers") ?: JSONArray()
            val ringsArray = root.optJSONArray("rings") ?: JSONArray()
            val attendanceArray = root.optJSONArray("attendanceRecords") ?: JSONArray()

            // 1. استعادة الطلاب
            val restoredStudents = mutableListOf<CircleStudent>()
            for (i in 0 until studentsArray.length()) {
                val obj = studentsArray.getJSONObject(i)
                val statusStr = obj.optString("attendanceStatus", AttendanceStatus.PRESENT.name)
                val status = try {
                    AttendanceStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    AttendanceStatus.PRESENT
                }
                restoredStudents.add(
                    CircleStudent(
                        id = obj.optString("id", "st_${i + 1}"),
                        name = obj.optString("name", "طالب مستعاد"),
                        guardianName = obj.optString("guardianName", "ولي أمر"),
                        guardianPhone = obj.optString("guardianPhone", "0500000000"),
                        attendanceStatus = status,
                        lastEvaluation = obj.optString("lastEvaluation", "ممتاز"),
                        registrationOrder = obj.optInt("registrationOrder", i + 1),
                        note = obj.optString("note", "مستعاد من نسخة احتياطية")
                    )
                )
            }

            // 2. استعادة المعلمين
            val restoredTeachers = mutableListOf<Teacher>()
            for (i in 0 until teachersArray.length()) {
                val obj = teachersArray.getJSONObject(i)
                val statusStr = obj.optString("status", TeacherStatus.ACTIVE.name)
                val status = try {
                    TeacherStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    TeacherStatus.ACTIVE
                }

                val ringsList = mutableListOf<String>()
                val ringsJArray = obj.optJSONArray("assignedRings")
                if (ringsJArray != null) {
                    for (r in 0 until ringsJArray.length()) {
                        ringsList.add(ringsJArray.getString(r))
                    }
                }

                val certsList = mutableListOf<String>()
                val certsJArray = obj.optJSONArray("certificates")
                if (certsJArray != null) {
                    for (c in 0 until certsJArray.length()) {
                        certsList.add(certsJArray.getString(c))
                    }
                }

                restoredTeachers.add(
                    Teacher(
                        id = obj.optString("id", "tch_${i + 1}"),
                        name = obj.optString("name", "معلم مستعاد"),
                        phone = obj.optString("phone", "0500000000"),
                        email = obj.optString("email", "teacher@maknoon.org.sa"),
                        qualification = obj.optString("qualification", "بكالوريوس دراسات إسلامية"),
                        experienceYears = obj.optInt("experienceYears", 5),
                        isAvailable = obj.optBoolean("isAvailable", true),
                        status = status,
                        assignedRings = ringsList.ifEmpty { listOf("حلقة الإمام عاصم") },
                        nationalId = obj.optString("nationalId", "1000000000"),
                        hireDate = obj.optString("hireDate", "01/01/2022"),
                        certificates = certsList.ifEmpty { listOf("إجازة في القرآن الكريم") },
                        leaveRequests = emptyList(),
                        auditLogs = emptyList()
                    )
                )
            }

            // 3. استعادة الحلقات القرآنية
            val restoredRings = mutableListOf<QuranRing>()
            for (i in 0 until ringsArray.length()) {
                val obj = ringsArray.getJSONObject(i)
                restoredRings.add(
                    QuranRing(
                        id = obj.optString("id", "r_${i + 1}"),
                        name = obj.optString("name", "حلقة مستعادة"),
                        teacherName = obj.optString("teacherName", "معلم الحلقة"),
                        timeSlot = obj.optString("timeSlot", "بعد صلاة العصر"),
                        studentCount = obj.optInt("studentCount", 20),
                        maxCapacity = obj.optInt("maxCapacity", 30),
                        currentJuz = obj.optString("currentJuz", "جزء عمّ"),
                        attendancePercent = obj.optInt("attendancePercent", 95),
                        level = obj.optString("level", "متوسط"),
                        location = obj.optString("location", "مقر الجمعية"),
                        zoomLink = obj.optString("zoomLink", ""),
                        description = obj.optString("description", "حلقة مستعادة من النسخة الاحتياطية")
                    )
                )
            }

            // تطبيق الاستعادة على مستودع التطبيق
            withContext(Dispatchers.Main) {
                if (restoredStudents.isNotEmpty()) {
                    SocietyRepository.allStudents = restoredStudents
                }
                if (restoredTeachers.isNotEmpty()) {
                    SocietyRepository.allTeachers = restoredTeachers
                }
                if (restoredRings.isNotEmpty()) {
                    SocietyRepository.allRings = restoredRings
                }

                // تحديث بيانات الجمعية الإجمالية
                SocietyRepository.tenants = SocietyRepository.tenants.map { t ->
                    if (t.id == targetTenant.id) {
                        t.copy(
                            studentsCount = restoredStudents.size.coerceAtLeast(t.studentsCount),
                            teachersCount = restoredTeachers.size.coerceAtLeast(t.teachersCount),
                            ringsCount = restoredRings.size.coerceAtLeast(t.ringsCount)
                        )
                    } else t
                }

                // تفعيل المزامنة المباشرة
                SyncManager.triggerSync()
            }

            val summary = RestoreSummary(
                tenantName = targetTenant.name,
                restoredStudentsCount = restoredStudents.size,
                restoredTeachersCount = restoredTeachers.size,
                restoredRingsCount = restoredRings.size,
                restoredAttendanceCount = attendanceArray.length(),
                exportDate = exportDate,
                sourceFileName = sourceFileName
            )

            Log.d(TAG, "Successfully restored tenant data from JSON backup: $summary")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore tenant from JSON", e)
            Result.failure(e)
        }
    }

    /**
     * استرجاع قائمة النسخ المتوفرة (محلياً أو في Firebase Storage)
     */
    suspend fun getAvailableBackups(tenant: Tenant, context: Context): List<BackupItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<BackupItem>()

        // 1. القائمة في الذاكرة الحالية
        list.addAll(localBackupHistory.filter { it.tenantId == tenant.id })

        // 2. فحص المجلد المحلي
        val backupDir = File(context.filesDir, "society_backups/${tenant.id}")
        if (backupDir.exists() && backupDir.isDirectory) {
            backupDir.listFiles { file -> file.extension == "json" }?.forEach { f ->
                if (list.none { it.fileName == f.name }) {
                    val sizeKb = (f.length() / 1024L).coerceAtLeast(1L)
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date(f.lastModified()))
                    list.add(
                        BackupItem(
                            id = f.nameWithoutExtension,
                            fileName = f.name,
                            timestamp = f.lastModified(),
                            formattedDate = dateStr,
                            sizeKb = sizeKb,
                            downloadUrl = "file://${f.absolutePath}",
                            tenantId = tenant.id,
                            tenantName = tenant.name,
                            studentCount = SocietyRepository.allStudents.size,
                            teacherCount = SocietyRepository.allTeachers.size,
                            attendanceRecordCount = SocietyRepository.allStudents.size,
                            isCloudSynced = false,
                            storagePath = "backups/${tenant.id}/${f.name}"
                        )
                    )
                }
            }
        }

        // 3. محاولة استعلام Firebase Storage إذا توفر الاتصال
        try {
            val storage = FirebaseStorage.getInstance()
            val backupsFolder = storage.reference.child("backups/${tenant.id}")
            val result = backupsFolder.listAll().await()
            result.items.forEach { item ->
                val alreadyExists = list.any { it.fileName == item.name }
                if (!alreadyExists && item.name != "latest_backup.json") {
                    val meta = try { item.metadata.await() } catch (e: Exception) { null }
                    val dUrl = try { item.downloadUrl.await().toString() } catch (e: Exception) { "" }
                    val time = meta?.creationTimeMillis ?: System.currentTimeMillis()
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date(time))
                    val sizeKb = (meta?.sizeBytes ?: 1024L) / 1024L

                    list.add(
                        BackupItem(
                            id = item.name.replace(".json", ""),
                            fileName = item.name,
                            timestamp = time,
                            formattedDate = dateFormatted,
                            sizeKb = sizeKb.coerceAtLeast(1L),
                            downloadUrl = dUrl,
                            tenantId = tenant.id,
                            tenantName = tenant.name,
                            studentCount = meta?.getCustomMetadata("studentsCount")?.toIntOrNull() ?: 25,
                            teacherCount = meta?.getCustomMetadata("teachersCount")?.toIntOrNull() ?: 4,
                            attendanceRecordCount = 25,
                            isCloudSynced = true,
                            storagePath = item.path
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Firebase storage listing skipped: ${e.localizedMessage}")
        }

        list.sortedByDescending { it.timestamp }
    }

    /**
     * تنزيل نسخة سحابية من Firebase Storage واستعادتها
     */
    suspend fun downloadAndRestoreFromStorage(
        backupItem: BackupItem,
        targetTenant: Tenant,
        context: Context
    ): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        try {
            // فحص الملف محلياً أولاً
            val backupDir = File(context.filesDir, "society_backups/${targetTenant.id}")
            val localFile = File(backupDir, backupItem.fileName)
            val jsonContent = if (localFile.exists()) {
                localFile.readText(Charsets.UTF_8)
            } else {
                val storage = FirebaseStorage.getInstance()
                val storageRef = storage.reference.child(backupItem.storagePath)
                val bytes = storageRef.getBytes(10 * 1024 * 1024L).await() // 10MB max
                String(bytes, Charsets.UTF_8).also {
                    backupDir.mkdirs()
                    localFile.writeBytes(bytes)
                }
            }

            restoreTenantFromJson(jsonContent, targetTenant, backupItem.fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and restore backup from storage", e)
            Result.failure(e)
        }
    }
}
