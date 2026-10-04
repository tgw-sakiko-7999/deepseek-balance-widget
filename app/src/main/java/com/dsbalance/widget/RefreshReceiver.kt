package com.dsbalance.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 开机和覆盖安装升级都会清掉已排的闹钟，这里只重排一次，余额交给链条首环去拉喵
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            RefreshScheduler.schedule(context)
            return
        }
        val screenOn =
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var hasWidgets = true
            try {
                if (screenOn) {
                    fetchAndStore(context)
                    hasWidgets = BalanceWidget.updateAll(context)
                } else {
                    // 灭屏时用户看不到组件，省掉这次网络请求和重渲染，只判断还要不要续闹钟喵
                    hasWidgets = BalanceWidget.hasWidgets(context)
                }
            } catch (e: Exception) {
                // 取数或刷新出错既不能让进程崩，也不能断链，下一环再判断组件还在不在喵
                Log.w(TAG, "refresh failed", e)
            } finally {
                // 桌面已经没有实例就停链，免得空转着每分钟发一次请求喵
                if (hasWidgets) {
                    RefreshScheduler.schedule(context)
                }
                result.finish()
            }
        }
    }

    private companion object {
        const val TAG = "RefreshReceiver"
    }
}
