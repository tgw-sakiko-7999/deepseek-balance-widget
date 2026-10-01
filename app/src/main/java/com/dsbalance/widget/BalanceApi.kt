package com.dsbalance.widget

import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class BalanceInfo(
    val currency: String,
    val totalBalance: BigDecimal,
    val grantedBalance: BigDecimal?,
)

/** 只抛这一种业务异常，message 就是给小组件看的短文案喵 */
class ApiException(message: String) : Exception(message)

object BalanceApi {
    private const val ENDPOINT = "https://api.deepseek.com/user/balance"

    fun fetch(apiKey: String): BalanceInfo {
        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code != 200) throw ApiException("HTTP $code：${body.take(120)}")
            return parse(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(body: String): BalanceInfo {
        val root = JSONObject(body)
        if (!root.optBoolean("is_available", false)) throw ApiException("账户不可用（is_available=false）")
        val infos = root.getJSONArray("balance_infos")
        if (infos.length() == 0) throw ApiException("返回没有余额条目")
        val info = infos.getJSONObject(0)
        return BalanceInfo(
            currency = info.getString("currency"),
            totalBalance = info.getString("total_balance").toBigDecimalOrNull()
                ?: throw ApiException("余额字段不是数字"),
            grantedBalance = info.optString("granted_balance", "").toBigDecimalOrNull(),
        )
    }
}
