package ru.macht.investmanager.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.macht.investmanager.data.SettingsManager
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {
    var bcsKey by mutableStateOf("")
    var bcsRefreshToken by mutableStateOf("")
    var deepSeekKey by mutableStateOf("")

    init {
        viewModelScope.launch {
            bcsKey = settingsManager.bcsKeyFlow.first() ?: ""
            bcsRefreshToken = settingsManager.bcsRefreshTokenFlow.first() ?: ""
            deepSeekKey = settingsManager.deepSeekKeyFlow.first() ?: ""
        }
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            settingsManager.saveKeys(bcsKey, bcsRefreshToken, deepSeekKey)
            onSaved()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки API") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Введите ваши ключи API для доступа к данным.")

            OutlinedTextField(
                value = viewModel.bcsKey,
                onValueChange = { viewModel.bcsKey = it },
                label = { Text("BCS API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = viewModel.bcsRefreshToken,
                onValueChange = { viewModel.bcsRefreshToken = it },
                label = { Text("BCS Refresh Token") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = viewModel.deepSeekKey,
                onValueChange = { viewModel.deepSeekKey = it },
                label = { Text("DeepSeek API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Button(
                onClick = {
                    viewModel.save {
                        onBackClick()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }
}