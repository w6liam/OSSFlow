package app.utterdroid

import android.content.Context
import app.utterdroid.api.CleanupClient
import app.utterdroid.api.ConnectionTester
import app.utterdroid.api.TranscriptionRouter
import app.utterdroid.audio.Recorder
import app.utterdroid.data.AppDatabase
import app.utterdroid.data.SettingsRepository
import app.utterdroid.dictation.DictationCoordinator
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase = AppDatabase.create(appContext)
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
    get() = (applicationContext as UtterDroidApp).container
