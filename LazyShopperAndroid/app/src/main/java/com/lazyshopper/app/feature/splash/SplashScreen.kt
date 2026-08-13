package com.lazyshopper.app.feature.splash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.lazyshopper.app.core.data.local.SessionManager
import com.lazyshopper.app.core.ui.components.FullScreenLoading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(private val session: SessionManager) : ViewModel() {
    suspend fun resolveRole(): String? = session.sessionFlow.first().let { s ->
        if (s.token.isNullOrBlank()) null else s.role
    }
}

@Composable
fun SplashScreen(
    onResolved: (role: String?) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        onResolved(viewModel.resolveRole())
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text("Lazy Shopper", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        FullScreenLoading()
    }
}
