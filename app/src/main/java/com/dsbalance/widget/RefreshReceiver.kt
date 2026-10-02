package com.dsbalance.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            RefreshScheduler.schedule(context)
            return
        }
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fetchAndStore(context)
                BalanceWidget.updateAll(context)
                RefreshScheduler.schedule(context)
            } finally {
                result.finish()
            }
        }
    }
}
