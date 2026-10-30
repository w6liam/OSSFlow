package app.ossflow.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeTranscriptDao : TranscriptDao {
    private val rows = MutableStateFlow<List<TranscriptEntity>>(emptyList())
    var deleteCalls = 0
        private set
    var deleteAllCalls = 0
        private set

    fun snapshot(): List<TranscriptEntity> = rows.value

    override fun observe(): Flow<List<TranscriptEntity>> = rows.asStateFlow()

    override suspend fun insert(item: TranscriptEntity) {
        rows.value = listOf(item) + rows.value
    }

    override suspend fun delete(id: Long) {
        deleteCalls += 1
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        deleteAllCalls += 1
        rows.value = emptyList()
    }
}

fun transcript(id: Long, text: String = "Hello $id") = TranscriptEntity(
    id = id,
    text = text,
    rawText = "raw $text",
    createdAt = 1_700_000_000_000L + id,
    cleanupSkipped = false,
    inserted = true,
)
