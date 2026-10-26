package com.bdcalorietracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "bd_calorie_reminder"

        val channel = NotificationChannel(
            channelId,
            "Daily Meal Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("BD Calorie Tracker • খাবার লগ করুন")
            .setContentText("Don't forget to snap or log your bhaat, dal, and meals today!")
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "bd_daily_meal_reminder"

        fun schedule(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (enabled) {
                val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(4, TimeUnit.HOURS)
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } else {
                workManager.cancelUniqueWork(WORK_NAME)
            }
        }
    }
}
