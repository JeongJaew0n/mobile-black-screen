package com.jjw.blackscreen.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    // Service 와 Activity 양쪽에서 쓰이므로 applicationContext 로 고정해 누수를 막는다.
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.toSettings() }

    /** 저장된 값에 [transform] 을 적용하고 그 결과를 돌려준다. */
    suspend fun update(transform: (Settings) -> Settings): Settings {
        var result = Settings()
        store.edit { prefs ->
            val next = transform(prefs.toSettings())
            result = next
            prefs[Keys.MODE] = next.mode.name
            prefs[Keys.SHOW_CLOCK] = next.showClock
            prefs[Keys.CLOCK_STYLE] = next.clockStyle.name
            prefs[Keys.SENTENCE] = next.sentence
            prefs[Keys.UNLOCK_GESTURE] = next.unlockGesture.name
            prefs[Keys.HOLD_MILLIS] = next.holdMillis
            prefs[Keys.BUBBLE_ENABLED] = next.bubbleEnabled
            prefs[Keys.BUBBLE_EDGE] = next.bubbleEdge.name
            prefs[Keys.BUBBLE_SIZE_DP] = next.bubbleSizeDp
            prefs[Keys.BUBBLE_Y_RATIO] = next.bubbleYRatio
        }
        return result
    }

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")

        /** 구버전 키(패턴 문자열). 읽기 전용 — [ClockStyle.fromLegacyPattern] 로 옮긴다. */
        val CLOCK_FORMAT = stringPreferencesKey("clock_format")
        val SENTENCE = stringPreferencesKey("sentence")
        val UNLOCK_GESTURE = stringPreferencesKey("unlock_gesture")
        val HOLD_MILLIS = intPreferencesKey("hold_millis")
        val BUBBLE_ENABLED = booleanPreferencesKey("bubble_enabled")
        val BUBBLE_EDGE = stringPreferencesKey("bubble_edge")
        val BUBBLE_SIZE_DP = intPreferencesKey("bubble_size_dp")

        /** 구버전 키. 읽기 전용 — [bubbleDpFromLegacyLevel] 로 옮긴다. */
        val BUBBLE_SIZE_LEVEL = intPreferencesKey("bubble_size_level")
        val BUBBLE_Y_RATIO = floatPreferencesKey("bubble_y_ratio")
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            // 비활성 모드가 저장돼 있으면 갇히지 않도록 교정한다.
            mode = Mode.sanitize(enumOrDefault(this[Keys.MODE], defaults.mode)),
            showClock = this[Keys.SHOW_CLOCK] ?: defaults.showClock,
            clockStyle = this[Keys.CLOCK_STYLE]?.let { enumOrDefault(it, defaults.clockStyle) }
                ?: ClockStyle.fromLegacyPattern(this[Keys.CLOCK_FORMAT]),
            sentence = this[Keys.SENTENCE] ?: defaults.sentence,
            unlockGesture = enumOrDefault(this[Keys.UNLOCK_GESTURE], defaults.unlockGesture),
            // 범위 밖 값이 들어와도 제스처가 먹통이 되지 않게 자른다.
            holdMillis = (this[Keys.HOLD_MILLIS] ?: defaults.holdMillis)
                .coerceIn(MIN_HOLD_MILLIS, MAX_HOLD_MILLIS),
            bubbleEnabled = this[Keys.BUBBLE_ENABLED] ?: defaults.bubbleEnabled,
            bubbleEdge = enumOrDefault(this[Keys.BUBBLE_EDGE], defaults.bubbleEdge),
            bubbleSizeDp = (
                this[Keys.BUBBLE_SIZE_DP]
                    ?: this[Keys.BUBBLE_SIZE_LEVEL]?.let(::bubbleDpFromLegacyLevel)
                    ?: defaults.bubbleSizeDp
                ).coerceIn(MIN_BUBBLE_DP, MAX_BUBBLE_DP),
            bubbleYRatio = this[Keys.BUBBLE_Y_RATIO] ?: defaults.bubbleYRatio,
        )
    }

    /** 저장된 이름이 사라진 enum 상수를 가리키면(다운그레이드·이름 변경) 기본값으로 되돌린다. */
    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
