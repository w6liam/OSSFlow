package app.ossflow.data

import kotlinx.coroutines.flow.Flow

class TranscriptRepository(private val dao: TranscriptDao) {
    fun observe(): Flow<List<TranscriptEntity>> = dao.observe()

    suspend fun delete(id: Long) {
        dao.delete(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
