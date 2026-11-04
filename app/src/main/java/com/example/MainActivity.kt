package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.receiver.AlarmReceiver
import com.example.service.AlarmRingtoneService
import com.example.ui.screens.AlarmMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AlarmViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn on screen and show over lockscreen when alarm is ringing
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        handleAlarmIntent(intent)

        setContent {
            MyApplicationTheme {
                AlarmMainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAlarmIntent(intent)
    }

    private fun handleAlarmIntent(intent: Intent?) {
        if (intent == null) return
        val isRinging = intent.getBooleanExtra(AlarmReceiver.EXTRA_IS_RINGING, false)
        if (isRinging) {
            // Already handled by AlarmRingtoneService StateFlow
        }
    }
}
