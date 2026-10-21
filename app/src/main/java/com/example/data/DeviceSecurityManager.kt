package com.example.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DeviceType(val label: String) {
    SMARTPHONE("هاتف ذكي"),
    TABLET("جهاز لوحي"),
    DESKTOP("حاسوب مكتبي / متصفح"),
    UNKNOWN("جهاز غير معروف")
}

data class ConnectedDevice(
    val id: String,
    val deviceName: String,
    val deviceModel: String,
    val deviceType: DeviceType,
    val osVersion: String,
    val ipAddress: String,
    val location: String,
    val lastActive: String,
    val loginDate: String,
    val isCurrentDevice: Boolean = false,
    val isTrusted: Boolean = true,
    val browserOrApp: String = "تطبيق المشرف (Android)",
    val isSuspected: Boolean = false
)

object DeviceSecurityManager {
    // Current Supervisor Devices
    var devices by mutableStateOf<List<ConnectedDevice>>(
        listOf(
            ConnectedDevice(
                id = "dev_curr_01",
                deviceName = "هاتفي الحالي (هذا الجهاز)",
                deviceModel = "Samsung Galaxy S24 Ultra",
                deviceType = DeviceType.SMARTPHONE,
                osVersion = "Android 14 (One UI 6.1)",
                ipAddress = "185.120.45.19",
                location = "الرياض، المملكة العربية السعودية",
                lastActive = "نشط الآن",
                loginDate = "اليوم، 10:15 ص",
                isCurrentDevice = true,
                isTrusted = true,
                browserOrApp = "تطبيق جمعية تحفيظ القرآن (v1.0.0)"
            ),
            ConnectedDevice(
                id = "dev_tablet_02",
                deviceName = "جهاز التسميع بالحلقة",
                deviceModel = "iPad Pro 11-inch",
                deviceType = DeviceType.TABLET,
                osVersion = "iPadOS 17.4",
                ipAddress = "185.120.45.22",
                location = "الرياض - قاعة الفاروق",
                lastActive = "منذ 45 دقيقة",
                loginDate = "أمس، 04:30 م",
                isCurrentDevice = false,
                isTrusted = true,
                browserOrApp = "بوابة المعلم اللوحية"
            ),
            ConnectedDevice(
                id = "dev_pc_03",
                deviceName = "حاسوب إدارة المركز",
                deviceModel = "Dell OptiPlex 7090",
                deviceType = DeviceType.DESKTOP,
                osVersion = "Windows 11 Pro",
                ipAddress = "176.105.88.14",
                location = "مبنى الإدارة العامة",
                lastActive = "منذ 3 ساعات",
                loginDate = "منذ 3 أيام",
                isCurrentDevice = false,
                isTrusted = true,
                browserOrApp = "Google Chrome 124.0"
            ),
            ConnectedDevice(
                id = "dev_unknown_04",
                deviceName = "جهاز غير معروف (متصفح مجهول)",
                deviceModel = "Linux x86_64 / WebKit",
                deviceType = DeviceType.UNKNOWN,
                osVersion = "نظام تشغيل غير محدد",
                ipAddress = "94.200.12.85",
                location = "جدة، المملكة العربية السعودية (موقع غير معتاد)",
                lastActive = "أمس، 11:20 م",
                loginDate = "أمس، 11:20 م",
                isCurrentDevice = false,
                isTrusted = false,
                browserOrApp = "متصفح غير موثق",
                isSuspected = true
            )
        )
    )

    fun remoteLogoutDevice(deviceId: String): Boolean {
        val device = devices.find { it.id == deviceId } ?: return false
        if (device.isCurrentDevice) return false // Cannot remote logout self
        devices = devices.filterNot { it.id == deviceId }
        return true
    }

    fun remoteLogoutAllOtherDevices(): Int {
        val count = devices.count { !it.isCurrentDevice }
        devices = devices.filter { it.isCurrentDevice }
        return count
    }

    fun toggleDeviceTrust(deviceId: String) {
        devices = devices.map {
            if (it.id == deviceId && !it.isCurrentDevice) {
                it.copy(isTrusted = !it.isTrusted, isSuspected = false)
            } else {
                it
            }
        }
    }

    fun registerNewDevice(
        deviceName: String,
        deviceModel: String,
        deviceType: DeviceType,
        location: String
    ) {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val newDev = ConnectedDevice(
            id = "dev_${System.currentTimeMillis()}",
            deviceName = deviceName.ifBlank { "جهاز إشرافي جديد" },
            deviceModel = deviceModel.ifBlank { "هاتف أندرويد" },
            deviceType = deviceType,
            osVersion = "Android 14",
            ipAddress = "192.168.1.${(20..250).random()}",
            location = location.ifBlank { "المملكة العربية السعودية" },
            lastActive = "نشط الآن",
            loginDate = dateFormat.format(Date()),
            isCurrentDevice = false,
            isTrusted = true
        )
        devices = devices + newDev
    }
}
