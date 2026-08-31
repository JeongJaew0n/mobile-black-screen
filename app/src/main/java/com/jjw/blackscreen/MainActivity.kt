package com.jjw.blackscreen

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjw.blackscreen.blackout.BlackoutActivity
import com.jjw.blackscreen.data.Mode
import com.jjw.blackscreen.data.Settings
import com.jjw.blackscreen.data.SettingsRepository
import com.jjw.blackscreen.overlay.OverlayService
import com.jjw.blackscreen.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = SettingsRepository(this)

        setContent {
            // 이 앱은 항상 어두운 팔레트로 간다. 설정 화면이 눈부시면 용도와 어긋난다.
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    val scope = rememberCoroutineScope()
                    val settings by repository.settings.collectAsStateWithLifecycle(
                        initialValue = Settings(),
                    )

                    // 오버레이 권한은 런타임 권한이 아니라 설정 화면에서 켜고 돌아온다.
                    // 따라서 결과 콜백이 없고, 복귀할 때마다 다시 확인해야 한다.
                    var canDrawOverlays by remember {
                        mutableStateOf(AndroidSettings.canDrawOverlays(this))
                    }
                    LifecycleResumeEffect(Unit) {
                        canDrawOverlays = AndroidSettings.canDrawOverlays(this@MainActivity)
                        onPauseOrDispose { }
                    }

                    val notificationPermission = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission(),
                    ) { /* 거부해도 차폐 자체는 동작한다. 알림만 보이지 않는다. */ }

                    SettingsScreen(
                        settings = settings,
                        onChange = { updated ->
                            scope.launch { repository.update { updated } }
                            if (updated.mode == Mode.OVERLAY) requestNotifications(notificationPermission::launch)
                        },
                        onStart = { start(settings.mode, canDrawOverlays) },
                        canDrawOverlays = canDrawOverlays,
                        onRequestOverlayPermission = ::openOverlaySettings,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
                    )
                }
            }
        }
    }

    private fun start(mode: Mode, canDrawOverlays: Boolean) {
        when (mode) {
            Mode.BLACKOUT -> startActivity(Intent(this, BlackoutActivity::class.java))
            Mode.OVERLAY ->
                if (canDrawOverlays) OverlayService.start(this) else openOverlaySettings()
        }
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri(),
            ),
        )
    }

    private fun requestNotifications(launch: (String) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
