package app.notype.ui.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.notype.container
import app.notype.data.DictionaryEntity
import app.notype.logic.DictionaryTerms
import app.notype.logic.HeardAsSuggestions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun DictionaryScreen(onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val dao = context.container.database.dictionary()
    val entries by dao.observe().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<EditorState?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (entries.size >= DictionaryTerms.MAX_TERMS) {
                        onMessage("The dictionary holds ${DictionaryTerms.MAX_TERMS} words.")
                    } else {
                        editor = EditorState(null, "", "", suggesting = false)
                    }
                },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add word")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Add a word. noType asks the cleanup model how a recognizer might hear it, and you can add more.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entries.isEmpty()) {
                item {
                    Text(
                        "Nothing here yet. Add a word, for example LawVu or Sissi.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(entries, key = { it.id }) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editor = EditorState(entry.id, entry.spelling, entry.heardAs, suggesting = false)
                            }
                            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(entry.spelling, style = MaterialTheme.typography.titleMedium)
                            if (entry.heardAs.isNotBlank()) {
                                Text(
                                    "Heard as ${entry.heardAs}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { scope.launch { dao.delete(entry.id) } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete ${entry.spelling}")
                        }
                    }
                }
            }
        }
    }

    val current = editor
    if (current != null) {
        DictionaryEditor(
            state = current,
            onDismiss = { editor = null },
            onSave = { spelling, heardAs ->
                scope.launch {
                    val trimmed = spelling.trim()
                    if (trimmed.isEmpty()) {
                        onMessage("Add the word you want.")
                        return@launch
                    }
                    if (trimmed.length > DictionaryTerms.MAX_CHARS) {
                        onMessage("Keep the word to ${DictionaryTerms.MAX_CHARS} characters.")
                        return@launch
                    }
                    if (current.id == null) {
                        if (dao.count() >= DictionaryTerms.MAX_TERMS) {
                            onMessage("The dictionary holds ${DictionaryTerms.MAX_TERMS} words.")
                            return@launch
                        }
                        val id = dao.insert(DictionaryEntity(spelling = trimmed, heardAs = ""))
                        editor = null
                        when (val heard = suggestMishearings(context, trimmed, "")) {
                            SuggestOutcome.MissingKey ->
                                onMessage("Saved. Add an API key to suggest mishearings.")
                            SuggestOutcome.Failed ->
                                onMessage("Saved. Suggestions were not added.")
                            is SuggestOutcome.Ready -> {
                                if (heard.heardAs.isBlank()) {
                                    onMessage("Saved. No mishearings suggested.")
                                } else {
                                    dao.update(DictionaryEntity(id = id, spelling = trimmed, heardAs = heard.heardAs))
                                    onMessage("Saved")
                                }
                            }
                        }
                    } else {
                        dao.update(DictionaryEntity(id = current.id, spelling = trimmed, heardAs = heardAs.trim()))
                        editor = null
                    }
                }
            },
            onSuggest = { spelling, heardAs, onResult ->
                scope.launch {
                    editor = editor?.copy(suggesting = true)
                    val heard = suggestMishearings(context, spelling, heardAs)
                    editor = editor?.copy(suggesting = false)
                    when (heard) {
                        SuggestOutcome.MissingKey -> onMessage("Add an API key to suggest mishearings.")
                        SuggestOutcome.Failed -> onMessage("Suggestions were not added.")
                        is SuggestOutcome.Ready -> {
                            if (DictionaryTerms.parseHeardAs(heard.heardAs).size ==
                                DictionaryTerms.parseHeardAs(heardAs).size
                            ) {
                                onMessage("No new mishearings suggested.")
                            }
                            onResult(heard.heardAs)
                        }
                    }
                }
            },
        )
    }
}

private sealed interface SuggestOutcome {
    data class Ready(val heardAs: String) : SuggestOutcome
    data object MissingKey : SuggestOutcome
    data object Failed : SuggestOutcome
}

private suspend fun suggestMishearings(
    context: android.content.Context,
    spelling: String,
    existing: String,
): SuggestOutcome {
    val settings = context.container.settings.flow.first()
    if (settings.effectiveCleanupApiKey.isBlank()) return SuggestOutcome.MissingKey
    return try {
        val modelText = context.container.cleanup.suggestMishearings(spelling, settings)
        SuggestOutcome.Ready(HeardAsSuggestions.merge(existing, modelText, spelling))
    } catch (_: Exception) {
        SuggestOutcome.Failed
    }
}

private data class EditorState(
    val id: Long?,
    val spelling: String,
    val heardAs: String,
    val suggesting: Boolean,
)

@Composable
private fun DictionaryEditor(
    state: EditorState,
    onDismiss: () -> Unit,
    onSave: (spelling: String, heardAs: String) -> Unit,
    onSuggest: (spelling: String, heardAs: String, onResult: (String) -> Unit) -> Unit,
) {
    var spelling by remember(state.id, state.spelling) { mutableStateOf(state.spelling) }
    var heardAs by remember(state.id) { mutableStateOf(state.heardAs) }
    val tooLong = spelling.trim().length > DictionaryTerms.MAX_CHARS
    val adding = state.id == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (adding) "Add word" else "Edit word") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = spelling,
                    onValueChange = { spelling = it },
                    label = { Text("Word") },
                    supportingText = { Text("LawVu") },
                    isError = tooLong,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!adding) {
                    OutlinedTextField(
                        value = heardAs,
                        onValueChange = { heardAs = it },
                        label = { Text("Often heard as") },
                        supportingText = { Text("Comma-separated. Add any the model missed.") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = {
                            onSuggest(spelling.trim(), heardAs) { heardAs = it }
                        },
                        enabled = spelling.trim().isNotEmpty() && !state.suggesting,
                    ) {
                        Text(if (state.suggesting) "Suggesting…" else "Suggest mishearings")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(spelling, heardAs) },
                enabled = spelling.trim().isNotEmpty() && !tooLong && !state.suggesting,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
