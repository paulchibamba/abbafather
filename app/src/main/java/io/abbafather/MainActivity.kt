package io.abbafather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.abbafather.core.designsystem.gallery.DesignSystemGallery
import io.abbafather.core.designsystem.theme.AbbaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AbbaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AbbaTheme.colors.oat,
                ) {
                    DesignSystemGallery(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
                    )
                }
            }
        }
    }
}
