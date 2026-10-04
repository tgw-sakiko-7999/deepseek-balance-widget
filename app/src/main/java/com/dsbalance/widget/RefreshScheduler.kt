package com.dsbalance.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

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
        val triggerAt = System.currentTimeMillis() + INTERVAL_MS
        // Android 12/12L 只认 SCHEDULE_EXACT_ALARM，13+ 才认 USE_EXACT_ALARM；
        // 拿不到精确闹钟权限就退回不精确的，宁可晚几秒也不能崩或者断链喵
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC, triggerAt, pi)
                return
            } catch (e: SecurityException) {
                // 权限查询结果与实际不一致时继续往下兜底喵
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC, triggerAt, pi)
    }
}
