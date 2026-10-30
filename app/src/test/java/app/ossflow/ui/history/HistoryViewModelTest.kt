package app.ossflow.ui.history

import app.ossflow.data.FakeTranscriptDao
import app.ossflow.data.TranscriptRepository
import app.ossflow.data.transcript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcher = object : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deleteAllClearsEveryTranscript() = runTest(dispatcher) {
        val dao = FakeTranscriptDao()
        dao.insert(transcript(1))
        dao.insert(transcript(2))
        val viewModel = HistoryViewModel(TranscriptRepository(dao))
        backgroundScope.launch { viewModel.transcripts.collect {} }
        advanceUntilIdle()
        assertEquals(listOf(2L, 1L), viewModel.transcripts.value.map { it.id })

        viewModel.deleteAll()
        advanceUntilIdle()

        assertEquals(emptyList<Long>(), viewModel.transcripts.value.map { it.id })
        assertEquals(1, dao.deleteAllCalls)
        assertEquals(0, dao.deleteCalls)
    }

    @Test
    fun deleteRemovesOnlyTheChosenTranscript() = runTest(dispatcher) {
        val dao = FakeTranscriptDao()
        dao.insert(transcript(1))
        dao.insert(transcript(2))
        val viewModel = HistoryViewModel(TranscriptRepository(dao))
        backgroundScope.launch { viewModel.transcripts.collect {} }
        advanceUntilIdle()

        viewModel.delete(1)
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.transcripts.value.map { it.id })
        assertEquals(1, dao.deleteCalls)
        assertEquals(0, dao.deleteAllCalls)
    }
}
