package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreTenantRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val tenantsCollection = db.collection("tenants")

    /**
     * Real-time Flow of all Tenants directly from Firestore
     */
    fun getTenantsFlow(): Flow<List<Tenant>> = callbackFlow {
        val listener: ListenerRegistration = tenantsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreTenantRepo", "Error listening to tenants in Firestore", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        Tenant(
                            id = doc.getString("id") ?: doc.id,
                            name = doc.getString("name") ?: "",
                            shortName = doc.getString("shortName") ?: (doc.getString("name") ?: ""),
                            city = doc.getString("city") ?: "",
                            code = doc.getString("code") ?: "",
                            ringsCount = (doc.getLong("ringsCount") ?: 0L).toInt(),
                            studentsCount = (doc.getLong("studentsCount") ?: 0L).toInt(),
                            teachersCount = (doc.getLong("teachersCount") ?: 0L).toInt(),
                            attendanceRate = (doc.getLong("attendanceRate") ?: 95L).toInt(),
                            unreadMessagesCount = (doc.getLong("unreadMessagesCount") ?: 0L).toInt(),
                            licenseNumber = doc.getString("licenseNumber") ?: "",
                            country = doc.getString("country") ?: "اليمن",
                            governorate = doc.getString("governorate") ?: (doc.getString("city") ?: "اليمن")
                        )
                    } catch (e: Exception) {
                        Log.e("FirestoreTenantRepo", "Failed to parse tenant doc", e)
                        null
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Add or update a tenant document in Firestore
     */
    suspend fun saveTenant(tenant: Tenant) {
        val map = hashMapOf(
            "id" to tenant.id,
            "name" to tenant.name,
            "shortName" to tenant.shortName,
            "city" to tenant.city,
            "code" to tenant.code,
            "ringsCount" to tenant.ringsCount,
            "studentsCount" to tenant.studentsCount,
            "teachersCount" to tenant.teachersCount,
            "attendanceRate" to tenant.attendanceRate,
            "unreadMessagesCount" to tenant.unreadMessagesCount,
            "licenseNumber" to tenant.licenseNumber,
            "country" to tenant.country,
            "governorate" to tenant.governorate,
            "updatedAt" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
            "syncedBy" to "adffakff@gmail.com"
        )
        tenantsCollection.document(tenant.id).set(map).await()
    }

    /**
     * Delete a tenant document from Firestore
     */
    suspend fun deleteTenant(tenantId: String) {
        tenantsCollection.document(tenantId).delete().await()
    }

    /**
     * Seed initial tenants list into Firestore if collection is empty
     */
    suspend fun seedInitialTenantsIfEmpty(initialList: List<Tenant>): Int {
        val snapshot = tenantsCollection.get().await()
        if (snapshot.isEmpty) {
            for (tenant in initialList) {
                saveTenant(tenant)
            }
            return initialList.size
        }
        return snapshot.size()
    }

    /**
     * Force upload all tenants into Firestore (overwriting/updating)
     */
    suspend fun syncAllToFirestore(list: List<Tenant>): Int {
        for (tenant in list) {
            saveTenant(tenant)
        }
        return list.size
    }
}
