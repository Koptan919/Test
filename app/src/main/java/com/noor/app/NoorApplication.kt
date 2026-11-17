package com.noor.app

import android.app.Application
import com.noor.app.data.remote.ContentSync
import com.noor.app.notifications.PrayerNotifier
import com.noor.app.notifications.PrayerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class NoorApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Everything below runs off the main thread so it never delays the first frame.
        appScope.launch {
            runCatching { PrayerNotifier.createChannel(this@NoorApplication) }
        }
        appScope.launch {
            container.settings.settings
                .distinctUntilChangedBy { PrayerScheduler.Key.from(it) }
                .collect { runCatching { PrayerScheduler.reschedule(this@NoorApplication, it) } }
        }
        appScope.launch {
            container.settings.settings
                .map { it.autoContentSync }
                .distinctUntilChanged()
                .collect { runCatching { ContentSync.schedule(this@NoorApplication, it) } }
        }
    }
}
