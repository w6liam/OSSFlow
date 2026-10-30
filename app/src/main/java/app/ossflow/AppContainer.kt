package app.ossflow

import android.content.Context
import app.ossflow.api.CleanupClient
import app.ossflow.api.ConnectionTester
import app.ossflow.api.TranscriptionRouter
import app.ossflow.audio.Recorder
import app.ossflow.data.AppDatabase
import app.ossflow.data.SettingsRepository
import app.ossflow.data.TranscriptRepository
import app.ossflow.dictation.DictationCoordinator
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase = AppDatabase.create(appContext)
    val transcripts: TranscriptRepository = TranscriptRepository(database.transcripts())
    val settings: SettingsRepository = SettingsRepository(appContext)
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    val transcription = TranscriptionRouter(http)
    val cleanup = CleanupClient(http)
    val tester = ConnectionTester(http)
    val recorder = Recorder(appContext)
    val coordinator = DictationCoordinator(
        context = appContext,
        database = database,
        settingsRepository = settings,
        transcription = transcription,
        cleanup = cleanup,
    )
}

val Context.container: AppContainer
    get() = (applicationContext as OSSFlowApp).container
