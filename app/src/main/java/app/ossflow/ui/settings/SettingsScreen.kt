package app.ossflow.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ossflow.container
import app.ossflow.logic.AppStyles
import app.ossflow.model.AppSettings
import app.ossflow.model.SttProtocol
import app.ossflow.model.WritingStyle
import app.ossflow.ui.BubbleControls
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
    var showStyleHelp by remember { mutableStateOf(false) }
    var addingApp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        draft = repository.flow.first()
        loaded = true
    }
    if (!loaded) return

    suspend fun saveDraft() {
        repository.update { current ->
            draft.copy(
                bubbleWanted = current.bubbleWanted,
                bubbleSnoozed = current.bubbleSnoozed,
            )
        }
        draft = repository.flow.first()
    }

    val saveOverrides: (Map<String, WritingStyle>) -> Unit = { next ->
        draft = draft.copy(appStyleOverrides = next)
        scope.launch { repository.update { it.copy(appStyleOverrides = next) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel("Bubble")
        BubbleControls(showTitle = false)
        SectionLabel("Transcription")
        ProtocolPicker(draft.sttProtocol) { protocol ->
            draft = draft.copy(sttProtocol = protocol)
        }
        OutlinedTextField(
            value = draft.sttBaseUrl,
            onValueChange = { draft = draft.copy(sttBaseUrl = it) },
            label = { Text("Base URL") },
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
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("Cleanup")
        OutlinedTextField(
            value = draft.cleanupInstructions,
            onValueChange = { draft = draft.copy(cleanupInstructions = it) },
            label = { Text("Custom Cleanup instructions") },
            placeholder = { Text("example: British English or US English") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        ToggleRow("Clean up the transcript", draft.cleanupEnabled) { enabled ->
            draft = draft.copy(cleanupEnabled = enabled)
            scope.launch { repository.update { it.copy(cleanupEnabled = enabled) } }
        }
        ToggleRow("Keep profanity", draft.keepProfanity) { keep ->
            draft = draft.copy(keepProfanity = keep)
            scope.launch { repository.update { it.copy(keepProfanity = keep) } }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Writing style",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            IconButton(onClick = { showStyleHelp = true }) {
                Icon(Icons.Filled.Info, contentDescription = "About writing style")
            }
        }
        Text(
            "Style changes tone and punctuation only. Meaning stays the same.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StylePicker(draft.writingStyle) { style ->
            draft = draft.copy(writingStyle = style)
            scope.launch { repository.update { it.copy(writingStyle = style) } }
        }
        if (draft.writingStyle == WritingStyle.SMART) {
            AppStyleSection(
                overrides = draft.appStyleOverrides,
                onChange = saveOverrides,
                onAdd = { addingApp = true },
            )
        }
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
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                scope.launch {
                    saveDraft()
                    onMessage("Saved")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }

        if (!draft.cleanupSameCredentials) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        saveDraft()
                        testResult = tester.test(draft.cleanupBaseUrl, draft.cleanupApiKey)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Test cleanup") }
        }
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
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        OutlinedButton(
            onClick = { confirmReset = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset time saved") }
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

    if (showStyleHelp) {
        AlertDialog(
            onDismissRequest = { showStyleHelp = false },
            title = { Text("Writing style") },
            text = {
                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text(STYLE_HELP)
                }
            },
            confirmButton = {
                TextButton(onClick = { showStyleHelp = false }) { Text("Close") }
            },
        )
    }

    if (addingApp) {
        AddAppDialog(
            onDismiss = { addingApp = false },
            onAdd = { pkg, style ->
                saveOverrides(AppStyles.withPackage(draft.appStyleOverrides, pkg, style))
                addingApp = false
            },
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
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
    val options = listOf(WritingStyle.OFF, WritingStyle.FORMAL, WritingStyle.INFORMAL, WritingStyle.SMART)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { style ->
            StyleOption(style = style, selected = selected == style, onSelect = { onSelect(style) })
        }
    }
}

@Composable
private fun StyleOption(
    style: WritingStyle,
    selected: Boolean,
    showSummary: Boolean = true,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(style.title, style = MaterialTheme.typography.bodyLarge)
            if (showSummary) {
                Text(
                    style.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppStyleSection(
    overrides: Map<String, WritingStyle>,
    onChange: (Map<String, WritingStyle>) -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SectionLabel("App styles")
        Text(
            "Tap a style to change it for that app. Apps that are not listed stay unchanged.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ToneHeading("Chat")
        for (app in AppStyles.chat) {
            AppStyleRow(app.name, AppStyles.selection(app, overrides)) { style ->
                onChange(AppStyles.withApp(overrides, app, style))
            }
        }
        ToneHeading("Work")
        for (app in AppStyles.work) {
            AppStyleRow(app.name, AppStyles.selection(app, overrides)) { style ->
                onChange(AppStyles.withApp(overrides, app, style))
            }
        }
        val extras = AppStyles.extras(overrides)
        if (extras.isNotEmpty()) {
            ToneHeading("Other apps")
            for ((pkg, style) in extras) {
                ExtraAppRow(
                    packageName = pkg,
                    style = style,
                    onSelect = { chosen -> onChange(AppStyles.withPackage(overrides, pkg, chosen)) },
                    onRemove = { onChange(AppStyles.withoutPackage(overrides, pkg)) },
                )
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Add an app")
        }
    }
}

@Composable
private fun ToneHeading(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun AppStyleRow(name: String, style: WritingStyle, onSelect: (WritingStyle) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        StyleMenu(style, onSelect)
    }
}

@Composable
private fun ExtraAppRow(
    packageName: String,
    style: WritingStyle,
    onSelect: (WritingStyle) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            packageName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        StyleMenu(style, onSelect)
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove $packageName")
        }
    }
}

@Composable
private fun StyleMenu(style: WritingStyle, onSelect: (WritingStyle) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(style.title) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            toneChoices.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun AddAppDialog(onDismiss: () -> Unit, onAdd: (String, WritingStyle) -> Unit) {
    var packageName by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(WritingStyle.FORMAL) }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add an app") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Use the Android package name if the app is not in the list.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = {
                        packageName = it
                        error = ""
                    },
                    label = { Text("Package name") },
                    placeholder = { Text("com.example.app") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                toneChoices.forEach { option ->
                    StyleOption(
                        style = option,
                        selected = style == option,
                        showSummary = false,
                        onSelect = { style = option },
                    )
                }
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pkg = packageName.trim()
                if (!AppStyles.isPackageName(pkg)) {
                    error = "Enter a package name, such as com.example.app."
                } else {
                    onAdd(pkg, style)
                }
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private val toneChoices = listOf(WritingStyle.OFF, WritingStyle.FORMAL, WritingStyle.INFORMAL)

private const val STYLE_HELP =
    "Cleanup fixes fillers, false starts, and obvious slips, then applies a writing style. " +
        "Style changes tone and punctuation only. It does not change meaning, and it does not answer or refuse what you dictated.\n\n" +
        "Off leaves the tone as you spoke it.\n\n" +
        "Formal uses polished, professional wording, with complete sentences and no slang. It suits work messages and email.\n\n" +
        "Informal uses relaxed, conversational wording, like a chat message.\n\n" +
        "Smart is the default. It picks Formal or Informal from the app you are dictating into. " +
        "Chat apps are informal: WhatsApp, Messages, Telegram, Signal, Messenger, Instagram, Snapchat, Discord, and X. " +
        "Work apps are formal: Slack, Microsoft Teams, Gmail, Outlook, Google Docs, LinkedIn, Notion, and Jira. " +
        "Any other app stays as you spoke it, the same as Off.\n\n" +
        "You can change the style for an app when Smart is selected. OSSFlow does not send the app name to the cleanup model."

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
