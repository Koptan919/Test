package com.noor.app

import android.app.Application
import com.noor.app.notifications.PrayerNotifier
import com.noor.app.notifications.PrayerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class NoorApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        PrayerNotifier.createChannel(this)
        // Keep the prayer reminder schedule in sync with the relevant settings.
        appScope.launch {
            container.settings.settings
                .distinctUntilChangedBy { PrayerScheduler.Key.from(it) }
                .collect { PrayerScheduler.reschedule(this@NoorApplication, it) }
        }
    }
}
