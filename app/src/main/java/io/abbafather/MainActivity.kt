    package io.abbafather

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.abbafather.core.designsystem.theme.AbbaTheme
import io.abbafather.navigation.AbbaNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app has one committed light look, so the bars are told so explicitly. Left to its
        // default, `enableEdgeToEdge` follows the *system's* dark-theme setting and paints white
        // icons over the oat ground and the sage home header on any phone set to dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            AbbaTheme {
                // No inset padding here: the shell hands the whole window to the nav host so the
                // prayer session can own the system bars.
                AbbaNavHost()
            }
        }
    }
}
