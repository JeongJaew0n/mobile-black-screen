package com.jjw.blackscreen.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jjw.blackscreen.R
import com.jjw.blackscreen.data.ClockFormats
import com.jjw.blackscreen.data.Gesture
import com.jjw.blackscreen.data.Mode
import com.jjw.blackscreen.data.Settings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onStart: () -> Unit,
    canDrawOverlays: Boolean,
    onRequestOverlayPermission: () -> Unit,
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

        Section(stringResource(R.string.section_mode))

        ModeRow(
            label = stringResource(R.string.mode_blackout),
            description = stringResource(R.string.mode_blackout_desc),
            selected = settings.mode == Mode.BLACKOUT,
            onSelect = { onChange(settings.copy(mode = Mode.BLACKOUT)) },
        )
        ModeRow(
            label = stringResource(R.string.mode_overlay),
            description = stringResource(R.string.mode_overlay_desc),
            selected = settings.mode == Mode.OVERLAY,
            onSelect = { onChange(settings.copy(mode = Mode.OVERLAY)) },
        )

        if (settings.mode == Mode.OVERLAY && !canDrawOverlays) {
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
                        text = stringResource(R.string.overlay_permission_needed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRequestOverlayPermission) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }
        }

        Section(stringResource(R.string.section_content))

        SwitchRow(
            label = stringResource(R.string.show_clock),
            checked = settings.showClock,
            onCheckedChange = { onChange(settings.copy(showClock = it)) },
        )

        if (settings.showClock) {
            Text(
                text = stringResource(R.string.clock_format),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            ClockFormats.ALL.forEach { format ->
                RadioRow(
                    label = format,
                    selected = settings.clockFormat == format,
                    onSelect = { onChange(settings.copy(clockFormat = format)) },
                )
            }
        }

        OutlinedTextField(
            value = settings.sentence,
            onValueChange = { onChange(settings.copy(sentence = it)) },
            label = { Text(stringResource(R.string.sentence)) },
            placeholder = { Text(stringResource(R.string.sentence_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )

        Section(stringResource(R.string.section_appearance))

        Text(
            text = "${stringResource(R.string.text_level)}  ${settings.textLevel}",
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = settings.textLevel.toFloat(),
            onValueChange = { onChange(settings.copy(textLevel = it.roundToInt())) },
            valueRange = 1f..5f,
            steps = 3,
        )

        SwitchRow(
            label = stringResource(R.string.burn_in_shift),
            description = stringResource(R.string.burn_in_shift_desc),
            checked = settings.burnInShiftEnabled,
            onCheckedChange = { onChange(settings.copy(burnInShiftEnabled = it)) },
        )

        Section(stringResource(R.string.section_bubble))

        SwitchRow(
            label = stringResource(R.string.bubble_enabled),
            description = stringResource(R.string.bubble_enabled_desc),
            checked = settings.bubbleEnabled,
            onCheckedChange = { onChange(settings.copy(bubbleEnabled = it)) },
        )
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
                onSelect = { onChange(settings.copy(unlockGesture = gesture)) },
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

private val Gesture.labelRes: Int
    get() = when (this) {
        Gesture.LONG_PRESS -> R.string.gesture_long_press
        Gesture.DOUBLE_TAP -> R.string.gesture_double_tap
        Gesture.TRIPLE_TAP -> R.string.gesture_triple_tap
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

/** 라디오 + 설명문. 두 모드의 트레이드오프는 설명을 읽어야만 판단할 수 있어 한 줄로 못 줄인다. */
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
