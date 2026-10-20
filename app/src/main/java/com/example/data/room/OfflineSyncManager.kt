package com.example.data.room

import android.content.Context
import com.example.data.CircleStudent
import com.example.data.QuranRing

/**
 * مدير المزامنة والعمل بدون إنترنت (Offline Mode & Sync Manager)
 * يدير التخزين المؤقت المحلي عبر Room Database ومزامنة العمليات المعلقة عند توفر الاتصال.
 */
object OfflineSyncManager {

    suspend fun cacheStudents(context: Context, tenantId: String, ringId: String, students: List<CircleStudent>) {
        val db = SocietyRoomDatabase.getDatabase(context)
        val entities = students.map { s ->
            StudentEntity(
                id = s.id,
                ringId = ringId,
                tenantId = tenantId,
                name = s.name,
                attendanceStatus = s.attendanceStatus.label,
                memorizationPart = s.lastEvaluation,
                phone = s.guardianPhone,
                isSynced = true
            )
        }
        db.studentDao().insertStudents(entities)
    }

    suspend fun cacheRings(context: Context, tenantId: String, rings: List<QuranRing>) {
        val db = SocietyRoomDatabase.getDatabase(context)
        val entities = rings.map { r ->
            RingEntity(
                id = r.id,
                tenantId = tenantId,
                name = r.name,
                teacherName = r.teacherName,
                studentsCount = r.studentCount,
                isSynced = true
            )
        }
        db.ringDao().insertRings(entities)
    }

    suspend fun updateStudentAttendanceOffline(context: Context, studentId: String, status: String) {
        val db = SocietyRoomDatabase.getDatabase(context)
        db.studentDao().updateAttendance(studentId, status, synced = false)
        db.syncQueueDao().enqueueAction(
            SyncQueueEntity(
                actionType = "UPDATE_ATTENDANCE",
                entityId = studentId,
                payload = "status:$status"
            )
        )
    }

    suspend fun getPendingSyncCount(context: Context): Int {
        val db = SocietyRoomDatabase.getDatabase(context)
        return db.syncQueueDao().getAllPendingActions().size
    }

    suspend fun syncPendingActions(context: Context): Int {
        val db = SocietyRoomDatabase.getDatabase(context)
        val pending = db.syncQueueDao().getAllPendingActions()
        var count = 0
        for (action in pending) {
            // محاكاة رفع البيانات ومزامنتها مع الخادم عند توفر الاتصال
            db.syncQueueDao().removeAction(action)
            count++
        }
        return count
    }
}
