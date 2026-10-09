package app.notype.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.notype.container
import app.notype.data.TimeSavedTotals
import app.notype.logic.TimeSaved
import app.notype.model.AppSettings
import app.notype.model.SttProtocol
import app.notype.model.WritingStyle
import app.notype.ui.BubbleControls
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val repository = context.container.settings
    val tester = context.container.tester
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(AppSettings.defaults()) }
    var loaded by remember { mutableStateOf(false) }
    var revealSttKey by remember { mutableStateOf(false) }
    var revealCleanupKey by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    val totals by repository.timeSaved.collectAsState(initial = TimeSavedTotals(0L, 0L))

    LaunchedEffect(Unit) {
        draft = repository.flow.first()
        loaded = true
    }
    if (!loaded) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BubbleControls()
        Text("Transcription", style = MaterialTheme.typography.titleMedium)
        ProtocolPicker(draft.sttProtocol) { protocol ->
            draft = draft.copy(sttProtocol = protocol)
        }
        OutlinedTextField(
            value = draft.sttBaseUrl,
            onValueChange = { draft = draft.copy(sttBaseUrl = it) },
            label = { Text("Base URL") },
            supportingText = {
                Text(
                    if (draft.sttProtocol == SttProtocol.GROK) {
                        "Grok uses /stt on this URL."
                    } else {
                        "OpenAI-compatible endpoints use /audio/transcriptions."
                    },
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SecretField(
            value = draft.sttApiKey,
            onValueChange = { draft = draft.copy(sttApiKey = it) },
            label = "API key",
            revealed = revealSttKey,
            onToggle = { revealSttKey = !revealSttKey },
        )
        OutlinedTextField(
            value = draft.sttModel,
            onValueChange = { draft = draft.copy(sttModel = it) },
            label = { Text("Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.sttLanguage,
            onValueChange = { draft = draft.copy(sttLanguage = it) },
            label = { Text("Language") },
            supportingText = { Text("For example en") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Cleanup", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        ToggleRow("Clean up the transcript", draft.cleanupEnabled) { enabled ->
            draft = draft.copy(cleanupEnabled = enabled)
            scope.launch { repository.update { it.copy(cleanupEnabled = enabled) } }
        }
        Text("Writing style", style = MaterialTheme.typography.bodyLarge)
        StylePicker(draft.writingStyle) { style ->
            draft = draft.copy(writingStyle = style)
            scope.launch { repository.update { it.copy(writingStyle = style) } }
        }
        Text(
            "Normal is standard English. Formal is polite and professional. Informal is relaxed, with contractions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ToggleRow("Use the same API key and base URL", draft.cleanupSameCredentials) { same ->
            draft = draft.copy(cleanupSameCredentials = same)
            scope.launch { repository.update { it.copy(cleanupSameCredentials = same) } }
        }
        if (!draft.cleanupSameCredentials) {
            OutlinedTextField(
                value = draft.cleanupBaseUrl,
                onValueChange = { draft = draft.copy(cleanupBaseUrl = it) },
                label = { Text("Cleanup base URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SecretField(
                value = draft.cleanupApiKey,
                onValueChange = { draft = draft.copy(cleanupApiKey = it) },
                label = "Cleanup API key",
                revealed = revealCleanupKey,
                onToggle = { revealCleanupKey = !revealCleanupKey },
            )
        }
        OutlinedTextField(
            value = draft.cleanupModel,
            onValueChange = { draft = draft.copy(cleanupModel = it) },
            label = { Text("Cleanup model") },
            supportingText = { Text("Chat completions. A non-reasoning model answers faster.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                scope.launch {
                    repository.update { draft }
                    draft = repository.flow.first()
                    onMessage("Saved")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }

        OutlinedButton(
            onClick = {
                scope.launch {
                    repository.update { draft }
                    val saved = repository.flow.first()
                    draft = saved
                    testResult = tester.test(saved.sttBaseUrl, saved.sttApiKey)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Test transcription") }

        if (!draft.cleanupSameCredentials) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        repository.update { draft }
                        val saved = repository.flow.first()
                        draft = saved
                        testResult = tester.test(saved.cleanupBaseUrl, saved.cleanupApiKey)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Test cleanup") }
        }
        Text("Time saved", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(
            TimeSaved.formatMinutes(TimeSaved.minutes(totals.words, totals.speakingMs)),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "Deleting a transcript leaves this total unchanged.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { confirmReset = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset time saved") }

        if (testResult.isNotEmpty()) {
            Text(
                testResult,
                color = if (testResult == "Connected") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset time saved?") },
            text = { Text("The counter goes back to zero. History stays.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch { repository.resetTimeSaved() }
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProtocolPicker(selected: SttProtocol, onSelect: (SttProtocol) -> Unit) {
    val options = SttProtocol.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, protocol ->
            SegmentedButton(
                selected = selected == protocol,
                onClick = { onSelect(protocol) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(if (protocol == SttProtocol.GROK) "Grok" else "OpenAI")
            }
        }
    }
}

@Composable
private fun StylePicker(selected: WritingStyle, onSelect: (WritingStyle) -> Unit) {
    val options = listOf(WritingStyle.FORMAL, WritingStyle.NORMAL, WritingStyle.INFORMAL)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, style ->
            SegmentedButton(
                selected = selected == style,
                onClick = { onSelect(style) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(
                    when (style) {
                        WritingStyle.FORMAL -> "Formal"
                        WritingStyle.NORMAL -> "Normal"
                        WritingStyle.INFORMAL -> "Informal"
                    },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    revealed: Boolean,
    onToggle: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = onToggle) { Text(if (revealed) "Hide" else "Show") }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
