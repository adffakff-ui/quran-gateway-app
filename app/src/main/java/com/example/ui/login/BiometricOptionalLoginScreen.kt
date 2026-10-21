package com.example.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.SocietyRepository
import com.example.ui.security.BiometricHelper

@Composable
fun BiometricOptionalLoginScreen(onSuccess: () -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (SocietyRepository.isBiometricLoginEnabled) {
            BiometricHelper.showBiometricPrompt(
                context = context,
                onSuccess = { onSuccess() },
                onError = {
                    errorMessage = it
                    onError(it)
                }
            )
        } else {
            onSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        } else {
            CircularProgressIndicator()
        }
    }
}
