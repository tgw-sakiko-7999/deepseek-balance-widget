package com.dsbalance.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object RefreshScheduler {
    private const val INTERVAL_MS = 60_000L

    /** 每分钟拉一次余额：非唤醒的精确单次闹钟，由 RefreshReceiver 触发后续约喵 */
    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, RefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + INTERVAL_MS, pi)
    }
}
