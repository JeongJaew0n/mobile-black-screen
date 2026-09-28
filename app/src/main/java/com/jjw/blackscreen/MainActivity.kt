package com.jjw.blackscreen

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjw.blackscreen.accessibility.ScreenOffAccessibilityService
import com.jjw.blackscreen.ui.theme.BlackScreenTheme
import androidx.compose.runtime.LaunchedEffect
import com.jjw.blackscreen.audio.MediaVolumeController
import com.jjw.blackscreen.data.Mode
import com.jjw.blackscreen.data.Settings
import com.jjw.blackscreen.media.MediaPermission
import com.jjw.blackscreen.media.MediaWatcher
import com.jjw.blackscreen.data.SettingsRepository
import com.jjw.blackscreen.service.ScreenCoverService
import com.jjw.blackscreen.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 앱은 기기 테마와 상관없이 항상 어둡다(BlackScreenTheme). 기본 enableEdgeToEdge() 는 시스템 테마를
        // 따라 바 스타일을 고르므로, 기기가 라이트 모드면 어두운 화면 위에 어두운 아이콘 + 밝은 내비바가
        // 나온다(실기기, 2026-09-28). 바도 항상 어두운 쪽으로 고정한다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        val repository = SettingsRepository(this)

        setContent {
            // 수묵화 팔레트. 항상 어두운 쪽 하나다 — 설정 화면이 눈부시면 용도와 어긋난다.
            BlackScreenTheme {
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
                    // 복귀 때 한 번 읽는 게 아니라 연결을 관찰한다 — 서비스는 앱보다 늦게 붙을 수 있다.
                    val accessibilityEnabled by ScreenOffAccessibilityService.connected.collectAsStateWithLifecycle()
                    var notificationAccess by remember {
                        mutableStateOf(MediaPermission.isGranted(this@MainActivity))
                    }
                    // 미리보기용 세션 관찰. 설정 화면이 RESUMED 이고 재생 바가 켜져 있을 때만.
                    val mediaWatcher = remember { MediaWatcher(this@MainActivity) }
                    LifecycleResumeEffect(settings.showMedia, notificationAccess) {
                        if (settings.showMedia && notificationAccess) mediaWatcher.start()
                        onPauseOrDispose { mediaWatcher.stop() }
                    }
                    val nowPlaying by mediaWatcher.nowPlaying.collectAsStateWithLifecycle()
                    LifecycleResumeEffect(Unit) {
                        canDrawOverlays = AndroidSettings.canDrawOverlays(this@MainActivity)
                        notificationAccess = MediaPermission.isGranted(this@MainActivity)
                        onPauseOrDispose { }
                    }

                    // 서비스가 복원하지 못하고 죽었을 때의 보험. 차폐가 떠 있지 않은데 저장값이
                    // 남아 있으면 복원을 놓친 것이다 — 볼륨은 물리 키로도 되돌릴 수 있지만
                    // 사용자가 왜 소리가 없는지 모르는 상태로 두지 않는다.
                    LaunchedEffect(Unit) {
                        val s = repository.settings.first()
                        val covering = ScreenOffAccessibilityService.instance?.isCovering == true
                        if (s.savedMediaVolume > 0 && !covering) {
                            MediaVolumeController(this@MainActivity).restore(s.savedMediaVolume)
                            repository.update { it.copy(savedMediaVolume = -1) }
                        }
                    }

                    val notificationPermission = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission(),
                    ) { /* 거부해도 차폐 자체는 동작한다. 알림만 보이지 않는다. */ }

                    // 서비스가 시스템에 의해 종료돼 있을 수 있다. 설정이 켜져 있으면 되살린다.
                    // addBubble 은 이미 떠 있으면 그냥 반환하므로 반복 호출해도 안전하다.
                    // ⚠️ 포그라운드가 아닐 때 startForegroundService 를 부르면
                    //    ForegroundServiceStartNotAllowedException 으로 앱이 죽는다.
                    //    실제로 그렇게 크래시한 적이 있어 RESUME 시점으로 옮기고
                    //    그래도 실패할 수 있으니 삼킨다.
                    // ⚠️ 여기서 composition 의 `settings` 를 읽지 말 것. 화면이 STOPPED 인 동안
                    //    수집이 멈춰 옛 스냅샷이 남는다 — 버블을 꾹 눌러 지운 뒤 앱으로 돌아오면
                    //    true 로 남은 값이 버블을 되살렸다. 저장소에서 지금 값을 다시 읽는다.
                    LifecycleResumeEffect(canDrawOverlays) {
                        if (canDrawOverlays) {
                            scope.launch {
                                val s = repository.settings.first()
                                // 닫은 버블은 여기서 돌아온다. 숨기기가 켜져 있으면 돌아오지 않는다.
                                if (s.bubbleEnabled && !s.bubbleHidden) {
                                    runCatching { ScreenCoverService.showBubble(this@MainActivity) }
                                }
                            }
                        }
                        onPauseOrDispose { }
                    }

                    SettingsScreen(
                        settings = settings,
                        onChange = { transform ->
                            scope.launch {
                                // 저장된 값을 읽어 변환한다. 화면 스냅샷을 통째로 쓰면
                                // 아직 로드되지 않은 항목들이 기본값으로 덮인다.
                                val before = repository.settings.first()
                                val after = repository.update(transform)
                                if (after.mode == Mode.OVERLAY || after.bubbleEnabled) {
                                    requestNotifications(notificationPermission::launch)
                                }
                                val wasShown = before.bubbleEnabled && !before.bubbleHidden
                                val isShown = after.bubbleEnabled && !after.bubbleHidden
                                if (isShown != wasShown) toggleBubble(isShown, canDrawOverlays)
                            }
                        },
                        onStart = { start(settings.mode) },
                        canDrawOverlays = canDrawOverlays,
                        accessibilityEnabled = accessibilityEnabled,
                        notificationAccess = notificationAccess,
                        nowPlaying = nowPlaying,
                        onRequestNotificationAccess = { MediaPermission.openSettings(this) },
                        mayBeRestricted = remember { InstallSource.mayBeRestricted(this@MainActivity) },
                        onOpenAppInfo = { InstallSource.openAppInfo(this@MainActivity) },
                        onMediaPlayPause = mediaWatcher::playPause,
                        onMediaNext = mediaWatcher::skipNext,
                        onMediaPrevious = mediaWatcher::skipPrevious,
                        onRequestOverlayPermission = ::openOverlaySettings,
                        onRequestAccessibility = { ScreenOff.openAccessibilitySettings(this) },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
                    )
                }
            }
        }
    }

    private fun start(mode: Mode) {
        when (ScreenOff.start(this, mode)) {
            ScreenOff.Result.STARTED -> Unit
            ScreenOff.Result.NEEDS_ACCESSIBILITY -> ScreenOff.openAccessibilitySettings(this)
            ScreenOff.Result.NEEDS_OVERLAY_PERMISSION -> openOverlaySettings()
        }
    }

    /** [show] = 표시 옵션이 켜져 있고 숨기기가 꺼져 있는 상태. 둘을 합친 값 하나만 본다. */
    private fun toggleBubble(show: Boolean, canDrawOverlays: Boolean) {
        when {
            !show -> ScreenCoverService.stopBubble(this)
            canDrawOverlays -> ScreenCoverService.showBubble(this)
            // 버블은 오버레이 권한 없이는 띄울 수 없다. 설정 화면으로 보내고,
            // 돌아오면 LifecycleResumeEffect 가 다시 시도한다.
            else -> openOverlaySettings()
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
