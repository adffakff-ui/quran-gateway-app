package com.example.ui.settings

import androidx.compose.runtime.Composable
import com.example.data.Tenant

/**
 * AppSettingsScreen
 * Delegates to the comprehensive SettingsScreen component to maintain backward compatibility.
 */
@Composable
fun AppSettingsScreen(
    currentTenant: Tenant,
    onTenantUpdated: (Tenant) -> Unit = {},
    darkModePref: String = "system",
    onDarkModeChange: (String) -> Unit = {},
    onNavigateToConnectedDevices: () -> Unit = {},
    onNavigateToAboutSociety: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    SettingsScreen(
        currentTenant = currentTenant,
        onTenantUpdated = onTenantUpdated,
        darkModePref = darkModePref,
        onDarkModeChange = onDarkModeChange,
        onNavigateToConnectedDevices = onNavigateToConnectedDevices,
        onNavigateToAboutSociety = onNavigateToAboutSociety,
        onBack = onBack
    )
}
