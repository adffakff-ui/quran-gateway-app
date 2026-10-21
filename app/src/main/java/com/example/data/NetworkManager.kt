package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * مدير الاتصال بالإنترنت لمراقبة حالة الشبكة وفرض سياسة العمل "أونلاين فقط"
 */
object NetworkManager {
    @Composable
    fun rememberConnectivityState(): State<Boolean> {
        val context = LocalContext.current
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val isConnected = remember {
            mutableStateOf(
                connectivityManager.activeNetwork?.let {
                    connectivityManager.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } ?: false
            )
        }

        DisposableEffect(context) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isConnected.value = true
                }
                override fun onLost(network: Network) {
                    isConnected.value = false
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
            onDispose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
        return isConnected
    }
}
