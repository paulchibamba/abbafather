package io.abbafather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.abbafather.core.designsystem.theme.AbbaTheme
import io.abbafather.navigation.AbbaNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AbbaTheme {
                // No inset padding here: the shell hands the whole window to the nav host so the
                // prayer session can own the system bars.
                AbbaNavHost()
            }
        }
    }
}
