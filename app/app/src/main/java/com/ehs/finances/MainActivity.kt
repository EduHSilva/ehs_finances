package com.ehs.finances

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ehs.finances.di.AppContainer
import com.ehs.finances.ui.FinancesApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container: AppContainer = (application as FinancesApplication).container
        setContent {
            FinancesApp(container)
        }
    }
}
