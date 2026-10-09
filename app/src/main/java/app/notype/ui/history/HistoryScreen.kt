package app.notype.ui.history

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import app.notype.container
import app.notype.data.TranscriptEntity
import app.notype.logic.TimeSaved
import app.notype.ui.BubbleControls
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
    val dao = context.container.database.transcripts()
    val items by dao.observe().collectAsState(initial = emptyList())
    val totals by context.container.settings.timeSaved.collectAsState(
        initial = app.notype.data.TimeSavedTotals(0L, 0L),
    )
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BubbleControls() }
        item {
            MinutesSavedCard(TimeSaved.minutes(totals.words, totals.speakingMs), onOpenImpact)
        }
        if (items.isEmpty()) {
            item {
                Text(
                    "No dictations yet. Turn the bubble on, open any app, then tap the bubble and speak.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(items, key = { it.id }) { transcript ->
            DeleteRow(onDelete = { scope.launch { dao.delete(transcript.id) } }) { close ->
                TranscriptCard(transcript) {
                    close()
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("noType", transcript.text))
                    onMessage("Copied")
                }
            }
        }
    }
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
            Text(TimeSaved.formatMinutes(minutes), style = MaterialTheme.typography.headlineSmall)
            Text(
                "Since the last reset",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(transcript.text, style = MaterialTheme.typography.bodyLarge)
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
        }
    }
}

private fun formatWhen(epochMs: Long): String {
    val time = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(time)
}
