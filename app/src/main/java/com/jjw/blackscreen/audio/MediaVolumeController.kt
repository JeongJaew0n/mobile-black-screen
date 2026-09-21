package com.jjw.blackscreen.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.jjw.blackscreen.data.MediaVolumeAction

/**
 * 차폐 중 미디어 소리를 끈다. **미디어(`STREAM_MUSIC`)만** 건드린다 — 알림·전화·알람은 그대로다.
 *
 * 새 권한이 필요 없다. `ACCESS_NOTIFICATION_POLICY`(방해 금지)는 벨소리·알림 스트림을 무음으로
 * 만들 때 요구되는 것이고 `STREAM_MUSIC` 은 대상이 아니다. 이 앱은 이미 특수 권한을 셋 받고
 * 있어 넷째를 더하지 않는 것이 중요했다.
 *
 * 볼륨을 바꿀 때 `flags = 0` 을 넘겨 **시스템 볼륨 팝업이 뜨지 않게** 한다. 차폐 위에 팝업이
 * 뜨면 "완전한 검정" 이 깨진다.
 */
class MediaVolumeController(context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    /** 블루투스로 나가는지 판단할 때 볼 출력 장치 종류. BLE 상수는 버전이 갈려 조건부로 넣는다. */
    private val bluetoothTypes: Set<Int> = buildSet {
        add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(AudioDeviceInfo.TYPE_BLE_HEADSET)
            add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(AudioDeviceInfo.TYPE_BLE_BROADCAST)
        }
    }

    /**
     * 지금 미디어가 **블루투스로 나가는가.**
     *
     * 사용자 결정으로 블루투스로 듣는 중에는 소리를 끄지 않는다 — 그때는 "조용히 두려는" 게
     * 아니라 "소리는 그대로 듣고 화면만 끄려는" 상황이기 때문이다. Android 는 출력 장치별로
     * 미디어 볼륨을 따로 기억하므로, 그냥 두면 블루투스 쪽 볼륨이 0 이 된다.
     *
     * **안전한 실패 방향은 "끄지 않는 것"** 이다. 판단이 틀려 안 꺼지면 사용자가 볼륨 키를
     * 내리면 되지만, 틀려서 블루투스 소리가 꺼지면 듣던 것이 갑자기 멎는다. 그래서 하위 버전
     * 폴백과 예외 처리 모두 "건너뛴다" 쪽으로 기운다.
     */
    fun isOnBluetooth(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 실제 라우팅을 직접 묻는다. "연결됨" 과 "지금 그리로 나간다" 는 다르다.
            // getAudioDevicesForAttributes 는 **API 33** 부터다(31 로 알고 있다가 lint 에 잡혔다).
            val media = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            audioManager.getAudioDevicesForAttributes(media).any { it.type in bluetoothTypes }
        } else {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in bluetoothTypes }
        }
    }.getOrDefault(true)

    val currentVolume: Int get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    /** 고정 볼륨 기기(일부 TV·독)에서는 조작이 무시된다. 그런 기기에서는 시도하지 않는다. */
    val isVolumeFixed: Boolean get() = audioManager.isVolumeFixed

    private fun setVolume(value: Int) {
        // flags = 0 → 시스템 볼륨 팝업이 뜨지 않는다.
        runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0) }
    }

    /**
     * 차폐를 켤 때 부른다. 실제로 음소거했고 **복원해야 하면** 그 직전 음량을 돌려준다.
     * 복원할 필요가 없으면(끔 / 볼륨 0 / 블루투스 / 고정 볼륨 / 이미 0) null.
     */
    fun applyOnCover(action: MediaVolumeAction): Int? {
        if (action == MediaVolumeAction.NONE) return null
        if (isVolumeFixed) return null
        // 판단은 차폐를 켜는 이 시점에 한 번만. 도중의 연결·해제는 따라가지 않는다.
        if (isOnBluetooth()) return null

        return when (action) {
            MediaVolumeAction.SET_TO_ZERO -> {
                // 켤 때마다 적용한다(사용자 결정). 되돌리지 않으므로 저장할 것도 없다.
                setVolume(0)
                null
            }

            MediaVolumeAction.MUTE_WHILE_COVERED -> {
                val before = currentVolume
                setVolume(0)
                // 이미 0 이었다면 복원할 것이 없다 — 0 을 "원래 음량" 으로 기억하면 안 된다.
                before.takeIf { it > 0 }
            }

            MediaVolumeAction.NONE -> null
        }
    }

    /**
     * 차폐를 풀 때 부른다. [saved] 는 [applyOnCover] 가 돌려준 값이다.
     *
     * 사용자가 차폐 중에 볼륨 키로 올렸더라도 저장해 둔 값으로 덮어쓴다. "현재 값이 0 이 아니면
     * 복원하지 않기" 는 잠깐 올렸다 내린 경우에 복원이 안 되어, 덮어쓰는 쪽이 예측 가능하다.
     */
    fun restore(saved: Int?) {
        if (saved == null || saved <= 0) return
        if (isVolumeFixed) return
        setVolume(saved)
    }
}
