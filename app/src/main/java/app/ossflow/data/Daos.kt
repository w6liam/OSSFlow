package app.ossflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcripts ORDER BY createdAt DESC")
    fun observe(): Flow<List<TranscriptEntity>>

    @Insert
    suspend fun insert(item: TranscriptEntity)

    @Query("DELETE FROM transcripts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transcripts")
    suspend fun deleteAll()
}

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary ORDER BY spelling COLLATE NOCASE ASC")
    fun observe(): Flow<List<DictionaryEntity>>

    @Query("SELECT * FROM dictionary ORDER BY spelling COLLATE NOCASE ASC")
    suspend fun all(): List<DictionaryEntity>

    @Insert
    suspend fun insert(item: DictionaryEntity): Long

    @Update
    suspend fun update(item: DictionaryEntity)

    @Query("DELETE FROM dictionary WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM dictionary")
    suspend fun count(): Int
}
