package app.ossflow.ui.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
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
import app.ossflow.container
import app.ossflow.data.DictionaryEntity
import app.ossflow.logic.DictionaryTerms
import app.ossflow.logic.HeardAsSuggestions
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
                        editor = EditorState(null, "", "")
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
            if (entries.isEmpty()) {
                item {
                    Text(
                        "Add names or terms you want spelled correctly.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(entries, key = { it.id }) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editor = EditorState(entry.id, entry.spelling, entry.heardAs)
                            }
                            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            entry.spelling,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
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
            onSave = { spelling ->
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
                                onMessage("Saved. Mishearings were not added. Add an API key.")
                            SuggestOutcome.Failed ->
                                onMessage("Saved. Mishearings were not added.")
                            is SuggestOutcome.Ready -> {
                                if (heard.heardAs.isNotBlank()) {
                                    dao.update(
                                        DictionaryEntity(id = id, spelling = trimmed, heardAs = heard.heardAs),
                                    )
                                }
                                onMessage("Saved")
                            }
                        }
                    } else {
                        dao.update(
                            DictionaryEntity(
                                id = current.id,
                                spelling = trimmed,
                                heardAs = current.heardAs,
                            ),
                        )
                        editor = null
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
)

@Composable
private fun DictionaryEditor(
    state: EditorState,
    onDismiss: () -> Unit,
    onSave: (spelling: String) -> Unit,
) {
    var spelling by remember(state.id, state.spelling) { mutableStateOf(state.spelling) }
    val tooLong = spelling.trim().length > DictionaryTerms.MAX_CHARS
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "Add word" else "Edit word") },
        text = {
            OutlinedTextField(
                value = spelling,
                onValueChange = { spelling = it },
                label = { Text("Word") },
                isError = tooLong,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(spelling) },
                enabled = spelling.trim().isNotEmpty() && !tooLong,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
