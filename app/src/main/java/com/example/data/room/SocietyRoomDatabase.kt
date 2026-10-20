package com.example.data.room

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "offline_students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val ringId: String,
    val tenantId: String,
    val name: String,
    val attendanceStatus: String,
    val memorizationPart: String,
    val phone: String,
    val isSynced: Boolean = true
)

@Entity(tableName = "offline_rings")
data class RingEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val teacherName: String,
    val studentsCount: Int,
    val isSynced: Boolean = true
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val entityId: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface StudentDao {
    @Query("SELECT * FROM offline_students WHERE tenantId = :tenantId")
    fun getStudentsForTenant(tenantId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM offline_students WHERE ringId = :ringId")
    fun getStudentsForRing(ringId: String): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Query("UPDATE offline_students SET attendanceStatus = :status, isSynced = :synced WHERE id = :studentId")
    suspend fun updateAttendance(studentId: String, status: String, synced: Boolean)

    @Query("SELECT * FROM offline_students WHERE isSynced = 0")
    suspend fun getUnsyncedStudents(): List<StudentEntity>
}

@Dao
interface RingDao {
    @Query("SELECT * FROM offline_rings WHERE tenantId = :tenantId")
    fun getRingsForTenant(tenantId: String): Flow<List<RingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRings(rings: List<RingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRing(ring: RingEntity)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getAllPendingActions(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueAction(action: SyncQueueEntity)

    @Delete
    suspend fun removeAction(action: SyncQueueEntity)

    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()
}

@Database(entities = [StudentEntity::class, RingEntity::class, SyncQueueEntity::class], version = 1, exportSchema = false)
abstract class SocietyRoomDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun ringDao(): RingDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: SocietyRoomDatabase? = null

        fun getDatabase(context: Context): SocietyRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SocietyRoomDatabase::class.java,
                    "society_offline_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
