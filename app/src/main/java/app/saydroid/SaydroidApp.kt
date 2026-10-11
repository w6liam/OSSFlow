package app.saydroid

import android.app.Application
import app.saydroid.overlay.BubbleNotifications

class SaydroidApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BubbleNotifications.ensureChannel(this)
    }
}
