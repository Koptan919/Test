package com.noor.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.noor.app.MainActivity
import com.noor.app.NoorApplication
import com.noor.app.R
import com.noor.app.data.prayer.PrayerRepository
import com.noor.app.data.prefs.AppSettings
import com.noor.app.ui.prayerName
import com.noor.app.util.Formatters
import com.noor.app.util.LocaleManager
import com.noor.core.prayer.Prayer
import com.noor.core.prayer.PrayerMoment
import com.noor.core.prayer.PrayerTimesCalculator
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object PrayerNotifier {
    const val CHANNEL_ID = "prayer_times"

    fun createChannel(context: Context) {
        val ctx = LocaleManager.wrap(context)
        val channel = NotificationChannel(
            CHANNEL_ID,
            ctx.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = ctx.getString(R.string.notification_channel_desc) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, prayer: Prayer, time: ZonedDateTime) {
        if (!canNotify(context)) return
        val ctx = LocaleManager.wrap(context)
        val arabic = LocaleManager.isArabic(context)
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val name = prayerName(ctx, prayer)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.notification_title, name))
            .setContentText(ctx.getString(R.string.notification_text, name, Formatters.time(time, arabic)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(prayer.ordinal, notification)
        } catch (_: SecurityException) {
            // Permission revoked between check and post; nothing to do.
        }
    }
}

/**
 * Schedules a single WorkManager job for the next enabled prayer. When it runs it posts
 * the reminder and schedules the following one, so only one job is ever pending.
 */
object PrayerScheduler {
    private const val WORK_NAME = "prayer_reminder"
    internal const val KEY_PRAYER = "prayer"
    internal const val KEY_TIME = "time"

    /** The subset of settings that affects scheduling. */
    data class Key(
        val enabled: Boolean,
        val prayers: Set<Prayer>,
        val city: String?,
        val device: Any?,
        val method: Any,
    ) {
        companion object {
            fun from(s: AppSettings) = Key(s.notificationsEnabled, s.notifyPrayers, s.cityId, s.deviceLocation, s.calculationMethod)
        }
    }

    fun reschedule(context: Context, settings: AppSettings, after: ZonedDateTime = ZonedDateTime.now()) {
        val wm = WorkManager.getInstance(context)
        if (!settings.notificationsEnabled || settings.notifyPrayers.isEmpty()) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val next = nextEnabled(settings, after) ?: run {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = Duration.between(Instant.now(), next.time.toInstant()).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<PrayerReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_PRAYER to next.prayer.name, KEY_TIME to next.time.toInstant().toEpochMilli()))
            .build()
        wm.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    internal fun nextEnabled(settings: AppSettings, from: ZonedDateTime): PrayerMoment? {
        val location = PrayerRepository().location(settings) ?: return null
        val calc = PrayerTimesCalculator(settings.calculationMethod)
        var cursor = from
        repeat(12) {
            val next = calc.nextPrayer(cursor, location.coordinates, location.zone) ?: return null
            if (next.prayer in settings.notifyPrayers) return next
            cursor = next.time
        }
        return null
    }
}

class PrayerReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as NoorApplication
        val settings = app.container.settings.settings.first()
        val prayer = inputData.getString(PrayerScheduler.KEY_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
        val time = inputData.getLong(PrayerScheduler.KEY_TIME, 0L)
        if (settings.notificationsEnabled && prayer != null && prayer in settings.notifyPrayers) {
            val due = Instant.ofEpochMilli(time)
            // Only remind if we are close to the prayer time (WorkManager may be deferred by Doze).
            if (abs(Duration.between(due, Instant.now()).toMinutes()) <= LATE_TOLERANCE_MIN) {
                val zone = PrayerRepository().location(settings)?.zone ?: java.time.ZoneId.systemDefault()
                PrayerNotifier.show(applicationContext, prayer, ZonedDateTime.ofInstant(due, zone))
            }
        }
        // Schedule the following reminder strictly after this one.
        val after = maxOf(Instant.now(), Instant.ofEpochMilli(time).plusSeconds(60))
        PrayerScheduler.reschedule(applicationContext, settings, ZonedDateTime.ofInstant(after, java.time.ZoneId.systemDefault()))
        return Result.success()
    }

    companion object {
        private const val LATE_TOLERANCE_MIN = 20L
    }
}
