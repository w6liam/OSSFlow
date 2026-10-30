package app.ossflow.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptRepositoryTest {
    @Test
    fun deleteAllRemovesEveryTranscriptInOneCall() = runBlocking {
        val dao = FakeTranscriptDao()
        dao.insert(transcript(1))
        dao.insert(transcript(2))
        dao.insert(transcript(3))
        val repository = TranscriptRepository(dao)

        repository.deleteAll()

        assertTrue(repository.observe().first().isEmpty())
        assertEquals(1, dao.deleteAllCalls)
        assertEquals(0, dao.deleteCalls)
    }

    @Test
    fun deleteAllOnAnEmptyHistoryStillUsesOneCall() = runBlocking {
        val dao = FakeTranscriptDao()
        val repository = TranscriptRepository(dao)

        repository.deleteAll()

        assertEquals(emptyList<TranscriptEntity>(), repository.observe().first())
        assertEquals(1, dao.deleteAllCalls)
        assertEquals(0, dao.deleteCalls)
    }

    @Test
    fun deleteRemovesOnlyTheChosenTranscript() = runBlocking {
        val dao = FakeTranscriptDao()
        dao.insert(transcript(1, "one"))
        dao.insert(transcript(2, "two"))
        val repository = TranscriptRepository(dao)

        repository.delete(1)

        assertEquals(listOf(2L), repository.observe().first().map { it.id })
        assertEquals(listOf("two"), dao.snapshot().map { it.text })
        assertEquals(1, dao.deleteCalls)
        assertEquals(0, dao.deleteAllCalls)
    }
}
