package com.example.durtlesreborn

import android.app.Application
import com.example.durtlesreborn.notify.NotificationScheduler
import com.example.durtlesreborn.notify.WidgetUpdater
import com.example.durtlesreborn.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DurtlesApp : Application() {
    val container by lazy { AppContainer(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Keep WorkManager's periodic sync in line with the setting.
        appScope.launch {
            container.settingsStore.settings
                .map { it.enableBackgroundSync }
                .distinctUntilChanged()
                .collect { SyncScheduler.setEnabled(this@DurtlesApp, it) }
        }
        // Keep the hourly notification alarm in line with the setting, and refresh the notification when it is switched on.
        appScope.launch {
            container.settingsStore.settings
                .map { it.enableNotifications }
                .distinctUntilChanged()
                .collect { enabled ->
                    val widgets = WidgetUpdater.hasWidgets(this@DurtlesApp)
                    if (enabled || widgets) {
                        NotificationScheduler.scheduleNext(this@DurtlesApp)
                        container.refreshAlerts()
                    } else {
                        NotificationScheduler.cancel(this@DurtlesApp)
                    }
                }
        }
    }
}
