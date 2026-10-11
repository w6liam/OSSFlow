package app.utterdroid

import android.app.Application
import app.utterdroid.overlay.BubbleNotifications

class UtterDroidApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BubbleNotifications.ensureChannel(this)
    }
}
