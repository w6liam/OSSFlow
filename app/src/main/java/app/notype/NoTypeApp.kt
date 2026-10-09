package app.notype

import android.app.Application
import app.notype.overlay.BubbleNotifications

class NoTypeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BubbleNotifications.ensureChannel(this)
    }
}
