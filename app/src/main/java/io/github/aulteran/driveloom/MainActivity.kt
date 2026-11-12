package io.github.aulteran.driveloom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.aulteran.driveloom.ui.PlaceholderScreen
import io.github.aulteran.driveloom.ui.theme.DriveLoomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DriveLoomTheme {
                PlaceholderScreen()
            }
        }
    }
}
