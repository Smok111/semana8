package com.example.semana8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.semana8.auth.AuthRepository
import com.example.semana8.auth.LoginViewModel
import com.example.semana8.auth.LoginViewModelFactory
import com.example.semana8.data.SessionDataStore
import com.example.semana8.ui.theme.Semana8Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authRepository = AuthRepository()
        val sessionDataStore = SessionDataStore(applicationContext)
        val startDestination = runBlocking {
            val hasLocalSession = sessionDataStore.isLoggedInFlow.first()
            val hasFirebaseSession = authRepository.hasActiveFirebaseSession()
            if (hasLocalSession && hasFirebaseSession) Screen.Home.route else Screen.Login.route
        }

        setContent {
            Semana8Theme {
                AppNavHost(startDestination, authRepository, sessionDataStore)
            }
        }
    }
}

@Composable
private fun AppNavHost(
    startDestination: String,
    authRepository: AuthRepository,
    sessionDataStore: SessionDataStore
) {
    val navController = rememberNavController()
    // NOTE: don't create the LoginViewModel here (shared across nav graph),
    // create it inside the Login composable so its state resets when navigating back.

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModelFactory(authRepository, sessionDataStore)
            )

            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val scope = rememberCoroutineScope()
            val ctx = LocalContext.current

            HomeScreen(
                onLogout = {
                    scope.launch {
                        try {
                            authRepository.logout()
                        } catch (_: Exception) {
                        }
                        try {
                            sessionDataStore.setLoggedIn(false)
                        } catch (_: Exception) {
                        }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                        android.widget.Toast.makeText(ctx, "Sesión cerrada", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Iniciar sesion",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                singleLine = true,
                label = { Text("Correo") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("Contrasena") }
            )

            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.login() },
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Entrar")
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onLogout: () -> Unit) {
    val showConfirm = remember { mutableStateOf(false) }

    if (showConfirm.value) {
        AlertDialog(
            onDismissRequest = { showConfirm.value = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Estás seguro que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm.value = false
                    onLogout()
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm.value = false }) {
                    Text("No")
                }
            }
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Bienvenido, inicio de sesion correcto")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showConfirm.value = true }) {
                Text("Cerrar sesion")
            }
        }
    }
}