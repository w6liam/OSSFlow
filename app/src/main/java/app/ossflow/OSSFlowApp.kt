package app.ossflow

import android.app.Application
import app.ossflow.overlay.BubbleNotifications

class OSSFlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BubbleNotifications.ensureChannel(this)
    }
}
