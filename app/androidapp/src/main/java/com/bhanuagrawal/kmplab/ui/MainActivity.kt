package com.bhanuagrawal.kmplab.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bhanuagrawal.kmplab.ui.theme.KmpLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startRoute = intent.getStringExtra("start_route") ?: Screen.HomeScreen.route

        setContent {

            KmpLabTheme {
                MainAppComposable(startRoute = startRoute)
            }


        }
        reportFullyDrawn()
    }
}


