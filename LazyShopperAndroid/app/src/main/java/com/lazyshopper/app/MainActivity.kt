package com.lazyshopper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.lazyshopper.app.core.navigation.RootNavGraph
import com.lazyshopper.app.core.theme.LazyShopperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LazyShopperRoot()
        }
    }
}

@Composable
private fun LazyShopperRoot() {
    LazyShopperTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            RootNavGraph(navController = navController)
        }
    }
}
