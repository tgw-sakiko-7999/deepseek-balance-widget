package com.dsbalance.widget

import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class BalanceInfo(
    val currency: String,
    val totalBalance: BigDecimal,
)

/** 只抛这一种业务异常，message 就是给小组件看的短文案喵 */
class ApiException(message: String) : Exception(message)

object BalanceApi {
    private const val ENDPOINT = "https://api.deepseek.com/user/balance"

    fun fetch(apiKey: String): BalanceInfo {
        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Accept", "application/json")
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code != 200) throw ApiException("HTTP $code：${body.take(120)}")
        // 响应读完、流已关闭，连接就回到连接池了；此时再 disconnect 反而会掐断它，
        // 下次还得重做一次 TLS 握手，所以这里不做喵
        return parse(body)
    }

    private fun parse(body: String): BalanceInfo {
        val root = JSONObject(body)
        val infos = root.getJSONArray("balance_infos")
        if (infos.length() == 0) throw ApiException("返回没有余额条目")
        // is_available=false 只表示余额不足以调 API，余额数字照常显示（0 元时由阈值提醒标红）喵
        val entries = (0 until infos.length()).map { infos.getJSONObject(it) }
        // 多币种账户优先挑有余额的那条，全为 0 时退回第一条喵
        val info = entries.firstOrNull {
            it.optString("total_balance").toBigDecimalOrNull()?.signum() == 1
        } ?: entries.first()
        return BalanceInfo(
            currency = info.getString("currency"),
            totalBalance = info.getString("total_balance").toBigDecimalOrNull()
                ?: throw ApiException("余额字段不是数字"),
        )
    }
}
