package com.premiabrygady.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.premiabrygady.app.ui.PremiaBrygadyApp
import com.premiabrygady.app.ui.PremiaBrygadyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PremiaBrygadyTheme {
                PremiaBrygadyApp()
            }
        }
    }
}
