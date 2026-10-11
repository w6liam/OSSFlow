package app.utterdroid.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.utterdroid.logic.DictionaryTerms
import app.utterdroid.model.Term

@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val rawText: String,
    val createdAt: Long,
    val cleanupSkipped: Boolean,
    val inserted: Boolean,
)

@Entity(tableName = "dictionary")
data class DictionaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spelling: String,
    val heardAs: String,
) {
    fun toTerm(): Term = Term(spelling, DictionaryTerms.parseHeardAs(heardAs))
}
