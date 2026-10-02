package com.synthesis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.synthesis.data.billing.SubscriptionManager
import com.synthesis.ui.navigation.SynthesisNavHost
import com.synthesis.ui.screens.billing.LockScreenPaywall
import com.synthesis.ui.theme.SynthesisTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var subscriptionManager: SubscriptionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissions()
        startTrialTickLoop()

        setContent {
            SynthesisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLocked by subscriptionManager.isLocked.collectAsState()
                    if (isLocked) {
                        LockScreenPaywall(
                            subscriptionManager = subscriptionManager,
                            onUnlocked = { /* app unlocks reactively */ }
                        )
                    } else {
                        SynthesisNavHost()
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun startTrialTickLoop() {
        lifecycleScope.launch {
            while (isActive) {
                delay(15_000L) // Tick usage every 15 seconds
                subscriptionManager.tickUsage()
            }
        }
    }
}
