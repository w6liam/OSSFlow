package app.ossflow.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.ossflow.data.TranscriptEntity
import app.ossflow.data.TranscriptRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: TranscriptRepository,
) : ViewModel() {
    val transcripts: StateFlow<List<TranscriptEntity>> = repository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAll() }
    }

    companion object {
        fun factory(repository: TranscriptRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(repository) as T
            }
    }
}
