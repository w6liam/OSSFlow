package app.ossflow.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.ossflow.container
import app.ossflow.logic.TimeSaved

@Composable
fun ImpactScreen(onBack: () -> Unit) {
    val totals by LocalContext.current.container.settings.timeSaved.collectAsState(
        initial = app.ossflow.data.TimeSavedTotals(0L, 0L),
    )
    val saved = TimeSaved.minutes(totals.words, totals.speakingMs)
    val typing = TimeSaved.typingMinutes(totals.words)
    val speaking = totals.speakingMs.coerceAtLeast(0L) / 60_000.0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Minutes saved", style = MaterialTheme.typography.labelLarge)
        Text(TimeSaved.formatMinutes(saved), style = MaterialTheme.typography.displaySmall)
        Stat("Words dictated", totals.words.toString())
        Stat("Typing at 40 words per minute", TimeSaved.formatMinutes(typing))
        Stat("Time spent speaking", TimeSaved.formatMinutes(speaking))
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
