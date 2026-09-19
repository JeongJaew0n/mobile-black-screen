package com.jjw.blackscreen.media

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 지금 재생 중인(또는 일시정지된) 것. 위치는 [currentPositionMs] 로 계산한다 — 저장값은 스냅샷이다. */
data class NowPlaying(
    val title: String,
    val subtitle: String,
    val positionMs: Long,
    val durationMs: Long,
    val playbackSpeed: Float,
    val canNext: Boolean,
    val canPrev: Boolean,
    /** `PlaybackState.lastPositionUpdateTime` — `SystemClock.elapsedRealtime()` 기준. */
    val updatedAtMs: Long,
    val playing: Boolean,
) {
    /** 시스템이 준 스냅샷에서 지금 위치를 계산한다. 초마다 물어볼 필요가 없는 이유다. */
    fun currentPositionMs(now: Long = SystemClock.elapsedRealtime()): Long {
        if (!playing) return positionMs.coerceAtLeast(0)
        val advanced = positionMs + ((now - updatedAtMs) * playbackSpeed).toLong()
        val cap = if (durationMs > 0) durationMs else Long.MAX_VALUE
        return advanced.coerceIn(0, cap)
    }
}

/**
 * 활성 미디어 세션을 지켜본다. **차폐가 떠 있을 때만** 살아 있어야 한다 — 호출부가 [start]/[stop] 을
 * 창 수명에 맞춘다. 백그라운드에서 세션을 듣지 않는다(전력, 그리고 원칙).
 *
 * 폴링하지 않는다. 세션 목록 변화와 각 세션의 상태·메타데이터 콜백만 받는다.
 * 콜백은 메인 Looper 로 받는다 — `MediaController.Callback` 은 등록한 Handler 의 스레드로 온다.
 */
class MediaWatcher(private val context: Context) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val component = MediaPermission.component(context)
    private val handler = Handler(Looper.getMainLooper())

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

    /** 권한이 없거나 도중에 빼앗겼다. 호출부가 안내 문구로 바꾼다. */
    private val _permissionLost = MutableStateFlow(false)
    val permissionLost: StateFlow<Boolean> = _permissionLost

    private var controllers: List<MediaController> = emptyList()

    /** [refresh] 가 고른 세션. 제어 버튼이 이걸 조작한다. null 이면 제어할 것이 없다. */
    private var selected: MediaController? = null
    private var started = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onSessionDestroyed() = reloadFromManager()
    }
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { reload(it) }

    fun start() {
        if (started) return
        started = true
        _permissionLost.value = false
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
            reload(manager.getActiveSessions(component))
        } catch (e: SecurityException) {
            // 알림 접근이 없거나 사용자가 껐다. 조용히 실패하면 "왜 안 뜨지" 가 되니 표시로 넘긴다.
            //
            // ⚠️ add 가 성공한 뒤 getActiveSessions 에서 던질 수 있다. 그때 리스너를 회수하지
            // 않으면 started=false 라 stop() 이 일찍 돌아가 영영 해제되지 않고, 다음 start()
            // 에서 중복 등록된다. 등록 전에 던졌다면 remove 는 무해하다.
            runCatching { manager.removeOnActiveSessionsChangedListener(sessionsListener) }
            _permissionLost.value = true
            started = false
        }
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { manager.removeOnActiveSessionsChangedListener(sessionsListener) }
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = emptyList()
        selected = null
        _nowPlaying.value = null
    }

    private fun reloadFromManager() {
        val list = try {
            manager.getActiveSessions(component)
        } catch (e: SecurityException) {
            // 도중에 권한을 잃었다. 콜백을 남겨 둘 이유가 없으니 전부 정리하고 물러난다.
            _permissionLost.value = true
            stop()
            return
        }
        reload(list)
    }

    private fun reload(list: List<MediaController>?) {
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = list.orEmpty()
        controllers.forEach { it.registerCallback(controllerCallback, handler) }
        refresh()
    }

    /**
     * 세션 하나를 고른다. 재생 중인 것 우선, 없으면 일시정지·버퍼링 중인 것(멈춘 곡이 무엇인지
     * 보는 용도가 있다). 정지·없음·오류는 표시하지 않는다 → null.
     */
    private fun refresh() {
        val playing = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val paused = controllers.firstOrNull {
            it.playbackState?.state in setOf(PlaybackState.STATE_PAUSED, PlaybackState.STATE_BUFFERING)
        }
        selected = playing ?: paused
        _nowPlaying.value = selected?.let(::toNowPlaying)
    }

    // 제어는 **미디어 버튼 이벤트**로 보낸다. transportControls.pause()/play() 는 세션이
    // ACTION_PAUSE / ACTION_PLAY 를 명시적으로 지원할 때만 먹는데, 곰오디오처럼 PLAY_PAUSE
    // 토글만 지원하는(actions=0x204) 앱이 흔하다 — 그런 앱에 pause() 를 보내면 조용히 무시된다.
    // 미디어 키는 잠금화면 재생 버튼이 쓰는 것과 같은 경로라 호환성이 가장 넓다.
    // 상태 변화는 세션 콜백이 알려 주므로 nowPlaying 을 직접 고치지 않는다.
    private fun sendKey(keyCode: Int) {
        val c = selected ?: return
        c.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        c.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
    fun playPause() = sendKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun skipNext() = sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun skipPrevious() = sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    private fun toNowPlaying(c: MediaController): NowPlaying? {
        val state = c.playbackState ?: return null
        val meta = c.metadata
        val appLabel = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
        }.getOrDefault(c.packageName)
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: appLabel
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val subtitle = if (artist.isNullOrBlank() || artist == title) appLabel else "$artist · $appLabel"
        return NowPlaying(
            title = title,
            subtitle = subtitle,
            positionMs = state.position,
            durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: -1L,
            playbackSpeed = if (state.playbackSpeed == 0f) 1f else state.playbackSpeed,
            updatedAtMs = state.lastPositionUpdateTime,
            canNext = state.actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            canPrev = state.actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            playing = state.state == PlaybackState.STATE_PLAYING,
        )
    }
}
