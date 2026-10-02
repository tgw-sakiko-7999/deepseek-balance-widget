package com.dsbalance.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = BalanceStore(this)
        RefreshScheduler.schedule(this)
        // 从组件长按菜单（重新配置）或初次添加进来时带 widget id，保存后要回传结果喵
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setContent {
            MaterialTheme {
                var apiKey by remember { mutableStateOf(store.apiKey) }
                var threshold by remember {
                    mutableStateOf(
                        store.threshold.takeIf { it > BigDecimal.ZERO }?.toPlainString().orEmpty(),
                    )
                }
                val scope = rememberCoroutineScope()
                var hasCustomMascot by remember { mutableStateOf(mascotFile(this@MainActivity).exists()) }
                val pickMascot = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia(),
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { saveMascotFromUri(this@MainActivity, uri) }
                            Toast.makeText(
                                this@MainActivity,
                                if (ok) "表情包已更换" else "图片处理失败",
                                Toast.LENGTH_SHORT,
                            ).show()
                            if (ok) {
                                hasCustomMascot = true
                                BalanceWidget.updateAll(this@MainActivity)
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("DeepSeek 余额小组件", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = threshold,
                        onValueChange = { threshold = it },
                        label = { Text("低额提醒阈值（0 为关闭）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            store.apiKey = apiKey
                            store.threshold = threshold.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            scope.launch {
                                fetchAndStore(this@MainActivity)
                                BalanceWidget.updateAll(this@MainActivity)
                                Toast.makeText(this@MainActivity, "已保存并刷新", Toast.LENGTH_SHORT).show()
                            }
                            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                                )
                                finish()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("保存并刷新") }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = {
                            pickMascot.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }) { Text("更换表情包") }
                        if (hasCustomMascot) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) { resetMascot(this@MainActivity) }
                                    if (ok) {
                                        hasCustomMascot = false
                                        BalanceWidget.updateAll(this@MainActivity)
                                        Toast.makeText(this@MainActivity, "已恢复默认", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) { Text("恢复默认") }
                        }
                    }
                    Text("API Key 只保存在本应用私有存储中，不会外传。", style = MaterialTheme.typography.bodySmall)
                    Text("保存后回到桌面：长按空白处 → 小组件 → DeepSeek 余额。", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

}
