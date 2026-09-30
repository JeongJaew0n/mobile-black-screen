package com.jjw.blackscreen.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jjw.blackscreen.data.ClockFont
import com.jjw.blackscreen.ui.family
import com.jjw.blackscreen.ui.weight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.jjw.blackscreen.data.DateStyle
import com.jjw.blackscreen.ui.theme.Comp
import com.jjw.blackscreen.ui.theme.Sys
import com.jjw.blackscreen.media.NowPlaying
import com.jjw.blackscreen.ui.rememberDateSample
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.jjw.blackscreen.data.MediaVolumeAction
import com.jjw.blackscreen.data.Settings
import kotlin.math.roundToInt
import android.content.pm.ApplicationInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.OutlinedButton
import com.jjw.blackscreen.data.AUTO_OFF_CHOICES
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.role
import androidx.compose.material3.FilterChipDefaults

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
    notificationAccess: Boolean,
    nowPlaying: NowPlaying?,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    /** 사이드로딩 + Android 13+ — '제한된 설정' 에 걸렸을 가능성. 권한 카드에 푸는 법을 덧붙인다. */
    mayBeRestricted: Boolean,
    onOpenAppInfo: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        // 위는 **보는 곳**(제목·권한 안내·탭·미리보기), 아래는 **누르는 곳**(시작 버튼).
        // 한 손 파지에서 엄지가 편한 곳이 아래다 — One UI 도 같은 구분을 쓴다(docs/ui-research.md §5).
        Column(
            Modifier
                .padding(horizontal = Comp.Settings.screenPadding)
                .padding(top = Comp.Settings.screenPadding),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )

            // 고를 수 있는 모드가 하나뿐이면 라디오는 소음이다. 섹션째 감춘다.
            // Mode.available 을 되돌리면 이 UI 도 그대로 돌아온다.
            if (Mode.available.size > 1) {
                SettingsGroup(stringResource(R.string.section_mode)) {
                    Mode.available.forEach { mode ->
                        ModeRow(
                            label = stringResource(mode.labelRes),
                            description = stringResource(mode.descriptionRes),
                            selected = settings.mode == mode,
                            onSelect = { onChange { s -> s.copy(mode = mode) } },
                        )
                    }
                }
            }

            // 권한 안내는 모드 선택을 숨겨도 계속 필요하다. 없으면 아무것도 못 한다.
            if (settings.mode == Mode.FULL && !accessibilityEnabled) {
                PermissionCard(
                    message = stringResource(R.string.accessibility_needed),
                    action = stringResource(R.string.accessibility_open),
                    onClick = onRequestAccessibility,
                    restrictedHelp = mayBeRestricted,
                    onOpenAppInfo = onOpenAppInfo,
                )
            }
            if (settings.mode == Mode.OVERLAY && !canDrawOverlays) {
                PermissionCard(
                    message = stringResource(R.string.overlay_permission_needed),
                    action = stringResource(R.string.grant_permission),
                    onClick = onRequestOverlayPermission,
                )
            }
        }

        // 화면(무엇이 뜨는가)과 동작(어떻게 동작하는가)을 탭으로 나눈다.
        // 선택한 탭은 회전을 넘겨 살리고 앱을 다시 열면 첫 탭으로 — 저장할 만한 상태는 아니다.
        var tab by rememberSaveable { mutableIntStateOf(0) }
        TabRow(selectedTabIndex = tab, modifier = Modifier.padding(top = Sys.Spacing.md)) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text(stringResource(R.string.tab_display)) },
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text(stringResource(R.string.tab_settings)) },
            )
        }

        // 탭마다 스크롤 위치를 따로 둔다 — 한쪽을 내려 두고 다른 탭에 가면 그쪽은 맨 위부터.
        val displayScroll = rememberScrollState()
        val behaviorScroll = rememberScrollState()

        // 화면 탭은 그 자체가 차폐 화면 편집기다. 미리보기는 **맨 위에 고정**하고, 바꾸는 즉시 결과가 보인다.
        // 예전엔 목록 맨 끝이라 시계를 켜면 결과가 화면 밖에 생겼다(docs/ui-research.md §1).
        // 스크롤하면 통째로 줄어 아래 옵션에 자리를 내준다.
        if (tab == 0) {
            val density = LocalDensity.current
            val range = with(density) { Comp.Preview.height.toPx() }
            val mediaPreview = settings.showMedia && (nowPlaying != null || !notificationAccess)
            Column(
                Modifier
                    .padding(horizontal = Comp.Settings.screenPadding)
                    .padding(top = Sys.Spacing.md),
            ) {
                BlackScreenPreview(
                    settings,
                    nowPlaying = if (mediaPreview) nowPlaying else null,
                    mediaPermissionMissing = settings.showMedia && !notificationAccess,
                    onMediaPlayPause = onMediaPlayPause,
                    onMediaNext = onMediaNext,
                    onMediaPrevious = onMediaPrevious,
                    // 값이 아니라 읽는 함수를 넘긴다. 컴포지션에서 스크롤 값을 읽으면 매 프레임 화면 전체가
                    // 다시 구성된다(lint FrequentlyChangingValue). 미리보기는 레이아웃·그리기 단계에서 읽는다.
                    collapse = { if (range > 0f) displayScroll.value / range else 0f },
                    emptyHint = stringResource(R.string.preview_empty),
                )
                Text(
                    text = stringResource(R.string.content_preview_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Comp.Settings.captionGap),
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(if (tab == 0) displayScroll else behaviorScroll)
                .padding(horizontal = Comp.Settings.screenPadding),
        ) {
            if (tab == 0) {

            SettingsGroup {
                SwitchRow(
                    label = stringResource(R.string.show_clock),
                    checked = settings.showClock,
                    onCheckedChange = { on -> onChange { s -> s.copy(showClock = on) } },
                )

                if (settings.showClock) {
                    GroupLabel(stringResource(R.string.clock_format))
                    // 두 개뿐인 단일 선택 — 칩 한 줄. 예시는 리소스에 박지 않고 현재 로케일로 서식한다.
                    ChoiceChips(
                        options = ClockStyle.entries,
                        selected = settings.clockStyle,
                        label = { style ->
                            stringResource(
                                R.string.clock_style_label,
                                stringResource(style.labelRes),
                                rememberClockSample(style),
                            )
                        },
                        onSelect = { style -> onChange { s -> s.copy(clockStyle = style) } },
                    )

                    GroupLabel(stringResource(R.string.clock_font))
                    // 글꼴 이름이 아니라 그 글꼴로 찍은 숫자를 보여준다. 이름을 읽고 상상하게 하면
                    // 고르고 나서 실망한다. 위 미리보기가 고른 글꼴로 즉시 바뀐다.
                    val digits = rememberClockSample(settings.clockStyle)
                        .let { Regex("\\d{1,2}:\\d{2}").find(it)?.value ?: it }
                    // 다섯 칸을 같은 폭으로 — 카드 안쪽 여백만큼 좁아지자 필기체 칸이 두 줄로 깨졌다(실기기).
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = Sys.Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Comp.Settings.fontChipGap),
                    ) {
                        ClockFont.entries.forEach { font ->
                            val selected = settings.clockFont == font
                            val name = stringResource(font.labelRes)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(Comp.Settings.fontChipRadius))
                                    .selectable(selected = selected, role = Role.RadioButton) {
                                        onChange { s -> s.copy(clockFont = font) }
                                    }
                                    .background(Sys.Color.canvas)
                                    .border(
                                        width = if (selected) Comp.Settings.selectedBorder else Comp.Settings.idleBorder,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Sys.Color.floatingBorder,
                                        shape = RoundedCornerShape(Comp.Settings.fontChipRadius),
                                    )
                                    .heightIn(min = Comp.Settings.rowMinHeight)
                                    .padding(
                                        horizontal = Comp.Settings.fontChipPaddingHorizontal,
                                        vertical = Comp.Settings.fontChipPaddingVertical,
                                    )
                                    .semantics { contentDescription = name },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = digits,
                                    color = Sys.Color.onCanvas,
                                    fontSize = Comp.Settings.fontChipSample,
                                    fontFamily = font.family,
                                    fontWeight = font.weight,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
            }

            SettingsGroup {
                SwitchRow(
                    label = stringResource(R.string.show_date),
                    checked = settings.showDate,
                    onCheckedChange = { on -> onChange { s -> s.copy(showDate = on) } },
                )
                if (settings.showDate) {
                    // 형식은 여섯 가지라 펼치면 길어진다. 한 줄(현재 형식의 오늘 날짜)만 보이고, 누르면 모달에서 고른다.
                    var dateDialog by rememberSaveable { mutableStateOf(false) }
                    ValueRow(
                        label = stringResource(R.string.date_format),
                        value = rememberDateSample(settings.dateStyle),
                        onClick = { dateDialog = true },
                    )
                    if (dateDialog) {
                        DateStyleDialog(
                            current = settings.dateStyle,
                            onSelect = { style ->
                                onChange { s -> s.copy(dateStyle = style) }
                                dateDialog = false
                            },
                            onDismiss = { dateDialog = false },
                        )
                    }
                }
            }

            SettingsGroup {
                SwitchRow(
                    label = stringResource(R.string.show_battery),
                    checked = settings.showBattery,
                    onCheckedChange = { on -> onChange { s -> s.copy(showBattery = on) } },
                )
                SwitchRow(
                    label = stringResource(R.string.show_media),
                    description = stringResource(R.string.show_media_desc),
                    checked = settings.showMedia,
                    onCheckedChange = { on -> onChange { s -> s.copy(showMedia = on) } },
                )
                // 권한을 처음부터 안 줬거나 나중에 꺼 버린 경우. 문구는 차폐 화면 안내와 같다.
                if (settings.showMedia && !notificationAccess) {
                    PermissionCard(
                        message = stringResource(R.string.media_permission_needed),
                        action = stringResource(R.string.grant_permission),
                        onClick = onRequestNotificationAccess,
                        restrictedHelp = mayBeRestricted,
                        onOpenAppInfo = onOpenAppInfo,
                    )
                }
                OutlinedTextField(
                    value = settings.sentence,
                    onValueChange = { text -> onChange { s -> s.copy(sentence = text) } },
                    label = { Text(stringResource(R.string.sentence)) },
                    placeholder = { Text(stringResource(R.string.sentence_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Sys.Spacing.sm),
                )
            }

            } else {

            SettingsGroup(stringResource(R.string.section_bubble)) {
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
                    SwitchRow(
                        label = stringResource(R.string.bubble_hidden),
                        description = stringResource(R.string.bubble_hidden_desc),
                        checked = settings.bubbleHidden,
                        onCheckedChange = { on -> onChange { s -> s.copy(bubbleHidden = on) } },
                    )
                    GroupLabel(stringResource(R.string.bubble_size_value, settings.bubbleSizeDp))
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

                GroupLabel(stringResource(R.string.bubble_icon))
                // 버블을 꺼 둔 상태에서도 고를 수 있게 스위치 밖에 둔다 — 켰을 때 어떤 모양이 뜰지
                // 미리 정해 두는 용도다. 미리보기는 실제 버블과 같은 BubbleGlyph 를 같은 배경 위에 그린다.
                // 이름표는 없다. 기호가 곧 이름이고, 글자가 붙으면 다섯 개가 한 줄에 안 들어간다.
                // 대신 contentDescription 으로 스크린리더에는 이름을 준다.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Sys.Spacing.xs),
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
                                .padding(Comp.Settings.swatchPadding)
                                .size(Comp.Settings.swatchSize.dp)
                                .clip(CircleShape)
                                .background(Sys.Color.floatingSurface)
                                .border(
                                    width = if (selected) Comp.Settings.selectedBorder else Comp.Settings.idleBorder,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Sys.Color.floatingBorder,
                                    shape = CircleShape,
                                )
                                .semantics { contentDescription = name },
                            contentAlignment = Alignment.Center,
                        ) {
                            BubbleGlyph(icon = icon, sizeDp = Comp.Settings.swatchSize)
                        }
                    }
                }
                GroupNote(stringResource(R.string.bubble_permission_note))
            }

            SettingsGroup(stringResource(R.string.section_sound)) {
                // 선택지마다 설명이 붙어 칩으로 줄이지 않는다 — 되돌아오는가를 읽어야 고를 수 있다.
                MediaVolumeAction.entries.forEach { action ->
                    ModeRow(
                        label = stringResource(action.labelRes),
                        description = stringResource(action.descriptionRes),
                        selected = settings.mediaVolumeAction == action,
                        onSelect = { onChange { s -> s.copy(mediaVolumeAction = action) } },
                    )
                }
                GroupNote(stringResource(R.string.media_volume_note))
            }

            SettingsGroup(stringResource(R.string.section_unlock)) {
                // 미제공 제스처(available = false)는 목록에서 빠진다. 코드는 남아 있다.
                // 고르면 아래에 누르는 시간 슬라이더가 붙어 라디오로 둔다.
                Gesture.available.forEach { gesture ->
                    RadioRow(
                        label = stringResource(gesture.labelRes),
                        selected = settings.unlockGesture == gesture,
                        onSelect = { onChange { s -> s.copy(unlockGesture = gesture) } },
                    )
                }
                // 누르는 시간은 꾹 눌러 해제에서만 의미가 있다.
                if (settings.unlockGesture == Gesture.LONG_PRESS) {
                    // 숫자 서식과 단위 어순은 리소스가 정한다 (독일어는 1,5 / 한국어는 뒤에 '초').
                    GroupLabel(stringResource(R.string.hold_duration_value, settings.holdMillis / 1000f))
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
            }

            SettingsGroup(stringResource(R.string.section_auto_off)) {
                // 여섯 개의 단일 선택 — 라디오는 다섯 개까지가 권장이라 세로로 너무 길었다. 칩 한 줄.
                ChoiceChips(
                    options = autoOffChoices(),
                    selected = settings.autoOffMinutes,
                    label = { minutes -> autoOffLabel(minutes) },
                    onSelect = { minutes -> onChange { s -> s.copy(autoOffMinutes = minutes) } },
                )
                GroupNote(stringResource(R.string.auto_off_desc))
            }
            }

            Spacer(Modifier.height(Sys.Spacing.lg))
        }

        // 누르는 곳. 목록을 어디까지 내려도 해제 방법·부수 효과 안내와 함께 늘 보인다.
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = Comp.Settings.screenPadding, vertical = Comp.Settings.bottomBarPadding),
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Comp.Settings.rowMinHeight),
            ) {
                Text(stringResource(R.string.start_blackout))
            }
            // 해제 방법은 차폐 화면에 단서가 없어 여기서 먼저 말한다.
            // 되돌아오지 않는 부수 효과(볼륨 0)와 자동 끄기도 같은 줄에 — 켜져 있는 줄 모르고 누르지 않게.
            Text(
                text = startHint(settings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Sys.Spacing.sm),
            )
        }
    }
}

/** 시작 버튼 아래 한 줄 — 해제 방법 · 소리 · 자동 끄기. 켜진 것만 잇는다. */
@Composable
private fun startHint(settings: Settings): String {
    val parts = buildList {
        add(
            stringResource(
                when (settings.unlockGesture) {
                    Gesture.TRIPLE_TAP -> R.string.start_hint_triple_tap
                    Gesture.DOUBLE_TAP -> R.string.start_hint_double_tap
                    Gesture.LONG_PRESS -> R.string.start_hint_long_press
                    Gesture.FINGERPRINT -> R.string.start_hint_fingerprint
                    Gesture.SLIDE_TO_UNLOCK -> R.string.start_hint_slide
                    Gesture.SWIPE -> R.string.start_hint_swipe
                },
            ),
        )
        when (settings.mediaVolumeAction) {
            MediaVolumeAction.NONE -> Unit
            MediaVolumeAction.MUTE_WHILE_COVERED -> add(stringResource(R.string.start_hint_mute))
            MediaVolumeAction.SET_TO_ZERO -> add(stringResource(R.string.start_hint_zero))
        }
        if (settings.autoOffMinutes > 0) {
            // 라디오와 같은 단위로 — '1시간' 을 골랐는데 '60분 뒤' 라고 하면 다른 설정처럼 읽힌다(실기기).
            add(stringResource(R.string.start_hint_timer, autoOffLabel(settings.autoOffMinutes)))
        }
    }
    return parts.joinToString(stringResource(R.string.start_hint_separator))
}

/** 디버그 빌드에는 시험용 1분을 앞에 붙인다 — 15분을 기다리지 않고 타이머를 확인하려고. */
@Composable
private fun autoOffChoices(): List<Int> {
    val debuggable = LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    return if (debuggable) listOf(AUTO_OFF_CHOICES.first(), 1) + AUTO_OFF_CHOICES.drop(1) else AUTO_OFF_CHOICES
}

@Composable
private fun autoOffLabel(minutes: Int): String = when {
    minutes <= 0 -> stringResource(R.string.auto_off_never)
    minutes % 60 == 0 -> pluralStringResource(R.plurals.auto_off_hours, minutes / 60, minutes / 60)
    else -> pluralStringResource(R.plurals.auto_off_minutes, minutes, minutes)
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

private val ClockFont.labelRes: Int
    get() = when (this) {
        ClockFont.LIGHT -> R.string.clock_font_light
        ClockFont.BOLD -> R.string.clock_font_bold
        ClockFont.SERIF -> R.string.clock_font_serif
        ClockFont.MONO -> R.string.clock_font_mono
        ClockFont.CURSIVE -> R.string.clock_font_cursive
    }

private val BubbleIcon.labelRes: Int
    get() = when (this) {
        BubbleIcon.BAR -> R.string.bubble_icon_bar
        BubbleIcon.DOT -> R.string.bubble_icon_dot
        BubbleIcon.RING -> R.string.bubble_icon_ring
        BubbleIcon.MOON -> R.string.bubble_icon_moon
        BubbleIcon.POWER -> R.string.bubble_icon_power
    }

private val MediaVolumeAction.labelRes: Int
    get() = when (this) {
        MediaVolumeAction.NONE -> R.string.media_volume_none
        MediaVolumeAction.MUTE_WHILE_COVERED -> R.string.media_volume_mute
        MediaVolumeAction.SET_TO_ZERO -> R.string.media_volume_zero
    }

private val MediaVolumeAction.descriptionRes: Int
    get() = when (this) {
        MediaVolumeAction.NONE -> R.string.media_volume_none_desc
        MediaVolumeAction.MUTE_WHILE_COVERED -> R.string.media_volume_mute_desc
        MediaVolumeAction.SET_TO_ZERO -> R.string.media_volume_zero_desc
    }

private val Gesture.labelRes: Int
    get() = when (this) {
        Gesture.LONG_PRESS -> R.string.gesture_long_press
        Gesture.DOUBLE_TAP -> R.string.gesture_double_tap
        Gesture.TRIPLE_TAP -> R.string.gesture_triple_tap
        Gesture.SWIPE -> R.string.gesture_swipe
        Gesture.SLIDE_TO_UNLOCK -> R.string.gesture_slide_to_unlock
        Gesture.FINGERPRINT -> R.string.gesture_fingerprint
    }

/**
 * 설정 묶음. 구분선 + 라벨 대신 **톤이 다른 카드**로 가른다 — 다크 테마에서 선은 약하고, Android 16 과
 * One UI 설정도 이렇게 묶는다(docs/ui-research.md §4). 색은 수묵화 토큰의 surfaceContainer.
 */
@Composable
private fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(top = Comp.Settings.groupGap)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Sys.Spacing.xs, bottom = Sys.Spacing.xs),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Comp.Settings.groupRadius))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(
                    horizontal = Comp.Settings.groupPaddingHorizontal,
                    vertical = Comp.Settings.groupPaddingVertical,
                ),
            content = content,
        )
    }
}

/** 묶음 안의 소제목(시계 형식, 버블 크기 …). */
@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = Sys.Spacing.sm),
    )
}

/** 묶음 끝의 설명. */
@Composable
private fun GroupNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = Sys.Spacing.sm),
    )
}

/**
 * 짧은 단일 선택. 안정 API 인 FilterChip — M3 Expressive 의 연결 버튼 그룹은 Compose 에서 아직 실험
 * API 라 쓰지 않는다. FlowRow 라 글자 크기를 키우면 줄이 바뀐다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Sys.Spacing.xs)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Comp.Settings.chipGap),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                // 기본 선택색(secondaryContainer)은 카드 바탕과 톤이 가까워 구분이 약했다 — 청회색 컨테이너로.
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    selectedBorderWidth = Comp.Settings.idleBorder,
                ),
                modifier = Modifier.semantics { role = Role.RadioButton },
            )
        }
    }
}

/** 행 전체가 하나의 스위치다 — 라벨을 눌러도 켜지고, 스크린리더는 라벨과 상태를 한 번에 읽는다. */
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
            .heightIn(min = Comp.Settings.rowMinHeight)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = Sys.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = Sys.Spacing.md)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun PermissionCard(
    message: String,
    action: String,
    onClick: () -> Unit,
    /** '제한된 설정' 푸는 법. 켜려는데 막혀 있으면 이 순서를 모르고는 못 푼다 — 경고를 먼저 봐야 메뉴가 생긴다. */
    restrictedHelp: Boolean = false,
    onOpenAppInfo: () -> Unit = {},
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Sys.Spacing.md),
    ) {
        Column(Modifier.padding(Sys.Spacing.lg)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (restrictedHelp) {
                Text(
                    text = stringResource(R.string.restricted_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = Sys.Spacing.sm),
                )
            }
            Spacer(Modifier.height(Sys.Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Sys.Spacing.sm)) {
                Button(onClick = onClick) { Text(action) }
                if (restrictedHelp) {
                    OutlinedButton(onClick = onOpenAppInfo) { Text(stringResource(R.string.open_app_info)) }
                }
            }
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
            .heightIn(min = Comp.Settings.rowMinHeight)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = Sys.Spacing.sm),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = Sys.Spacing.sm)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 라벨 + 현재 값 한 줄. 누르면 호출부가 모달을 연다. */
@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Comp.Settings.rowMinHeight)
            .clip(RoundedCornerShape(Comp.Settings.rowRadius))
            .clickable(onClick = onClick)
            .padding(vertical = Sys.Spacing.md, horizontal = Sys.Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * 날짜 형식 모달. 항목 이름은 없다 — **오늘 날짜를 그 형식으로 찍은 것**이 항목이다.
 * "중간 · 요일 포함" 같은 이름을 읽고 상상하게 하는 것보다 결과를 보여주는 쪽이 빠르다.
 */
@Composable
private fun DateStyleDialog(
    current: DateStyle,
    onSelect: (DateStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.date_format)) },
        text = {
            Column {
                DateStyle.entries.forEach { style ->
                    RadioRow(
                        label = rememberDateSample(style),
                        selected = style == current,
                        onSelect = { onSelect(style) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Comp.Settings.rowMinHeight)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = Comp.Settings.rowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(start = Sys.Spacing.sm),
        )
    }
}
