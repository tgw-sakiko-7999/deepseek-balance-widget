package com.dsbalance.widget

import android.content.Context
import android.content.SharedPreferences
import java.math.BigDecimal

/** 设置项与最近一次余额快照，全走一个 SharedPreferences 文件喵 */
class BalanceStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ds_balance", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    /** 0 表示关闭低额提醒喵 */
    var threshold: BigDecimal
        get() = prefs.getString(KEY_THRESHOLD, null)?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        set(value) = prefs.edit().putString(KEY_THRESHOLD, value.toPlainString()).apply()

    var lastBalance: BigDecimal?
        get() = prefs.getString(KEY_LAST_BALANCE, null)?.toBigDecimalOrNull()
        set(value) {
            val editor = prefs.edit()
            if (value == null) editor.remove(KEY_LAST_BALANCE)
            else editor.putString(KEY_LAST_BALANCE, value.toPlainString())
            editor.apply()
        }

    var lastCurrency: String?
        get() = prefs.getString(KEY_CURRENCY, null)
        set(value) {
            val editor = prefs.edit()
            if (value == null) editor.remove(KEY_CURRENCY)
            else editor.putString(KEY_CURRENCY, value)
            editor.apply()
        }

    var lastUpdated: Long
        get() = prefs.getLong(KEY_UPDATED, 0L)
        set(value) = prefs.edit().putLong(KEY_UPDATED, value).apply()

    var lastError: String?
        get() = prefs.getString(KEY_ERROR, null)
        set(value) {
            val editor = prefs.edit()
            if (value == null) editor.remove(KEY_ERROR)
            else editor.putString(KEY_ERROR, value)
            editor.apply()
        }

    private companion object {
        const val KEY_API = "api_key"
        const val KEY_THRESHOLD = "threshold"
        const val KEY_LAST_BALANCE = "last_balance"
        const val KEY_CURRENCY = "last_currency"
        const val KEY_UPDATED = "last_updated"
        const val KEY_ERROR = "last_error"
    }
}
