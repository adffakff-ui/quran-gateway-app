package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

enum class SyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE_PENDING,
    ERROR
}

object SyncManager {
    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow("الآن (متزامن مع السحابة)")
    val lastSyncedTime: StateFlow<String> = _lastSyncedTime.asStateFlow()

    suspend fun triggerSync() {
        _syncStatus.value = SyncStatus.SYNCING
        _lastSyncedTime.value = "جارِ المزامنة مع السحابة..."
        delay(1500) // Simulate cloud sync with Firestore
        _syncStatus.value = SyncStatus.SYNCED
        _lastSyncedTime.value = "تمت المزامنة بنجاح ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale("ar")).format(java.util.Date())}"
    }

    fun setOfflineMode() {
        _syncStatus.value = SyncStatus.OFFLINE_PENDING
        _lastSyncedTime.value = "غير متصل (سيتم المزامنة لاحقاً)"
    }
}
