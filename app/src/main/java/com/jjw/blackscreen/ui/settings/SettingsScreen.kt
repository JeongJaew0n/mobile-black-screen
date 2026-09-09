package com.jjw.blackscreen.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.jjw.blackscreen.bubble.BubbleGlyph
import com.jjw.blackscreen.data.BubbleIcon
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jjw.blackscreen.ui.rememberClockSample
import com.jjw.blackscreen.ui.BlackScreenPreview
import com.jjw.blackscreen.R
import com.jjw.blackscreen.data.BUBBLE_STEP_DP
import com.jjw.blackscreen.data.ClockStyle
import com.jjw.blackscreen.data.Gesture
import com.jjw.blackscreen.data.HOLD_STEP_MILLIS
import com.jjw.blackscreen.data.MAX_BUBBLE_DP
import com.jjw.blackscreen.data.MAX_HOLD_MILLIS
import com.jjw.blackscreen.data.MIN_BUBBLE_DP
import com.jjw.blackscreen.data.MIN_HOLD_MILLIS
import com.jjw.blackscreen.data.Mode
import com.jjw.blackscreen.data.Settings
import kotlin.math.roundToInt

/**
 * @param onChange 저장된 값에 적용할 **변환 함수**를 넘긴다. 화면이 들고 있는 스냅샷을
 *   통째로 쓰면, DataStore 가 아직 값을 안 뱉은 시점(앱을 막 연 직후)에 항목 하나를
 *   바꿔도 나머지가 전부 기본값으로 덮인다. 실제로 그 버그를 냈다.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onStart: () -> Unit,
    canDrawOverlays: Boolean,
    accessibilityEnabled: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibility: () -> Unit,
    /** 슬라이더를 만지는 동안 true. 호출부가 창 밝기를 실제 값으로 낮춰 미리보기를 만든다. */
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(20.dp))

        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.start_blackout))
        }

        // 고를 수 있는 모드가 하나뿐이면 라디오는 소음이다. 섹션째 감춘다.
        // Mode.available 을 되돌리면 이 UI 도 그대로 돌아온다.
        if (Mode.available.size > 1) {
            Section(stringResource(R.string.section_mode))
            Mode.available.forEach { mode ->
                ModeRow(
                    label = stringResource(mode.labelRes),
                    description = stringResource(mode.descriptionRes),
                    selected = settings.mode == mode,
                    onSelect = { onChange { s -> s.copy(mode = mode) } },
                )
            }
        }

        // 권한 안내는 모드 선택을 숨겨도 계속 필요하다. 없으면 아무것도 못 한다.
        if (settings.mode == Mode.FULL && !accessibilityEnabled) {
            PermissionCard(
                message = stringResource(R.string.accessibility_needed),
                action = stringResource(R.string.accessibility_open),
                onClick = onRequestAccessibility,
            )
        }
        if (settings.mode == Mode.OVERLAY && !canDrawOverlays) {
            PermissionCard(
                message = stringResource(R.string.overlay_permission_needed),
                action = stringResource(R.string.grant_permission),
                onClick = onRequestOverlayPermission,
            )
        }

        Section(stringResource(R.string.section_content))

        SwitchRow(
            label = stringResource(R.string.show_clock),
            checked = settings.showClock,
            onCheckedChange = { on -> onChange { s -> s.copy(showClock = on) } },
        )

        if (settings.showClock) {
            Text(
                text = stringResource(R.string.clock_format),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            ClockStyle.entries.forEach { style ->
                RadioRow(
                    // 예시는 리소스에 박지 않고 현재 로케일로 서식한다 — 언어마다 다르다.
                    label = stringResource(
                        R.string.clock_style_label,
                        stringResource(style.labelRes),
                        rememberClockSample(style),
                    ),
                    selected = settings.clockStyle == style,
                    onSelect = { onChange { s -> s.copy(clockStyle = style) } },
                )
            }
        }

        OutlinedTextField(
            value = settings.sentence,
            onValueChange = { text -> onChange { s -> s.copy(sentence = text) } },
            label = { Text(stringResource(R.string.sentence)) },
            placeholder = { Text(stringResource(R.string.sentence_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )

        // 시계·문장을 켰을 때 실제로 어떻게 뜨는지. 차폐 화면과 같은 Composable 을 그린다.
        // 표시할 것이 없으면 상자도 없다 — 검은 사각형만 덜렁 있으면 고장으로 보인다.
        if (settings.hasContent) {
            BlackScreenPreview(settings, Modifier.padding(top = 16.dp))
            Text(
                text = stringResource(R.string.content_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Section(stringResource(R.string.section_bubble))

        SwitchRow(
            label = stringResource(R.string.bubble_enabled),
            description = stringResource(R.string.bubble_enabled_desc),
            checked = settings.bubbleEnabled,
            onCheckedChange = { on -> onChange { s -> s.copy(bubbleEnabled = on) } },
        )
        // 기본값이 켜짐이라 첫 실행에서 여기 걸린다. 스위치는 켜져 있는데 화면에는
        // 아무것도 없는 상태를 설명하지 않으면 고장으로 보인다.
        if (settings.bubbleEnabled && !canDrawOverlays) {
            PermissionCard(
                message = stringResource(R.string.overlay_permission_needed),
                action = stringResource(R.string.grant_permission),
                onClick = onRequestOverlayPermission,
            )
        }
        if (settings.bubbleEnabled) {
            Text(
                text = stringResource(R.string.bubble_size_value, settings.bubbleSizeDp),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Slider(
                value = settings.bubbleSizeDp.toFloat(),
                onValueChange = { v ->
                    // 꾹 눌러 해제 시간과 같은 방식 — 눈금 없이 움직이되 값은 2dp 에 떨군다.
                    val snapped = (v / BUBBLE_STEP_DP).roundToInt() * BUBBLE_STEP_DP
                    // 같은 값이면 넘기지 않는다. 여기 저장은 떠 있는 버블 창의
                    // updateViewLayout 까지 부르므로, 한 칸 안에서 끄는 동안
                    // 같은 값을 계속 쓰면 창을 헛되이 다시 재운다.
                    if (snapped != settings.bubbleSizeDp) {
                        onChange { s -> s.copy(bubbleSizeDp = snapped) }
                    }
                },
                valueRange = MIN_BUBBLE_DP.toFloat()..MAX_BUBBLE_DP.toFloat(),
                steps = 0,
            )

        }

        Text(
            text = stringResource(R.string.bubble_icon),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        // 버블을 꺼 둔 상태에서도 고를 수 있게 스위치 밖에 둔다 — 켰을 때 어떤 모양이 뜰지
        // 미리 정해 두는 용도다. 미리보기는 실제 버블과 같은 BubbleGlyph 를 같은 배경 위에 그린다.
        // 이름표는 없다. 기호가 곧 이름이고, 글자가 붙으면 다섯 개가 한 줄에 안 들어간다.
        // 대신 contentDescription 으로 스크린리더에는 이름을 준다.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BubbleIcon.entries.forEach { icon ->
                val selected = settings.bubbleIcon == icon
                val name = stringResource(icon.labelRes)
                Box(
                    Modifier
                        .clip(CircleShape)
                        // clickable + 수동 selected 시맨틱은 접근성 트리에 안 실렸다(실측).
                        // selectable 이 선택 상태와 라디오 역할을 함께 실어 준다.
                        .selectable(selected = selected, role = Role.RadioButton) {
                            onChange { s -> s.copy(bubbleIcon = icon) }
                        }
                        .padding(6.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xE0141414))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF),
                            shape = CircleShape,
                        )
                        .semantics { contentDescription = name },
                    contentAlignment = Alignment.Center,
                ) {
                    BubbleGlyph(icon = icon, sizeDp = 52)
                }
            }
        }

        Text(
            text = stringResource(R.string.bubble_permission_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Section(stringResource(R.string.section_unlock))

        Gesture.entries.forEach { gesture ->
            RadioRow(
                label = stringResource(gesture.labelRes),
                selected = settings.unlockGesture == gesture,
                onSelect = { onChange { s -> s.copy(unlockGesture = gesture) } },
            )
        }

        // 누르는 시간은 꾹 눌러 해제에서만 의미가 있다.
        if (settings.unlockGesture == Gesture.LONG_PRESS) {
            Text(
                // 숫자 서식과 단위 어순은 리소스가 정한다 (독일어는 1,5 / 한국어는 뒤에 '초').
                text = stringResource(R.string.hold_duration_value, settings.holdMillis / 1000f),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Slider(
                value = settings.holdMillis.toFloat(),
                onValueChange = { v ->
                    // 눈금은 그리지 않지만 값은 0.1초에 떨어뜨린다.
                    // 안 그러면 1,437ms 같은 값이 저장돼 표시가 지저분해진다.
                    val snapped = (v / HOLD_STEP_MILLIS).roundToInt() * HOLD_STEP_MILLIS
                    onChange { s -> s.copy(holdMillis = snapped) }
                },
                valueRange = MIN_HOLD_MILLIS.toFloat()..MAX_HOLD_MILLIS.toFloat(),
                // steps = 0 이라 눈금 없이 매끄럽게 움직인다.
                steps = 0,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

/** 패턴 문자열을 그대로 보여주면 알아보기 어렵다. 예시가 들어간 문구로 바꾼다. */
private val ClockStyle.labelRes: Int
    get() = when (this) {
        ClockStyle.H24 -> R.string.clock_24h
        ClockStyle.H12 -> R.string.clock_12h
    }

private val Mode.labelRes: Int
    get() = when (this) {
        Mode.FULL -> R.string.mode_full
        Mode.OVERLAY -> R.string.mode_overlay
        Mode.BLACKOUT -> R.string.mode_blackout
    }

private val Mode.descriptionRes: Int
    get() = when (this) {
        Mode.FULL -> R.string.mode_full_desc
        Mode.OVERLAY -> R.string.mode_overlay_desc
        Mode.BLACKOUT -> R.string.mode_blackout_desc
    }

private val BubbleIcon.labelRes: Int
    get() = when (this) {
        BubbleIcon.BAR -> R.string.bubble_icon_bar
        BubbleIcon.DOT -> R.string.bubble_icon_dot
        BubbleIcon.RING -> R.string.bubble_icon_ring
        BubbleIcon.MOON -> R.string.bubble_icon_moon
        BubbleIcon.POWER -> R.string.bubble_icon_power
    }

private val Gesture.labelRes: Int
    get() = when (this) {
        Gesture.LONG_PRESS -> R.string.gesture_long_press
        Gesture.DOUBLE_TAP -> R.string.gesture_double_tap
        Gesture.TRIPLE_TAP -> R.string.gesture_triple_tap
        Gesture.SWIPE -> R.string.gesture_swipe
        Gesture.SLIDE_TO_UNLOCK -> R.string.gesture_slide_to_unlock
    }

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * 실제 Screen Off 화면과 같은 조건의 표본 — 검은 바탕에 흰 글자.
 *
 * 이 상자만 보면 레벨과 무관하게 늘 같아 보인다. 슬라이더를 만지는 동안 창 전체가
 * 실제 밝기로 어두워지면서 비로소 차이가 드러난다. 둘이 한 쌍이다.
 */

@Composable
private fun PermissionCard(message: String, action: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClick) { Text(action) }
        }
    }
}

/** 라디오 + 설명문. 모드별 트레이드오프는 설명을 읽어야만 판단할 수 있어 한 줄로 못 줄인다. */
@Composable
private fun ModeRow(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
