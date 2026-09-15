package com.jjw.blackscreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.jjw.blackscreen.biometric.BiometricUnlockActivity
import com.jjw.blackscreen.media.NowPlaying
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjw.blackscreen.data.Gesture
import com.jjw.blackscreen.data.Settings
import kotlin.math.roundToInt

/**
 * Blackout 모드와 Overlay 모드가 공유하는 차폐 화면.
 *
 * 이 Composable 은 Activity 창과 Service 가 WindowManager 에 붙인 창 양쪽에서 그대로
 * 재사용된다. 두 모드의 차이는 "어떤 창에 올리는가"뿐이고 표시 내용은 완전히 동일하다.
 */
@Composable
fun BlackScreenRoot(
    settings: Settings,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    /** 지문 프롬프트가 떠 있는 동안 true — 검정을 반투명으로 낮춰 아래 SystemUI 가 비친다. */
    dimmed: Boolean = false,
    /** 재생 중인 것. 호출부(접근성 서비스)가 차폐 수명에 맞춰 관찰한다. */
    nowPlaying: NowPlaying? = null,
    /** 재생 바를 켰는데 알림 접근이 없다 — 재생 바 자리에 안내 문구를 띄운다. */
    mediaPermissionMissing: Boolean = false,
    onMediaPlayPause: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaPrevious: () -> Unit = {},
) {
    var slideOffset by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableIntStateOf(0) }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { containerHeight = it.height },
    ) {
        // 검은 표면 전체가 함께 움직여야 비워진 자리로 아래 앱이 드러난다.
        // 배경을 바깥 Box 에 두면 밀어도 검정이 그대로 남는다.
        Box(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(0, slideOffset.roundToInt()) }
                .background(Color.Black.copy(alpha = if (dimmed) DimmedAlpha else 1f)),
        ) {
            BlackScreenContent(settings)
            if (settings.showMedia) {
                MediaBarLayer(
                    nowPlaying = nowPlaying,
                    permissionMissing = mediaPermissionMissing,
                    tint = settings.textColor,
                    fontFamily = settings.clockFont.family,
                )
            }
        }

        if (settings.unlockGesture == Gesture.SWIPE) {
            SlideToDismissLayer(
                onOffsetChange = { slideOffset = it },
                containerHeight = containerHeight,
                onUnlock = onUnlock,
            )
        } else if (settings.unlockGesture == Gesture.FINGERPRINT) {
            // 탭하면 투명 Activity 가 시스템 지문 프롬프트를 띄운다. 차폐 창이 보이는 동안은
            // 우리 UID 에 보이는 창이 있어 백그라운드 Activity 시작 제한에 걸리지 않는다.
            val context = LocalContext.current
            FingerprintUnlockLayer(
                onTrigger = {
                    runCatching {
                        context.startActivity(
                            Intent(context, BiometricUnlockActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                tint = settings.textColor,
                // 재생 컨트롤이 화면에 있을 때만 아이콘 영역으로 좁힌다.
                iconOnly = settings.showMedia && nowPlaying != null,
            )
        } else if (settings.unlockGesture == Gesture.SLIDE_TO_UNLOCK) {
            SlideToUnlockLayer(
                tint = settings.textColor,
                onUnlock = onUnlock,
            )
        } else {
            UnlockGestureLayer(
                gesture = settings.unlockGesture,
                ringColor = settings.textColor,
                holdMillis = settings.holdMillis,
                onUnlock = onUnlock,
            )
        }

        // 제어 버튼은 해제 레이어보다 **위**에 온다. 버튼이 탭을 소비해야 그 탭이 해제 제스처로
        // 새지 않는다. 그래서 Box 의 마지막 자식이다.
        if (settings.showMedia && nowPlaying != null) {
            MediaControlsLayer(
                nowPlaying = nowPlaying,
                tint = settings.textColor,
                onPlayPause = onMediaPlayPause,
                onNext = onMediaNext,
                onPrevious = onMediaPrevious,
            )
        }
    }
}

/** 세로 -0.3 = 정중앙에서 위쪽으로 화면 높이의 15% 지점. */
private val ContentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.3f)

/**
 * 인증 UI(지문 프롬프트·PIN 입력)가 떠 있는 동안의 검정 알파.
 *
 * 0.55 로는 PIN 입력 화면의 숫자가 잘 안 보였다. 탈출구는 **실제로 쓸 수 있어야** 의미가 있어
 * 거의 투명하게 낮춘다. 인증하려는 사람은 폰 주인이고, 차폐는 보안이 아니라 "꺼진 것처럼
 * 보이게" 하는 기능이라 이 몇 초 동안 아래가 드러나도 목적에 어긋나지 않는다.
 */
private const val DimmedAlpha = 0.12f

/** 미리보기 상자 높이. 시계(64sp)와 문장 한 줄이 들어가는 최소치. */
private val PreviewHeight = 180.dp
private val PreviewHeightWithMedia = 300.dp

/**
 * 설정 화면용 미리보기. **차폐 화면과 같은 [BlackScreenContent] 를 그대로 그린다.**
 *
 * 폰트 크기·정렬·번인 궤도까지 실제와 같은 코드다 — 미리보기 전용 레이아웃을 따로 두면
 * 실제와 어긋나는 날이 온다. 다른 것은 상자 높이와 둥근 모서리뿐이다.
 *
 * ⚠️ 패널 밝기(0.30)는 보여줄 수 없다. 이 앱의 창 밝기는 `screenBrightness` 로 낮추는데
 * 설정 화면 창을 낮추면 설정 자체가 안 보인다. 실제 화면은 이보다 어둡다 — 설정 화면이
 * 그 사실을 캡션으로 알린다.
 */
@Composable
fun BlackScreenPreview(
    settings: Settings,
    modifier: Modifier = Modifier,
    nowPlaying: NowPlaying? = null,
    mediaPermissionMissing: Boolean = false,
    onMediaPlayPause: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaPrevious: () -> Unit = {},
) {
    val showBar = settings.showMedia && (nowPlaying != null || mediaPermissionMissing)
    Box(
        modifier
            .fillMaxWidth()
            // 재생 바까지 보이려면 상자가 더 커야 한다. 시계 아래 여백에 겹치지 않게.
            .height(if (showBar) PreviewHeightWithMedia else PreviewHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
    ) {
        BlackScreenContent(settings)
        if (showBar) {
            MediaBarLayer(
                nowPlaying = nowPlaying,
                permissionMissing = mediaPermissionMissing,
                tint = settings.textColor,
                fontFamily = settings.clockFont.family,
                bottomMargin = 16.dp,
            )
            if (nowPlaying != null) {
                MediaControlsLayer(
                    nowPlaying = nowPlaying,
                    tint = settings.textColor,
                    onPlayPause = onMediaPlayPause,
                    onNext = onMediaNext,
                    onPrevious = onMediaPrevious,
                    bottomMargin = 16.dp,
                )
            }
        }
    }
}

@Composable
private fun BlackScreenContent(settings: Settings) {
    // 기본값은 요구사항대로 완전한 무표시다. 이때는 텍스트 레이아웃 자체를 만들지 않는다.
    if (!settings.hasContent) return

    val offset = rememberBurnInShift()

    // 정중앙보다 조금 위. 손에 들었을 때 시선이 자연스럽게 닿는 높이다.
    Box(Modifier.fillMaxSize(), contentAlignment = ContentAlignment) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset { offset },
        ) {
            if (settings.showClock) {
                Text(
                    text = rememberClockText(settings.clockStyle),
                    color = settings.textColor,
                    fontSize = 64.sp,
                    fontFamily = settings.clockFont.family,
                    fontWeight = settings.clockFont.weight,
                )
            }
            if (settings.showDate) {
                // 시계와 같은 글꼴 계열, 굵기는 보통. 한글(년·월·일)은 어차피 한글 글꼴로 떨어진다.
                Text(
                    text = rememberDateText(settings.dateStyle),
                    color = settings.textColor,
                    fontSize = 22.sp,
                    fontFamily = settings.clockFont.family,
                    modifier = Modifier.padding(top = if (settings.showClock) 8.dp else 0.dp),
                )
            }
            if (settings.showBattery) {
                BatteryLine(
                    status = rememberBatteryStatus(),
                    tint = settings.textColor,
                    fontFamily = settings.clockFont.family,
                    modifier = Modifier.padding(top = if (settings.showClock || settings.showDate) 8.dp else 0.dp),
                )
            }
            if (settings.sentence.isNotBlank()) {
                Text(
                    text = settings.sentence,
                    color = settings.textColor,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .padding(top = if (settings.showClock || settings.showDate || settings.showBattery) 24.dp else 0.dp),
                )
            }
        }
    }
}
