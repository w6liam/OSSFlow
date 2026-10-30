package app.ossflow.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ossflow.R
import app.ossflow.container
import app.ossflow.data.TranscriptEntity
import app.ossflow.logic.TimeSaved
import app.ossflow.ui.BubbleControls

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private enum class DeleteDrag { Closed, Tab, Dismissed }

@Composable
fun HistoryScreen(onMessage: (String) -> Unit, onOpenImpact: () -> Unit) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(context.container.transcripts),
    )
    val items by viewModel.transcripts.collectAsState()
    val totals by context.container.settings.timeSaved.collectAsState(
        initial = app.ossflow.data.TimeSavedTotals(0L, 0L),
    )
    var confirmDeleteAll by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BubbleControls() }
        item {
            MinutesSavedCard(TimeSaved.minutes(totals.words, totals.speakingMs), onOpenImpact)
        }
        item {
            TranscriptionsHeader(
                canDelete = items.isNotEmpty(),
                onDeleteAll = { confirmDeleteAll = true },
            )
        }
        if (items.isEmpty()) {
            item {
                Text(
                    "No dictations yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(items, key = { it.id }) { transcript ->
            DeleteRow(onDelete = { viewModel.delete(transcript.id) }) { close ->
                TranscriptCard(transcript) {
                    close()
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("OSSFlow", transcript.text))
                    onMessage("Copied")
                }
            }
        }
    }
    if (confirmDeleteAll) {
        DeleteAllTranscriptsDialog(
            onDismiss = { confirmDeleteAll = false },
            onConfirm = {
                confirmDeleteAll = false
                viewModel.deleteAll()
            },
        )
    }
}

@Composable
private fun TranscriptionsHeader(canDelete: Boolean, onDeleteAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Transcriptions",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (canDelete) {
            TextButton(
                onClick = onDeleteAll,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.delete_all))
            }
        }
    }
}

@Composable
private fun DeleteAllTranscriptsDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_all_transcripts_title)) },
        text = { Text(stringResource(R.string.delete_all_transcripts_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.delete_all))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun MinutesSavedCard(minutes: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Minutes saved",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                TimeSaved.formatMinutes(minutes),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun DeleteRow(onDelete: () -> Unit, content: @Composable (close: () -> Unit) -> Unit) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var rowWidth by remember { mutableFloatStateOf(0f) }
    val state = remember {
        lateinit var held: AnchoredDraggableState<DeleteDrag>
        held = AnchoredDraggableState(
            initialValue = DeleteDrag.Closed,
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { 100_000f },
            snapAnimationSpec = tween(200),
            decayAnimationSpec = exponentialDecay(),
            confirmValueChange = { target ->
                target != DeleteDrag.Dismissed || held.offset <= -rowWidth * 0.65f
            },
        )
        held
    }
    LaunchedEffect(rowWidth) {
        if (rowWidth <= 0f) return@LaunchedEffect
        val tab = with(density) { 88.dp.toPx() }.coerceAtMost(rowWidth * 0.45f)
        state.updateAnchors(
            DraggableAnchors {
                DeleteDrag.Closed at 0f
                DeleteDrag.Tab at -tab
                DeleteDrag.Dismissed at -rowWidth
            },
            state.targetValue,
        )
    }
    LaunchedEffect(state.settledValue) {
        if (state.settledValue == DeleteDrag.Dismissed) onDelete()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidth = it.width.toFloat() },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CardDefaults.shape)
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(88.dp)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.onError)
            }
        }
        val offset = state.offset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(if (offset.isNaN()) 0 else offset.roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal),
        ) {
            content {
                if (state.currentValue != DeleteDrag.Closed) {
                    scope.launch { state.snapTo(DeleteDrag.Closed) }
                }
            }
        }
    }
}

@Composable
private fun TranscriptCard(transcript: TranscriptEntity, onClick: () -> Unit) {
    var showOriginal by remember(transcript.id) { mutableStateOf(false) }
    val rawDiffers = transcript.rawText.isNotEmpty() && transcript.rawText != transcript.text
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(transcript.text, style = MaterialTheme.typography.bodyLarge)
            if (rawDiffers && showOriginal) {
                OriginalTranscript(transcript.rawText)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    formatWhen(transcript.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (transcript.cleanupSkipped) {
                    Text(
                        "Not cleaned",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (!transcript.inserted) {
                    Text(
                        "Not inserted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (rawDiffers) {
                TextButton(
                    onClick = { showOriginal = !showOriginal },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        if (showOriginal) "Hide original" else "Show original",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun OriginalTranscript(rawText: String) {
    Text(
        "Original",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        rawText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatWhen(epochMs: Long): String {
    val time = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(time)
}
