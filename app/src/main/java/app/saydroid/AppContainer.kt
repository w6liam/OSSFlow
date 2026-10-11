package app.saydroid

import android.content.Context
import app.saydroid.api.CleanupClient
import app.saydroid.api.ConnectionTester
import app.saydroid.api.TranscriptionRouter
import app.saydroid.audio.Recorder
import app.saydroid.data.AppDatabase
import app.saydroid.data.SettingsRepository
import app.saydroid.dictation.DictationCoordinator
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
    get() = (applicationContext as SaydroidApp).container
